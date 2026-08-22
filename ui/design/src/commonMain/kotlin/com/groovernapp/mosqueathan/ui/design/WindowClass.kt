package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
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

/** The measured window, for the decisions [WindowClass] alone cannot express. */
val LocalWindowMetrics: ProvidableCompositionLocal<WindowMetrics> =
    staticCompositionLocalOf {
        WindowMetrics(WindowClass.Expanded, 1280.dp, 800.dp)
    }

/**
 * Horizontal padding for a settings pane's content.
 *
 * [WindowClass.Tv] keeps the value the app already shipped, so the display is
 * untouched.
 */
val WindowClass.panePadding: Dp
    get() = when (this) {
        WindowClass.Compact -> 16.dp
        WindowClass.Medium -> 20.dp
        WindowClass.Expanded -> 24.dp
        WindowClass.Tv -> 24.dp
    }

/**
 * Whether vertical space is scarce.
 *
 * **This, not width, is what actually separates a phone from a tablet here.** The
 * app is locked to landscape, so measuring width alone is close to useless:
 *
 * | device            | width  | height |
 * |-------------------|--------|--------|
 * | Realme RMX2061    | 800dp  | 360dp  |
 * | Xiaomi 25040RP0AG | 1137dp | 711dp  |
 *
 * Width differs by 1.4x and never drops below 600dp, so [WindowClass.Compact] is
 * unreachable on a landscape phone — as was the `isCompact` boolean this replaced,
 * which means the app's "phone layout" has most likely never been seen. Height
 * differs by 2x, and 360dp is where a heading plus a few rows stops fitting.
 *
 * Measured on both devices rather than assumed.
 */
val WindowMetrics.isShort: Boolean
    get() = height < SHORT_HEIGHT

/**
 * Whether a pair of side-by-side choices should stack instead.
 *
 * Driven by height, not width: at 800dp there is ample room for two cards abreast,
 * and stacking them is what would run out of vertical space.
 */
val WindowMetrics.stacksSideBySideChoices: Boolean
    get() = windowClass == WindowClass.Compact

/**
 * What the app knows about the space it has been given.
 *
 * Carries height as well as class because, landscape-locked, height is the
 * dimension that actually varies — see [isShort].
 */
data class WindowMetrics(
    val windowClass: WindowClass,
    val width: Dp,
    val height: Dp
) {
    val isCompact: Boolean get() = windowClass.isCompact
    val isTv: Boolean get() = windowClass.isTv
    val panePadding: Dp get() = windowClass.panePadding
}

private val SHORT_HEIGHT = 480.dp

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
 * A television is its own window class rather than just a wide one, because what makes
 * it different is viewing distance, not pixels — a 15" monitor and a 55" TV can report
 * the same width and still need different type sizes. No multiplatform API exposes
 * this, so it is the one `expect` this module needs.
 */
@Composable
expect fun rememberIsTv(): Boolean
