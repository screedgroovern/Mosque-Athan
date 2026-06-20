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
import com.groovernapp.mosqueathan.ui.component.ClockSection
import com.groovernapp.mosqueathan.ui.component.NextPrayerHeader
import com.groovernapp.mosqueathan.ui.component.PrayerTimesPanel
import com.groovernapp.mosqueathan.ui.component.RunningTextBar

@Composable
fun DashboardContent(
    state: DashboardUiState.Dashboard,
    customBackgroundPath: String,
    onNavigateToSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
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
                            horizontal = 32.dp,
                            vertical = 20.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var clickCount by remember { mutableStateOf(0) }
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            // First Line: HH:mm with blinking colon
                            val timeParts = state.currentTime.split(":")
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
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = ":",
                                    color = if (isColonVisible) Color.White else Color.Transparent,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = minutes,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Second Line: Next_prayer · countdown
                            val nextPrayerName = state.nextPrayer.prayerType.displayName.lowercase().replaceFirstChar { it.uppercase() }
                            Text(
                                text = "$nextPrayerName · ${state.nextPrayer.countdown}",
                                color = Color(0xFFFFD54F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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