package com.groovernapp.mosqueathan.data.room

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A migrated display has to *say* it has data, not merely have it.
 *
 * A sync compares `table_stamps`, not the rows. The migration sets `lastModified` on every row it
 * copies, and for a while that was all it did — so a freshly upgraded display reported "never
 * written" for tables plainly full of announcements and schedules. A controller comparing notes
 * would have concluded the display had nothing and offered its own copy as the only one worth
 * keeping, which is the opposite of what the migration exists to protect.
 *
 * Invisible in the usual way: every row is present and correct, the display shows everything, and
 * the damage appears only at the first sync afterwards.
 *
 * `migrateAll` is a non-abstract interface method, so this exercises the real implementation
 * through the fake DAO rather than a copy of it.
 */
class MigrationStampsTablesTest {

    private val at = 1_700_000_000_000L

    @Test
    fun everyTableTheMigrationFillsIsStamped() {
        val dao = FakeMosqueDao()

        runBlocking {
            dao.migrateAll(
                config = MosqueConfigEntity(lastModified = at),
                announcements = listOf(AnnouncementEntity("a1", "satu", true, 0, at)),
                media = listOf(MediaEntity("m1", "/a.jpg", false, true, 0, at)),
                fridayMedia = listOf(FridayMediaEntity("f1", "/f.jpg", false, 0, at)),
                backgrounds = listOf(DashboardBackgroundEntity("b1", "/b.jpg", true, 0, at)),
                slideshow = listOf(
                    SlideshowScheduleEntity("s1", "08:00", "17:00", "Monday", "", 1, "Monday", false, at)
                ),
                standby = listOf(
                    StandbyScheduleEntity("t1", "22:00", "04:00", "Monday", "", 1, "Monday", false, at)
                ),
                kemenag = emptyList(),
                stampedAt = at
            )

            val stamps = dao.tableTimestamps().associate { it.tableName to it.lastModified }
            assertEquals(
                "a migrated display must report every table it actually holds",
                setOf(
                    "mosque_config", "announcements", "media", "friday_media",
                    "dashboard_backgrounds", "slideshow_schedules", "standby_schedules"
                ),
                stamps.keys
            )
            assertTrue(
                "the whole migration must read as one moment, not a spread of them",
                stamps.values.all { it == at }
            )
        }
    }

    @Test
    fun anEmptyTableIsNotStamped() {
        // "Nothing was migrated into this table" and "this table changed at the upgrade" are
        // different claims, and a sync treats them differently: a stamp on an empty table would
        // make a display look like it had deliberately emptied something.
        val dao = FakeMosqueDao()

        runBlocking {
            dao.migrateAll(
                config = MosqueConfigEntity(lastModified = at),
                announcements = emptyList(),
                media = emptyList(),
                fridayMedia = emptyList(),
                backgrounds = emptyList(),
                slideshow = emptyList(),
                standby = emptyList(),
                kemenag = emptyList(),
                stampedAt = at
            )

            val stamped = dao.tableTimestamps().map { it.tableName }.toSet()
            assertEquals(setOf("mosque_config"), stamped)
        }
    }

    @Test
    fun aMigrationWithNoConfigRowStampsNoConfig() {
        // The config row is also the already-migrated marker, so claiming it when it was not
        // written would make the next launch skip a migration that never happened.
        val dao = FakeMosqueDao()

        runBlocking {
            dao.migrateAll(
                config = null,
                announcements = listOf(AnnouncementEntity("a1", "satu", true, 0, at)),
                media = emptyList(),
                fridayMedia = emptyList(),
                backgrounds = emptyList(),
                slideshow = emptyList(),
                standby = emptyList(),
                kemenag = emptyList(),
                stampedAt = at
            )

            val stamped = dao.tableTimestamps().map { it.tableName }.toSet()
            assertEquals(setOf("announcements"), stamped)
            assertEquals(null, dao.getConfig())
        }
    }
}
