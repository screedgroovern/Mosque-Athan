package com.groovernapp.mosqueathan.ui.design

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How much room the UI has, and at what viewing distance.
 *
 * A TV is not simply a wide screen. A 15" monitor at 60cm and a 43" TV at 3m can both
 * report 1920px, but they need different type scales — the variable is viewing
 * distance, which no width breakpoint can express. So [Tv] is its own class regardless
 * of width, rather than collapsing into [Expanded].
 */
enum class WindowClass {
    /** Phones. Width < 600dp. */
    Compact,

    /** 10" tablets in portrait. 600dp ≤ width < 840dp. */
    Medium,

    /** 10" tablets in landscape, 15" screens. Width ≥ 840dp. */
    Expanded,

    /** Television, at any width. Larger type, D-pad focus. */
    Tv;

    /**
     * The phone layout switch.
     *
     * Replaces the `isCompact` boolean that was computed in three different places
     * (inconsistently — only one of them excluded TVs) and then threaded by hand
     * through roughly 86 call sites.
     */
    val isCompact: Boolean get() = this == Compact

    val isTv: Boolean get() = this == Tv
}

/**
 * The current [WindowClass].
 *
 * Read this instead of accepting an `isCompact` parameter. Components that read tokens
 * do not need it at all: the tokens themselves resolve per class, so a button never has
 * to ask whether it is on a TV.
 */
val LocalWindowClass: ProvidableCompositionLocal<WindowClass> =
    staticCompositionLocalOf { WindowClass.Expanded }

/**
 * Horizontal padding for a settings pane's content.
 *
 * [WindowClass.Tv] keeps the value the app already shipped, so the display is
 * untouched; the phone and tablet tiers are the part that had never been thought
 * about — before this there was one breakpoint and everything above it got 24dp.
 */
val WindowClass.panePadding: Dp
    get() = when (this) {
        WindowClass.Compact -> 16.dp
        WindowClass.Medium -> 20.dp
        WindowClass.Expanded -> 24.dp
        WindowClass.Tv -> 24.dp
    }

/**
 * Whether a pair of side-by-side choices should stack instead.
 *
 * True only on a phone, where two cards in a row leaves each too narrow to read.
 */
val WindowClass.stacksSideBySideChoices: Boolean
    get() = this == WindowClass.Compact

/** Classifies a measured width. Kept separate from measurement so it is testable. */
fun windowClassFor(width: Dp, isTv: Boolean): WindowClass = when {
    isTv -> WindowClass.Tv
    width < 600.dp -> WindowClass.Compact
    width < 840.dp -> WindowClass.Medium
    else -> WindowClass.Expanded
}

/**
 * Whether this device reports itself as a television.
 *
 * Android-only today. When `ui:design` becomes multiplatform in step 6 this is the one
 * piece here that needs `expect`/`actual`.
 */
@Composable
fun rememberIsTv(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.isTelevision() }
}

private fun Context.isTelevision(): Boolean {
    val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}
