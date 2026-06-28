package com.groovernapp.mosqueathan.ui.dashboard

import android.graphics.BitmapFactory
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
import androidx.compose.ui.tooling.preview.Preview
import com.groovernapp.mosqueathan.R
import com.groovernapp.mosqueathan.data.DashboardBackgroundEntity
import com.groovernapp.mosqueathan.data.MediaEntity
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.ui.theme.MosqueAthanTheme
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

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
    isFridayStandby: Boolean = false,
    jumatWarningText: String = "",
    isFridayBeforeAdzan: Boolean = false,
    isFridayAfterAdzan: Boolean = false,
    onNavigateToSettings: () -> Unit,
    onStartAdzanNow: () -> Unit = {},
    onForceFridaySilence: () -> Unit = {}
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
            val delayMs = dashboardTransitionInterval.coerceAtLeast(1).toLong() * 1000L
            delay(delayMs)
            currentDashboardBgIndex = (currentDashboardBgIndex + 1) % activeDashboardBgs.size
        }
    }

    // Single customBackgroundPath fallback bitmap
    val customBgBitmap = remember(customBackgroundPath) {
        if (customBackgroundPath.isNotEmpty() && File(customBackgroundPath).exists()) {
            try {
                BitmapFactory.decodeFile(customBackgroundPath)?.asImageBitmap()
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
            val alpha = if (isFridayAfterAdzan) 0.65f else if (isFridayStandby) 0.30f else 0.45f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = alpha))
            )
        }

        when (state) {
            is DashboardUiState.Dashboard -> DashboardContent(
                state = state,
                customBackgroundPath = customBackgroundPath,
                isFridayStandby = isFridayStandby,
                jumatWarningText = jumatWarningText,
                isFridayBeforeAdzan = isFridayBeforeAdzan,
                isFridayAfterAdzan = isFridayAfterAdzan,
                onNavigateToSettings = onNavigateToSettings,
                onStartAdzanNow = onStartAdzanNow,
                onForceFridaySilence = onForceFridaySilence
            )
            is DashboardUiState.Adzan -> AdzanContent(state)
            is DashboardUiState.Iqomah -> IqomahContent(state)
        }
    }
}

@Composable
private fun DashboardBgSlide(filePath: String) {
    val bitmap = remember(filePath) {
        if (filePath.isNotEmpty() && File(filePath).exists()) {
            try {
                BitmapFactory.decodeFile(filePath)?.asImageBitmap()
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
            if (media.filePath.isNotEmpty() && File(media.filePath).exists()) {
                try {
                    BitmapFactory.decodeFile(media.filePath)?.asImageBitmap()
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

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
fun DashboardScreenPreview() {
    val samplePrayerTimes = PrayerTimesUiModel(
        imsak = "04:15",
        subuh = "04:25",
        syuruq = "05:40",
        dzuhur = "12:00",
        ashar = "15:15",
        maghrib = "18:10",
        isya = "19:20",
        highlightedPrayer = PrayerType.DZUHUR
    )

    val sampleNextPrayer = NextPrayerUiModel(
        prayerType = PrayerType.ASHAR,
        prayerTime = "15:15",
        countdown = "02:45:10"
    )

    val sampleState = DashboardUiState.Dashboard(
        currentTime = "12:30",
        gregorianDate = "Senin, 1 Januari 2024",
        hijriDate = "19 Jumadil Akhir 1445 H",
        prayerTimes = samplePrayerTimes,
        nextPrayer = sampleNextPrayer,
        runningText = "Selamat datang di Masjid Agung. Mohon matikan handphone selama berada di dalam masjid.",
        isSlideshowActive = false
    )

    MosqueAthanTheme {
        DashboardScreen(
            state = sampleState,
            customBackgroundPath = "",
            mediaList = emptyList(),
            dashboardBackgrounds = emptyList(),
            transitionType = "FADE",
            slideIntervalSeconds = 10,
            videoWaitUntilEnd = false,
            includeDefaultImage = true,
            isFridayStandby = false,
            jumatWarningText = "",
            isFridayBeforeAdzan = true,
            isFridayAfterAdzan = true,
            onNavigateToSettings = {},
            onStartAdzanNow = {}
        )
    }
}