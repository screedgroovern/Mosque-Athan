package com.groovernapp.mosqueathan.ui.settings.prayerschedule

import com.groovernapp.mosqueathan.domain.parseKemenagCities
import com.groovernapp.mosqueathan.protocol.HttpFetcher
import com.groovernapp.mosqueathan.protocol.HttpResult
import com.groovernapp.mosqueathan.protocol.urlEncodePathSegment
import com.groovernapp.mosqueathan.protocol.RemoteActions
import kotlinx.serialization.json.put
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.protocol.RemoteActionSink
import com.groovernapp.mosqueathan.protocol.pushConfig
import com.groovernapp.mosqueathan.domain.PrayerType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The Prayer Schedule category: coordinates, calculation method, per-prayer offsets,
 * and the Kemenag city lookup.
 *
 * The city search moved here from `SettingsViewModel`, where it sat alongside every
 * other category. It belongs to exactly one pane.
 */
class PrayerScheduleViewModel(
    private val repository: ConfigRepository,
    private val remote: RemoteActionSink,
    private val http: HttpFetcher
) : ViewModel() {

    private val searchResults = MutableStateFlow<List<KemenagCity>>(emptyList())
    private val isSearching = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val schedules = repository.getConfigFlow()
        .map { it.kemenagCityId }
        .distinctUntilChanged()
        .flatMapLatest { repository.getKemenagSchedulesFlow(it) }

    val uiState: StateFlow<PrayerScheduleUiState> =
        combine(
            repository.getConfigFlow(),
            schedules,
            searchResults,
            isSearching
        ) { config, schedules, results, searching ->
            PrayerScheduleUiState(
                latitude = config.latitude,
                longitude = config.longitude,
                calculationMethod = config.calculationMethodName,
                offsets = PrayerType.entries.associateWith { config.offsetFor(it) },
                kemenag = KemenagState(
                    cityId = config.kemenagCityId,
                    cityName = config.kemenagCityName,
                    duration = config.kemenagDuration,
                    targetDate = config.kemenagTargetDate,
                    searchResults = results,
                    isSearching = searching,
                    schedules = schedules
                )
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PrayerScheduleUiState()
        )

    fun onEvent(event: PrayerScheduleEvent) {
        when (event) {
            is PrayerScheduleEvent.LatitudeChanged -> edit { it.copy(latitude = event.value) }
            is PrayerScheduleEvent.LongitudeChanged -> edit { it.copy(longitude = event.value) }
            is PrayerScheduleEvent.MethodSelected -> edit { it.copy(calculationMethodName = event.method) }
            is PrayerScheduleEvent.OffsetChanged -> edit { it.withOffset(event.prayer, event.minutes) }

            is PrayerScheduleEvent.KemenagCityChosen ->
                edit { it.copy(kemenagCityId = event.city.id, kemenagCityName = event.city.name) }

            is PrayerScheduleEvent.KemenagDurationChosen -> edit { it.copy(kemenagDuration = event.duration) }
            is PrayerScheduleEvent.KemenagTargetDateChosen -> edit { it.copy(kemenagTargetDate = event.date) }

            is PrayerScheduleEvent.KemenagCitySearched -> searchCities(event.query)

            PrayerScheduleEvent.KemenagDownloadRequested -> {
                val state = uiState.value.kemenag
                repository.triggerKemenagFetchForDuration(
                    cityId = state.cityId,
                    duration = state.duration,
                    targetDateStr = state.targetDate
                )
            }
        }
    }

    /**
     * Writes the change and tells the TV, if this device is controlling one.
     *
     * The push is not optional: on a controller, a setting that only lands locally
     * looks saved and never takes effect.
     */
    private fun edit(change: (MosqueConfig) -> MosqueConfig) {
        viewModelScope.launch {
            val updated = change(repository.getConfig())
            repository.updateConfig(updated)
            remote.send(RemoteActions.UPDATE_CONFIG) { pushConfig(updated) }
        }
    }

    private fun searchCities(query: String) {
        if (query.isBlank()) {
            searchResults.value = emptyList()
            return
        }
        isSearching.value = true
        viewModelScope.launch {
            val url = CITY_SEARCH_URL + urlEncodePathSegment(query)
            searchResults.value = when (val result = http.getText(url)) {
                is HttpResult.Success -> parseKemenagCities(result.body).map { (id, name) -> KemenagCity(id, name) }
                // A failed search shows no results, which is what an empty query shows
                // too. The takmir can retype; there is nowhere sensible to surface a
                // network error in a type-ahead field.
                is HttpResult.Failure -> emptyList()
            }
            isSearching.value = false
        }
    }

    private companion object {
        const val TAG = "PrayerScheduleViewModel"
        const val CITY_SEARCH_URL = "https://api.myquran.com/v3/sholat/kabkota/cari/"
        const val TIMEOUT_MS = 5_000
    }
}

private fun MosqueConfig.offsetFor(prayer: PrayerType): Int = when (prayer) {
    PrayerType.IMSAK -> offsetImsak
    PrayerType.SUBUH -> offsetSubuh
    PrayerType.SYURUQ -> offsetSyuruq
    PrayerType.DZUHUR -> offsetDzuhur
    PrayerType.ASHAR -> offsetAshar
    PrayerType.MAGHRIB -> offsetMaghrib
    PrayerType.ISYA -> offsetIsya
}

private fun MosqueConfig.withOffset(prayer: PrayerType, minutes: Int): MosqueConfig =
    when (prayer) {
        PrayerType.IMSAK -> copy(offsetImsak = minutes)
        PrayerType.SUBUH -> copy(offsetSubuh = minutes)
        PrayerType.SYURUQ -> copy(offsetSyuruq = minutes)
        PrayerType.DZUHUR -> copy(offsetDzuhur = minutes)
        PrayerType.ASHAR -> copy(offsetAshar = minutes)
        PrayerType.MAGHRIB -> copy(offsetMaghrib = minutes)
        PrayerType.ISYA -> copy(offsetIsya = minutes)
    }
