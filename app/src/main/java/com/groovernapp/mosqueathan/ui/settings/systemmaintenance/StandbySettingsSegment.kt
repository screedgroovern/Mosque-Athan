package com.groovernapp.mosqueathan.ui.settings.systemmaintenance

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.util.Lang
import com.groovernapp.mosqueathan.data.StandbyScheduleEntity
import com.groovernapp.mosqueathan.ui.component.Schedule

@Composable
fun StandbySettingsSegment(
    standbyModeEnabled: Boolean,
    onStandbyModeEnabledChange: (Boolean) -> Unit,
    standbySchedules: List<StandbyScheduleEntity>,
    onAddStandbySchedule: (day: String, isSpecificDate: Boolean, startTime: String, endTime: String) -> Unit,
    onDeleteStandbySchedule: (String) -> Unit,
    onUpdateStandbyScheduleTime: (com.groovernapp.mosqueathan.data.ScheduleItem, String, String) -> Unit,
    onUpdateStandbyScheduleDays: (com.groovernapp.mosqueathan.data.ScheduleItem, String) -> Unit,
    onUpdateStandbyScheduleDates: (com.groovernapp.mosqueathan.data.ScheduleItem, String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
    ) {
        // Toggle Switch standbyModeEnabled
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Lang.get("Automatic Standby Mode", "Mode Standby Otomatis"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Lang.get(
                        "Put the TV in standby mode (black screen) outside operational hours.",
                        "Masuk ke mode standby (layar hitam) di luar jam operasional."
                    ),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            Switch(
                checked = standbyModeEnabled,
                onCheckedChange = onStandbyModeEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFFFFD54F),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
        }

        Divider(color = Color.White.copy(alpha = 0.15f))

        // Schedule Component for Standby schedules
        Schedule(
            title = "",
            schedules = standbySchedules,
            addButtonText = Lang.get("Add Standby", "Tambah Standby"),
            scheduleListButtonText = Lang.get("Standby Schedules", "Jadwal Standby"),
            onAddSchedule = {
                onAddStandbySchedule("Monday", false, "22:00", "04:00")
            },
            onDeleteSchedule = { schedule ->
                onDeleteStandbySchedule(schedule.id)
            },
            onUpdateScheduleTime = { schedule, start, end ->
                onUpdateStandbyScheduleTime(schedule, start, end)
            },
            onUpdateScheduleDays = { schedule, days ->
                onUpdateStandbyScheduleDays(schedule, days)
            },
            onUpdateScheduleDates = { schedule, dates ->
                onUpdateStandbyScheduleDates(schedule, dates)
            }
        )

        Text(
            text = Lang.get(
                "You can now set multiple standby schedules for different days or specific dates. TV will sleep during active standby periods.",
                "Anda sekarang dapat mengatur beberapa jadwal standby untuk hari yang berbeda atau tanggal spesifik. TV akan mati selama periode standby aktif."
            ),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp
        )
    }
}
