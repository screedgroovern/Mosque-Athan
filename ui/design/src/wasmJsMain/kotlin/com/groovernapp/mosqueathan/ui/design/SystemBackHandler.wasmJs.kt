package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.Composable

/**
 * Not wired up yet, and deliberately left visible as such.
 *
 * A browser *does* have a back gesture, through `history.pushState` and `popstate`, so
 * unlike the desktop actual this one is a gap rather than a decision. Wiring it means
 * deciding what a settings drawer should do to the page's history — whether closing it is
 * a navigation the user can go back through — and that is a product question, not a
 * porting one.
 *
 * Until it is answered, the browser's back button leaves the page instead of closing the
 * drawer. That is worse than the Android behaviour and is why this comment exists.
 */
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Intentionally empty; see above.
}
