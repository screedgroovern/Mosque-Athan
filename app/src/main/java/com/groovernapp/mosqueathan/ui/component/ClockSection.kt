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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.ui.dashboard.DashboardUiState
import com.groovernapp.mosqueathan.ui.dashboard.NextPrayerUiModel
import com.groovernapp.mosqueathan.ui.dashboard.PrayerTimesUiModel

@Composable
fun ClockSection(state: DashboardUiState.Dashboard) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = state.currentTime,
            color = Color.White,
            fontSize = 170.sp,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Masjid Al-Hikmah Tegalrejo",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp
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

@Preview(showBackground = true)
@Composable
fun ClockSectionPreview() {
    ClockSection(
        DashboardUiState.Dashboard(
            currentTime = "10:18:12",
            gregorianDate = "-",
            hijriDate = "-",
            prayerTimes = PrayerTimesUiModel(
                imsak = "--:--",
                subuh = "04:00",
                syuruq = "--:--",
                dzuhur = "--:--",
                ashar = "--:--",
                maghrib = "--:--",
                isya = "--:--",
                highlightedPrayer = PrayerType.SUBUH
            ),
            nextPrayer = NextPrayerUiModel(
                prayerType = PrayerType.SUBUH,
                prayerTime = "--:--",
                countdown = "--:--:--"
            ),
            runningText = "Text running",
            isSlideshowActive = false
        )
    )
}