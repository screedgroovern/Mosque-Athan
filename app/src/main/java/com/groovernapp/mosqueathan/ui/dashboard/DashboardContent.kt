package com.groovernapp.mosqueathan.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.withTimeoutOrNull
import com.groovernapp.mosqueathan.ui.component.ClockSection
import com.groovernapp.mosqueathan.ui.component.NextPrayerHeader
import com.groovernapp.mosqueathan.ui.component.PrayerTimesPanel
import com.groovernapp.mosqueathan.ui.component.RunningTextBar
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun DashboardContent(
    state: DashboardUiState.Dashboard,
    customBackgroundPath: String,
    isFridayStandby: Boolean = false,
    jumatWarningText: String = "",
    isFridayBeforeAdzan: Boolean = false,
    isFridayAfterAdzan: Boolean = false,
    onNavigateToSettings: () -> Unit,
    onStartAdzanNow: () -> Unit = {},
    onForceFridaySilence: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isFridayBeforeAdzan) {
            // Sebelum adzan: Only show SmallClockCard at top-left
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MosqueTheme.spacing.huge,
                        vertical = MosqueTheme.spacing.xxl * MosqueTheme.displayScale
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallClockCard(
                    currentTime = state.currentTime,
                    nextPrayer = state.nextPrayer,
                    onNavigateToSettings = onNavigateToSettings,
                    onStartAdzanNow = onStartAdzanNow,
                    onForceFridaySilence = onForceFridaySilence,
                    isFridayBeforeAdzan = isFridayBeforeAdzan
                )
            }
            if (!state.isSlideshowActive) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Persiapan Ibadah Jum'at",
                        color = MosqueTheme.colors.textPrimary,
                        fontSize = MosqueTheme.typography.clockLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        } else if (isFridayAfterAdzan) {
            // Setelah adzan (Silence Mode): Centered large clock & info (like dashboard)
            Spacer(modifier = Modifier.weight(1f))
            ClockSection(state = state)
            Spacer(modifier = Modifier.weight(1f))
        } else if (isFridayStandby) {
            // Fallback for previewing Friday Mode
            AnimatedContent(
                targetState = state.isSlideshowActive,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "FridayHeaderTransition"
            ) { isSlideshow ->
                if (isSlideshow) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                        horizontal = MosqueTheme.spacing.huge,
                        vertical = MosqueTheme.spacing.xxl * MosqueTheme.displayScale
                    ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallClockCard(
                            currentTime = state.currentTime,
                            nextPrayer = state.nextPrayer,
                            onNavigateToSettings = onNavigateToSettings
                        )
                    }
                } else {
                    NextPrayerHeader(state.nextPrayer, onNavigateToSettings)
                }
            }

            if (!state.isSlideshowActive) {
                Spacer(modifier = Modifier.weight(1f))
            }

            AnimatedContent(
                targetState = state.isSlideshowActive,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "FridayMiddleTransition"
            ) { isSlideshow ->
                if (!isSlideshow) {
                    ClockSection(state = state)
                }
            }

            if (!state.isSlideshowActive) {
                Spacer(modifier = Modifier.weight(1f))
            }
        } else {
            AnimatedContent(
                targetState = state.isSlideshowActive,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "HeaderTransition"
            ) { isSlideshow ->
                if (isSlideshow) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MosqueTheme.spacing.huge,
                                vertical = MosqueTheme.spacing.xxl
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallClockCard(
                            currentTime = state.currentTime,
                            nextPrayer = state.nextPrayer,
                            onNavigateToSettings = onNavigateToSettings
                        )
                    }
                } else {
                    NextPrayerHeader(state.nextPrayer, onNavigateToSettings)
                }
            }

            if (!state.isSlideshowActive) {
                Spacer(modifier = Modifier.weight(1f))
            }

            AnimatedContent(
                targetState = state.isSlideshowActive,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "MiddleTransition"
            ) { isSlideshow ->
                if (!isSlideshow) {
                    ClockSection(state = state)
                }
            }

            if (!state.isSlideshowActive) {
                Spacer(modifier = Modifier.weight(1f))
            }
            
            if (!state.isSlideshowActive) {
                PrayerTimesPanel(
                    prayerTimes = state.prayerTimes,
                    isSlideshowActive = state.isSlideshowActive
                )
                RunningTextBar(text = state.runningText)
            }
        }
    }
}

@Composable
fun SmallClockCard(
    currentTime: String,
    nextPrayer: NextPrayerUiModel,
    onNavigateToSettings: () -> Unit,
    onStartAdzanNow: (() -> Unit)? = null,
    onForceFridaySilence: () -> Unit = {},
    isFridayBeforeAdzan: Boolean = false
) {
    var clickCount by remember { mutableStateOf(0) }
    Box(
        modifier = Modifier
            .background(
                color = MosqueTheme.colors.scrim.copy(alpha = 0.6f),
                shape = RoundedCornerShape(MosqueTheme.radius.md)
            )
            .padding(horizontal = MosqueTheme.spacing.xl, vertical = MosqueTheme.spacing.md)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                clickCount++
                if (clickCount >= 5) {
                    clickCount = 0
                    onNavigateToSettings()
                }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val timeParts = currentTime.split(":")
            val hours = timeParts.getOrNull(0) ?: "00"
            val minutes = timeParts.getOrNull(1) ?: "00"
            val secondsStr = timeParts.getOrNull(2) ?: "00"
            val secondsVal = secondsStr.toIntOrNull() ?: 0
            val isColonVisible = secondsVal % 2 == 0

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hours,
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = ":",
                    color = if (isColonVisible) MosqueTheme.colors.textPrimary else MosqueTheme.colors.transparent,
                    fontSize = MosqueTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = minutes,
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(MosqueTheme.spacing.xxs))

            val nextPrayerName = nextPrayer.prayerType.displayName.lowercase().replaceFirstChar { it.uppercase() }
            Text(
                text = "$nextPrayerName · ${nextPrayer.countdown}",
                color = MosqueTheme.colors.accent,
                fontSize = MosqueTheme.typography.label,
                fontWeight = FontWeight.Bold
            )

            if (onStartAdzanNow != null) {
                Spacer(modifier = Modifier.height(MosqueTheme.spacing.md))
                val cardModifier = if (isFridayBeforeAdzan) {
                    Modifier
                        .background(MosqueTheme.colors.accent.copy(alpha = 0.2f), shape = RoundedCornerShape(MosqueTheme.radius.xs))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var releasedBeforeTimeout = false
                                    val result = withTimeoutOrNull(2000L) {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.changes.any { !it.pressed }) {
                                                releasedBeforeTimeout = true
                                                break
                                            }
                                        }
                                        true
                                    }
                                    if (result == null) {
                                        onForceFridaySilence()
                                    } else if (releasedBeforeTimeout) {
                                        onStartAdzanNow()
                                    }
                                }
                            }
                        }
                } else {
                    Modifier
                        .background(MosqueTheme.colors.accent.copy(alpha = 0.2f), shape = RoundedCornerShape(MosqueTheme.radius.xs))
                        .clickable { onStartAdzanNow() }
                }
                Box(
                    modifier = cardModifier
                        .padding(horizontal = MosqueTheme.spacing.lg, vertical = MosqueTheme.spacing.sm)
                ) {
                    Text("Mulai Adzan Sekarang", color = MosqueTheme.colors.accent, fontSize = MosqueTheme.typography.caption, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}