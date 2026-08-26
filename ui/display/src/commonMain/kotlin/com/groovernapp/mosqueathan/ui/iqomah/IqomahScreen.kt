package com.groovernapp.mosqueathan.ui.iqomah

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.groovernapp.mosqueathan.ui.design.displayBackgrounds
import com.groovernapp.mosqueathan.ui.component.CountdownScreen
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun IqomahScreen(state: IqomahUiModel, onDismiss: () -> Unit = {}) {
    CountdownScreen(
        background = displayBackgrounds.iqomah,
        currentTime = state.currentTime,
        titleText = "MENUJU IQOMAH",
        prayerName = state.prayerType.displayName,
        countdownValue = state.iqomahCountdown,
        footerText = state.footerText,
        gradientColors = listOf(
            MosqueTheme.colors.scrim.copy(alpha = 0.76f),
            MosqueTheme.colors.backdrop.copy(alpha = 0.86f),
            MosqueTheme.colors.scrim.copy(alpha = 0.80f)
        ),
        onDismiss = onDismiss
    )
}
