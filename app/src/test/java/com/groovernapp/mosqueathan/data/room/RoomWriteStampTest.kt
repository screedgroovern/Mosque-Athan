package com.groovernapp.mosqueathan.data.room

import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.domain.MosqueConfig
import kotlin.reflect.full.memberFunctions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every write stamps `lastModified`, and a member added later without a stamp fails here.
 *
 * ### Why this cannot be noticed at runtime
 *
 * `lastModified` is never displayed. Nothing reads it except the sync comparison, and that
 * comparison's answer is a *dialog* — so a write that forgot its stamp does not throw, does not
 * log, and does not change a pixel on the display. It changes what the display and a takmir's
 * phone believe about each other later, when one of them is offline and both have edited. The
 * failure is "the phone's Friday schedule silently lost", days after the write, in a code path
 * that looks correct because the value it compared was a perfectly plausible number.
 *
 * So the guard cannot be behavioural, and it cannot be per-field either: it has to be over the
 * *interface*, because the thing that will go wrong is a 60th member added beside the 59 and
 * written the way the DAO invites — `upsertMedia(listOf(row.copy(isUsed = x)))`, which compiles,
 * works, and carries whatever stamp the row already had.
 *
 * ### How
 *
 * Every seeded row starts at [STALE] and the clock is pinned to [WRITE_AT], so each case asserts
 * two things at once: some row now carries [WRITE_AT] (the write was stamped), and no row carries
 * anything else (nothing was written unstamped, which would read as `0`, and nothing was written
 * with a stamp the caller invented). [everyMutatingMemberIsExercisedOrWithheldWithAReason] is what
 * makes the pair total over the interface rather than over the list someone remembered.
 */
class RoomWriteStampTest {

    private companion object {
        /** What every seeded row carries. Any value except [WRITE_AT] and `0` would do. */
        const val STALE = 111L

        /** What the pinned clock returns, so a stamp is distinguishable from a survival. */
        const val WRITE_AT = 999L

        val OBJECT_METHODS = setOf("equals", "hashCode", "toString")
    }

    /**
     * One member of [ConfigRepository], exercised against a freshly seeded DAO.
     *
     * [mustMove] names the rows this particular write is required to stamp, and it exists because
     * "some row carries the clock's value" is too weak for a write that touches two tables:
     * `updatePinCode` writes the config row *and* every device, so the config stamp alone
     * satisfies the general assertion while the devices keep a stale one. Found by mutation, not
     * by reading — dropping the device stamp survived the first version of this test.
     */
    private class Case(
        val member: String,
        val what: String,
        val mustMove: ((FakeMosqueDao) -> List<Long>)? = null,
        val exercise: suspend (RoomConfigRepository) -> Unit
    )

    /**
     * Members that write nothing a stamp could land on, each with the reason.
     *
     * The deletes are not an oversight: the row is gone, so there is nothing left to carry a
     * timestamp. That does have a consequence worth knowing — `tableTimestamps()` takes
     * `MAX(lastModified)`, so a table whose newest row was deleted reports the same value as
     * before and a sync cannot see the deletion. That is a design question for the sync, not a
     * missing stamp here, and it is recorded rather than asserted.
     */
    private val withheld = mapOf(
        "clearAnnouncements" to "removes every row; nothing remains to stamp",
        "deleteAnnouncement" to "removes the row; nothing remains to stamp",
        "deleteMediaItem" to "removes the row; nothing remains to stamp",
        "deleteFridayMedia" to "removes the row; nothing remains to stamp",
        "deleteDashboardBackground" to "removes the row; nothing remains to stamp",
        "deleteSlideshowSchedule" to "removes the row; nothing remains to stamp",
        "deleteStandbySchedule" to "removes the row; nothing remains to stamp",
        "deleteDevice" to "removes the row; nothing remains to stamp",
        "triggerKemenagFetchForDuration" to
            "stores only what an HTTP fetch returned, and KemenagFetcher is a concrete class " +
            "with no seam, so this path cannot be reached from a unit test without a network",
        "tableTimestamps" to
            "a read, not a write — it is suspend only because Room's queries are, and this " +
            "class's heuristic reads suspend as mutating. It is what the stamps are *for*, and " +
            "the guard that it stays truthful is SyncTimestampCoverageTest"
    )

    private val cases = listOf(
        Case("resetToDefaults", "resetToDefaults()") { it.resetToDefaults() },
        Case("updateConfig", "updateConfig(config)") {
            it.updateConfig(MosqueConfig(adzanLeadMinutes = 7))
        },
        Case("updateConfig", "updateConfig(field by field)") {
            it.updateConfig(
                latitude = -6.2,
                longitude = 106.8,
                iqomahSubuh = 1,
                iqomahDzuhur = 2,
                iqomahAshar = 3,
                iqomahMaghrib = 4,
                iqomahIsya = 5,
                calculationMethodName = "EGYPTIAN",
                adzanLeadMinutes = 6
            )
        },
        Case("updateConnectivityMode", "updateConnectivityMode()") {
            it.updateConnectivityMode("REMOTE")
        },
        Case("updateRemoteTvIp", "updateRemoteTvIp()") { it.updateRemoteTvIp("192.168.1.9") },
        Case("updateShowSettingsOnTv", "updateShowSettingsOnTv()") {
            it.updateShowSettingsOnTv(true)
        },
        Case("updateStandbyEnabled", "updateStandbyEnabled()") { it.updateStandbyEnabled(true) },
        Case("updateStandbySettings", "updateStandbySettings()") {
            it.updateStandbySettings(true, "22:00", "04:00")
        },
        Case("updateThemeSettings", "updateThemeSettings()") {
            it.updateThemeSettings("SLIDE", 5, videoWaitUntilEnd = true, includeDefaultImage = false)
        },
        Case("updateSlideshowEnabled", "updateSlideshowEnabled()") {
            it.updateSlideshowEnabled(true)
        },
        // A path that does not exist on disk, so no file is deleted and this stays a pure
        // database assertion; deleteFileIfNoLongerReferenced has its own tests.
        Case("updateCustomBackground", "updateCustomBackground()") {
            it.updateCustomBackground("/nonexistent/background.jpg")
        },
        Case(
            "updatePinCode",
            "updatePinCode()",
            mustMove = { dao -> dao.deviceRows.map { device -> device.lastModified } }
        ) { it.updatePinCode("1234", "d1") },
        Case("addAnnouncement", "addAnnouncement()") { it.addAnnouncement("halo", "a3") },
        Case("updateAnnouncement", "updateAnnouncement()") {
            it.updateAnnouncement("a1", "diubah", isActive = false)
        },
        Case(
            "updateAnnouncementsOrder",
            "updateAnnouncementsOrder()",
            mustMove = { dao -> dao.announcementRows.map { row -> row.lastModified } }
        ) { it.updateAnnouncementsOrder(listOf("a2", "a1")) },
        Case("addMediaItem", "addMediaItem()") {
            it.addMediaItem("/nonexistent/slide.jpg", isVideo = false)
        },
        Case("updateMediaItemPath", "updateMediaItemPath()") {
            it.updateMediaItemPath("m1", "/nonexistent/other.jpg")
        },
        Case("updateMediaItemUsed", "updateMediaItemUsed()") {
            it.updateMediaItemUsed("m1", isUsed = false)
        },
        Case(
            "updateMediaListOrder",
            "updateMediaListOrder()",
            mustMove = { dao -> dao.mediaRows.map { row -> row.lastModified } }
        ) { it.updateMediaListOrder(listOf("m2", "m1")) },
        Case("addFridayMedia", "addFridayMedia()") {
            it.addFridayMedia("/nonexistent/friday.mp4", isVideo = true)
        },
        Case("updateFridayMediaItemPath", "updateFridayMediaItemPath()") {
            it.updateFridayMediaItemPath("f1", "/nonexistent/friday2.mp4")
        },
        Case(
            "updateFridayMediaOrder",
            "updateFridayMediaOrder()",
            mustMove = { dao -> dao.fridayMediaRows.map { row -> row.lastModified } }
        ) { it.updateFridayMediaOrder(listOf("f2", "f1")) },
        Case("addDashboardBackground", "addDashboardBackground()") {
            it.addDashboardBackground("/nonexistent/bg.jpg", "b3")
        },
        Case("updateDashboardBackgroundUsed", "updateDashboardBackgroundUsed()") {
            it.updateDashboardBackgroundUsed("b1", isUsed = false)
        },
        Case(
            "updateDashboardBackgroundOrder",
            "updateDashboardBackgroundOrder()",
            mustMove = { dao -> dao.backgroundRows.map { row -> row.lastModified } }
        ) { it.updateDashboardBackgroundOrder(listOf("b2", "b1")) },
        Case("addSlideshowSchedule", "addSlideshowSchedule()") {
            it.addSlideshowSchedule("MONDAY", false, "10:00", "11:00", "s3")
        },
        Case("updateSlideshowScheduleTime", "updateSlideshowScheduleTime()") {
            it.updateSlideshowScheduleTime("s1", "10:00", "11:00")
        },
        Case("updateSlideshowScheduleDays", "updateSlideshowScheduleDays()") {
            it.updateSlideshowScheduleDays("s1", "MONDAY,FRIDAY")
        },
        Case("updateSlideshowScheduleDates", "updateSlideshowScheduleDates()") {
            it.updateSlideshowScheduleDates("s1", "2026-01-01")
        },
        Case("addStandbySchedule", "addStandbySchedule()") {
            it.addStandbySchedule("2026-01-01", true, "22:00", "04:00", "t3")
        },
        Case("updateStandbyScheduleTime", "updateStandbyScheduleTime()") {
            it.updateStandbyScheduleTime("t1", "23:00", "05:00")
        },
        Case("updateStandbyScheduleDays", "updateStandbyScheduleDays()") {
            it.updateStandbyScheduleDays("t1", "SATURDAY")
        },
        Case("updateStandbyScheduleDates", "updateStandbyScheduleDates()") {
            it.updateStandbyScheduleDates("t1", "2026-03-03")
        },
        Case(
            "setDeviceMaster",
            "setDeviceMaster()",
            mustMove = { dao -> dao.deviceRows.map { device -> device.lastModified } }
        ) { it.setDeviceMaster("d2") }
    )

    /** Two rows in every table, all of them stale, so any of them could be the one that moves. */
    private fun seededDao(): FakeMosqueDao = FakeMosqueDao().apply {
        seed(
            config = MosqueConfigEntity(lastModified = STALE),
            announcements = listOf(
                AnnouncementEntity("a1", "satu", true, 0, STALE),
                AnnouncementEntity("a2", "dua", true, 1, STALE)
            ),
            media = listOf(
                MediaEntity("m1", "/nonexistent/m1.jpg", false, true, 0, STALE),
                MediaEntity("m2", "/nonexistent/m2.jpg", false, true, 1, STALE)
            ),
            fridayMedia = listOf(
                FridayMediaEntity("f1", "/nonexistent/f1.jpg", false, 0, STALE),
                FridayMediaEntity("f2", "/nonexistent/f2.jpg", false, 1, STALE)
            ),
            backgrounds = listOf(
                DashboardBackgroundEntity("b1", "/nonexistent/b1.jpg", true, 0, STALE),
                DashboardBackgroundEntity("b2", "/nonexistent/b2.jpg", true, 1, STALE)
            ),
            slideshow = listOf(
                SlideshowScheduleEntity("s1", "08:00", "09:00", "MONDAY", "", 1, "MONDAY", false, STALE),
                SlideshowScheduleEntity("s2", "09:00", "10:00", "TUESDAY", "", 2, "TUESDAY", false, STALE)
            ),
            standby = listOf(
                StandbyScheduleEntity("t1", "22:00", "04:00", "MONDAY", "", 1, "MONDAY", false, STALE),
                StandbyScheduleEntity("t2", "23:00", "05:00", "TUESDAY", "", 2, "TUESDAY", false, STALE)
            ),
            devices = listOf(
                ConnectedDeviceEntity("d1", "hp takmir", false, true, 1, 2, false, STALE),
                ConnectedDeviceEntity("d2", "tablet", true, true, 1, 1, false, STALE)
            ),
            kemenag = listOf(
                KemenagScheduleEntity(
                    dateString = "2026-01-01", cityId = "jakarta", imsak = "04:20",
                    subuh = "04:30", terbit = "05:45", dhuha = "06:10", dzuhur = "12:00",
                    ashar = "15:15", maghrib = "18:00", isya = "19:10", lastModified = STALE
                )
            )
        )
    }

    /** Mutating members of the interface: everything that is `suspend`, plus the fire-and-forget
     *  fetch trigger. Reads are the non-suspend `get*`/`observe*` family and nothing else. */
    private val mutatingMembers: Set<String> by lazy {
        ConfigRepository::class.memberFunctions
            .filterNot { it.name in OBJECT_METHODS }
            .filter { it.isSuspend || !(it.name.startsWith("get") || it.name.startsWith("observe")) }
            .map { it.name }
            .toSet()
    }

    // --- vacuous-pass guards -------------------------------------------------------------------

    @Test
    fun reflectionSeesTheInterfaceAndSeparatesReadsFromWrites() {
        // Without this, a change that made `memberFunctions` return nothing — or that made every
        // member look like a read — would leave the coverage test comparing two empty sets and
        // passing while nothing at all was checked.
        assertTrue(
            "no mutating members found on ConfigRepository; the classification below is broken",
            mutatingMembers.size >= 40
        )
        listOf("getConfig", "getMediaList", "getIqomahMinutes", "observeFridayMedia").forEach {
            assertTrue("$it was classified as a write", it !in mutatingMembers)
        }
        listOf("updateConfig", "addMediaItem", "resetToDefaults", "updatePinCode").forEach {
            assertTrue("$it was classified as a read", it in mutatingMembers)
        }
    }

    @Test
    fun everyExercisedNameIsRealMemberOfTheInterface() {
        // A typo in a case's `member` would leave the real member uncovered *and* satisfy nothing,
        // which the coverage test would report as a missing member — but only if the typo is not
        // itself in `withheld`. Naming it separately makes the cause legible.
        val unknown = cases.map { it.member }.toSet() - mutatingMembers
        assertTrue("cases name members ConfigRepository does not have: $unknown", unknown.isEmpty())
    }

    @Test
    fun theWithheldListDoesNotNameAMemberThatNoLongerExists() {
        val stale = withheld.keys - mutatingMembers
        assertTrue("`withheld` names members that no longer exist: $stale", stale.isEmpty())
    }

    @Test
    fun noWithheldMemberIsAlsoExercised() {
        val both = withheld.keys.intersect(cases.map { it.member }.toSet())
        assertTrue("withheld and exercised at the same time: $both", both.isEmpty())
    }

    // --- the guards ----------------------------------------------------------------------------

    @Test
    fun everyMutatingMemberIsExercisedOrWithheldWithAReason() {
        val covered = cases.map { it.member }.toSet()
        val missing = (mutatingMembers - covered - withheld.keys).sorted()
        assertTrue(
            "these writes are not checked for a lastModified stamp: $missing\n" +
                "Add a Case for each, or withhold it with a reason. A write that forgets the " +
                "stamp is invisible until a sync compares the wrong number.",
            missing.isEmpty()
        )
    }

    @Test
    fun everyWriteStampsTheRowItTouched() {
        val failures = cases.mapNotNull { case ->
            val dao = seededDao()
            val repository = roomRepository(dao) { WRITE_AT }
            runBlocking { case.exercise(repository) }
            val stamps = dao.everyStamp()
            val required = case.mustMove?.invoke(dao).orEmpty()
            when {
                stamps.none { it == WRITE_AT } ->
                    "${case.what}: no row carries the clock's value. Either nothing was written, " +
                        "or the write carried the row's existing lastModified forward."
                stamps.any { it != STALE && it != WRITE_AT } ->
                    "${case.what}: a row carries ${stamps.filter { it != STALE && it != WRITE_AT }}, " +
                        "which is neither the seeded value nor the clock's — an unstamped write " +
                        "reads back as 0."
                required.any { it != WRITE_AT } ->
                    "${case.what}: it also writes rows in another table, and those still carry " +
                        "$required. A second table's stamp is hidden by the first one's."
                else -> null
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun eachWriteReadsTheClockAgainRatherThanReusingTheFirstValue() {
        // A stamp captured once at construction would satisfy every assertion above and still be
        // wrong: the column would record when the app started, not when the setting changed, and
        // a sync would then think the display had not been touched since boot.
        var tick = 1_000L
        val dao = seededDao()
        val repository = roomRepository(dao) { tick }

        runBlocking { repository.updateShowSettingsOnTv(true) }
        assertEquals(1_000L, dao.configRow?.lastModified)

        tick = 2_000L
        runBlocking { repository.updateShowSettingsOnTv(false) }
        assertEquals(2_000L, dao.configRow?.lastModified)
    }

    @Test
    fun aConfigRowTheWriteHadToCreateIsStampedToo() {
        // `editConfig` falls back to `MosqueConfigEntity()` when the table is empty — which is
        // every first write on a fresh install, and the one place a default `lastModified = 0L`
        // could reach disk. It stamps the result, not the input, so it cannot.
        val dao = FakeMosqueDao()
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking { repository.updateStandbyEnabled(true) }

        val row = dao.configRow
        assertTrue("no config row was created", row != null)
        assertEquals(WRITE_AT, row?.lastModified)
    }

    @Test
    fun aWriteThatMatchesNoRowWritesNothingAtAll() {
        // The `?: return` in each edit helper. Worth pinning: the tempting alternative is to
        // upsert the change anyway, which would insert a half-built row under an id the caller
        // mistyped — and a stamped one, so it would pass every assertion above.
        val dao = seededDao()
        val repository = roomRepository(dao) { WRITE_AT }

        runBlocking {
            repository.updateMediaItemUsed("no-such-media", isUsed = false)
            repository.updateAnnouncement("no-such-announcement", "x", isActive = true)
            repository.updateFridayMediaItemPath("no-such-friday", "/nonexistent/x.jpg")
            repository.updateDashboardBackgroundUsed("no-such-background", isUsed = false)
            repository.updateSlideshowScheduleTime("no-such-schedule", "1", "2")
            repository.updateStandbyScheduleDays("no-such-schedule", "MONDAY")
        }

        assertEquals(
            "a write against an unknown id changed something",
            seededDao().rowCounts(),
            dao.rowCounts()
        )
        assertTrue(
            "a write against an unknown id stamped a row",
            dao.everyStamp().all { it == STALE }
        )
    }
}
