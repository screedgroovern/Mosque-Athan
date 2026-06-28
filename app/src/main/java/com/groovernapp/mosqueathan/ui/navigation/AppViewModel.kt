package com.groovernapp.mosqueathan.ui.navigation

import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Duration.Companion.seconds

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
    var isPreviewMode = false
        private set
    private var currentConfig = MosqueConfigEntity()

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
    private var forcedStandby = false
    private var forcedFridaySilence = false

    private val repository = com.groovernapp.mosqueathan.data.MosqueConfigRepository.getInstance()

    fun initialize(
        dashboardViewModel: DashboardViewModel,
        adzanViewModel: AdzanViewModel,
        iqomahViewModel: IqomahViewModel
    ) {
        this.dashboardViewModel = dashboardViewModel
        this.adzanViewModel = adzanViewModel
        this.iqomahViewModel = iqomahViewModel
        currentConfig = repository.getConfig()

        if (coordinatorJob == null) {
            startCoordinator()
            // Start collecting from Dashboard initially
            transitionToDashboard()

            viewModelScope.launch {
                repository.getConfigFlow().collect { config ->
                    currentConfig = config
                    lastCalculatedDate = null
                    lastTriggeredPrayer = null
                    forcedFridaySilence = false
                    updateStandbyStatus()
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
        forcedFridaySilence = false
        val config = currentConfig
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
            kemenagCityId = config.kemenagCityId,
            jumatEnabled = config.jumatEnabled,
            offsetJumat = config.offsetJumat
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
            val config = currentConfig
            val adzanLeadSeconds = config.adzanLeadMinutes * 60
            
            // Check if we should transition to Adzan
            prayers.forEach { (prayer, time) ->
                val prayerMillis = time.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                val secondsUntilPrayer = ((prayerMillis - now) / 1000).toInt()
                
                if (lastTriggeredPrayer != prayer && secondsUntilPrayer in 1..adzanLeadSeconds) {
                    lastTriggeredPrayer = prayer
                    isPreviewMode = false
                    transitionToAdzan(prayer, time)
                    return
                }
            }

            // Check if we missed the adzan but are inside the Iqomah window
            prayers.forEach { (prayer, time) ->
                val prayerMillis = time.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                val iqomahMinutes = getIqomahMinutesFromConfig(prayer, config)
                val iqomahDurationSeconds = iqomahMinutes * 60
                
                val elapsedSeconds = ((now - prayerMillis) / 1000).toInt()
                if (iqomahDurationSeconds > 0 && elapsedSeconds in 0 until iqomahDurationSeconds) {
                    if (lastTriggeredPrayer != prayer) {
                        lastTriggeredPrayer = prayer
                        isPreviewMode = false
                        val remainingSeconds = iqomahDurationSeconds - elapsedSeconds
                        val zone = TimeZone.currentSystemDefault()
                        val currentLocalDate = Clock.System.now().toLocalDateTime(zone)
                        val isFridayDzuhur = currentLocalDate.dayOfWeek == kotlinx.datetime.DayOfWeek.FRIDAY && prayer == PrayerType.DZUHUR
                        if (!isFridayDzuhur) {
                            transitionToIqomah(prayer, customDurationSeconds = remainingSeconds)
                        }
                        return
                    }
                }
            }
        }
    }

    private fun getIqomahMinutesFromConfig(prayerType: PrayerType, config: MosqueConfigEntity): Int {
        return when (prayerType) {
            PrayerType.SUBUH -> config.iqomahSubuh
            PrayerType.DZUHUR -> config.iqomahDzuhur
            PrayerType.ASHAR -> config.iqomahAshar
            PrayerType.MAGHRIB -> config.iqomahMaghrib
            PrayerType.ISYA -> config.iqomahIsya
            else -> 0
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
                            val zone = TimeZone.currentSystemDefault()
                            val nowTime = Clock.System.now().toLocalDateTime(zone)
                            if (nowTime.dayOfWeek == kotlinx.datetime.DayOfWeek.FRIDAY && prayerType == PrayerType.DZUHUR) {
                                transitionToDashboard()
                                if (currentConfig.jumatEnabled && currentConfig.jumatStandbyOnly) {
                                    forceStandbyMode()
                                }
                            } else {
                                transitionToIqomah(prayerType)
                            }
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
        isPreviewMode = false

        val mappedFridayMediaFlow = repository.observeFridayMedia().map { fmList: List<com.groovernapp.mosqueathan.data.FridayMediaEntity> ->
            fmList.map { fm ->
                com.groovernapp.mosqueathan.data.MediaEntity().apply {
                    id = fm.id
                    filePath = fm.filePath
                    isVideo = fm.isVideo
                    sortOrder = fm.sortOrder
                }
            }
        }

        activeStateCollectorJob = viewModelScope.launch {
            combine(
                dashboardViewModel.uiState,
                repository.getConfigFlow(),
                repository.getMediaListFlow(),
                repository.getDashboardBackgroundsFlow(),
                mappedFridayMediaFlow
            ) { dashboardState, config, mediaList, dashboardBackgrounds, fridayMedia ->
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val isFriday = isFridayModeActive(config, now)
                
                var isFridayBeforeAdzan = false
                var isFridayAfterAdzan = false

                if (isFriday && dashboardState is DashboardUiState.Dashboard) {
                    val dzuhurTimeStr = dashboardState.prayerTimes.dzuhur
                    val dzuhurMins = parseTimeToMinutes(dzuhurTimeStr)
                    val currentMins = now.hour * 60 + now.minute
                    if (dzuhurMins != null) {
                        if (currentMins < dzuhurMins && !forcedFridaySilence) {
                            isFridayBeforeAdzan = true
                        } else {
                            isFridayAfterAdzan = true
                        }
                    }
                }
                
                var finalMediaList = mediaList
                var tType = config.transitionType
                var sInterval = config.slideIntervalSeconds
                var vWait = config.videoWaitUntilEnd

                if (isFriday && isFridayBeforeAdzan && config.jumatSlideshowEnabled) {
                    finalMediaList = if (config.includeGlobalSlideshow) {
                        fridayMedia + mediaList
                    } else {
                        fridayMedia
                    }
                    tType = config.jumatTransitionType
                    sInterval = config.jumatSlideIntervalSeconds
                    vWait = config.jumatVideoWaitUntilEnd
                }

                AppUiState.Dashboard(
                    state = dashboardState,
                    customBackgroundPath = config.customBackgroundPath,
                    mediaList = finalMediaList,
                    dashboardBackgrounds = dashboardBackgrounds,
                    transitionType = tType,
                    slideIntervalSeconds = sInterval,
                    videoWaitUntilEnd = vWait,
                    includeDefaultImage = config.includeDefaultImage,
                    dashboardTransitionType = config.dashboardTransitionType,
                    dashboardTransitionInterval = config.dashboardTransitionInterval,
                    isFridayStandby = isFriday,
                    jumatWarningText = config.jumatWarningText,
                    isFridayBeforeAdzan = isFridayBeforeAdzan,
                    isFridayAfterAdzan = isFridayAfterAdzan
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
        isPreviewMode = false

        activeStateCollectorJob = viewModelScope.launch {
            combine(
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
        isPreviewMode = true

        activeStateCollectorJob = viewModelScope.launch {
            combine(
                dashboardViewModel.uiState,
                repository.getConfigFlow(),
                repository.getMediaListFlow(),
                repository.getDashboardBackgroundsFlow()
            ) { dashboardState, config, mediaList, dashboardBackgrounds ->
                AppUiState.Dashboard(
                    state = dashboardState.copy(isSlideshowActive = true),
                    customBackgroundPath = config.customBackgroundPath,
                    mediaList = mediaList,
                    dashboardBackgrounds = dashboardBackgrounds,
                    transitionType = config.transitionType,
                    slideIntervalSeconds = config.slideIntervalSeconds,
                    videoWaitUntilEnd = config.videoWaitUntilEnd,
                    includeDefaultImage = config.includeDefaultImage,
                    isPreviewMode = true,
                    dashboardTransitionType = config.dashboardTransitionType,
                    dashboardTransitionInterval = config.dashboardTransitionInterval
                )
            }.collect { state ->
                _appState.value = state
            }
        }

        viewModelScope.launch {
            delay(20000)
            if (_activeScreen.value is ActiveScreen.Dashboard && isPreviewMode) {
                exitPreview()
            }
        }
    }

    fun previewFridaySlideshow() {
        iqomahCollectorJob?.cancel()
        activeStateCollectorJob?.cancel()
        _activeScreen.value = ActiveScreen.Dashboard
        isPreviewMode = true

        val mappedFridayMediaFlow = repository.observeFridayMedia().map { fmList ->
            fmList.map { fm ->
                com.groovernapp.mosqueathan.data.MediaEntity().apply {
                    id = fm.id
                    filePath = fm.filePath
                    isVideo = fm.isVideo
                    sortOrder = fm.sortOrder
                }
            }
        }

        activeStateCollectorJob = viewModelScope.launch {
            combine(
                dashboardViewModel.uiState,
                repository.getConfigFlow(),
                repository.getMediaListFlow(),
                repository.getDashboardBackgroundsFlow(),
                mappedFridayMediaFlow
            ) { dashboardState, config, mediaList, dashboardBackgrounds, fridayMedia ->
                val finalMediaList = if (config.includeGlobalSlideshow) {
                    fridayMedia + mediaList
                } else {
                    fridayMedia
                }

                AppUiState.Dashboard(
                    state = dashboardState.copy(isSlideshowActive = true),
                    customBackgroundPath = config.customBackgroundPath,
                    mediaList = finalMediaList,
                    dashboardBackgrounds = dashboardBackgrounds,
                    transitionType = config.jumatTransitionType,
                    slideIntervalSeconds = config.jumatSlideIntervalSeconds,
                    videoWaitUntilEnd = config.jumatVideoWaitUntilEnd,
                    includeDefaultImage = config.includeDefaultImage,
                    isPreviewMode = true,
                    dashboardTransitionType = config.dashboardTransitionType,
                    dashboardTransitionInterval = config.dashboardTransitionInterval,
                    isFridayStandby = false,
                    jumatWarningText = config.jumatWarningText,
                    isFridayBeforeAdzan = true,
                    isFridayAfterAdzan = false
                )
            }.collect { state ->
                _appState.value = state
            }
        }

        viewModelScope.launch {
            delay(20.seconds)
            if (_activeScreen.value is ActiveScreen.Dashboard && isPreviewMode) {
                exitPreview()
            }
        }
    }

    fun exitPreview() {
        isPreviewMode = false
        transitionToSettings()
    }

    private fun isFridayModeActive(config: MosqueConfigEntity, now: LocalDateTime): Boolean {
        if (!config.jumatEnabled) return false
        if (now.dayOfWeek != kotlinx.datetime.DayOfWeek.FRIDAY) return false
        val currentMinutes = now.hour * 60 + now.minute
        val startMins = parseTimeToMinutes(config.jumatStartTime) ?: return false
        val endMins = parseTimeToMinutes(config.jumatEndTime) ?: return false
        return currentMinutes >= startMins && currentMinutes < endMins
    }

    fun forceStandbyMode() {
        forcedStandby = true
        _isStandbyActive.value = true
    }

    fun forceFridaySilence() {
        forcedFridaySilence = true
        if (currentConfig.jumatStandbyOnly) {
            forceStandbyMode()
        } else {
            updateStandbyStatus()
        }
    }

    fun exitStandbyMode() {
        forcedStandby = false
        _isStandbyActive.value = false
    }

    private fun updateStandbyStatus() {
        val config = currentConfig
        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(zone)

        if (config.jumatEnabled && now.dayOfWeek == kotlinx.datetime.DayOfWeek.FRIDAY) {
            val currentMins = now.hour * 60 + now.minute
            val endMins = parseTimeToMinutes(config.jumatEndTime)
            if (endMins != null && currentMins >= endMins) {
                forcedStandby = false
                forcedFridaySilence = false
            }
        }

        if (forcedStandby) {
            _isStandbyActive.value = true
            return
        }

        val currentScreen = _activeScreen.value
        if (currentScreen is ActiveScreen.Adzan || 
            currentScreen is ActiveScreen.Iqomah || 
            currentScreen is ActiveScreen.Settings
        ) {
            _isStandbyActive.value = false
            return
        }

        // Friday standby-only after adzan check (independent of general config.standbyModeEnabled)
        val isFriday = isFridayModeActive(config, now)
        if (isFriday && config.jumatStandbyOnly) {
            val prayerTimes = (dashboardViewModel.uiState.value as? DashboardUiState.Dashboard)
            if (prayerTimes != null) {
                val dzuhurTimeStr = prayerTimes.prayerTimes.dzuhur
                val dzuhurMins = parseTimeToMinutes(dzuhurTimeStr)
                val currentMins = now.hour * 60 + now.minute
                if (dzuhurMins != null && (currentMins >= dzuhurMins || forcedFridaySilence)) {
                    _isStandbyActive.value = true
                    return
                }
            }
        }

        if (!config.standbyModeEnabled) {
            if (_isStandbyActive.value) {
                android.util.Log.d("StandbyDebug", "Standby disabled in config, deactivating")
            }
            _isStandbyActive.value = false
            return
        }

        val standbySchedules = repository.getStandbySchedules()
        var shouldBeActive = false
        for (sched in standbySchedules) {
            if (com.groovernapp.mosqueathan.util.PrayerUtils.isScheduleActiveNew(
                    now = now,
                    activeDays = sched.activeDays,
                    specificDates = sched.specificDates,
                    startTime = sched.startTime,
                    endTime = sched.endTime,
                    legacyDay = sched.day,
                    legacyIsSpecificDate = sched.isSpecificDate
                )
            ) {
                shouldBeActive = true
                break
            }
        }

        if (shouldBeActive != _isStandbyActive.value) {
            android.util.Log.d("StandbyDebug", "Standby state changing: $shouldBeActive")
        }

        _isStandbyActive.value = shouldBeActive
    }

    private fun parseTimeToMinutes(timeStr: String): Int? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    override fun onCleared() {
        super.onCleared()
        coordinatorJob?.cancel()
        iqomahCollectorJob?.cancel()
        activeStateCollectorJob?.cancel()
    }
}
