package com.groovernapp.mosqueathan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable fullscreen immersive mode
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        appViewModel.initialize(dashboardViewModel, adzanViewModel, iqomahViewModel)

        setContent {
            MosqueAthanTheme {
                val appState by appViewModel.appState.collectAsState()
                when (val state = appState) {
                    is AppUiState.Dashboard -> DashboardScreen(
                        state = state.state,
                        customBackgroundPath = state.customBackgroundPath,
                        mediaList = state.mediaList,
                        transitionType = state.transitionType,
                        slideIntervalSeconds = state.slideIntervalSeconds,
                        videoWaitUntilEnd = state.videoWaitUntilEnd,
                        onNavigateToSettings = {
                            appViewModel.transitionToSettings()
                        }
                    )
                    is AppUiState.Adzan -> AdzanScreen(state = state.state)
                    is AppUiState.Iqomah -> IqomahScreen(state = state.state)
                    is AppUiState.Settings -> {
                        BackHandler {
                            appViewModel.transitionToDashboard()
                        }
                        SettingsScreen(
                            config = state.config,
                            announcements = state.announcements,
                            mediaList = state.mediaList,
                            onSaveConfig = { latitude, longitude, iqomahSubuh, iqomahDzuhur, iqomahAshar, iqomahMaghrib, iqomahIsya, calculationMethodName, adzanLeadMinutes ->
                                settingsViewModel.updateConfig(
                                    latitude, longitude, iqomahSubuh, iqomahDzuhur, iqomahAshar, iqomahMaghrib, iqomahIsya, calculationMethodName, adzanLeadMinutes
                                )
                            },
                            onAddAnnouncement = { content ->
                                settingsViewModel.addAnnouncement(content)
                            },
                            onDeleteAnnouncement = { id ->
                                settingsViewModel.deleteAnnouncement(id)
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
                            onAddMediaItem = { uri ->
                                settingsViewModel.addMediaItem(uri, this@MainActivity)
                            },
                            onDeleteMediaItem = { item ->
                                settingsViewModel.deleteMediaItem(item)
                            },
                            onUpdateThemeSettings = { transitionType, intervalSeconds, videoWaitUntilEnd ->
                                settingsViewModel.updateThemeSettings(transitionType, intervalSeconds, videoWaitUntilEnd)
                            },
                            onBack = {
                                appViewModel.transitionToDashboard()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_S) {
            appViewModel.toggleSettings()
            return true
        }

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


}

