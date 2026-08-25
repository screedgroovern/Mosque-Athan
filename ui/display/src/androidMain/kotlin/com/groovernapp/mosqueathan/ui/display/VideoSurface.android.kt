package com.groovernapp.mosqueathan.ui.display

import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun VideoSurface(
    path: String,
    loop: Boolean,
    onEnded: () -> Unit,
    modifier: Modifier
) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoPath(path)
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = loop
                    mediaPlayer.setVolume(0f, 0f)
                    start()
                }
                setOnCompletionListener { if (!loop) onEnded() }
                setOnErrorListener { _, _, _ ->
                    if (!loop) onEnded()
                    true
                }
            }
        },
        modifier = modifier
    )
}
