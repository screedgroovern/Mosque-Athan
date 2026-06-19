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
import com.groovernapp.mosqueathan.data.DashboardBackgroundEntity
import com.groovernapp.mosqueathan.data.MediaEntity
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    customBackgroundPath: String,
    mediaList: List<MediaEntity>,
    dashboardBackgrounds: List<DashboardBackgroundEntity> = emptyList(),
    transitionType: String,
    slideIntervalSeconds: Int,
    videoWaitUntilEnd: Boolean,
    includeDefaultImage: Boolean,
    dashboardTransitionType: String = "FADE",
    dashboardTransitionInterval: Int = 10,
    onNavigateToSettings: () -> Unit
) {
    // --- Slideshow media list (for when isSlideshowActive) ---
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

    var currentSlideshowIndex by remember(slideshowList) { mutableStateOf(0) }

    if (slideshowList.isNotEmpty()) {
        val currentMedia = slideshowList.getOrNull(currentSlideshowIndex)
        val isVideo = currentMedia?.isVideo == true

        LaunchedEffect(currentSlideshowIndex, slideshowList, slideIntervalSeconds, videoWaitUntilEnd) {
            if (isVideo && videoWaitUntilEnd) {
                // Video completion listener will handle advancing the slide index.
            } else {
                delay(slideIntervalSeconds * 1000L)
                currentSlideshowIndex = (currentSlideshowIndex + 1) % slideshowList.size
            }
        }
    }

    // --- Dashboard backgrounds list (for when NOT isSlideshowActive) ---
    val activeDashboardBgs = remember(dashboardBackgrounds) {
        dashboardBackgrounds.filter { it.isUsed }
    }

    var currentDashboardBgIndex by remember(activeDashboardBgs) { mutableStateOf(0) }

    if (activeDashboardBgs.size > 1) {
        LaunchedEffect(currentDashboardBgIndex, activeDashboardBgs, dashboardTransitionInterval) {
            delay(dashboardTransitionInterval * 1000L)
            currentDashboardBgIndex = (currentDashboardBgIndex + 1) % activeDashboardBgs.size
        }
    }

    // Single customBackgroundPath fallback bitmap
    val customBgBitmap = remember(customBackgroundPath) {
        if (customBackgroundPath.isNotEmpty() && java.io.File(customBackgroundPath).exists()) {
            try {
                android.graphics.BitmapFactory.decodeFile(customBackgroundPath)?.asImageBitmap()
            } catch (e: Throwable) {
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
                val currentMedia = slideshowList.getOrNull(currentSlideshowIndex)
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
                                currentSlideshowIndex = (currentSlideshowIndex + 1) % slideshowList.size
                            }
                        )
                    }
                }
            } else {
                // Dashboard background mode — cycle through dashboardBackgrounds list
                if (activeDashboardBgs.isNotEmpty()) {
                    val currentBg = activeDashboardBgs.getOrNull(currentDashboardBgIndex)
                    if (currentBg != null) {
                        AnimatedContent(
                            targetState = currentBg,
                            transitionSpec = {
                                when (dashboardTransitionType) {
                                    "SLIDE" -> {
                                        (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(800))) togetherWith
                                                (slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(800)))
                                    }
                                    "ZOOM" -> {
                                        (scaleIn(initialScale = 0.92f) + fadeIn(animationSpec = tween(800))) togetherWith
                                                (scaleOut(targetScale = 1.08f) + fadeOut(animationSpec = tween(800)))
                                    }
                                    "INSTANT" -> {
                                        ContentTransform(
                                            initialContentExit = ExitTransition.None,
                                            targetContentEnter = EnterTransition.None
                                        )
                                    }
                                    else -> { // "FADE"
                                        fadeIn(animationSpec = tween(800)) togetherWith fadeOut(animationSpec = tween(800))
                                    }
                                }
                            },
                            label = "DashboardBgTransition"
                        ) { bg ->
                            DashboardBgSlide(filePath = bg.filePath)
                        }
                    }
                } else if (customBgBitmap != null) {
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
private fun DashboardBgSlide(filePath: String) {
    val bitmap = remember(filePath) {
        if (filePath.isNotEmpty() && java.io.File(filePath).exists()) {
            try {
                android.graphics.BitmapFactory.decodeFile(filePath)?.asImageBitmap()
            } catch (e: Throwable) {
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
                } catch (e: Throwable) {
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