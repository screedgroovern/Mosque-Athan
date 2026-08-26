package com.groovernapp.mosqueathan.ui.display

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

/**
 * No video in the browser build, and this one is a gap rather than a decision.
 *
 * A browser plays video perfectly well — but Compose for web renders into a canvas, so a
 * `<video>` element has to be composited with that canvas rather than laid out inside it.
 * That is the same obstacle as the camera preview behind `rememberQrScanner`, and it is
 * the reason CLAUDE.md calls the web display "best-effort preview" rather than a display.
 *
 * It still reports [onEnded] so a preview cannot wedge on a slide that never advances —
 * the slideshow keeps moving, showing nothing where the video would be.
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
