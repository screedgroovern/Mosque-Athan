package com.groovernapp.mosqueathan.ui.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import com.groovernapp.mosqueathan.util.optionalBoolean
import com.groovernapp.mosqueathan.util.optionalDouble
import com.groovernapp.mosqueathan.util.optionalInt
import com.groovernapp.mosqueathan.util.optionalString
import com.groovernapp.mosqueathan.domain.MediaStore
import com.groovernapp.mosqueathan.protocol.HttpFetcher
import com.groovernapp.mosqueathan.protocol.HttpResult
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.Announcement
import com.groovernapp.mosqueathan.domain.Media
import com.groovernapp.mosqueathan.domain.ConnectedDevice
import com.groovernapp.mosqueathan.domain.FridayMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.groovernapp.mosqueathan.util.ImageUtils

class SettingsViewModel(
    private val repository: ConfigRepository,
    private val mediaFileStore: MediaStore,
    private val http: HttpFetcher
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



    init {
        viewModelScope.launch {
            repository.getConfigFlow().collect { config ->
                _configState.value = config
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
    }




    fun resetToDefaults() {
        viewModelScope.launch {
            mediaFileStore.deleteCustomBackground()
            repository.resetToDefaults()
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
        
        viewModelScope.launch {
            when (val result = http.getText("http://${config.remoteTvIp}:8080/api/get_config")) {
                is HttpResult.Failure ->
                    _messages.emit(Lang.get("Sync failed: ${result.reason}", "Sinkronisasi gagal: ${result.reason}"))

                is HttpResult.Success -> try {
                    val response = result.body
                    val json = Json.parseToJsonElement(response).jsonObject
                    val configJson = json["config"] as? JsonObject
                    
                    if (configJson != null) {
                        repository.updateConfig(
                            latitude = configJson.optionalDouble("latitude"),
                            longitude = configJson.optionalDouble("longitude"),
                            iqomahSubuh = configJson.optionalInt("iqomahSubuh"),
                            iqomahDzuhur = configJson.optionalInt("iqomahDzuhur"),
                            iqomahAshar = configJson.optionalInt("iqomahAshar"),
                            iqomahMaghrib = configJson.optionalInt("iqomahMaghrib"),
                            iqomahIsya = configJson.optionalInt("iqomahIsya"),
                            calculationMethodName = configJson.optionalString("calculationMethodName"),
                            adzanLeadMinutes = configJson.optionalInt("adzanLeadMinutes"),
                            offsetImsak = configJson.optionalInt("offsetImsak"),
                            offsetSubuh = configJson.optionalInt("offsetSubuh"),
                            offsetSyuruq = configJson.optionalInt("offsetSyuruq"),
                            offsetDzuhur = configJson.optionalInt("offsetDzuhur"),
                            offsetAshar = configJson.optionalInt("offsetAshar"),
                            offsetMaghrib = configJson.optionalInt("offsetMaghrib"),
                            offsetIsya = configJson.optionalInt("offsetIsya"),
                            kemenagCityId = configJson.optionalString("kemenagCityId"),
                            kemenagCityName = configJson.optionalString("kemenagCityName"),
                            kemenagDuration = configJson.optionalString("kemenagDuration"),
                            kemenagTargetDate = configJson.optionalString("kemenagTargetDate"),
                            triggerFetch = true,
                            adzanFooterText = configJson.optionalString("adzanFooterText"),
                            iqomahFooterText = configJson.optionalString("iqomahFooterText")
                        )
                        repository.updateStandbySettings(
                            enabled = configJson.optionalBoolean("standbyModeEnabled"),
                            startTime = configJson.optionalString("standbyStartTime"),
                            endTime = configJson.optionalString("standbyEndTime")
                        )
                        repository.updateSlideshowEnabled(
                            enabled = configJson.optionalBoolean("slideshowEnabled")
                        )
                        
                        // Sync announcements
                        val announcements = json["announcements"] as? JsonArray
                        if (announcements != null) {
                            repository.clearAnnouncements()
                            announcements.filterIsInstance<JsonObject>().forEach { item ->
                                repository.addAnnouncement(
                                    content = item.optionalString("content"),
                                    customId = item.optionalString("id")
                                )
                            }
                        }
                        
                        _messages.emit(Lang.get("Sync complete.", "Sinkronisasi berhasil!"))
                    }
                } catch (e: Exception) {
                    // The fetch succeeded and the body did not parse: a display running
                    // different code, or something else answering on that address.
                    val reason = e.message ?: "unreadable response"
                    _messages.emit(
                        Lang.get("Sync failed: $reason", "Sinkronisasi gagal: $reason")
                    )
                }
            }
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

}
