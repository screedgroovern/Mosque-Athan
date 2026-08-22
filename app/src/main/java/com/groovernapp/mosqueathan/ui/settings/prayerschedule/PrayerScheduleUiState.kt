package com.groovernapp.mosqueathan.ui.settings.prayerschedule

import com.groovernapp.mosqueathan.data.KemenagScheduleEntity
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.PrayerType

/** The calculation-method name that means "use pre-fetched Kemenag schedules". */
const val KEMENAG_METHOD = "KEMENAG"

/**
 * An Indonesian city, as returned by the Kemenag lookup.
 *
 * Replaces `Pair<String, String>`, which gave the reader no way to know which half
 * was the id.
 */
data class KemenagCity(val id: String, val name: String)

/** Everything the Kemenag half of the pane needs; only meaningful when [KEMENAG_METHOD] is selected. */
data class KemenagState(
    val cityId: String = "",
    val cityName: String = "",
    val duration: String = "",
    val targetDate: String = "",
    val searchResults: List<KemenagCity> = emptyList(),
    val isSearching: Boolean = false,
    val schedules: List<KemenagScheduleEntity> = emptyList()
)

/**
 * Everything the Prayer Schedule pane draws.
 *
 * The per-prayer offsets are a map, which is what collapses the old signature most:
 * seven values plus seven callbacks became one entry and one event.
 *
 * Latitude and longitude are `Double` here, not text. The raw string a user is
 * part-way through typing — "-7." parses to nothing — is buffered inside the field
 * itself, so this state only ever holds a coordinate that is real.
 */
data class PrayerScheduleUiState(
    val latitude: Double = MosqueConfig.DEFAULT_LATITUDE,
    val longitude: Double = MosqueConfig.DEFAULT_LONGITUDE,
    val calculationMethod: String = MosqueConfig.DEFAULT_CALCULATION_METHOD,
    val offsets: Map<PrayerType, Int> = emptyMap(),
    val kemenag: KemenagState = KemenagState()
) {
    val usesKemenag: Boolean get() = calculationMethod == KEMENAG_METHOD

    /** 0 for a prayer with no offset set, which is also the correct default. */
    fun offsetFor(prayer: PrayerType): Int = offsets[prayer] ?: 0
}

sealed interface PrayerScheduleEvent {
    data class LatitudeChanged(val value: Double) : PrayerScheduleEvent
    data class LongitudeChanged(val value: Double) : PrayerScheduleEvent
    data class MethodSelected(val method: String) : PrayerScheduleEvent
    data class OffsetChanged(val prayer: PrayerType, val minutes: Int) : PrayerScheduleEvent
    data class KemenagCityChosen(val city: KemenagCity) : PrayerScheduleEvent
    data class KemenagDurationChosen(val duration: String) : PrayerScheduleEvent
    data class KemenagTargetDateChosen(val date: String) : PrayerScheduleEvent
    data class KemenagCitySearched(val query: String) : PrayerScheduleEvent
    data object KemenagDownloadRequested : PrayerScheduleEvent
}
