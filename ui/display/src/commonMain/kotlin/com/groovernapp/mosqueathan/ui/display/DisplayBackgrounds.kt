package com.groovernapp.mosqueathan.ui.display

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter

/**
 * The three built-in background images, supplied by the platform that owns them.
 *
 * These are Android drawables today, and `main_bg_default` cannot simply move here:
 * `SettingsServer` serves the same file as raw bytes over HTTP to the web panel, so it
 * has to stay reachable as an Android resource. Passing the images in as `Painter`s
 * leaves them where the server can still read them while keeping this module free of
 * `R`.
 *
 * A `CompositionLocal` rather than a parameter for the same reason as
 * `LocalWindowClass`: the dashboard needs its default background three levels down, and
 * threading an argument through every layer to reach one leaf is what the theme tokens
 * already avoid.
 *
 * Compose Multiplatform resources are the eventual home for all three — but only once
 * the panel stops needing raw bytes, which is step 7's business.
 */
class DisplayBackgrounds(
    val dashboard: Painter,
    val adzan: Painter,
    val iqomah: Painter
)

/**
 * Read it through [displayBackgrounds]. There is no sensible default — a missing
 * provider is a wiring mistake, and a blank screen on a mosque wall is a worse way to
 * find out than an exception during development.
 */
val LocalDisplayBackgrounds: ProvidableCompositionLocal<DisplayBackgrounds?> =
    staticCompositionLocalOf { null }

/** The built-in backgrounds. Throws if the host forgot to provide them. */
val displayBackgrounds: DisplayBackgrounds
    @Composable get() = LocalDisplayBackgrounds.current
        ?: error(
            "No DisplayBackgrounds provided. Wrap the display screens in " +
                "CompositionLocalProvider(LocalDisplayBackgrounds provides ...)."
        )
