package com.groovernapp.mosqueathan.ui.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * The app's theme: Material3's dark scheme plus this project's own tokens.
 *
 * Tokens are reached through [MosqueTheme], not through `MaterialTheme`, because the
 * app's palette is not a Material colour scheme — it is a gold accent over a set of
 * hand-picked near-blacks. Expressing it as `colorScheme` roles would have meant
 * changing colours.
 */
@Composable
fun MosqueTheme(
    content: @Composable () -> Unit
) {
    // LocalWindowInfo rather than BoxWithConstraints: this reads the window size
    // without introducing a layout node, so wrapping the app in the theme cannot
    // shift anything. It is also the same measurement the settings screens already
    // perform, and it survives the move to commonMain.
    val density = LocalDensity.current
    val container = LocalWindowInfo.current.containerSize
    val widthDp = with(density) { container.width.toDp() }
    val heightDp = with(density) { container.height.toDp() }
    val isTv = rememberIsTv()
    val windowClass = windowClassFor(widthDp, isTv)

    CompositionLocalProvider(
        LocalWindowClass provides windowClass,
        LocalWindowMetrics provides WindowMetrics(windowClass, widthDp, heightDp),
        LocalTypeScale provides displayTypeScaleFor(heightDp, isTv),
        LocalDisplayScale provides displayScaleFor(heightDp, isTv)
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(),
            content = content
        )
    }
}

/** Entry point for design tokens: `MosqueTheme.colors.accent`, `MosqueTheme.spacing.md`. */
object MosqueTheme {
    val colors = MosqueColors
    /**
     * Type roles, scaled for the window's height.
     *
     * Identical to the base scale at 600dp tall and above, so nothing changes on the
     * television or the tablet; on a phone in landscape the display sizes shrink so
     * that the prayer times still fit on screen.
     */
    val typography: TypeScale
        @Composable get() = LocalTypeScale.current

    /**
     * Multiplier for display-screen spacing. 1.0 on a television or tablet.
     *
     * Use it for padding and fixed heights on the dashboard, adzan and iqomah
     * screens, so vertical space shrinks with the type rather than against it.
     */
    val displayScale: Float
        @Composable get() = LocalDisplayScale.current
    val spacing = MosqueSpacing
    val size = MosqueSize
    val radius = MosqueRadius
    val stroke = MosqueStroke

    /** The current window class. Shorthand for [LocalWindowClass]. */
    val windowClass: WindowClass
        @Composable get() = LocalWindowClass.current

    /** The measured window, including height. */
    val window: WindowMetrics
        @Composable get() = LocalWindowMetrics.current
}

/** The active type scale. Provided by [MosqueTheme]; read it through `MosqueTheme.typography`. */
val LocalTypeScale: ProvidableCompositionLocal<TypeScale> =
    staticCompositionLocalOf { MosqueTypography }

/** The active display scale. Read it through `MosqueTheme.displayScale`. */
val LocalDisplayScale: ProvidableCompositionLocal<Float> = staticCompositionLocalOf { 1f }
