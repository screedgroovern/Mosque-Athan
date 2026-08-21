package com.groovernapp.mosqueathan.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun IqomahContent(
    state: DashboardUiState.Iqomah
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = state.iqomah.remainingSeconds.toString(),
            fontSize = MosqueTheme.typography.iqomahCountdown
        )
    }
}