package com.groovernapp.mosqueathan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.res.painterResource
import com.groovernapp.mosqueathan.ui.design.LocalAppVersion
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.display.DisplayBackgrounds
import com.groovernapp.mosqueathan.ui.display.LocalDisplayBackgrounds

class MainActivity : ComponentActivity() {

    val appViewModel: AppViewModel by viewModel()
    private val dashboardViewModel: DashboardViewModel by viewModel()
    private val adzanViewModel: AdzanViewModel by viewModel()
    private val iqomahViewModel: IqomahViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()

    private val secretSequence = com.groovernapp.mosqueathan.ui.navigation.SecretSequence(
        listOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_DOWN
        )
    )
    private val configRepository: com.groovernapp.mosqueathan.data.MosqueConfigRepository by inject()
    private var settingsServer: com.groovernapp.mosqueathan.server.SettingsServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Enable fullscreen immersive mode
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        appViewModel.initialize(dashboardViewModel, adzanViewModel, iqomahViewModel)

        settingsServer = com.groovernapp.mosqueathan.server.SettingsServer(this, configRepository).apply {
            start(8080)
        }

        setContent {
            MosqueAthanTheme {
                // The three built-in backgrounds are Android drawables and stay in :app —
                // SettingsServer still serves main_bg_default to the web panel as raw
                // bytes, so it has to remain reachable as a resource. ui:display receives
                // them as Painters instead of knowing about R.
                CompositionLocalProvider(
                    // Only the platform can answer this, so it is handed in here rather
                    // than read from the screen that shows it.
                    LocalAppVersion provides appVersionName(),
                    LocalDisplayBackgrounds provides DisplayBackgrounds(
                        dashboard = painterResource(R.drawable.main_bg_default),
                        adzan = painterResource(R.drawable.adzan_bg_default),
                        iqomah = painterResource(R.drawable.iqomah_bg_default)
                    )
                ) {
                    val appState by appViewModel.appState.collectAsState()
                    val isStandbyActive by appViewModel.isStandbyActive.collectAsState()

                    val window = this@MainActivity.window
                    LaunchedEffect(isStandbyActive) {
                        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        val params = window.attributes
                        if (isStandbyActive) {
                            params.screenBrightness = 0.0f
                        } else {
                            params.screenBrightness =
                                android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        }
                        window.attributes = params
                    }

                    LaunchedEffect(appState is AppUiState.Settings) {
                        if (appState is AppUiState.Settings) {
                            this@MainActivity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        } else {
                            this@MainActivity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (val state = appState) {
                            is AppUiState.Dashboard -> {
                                if (state.isPreviewMode) {
                                    BackHandler {
                                        appViewModel.exitPreview()
                                    }
                                }
                                DashboardScreen(
                                    state = state.state,
                                    customBackgroundPath = state.customBackgroundPath,
                                    mediaList = state.mediaList,
                                    dashboardBackgrounds = state.dashboardBackgrounds,
                                    transitionType = state.transitionType,
                                    slideIntervalSeconds = state.slideIntervalSeconds,
                                    videoWaitUntilEnd = state.videoWaitUntilEnd,
                                    includeDefaultImage = state.includeDefaultImage,
                                    dashboardTransitionType = state.dashboardTransitionType,
                                    dashboardTransitionInterval = state.dashboardTransitionInterval,
                                    dashboardRandomize = state.dashboardRandomize,
                                    isFridayStandby = state.isFridayStandby,
                                    jumatWarningText = state.jumatWarningText,
                                    isFridayBeforeAdzan = state.isFridayBeforeAdzan,
                                    isFridayAfterAdzan = state.isFridayAfterAdzan,
                                    onNavigateToSettings = {
                                        appViewModel.transitionToSettings()
                                    },
                                    onStartAdzanNow = {
                                        appViewModel.forceStandbyMode()
                                    },
                                    onForceFridaySilence = {
                                        appViewModel.forceFridaySilence()
                                    }
                                )
                            }

                            is AppUiState.Adzan -> {
                                if (appViewModel.isPreviewMode) {
                                    BackHandler {
                                        appViewModel.exitPreview()
                                    }
                                }
                                AdzanScreen(
                                    state = state.state,
                                    onDismiss = { appViewModel.transitionToSettings() }
                                )
                            }

                            is AppUiState.Iqomah -> {
                                if (appViewModel.isPreviewMode) {
                                    BackHandler {
                                        appViewModel.exitPreview()
                                    }
                                }
                                IqomahScreen(
                                    state = state.state,
                                    onDismiss = { appViewModel.transitionToSettings() }
                                )
                            }

                            is AppUiState.Settings -> {
                                BackHandler {
                                    appViewModel.transitionToDashboard()
                                }
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onPreviewAdzan = {
                                        appViewModel.previewAdzan()
                                    },
                                    onPreviewIqomah = {
                                        appViewModel.previewIqomah()
                                    },
                                    onPreviewSlideshow = {
                                        appViewModel.previewSlideshow()
                                    },
                                    onPreviewFridaySlideshow = {
                                        appViewModel.previewFridaySlideshow()
                                    },
                                    onBack = {
                                        appViewModel.transitionToDashboard()
                                    }
                                )
                            }
                        } // End of when
                    
                        if (isStandbyActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MosqueTheme.colors.scrim)
                                    .pointerInput(Unit) {
                                        detectTapGestures { }
                                    }
                            )
                        }
                    } // End of parent Box
                }
                }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val wasStandby = appViewModel.isStandbyActive.value
        if (wasStandby) {
            appViewModel.exitStandbyMode()
        }

        // The press that wakes the screen still counts towards the sequence.
        //
        // It used to return here, and that made settings unreachable for the six
        // hours standby is scheduled: exitStandbyMode only clears the flag, while
        // the coordinator loop re-enters standby a second later because the clock is
        // still inside the window. Every press was swallowed, so UP UP DOWN DOWN
        // never recorded a single entry — on a device whose only input is a remote.
        if (appViewModel.activeScreen.value is AppViewModel.ActiveScreen.Dashboard) {
            if (secretSequence.accept(keyCode)) {
                appViewModel.transitionToSettings()
                return true
            }
        } else {
            secretSequence.reset()
        }

        // Waking from standby should not also act on whatever was pressed.
        return if (wasStandby) true else super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        settingsServer?.stop()
    }

    /** The build's version name, or a placeholder if the package manager will not say. */
    private fun appVersionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: UNKNOWN_VERSION
    } catch (e: Exception) {
        UNKNOWN_VERSION
    }

    private companion object {
        const val UNKNOWN_VERSION = "unknown"
    }
}
