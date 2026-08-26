package com.groovernapp.mosqueathan.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.settings.SettingsScreen
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel

/**
 * The browser controller's entry point.
 *
 * It renders the settings UI and nothing else, on purpose: settings is what the
 * hand-written web panel duplicates, and sharing one implementation is the whole reason
 * this module exists. The display screens are a later, best-effort preview.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        KoinApplication(application = { modules(webModule) }) {
            MosqueTheme {
                SettingsScreen(
                    viewModel = koinViewModel(),
                    // Nothing to go back to: in the browser the settings UI *is* the app,
                    // where on the television it is a screen you leave to return to the
                    // clock.
                    onBack = {}
                )
            }
        }
    }
}
