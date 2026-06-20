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
                horizontal = 32.dp,
                vertical = 20.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = nextPrayer.prayerType.displayName,
            color = Color(0xFFFFD54F),
            fontSize = 30.sp,
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

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "•",
            color = Color.White,
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = nextPrayer.countdown,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
    }
}