package com.groovernapp.mosqueathan.ui.settings.adzaniqomah

import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.PrayerType

/**
 * The five prayers that have an iqomah countdown.
 *
 * Imsak and Syuruq are informational and are never followed by a congregation, which
 * is why `PrayerType.iqomahMinutes` is 0 for them.
 */
val IQOMAH_PRAYERS: List<PrayerType> = listOf(
    PrayerType.SUBUH,
    PrayerType.DZUHUR,
    PrayerType.ASHAR,
    PrayerType.MAGHRIB,
    PrayerType.ISYA
)

/**
 * Everything the Adzan & Iqomah pane draws.
 *
 * The iqomah durations are a map rather than five named fields, which is what
 * collapses the old signature the most: five values plus five callbacks became one
 * entry and one event. The pane can now loop over [IQOMAH_PRAYERS] instead of
 * repeating itself five times, and adding a sixth prayer would need no new
 * parameters.
 */
data class AdzanIqomahUiState(
    val adzanLeadMinutes: Int = MosqueConfig.DEFAULT_ADZAN_LEAD_MINUTES,
    val iqomahMinutes: Map<PrayerType, Int> = emptyMap(),
    val adzanFooterText: String = "",
    val iqomahFooterText: String = ""
) {
    /** 0 for a prayer with no iqomah, which is also the safe default for a missing key. */
    fun iqomahFor(prayer: PrayerType): Int = iqomahMinutes[prayer] ?: 0
}

sealed interface AdzanIqomahEvent {
    data class AdzanLeadMinutesChanged(val minutes: Int) : AdzanIqomahEvent
    data class IqomahMinutesChanged(val prayer: PrayerType, val minutes: Int) : AdzanIqomahEvent
    data class FootersChanged(val adzan: String, val iqomah: String) : AdzanIqomahEvent
    data object PreviewAdzanRequested : AdzanIqomahEvent
    data object PreviewIqomahRequested : AdzanIqomahEvent
}
