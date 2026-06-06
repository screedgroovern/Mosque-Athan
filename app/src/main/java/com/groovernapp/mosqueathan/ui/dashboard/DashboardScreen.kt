package com.groovernapp.mosqueathan.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.groovernapp.mosqueathan.R
import com.groovernapp.mosqueathan.data.MediaEntity
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    customBackgroundPath: String,
    mediaList: List<MediaEntity>,
    transitionType: String,
    slideIntervalSeconds: Int,
    videoWaitUntilEnd: Boolean,
    includeDefaultImage: Boolean,
    onNavigateToSettings: () -> Unit
) {
    val slideshowList = remember(mediaList, includeDefaultImage) {
        val list = mediaList.toMutableList()
        if (includeDefaultImage) {
            val defaultMedia = MediaEntity().apply {
                id = "default_bg"
                filePath = ""
                isVideo = false
            }
            list.add(0, defaultMedia)
        }
        list
    }

    var currentMediaIndex by remember(slideshowList) { mutableStateOf(0) }

    if (slideshowList.isNotEmpty()) {
        val currentMedia = slideshowList.getOrNull(currentMediaIndex)
        val isVideo = currentMedia?.isVideo == true

        LaunchedEffect(currentMediaIndex, slideshowList, slideIntervalSeconds, videoWaitUntilEnd) {
            if (isVideo && videoWaitUntilEnd) {
                // Video completion listener will handle advancing the slide index.
            } else {
                delay(slideIntervalSeconds * 1000L)
                currentMediaIndex = (currentMediaIndex + 1) % slideshowList.size
            }
        }
    }

    val customBgBitmap = remember(customBackgroundPath) {
        if (customBackgroundPath.isNotEmpty() && java.io.File(customBackgroundPath).exists()) {
            try {
                android.graphics.BitmapFactory.decodeFile(customBackgroundPath)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val isSlideshowActive = (state as? DashboardUiState.Dashboard)?.isSlideshowActive == true

        AnimatedContent(
            targetState = isSlideshowActive,
            transitionSpec = {
                fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
            },
            label = "BackgroundModeTransition"
        ) { isSlideshow ->
            if (isSlideshow && slideshowList.isNotEmpty()) {
                val currentMedia = slideshowList.getOrNull(currentMediaIndex)
                if (currentMedia != null) {
                    AnimatedContent(
                        targetState = currentMedia,
                        transitionSpec = {
                            when (transitionType) {
                                "SLIDE" -> {
                                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(500))) togetherWith
                                            (slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(500)))
                                }
                                "ZOOM" -> {
                                    (scaleIn(initialScale = 0.8f) + fadeIn(animationSpec = tween(500))) togetherWith
                                            (scaleOut(targetScale = 1.2f) + fadeOut(animationSpec = tween(500)))
                                }
                                "INSTANT" -> {
                                    ContentTransform(
                                        initialContentExit = ExitTransition.None,
                                        targetContentEnter = EnterTransition.None
                                    )
                                }
                                else -> { // "FADE"
                                    fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
                                }
                            }
                        },
                        label = "SlideshowTransition"
                    ) { media ->
                        MediaSlide(
                            media = media,
                            videoWaitUntilEnd = videoWaitUntilEnd,
                            onVideoEnded = {
                                currentMediaIndex = (currentMediaIndex + 1) % slideshowList.size
                            }
                        )
                    }
                }
            } else {
                if (customBgBitmap != null) {
                    Image(
                        bitmap = customBgBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.main_bg_default),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isSlideshowActive,
            enter = fadeIn(animationSpec = tween(600)),
            exit = fadeOut(animationSpec = tween(600))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        }

        when (state) {
            is DashboardUiState.Dashboard -> DashboardContent(state, customBackgroundPath, onNavigateToSettings)
            is DashboardUiState.Adzan -> AdzanContent(state)
            is DashboardUiState.Iqomah -> IqomahContent(state)
        }
    }
}

@Composable
private fun MediaSlide(
    media: MediaEntity,
    videoWaitUntilEnd: Boolean,
    onVideoEnded: () -> Unit
) {
    if (media.isVideo) {
        key(media.id) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { context ->
                    android.widget.VideoView(context).apply {
                        setVideoPath(media.filePath)
                        setOnPreparedListener { mediaPlayer ->
                            mediaPlayer.isLooping = !videoWaitUntilEnd
                            mediaPlayer.setVolume(0f, 0f) // Keep mosque displays silent
                            start()
                        }
                        setOnCompletionListener {
                            if (videoWaitUntilEnd) {
                                onVideoEnded()
                            }
                        }
                        setOnErrorListener { _, _, _ ->
                            if (videoWaitUntilEnd) {
                                onVideoEnded()
                            }
                            true
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        val bitmap = remember(media.filePath) {
            if (media.filePath.isNotEmpty() && java.io.File(media.filePath).exists()) {
                try {
                    android.graphics.BitmapFactory.decodeFile(media.filePath)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(R.drawable.main_bg_default),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}