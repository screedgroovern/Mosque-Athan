package com.groovernapp.mosqueathan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Realm-backed repository tests. These stay in `:app` because they need the Realm
 * runtime, which is Android-only.
 *
 * The prayer-time calculations that used to live here — including the Kemenag path,
 * which previously needed a live database — moved to `:core:prayer`'s `commonTest`
 * once `PrayerCalculator` took its schedule source as a parameter.
 */
class RealmRepositoryTest {

    // Constructed directly, which is the point of step 2: before the singleton went
    // away this had to be MosqueConfigRepository.getInstance(), so every test shared
    // one process-wide instance whether it wanted to or not.
    private val repository = com.groovernapp.mosqueathan.data.MosqueConfigRepository()

    @Test
    fun mosqueConfigDefaultsAndUpdates() = kotlinx.coroutines.runBlocking {
        repository.resetToDefaults()
        val config = repository.getConfig()
        assertEquals(-7.713585464212388, config.latitude, 0.0001)
        assertEquals(110.37963165899664, config.longitude, 0.0001)
        assertEquals(10, config.iqomahSubuh)
        assertEquals(5, config.iqomahMaghrib)

        repository.updateConfig(
            latitude = -6.200000,
            longitude = 106.816666,
            iqomahSubuh = 12,
            iqomahDzuhur = 8,
            iqomahAshar = 8,
            iqomahMaghrib = 7,
            iqomahIsya = 15,
            calculationMethodName = "EGYPTIAN",
            adzanLeadMinutes = 5
        )

        val updated = repository.getConfig()
        assertEquals(-6.200000, updated.latitude, 0.0001)
        assertEquals(106.816666, updated.longitude, 0.0001)
        assertEquals(12, updated.iqomahSubuh)
        assertEquals(8, updated.iqomahDzuhur)
        assertEquals(8, updated.iqomahAshar)
        assertEquals(7, updated.iqomahMaghrib)
        assertEquals(15, updated.iqomahIsya)
        assertEquals("EGYPTIAN", updated.calculationMethodName)
        assertEquals(5, updated.adzanLeadMinutes)
    }

    @Test
    fun announcementCrud() = kotlinx.coroutines.runBlocking {
        repository.resetToDefaults()

        val active = repository.getActiveAnnouncements()
        assertEquals(3, active.size)
        assertEquals("Mari rapatkan shaf dan mohon handphone di-silent.", active[0].content)

        repository.addAnnouncement("Pengajian rutin hari Ahad pagi jam 06:00 WIB.")
        val updated = repository.getActiveAnnouncements()
        assertEquals(4, updated.size)
        assertTrue(updated.any { it.content == "Pengajian rutin hari Ahad pagi jam 06:00 WIB." })

        val newId = updated.first { it.content == "Pengajian rutin hari Ahad pagi jam 06:00 WIB." }.id
        repository.updateAnnouncement(newId, "Pengajian rutin hari Ahad pagi jam 07:00 WIB.", true)
        val afterUpdate = repository.getActiveAnnouncements()
        assertTrue(afterUpdate.any { it.content == "Pengajian rutin hari Ahad pagi jam 07:00 WIB." })

        repository.deleteAnnouncement(newId)
        val afterDelete = repository.getActiveAnnouncements()
        assertEquals(3, afterDelete.size)
        assertFalse(afterDelete.any { it.id == newId })
    }

    /**
     * The Kemenag schedule still round-trips through Realm; the calculation that
     * consumes it is covered in `:core:prayer`.
     */
    @Test
    fun kemenagScheduleRoundTripsThroughRealm() = kotlinx.coroutines.runBlocking {
        repository.resetToDefaults()
        val cityId = "58a2fc6ed39fd083f55d4182bf88826d"

        repository.saveKemenagSchedule(
            com.groovernapp.mosqueathan.domain.KemenagSchedule(dateString = "2026-06-05", cityId = cityId, imsak = "04:27", subuh = "04:37", terbit = "05:54", dhuha = "06:24", dzuhur = "11:55", ashar = "15:16", maghrib = "17:48", isya = "19:02")
        )

        val source = com.groovernapp.mosqueathan.data.RealmKemenagScheduleSource(repository)
        val schedule = source.scheduleFor(cityId, "2026-06-05")
        assertTrue(schedule != null)
        assertEquals("04:27", schedule!!.imsak)
        assertEquals("04:37", schedule.subuh)
        assertEquals("11:55", schedule.dzuhur)
        assertEquals("19:02", schedule.isya)

        assertEquals(null, source.scheduleFor(cityId, "1999-01-01"))
    }
}
