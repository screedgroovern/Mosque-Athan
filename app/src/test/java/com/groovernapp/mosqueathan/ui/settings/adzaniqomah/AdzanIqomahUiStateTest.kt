package com.groovernapp.mosqueathan.ui.settings.adzaniqomah

import com.groovernapp.mosqueathan.domain.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdzanIqomahUiStateTest {

    @Test
    fun onlyThePrayersWithACongregationHaveIqomah() {
        // Imsak and Syuruq are informational, so they must not appear as rows the
        // user can set a countdown for.
        assertEquals(5, IQOMAH_PRAYERS.size)
        assertFalse(IQOMAH_PRAYERS.contains(PrayerType.IMSAK))
        assertFalse(IQOMAH_PRAYERS.contains(PrayerType.SYURUQ))
    }

    @Test
    fun iqomahPrayersAreInTheOrderOfTheDay() {
        assertEquals(
            listOf(
                PrayerType.SUBUH, PrayerType.DZUHUR, PrayerType.ASHAR,
                PrayerType.MAGHRIB, PrayerType.ISYA
            ),
            IQOMAH_PRAYERS
        )
    }

    @Test
    fun iqomahForReturnsTheStoredValue() {
        val state = AdzanIqomahUiState(
            iqomahMinutes = mapOf(PrayerType.MAGHRIB to 5, PrayerType.ISYA to 12)
        )
        assertEquals(5, state.iqomahFor(PrayerType.MAGHRIB))
        assertEquals(12, state.iqomahFor(PrayerType.ISYA))
    }

    @Test
    fun aMissingPrayerReadsAsZeroRatherThanThrowing() {
        // A blank map is what the state holds before the first config emission.
        // Throwing here would crash the settings screen on open.
        val state = AdzanIqomahUiState()
        PrayerType.entries.forEach { assertEquals(0, state.iqomahFor(it)) }
    }

    @Test
    fun everyIqomahPrayerCanBeRoundTrippedThroughTheMap() {
        val minutes = IQOMAH_PRAYERS.withIndex().associate { (i, p) -> p to (i + 1) * 3 }
        val state = AdzanIqomahUiState(iqomahMinutes = minutes)
        IQOMAH_PRAYERS.forEachIndexed { i, prayer ->
            assertEquals((i + 1) * 3, state.iqomahFor(prayer))
        }
    }

    @Test
    fun theDefaultLeadTimeMatchesTheAppDefault() {
        assertEquals(
            com.groovernapp.mosqueathan.domain.MosqueConfigDefaults.DEFAULT_ADZAN_LEAD_MINUTES,
            AdzanIqomahUiState().adzanLeadMinutes
        )
    }

    @Test
    fun footersDefaultToEmptyRatherThanNull() {
        val state = AdzanIqomahUiState()
        assertTrue(state.adzanFooterText.isEmpty())
        assertTrue(state.iqomahFooterText.isEmpty())
    }
}
