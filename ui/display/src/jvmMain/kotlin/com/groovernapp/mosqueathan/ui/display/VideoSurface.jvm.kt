package com.groovernapp.mosqueathan.ui.display

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

/**
 * No video on the desktop build.
 *
 * The jvm target exists as the guardrail that catches Android code reaching
 * `commonMain`, and a desktop build is a controller rather than the mosque display, so
 * there is no slideshow to play. It still reports [onEnded] so a preview cannot wedge
 * on a slide that never advances.
 */
@Composable
actual fun VideoSurface(
    path: String,
    loop: Boolean,
    onEnded: () -> Unit,
    modifier: Modifier
) {
    if (!loop) {
        LaunchedEffect(path) { onEnded() }
    }
    Box(modifier)
}
