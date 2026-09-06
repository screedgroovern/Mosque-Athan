package com.groovernapp.mosqueathan.data.room

import com.groovernapp.mosqueathan.domain.PrayerType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The seven synchronous reads answer with what is stored, from the first call.
 *
 * ### Why this cannot be noticed at runtime
 *
 * `getConfig()` returns `MosqueConfig()` when it has nothing — factory defaults, which are a
 * complete and plausible configuration: Jakarta's coordinates, SINGAPORE's calculation method,
 * every offset zero. A cache that failed to prime therefore does not throw and does not blank the
 * screen. The display shows *prayer times*; they are simply another mosque's. `AppViewModel`'s
 * coordinator ticks once a second and would correct itself as soon as a flow emitted, so the
 * window is short — long enough to compute the wrong Adzan transition on the tick that matters,
 * and far too short for anyone to photograph.
 *
 * Room made this a real risk where Realm did not: Realm's reads were synchronous, so there was
 * nothing to prime and nothing that could be missed.
 *
 * The priming test runs against a DAO whose `observe*` flows emit **nothing at all**
 * ([FakeMosqueDao]'s `observable = false`), so a pass cannot be explained by a flow having
 * happened to arrive first. That is the only arrangement in which "primed" and "collected
 * quickly" are distinguishable.
 */
class RoomSynchronousReadTest {

    private val stored = FakeMosqueDao(observable = false).apply {
        seed(
            config = MosqueConfigEntity(
                latitude = -7.5,
                longitude = 110.5,
                iqomahSubuh = 11,
                iqomahDzuhur = 12,
                iqomahAshar = 13,
                iqomahMaghrib = 14,
                iqomahIsya = 15,
                calculationMethodName = "KEMENAG",
                lastModified = 1L
            ),
            media = listOf(
                MediaEntity("m2", "/nonexistent/second.jpg", false, true, 1, 1L),
                MediaEntity("m1", "/nonexistent/first.jpg", false, true, 0, 1L)
            ),
            fridayMedia = listOf(FridayMediaEntity("f1", "/nonexistent/f1.mp4", true, 0, 1L)),
            backgrounds = listOf(
                DashboardBackgroundEntity("b1", "/nonexistent/b1.jpg", true, 0, 1L),
                DashboardBackgroundEntity("b2", "/nonexistent/b2.jpg", false, 1, 1L)
            ),
            slideshow = listOf(
                SlideshowScheduleEntity("s1", "08:00", "09:00", "MONDAY", "", 1, "MONDAY", false, 1L)
            ),
            standby = listOf(
                StandbyScheduleEntity("t1", "22:00", "04:00", "MONDAY", "", 1, "MONDAY", false, 1L)
            )
        )
    }

    // --- vacuous-pass guard --------------------------------------------------------------------

    @Test
    fun theSeededValuesAreNotTheDefaults() {
        // Every assertion below would pass against an unprimed cache if the seed happened to
        // match `MosqueConfig()`. It must not, and this is the only thing standing between a
        // meaningful pass and a vacuous one.
        val defaults = roomRepository(FakeMosqueDao(observable = false)) { 0L }.getConfig()
        val seeded = requireNotNull(stored.configRow).toDomain()
        assertNotEquals("the seeded config is the default config", defaults, seeded)
        assertTrue("nothing was seeded", stored.rowCounts().values.sum() > 1)
    }

    // --- priming -------------------------------------------------------------------------------

    @Test
    fun everySynchronousReadIsPrimedBeforeAnyFlowEmits() {
        val repository = roomRepository(stored) { 0L }

        assertEquals(-7.5, repository.getConfig().latitude, 0.0)
        assertEquals("KEMENAG", repository.getConfig().calculationMethodName)
        assertEquals(listOf("m1", "m2"), repository.getMediaList().map { it.id })
        assertEquals(listOf("f1"), repository.getFridayMedia().map { it.id })
        assertEquals(listOf("b1", "b2"), repository.getDashboardBackgrounds().map { it.id })
        assertEquals(listOf("s1"), repository.getSlideshowSchedules().map { it.id })
        assertEquals(listOf("t1"), repository.getStandbySchedules().map { it.id })
        assertEquals(11, repository.getIqomahMinutes(PrayerType.SUBUH))
    }

    @Test
    fun theListReadsKeepTheOrderTheDaoSortedThemInto() {
        // Seeded out of order on purpose: `media` above lists m2 before m1. The DAO's ORDER BY is
        // what the display's slide order *is*, so a cache that preserved insertion order would
        // show the slides in the wrong sequence with nothing else wrong.
        val repository = roomRepository(stored) { 0L }
        assertEquals(
            listOf(0, 1),
            repository.getMediaList().map { it.sortOrder }
        )
    }

    @Test
    fun iqomahMinutesComeFromTheConfigAndAreZeroForTheInformationalPrayers() {
        val repository = roomRepository(stored) { 0L }
        assertEquals(11, repository.getIqomahMinutes(PrayerType.SUBUH))
        assertEquals(12, repository.getIqomahMinutes(PrayerType.DZUHUR))
        assertEquals(13, repository.getIqomahMinutes(PrayerType.ASHAR))
        assertEquals(14, repository.getIqomahMinutes(PrayerType.MAGHRIB))
        assertEquals(15, repository.getIqomahMinutes(PrayerType.ISYA))
        // Never a countdown; see PrayerType's note. Returning a real value here would put the
        // display into an iqomah screen for imsak, which no mosque wants.
        assertEquals(0, repository.getIqomahMinutes(PrayerType.IMSAK))
        assertEquals(0, repository.getIqomahMinutes(PrayerType.SYURUQ))
    }

    @Test
    fun anEmptyDatabaseReadsBackAsDefaultsRatherThanFailing() {
        // A fresh install, and the reason `getConfig()` returning defaults is correct behaviour
        // rather than the bug this class guards against.
        val repository = roomRepository(FakeMosqueDao(observable = false)) { 0L }
        assertEquals("SINGAPORE", repository.getConfig().calculationMethodName)
        assertTrue(repository.getMediaList().isEmpty())
        assertTrue(repository.getStandbySchedules().isEmpty())
        assertTrue(repository.getSlideshowSchedules().isEmpty())
        assertTrue(repository.getDashboardBackgrounds().isEmpty())
        assertTrue(repository.getFridayMedia().isEmpty())
    }

    @Test
    fun aSynchronousReadDoesNotGoBackToTheDao() {
        // The stated reason the cache exists: the coordinator calls getConfig() once a second for
        // the life of the display. A cache that re-read the database per call would be correct by
        // value and wrong by design, and nothing about the value would say so.
        val dao = FakeMosqueDao(observable = false).apply {
            seed(config = MosqueConfigEntity(adzanLeadMinutes = 9, lastModified = 1L))
        }
        val repository = roomRepository(dao) { 0L }
        assertTrue("the prime performed no reads, so this test counts nothing", dao.suspendReads > 0)

        val afterPriming = dao.suspendReads
        repeat(100) {
            repository.getConfig()
            repository.getMediaList()
            repository.getStandbySchedules()
            repository.getSlideshowSchedules()
            repository.getDashboardBackgrounds()
            repository.getFridayMedia()
            repository.getIqomahMinutes(PrayerType.ISYA)
        }
        assertEquals("a synchronous read reached the DAO", afterPriming, dao.suspendReads)
    }

    // --- staying current -----------------------------------------------------------------------

    @Test
    fun theCacheFollowsAWriteMadeThroughTheRepository() {
        val dao = FakeMosqueDao().apply {
            seed(config = MosqueConfigEntity(lastModified = 1L))
        }
        val repository = roomRepository(dao) { 2L }

        runBlocking {
            repository.updateStandbyEnabled(true)
            repository.addMediaItem("/nonexistent/new.jpg", isVideo = false)
            repository.addStandbySchedule("MONDAY", false, "22:00", "04:00", "t9")
            repository.addSlideshowSchedule("MONDAY", false, "08:00", "09:00", "s9")
            repository.addDashboardBackground("/nonexistent/bg.jpg", "b9")
            repository.addFridayMedia("/nonexistent/f.mp4", isVideo = true)
        }

        assertTrue(repository.getConfig().standbyModeEnabled)
        assertEquals(1, repository.getMediaList().size)
        assertEquals(listOf("t9"), repository.getStandbySchedules().map { it.id })
        assertEquals(listOf("s9"), repository.getSlideshowSchedules().map { it.id })
        assertEquals(listOf("b9"), repository.getDashboardBackgrounds().map { it.id })
        assertEquals(1, repository.getFridayMedia().size)
    }

    @Test
    fun theCacheFollowsAWriteMadeAroundTheRepository() {
        // Not a hypothetical: the display's own settings UI and `SettingsServer` both write, and
        // after step 3 a sync will write rows the repository never saw. The collectors in `init`
        // are what make the cache follow those, and nothing else in the class does.
        val dao = FakeMosqueDao().apply {
            seed(config = MosqueConfigEntity(adzanLeadMinutes = 3, lastModified = 1L))
        }
        val repository = roomRepository(dao) { 2L }
        assertEquals(3, repository.getConfig().adzanLeadMinutes)

        runBlocking {
            dao.upsertConfig(MosqueConfigEntity(adzanLeadMinutes = 5, lastModified = 9L))
            dao.upsertMedia(listOf(MediaEntity("m9", "/nonexistent/m9.jpg", false, true, 0, 9L)))
        }

        assertEquals(5, repository.getConfig().adzanLeadMinutes)
        assertEquals(listOf("m9"), repository.getMediaList().map { it.id })
    }
}
