package com.groovernapp.mosqueathan.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResetSettingsSegment(onResetToDefaults: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        TvFocusableDangerButton(
            onClick = onResetToDefaults,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .height(56.dp)
                .padding(horizontal = 32.dp)
        ) {
            Text(
                text = com.groovernapp.mosqueathan.util.Lang.get("RESET ALL SETTINGS TO DEFAULT", "RESET SEMUA PENGATURAN KE DEFAULT"),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
