package com.groovernapp.mosqueathan.data.room

import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The rules the Realm repository grew the hard way, re-asserted against the Room one.
 *
 * ### Why these cannot be noticed at runtime
 *
 * Each of them is a rule about what happens to something *nobody is looking at*:
 *
 * - **The file follows the row.** A media file left behind after its row is deleted is invisible
 *   by construction: it is no longer in any list, so no screen shows it and no count changes. It
 *   accumulates in the display's private storage until the television runs out of space, months
 *   later. This is not hypothetical — it shipped, and it was found by measuring free space rather
 *   than by using the app. The rule lives in the repository precisely so that all four
 *   deletion paths inherit it instead of four panes each remembering.
 * - **A PIN change de-authorises the other devices.** If it did not, the takmir who set a PIN to
 *   lock a lost phone out would see the PIN prompt appear, be satisfied, and the lost phone would
 *   still be paired.
 * - **A reset writes a fresh row.** Field-by-field resets are how a field added later keeps its
 *   old value through a reset, and the user's only signal is a setting they explicitly cleared
 *   still being set.
 * - **Reordering renumbers.** Order is a column, not a list position, so a reorder that wrote
 *   nothing looks exactly like a reorder that worked until the next time the list is read from
 *   the database.
 *
 * The file rules use real files in a temporary folder, because "no longer referenced" is a
 * question about the filesystem and a fake would only be asserting itself.
 */
class RoomCarriedOverBehaviourTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private companion object {
        const val WRITE_AT = 7_777L
        const val STALE = 111L
    }

    private fun existingFile(name: String): File =
        temporaryFolder.newFile(name).apply { writeText("bytes of $name") }

    private fun media(id: String, path: String, order: Int = 0) =
        MediaEntity(id, path, false, true, order, STALE)

    // --- the file follows the row --------------------------------------------------------------

    @Test
    fun deletingTheLastRowThatNamesAFileDeletesTheFile() {
        val file = existingFile("slide.jpg")
        val dao = FakeMosqueDao().apply { seed(media = listOf(media("m1", file.path))) }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking { repository.deleteMediaItem("m1") }

        assertFalse("the file outlived its only row", file.exists())
        assertTrue(dao.mediaRows.isEmpty())
    }

    @Test
    fun deletingOneOfTwoRowsThatNameTheSameFileKeepsTheFile() {
        // The same file can be a slide and a Friday slide at once — "Use global slideshow" is
        // built on that. Deleting one row must not pull the file out from under the other, which
        // would leave a row pointing at nothing and a black frame in the slideshow.
        val file = existingFile("shared.jpg")
        val dao = FakeMosqueDao().apply {
            seed(media = listOf(media("m1", file.path, 0), media("m2", file.path, 1)))
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking { repository.deleteMediaItem("m1") }

        assertTrue("a file still named by another row was deleted", file.exists())
    }

    @Test
    fun everyTableIsConsultedBeforeAFileIsDeleted() {
        // Four tables can name a file, and a check that missed one would delete a file that is
        // still in use — the worse direction of this bug, and the one a leak test cannot see.
        val holders = listOf<Pair<String, (String) -> FakeMosqueDao>>(
            "friday media" to { path ->
                FakeMosqueDao().apply {
                    seed(
                        media = listOf(media("m1", path)),
                        fridayMedia = listOf(FridayMediaEntity("f1", path, false, 0, STALE))
                    )
                }
            },
            "a dashboard background" to { path ->
                FakeMosqueDao().apply {
                    seed(
                        media = listOf(media("m1", path)),
                        backgrounds = listOf(DashboardBackgroundEntity("b1", path, true, 0, STALE))
                    )
                }
            },
            "the custom background" to { path ->
                FakeMosqueDao().apply {
                    seed(
                        config = MosqueConfigEntity(customBackgroundPath = path, lastModified = STALE),
                        media = listOf(media("m1", path))
                    )
                }
            }
        )

        holders.forEachIndexed { index, (what, build) ->
            val file = existingFile("held-$index.jpg")
            val repository = roomRepository(build(file.path)) { WRITE_AT }
            runBlocking { repository.deleteMediaItem("m1") }
            assertTrue("a file still named by $what was deleted", file.exists())
        }
    }

    @Test
    fun theRuleReachesEveryDeletionPath() {
        // The Realm implementation's actual defect was not the rule, it was that only one of the
        // four callers applied it. Asserting the rule once per path is what would have caught it.
        val slide = existingFile("path-slide.jpg")
        val friday = existingFile("path-friday.jpg")
        val background = existingFile("path-background.jpg")
        val dao = FakeMosqueDao().apply {
            seed(
                media = listOf(media("m1", slide.path)),
                fridayMedia = listOf(FridayMediaEntity("f1", friday.path, false, 0, STALE)),
                backgrounds = listOf(DashboardBackgroundEntity("b1", background.path, true, 0, STALE))
            )
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking {
            repository.deleteMediaItem("m1")
            repository.deleteFridayMedia("f1")
            repository.deleteDashboardBackground("b1")
        }

        assertFalse("deleteMediaItem left its file", slide.exists())
        assertFalse("deleteFridayMedia left its file", friday.exists())
        assertFalse("deleteDashboardBackground left its file", background.exists())
    }

    @Test
    fun replacingTheCustomBackgroundDeletesTheOneItReplaced() {
        val previous = existingFile("previous-bg.jpg")
        val chosen = existingFile("chosen-bg.jpg")
        val dao = FakeMosqueDao().apply {
            seed(config = MosqueConfigEntity(customBackgroundPath = previous.path, lastModified = STALE))
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking { repository.updateCustomBackground(chosen.path) }

        assertFalse("the replaced background was left on disk", previous.exists())
        assertTrue("the chosen background was deleted", chosen.exists())
        assertEquals(chosen.path, dao.configRow?.customBackgroundPath)
    }

    @Test
    fun choosingTheSameCustomBackgroundAgainDeletesNothing() {
        /*
         * Two independent things protect this, which is worth knowing because the obvious one is
         * not the one doing the work. `updateCustomBackground` guards with `previous != path`, but
         * removing that guard does not break this test: the reference check runs *after* the row
         * is written, so the config row still names the file and it is spared anyway. Measured by
         * mutation, not assumed — the guard is a redundant second line, and a reviewer who deleted
         * it as dead code would be right about the behaviour and wrong about the intent.
         */
        val chosen = existingFile("same-bg.jpg")
        val dao = FakeMosqueDao().apply {
            seed(config = MosqueConfigEntity(customBackgroundPath = chosen.path, lastModified = STALE))
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking { repository.updateCustomBackground(chosen.path) }

        assertTrue("re-selecting the current background deleted it", chosen.exists())
    }

    @Test
    fun replacingACustomBackgroundThatIsAlsoASlideKeepsTheFile() {
        val shared = existingFile("shared-bg.jpg")
        val chosen = existingFile("new-bg.jpg")
        val dao = FakeMosqueDao().apply {
            seed(
                config = MosqueConfigEntity(customBackgroundPath = shared.path, lastModified = STALE),
                media = listOf(media("m1", shared.path))
            )
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking { repository.updateCustomBackground(chosen.path) }

        assertTrue("a file still used as a slide was deleted", shared.exists())
    }

    @Test
    fun aMissingFileAndAnEmptyPathAreNotFailures() {
        // Both are ordinary: a row can outlive its file (storage cleared, a restore), and an empty
        // path is what "no custom background" is stored as. An exception here would abort a
        // deletion the user asked for and leave the row in place.
        val dao = FakeMosqueDao().apply {
            seed(
                media = listOf(
                    media("m1", "/nonexistent/gone.jpg", 0),
                    media("m2", "", 1)
                )
            )
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking {
            repository.deleteMediaItem("m1")
            repository.deleteMediaItem("m2")
        }

        assertTrue(dao.mediaRows.isEmpty())
    }

    // --- the PIN -------------------------------------------------------------------------------

    @Test
    fun settingAPinDeAuthorisesEveryDeviceExceptTheOneThatSetIt() {
        val dao = FakeMosqueDao().apply {
            seed(
                config = MosqueConfigEntity(lastModified = STALE),
                // No master among them: whether a master should be spared is disputed between
                // the two implementations, and it is pinned in exactly one place
                // ([theMasterDeviceIsNotSpared]) so that settling it touches one test.
                devices = listOf(
                    ConnectedDeviceEntity("changer", "hp takmir", false, true, 1, 3, false, STALE),
                    ConnectedDeviceEntity("other", "hp lain", false, true, 1, 2, false, STALE),
                    ConnectedDeviceEntity("third", "hp ketiga", false, true, 1, 1, false, STALE)
                )
            )
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking { repository.updatePinCode("1234", "changer") }

        val allowed = dao.deviceRows.filter { it.isAllowed }.map { it.deviceId }
        assertEquals(listOf("changer"), allowed)
        assertEquals("1234", dao.configRow?.pinCode)
    }

    @Test
    fun clearingThePinLetsEveryDeviceBackIn() {
        val dao = FakeMosqueDao().apply {
            seed(
                config = MosqueConfigEntity(pinCode = "1234", lastModified = STALE),
                devices = listOf(
                    ConnectedDeviceEntity("a", "satu", false, false, 1, 2, false, STALE),
                    ConnectedDeviceEntity("b", "dua", false, false, 1, 1, false, STALE)
                )
            )
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking { repository.updatePinCode("", null) }

        assertTrue("clearing the PIN left a device locked out", dao.deviceRows.all { it.isAllowed })
        assertEquals("", dao.configRow?.pinCode)
    }

    @Test
    fun aPinChangeWithNoChangerDeAuthorisesEverything() {
        // `changerDeviceId` is nullable, and null is what the display itself passes when a takmir
        // sets the PIN standing at the television. Every *remote* device should then be locked
        // out — the display is not one of them.
        val dao = FakeMosqueDao().apply {
            seed(
                config = MosqueConfigEntity(lastModified = STALE),
                devices = listOf(ConnectedDeviceEntity("a", "satu", false, true, 1, 1, false, STALE))
            )
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking { repository.updatePinCode("9999", null) }

        assertTrue(dao.deviceRows.none { it.isAllowed })
    }

    @Test
    fun theMasterDeviceIsSpared() {
        /*
         * Settled in favour of sparing it, matching `MosqueConfigRepository`. The consequence of
         * the other answer is concrete: a takmir sets a PIN from a phone and locks the mosque's
         * own paired tablet out of its own display.
         */
        val dao = FakeMosqueDao().apply {
            seed(
                config = MosqueConfigEntity(lastModified = STALE),
                devices = listOf(
                    ConnectedDeviceEntity("changer", "hp", false, true, 1, 2, false, STALE),
                    ConnectedDeviceEntity("master", "tablet", true, true, 1, 1, false, STALE)
                )
            )
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking { repository.updatePinCode("1234", "changer") }

        assertTrue(
            "the master was de-authorised; that locks the mosque's own tablet out of its display",
            dao.deviceRows.first { it.deviceId == "master" }.isAllowed
        )
    }

    @Test
    fun setDeviceMasterLeavesExactlyOneMaster() {
        val dao = FakeMosqueDao().apply {
            seed(
                devices = listOf(
                    ConnectedDeviceEntity("a", "satu", true, true, 1, 3, false, STALE),
                    ConnectedDeviceEntity("b", "dua", false, true, 1, 2, false, STALE),
                    ConnectedDeviceEntity("c", "tiga", true, true, 1, 1, false, STALE)
                )
            )
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking { repository.setDeviceMaster("b") }

        assertEquals(listOf("b"), dao.deviceRows.filter { it.isMaster }.map { it.deviceId })
    }

    // --- reset ---------------------------------------------------------------------------------

    @Test
    fun resetToDefaultsEmptiesTheTablesAndWritesAWholeFreshConfigRow() {
        val dao = FakeMosqueDao().apply {
            seed(
                config = MosqueConfigEntity(
                    latitude = 1.0,
                    pinCode = "1234",
                    jumatEnabled = true,
                    murottalAudioPaths = "/nonexistent/a.mp3",
                    lastModified = STALE
                ),
                announcements = listOf(AnnouncementEntity("a1", "satu", true, 0, STALE)),
                media = listOf(media("m1", "/nonexistent/m1.jpg")),
                fridayMedia = listOf(FridayMediaEntity("f1", "/nonexistent/f1.jpg", false, 0, STALE)),
                backgrounds = listOf(DashboardBackgroundEntity("b1", "/nonexistent/b1.jpg", true, 0, STALE)),
                slideshow = listOf(
                    SlideshowScheduleEntity("s1", "08:00", "09:00", "MONDAY", "", 1, "MONDAY", false, STALE)
                ),
                standby = listOf(
                    StandbyScheduleEntity("t1", "22:00", "04:00", "MONDAY", "", 1, "MONDAY", false, STALE)
                ),
                kemenag = listOf(KemenagScheduleEntity(dateString = "2026-01-01", cityId = "jakarta"))
            )
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking { repository.resetToDefaults() }

        // Whole-row equality rather than field by field: that is the property that makes a
        // config field added later reset too, and it is the only assertion that cannot rot.
        assertEquals(MosqueConfigEntity(lastModified = WRITE_AT), dao.configRow)
        assertEquals(
            mapOf(
                // One, not zero: the seeded default.
                "announcements" to 1,
                "media" to 0,
                "friday_media" to 0,
                "dashboard_backgrounds" to 0,
                "slideshow_schedules" to 0,
                "standby_schedules" to 0,
                "kemenag_schedules" to 0
            ),
            dao.rowCounts().filterKeys { it != "mosque_config" && it != "connected_devices" }
        )
    }

    @Test
    fun resetToDefaultsClearsPairedDevicesAndSeedsTheDefaultAnnouncement() {
        /*
         * Recorded, not endorsed — two divergences from `MosqueConfigRepository`, which clears
         * `connected_devices` and then writes three default announcements. Both are reported
         * rather than fixed here. Pinned because each is invisible in the opposite direction:
         * after a reset the running text is empty rather than showing the three defaults, and
         * every previously paired phone is still paired against a PIN that is now blank.
         */
        val dao = FakeMosqueDao().apply {
            seed(
                config = MosqueConfigEntity(lastModified = STALE),
                announcements = listOf(AnnouncementEntity("a1", "satu", true, 0, STALE)),
                devices = listOf(ConnectedDeviceEntity("a", "hp", false, true, 1, 1, false, STALE))
            )
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking { repository.resetToDefaults() }

        // A reset clears the PIN, so leaving a paired phone approved against a blank PIN is the
        // opposite of what a reset is for; and a television with an empty running-text bar looks
        // broken rather than reset, which is why one default announcement is seeded.
        assertTrue("a reset left a device paired", dao.deviceRows.isEmpty())
        assertEquals(listOf("default_1"), dao.announcementRows.map { it.id })
    }

    // --- ordering ------------------------------------------------------------------------------

    @Test
    fun everyOrderingWriteRenumbersFromTheGivenOrder() {
        val dao = FakeMosqueDao().apply {
            seed(
                announcements = listOf(
                    AnnouncementEntity("a1", "satu", true, 0, STALE),
                    AnnouncementEntity("a2", "dua", true, 1, STALE),
                    AnnouncementEntity("a3", "tiga", true, 2, STALE)
                ),
                media = listOf(media("m1", "/nonexistent/1.jpg", 0), media("m2", "/nonexistent/2.jpg", 1)),
                fridayMedia = listOf(
                    FridayMediaEntity("f1", "/nonexistent/f1.jpg", false, 0, STALE),
                    FridayMediaEntity("f2", "/nonexistent/f2.jpg", false, 1, STALE)
                ),
                backgrounds = listOf(
                    DashboardBackgroundEntity("b1", "/nonexistent/b1.jpg", true, 0, STALE),
                    DashboardBackgroundEntity("b2", "/nonexistent/b2.jpg", true, 1, STALE)
                )
            )
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking {
            repository.updateAnnouncementsOrder(listOf("a3", "a1", "a2"))
            repository.updateMediaListOrder(listOf("m2", "m1"))
            repository.updateFridayMediaOrder(listOf("f2", "f1"))
            repository.updateDashboardBackgroundOrder(listOf("b2", "b1"))
        }

        assertEquals(listOf("a3", "a1", "a2"), dao.announcementRows.map { it.id })
        assertEquals(listOf(0, 1, 2), dao.announcementRows.map { it.position })
        assertEquals(listOf("m2", "m1"), dao.mediaRows.map { it.id })
        assertEquals(listOf(0, 1), dao.mediaRows.map { it.sortOrder })
        assertEquals(listOf("f2", "f1"), dao.fridayMediaRows.map { it.id })
        assertEquals(listOf("b2", "b1"), dao.backgroundRows.map { it.id })
        // The rows are otherwise untouched: a reorder that rewrote a path or a flag would be a
        // data-loss bug wearing a reorder's clothes.
        assertEquals(listOf("dua", "satu", "tiga"), dao.announcementRows.map { it.content }.sorted())
    }

    @Test
    fun anOrderingWriteIgnoresAnIdItDoesNotKnow() {
        // Not hypothetical: a controller reorders a list it fetched before the display deleted a
        // slide, so the ids it sends can include one that no longer exists. Inserting a row for
        // it would put an empty slide in the slideshow.
        val dao = FakeMosqueDao().apply {
            seed(media = listOf(media("m1", "/nonexistent/1.jpg", 0), media("m2", "/nonexistent/2.jpg", 1)))
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking { repository.updateMediaListOrder(listOf("gone", "m2", "m1")) }

        assertEquals(2, dao.mediaRows.size)
        assertEquals(listOf("m2", "m1"), dao.mediaRows.map { it.id })
        // Note the unknown id still consumes an index, so the numbering starts at 1 rather than
        // 0. Harmless — the column is only ever read as a relative order — and pinned so that a
        // future change to it is a decision rather than a surprise.
        assertEquals(listOf(1, 2), dao.mediaRows.map { it.sortOrder })
    }

    @Test
    fun addingToAListPutsItLast() {
        val dao = FakeMosqueDao().apply {
            seed(
                announcements = listOf(AnnouncementEntity("a1", "satu", true, 4, STALE)),
                media = listOf(media("m1", "/nonexistent/1.jpg", 9))
            )
        }
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking {
            repository.addAnnouncement("dua", "a2")
            repository.addMediaItem("/nonexistent/2.jpg", isVideo = false)
        }

        assertEquals(5, dao.announcementRows.first { it.id == "a2" }.position)
        assertEquals(10, dao.mediaRows.first { it.id != "m1" }.sortOrder)
    }

    // --- one divergence worth pinning ----------------------------------------------------------

    @Test
    fun theActiveAnnouncementsFlowCarriesOnlyTheActiveOnes() {
        /*
         * This began as a recorded divergence: the implementation served the whole table where
         * `MosqueConfigRepository` queries `isActive == true`. It has since been fixed, and the
         * test flipped with it.
         *
         * The failure it guards is invisible in the obvious way: the dashboard's running text
         * would show an announcement a takmir had switched off, and the switch in settings would
         * appear to do nothing while having saved perfectly. Nothing throws, and the text on
         * screen is a real announcement.
         */
        val dao = FakeMosqueDao().apply {
            seed(
                announcements = listOf(
                    AnnouncementEntity("a1", "aktif", true, 0, STALE),
                    AnnouncementEntity("a2", "dimatikan", false, 1, STALE)
                )
            )
        }
        val repository = roomRepository(dao) { WRITE_AT }

        val served = runBlocking { repository.getActiveAnnouncementsFlow().first() }

        assertEquals(
            "a switched-off announcement reached the running text",
            listOf("a1"), served.map { it.id }
        )
    }
}
