package com.groovernapp.mosqueathan.ui.settings.systemmaintenance

import com.groovernapp.mosqueathan.ui.settings.*

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
import com.groovernapp.mosqueathan.util.Lang
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

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
                text = Lang.get("Reset all Settings to default", "Reset semua Pengaturan ke default"),
                color = MosqueTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = MosqueTheme.typography.body
            )
        }
    }
}
