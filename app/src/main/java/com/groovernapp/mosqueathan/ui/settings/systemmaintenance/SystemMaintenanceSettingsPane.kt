package com.groovernapp.mosqueathan.ui.settings.systemmaintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.groovernapp.mosqueathan.data.StandbyScheduleEntity
import com.groovernapp.mosqueathan.ui.settings.SettingsCard
import com.groovernapp.mosqueathan.util.Lang

@Composable
fun SystemMaintenanceSettingsPane(
    standbyModeEnabled: Boolean,
    onStandbyModeEnabledChange: (Boolean) -> Unit,
    standbySchedules: List<StandbyScheduleEntity>,
    onAddStandbySchedule: (String, Boolean, String, String) -> Unit,
    onDeleteStandbySchedule: (String) -> Unit,
    onResetToDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SettingsCard(
            title = Lang.get("Standby & Display", "Standby & Tampilan"),
            icon = Icons.Default.Tv
        ) {
            StandbySettingsSegment(
                standbyModeEnabled = standbyModeEnabled,
                onStandbyModeEnabledChange = onStandbyModeEnabledChange,
                standbySchedules = standbySchedules,
                onAddStandbySchedule = onAddStandbySchedule,
                onDeleteStandbySchedule = onDeleteStandbySchedule
            )
        }

        SettingsCard(
            title = Lang.get("Reset Configurations", "Reset Konfigurasi"),
            icon = Icons.Default.Restore
        ) {
            ResetSettingsSegment(onResetToDefaults = onResetToDefaults)
        }
    }
}
