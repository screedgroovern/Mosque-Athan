package com.groovernapp.mosqueathan.ui.adzan

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.groovernapp.mosqueathan.R
import com.groovernapp.mosqueathan.ui.component.CountdownScreen

@Composable
fun AdzanScreen(state: AdzanUiModel, onDismiss: () -> Unit = {}) {
    CountdownScreen(
        backgroundImageRes = R.drawable.adzan_bg_default,
        currentTime = state.currentTime,
        titleText = "MENJELANG ADZAN",
        prayerName = state.prayerType.displayName,
        countdownValue = state.countdown,
        footerText = state.footerText,
        gradientColors = listOf(
            Color.Black.copy(alpha = 0.70f),
            Color(0xFF142018).copy(alpha = 0.82f),
            Color.Black.copy(alpha = 0.78f)
        ),
        onDismiss = onDismiss
    )
}
