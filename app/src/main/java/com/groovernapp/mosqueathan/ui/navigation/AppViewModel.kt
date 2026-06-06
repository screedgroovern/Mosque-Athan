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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
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
    private var lastAdzanBeepedSecond = -1
    private var isPreviewMode = false

    private val _appState = MutableStateFlow<AppUiState>(
        AppUiState.Dashboard(
            DashboardUiState.Dashboard(
                currentTime = "--:--:--",
                gregorianDate = "-",
                hijriDate = "-",
                prayerTimes = PrayerTimesUiModel(
                    imsak = "--:--",
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
                runningText = "",
                isSlideshowActive = false
            ),
            customBackgroundPath = "",
            mediaList = emptyList(),
            transitionType = "FADE",
            slideIntervalSeconds = 10,
            videoWaitUntilEnd = false,
            includeDefaultImage = true
        )
    )
    val appState: StateFlow<AppUiState> = _appState.asStateFlow()

    private val _isStandbyActive = MutableStateFlow(false)
    val isStandbyActive: StateFlow<Boolean> = _isStandbyActive.asStateFlow()

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
                updateStandbyStatus()
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
            calculationMethodName = config.calculationMethodName,
            offsetImsak = config.offsetImsak,
            offsetSubuh = config.offsetSubuh,
            offsetSyuruq = config.offsetSyuruq,
            offsetDzuhur = config.offsetDzuhur,
            offsetAshar = config.offsetAshar,
            offsetMaghrib = config.offsetMaghrib,
            offsetIsya = config.offsetIsya,
            kemenagCityId = config.kemenagCityId
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
                    isPreviewMode = false
                    transitionToAdzan(prayer, time)
                }
            }
        } else if (currentScreen is ActiveScreen.Adzan) {
            // Managed by UI state flow collector in transitionToAdzan
        }
    }

    private fun playShortBeep() {
        playSineWave(1800.0, 150)
    }

    private fun playLongBeep() {
        playSineWave(2200.0, 1500)
    }

    private fun playSineWave(frequency: Double, durationMs: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sampleRate = 8000
                val numSamples = (durationMs / 1000.0 * sampleRate).toInt()
                val samples = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    samples[i] = (Math.sin(2.0 * Math.PI * frequency * t) * Short.MAX_VALUE).toInt().toShort()
                }
                val audioTrack = android.media.AudioTrack(
                    android.media.AudioManager.STREAM_ALARM,
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_OUT_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    numSamples * 2,
                    android.media.AudioTrack.MODE_STATIC
                )
                audioTrack.write(samples, 0, numSamples)
                audioTrack.play()
                delay(durationMs.toLong() + 100)
                audioTrack.release()
            } catch (_: Exception) {
                // Suppress audio failure if executed outside supported environment
            }
        }
    }

    private fun transitionToAdzan(prayerType: PrayerType, prayerTime: LocalDateTime) {
        lastAdzanBeepedSecond = -1
        activeStateCollectorJob?.cancel()
        _activeScreen.value = ActiveScreen.Adzan(prayerType, prayerTime)
        adzanViewModel.start(prayerType, prayerTime)

        activeStateCollectorJob = viewModelScope.launch {
            var hasStarted = false
            adzanViewModel.uiState.collect { state ->
                _appState.value = AppUiState.Adzan(state)
                val remaining = state.remainingSeconds
                if (remaining > 0) {
                    hasStarted = true
                }
                if (hasStarted) {
                    if (remaining in 1..3 && remaining != lastAdzanBeepedSecond) {
                        lastAdzanBeepedSecond = remaining
                        playShortBeep()
                    }
                    if (remaining == 0 && lastAdzanBeepedSecond != 0) {
                        lastAdzanBeepedSecond = 0
                        playLongBeep()
                        if (isPreviewMode) {
                            viewModelScope.launch {
                                delay(2000)
                                transitionToSettings()
                            }
                        } else {
                            transitionToIqomah(prayerType)
                        }
                    }
                }
            }
        }
    }

    private fun transitionToIqomah(prayerType: PrayerType, customDurationSeconds: Int? = null) {
        iqomahCollectorJob?.cancel()
        activeStateCollectorJob?.cancel()
        _activeScreen.value = ActiveScreen.Iqomah(prayerType)
        iqomahViewModel.start(prayerType, customDurationSeconds)

        activeStateCollectorJob = viewModelScope.launch {
            iqomahViewModel.uiState.collect { state ->
                _appState.value = AppUiState.Iqomah(state)
            }
        }

        iqomahCollectorJob = viewModelScope.launch {
            var hasStarted = false
            var lastIqomahBeepedSecond = -1
            iqomahViewModel.uiState.collect { state ->
                val remaining = state.remainingSeconds
                if (remaining > 0) {
                    hasStarted = true
                }
                if (hasStarted) {
                    if (remaining in 1..3 && remaining != lastIqomahBeepedSecond) {
                        lastIqomahBeepedSecond = remaining
                        playShortBeep()
                    }
                    if (remaining == 0 && lastIqomahBeepedSecond != 0) {
                        lastIqomahBeepedSecond = 0
                        playLongBeep()
                        if (isPreviewMode) {
                            viewModelScope.launch {
                                delay(2000)
                                transitionToSettings()
                            }
                        } else {
                            transitionToDashboard()
                        }
                    }
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
                    videoWaitUntilEnd = config.videoWaitUntilEnd,
                    includeDefaultImage = config.includeDefaultImage
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

    fun previewAdzan() {
        isPreviewMode = true
        val zone = TimeZone.currentSystemDefault()
        val simulatedTime = Clock.System.now().plus(20, DateTimeUnit.SECOND).toLocalDateTime(zone)
        transitionToAdzan(PrayerType.SUBUH, simulatedTime)
    }

    fun previewIqomah() {
        isPreviewMode = true
        transitionToIqomah(PrayerType.SUBUH, customDurationSeconds = 20)
    }

    fun previewSlideshow() {
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
                    state = dashboardState.copy(isSlideshowActive = true),
                    customBackgroundPath = config.customBackgroundPath,
                    mediaList = mediaList,
                    transitionType = config.transitionType,
                    slideIntervalSeconds = config.slideIntervalSeconds,
                    videoWaitUntilEnd = config.videoWaitUntilEnd,
                    includeDefaultImage = config.includeDefaultImage
                )
            }.collect { state ->
                _appState.value = state
            }
        }
    }

    private fun updateStandbyStatus() {
        val config = repository.getConfig()
        if (!config.standbyModeEnabled) {
            _isStandbyActive.value = false
            return
        }

        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(zone)
        val currentMinutes = now.hour * 60 + now.minute

        val startMinutes = parseTimeToMinutes(config.standbyStartTime) ?: 0
        val endMinutes = parseTimeToMinutes(config.standbyEndTime) ?: 0

        _isStandbyActive.value = if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            // Interval crosses midnight, e.g., 22:00 to 04:00
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int? {
        val parts = timeStr.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    override fun onCleared() {
        super.onCleared()
        coordinatorJob?.cancel()
        iqomahCollectorJob?.cancel()
        activeStateCollectorJob?.cancel()
    }
}
