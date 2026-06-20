package com.groovernapp.mosqueathan.ui.iqomah

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.groovernapp.mosqueathan.R
import com.groovernapp.mosqueathan.ui.component.CountdownScreen

@Composable
fun IqomahScreen(state: IqomahUiModel, onDismiss: () -> Unit = {}) {
    CountdownScreen(
        backgroundImageRes = R.drawable.iqomah_bg_default,
        currentTime = state.currentTime,
        titleText = "MENUJU IQOMAH",
        prayerName = state.prayerType.displayName,
        countdownValue = state.iqomahCountdown,
        footerText = state.footerText,
        gradientColors = listOf(
            Color.Black.copy(alpha = 0.76f),
            Color(0xFF18231B).copy(alpha = 0.86f),
            Color.Black.copy(alpha = 0.80f)
        ),
        onDismiss = onDismiss
    )
}
