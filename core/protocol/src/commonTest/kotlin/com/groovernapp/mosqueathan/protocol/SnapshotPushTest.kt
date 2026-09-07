package com.groovernapp.mosqueathan.protocol

import com.groovernapp.mosqueathan.domain.Announcement
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.SlideshowSchedule
import com.groovernapp.mosqueathan.domain.StandbySchedule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The push direction, which did not exist until now.
 *
 * Sync was one GET followed by overwriting this device, so the only thing that could go wrong was
 * losing a controller's work. Now a phone can overwrite the television the whole congregation is
 * looking at, and three of these tests are about the difference between "leave that alone" and
 * "empty that".
 */
class SnapshotPushTest {

    private val announcement = Announcement(id = "a1", content = "halo", isActive = true, position = 0)
    private val slideshow = SlideshowSchedule(id = "s1", startTime = "05:00", endTime = "06:00", day = "Monday")
    private val standby = StandbySchedule(id = "b1", startTime = "22:00", endTime = "04:00", day = "Monday")

    private fun push(tables: Collection<String>) = SnapshotPush.of(
        tables = tables,
        config = { MosqueConfig(adzanLeadMinutes = 9) },
        announcements = { listOf(announcement) },
        slideshowSchedules = { listOf(slideshow) },
        standbySchedules = { listOf(standby) }
    )

    @Test
    fun takeServerPushesNothing() {
        val stamps = SyncConflict.Stamps(SyncConflict.COMPARED_TABLES.associateWith { 5L })
        val newer = SyncConflict.Stamps(SyncConflict.COMPARED_TABLES.associateWith { 9L })
        assertEquals(
            emptyList(),
            SyncConflict.tablesToPush(SyncConflict.Choice.TakeServer, server = stamps, local = newer),
            "choosing the display's copy must send nothing, however new this device's is"
        )
    }

    @Test
    fun takeLocalPushesEveryPushableTable() {
        val nothing = SyncConflict.Stamps()
        assertEquals(
            SyncConflict.PUSHABLE_TABLES,
            SyncConflict.tablesToPush(SyncConflict.Choice.TakeLocal, server = nothing, local = nothing),
            "choosing this device's copy must send every table that can cross"
        )
    }

    @Test
    fun takeLocalNeverPushesAMediaTable() {
        val pushed = SyncConflict.tablesToPush(
            SyncConflict.Choice.TakeLocal,
            server = SyncConflict.Stamps(),
            local = SyncConflict.Stamps(SyncConflict.COMPARED_TABLES.associateWith { 9L })
        )
        // The rows name files, and a path on a phone names nothing on the display. Sending them
        // would leave a television reading "failed to load image" for every slide, having thrown
        // away the rows that named files it does hold.
        listOf("media", "friday_media", "dashboard_backgrounds").forEach { table ->
            assertFalse(table in pushed, "$table must never be pushed: its rows name files")
        }
    }

    @Test
    fun newestWinsPushesOnlyTheTablesThisDeviceWroteLater() {
        val server = SyncConflict.Stamps(
            mapOf("mosque_config" to 100L, "announcements" to 100L, "slideshow_schedules" to 100L)
        )
        val local = SyncConflict.Stamps(
            mapOf("mosque_config" to 200L, "announcements" to 50L, "standby_schedules" to 1L)
        )
        assertEquals(
            listOf("mosque_config", "standby_schedules"),
            SyncConflict.tablesToPush(SyncConflict.Choice.NewestWins, server, local),
            "only the tables this device wrote more recently travel"
        )
    }

    @Test
    fun aTableNotNamedIsNullRatherThanEmpty() {
        val only = push(listOf(SnapshotPush.TABLE_ANNOUNCEMENTS))
        assertEquals(listOf(announcement), only.announcements)
        // Null and empty are the two answers that must never be confused: null leaves the
        // display's table alone, empty empties it.
        assertNull(only.config, "an unnamed table must be null, not a default")
        assertNull(only.slideshowSchedules)
        assertNull(only.standbySchedules)
        assertFalse(only.isEmpty)
    }

    @Test
    fun anEmptyTableStillCrossesAsEmpty() {
        val emptied = SnapshotPush(announcements = emptyList())
        val message = buildJsonObject {
            put("action", RemoteActions.APPLY_SNAPSHOT)
            putSnapshotPush(emptied)
        }.toString()

        val received = parseSnapshotPush(message)
        // This is how a running text deleted at home actually disappears from the display. An
        // additive sync would restore it from the other side on the next sync.
        assertEquals(emptyList(), received.announcements, "an empty list must survive the wire")
        assertNull(received.config, "and an absent table must still arrive absent")
    }

    @Test
    fun anEmptyPushIsRecognisableWithoutInspectingEveryField() {
        assertTrue(SnapshotPush().isEmpty)
        assertTrue(push(emptyList()).isEmpty, "naming no table must produce an empty push")
    }

    @Test
    fun theEnvelopeKeysDoNotStopThePayloadDecoding() {
        val message = buildJsonObject {
            put("action", RemoteActions.APPLY_SNAPSHOT)
            put("deviceId", "remote_abc")
            putSnapshotPush(push(SyncConflict.PUSHABLE_TABLES))
        }.toString()

        val received = parseSnapshotPush(message)
        assertEquals(9, received.config?.adzanLeadMinutes)
        assertEquals(listOf(announcement), received.announcements)
        assertEquals(listOf(slideshow), received.slideshowSchedules)
        assertEquals(listOf(standby), received.standbySchedules)
    }

    @Test
    fun aPayloadWithTheWrongTypesThrowsRatherThanDecodingToDefaults() {
        // Found on hardware, restoring a display from values read out of SQLite: integer 0/1 where
        // a Boolean belongs. Silently decoding to defaults would be the worst outcome — the push
        // would apply, carrying every Boolean setting as false, and report success. Throwing lets
        // the caller refuse the whole message, which is what the display does.
        val wrongTypes = """{"action":"apply_snapshot","config":{"jumatEnabled":1}}"""

        val thrown = runCatching { parseSnapshotPush(wrongTypes) }.exceptionOrNull()
        assertTrue(
            thrown != null,
            "a payload whose booleans are 0/1 must not decode; it must be refused"
        )
    }

    @Test
    fun theReceiverKeepsItsOwnFilePaths() {
        val sender = MosqueConfig(
            adzanLeadMinutes = 9,
            customBackgroundPath = "/data/user/0/…/phone-picture.jpg",
            murottalAudioPaths = "/data/user/0/…/phone-recitation.mp3"
        )
        val display = MosqueConfig(
            customBackgroundPath = "/data/data/tv/files/bg.jpg",
            murottalAudioPaths = "/data/data/tv/files/murottal.mp3"
        )

        val applied = SnapshotPush(config = sender).sanitisedConfig(display)

        assertEquals(9, applied?.adzanLeadMinutes, "the settings themselves must cross")
        // A path on the sender's storage names nothing here. Adopting it would leave the display
        // on its default background and silent at the Friday recitation.
        assertEquals("/data/data/tv/files/bg.jpg", applied?.customBackgroundPath)
        assertEquals("/data/data/tv/files/murottal.mp3", applied?.murottalAudioPaths)
    }

    @Test
    fun noConfigInMeansNoConfigOut() {
        assertNull(
            SnapshotPush(announcements = emptyList()).sanitisedConfig(MosqueConfig()),
            "a push that names no config must not conjure one from the current row"
        )
    }
}
