package com.groovernapp.mosqueathan.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.ui.dashboard.DashboardUiState

@Composable
fun ClockSection(state: DashboardUiState.Dashboard) {
    Column(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = state.currentTime,
            color = Color.White,
            fontSize = 120.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Masjid Al-Hikmah Tegalrejo",
            color = Color.White,
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = state.gregorianDate,
            color = Color.White,
            fontSize = 22.sp
        )

        Text(
            text = state.hijriDate,
            color = Color.White,
            fontSize = 22.sp
        )
    }
}