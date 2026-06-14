package com.groovernapp.mosqueathan.ui.settings.systemmaintenance

import com.groovernapp.mosqueathan.ui.settings.prayerschedule.DatePickerField

import com.groovernapp.mosqueathan.ui.settings.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.util.Lang
import com.groovernapp.mosqueathan.data.StandbyScheduleEntity

@Composable
fun TimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val parts = value.split(":")
    var hour by remember(value) { mutableIntStateOf(parts.getOrNull(0)?.toIntOrNull() ?: 0) }
    var minute by remember(value) { mutableIntStateOf(parts.getOrNull(1)?.toIntOrNull() ?: 0) }

    fun emitChange() {
        val h = hour.toString().padStart(2, '0')
        val m = minute.toString().padStart(2, '0')
        onValueChange("$h:$m")
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Hour stepper
            TvFocusableButton(
                onClick = { hour = (hour - 1 + 24) % 24; emitChange() },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("−", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = hour.toString().padStart(2, '0'),
                color = Color(0xFFFFD54F),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )

            TvFocusableButton(
                onClick = { hour = (hour + 1) % 24; emitChange() },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = ":",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Minute stepper
            TvFocusableButton(
                onClick = { minute = (minute - 1 + 60) % 60; emitChange() },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("−", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = minute.toString().padStart(2, '0'),
                color = Color(0xFFFFD54F),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )

            TvFocusableButton(
                onClick = { minute = (minute + 1) % 60; emitChange() },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StandbySettingsSegment(
    standbyModeEnabled: Boolean,
    onStandbyModeEnabledChange: (Boolean) -> Unit,
    standbySchedules: List<StandbyScheduleEntity>,
    onAddStandbySchedule: (day: String, isSpecificDate: Boolean, startTime: String, endTime: String) -> Unit,
    onDeleteStandbySchedule: (String) -> Unit
) {
    var isSpecificDate by remember { mutableStateOf(false) }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var specificDateText by remember { mutableStateOf("") }
    var startTimeText by remember { mutableStateOf("22:00") }
    var endTimeText by remember { mutableStateOf("04:00") }
    
    var dayDropdownExpanded by remember { mutableStateOf(false) }
    var showListDialog by remember { mutableStateOf(false) }

    val daysOfWeek = remember {
        listOf(
            "Monday" to Lang.get("Monday", "Senin"),
            "Tuesday" to Lang.get("Tuesday", "Selasa"),
            "Wednesday" to Lang.get("Wednesday", "Rabu"),
            "Thursday" to Lang.get("Thursday", "Kamis"),
            "Friday" to Lang.get("Friday", "Jumat"),
            "Saturday" to Lang.get("Saturday", "Sabtu"),
            "Sunday" to Lang.get("Sunday", "Minggu")
        )
    }

    if (showListDialog) {
        AlertDialog(
            onDismissRequest = { showListDialog = false },
            title = { Text(Lang.get("Standby Times List", "Daftar Waktu Standby"), color = Color.White) },
            text = {
                Box(modifier = Modifier.width(400.dp).height(300.dp)) {
                    if (standbySchedules.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(Lang.get("No standby schedules set.", "Tidak ada jadwal standby."), color = Color.White.copy(alpha = 0.5f))
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(standbySchedules) { schedule ->
                                val dayLabel = if (schedule.isSpecificDate) {
                                    schedule.day
                                } else {
                                    daysOfWeek.firstOrNull { it.first.equals(schedule.day, ignoreCase = true) }?.second ?: schedule.day
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = dayLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "${schedule.startTime} - ${schedule.endTime}", color = Color(0xFFFFD54F), fontSize = 13.sp)
                                    }
                                    TvFocusableDangerButton(
                                        onClick = { onDeleteStandbySchedule(schedule.id) },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(Lang.get("Delete", "Hapus"), color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TvFocusablePrimaryButton(
                    onClick = { showListDialog = false }
                ) {
                    Text(Lang.get("Close", "Tutup"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF181C1A)
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
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
                    checkedThumbColor = Color(0xFFFFD54F),
                    checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
        }

        Divider(color = Color.White.copy(alpha = 0.15f))

        // Specific Date Checkbox / Switch
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { isSpecificDate = !isSpecificDate }
        ) {
            Checkbox(
                checked = isSpecificDate,
                onCheckedChange = { isSpecificDate = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFFFFD54F),
                    checkmarkColor = Color.Black,
                    uncheckedColor = Color.White.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Lang.get("Specific Date", "Tanggal Spesifik"),
                color = Color.White,
                fontSize = 14.sp
            )
        }

        // Day or Date Input (above)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isSpecificDate) Lang.get("Date", "Tanggal") else Lang.get("Day", "Hari"),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            if (isSpecificDate) {
                DatePickerField(
                    value = specificDateText,
                    onValueChange = { specificDateText = it },
                    label = "",
                    placeholder = "YYYY-MM-DD",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { dayDropdownExpanded = true }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = daysOfWeek[selectedDayIndex].second, color = Color.White, fontSize = 16.sp)
                    DropdownMenu(
                        expanded = dayDropdownExpanded,
                        onDismissRequest = { dayDropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF0C120F))
                    ) {
                        daysOfWeek.forEachIndexed { index, (_, label) ->
                            DropdownMenuItem(
                                text = { Text(text = label, color = Color.White) },
                                onClick = {
                                    selectedDayIndex = index
                                    dayDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Start & End Time Inputs (below)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Start Time
            TimePickerField(
                value = startTimeText,
                onValueChange = { startTimeText = it },
                label = Lang.get("Start Time (HH:mm)", "Mulai (HH:mm)"),
                placeholder = "22:00",
                modifier = Modifier.weight(1f)
            )

            // End Time
            TimePickerField(
                value = endTimeText,
                onValueChange = { endTimeText = it },
                label = Lang.get("End Time (HH:mm)", "Selesai (HH:mm)"),
                placeholder = "04:00",
                modifier = Modifier.weight(1f)
            )
        }

        // Add Button & Standby Times List Button
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TvFocusablePrimaryButton(
                onClick = {
                    val dayVal = if (isSpecificDate) specificDateText else daysOfWeek[selectedDayIndex].first
                    if (dayVal.isNotEmpty() && startTimeText.isNotEmpty() && endTimeText.isNotEmpty()) {
                        onAddStandbySchedule(dayVal, isSpecificDate, startTimeText, endTimeText)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(Lang.get("Add", "Tambah"), color = Color.Black, fontWeight = FontWeight.Bold)
            }

            TvFocusableButton(
                onClick = { showListDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(Lang.get("Standby Times", "Waktu Standby"), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

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
