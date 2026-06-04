package com.groovernapp.mosqueathan

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.util.PrayerCalculator
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class PrayerCalculatorTest {

    @Test
    fun testNextPrayerStandardDaytime() {
        val zone = TimeZone.currentSystemDefault()
        // Mock time at June 4, 2026 at 10:00:00 AM (after Subuh/Syuruq, before Dzuhur)
        val mockNow = LocalDateTime(2026, 6, 4, 10, 0, 0).toInstant(zone)
        
        val result = PrayerCalculator.calculate(mockNow)
        
        // Assert that the next prayer is DZUHUR
        assertEquals(PrayerType.DZUHUR, result.nextPrayer)
        
        // Assert next prayer time is in the future relative to mockNow
        val mockNowMillis = mockNow.toEpochMilliseconds()
        val nextPrayerMillis = result.nextPrayerTime.toInstant(zone).toEpochMilliseconds()
        assertTrue("Next prayer time ${result.nextPrayerTime} should be after mockNow", nextPrayerMillis > mockNowMillis)
    }

    @Test
    fun testNextPrayerAfterIsha() {
        val zone = TimeZone.currentSystemDefault()
        // Mock time at June 4, 2026 at 9:00:00 PM (21:00:00), which is past Isha
        val mockNow = LocalDateTime(2026, 6, 4, 21, 0, 0).toInstant(zone)
        
        val result = PrayerCalculator.calculate(mockNow)
        
        // Next prayer after Isha should be SUBUH (representing tomorrow's Subuh)
        assertEquals(PrayerType.SUBUH, result.nextPrayer)
        
        // Next prayer time should be tomorrow's Subuh (June 5, 2026)
        val nextPrayerLocal = result.nextPrayerTime
        assertEquals(2026, nextPrayerLocal.year)
        assertEquals(6, nextPrayerLocal.month.number)
        assertEquals(5, nextPrayerLocal.day) // Should be June 5 (tomorrow)
        
        // Assert next prayer time is in the future relative to mockNow
        val mockNowMillis = mockNow.toEpochMilliseconds()
        val nextPrayerMillis = result.nextPrayerTime.toInstant(zone).toEpochMilliseconds()
        assertTrue("Next prayer time ${result.nextPrayerTime} should be in the future", nextPrayerMillis > mockNowMillis)
    }

    @Test
    fun testRealmMosqueConfig() = kotlinx.coroutines.runBlocking {
        val repository = com.groovernapp.mosqueathan.data.MosqueConfigRepository.getInstance()
        repository.resetToDefaults()
        val config = repository.getConfig()
        // Check default values
        assertEquals(-7.713585464212388, config.latitude, 0.0001)
        assertEquals(110.37963165899664, config.longitude, 0.0001)
        assertEquals(10, config.iqomahSubuh)
        assertEquals(5, config.iqomahMaghrib)

        // Try updating config
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
    fun testRealmAnnouncements() = kotlinx.coroutines.runBlocking {
        val repository = com.groovernapp.mosqueathan.data.MosqueConfigRepository.getInstance()
        repository.resetToDefaults()
        
        val active = repository.getActiveAnnouncements()
        assertEquals(3, active.size)
        assertEquals("Mari rapatkan shaf dan mohon handphone di-silent.", active[0].content)

        // Test adding announcement
        repository.addAnnouncement("Pengajian rutin hari Ahad pagi jam 06:00 WIB.")
        val updated = repository.getActiveAnnouncements()
        assertEquals(4, updated.size)
        assertTrue(updated.any { it.content == "Pengajian rutin hari Ahad pagi jam 06:00 WIB." })

        // Test updating announcement
        val newId = updated.first { it.content == "Pengajian rutin hari Ahad pagi jam 06:00 WIB." }.id
        repository.updateAnnouncement(newId, "Pengajian rutin hari Ahad pagi jam 07:00 WIB.", true)
        val afterUpdate = repository.getActiveAnnouncements()
        assertTrue(afterUpdate.any { it.content == "Pengajian rutin hari Ahad pagi jam 07:00 WIB." })
        
        // Test deleting announcement
        repository.deleteAnnouncement(newId)
        val afterDelete = repository.getActiveAnnouncements()
        assertEquals(3, afterDelete.size)
        assertFalse(afterDelete.any { it.id == newId })
    }
}
