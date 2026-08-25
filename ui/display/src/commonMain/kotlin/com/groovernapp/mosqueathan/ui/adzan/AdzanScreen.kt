package com.groovernapp.mosqueathan.ui.adzan

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.groovernapp.mosqueathan.ui.display.displayBackgrounds
import com.groovernapp.mosqueathan.ui.component.CountdownScreen
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun AdzanScreen(state: AdzanUiModel, onDismiss: () -> Unit = {}) {
    CountdownScreen(
        background = displayBackgrounds.adzan,
        currentTime = state.currentTime,
        titleText = "MENJELANG ADZAN",
        prayerName = state.prayerType.displayName,
        countdownValue = state.countdown,
        footerText = state.footerText,
        gradientColors = listOf(
            MosqueTheme.colors.scrim.copy(alpha = 0.70f),
            MosqueTheme.colors.backdrop.copy(alpha = 0.82f),
            MosqueTheme.colors.scrim.copy(alpha = 0.78f)
        ),
        onDismiss = onDismiss
    )
}
