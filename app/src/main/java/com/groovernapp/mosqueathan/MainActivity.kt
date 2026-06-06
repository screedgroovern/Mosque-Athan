package com.groovernapp.mosqueathan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.groovernapp.mosqueathan.ui.adzan.AdzanScreen
import com.groovernapp.mosqueathan.ui.adzan.AdzanViewModel
import com.groovernapp.mosqueathan.ui.dashboard.DashboardScreen
import com.groovernapp.mosqueathan.ui.dashboard.DashboardViewModel
import com.groovernapp.mosqueathan.ui.iqomah.IqomahScreen
import com.groovernapp.mosqueathan.ui.iqomah.IqomahViewModel
import com.groovernapp.mosqueathan.ui.navigation.AppUiState
import com.groovernapp.mosqueathan.ui.navigation.AppViewModel
import com.groovernapp.mosqueathan.ui.settings.SettingsScreen
import com.groovernapp.mosqueathan.ui.settings.SettingsViewModel
import com.groovernapp.mosqueathan.ui.theme.MosqueAthanTheme
import android.view.KeyEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    val appViewModel: AppViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val adzanViewModel: AdzanViewModel by viewModels()
    private val iqomahViewModel: IqomahViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val settingsSecretSequence = listOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_DOWN
    )
    private val inputKeyHistory = mutableListOf<Int>()
    private var settingsServer: com.groovernapp.mosqueathan.server.SettingsServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Enable fullscreen immersive mode
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        appViewModel.initialize(dashboardViewModel, adzanViewModel, iqomahViewModel)

        settingsServer = com.groovernapp.mosqueathan.server.SettingsServer(this).apply {
            start(8080)
        }

        setContent {
            MosqueAthanTheme {
                val appState by appViewModel.appState.collectAsState()
                val isStandbyActive by appViewModel.isStandbyActive.collectAsState()

                val window = this@MainActivity.window
                LaunchedEffect(isStandbyActive) {
                    if (isStandbyActive) {
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = appState) {
                    is AppUiState.Dashboard -> DashboardScreen(
                        state = state.state,
                        customBackgroundPath = state.customBackgroundPath,
                        mediaList = state.mediaList,
                        transitionType = state.transitionType,
                        slideIntervalSeconds = state.slideIntervalSeconds,
                        videoWaitUntilEnd = state.videoWaitUntilEnd,
                        includeDefaultImage = state.includeDefaultImage,
                        onNavigateToSettings = {
                            appViewModel.transitionToSettings()
                        }
                    )
                    is AppUiState.Adzan -> AdzanScreen(state = state.state, onDismiss = { appViewModel.transitionToSettings() })
                    is AppUiState.Iqomah -> IqomahScreen(state = state.state, onDismiss = { appViewModel.transitionToSettings() })
                    is AppUiState.Settings -> {
                        BackHandler {
                            appViewModel.transitionToDashboard()
                        }
                        val devices by settingsViewModel.devicesState.collectAsState()
                        val citySearchResults by settingsViewModel.citySearchResults.collectAsState()
                        val isSearchingCities by settingsViewModel.isSearchingCities.collectAsState()
                        val kemenagSchedules by settingsViewModel.kemenagSchedulesState.collectAsState()
                        val slideshowSchedules by settingsViewModel.slideshowSchedulesState.collectAsState()
                        SettingsScreen(
                            config = state.config,
                            announcements = state.announcements,
                            devices = devices,
                            citySearchResults = citySearchResults,
                            isSearchingCities = isSearchingCities,
                            kemenagSchedules = kemenagSchedules,
                            onSearchKemenagCities = { query -> settingsViewModel.searchKemenagCities(query) },
                            onSaveConfig = { latitude, longitude, iqomahSubuh, iqomahDzuhur, iqomahAshar, iqomahMaghrib, iqomahIsya, calculationMethodName, adzanLeadMinutes, offsetImsak, offsetSubuh, offsetSyuruq, offsetDzuhur, offsetAshar, offsetMaghrib, offsetIsya, kemenagCityId, kemenagCityName, kemenagDuration, kemenagTargetDate ->
                                settingsViewModel.updateConfig(
                                    latitude, longitude, iqomahSubuh, iqomahDzuhur, iqomahAshar, iqomahMaghrib, iqomahIsya, calculationMethodName, adzanLeadMinutes,
                                    offsetImsak, offsetSubuh, offsetSyuruq, offsetDzuhur, offsetAshar, offsetMaghrib, offsetIsya, kemenagCityId, kemenagCityName,
                                    kemenagDuration, kemenagTargetDate
                                )
                            },
                            onAddAnnouncement = { content ->
                                settingsViewModel.addAnnouncement(content)
                            },
                            onDeleteAnnouncement = { id ->
                                settingsViewModel.deleteAnnouncement(id)
                            },
                            onEditAnnouncement = { id, content ->
                                settingsViewModel.editAnnouncement(id, content)
                            },
                            onReorderAnnouncements = { ids ->
                                settingsViewModel.reorderAnnouncements(ids)
                            },
                            onResetToDefaults = {
                                settingsViewModel.resetToDefaults(this@MainActivity)
                            },
                            onSelectCustomBackground = { uri ->
                                settingsViewModel.updateCustomBackground(uri, this@MainActivity)
                            },
                            onClearCustomBackground = {
                                settingsViewModel.clearCustomBackground(this@MainActivity)
                            },
                            mediaList = state.mediaList,
                            onAddMediaItems = { uris ->
                                settingsViewModel.addMediaItems(uris, this@MainActivity)
                            },
                            onDeleteMediaItem = { item ->
                                settingsViewModel.deleteMediaItem(item)
                            },
                            onUpdateThemeSettings = { transitionType, intervalSeconds, videoWaitUntilEnd, includeDefaultImage ->
                                settingsViewModel.updateThemeSettings(transitionType, intervalSeconds, videoWaitUntilEnd, includeDefaultImage)
                            },
                            onUpdateShowSettingsOnTv = { show ->
                                settingsViewModel.updateShowSettingsOnTv(show)
                            },
                            onSetDeviceMaster = { deviceId ->
                                settingsViewModel.setDeviceMaster(deviceId)
                            },
                            onUpdatePinCode = { newPin ->
                                settingsViewModel.updatePinCode(newPin)
                            },
                            onDeleteDevice = { deviceId ->
                                settingsViewModel.deleteDevice(deviceId)
                            },
                            onUpdateFooters = { adzan, iqomah ->
                                settingsViewModel.updateFooters(adzan, iqomah)
                            },
                            onPreviewAdzan = {
                                appViewModel.previewAdzan()
                            },
                            onPreviewIqomah = {
                                appViewModel.previewIqomah()
                            },
                            onPreviewSlideshow = {
                                appViewModel.previewSlideshow()
                            },
                             onDownloadKemenagSchedules = { cityId, cityName, duration, targetDate ->
                                 settingsViewModel.updateConfig(
                                     latitude = state.config.latitude,
                                     longitude = state.config.longitude,
                                     iqomahSubuh = state.config.iqomahSubuh,
                                     iqomahDzuhur = state.config.iqomahDzuhur,
                                     iqomahAshar = state.config.iqomahAshar,
                                     iqomahMaghrib = state.config.iqomahMaghrib,
                                     iqomahIsya = state.config.iqomahIsya,
                                     calculationMethodName = "KEMENAG",
                                     adzanLeadMinutes = state.config.adzanLeadMinutes,
                                     offsetImsak = state.config.offsetImsak,
                                     offsetSubuh = state.config.offsetSubuh,
                                     offsetSyuruq = state.config.offsetSyuruq,
                                     offsetDzuhur = state.config.offsetDzuhur,
                                     offsetAshar = state.config.offsetAshar,
                                     offsetMaghrib = state.config.offsetMaghrib,
                                     offsetIsya = state.config.offsetIsya,
                                     kemenagCityId = cityId,
                                     kemenagCityName = cityName,
                                     kemenagDuration = duration,
                                     kemenagTargetDate = targetDate
                                 )
                             },
                            slideshowSchedules = slideshowSchedules,
                            onAddSlideshowSchedule = { startTime, endTime ->
                                settingsViewModel.addSlideshowSchedule(startTime, endTime)
                            },
                            onDeleteSlideshowSchedule = { id ->
                                settingsViewModel.deleteSlideshowSchedule(id)
                            },
                            onSaveStandby = { enabled, startTime, endTime ->
                                settingsViewModel.updateStandbySettings(enabled, startTime, endTime)
                            },
                            onBack = {
                                appViewModel.transitionToDashboard()
                            }
                        )
                    }
                }
                } // End of parent Box

                if (isStandbyActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .clickable(enabled = false) {}
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (appViewModel.activeScreen.value is AppViewModel.ActiveScreen.Dashboard) {
            if (keyCode in listOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN)) {
                inputKeyHistory.add(keyCode)
                if (inputKeyHistory.size > settingsSecretSequence.size) {
                    inputKeyHistory.removeAt(0)
                }
                if (inputKeyHistory == settingsSecretSequence) {
                    inputKeyHistory.clear()
                    appViewModel.transitionToSettings()
                    return true
                }
            } else {
                inputKeyHistory.clear()
            }
        } else {
            inputKeyHistory.clear()
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        settingsServer?.stop()
    }

}

