package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.Composable

/**
 * Does nothing, and that is the decision rather than an unwritten implementation.
 *
 * A desktop window has no system back button to intercept — there is no dispatcher to
 * register with and no gesture to consume. A screen that also wants a visible "back"
 * affordance must draw one; that is the screen's business, not this seam's.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
}
