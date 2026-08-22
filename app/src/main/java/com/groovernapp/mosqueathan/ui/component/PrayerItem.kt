package com.groovernapp.mosqueathan.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun PrayerItem(
    title: String,
    time: String,
    highlighted: Boolean,
    isSlideshowActive: Boolean
) {
    val color = if (highlighted) MosqueTheme.colors.accent else MosqueTheme.colors.textPrimary

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Text(
            text = title,
            color = color,
            fontSize = if (isSlideshowActive) MosqueTheme.typography.body else MosqueTheme.typography.prayerLabel,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(if (isSlideshowActive) 5.dp else 6.dp))

        Text(
            text = time,
            color = color,
            fontSize = if (isSlideshowActive) MosqueTheme.typography.prayerNameCompact else MosqueTheme.typography.prayerName,
            fontWeight = FontWeight.Bold
        )
    }
}