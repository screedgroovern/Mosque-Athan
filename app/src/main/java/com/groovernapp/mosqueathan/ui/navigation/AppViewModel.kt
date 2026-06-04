package com.groovernapp.mosqueathan.ui.navigation

import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.ui.adzan.AdzanViewModel
import com.groovernapp.mosqueathan.ui.dashboard.DashboardUiState
import com.groovernapp.mosqueathan.ui.dashboard.DashboardViewModel
import com.groovernapp.mosqueathan.ui.dashboard.NextPrayerUiModel
import com.groovernapp.mosqueathan.ui.dashboard.PrayerTimesUiModel
import com.groovernapp.mosqueathan.ui.iqomah.IqomahViewModel
import com.groovernapp.mosqueathan.util.PrayerCalculator
import com.groovernapp.mosqueathan.util.PrayerResult
import kotlin.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn

class AppViewModel : ViewModel() {


    private val _activeScreen = MutableStateFlow<ActiveScreen>(ActiveScreen.Dashboard)
    val activeScreen = _activeScreen.asStateFlow()

    sealed interface ActiveScreen {
        object Dashboard : ActiveScreen
        data class Adzan(val prayerType: PrayerType, val prayerTime: LocalDateTime) : ActiveScreen
        data class Iqomah(val prayerType: PrayerType) : ActiveScreen
        object Settings : ActiveScreen
    }

    lateinit var dashboardViewModel: DashboardViewModel
    lateinit var adzanViewModel: AdzanViewModel
    lateinit var iqomahViewModel: IqomahViewModel

    private var coordinatorJob: Job? = null
    private var iqomahCollectorJob: Job? = null
    private var activeStateCollectorJob: Job? = null

    private var prayerResult: PrayerResult? = null
    private var lastCalculatedDate: LocalDate? = null
    private var lastTriggeredPrayer: PrayerType? = null

    private val _appState = MutableStateFlow<AppUiState>(
        AppUiState.Dashboard(
            DashboardUiState.Dashboard(
                currentTime = "--:--:--",
                gregorianDate = "-",
                hijriDate = "-",
                prayerTimes = PrayerTimesUiModel(
                    subuh = "--:--",
                    syuruq = "--:--",
                    dzuhur = "--:--",
                    ashar = "--:--",
                    maghrib = "--:--",
                    isya = "--:--",
                    highlightedPrayer = PrayerType.SUBUH
                ),
                nextPrayer = NextPrayerUiModel(
                    prayerType = PrayerType.SUBUH,
                    prayerTime = "--:--",
                    countdown = "--:--:--"
                ),
                runningText = ""
            ),
            customBackgroundPath = "",
            mediaList = emptyList(),
            transitionType = "FADE",
            slideIntervalSeconds = 10,
            videoWaitUntilEnd = false
        )
    )
    val appState: StateFlow<AppUiState> = _appState.asStateFlow()

    private val repository = com.groovernapp.mosqueathan.data.MosqueConfigRepository.getInstance()

    fun initialize(
        dashboardViewModel: DashboardViewModel,
        adzanViewModel: AdzanViewModel,
        iqomahViewModel: IqomahViewModel
    ) {
        this.dashboardViewModel = dashboardViewModel
        this.adzanViewModel = adzanViewModel
        this.iqomahViewModel = iqomahViewModel

        if (coordinatorJob == null) {
            startCoordinator()
            // Start collecting from Dashboard initially
            transitionToDashboard()

            viewModelScope.launch {
                repository.getConfigFlow().collect {
                    lastCalculatedDate = null
                    lastTriggeredPrayer = null
                }
            }
        }
    }

    private fun startCoordinator() {
        coordinatorJob = viewModelScope.launch {
            while (true) {
                refreshPrayerSchedule()
                checkTransitions()
                delay(1000)
            }
        }
    }

    private fun refreshPrayerSchedule() {
        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(zone)
        if (today == lastCalculatedDate) {
            return
        }

        lastCalculatedDate = today
        lastTriggeredPrayer = null
        val config = repository.getConfig()
        prayerResult = PrayerCalculator.calculate(
            latitude = config.latitude,
            longitude = config.longitude,
            calculationMethodName = config.calculationMethodName
        )
    }

    private fun checkTransitions() {
        val result = prayerResult ?: return
        val now = Clock.System.now().toEpochMilliseconds()

        val prayers = listOf(
            PrayerType.SUBUH to result.subuh,
            PrayerType.DZUHUR to result.dzuhur,
            PrayerType.ASHAR to result.ashar,
            PrayerType.MAGHRIB to result.maghrib,
            PrayerType.ISYA to result.isya
        )

        val currentScreen = _activeScreen.value

        if (currentScreen is ActiveScreen.Dashboard) {
            val config = repository.getConfig()
            val adzanLeadSeconds = config.adzanLeadMinutes * 60
            prayers.forEach { (prayer, time) ->
                val prayerMillis = time.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                val secondsUntilPrayer = ((prayerMillis - now) / 1000).toInt()
                
                if (lastTriggeredPrayer != prayer && secondsUntilPrayer in 1..adzanLeadSeconds) {
                    lastTriggeredPrayer = prayer
                    transitionToAdzan(prayer, time)
                }
            }
        } else if (currentScreen is ActiveScreen.Adzan) {
            val prayerTime = currentScreen.prayerTime
            val prayerMillis = prayerTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            val secondsUntilPrayer = ((prayerMillis - now) / 1000).toInt()

            if (secondsUntilPrayer <= 0) {
                try {
                    ToneGenerator(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100)
                        .startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1400)
                } catch (_: Exception) {
                    // Suppress audio failure if executed outside supported environment
                }
                transitionToIqomah(currentScreen.prayerType)
            }
        }
    }

    private fun transitionToAdzan(prayerType: PrayerType, prayerTime: LocalDateTime) {
        activeStateCollectorJob?.cancel()
        _activeScreen.value = ActiveScreen.Adzan(prayerType, prayerTime)
        adzanViewModel.start(prayerType, prayerTime)

        activeStateCollectorJob = viewModelScope.launch {
            adzanViewModel.uiState.collect { state ->
                _appState.value = AppUiState.Adzan(state)
            }
        }
    }

    private fun transitionToIqomah(prayerType: PrayerType) {
        iqomahCollectorJob?.cancel()
        activeStateCollectorJob?.cancel()
        _activeScreen.value = ActiveScreen.Iqomah(prayerType)
        iqomahViewModel.start(prayerType)

        activeStateCollectorJob = viewModelScope.launch {
            iqomahViewModel.uiState.collect { state ->
                _appState.value = AppUiState.Iqomah(state)
            }
        }

        iqomahCollectorJob = viewModelScope.launch {
            var hasStarted = false
            iqomahViewModel.uiState.collect { state ->
                if (state.remainingSeconds > 0) {
                    hasStarted = true
                }
                if (hasStarted && state.remainingSeconds == 0) {
                    viewModelScope.launch {
                        try {
                            val tg = ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
                            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
                            delay(350)
                            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
                            delay(350)
                            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 600)
                        } catch (_: Exception) {
                            // Suppress audio failure if executed outside supported environment
                        }
                    }
                    transitionToDashboard()
                }
            }
        }
    }

    fun transitionToDashboard() {
        iqomahCollectorJob?.cancel()
        activeStateCollectorJob?.cancel()
        _activeScreen.value = ActiveScreen.Dashboard

        activeStateCollectorJob = viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                dashboardViewModel.uiState,
                repository.getConfigFlow(),
                repository.getMediaListFlow()
            ) { dashboardState, config, mediaList ->
                AppUiState.Dashboard(
                    state = dashboardState,
                    customBackgroundPath = config.customBackgroundPath,
                    mediaList = mediaList,
                    transitionType = config.transitionType,
                    slideIntervalSeconds = config.slideIntervalSeconds,
                    videoWaitUntilEnd = config.videoWaitUntilEnd
                )
            }.collect { state ->
                _appState.value = state
            }
        }
    }

    fun transitionToSettings() {
        iqomahCollectorJob?.cancel()
        activeStateCollectorJob?.cancel()
        _activeScreen.value = ActiveScreen.Settings

        activeStateCollectorJob = viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                repository.getConfigFlow(),
                repository.getActiveAnnouncementsFlow(),
                repository.getMediaListFlow()
            ) { config, announcements, mediaList ->
                AppUiState.Settings(config, announcements, mediaList)
            }.collect { state ->
                _appState.value = state
            }
        }
    }

    fun toggleSettings() {
        if (_activeScreen.value is ActiveScreen.Settings) {
            transitionToDashboard()
        } else {
            transitionToSettings()
        }
    }

    override fun onCleared() {
        super.onCleared()
        coordinatorJob?.cancel()
        iqomahCollectorJob?.cancel()
        activeStateCollectorJob?.cancel()
    }
}
