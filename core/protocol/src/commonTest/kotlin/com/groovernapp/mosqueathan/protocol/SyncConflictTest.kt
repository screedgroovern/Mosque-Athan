package com.groovernapp.mosqueathan.protocol

import com.groovernapp.mosqueathan.protocol.SyncConflict.Choice
import com.groovernapp.mosqueathan.protocol.SyncConflict.Side
import com.groovernapp.mosqueathan.protocol.SyncConflict.Stamps
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The sync decision, tested directly, because the alternative is testing it by losing data.
 *
 * Every case here is a way a takmir's work disappears if the rule is wrong, and none of them is
 * observable from outside a real sync between two real devices.
 */
class SyncConflictTest {

    private fun stamps(vararg pairs: Pair<String, Long>) = Stamps(pairs.toMap())

    @Test
    fun takeServerGivesEveryTableToTheServer() {
        val r = SyncConflict.resolve(
            Choice.TakeServer,
            server = stamps("mosque_config" to 1L),
            local = stamps("announcements" to 999L)
        )
        assertTrue(
            SyncConflict.COMPARED_TABLES.all { r.sideFor(it) == Side.Server },
            "a wholesale choice must not be quietly overridden by a timestamp"
        )
    }

    @Test
    fun takeLocalGivesEveryTableToLocal() {
        val r = SyncConflict.resolve(
            Choice.TakeLocal,
            server = stamps("mosque_config" to 999L),
            local = stamps("mosque_config" to 1L)
        )
        assertTrue(
            SyncConflict.COMPARED_TABLES.all { r.sideFor(it) == Side.Local },
            "picking local must keep local even where the server is newer"
        )
    }

    @Test
    fun newestWinsResolvesEachTableSeparately() {
        // The case the whole per-table design exists for: a takmir edits the Friday schedule at
        // home while someone at the mosque adds a running text. Either wholesale choice throws one
        // of them away.
        val r = SyncConflict.resolve(
            Choice.NewestWins,
            server = stamps("announcements" to 200L, "friday_media" to 50L),
            local = stamps("announcements" to 100L, "friday_media" to 300L)
        )
        assertEquals(Side.Server, r.sideFor("announcements"))
        assertEquals(Side.Local, r.sideFor("friday_media"))
    }

    @Test
    fun aTieGoesToTheServer() {
        val r = SyncConflict.resolve(
            Choice.NewestWins,
            server = stamps("media" to 500L),
            local = stamps("media" to 500L)
        )
        assertEquals(
            Side.Server, r.sideFor("media"),
            "the display is what the mosque sees, and equal stamps usually mean neither side moved"
        )
    }

    @Test
    fun aTableNeitherSideHasWrittenGoesToTheServer() {
        val r = SyncConflict.resolve(Choice.NewestWins, server = Stamps(), local = Stamps())
        assertTrue(SyncConflict.COMPARED_TABLES.all { r.sideFor(it) == Side.Server })
    }

    @Test
    fun everyComparedTableIsResolved() {
        // A table missing from the result would silently fall through to the default, which is the
        // kind of gap that only shows up as one pane of settings not syncing.
        val r = SyncConflict.resolve(Choice.NewestWins, Stamps(), Stamps())
        assertEquals(SyncConflict.COMPARED_TABLES.toSet(), r.byTable.keys)
    }

    @Test
    fun devicesAndKemenagAreNotCompared() {
        assertFalse("connected_devices" in SyncConflict.COMPARED_TABLES)
        assertFalse("kemenag_schedules" in SyncConflict.COMPARED_TABLES)
    }

    @Test
    fun differingTablesNamesOnlyWhatDiffers() {
        val differing = SyncConflict.differingTables(
            server = stamps("media" to 5L, "announcements" to 7L),
            local = stamps("media" to 5L, "announcements" to 9L)
        )
        assertEquals(listOf("announcements"), differing)
    }

    @Test
    fun aFreshControllerPairingIsNotAConflict() {
        // The commonest first sync there is, and asking here is how a dialog earns the reflex that
        // dismisses it later on the one occasion it mattered.
        assertFalse(
            SyncConflict.needsAsking(
                server = stamps("mosque_config" to 100L, "announcements" to 100L),
                local = Stamps()
            )
        )
    }

    @Test
    fun anEstablishedDisplayMeetingAnEstablishedControllerIsAConflict() {
        assertTrue(
            SyncConflict.needsAsking(
                server = stamps("mosque_config" to 100L),
                local = stamps("announcements" to 100L)
            )
        )
    }

    @Test
    fun identicalSidesAreNeverAConflict() {
        val same = stamps("mosque_config" to 42L, "media" to 7L)
        assertFalse(SyncConflict.needsAsking(server = same, local = same))
    }
}
