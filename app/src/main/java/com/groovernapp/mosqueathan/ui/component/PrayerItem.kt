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

@Composable
fun PrayerItem(
    title: String,
    time: String,
    highlighted: Boolean
) {
    val color = if (highlighted) Color(0xFFFFD54F) else Color.White

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Text(
            text = title,
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = time,
            color = color,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}