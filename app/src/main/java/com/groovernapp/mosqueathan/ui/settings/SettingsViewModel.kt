package com.groovernapp.mosqueathan.ui.settings

import com.groovernapp.mosqueathan.data.media.MediaFileStore
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.Announcement
import com.groovernapp.mosqueathan.domain.Media
import com.groovernapp.mosqueathan.domain.ConnectedDevice
import com.groovernapp.mosqueathan.domain.FridayMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import com.groovernapp.mosqueathan.util.ImageUtils

class SettingsViewModel(
    private val repository: MosqueConfigRepository,
    private val mediaFileStore: MediaFileStore
) : ViewModel() {

    /**
     * Short messages for the user — sync succeeded, sync failed.
     *
     * A flow rather than a `Toast`, because a `Toast` needs a `Context` and that is the
     * single reason `syncWithTvServer` and `resetToDefaults` used to take one. The
     * screen decides how to show these; the ViewModel only says what happened.
     *
     * `SharedFlow` with no replay: a message is an event, and re-showing "sync failed"
     * on every recomposition would be worse than dropping it.
     */
    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()


    private val _configState = MutableStateFlow(MosqueConfig())
    val configState = _configState.asStateFlow()

    private val _announcementsState = MutableStateFlow<List<Announcement>>(emptyList())
    val announcementsState = _announcementsState.asStateFlow()

    private val _devicesState = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val devicesState = _devicesState.asStateFlow()

    private val _mediaListState = MutableStateFlow<List<Media>>(emptyList())
    val mediaListState = _mediaListState.asStateFlow()

    private val _fridayMediaListState = MutableStateFlow<List<FridayMedia>>(emptyList())
    val fridayMediaListState = _fridayMediaListState.asStateFlow()

    private val _kemenagSchedulesState = MutableStateFlow<List<com.groovernapp.mosqueathan.domain.KemenagSchedule>>(emptyList())
    val kemenagSchedulesState = _kemenagSchedulesState.asStateFlow()

    init {
        viewModelScope.launch {
            var schedulesJob: Job? = null
            repository.getConfigFlow().collect { config ->
                _configState.value = config
                schedulesJob?.cancel()
                schedulesJob = viewModelScope.launch {
                    repository.getKemenagSchedulesFlow(config.kemenagCityId).collect { list ->
                        _kemenagSchedulesState.value = list
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.getActiveAnnouncementsFlow().collect {
                _announcementsState.value = it
            }
        }
        viewModelScope.launch {
            repository.getConnectedDevicesFlow().collect {
                _devicesState.value = it
            }
        }
        viewModelScope.launch {
            repository.getMediaListFlow().collect {
                _mediaListState.value = it
            }
        }
        viewModelScope.launch {
            repository.observeFridayMedia().collect {
                _fridayMediaListState.value = it
            }
        }
    }

    private val _citySearchResults = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val citySearchResults = _citySearchResults.asStateFlow()

    private val _isSearchingCities = MutableStateFlow(false)
    val isSearchingCities = _isSearchingCities.asStateFlow()

    fun searchKemenagCities(query: String) {
        if (query.trim().isEmpty()) {
            _citySearchResults.value = emptyList()
            return
        }
        _isSearchingCities.value = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val urlString = "https://api.myquran.com/v3/sholat/kabkota/cari/${java.net.URLEncoder.encode(query, "UTF-8")}"
                val url = java.net.URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()

                if (connection.responseCode == 200) {
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
                    val response = java.lang.StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val json = org.json.JSONObject(response.toString())
                    if (json.optBoolean("status")) {
                        val arr = json.optJSONArray("data")
                        val results = mutableListOf<Pair<String, String>>()
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val item = arr.getJSONObject(i)
                                val id = item.optString("id")
                                val lokasi = item.optString("lokasi")
                                if (id.isNotEmpty() && lokasi.isNotEmpty()) {
                                    results.add(id to lokasi)
                                }
                            }
                        }
                        _citySearchResults.value = results
                    } else {
                        _citySearchResults.value = emptyList()
                    }
                } else {
                    _citySearchResults.value = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error searching Kemenag cities", e)
                _citySearchResults.value = emptyList()
            } finally {
                _isSearchingCities.value = false
            }
        }
    }

    fun updateConfig(
        latitude: Double,
        longitude: Double,
        iqomahSubuh: Int,
        iqomahDzuhur: Int,
        iqomahAshar: Int,
        iqomahMaghrib: Int,
        iqomahIsya: Int,
        calculationMethodName: String,
        adzanLeadMinutes: Int,
        offsetImsak: Int = 0,
        offsetSubuh: Int = 0,
        offsetSyuruq: Int = 0,
        offsetDzuhur: Int = 0,
        offsetAshar: Int = 0,
        offsetMaghrib: Int = 0,
        offsetIsya: Int = 0,
        kemenagCityId: String = "",
        kemenagCityName: String = "",
        kemenagDuration: String = "1_MONTH",
        kemenagTargetDate: String = "",
        adzanFooterText: String? = null,
        iqomahFooterText: String? = null
    ) {
        viewModelScope.launch {
            repository.updateConfig(
                latitude, longitude,
                iqomahSubuh, iqomahDzuhur, iqomahAshar, iqomahMaghrib, iqomahIsya,
                calculationMethodName, adzanLeadMinutes,
                offsetImsak, offsetSubuh, offsetSyuruq, offsetDzuhur, offsetAshar, offsetMaghrib, offsetIsya,
                kemenagCityId, kemenagCityName, kemenagDuration, kemenagTargetDate,
                triggerFetch = true,
                adzanFooterText = adzanFooterText,
                iqomahFooterText = iqomahFooterText
            )
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            mediaFileStore.deleteCustomBackground()
            repository.resetToDefaults()
        }
    }

    fun deleteMediaItem(item: Media) {
        viewModelScope.launch {
            try {
                val file = java.io.File(item.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
            repository.deleteMediaItem(item.id)
        }
    }

    private val _slideshowSchedulesState = MutableStateFlow<List<com.groovernapp.mosqueathan.domain.SlideshowSchedule>>(emptyList())
    val slideshowSchedulesState = _slideshowSchedulesState.asStateFlow()

    private val _dashboardBackgroundsState = MutableStateFlow<List<com.groovernapp.mosqueathan.domain.DashboardBackground>>(emptyList())
    val dashboardBackgroundsState = _dashboardBackgroundsState.asStateFlow()

    private val _standbySchedulesState = MutableStateFlow<List<com.groovernapp.mosqueathan.domain.StandbySchedule>>(emptyList())
    val standbySchedulesState = _standbySchedulesState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSlideshowSchedulesFlow().collect {
                _slideshowSchedulesState.value = it
            }
        }
        viewModelScope.launch {
            repository.getDashboardBackgroundsFlow().collect {
                _dashboardBackgroundsState.value = it
            }
        }
        viewModelScope.launch {
            repository.getStandbySchedulesFlow().collect {
                _standbySchedulesState.value = it
            }
        }
    }

    fun addSlideshowSchedule(day: String, isSpecificDate: Boolean, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.addSlideshowSchedule(day, isSpecificDate, startTime, endTime)
        }
    }

    fun deleteSlideshowSchedule(id: String) {
        viewModelScope.launch {
            repository.deleteSlideshowSchedule(id)
        }
    }

    fun addStandbySchedule(day: String, isSpecificDate: Boolean, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.addStandbySchedule(day, isSpecificDate, startTime, endTime)
        }
    }

    fun deleteStandbySchedule(id: String) {
        viewModelScope.launch {
            repository.deleteStandbySchedule(id)
        }
    }

    fun syncWithTvServer() {
        val config = _configState.value
        if (config.connectivityMode != "REMOTE" || config.remoteTvIp.isEmpty()) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("http://${config.remoteTvIp}:8080/api/get_config")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.connect()
                
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)
                    val configJson = json.optJSONObject("config")
                    
                    if (configJson != null) {
                        repository.updateConfig(
                            latitude = configJson.optDouble("latitude"),
                            longitude = configJson.optDouble("longitude"),
                            iqomahSubuh = configJson.optInt("iqomahSubuh"),
                            iqomahDzuhur = configJson.optInt("iqomahDzuhur"),
                            iqomahAshar = configJson.optInt("iqomahAshar"),
                            iqomahMaghrib = configJson.optInt("iqomahMaghrib"),
                            iqomahIsya = configJson.optInt("iqomahIsya"),
                            calculationMethodName = configJson.optString("calculationMethodName"),
                            adzanLeadMinutes = configJson.optInt("adzanLeadMinutes"),
                            offsetImsak = configJson.optInt("offsetImsak"),
                            offsetSubuh = configJson.optInt("offsetSubuh"),
                            offsetSyuruq = configJson.optInt("offsetSyuruq"),
                            offsetDzuhur = configJson.optInt("offsetDzuhur"),
                            offsetAshar = configJson.optInt("offsetAshar"),
                            offsetMaghrib = configJson.optInt("offsetMaghrib"),
                            offsetIsya = configJson.optInt("offsetIsya"),
                            kemenagCityId = configJson.optString("kemenagCityId"),
                            kemenagCityName = configJson.optString("kemenagCityName"),
                            kemenagDuration = configJson.optString("kemenagDuration"),
                            kemenagTargetDate = configJson.optString("kemenagTargetDate"),
                            triggerFetch = true,
                            adzanFooterText = configJson.optString("adzanFooterText"),
                            iqomahFooterText = configJson.optString("iqomahFooterText")
                        )
                        repository.updateStandbySettings(
                            enabled = configJson.optBoolean("standbyModeEnabled"),
                            startTime = configJson.optString("standbyStartTime"),
                            endTime = configJson.optString("standbyEndTime")
                        )
                        repository.updateSlideshowEnabled(
                            enabled = configJson.optBoolean("slideshowEnabled")
                        )
                        
                        // Sync announcements
                        val announcementsArray = json.optJSONArray("announcements")
                        if (announcementsArray != null) {
                            repository.clearAnnouncements()
                            for (i in 0 until announcementsArray.length()) {
                                val item = announcementsArray.getJSONObject(i)
                                repository.addAnnouncement(
                                    content = item.optString("content"),
                                    customId = item.optString("id")
                                )
                            }
                        }
                        
                        _messages.emit(Lang.get("Sync complete.", "Sinkronisasi berhasil!"))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to sync with TV", e)
                _messages.emit(
                    Lang.get("Sync failed: ${e.message}", "Sinkronisasi gagal: ${e.message}")
                )
            }
        }
    }

    fun updateConnectivityMode(mode: String) {
        viewModelScope.launch {
            repository.updateConnectivityMode(mode)
        }
    }

    fun updateRemoteTvIp(ip: String) {
        viewModelScope.launch {
            repository.updateRemoteTvIp(ip)
        }
    }

    fun updateFooters(adzanFooter: String, iqomahFooter: String) {
        viewModelScope.launch {
            val config = repository.getConfig()
            repository.updateConfig(
                latitude = config.latitude,
                longitude = config.longitude,
                iqomahSubuh = config.iqomahSubuh,
                iqomahDzuhur = config.iqomahDzuhur,
                iqomahAshar = config.iqomahAshar,
                iqomahMaghrib = config.iqomahMaghrib,
                iqomahIsya = config.iqomahIsya,
                calculationMethodName = config.calculationMethodName,
                adzanLeadMinutes = config.adzanLeadMinutes,
                offsetImsak = config.offsetImsak,
                offsetSubuh = config.offsetSubuh,
                offsetSyuruq = config.offsetSyuruq,
                offsetDzuhur = config.offsetDzuhur,
                offsetAshar = config.offsetAshar,
                offsetMaghrib = config.offsetMaghrib,
                offsetIsya = config.offsetIsya,
                kemenagCityId = config.kemenagCityId,
                kemenagCityName = config.kemenagCityName,
                kemenagDuration = config.kemenagDuration,
                kemenagTargetDate = config.kemenagTargetDate,
                adzanFooterText = adzanFooter,
                iqomahFooterText = iqomahFooter
            )
        }
    }

    fun updateSlideshowScheduleTime(id: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.updateSlideshowScheduleTime(id, startTime, endTime)
        }
    }

    fun updateSlideshowScheduleDays(id: String, activeDays: String) {
        viewModelScope.launch {
            repository.updateSlideshowScheduleDays(id, activeDays)
        }
    }

    fun updateSlideshowScheduleDates(id: String, specificDates: String) {
        viewModelScope.launch {
            repository.updateSlideshowScheduleDates(id, specificDates)
        }
    }

    fun updateSlideshowEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSlideshowEnabled(enabled)
        }
    }

    fun updateStandbyScheduleTime(id: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.updateStandbyScheduleTime(id, startTime, endTime)
        }
    }

    fun updateStandbyScheduleDays(id: String, activeDays: String) {
        viewModelScope.launch {
            repository.updateStandbyScheduleDays(id, activeDays)
        }
    }

    fun updateStandbyScheduleDates(id: String, specificDates: String) {
        viewModelScope.launch {
            repository.updateStandbyScheduleDates(id, specificDates)
        }
    }

    fun updateStandbyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateStandbyEnabled(enabled)
        }
    }

    fun deleteDashboardBackground(id: String) {
        viewModelScope.launch {
            try {
                val item = _dashboardBackgroundsState.value.firstOrNull { it.id == id }
                if (item != null) {
                    val file = java.io.File(item.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (_: Exception) {}
            repository.deleteDashboardBackground(id)
        }
    }

    fun updateDashboardBackgroundUsed(id: String, isUsed: Boolean) {
        viewModelScope.launch {
            repository.updateDashboardBackgroundUsed(id, isUsed)
        }
    }

    fun updateDashboardBackgroundOrder(ids: List<String>) {
        viewModelScope.launch {
            repository.updateDashboardBackgroundOrder(ids)
        }
    }

    fun updateDashboardSettings(randomize: Boolean, transitionType: String, intervalSeconds: Int) {
        viewModelScope.launch {
            val config = repository.getConfig()
            repository.updateConfig(
                latitude = config.latitude,
                longitude = config.longitude,
                iqomahSubuh = config.iqomahSubuh,
                iqomahDzuhur = config.iqomahDzuhur,
                iqomahAshar = config.iqomahAshar,
                iqomahMaghrib = config.iqomahMaghrib,
                iqomahIsya = config.iqomahIsya,
                calculationMethodName = config.calculationMethodName,
                adzanLeadMinutes = config.adzanLeadMinutes,
                offsetImsak = config.offsetImsak,
                offsetSubuh = config.offsetSubuh,
                offsetSyuruq = config.offsetSyuruq,
                offsetDzuhur = config.offsetDzuhur,
                offsetAshar = config.offsetAshar,
                offsetMaghrib = config.offsetMaghrib,
                offsetIsya = config.offsetIsya,
                kemenagCityId = config.kemenagCityId,
                kemenagCityName = config.kemenagCityName,
                kemenagDuration = config.kemenagDuration,
                kemenagTargetDate = config.kemenagTargetDate,
                adzanFooterText = config.adzanFooterText,
                iqomahFooterText = config.iqomahFooterText,
                dashboardRandomize = randomize,
                dashboardTransitionType = transitionType,
                dashboardTransitionInterval = intervalSeconds
            )
        }
    }

    fun updateMediaItemUsed(id: String, isUsed: Boolean) {
        viewModelScope.launch {
            repository.updateMediaItemUsed(id, isUsed)
        }
    }
    fun updateMediaListOrder(ids: List<String>) {
        viewModelScope.launch {
            repository.updateMediaListOrder(ids)
        }
    }

    fun deleteFridayMediaItem(item: FridayMedia) {
        viewModelScope.launch {
            try {
                val file = java.io.File(item.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
            repository.deleteFridayMedia(item.id)
        }
    }

    fun updateFridayMediaOrder(ids: List<String>) {
        viewModelScope.launch {
            repository.updateFridayMediaOrder(ids)
        }
    }
}
