package com.groovernapp.mosqueathan.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.groovernapp.mosqueathan.ui.dashboard.NextPrayerUiModel
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun NextPrayerHeader(
    nextPrayer: NextPrayerUiModel,
    onNavigateToSettings: () -> Unit
) {
    var clickCount by remember { mutableStateOf(0) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MosqueTheme.spacing.huge,
                vertical = MosqueTheme.spacing.xxl
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = nextPrayer.prayerType.displayName,
            color = MosqueTheme.colors.accent,
            fontSize = MosqueTheme.typography.headline,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                clickCount++
                if (clickCount >= 5) {
                    clickCount = 0
                    onNavigateToSettings()
                }
            }
        )

        Spacer(modifier = Modifier.width(MosqueTheme.spacing.lg))

        Text(
            text = "•",
            color = MosqueTheme.colors.textPrimary,
            fontSize = MosqueTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.width(MosqueTheme.spacing.lg))

        Text(
            text = nextPrayer.countdown,
            color = MosqueTheme.colors.textPrimary,
            fontSize = MosqueTheme.typography.headline,
            fontWeight = FontWeight.Bold
        )
    }
}