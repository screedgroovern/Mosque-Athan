package com.groovernapp.mosqueathan.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.groovernapp.mosqueathan.ui.component.ClockSection
import com.groovernapp.mosqueathan.ui.component.NextPrayerHeader
import com.groovernapp.mosqueathan.ui.component.PrayerTimesPanel
import com.groovernapp.mosqueathan.ui.component.RunningTextBar

@Composable
fun DashboardContent(
    state: DashboardUiState.Dashboard,
    customBackgroundPath: String,
    onNavigateToSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        NextPrayerHeader(state.nextPrayer, onNavigateToSettings)
        Spacer(modifier = Modifier.weight(1f))
        ClockSection(state = state)
        Spacer(modifier = Modifier.weight(1f))
        PrayerTimesPanel(prayerTimes = state.prayerTimes)
        RunningTextBar(text = state.runningText)
    }
}