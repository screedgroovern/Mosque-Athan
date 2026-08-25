package com.groovernapp.mosqueathan.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.groovernapp.mosqueathan.ui.display.VideoSurface
import com.groovernapp.mosqueathan.ui.display.decodeImageFile
import com.groovernapp.mosqueathan.ui.display.displayBackgrounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.groovernapp.mosqueathan.domain.DashboardBackground
import com.groovernapp.mosqueathan.domain.Media
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    customBackgroundPath: String,
    mediaList: List<Media>,
    dashboardBackgrounds: List<DashboardBackground> = emptyList(),
    transitionType: String,
    slideIntervalSeconds: Int,
    videoWaitUntilEnd: Boolean,
    includeDefaultImage: Boolean,
    dashboardTransitionType: String = "FADE",
    dashboardTransitionInterval: Int = 10,
    dashboardRandomize: Boolean = false,
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
            val defaultMedia = Media(id = "default_bg", filePath = "", isVideo = false)
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
    // Keyed on the file paths, not on the Realm entities: the entity list is handed down as a fresh
    // list of frozen objects on every Realm version bump, which reset the rotation index back to 0
    // before the interval ever elapsed — the background then never changed.
    val activeBgPaths = dashboardBackgrounds.filter { it.isUsed }.map { it.filePath }

    var currentDashboardBgIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(activeBgPaths, dashboardTransitionInterval, dashboardRandomize) {
        if (currentDashboardBgIndex >= activeBgPaths.size) {
            currentDashboardBgIndex = 0
        }
        if (activeBgPaths.size < 2) return@LaunchedEffect

        val delayMs = dashboardTransitionInterval.coerceAtLeast(1).toLong() * 1000L
        while (true) {
            delay(delayMs)
            currentDashboardBgIndex = if (dashboardRandomize) {
                nextRandomIndex(currentDashboardBgIndex, activeBgPaths.size)
            } else {
                (currentDashboardBgIndex + 1) % activeBgPaths.size
            }
        }
    }

    // Single customBackgroundPath fallback bitmap
    val customBgBitmap = remember(customBackgroundPath) {
        decodeImageFile(customBackgroundPath)
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
                if (activeBgPaths.isNotEmpty()) {
                    val currentBgPath = activeBgPaths.getOrNull(currentDashboardBgIndex)
                        ?: activeBgPaths.first()
                    AnimatedContent(
                        targetState = currentBgPath,
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
                    ) { path ->
                        DashboardBgSlide(filePath = path)
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
                        painter = displayBackgrounds.dashboard,
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
                    .background(MosqueTheme.colors.scrim.copy(alpha = alpha))
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

private fun nextRandomIndex(currentIndex: Int, size: Int): Int {
    if (size < 2) return 0
    // Draw from the other slides only, so a random rotation never repeats the same background.
    val draw = Random.nextInt(size - 1)
    return if (draw >= currentIndex) draw + 1 else draw
}

@Composable
private fun DashboardBgSlide(filePath: String) {
    val bitmap = remember(filePath) {
        decodeImageFile(filePath)
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
            painter = displayBackgrounds.dashboard,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun MediaSlide(
    media: Media,
    videoWaitUntilEnd: Boolean,
    onVideoEnded: () -> Unit
) {
    if (media.isVideo) {
        key(media.id) {
            VideoSurface(
                path = media.filePath,
                loop = !videoWaitUntilEnd,
                onEnded = onVideoEnded,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        val bitmap = remember(media.filePath) {
            decodeImageFile(media.filePath)
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
                painter = displayBackgrounds.dashboard,
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

    MosqueTheme {
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
