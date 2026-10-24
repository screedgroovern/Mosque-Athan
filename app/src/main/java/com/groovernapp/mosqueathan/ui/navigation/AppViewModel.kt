package com.groovernapp.mosqueathan.ui.navigation

import com.groovernapp.mosqueathan.domain.DisplayDataSource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.domain.PreviewKind
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

class AppViewModel(
    private val repository: DisplayDataSource,
    private val kemenagSource: com.groovernapp.mosqueathan.domain.KemenagScheduleSource,
    private val murottal: com.groovernapp.mosqueathan.audio.MurottalPlayer
) : ViewModel() {


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
    private val _activePreview = MutableStateFlow<PreviewKind?>(null)

    /**
     * The preview on screen, if any. The server reports it in every snapshot, so a controller can
     * turn the right Preview button into Stop, and drop it again when the display ends a preview
     * itself.
     */
    val activePreview: StateFlow<PreviewKind?> = _activePreview.asStateFlow()

    val isPreviewMode: Boolean get() = _activePreview.value != null

    /** The screen a preview interrupted, which ending the preview goes back to. */
    private var screenBeforePreview: ActiveScreen? = null
    private var currentConfig = MosqueConfig()

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
                updateMurottal()
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
            kemenagSource = kemenagSource,
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
                    _activePreview.value = null
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
                        _activePreview.value = null
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

    private fun getIqomahMinutesFromConfig(prayerType: PrayerType, config: MosqueConfig): Int {
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
        // Immediately, not on the next tick: the recitation must not overlap the call to prayer
        // even by a second, and the adzan screen appears `adzanLeadMinutes` *before* the time
        // `shouldPlayMurottal` compares against.
        murottal.stop()
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
                                exitPreview()
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
                                exitPreview()
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
        _activePreview.value = null

        val mappedFridayMediaFlow = repository.observeFridayMedia().map { fmList: List<com.groovernapp.mosqueathan.domain.FridayMedia> ->
            fmList.map { fm ->
                com.groovernapp.mosqueathan.domain.Media(id = fm.id, filePath = fm.filePath, isVideo = fm.isVideo, sortOrder = fm.sortOrder)
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

                if (isFriday) {
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

                // While Friday mode owns the screen only the dedicated Friday slideshow may cover the
                // dashboard. A general slideshow schedule overlapping the Friday window would
                // otherwise keep painting its media and hide the dimming overlay.
                val effectiveState = if (isFriday) {
                    dashboardState.copy(
                        isSlideshowActive = isFridayBeforeAdzan && config.jumatSlideshowEnabled
                    )
                } else {
                    dashboardState
                }

                AppUiState.Dashboard(
                    state = effectiveState,
                    customBackgroundPath = config.customBackgroundPath,
                    mediaList = finalMediaList,
                    dashboardBackgrounds = dashboardBackgrounds,
                    transitionType = tType,
                    slideIntervalSeconds = sInterval,
                    videoWaitUntilEnd = vWait,
                    includeDefaultImage = config.includeDefaultImage,
                    dashboardTransitionType = config.dashboardTransitionType,
                    dashboardTransitionInterval = config.dashboardTransitionInterval,
                    dashboardRandomize = config.dashboardRandomize,
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
        _activePreview.value = null

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
        startPreview(PreviewKind.Adzan)
        val zone = TimeZone.currentSystemDefault()
        val simulatedTime = Clock.System.now().plus(20, DateTimeUnit.SECOND).toLocalDateTime(zone)
        transitionToAdzan(PrayerType.SUBUH, simulatedTime)
    }

    fun previewIqomah() {
        startPreview(PreviewKind.Iqomah)
        transitionToIqomah(PrayerType.SUBUH, customDurationSeconds = 20)
    }

    fun previewSlideshow() {
        // Before the screen changes: this is where ending the preview returns to.
        startPreview(PreviewKind.Slideshow)
        iqomahCollectorJob?.cancel()
        activeStateCollectorJob?.cancel()
        _activeScreen.value = ActiveScreen.Dashboard

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
                    dashboardTransitionInterval = config.dashboardTransitionInterval,
                    dashboardRandomize = config.dashboardRandomize
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
        startPreview(PreviewKind.FridaySlideshow)
        iqomahCollectorJob?.cancel()
        activeStateCollectorJob?.cancel()
        _activeScreen.value = ActiveScreen.Dashboard

        val mappedFridayMediaFlow = repository.observeFridayMedia().map { fmList ->
            fmList.map { fm ->
                com.groovernapp.mosqueathan.domain.Media(id = fm.id, filePath = fm.filePath, isVideo = fm.isVideo, sortOrder = fm.sortOrder)
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
                    dashboardRandomize = config.dashboardRandomize,
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

    /**
     * Ends the preview and goes back to the screen it interrupted.
     *
     * That used to be Settings every time, which was wrong in the case that matters: a controller
     * previews on a television that is showing the clock, and the television was left on its own
     * settings screen afterwards. Settings is only the answer when the preview started there.
     */
    fun exitPreview() {
        if (!isPreviewMode) return
        val back = screenBeforePreview
        screenBeforePreview = null
        _activePreview.value = null
        if (back is ActiveScreen.Settings) transitionToSettings() else transitionToDashboard()
    }

    private fun startPreview(kind: PreviewKind) {
        // Only the first preview of a run records where it came from: moving straight from one
        // preview to another would otherwise store the first preview as the place to return to.
        if (!isPreviewMode) screenBeforePreview = _activeScreen.value
        _activePreview.value = kind
    }

    /**
     * Starts and stops the pre-adzan recitation.
     *
     * Called once a second from the coordinator, and deliberately written as "should it be playing
     * right now" rather than as two edges. An edge-triggered version has to be right about every
     * way the window can be entered or left — the date rolling over, the config changing under it,
     * the app being restarted mid-window by [com.groovernapp.mosqueathan.BootReceiver] — and
     * getting any of those wrong leaves a television reciting through the sermon.
     * [MurottalPlayer.start] and [MurottalPlayer.stop] both ignore calls that change nothing.
     */
    private fun updateMurottal() {
        val config = currentConfig
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val shouldPlay = com.groovernapp.mosqueathan.audio.shouldPlayMurottal(
            config = config,
            now = now,
            dzuhur = prayerResult?.dzuhur,
            forcedSilence = forcedFridaySilence
        )
        if (shouldPlay) {
            murottal.start(config.murottalAudioPaths.split(","))
        } else {
            murottal.stop()
        }
    }

    private fun isFridayModeActive(config: MosqueConfig, now: LocalDateTime): Boolean {
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

    /** Delegates to the one in `audio`, which the murottal schedule tests cover. */
    private fun parseTimeToMinutes(timeStr: String): Int? =
        com.groovernapp.mosqueathan.audio.minutesOfDay(timeStr)

    override fun onCleared() {
        murottal.stop()
        super.onCleared()
        coordinatorJob?.cancel()
        iqomahCollectorJob?.cancel()
        activeStateCollectorJob?.cancel()
    }
}
