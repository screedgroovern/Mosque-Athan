package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.Composable

/**
 * Intercepts the platform's own "go back" gesture while [enabled].
 *
 * Only the platform knows what going back means, and on most of them it means nothing:
 * `androidx.activity.compose.BackHandler` is tied to Android's
 * `OnBackPressedDispatcher`, which no other target has. So this is an `expect` rather
 * than something built on a shared abstraction — there is no shared thing to abstract.
 *
 * Nesting follows Android's rule: the innermost enabled handler wins, and disabling one
 * hands the press to whatever is behind it.
 */
@Composable
expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)
