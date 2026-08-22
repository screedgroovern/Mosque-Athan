package com.groovernapp.mosqueathan.ui.settings.prayerschedule

import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerScheduleUiStateTest {

    @Test
    fun everyPrayerCanCarryAnOffsetIncludingTheInformationalOnes() {
        // Unlike iqomah, an offset is meaningful for Imsak and Syuruq: they are
        // displayed, so a mosque may want to correct them even though no
        // congregation follows.
        val offsets = PrayerType.entries.withIndex().associate { (i, p) -> p to i - 3 }
        val state = PrayerScheduleUiState(offsets = offsets)
        PrayerType.entries.forEachIndexed { i, prayer ->
            assertEquals(i - 3, state.offsetFor(prayer))
        }
    }

    @Test
    fun aMissingOffsetReadsAsZero()
    {
        // The state before the first config emission. Zero is also the correct
        // meaning of "no correction", so this default is not just crash avoidance.
        val state = PrayerScheduleUiState()
        PrayerType.entries.forEach { assertEquals(0, state.offsetFor(it)) }
    }

    @Test
    fun negativeOffsetsSurvive() {
        val state = PrayerScheduleUiState(offsets = mapOf(PrayerType.IMSAK to -10))
        assertEquals(-10, state.offsetFor(PrayerType.IMSAK))
    }

    @Test
    fun usesKemenagOnlyForTheKemenagMethod() {
        assertTrue(PrayerScheduleUiState(calculationMethod = KEMENAG_METHOD).usesKemenag)
        assertFalse(PrayerScheduleUiState(calculationMethod = "SINGAPORE").usesKemenag)
        assertFalse(PrayerScheduleUiState(calculationMethod = "").usesKemenag)
        assertFalse(PrayerScheduleUiState(calculationMethod = "kemenag").usesKemenag)
    }

    @Test
    fun theKemenagMethodNameMatchesWhatPrayerCalculatorLooksFor() {
        // PrayerCalculator branches on this exact string. If they drift, the pane
        // shows the city picker while the calculation quietly uses adhan2.
        assertEquals("KEMENAG", KEMENAG_METHOD)
    }

    @Test
    fun defaultsComeFromTheAppDefaults() {
        val state = PrayerScheduleUiState()
        assertEquals(MosqueConfig.DEFAULT_LATITUDE, state.latitude, 0.0)
        assertEquals(MosqueConfig.DEFAULT_LONGITUDE, state.longitude, 0.0)
        assertEquals(MosqueConfig.DEFAULT_CALCULATION_METHOD, state.calculationMethod)
    }

    @Test
    fun aCityIsIdentifiableRatherThanAPairOfStrings() {
        val city = KemenagCity(id = "1219", name = "KOTA YOGYAKARTA")
        assertEquals("1219", city.id)
        assertEquals("KOTA YOGYAKARTA", city.name)
    }

    @Test
    fun kemenagStateStartsEmptyAndNotSearching() {
        val kemenag = PrayerScheduleUiState().kemenag
        assertTrue(kemenag.cityId.isEmpty())
        assertTrue(kemenag.searchResults.isEmpty())
        assertTrue(kemenag.schedules.isEmpty())
        assertFalse(kemenag.isSearching)
    }
}
