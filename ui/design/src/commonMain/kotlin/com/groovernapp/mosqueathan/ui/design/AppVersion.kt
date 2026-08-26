package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The running build's version name, for the settings screen to display.
 *
 * Provided by the host rather than read where it is shown, because every platform answers
 * differently — Android through `PackageManager`, a desktop build from its manifest, the
 * web from whatever the bundle was stamped with — and none of those belong in a screen.
 *
 * The same shape as `LocalDisplayBackgrounds`: the platform hands in what only it can
 * know, at the composition root.
 *
 * Defaults to a placeholder rather than throwing. A missing version string is a cosmetic
 * problem in one corner of one pane, unlike a missing background, which is the screen.
 */
val LocalAppVersion: ProvidableCompositionLocal<String> = staticCompositionLocalOf { "unknown" }

/** The running build's version name. Shorthand for [LocalAppVersion]. */
val appVersion: String
    @Composable get() = LocalAppVersion.current
