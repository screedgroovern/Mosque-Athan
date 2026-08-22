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
import androidx.compose.ui.tooling.preview.Preview
import com.groovernapp.mosqueathan.data.StandbyScheduleEntity
import com.groovernapp.mosqueathan.util.Lang
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.component.MosqueSectionCard

@Composable
fun SystemMaintenanceSettingsPane(
    standbyModeEnabled: Boolean,
    onStandbyModeEnabledChange: (Boolean) -> Unit,
    standbySchedules: List<StandbyScheduleEntity>,
    onAddStandbySchedule: (String, Boolean, String, String) -> Unit,
    onDeleteStandbySchedule: (String) -> Unit,
    onUpdateStandbyScheduleTime: (com.groovernapp.mosqueathan.data.ScheduleItem, String, String) -> Unit,
    onUpdateStandbyScheduleDays: (com.groovernapp.mosqueathan.data.ScheduleItem, String) -> Unit,
    onUpdateStandbyScheduleDates: (com.groovernapp.mosqueathan.data.ScheduleItem, String) -> Unit,
    onResetToDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xxxl)
    ) {
        MosqueSectionCard(
            title = Lang.get("Standby & Display", "Standby & Tampilan"),
            icon = Icons.Default.Tv
        ) {
            StandbySettingsSegment(
                standbyModeEnabled = standbyModeEnabled,
                onStandbyModeEnabledChange = onStandbyModeEnabledChange,
                standbySchedules = standbySchedules,
                onAddStandbySchedule = onAddStandbySchedule,
                onDeleteStandbySchedule = onDeleteStandbySchedule,
                onUpdateStandbyScheduleTime = onUpdateStandbyScheduleTime,
                onUpdateStandbyScheduleDays = onUpdateStandbyScheduleDays,
                onUpdateStandbyScheduleDates = onUpdateStandbyScheduleDates
            )
        }

        MosqueSectionCard(
            title = Lang.get("Reset Configurations", "Reset Konfigurasi"),
            icon = Icons.Default.Restore
        ) {
            ResetSettingsSegment(onResetToDefaults = onResetToDefaults)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun SystemMaintenanceSettingsPanePreview() {
    val mockSchedules = listOf(
        StandbyScheduleEntity().apply {
            id = "1"
            day = "Monday"
            isSpecificDate = false
            startTime = "22:00"
            endTime = "04:00"
        },
        StandbyScheduleEntity().apply {
            id = "2"
            day = "2026-06-18"
            isSpecificDate = true
            startTime = "12:00"
            endTime = "13:00"
        }
    )
    SystemMaintenanceSettingsPane(
        standbyModeEnabled = true,
        onStandbyModeEnabledChange = {},
        standbySchedules = mockSchedules,
        onAddStandbySchedule = { _, _, _, _ -> },
        onDeleteStandbySchedule = {},
        onUpdateStandbyScheduleTime = { _, _, _ -> },
        onUpdateStandbyScheduleDays = { _, _ -> },
        onUpdateStandbyScheduleDates = { _, _ -> },
        onResetToDefaults = {}
    )
}
