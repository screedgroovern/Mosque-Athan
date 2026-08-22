package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager

/**
 * Puts the composition into non-touch input mode, which is what a television is
 * always in.
 *
 * This matters more than it looks. Compose deliberately excludes `clickable` nodes
 * from focus while in **touch** input mode — you do not want a focus ring appearing
 * because someone tapped the screen. A test running on a phone starts in touch mode,
 * so every focus assertion fails there while the same code works correctly on a TV.
 *
 * Call this at the top of `setContent` in any test that asserts focus.
 */
@Composable
fun RequestDpadInputMode() {
    val inputMode = LocalInputModeManager.current
    SideEffect {
        @Suppress("DEPRECATION_ERROR")
        inputMode.requestInputMode(InputMode.Keyboard)
    }
}
