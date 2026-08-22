package com.groovernapp.mosqueathan.ui.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
    val windowClass = windowClassFor(widthDp, rememberIsTv())

    CompositionLocalProvider(
        LocalWindowClass provides windowClass,
        LocalWindowMetrics provides WindowMetrics(windowClass, widthDp, heightDp)
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
    val typography = MosqueTypography
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
