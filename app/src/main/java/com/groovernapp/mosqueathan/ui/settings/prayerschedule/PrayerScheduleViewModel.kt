package com.groovernapp.mosqueathan.ui.settings.prayerschedule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import com.groovernapp.mosqueathan.data.copy
import com.groovernapp.mosqueathan.domain.PrayerType
import kotlinx.coroutines.Dispatchers
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The Prayer Schedule category: coordinates, calculation method, per-prayer offsets,
 * and the Kemenag city lookup.
 *
 * The city search moved here from `SettingsViewModel`, where it sat alongside every
 * other category. It belongs to exactly one pane.
 */
class PrayerScheduleViewModel(
    private val repository: MosqueConfigRepository
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

    private fun edit(change: (MosqueConfigEntity) -> MosqueConfigEntity) {
        viewModelScope.launch { repository.updateConfig(change(repository.getConfig())) }
    }

    private fun searchCities(query: String) {
        if (query.isBlank()) {
            searchResults.value = emptyList()
            return
        }
        isSearching.value = true
        viewModelScope.launch(Dispatchers.IO) {
            searchResults.value = try {
                fetchCities(query)
            } catch (e: Exception) {
                Log.e(TAG, "Kemenag city search failed", e)
                emptyList()
            } finally {
                isSearching.value = false
            }
        }
    }

    private fun fetchCities(query: String): List<KemenagCity> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val connection = URL("$CITY_SEARCH_URL$encoded").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.connect()
        if (connection.responseCode != 200) return emptyList()

        val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        val json = org.json.JSONObject(body)
        if (!json.optBoolean("status")) return emptyList()

        val array = json.optJSONArray("data") ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val item = array.getJSONObject(i)
            val id = item.optString("id")
            val name = item.optString("lokasi")
            if (id.isNotEmpty() && name.isNotEmpty()) KemenagCity(id, name) else null
        }
    }

    private companion object {
        const val TAG = "PrayerScheduleViewModel"
        const val CITY_SEARCH_URL = "https://api.myquran.com/v3/sholat/kabkota/cari/"
        const val TIMEOUT_MS = 5_000
    }
}

private fun MosqueConfigEntity.offsetFor(prayer: PrayerType): Int = when (prayer) {
    PrayerType.IMSAK -> offsetImsak
    PrayerType.SUBUH -> offsetSubuh
    PrayerType.SYURUQ -> offsetSyuruq
    PrayerType.DZUHUR -> offsetDzuhur
    PrayerType.ASHAR -> offsetAshar
    PrayerType.MAGHRIB -> offsetMaghrib
    PrayerType.ISYA -> offsetIsya
}

private fun MosqueConfigEntity.withOffset(prayer: PrayerType, minutes: Int): MosqueConfigEntity =
    when (prayer) {
        PrayerType.IMSAK -> copy(offsetImsak = minutes)
        PrayerType.SUBUH -> copy(offsetSubuh = minutes)
        PrayerType.SYURUQ -> copy(offsetSyuruq = minutes)
        PrayerType.DZUHUR -> copy(offsetDzuhur = minutes)
        PrayerType.ASHAR -> copy(offsetAshar = minutes)
        PrayerType.MAGHRIB -> copy(offsetMaghrib = minutes)
        PrayerType.ISYA -> copy(offsetIsya = minutes)
    }
