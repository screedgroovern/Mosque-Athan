package com.groovernapp.mosqueathan.ui.display

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Plays the video at [path], filling its bounds, silently.
 *
 * Always muted: this renders on a mosque display where the adzan comes from the
 * building's own sound system, so a slideshow video with audio would talk over it.
 *
 * [onEnded] fires when playback finishes or fails — a failure has to advance the
 * slideshow too, or one unplayable file freezes the display on a black rectangle until
 * someone notices. When [loop] is true the video repeats instead and [onEnded] is not
 * called.
 */
@Composable
expect fun VideoSurface(
    path: String,
    loop: Boolean,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier
)
