package com.groovernapp.mosqueathan.data.room

import com.groovernapp.mosqueathan.domain.Announcement
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.SlideshowSchedule
import com.groovernapp.mosqueathan.domain.StandbySchedule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a sync push actually does to a display's tables.
 *
 * This is the one operation in the app that can overwrite the television the whole congregation is
 * looking at with a copy from someone's phone, so the cases here are mostly about what it must
 * *not* touch. `SnapshotConfigCoverageTest` covers the field list; this covers the rows.
 */
class RoomApplySnapshotTest {

    private companion object {
        const val SEEDED_AT = 111L
        const val PUSHED_AT = 999L
    }

    private fun display(): Pair<FakeMosqueDao, RoomConfigRepository> {
        val dao = FakeMosqueDao()
        dao.seed(
            config = MosqueConfigEntity(
                adzanLeadMinutes = 3,
                pinCode = "1234",
                connectivityMode = "SERVER",
                remoteTvIp = "",
                customBackgroundPath = "/tv/files/bg.jpg",
                murottalAudioPaths = "/tv/files/murottal.mp3",
                lastModified = SEEDED_AT
            ),
            announcements = listOf(
                AnnouncementEntity(id = "on", content = "tampil", isActive = true, position = 0, lastModified = SEEDED_AT),
                AnnouncementEntity(id = "off", content = "arsip", isActive = false, position = 1, lastModified = SEEDED_AT)
            ),
            media = listOf(
                MediaEntity(id = "m1", filePath = "/tv/files/slide.jpg", isVideo = false, isUsed = true, sortOrder = 0, lastModified = SEEDED_AT)
            ),
            slideshow = listOf(
                SlideshowScheduleEntity(id = "tv-s", startTime = "05:00", endTime = "06:00", day = "Monday", createdAt = 1, lastModified = SEEDED_AT)
            ),
            standby = listOf(
                StandbyScheduleEntity(id = "tv-b", startTime = "22:00", endTime = "04:00", day = "Monday", createdAt = 1, lastModified = SEEDED_AT)
            )
        )
        return dao to roomRepository(dao) { PUSHED_AT }
    }

    private val phoneAnnouncement =
        Announcement(id = "phone-a", content = "dari rumah", isActive = true, position = 0)

    @Test
    fun aTableNamedInThePushReplacesWhatWasHere() = runBlocking {
        val (dao, repository) = display()

        repository.applySnapshot(
            config = null,
            announcements = null,
            slideshowSchedules = listOf(
                SlideshowSchedule(id = "phone-s", startTime = "17:00", endTime = "18:00", day = "Friday")
            ),
            standbySchedules = null
        )

        // Replaced, not merged. An additive sync cannot carry a deletion, so a schedule the takmir
        // removed at home would come back from the display on the next sync.
        assertEquals(listOf("phone-s"), dao.slideshowRows.map { it.id })
    }

    @Test
    fun anEmptyListEmptiesTheTable() = runBlocking {
        val (dao, repository) = display()

        repository.applySnapshot(null, null, null, standbySchedules = emptyList())

        assertEquals(emptyList<String>(), dao.standbyRows.map { it.id })
    }

    @Test
    fun aTableLeftOutOfThePushIsUntouched() = runBlocking {
        val (dao, repository) = display()

        repository.applySnapshot(null, null, null, standbySchedules = emptyList())

        // Null and empty are the two answers that must never be confused. Standby was emptied
        // above; slideshow was not named, so it keeps both its row and its stamp.
        assertEquals(listOf("tv-s"), dao.slideshowRows.map { it.id })
        assertEquals(SEEDED_AT, dao.slideshowRows.single().lastModified)
    }

    @Test
    fun theMediaTableIsNeverReachableFromASnapshot() = runBlocking {
        val (dao, repository) = display()

        repository.applySnapshot(
            config = MosqueConfig(adzanLeadMinutes = 9),
            announcements = emptyList(),
            slideshowSchedules = emptyList(),
            standbySchedules = emptyList()
        )

        // There is no argument that could empty it, which is the point: those rows name files, and
        // a display holding rows for files it does not have shows "failed to load image".
        assertEquals(listOf("m1"), dao.mediaRows.map { it.id })
        assertEquals(SEEDED_AT, dao.mediaRows.single().lastModified)
    }

    @Test
    fun aSwitchedOffAnnouncementSurvivesAPushThatCannotSeeIt() = runBlocking {
        val (dao, repository) = display()

        repository.applySnapshot(
            config = null,
            announcements = listOf(phoneAnnouncement),
            slideshowSchedules = null,
            standbySchedules = null
        )

        // The state payload carries active announcements only, so a controller never learns the
        // archived one exists and cannot have meant to delete it.
        assertEquals(setOf("off", "phone-a"), dao.announcementRows.map { it.id }.toSet())
        assertEquals(false, dao.announcementRows.single { it.id == "off" }.isActive)
    }

    @Test
    fun theFourDeviceLocalConfigFieldsAreKept() = runBlocking {
        val (dao, repository) = display()

        repository.applySnapshot(
            config = MosqueConfig(
                adzanLeadMinutes = 9,
                pinCode = "",
                connectivityMode = "REMOTE",
                remoteTvIp = "192.168.1.77"
            ),
            announcements = null,
            slideshowSchedules = null,
            standbySchedules = null
        )

        val row = dao.configRow!!
        assertEquals("the setting itself must cross", 9, row.adzanLeadMinutes)
        // A snapshot never carries a PIN, so adopting its empty default would unlock the mosque's
        // own settings; and a display that took a controller's role would start posting to itself.
        assertEquals("1234", row.pinCode)
        assertEquals("SERVER", row.connectivityMode)
        assertEquals("", row.remoteTvIp)
        assertEquals("config", row.id)
    }

    @Test
    fun everyTableThePushNamedIsStampedAndNoOtherIs() = runBlocking {
        val (dao, repository) = display()

        repository.applySnapshot(
            config = MosqueConfig(adzanLeadMinutes = 9),
            announcements = listOf(phoneAnnouncement),
            slideshowSchedules = null,
            standbySchedules = null
        )

        val stamps = repository.tableTimestamps()
        assertEquals(PUSHED_AT, stamps["mosque_config"])
        assertEquals(PUSHED_AT, stamps["announcements"])
        // Absent, not stale: `table_stamps` holds a row only for a table that has been written
        // here, and the seed writes rows without going through a write. A stamp invented for a
        // table nothing touched is what makes the *next* sync offer to overwrite work this device
        // already took.
        assertNull(stamps["slideshow_schedules"])
        assertNull(stamps["standby_schedules"])
    }

    @Test
    fun anEmptyPushWritesNothingAtAll() = runBlocking {
        val (dao, repository) = display()
        val before = repository.tableTimestamps()

        repository.applySnapshot(null, null, null, null)

        assertEquals(before, repository.tableTimestamps())
        assertEquals(3, dao.configRow!!.adzanLeadMinutes)
        assertEquals(2, dao.announcementRows.size)
    }

    @Test
    fun theCachesTheDisplayReadsEverySecondMoveWithTheWrite() = runBlocking {
        val (_, repository) = display()

        repository.applySnapshot(
            config = MosqueConfig(adzanLeadMinutes = 9),
            announcements = listOf(phoneAnnouncement),
            slideshowSchedules = emptyList(),
            standbySchedules = null
        )

        // `AppViewModel`'s coordinator reads these synchronously once a second. A push that changed
        // the database and not the cache would leave the television on the old settings until
        // something else happened to refresh it.
        assertEquals(9, repository.getConfig().adzanLeadMinutes)
        assertEquals(listOf("phone-a"), repository.getActiveAnnouncements().map { it.id })
        assertTrue(repository.getSlideshowSchedules().isEmpty())
        assertNull(repository.getSlideshowSchedules().firstOrNull())
    }
}
