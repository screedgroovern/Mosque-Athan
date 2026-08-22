package com.groovernapp.mosqueathan.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.groovernapp.mosqueathan.data.ScheduleItem
import com.groovernapp.mosqueathan.util.Lang
import java.util.Calendar
import java.util.TimeZone
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.component.MosquePrimaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueDangerButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueSecondaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueTertiaryButton
import com.groovernapp.mosqueathan.ui.design.focusHighlight
import com.groovernapp.mosqueathan.ui.design.MosqueRadius
import com.groovernapp.mosqueathan.ui.design.component.MosqueIconButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueTextButton

@Composable
fun Schedule(
    title: String,
    schedules: List<ScheduleItem>,
    onAddSchedule: () -> Unit,
    onDeleteSchedule: (ScheduleItem) -> Unit,
    onUpdateScheduleTime: (ScheduleItem, String, String) -> Unit, // startTime, endTime
    onUpdateScheduleDays: (ScheduleItem, String) -> Unit, // Comma separated active days
    onUpdateScheduleDates: (ScheduleItem, String) -> Unit, // Comma separated dates
    modifier: Modifier = Modifier,
    addButtonText: String = "Add",
    scheduleListButtonText: String = "Schedules"
) {
    val context = LocalContext.current
    val isTv = remember(context) {
        context.packageManager.hasSystemFeature("android.software.leanback")
    }

    // Sort descending by created timestamp and limit to 5
    val displayedSchedules = remember(schedules) {
        schedules.sortedByDescending { it.createdAt }.take(5)
    }

    var activeTimeEditSchedule by remember { mutableStateOf<ScheduleItem?>(null) }
    var activeDateEditSchedule by remember { mutableStateOf<ScheduleItem?>(null) }
    var showAllSchedulesDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
    ) {
        if (title.isNotEmpty()) {
            // Left-aligned Title
            Text(
                text = title,
                color = MosqueTheme.colors.accent,
                fontWeight = FontWeight.Bold,
                fontSize = MosqueTheme.typography.body,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // List of schedules
        if (displayedSchedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MosqueTheme.radius.md))
                    .background(MosqueTheme.colors.fillSubtle)
                    .padding(vertical = MosqueTheme.spacing.xxxl),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Lang.get("No schedules added yet.", "Belum ada jadwal yang ditambahkan."),
                    color = MosqueTheme.colors.textMuted,
                    fontSize = MosqueTheme.typography.label
                )
            }
        } else {
            displayedSchedules.forEach { schedule ->
                ScheduleListItem(
                    schedule = schedule,
                    isTv = isTv,
                    onEditTimeClick = { activeTimeEditSchedule = schedule },
                    onToggleDay = { dayName, enabled ->
                        val daysList = schedule.activeDays.split(",")
                            .filter { it.isNotEmpty() }
                            .toMutableList()
                        if (enabled) {
                            if (!daysList.contains(dayName)) daysList.add(dayName)
                        } else {
                            daysList.remove(dayName)
                        }
                        onUpdateScheduleDays(schedule, daysList.joinToString(","))
                    },
                    onScheduleClick = { activeDateEditSchedule = schedule },
                    onDeleteClick = { onDeleteSchedule(schedule) }
                )
            }
        }
        
        // Add & Schedule List Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MosquePrimaryButton(
                onClick = onAddSchedule,
                shape = RoundedCornerShape(MosqueTheme.radius.md),
                contentPadding = PaddingValues(horizontal = MosqueTheme.spacing.xl, vertical = MosqueTheme.spacing.lg),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = addButtonText,
                        fontWeight = FontWeight.Bold,
                        fontSize = MosqueTheme.typography.bodyCompact
                    )
                }
            }

            MosqueSecondaryButton(
                onClick = { showAllSchedulesDialog = true },
                shape = RoundedCornerShape(MosqueTheme.radius.md),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(
                    text = scheduleListButtonText,
                    color = MosqueTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = MosqueTheme.typography.bodyCompact
                )
            }
        }
    }

    if (showAllSchedulesDialog) {
        val localizedDays = remember {
            listOf(
                "Monday" to Lang.get("Mon", "Sen"),
                "Tuesday" to Lang.get("Tue", "Sel"),
                "Wednesday" to Lang.get("Wed", "Rab"),
                "Thursday" to Lang.get("Thu", "Kam"),
                "Friday" to Lang.get("Fri", "Jum"),
                "Saturday" to Lang.get("Sat", "Sab"),
                "Sunday" to Lang.get("Sun", "Min")
            )
        }
        
        val sortedSchedules = remember(schedules) {
            schedules.sortedByDescending { it.createdAt }
        }

        AlertDialog(
            onDismissRequest = { showAllSchedulesDialog = false },
            title = { Text(Lang.get("Schedules List", "Daftar Jadwal"), color = MosqueTheme.colors.textPrimary) },
            text = {
                Box(modifier = Modifier.width(480.dp).height(400.dp)) {
                    if (sortedSchedules.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(Lang.get("No schedules set.", "Tidak ada jadwal."), color = MosqueTheme.colors.textMuted)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(sortedSchedules) { schedule ->
                                ScheduleListItem(
                                    schedule = schedule,
                                    isTv = isTv,
                                    onEditTimeClick = { activeTimeEditSchedule = schedule },
                                    onToggleDay = { dayName, enabled ->
                                        val daysList = schedule.activeDays.split(",")
                                            .filter { it.isNotEmpty() }
                                            .toMutableList()
                                        if (enabled) {
                                            if (!daysList.contains(dayName)) daysList.add(dayName)
                                        } else {
                                            daysList.remove(dayName)
                                        }
                                        onUpdateScheduleDays(schedule, daysList.joinToString(","))
                                    },
                                    onScheduleClick = { activeDateEditSchedule = schedule },
                                    onDeleteClick = { onDeleteSchedule(schedule) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                MosquePrimaryButton(
                    onClick = { showAllSchedulesDialog = false }
                ) {
                    Text(Lang.get("Close", "Tutup"), color = MosqueTheme.colors.onAccent, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MosqueTheme.colors.surface
        )
    }

    // Time Edit Dialog
    activeTimeEditSchedule?.let { schedule ->
        TimePickerDialog(
            initialStartTime = schedule.startTime,
            initialEndTime = schedule.endTime,
            isTv = isTv,
            onDismiss = { activeTimeEditSchedule = null },
            onSave = { start, end ->
                onUpdateScheduleTime(schedule, start, end)
                activeTimeEditSchedule = null
            }
        )
    }

    // Specific Multi Date Picker Dialog
    activeDateEditSchedule?.let { schedule ->
        MultiDatePickerDialog(
            initialDates = schedule.specificDates,
            isTv = isTv,
            onDismiss = { activeDateEditSchedule = null },
            onSave = { dates ->
                onUpdateScheduleDates(schedule, dates)
                activeDateEditSchedule = null
            }
        )
    }
}

@Composable
fun ScheduleListItem(
    schedule: ScheduleItem,
    isTv: Boolean,
    onEditTimeClick: () -> Unit,
    onToggleDay: (String, Boolean) -> Unit,
    onScheduleClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val localizedDays = remember {
        listOf(
            "Monday" to Lang.get("Mon", "Sen"),
            "Tuesday" to Lang.get("Tue", "Sel"),
            "Wednesday" to Lang.get("Wed", "Rab"),
            "Thursday" to Lang.get("Thu", "Kam"),
            "Friday" to Lang.get("Fri", "Jum"),
            "Saturday" to Lang.get("Sat", "Sab"),
            "Sunday" to Lang.get("Sun", "Min")
        )
    }

    var activeDaysState by remember(schedule.activeDays) {
        mutableStateOf(schedule.activeDays)
    }

    val activeDaysList = remember(activeDaysState) {
        activeDaysState.split(",").filter { it.isNotEmpty() }
    }

    val datesCount = remember(schedule.specificDates) {
        schedule.specificDates.split(",").filter { it.isNotEmpty() }.size
    }

    Card(
        shape = RoundedCornerShape(MosqueTheme.radius.lg),
        colors = CardDefaults.cardColors(
            containerColor = MosqueTheme.colors.fillSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(MosqueTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg)
        ) {
            // Row 1: Time [ HH:mm ] and Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Time clickable
                Text(
                    text = "${schedule.startTime} - ${schedule.endTime}",
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.prayerNameCompact,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .focusHighlight(shape = RoundedCornerShape(MosqueRadius.md), scaleUp = false)
                        .clickable(onClick = onEditTimeClick)
                        .padding(vertical = MosqueTheme.spacing.xs)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                ) {
                    // Edit Button (Vertically centered)
                    MosqueIconButton(
                        onClick = onEditTimeClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MosqueTheme.colors.accent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Time",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Delete Button
                    MosqueIconButton(
                        onClick = onDeleteClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MosqueTheme.colors.danger.copy(alpha = 0.8f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Row 2: Monday - Sunday Toggle Buttons
            if (isTv) {
                // TV layout: simpler display of active days to prevent complex D-pad horizontal switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.sm)
                ) {
                    localizedDays.forEach { (englishName, localizedName) ->
                        val isActive = activeDaysList.contains(englishName)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(MosqueTheme.radius.xs))
                                .background(if (isActive) MosqueTheme.colors.accent else MosqueTheme.colors.fill)
                                .clickable {
                                    val nextActive = !isActive
                                    val daysList = activeDaysList.toMutableList()
                                    if (nextActive) {
                                        if (!daysList.contains(englishName)) daysList.add(englishName)
                                    } else {
                                        daysList.remove(englishName)
                                    }
                                    activeDaysState = daysList.joinToString(",")
                                    onToggleDay(englishName, nextActive)
                                }
                                .padding(horizontal = MosqueTheme.spacing.md, vertical = MosqueTheme.spacing.xs),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = localizedName,
                                color = if (isActive) MosqueTheme.colors.onAccent else MosqueTheme.colors.textSecondary,
                                fontSize = MosqueTheme.typography.caption,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Mobile layout: Day Toggle buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    localizedDays.forEach { (englishName, localizedName) ->
                        val isActive = activeDaysList.contains(englishName)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                    .clip(RoundedCornerShape(10))
                                .background(if (isActive) MosqueTheme.colors.accent else MosqueTheme.colors.fill)
                                .border(
                                    width = 1.dp,
                                    color = if (isActive) MosqueTheme.colors.accent else MosqueTheme.colors.fillStrong,
                                    shape = RoundedCornerShape(10)
                                )
                                .clickable {
                                    val nextActive = !isActive
                                    val daysList = activeDaysList.toMutableList()
                                    if (nextActive) {
                                        if (!daysList.contains(englishName)) daysList.add(englishName)
                                    } else {
                                        daysList.remove(englishName)
                                    }
                                    activeDaysState = daysList.joinToString(",")
                                    onToggleDay(englishName, nextActive)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = localizedName,
                                color = if (isActive) MosqueTheme.colors.onAccent else MosqueTheme.colors.textPrimary,
                                fontSize = MosqueTheme.typography.caption,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Row 3: Aligned-Right "Schedule" button with calendar badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgedBox(
                    badge = {
                        if (datesCount > 0) {
                            Badge(
                                containerColor = MosqueTheme.colors.accent,
                                contentColor = MosqueTheme.colors.onAccent
                            ) {
                                Text(text = datesCount.toString())
                            }
                        }
                    }
                ) {
                    MosqueTertiaryButton(
                        onClick = onScheduleClick,
                        shape = RoundedCornerShape(MosqueTheme.radius.md),
                        contentPadding = PaddingValues(horizontal = MosqueTheme.spacing.lg, vertical = MosqueTheme.spacing.sm)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MosqueTheme.colors.accent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Lang.get("Schedule", "Jadwal Tanggal"),
                                fontSize = MosqueTheme.typography.caption,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}


// TV friendly time steppers dialog
@Composable
fun TimePickerDialog(
    initialStartTime: String,
    initialEndTime: String,
    isTv: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var startHour by remember { mutableIntStateOf(initialStartTime.split(":").getOrNull(0)?.toIntOrNull() ?: 0) }
    var startMinute by remember { mutableIntStateOf(initialStartTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0) }
    var endHour by remember { mutableIntStateOf(initialEndTime.split(":").getOrNull(0)?.toIntOrNull() ?: 0) }
    var endMinute by remember { mutableIntStateOf(initialEndTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(MosqueTheme.radius.lg),
            colors = CardDefaults.cardColors(containerColor = MosqueTheme.colors.surface),
            modifier = Modifier
                .width(420.dp)
                .padding(MosqueTheme.spacing.xl)
        ) {
            Column(
                modifier = Modifier.padding(MosqueTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
            ) {
                Text(
                    text = Lang.get("Edit Schedule Time", "Atur Jam Jadwal"),
                    color = MosqueTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = MosqueTheme.typography.body
                )

                // Start Time steppers
                TimeStepperField(
                    hour = startHour,
                    minute = startMinute,
                    label = Lang.get("Start Time", "Waktu Mulai"),
                    onHourChange = { startHour = it },
                    onMinuteChange = { startMinute = it }
                )

                // End Time steppers
                TimeStepperField(
                    hour = endHour,
                    minute = endMinute,
                    label = Lang.get("End Time", "Waktu Selesai"),
                    onHourChange = { endHour = it },
                    onMinuteChange = { endMinute = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg)
                ) {
                    MosqueDangerButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(MosqueTheme.radius.md),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Lang.get("Cancel", "Batal"), color = MosqueTheme.colors.textPrimary)
                    }

                    MosquePrimaryButton(
                        onClick = {
                            val start = "${startHour.toString().padStart(2, '0')}:${startMinute.toString().padStart(2, '0')}"
                            val end = "${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}"
                            onSave(start, end)
                        },
                        shape = RoundedCornerShape(MosqueTheme.radius.md),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Lang.get("Save", "Simpan"), color = MosqueTheme.colors.onAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TimeStepperField(
    hour: Int,
    minute: Int,
    label: String,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Column {
        Text(text = label, color = MosqueTheme.colors.textSecondary, fontSize = MosqueTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xs)
        ) {
            MosqueSecondaryButton(
                onClick = { onHourChange((hour - 1 + 24) % 24) },
                shape = RoundedCornerShape(MosqueTheme.radius.sm),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("−", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(
                text = hour.toString().padStart(2, '0'),
                color = MosqueTheme.colors.accent,
                fontSize = MosqueTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )
            MosqueSecondaryButton(
                onClick = { onHourChange((hour + 1) % 24) },
                shape = RoundedCornerShape(MosqueTheme.radius.sm),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("+", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            Text(
                text = ":",
                color = MosqueTheme.colors.textPrimary,
                fontSize = MosqueTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = MosqueTheme.spacing.xs)
            )

            MosqueSecondaryButton(
                onClick = { onMinuteChange((minute - 1 + 60) % 60) },
                shape = RoundedCornerShape(MosqueTheme.radius.sm),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("−", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(
                text = minute.toString().padStart(2, '0'),
                color = MosqueTheme.colors.accent,
                fontSize = MosqueTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )
            MosqueSecondaryButton(
                onClick = { onMinuteChange((minute + 1) % 60) },
                shape = RoundedCornerShape(MosqueTheme.radius.sm),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("+", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Multi date selection dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiDatePickerDialog(
    initialDates: String,
    isTv: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val datesList = remember {
        initialDates.split(",").filter { it.isNotEmpty() }.toMutableStateList()
    }

    var showSingleDatePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(MosqueTheme.radius.lg),
            colors = CardDefaults.cardColors(containerColor = MosqueTheme.colors.surface),
            modifier = Modifier
                .width(420.dp)
                .height(450.dp)
                .padding(MosqueTheme.spacing.xl)
        ) {
            Column(
                modifier = Modifier.padding(MosqueTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
            ) {
                Text(
                    text = Lang.get("Specific Dates List", "Daftar Tanggal Spesifik"),
                    color = MosqueTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = MosqueTheme.typography.body
                )

                // Add date button
                MosquePrimaryButton(
                    onClick = { showSingleDatePicker = true },
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text(Lang.get("+ Add Date", "+ Tambah Tanggal"), color = MosqueTheme.colors.onAccent, fontWeight = FontWeight.Bold)
                }

                // List of dates with delete option (extremely TV friendly)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    if (datesList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = MosqueTheme.spacing.huge),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = Lang.get("No specific dates added.", "Tidak ada tanggal spesifik."),
                                    color = MosqueTheme.colors.textMuted,
                                    fontSize = MosqueTheme.typography.labelSmall
                                )
                            }
                        }
                    } else {
                        items(datesList) { dateString ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(MosqueTheme.radius.md))
                                    .background(MosqueTheme.colors.fillSubtle)
                                    .padding(horizontal = MosqueTheme.spacing.lg, vertical = MosqueTheme.spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = dateString, color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.label)
                                MosqueIconButton(
                                    onClick = { datesList.remove(dateString) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MosqueTheme.colors.danger.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg)
                ) {
                    MosqueSecondaryButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(MosqueTheme.radius.md),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Lang.get("Cancel", "Batal"), color = MosqueTheme.colors.textPrimary)
                    }

                    MosquePrimaryButton(
                        onClick = {
                            onSave(datesList.joinToString(","))
                        },
                        shape = RoundedCornerShape(MosqueTheme.radius.md),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Lang.get("Save", "Simpan"), color = MosqueTheme.colors.onAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showSingleDatePicker) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = calendar.timeInMillis)

        DatePickerDialog(
            onDismissRequest = { showSingleDatePicker = false },
            confirmButton = {
                MosqueTextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = millis
                        val y = cal.get(Calendar.YEAR)
                        val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                        val dateString = "$y-$m-$d"
                        if (!datesList.contains(dateString)) {
                            datesList.add(dateString)
                        }
                    }
                    showSingleDatePicker = false
                }) {
                    Text(Lang.get("OK", "OK"), color = MosqueTheme.colors.accent)
                }
            },
            dismissButton = {
                MosqueTextButton(onClick = { showSingleDatePicker = false }) {
                    Text(Lang.get("Cancel", "Batal"), color = MosqueTheme.colors.textPrimary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = MosqueTheme.colors.surface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = MosqueTheme.colors.textPrimary,
                    headlineContentColor = MosqueTheme.colors.textPrimary,
                    weekdayContentColor = MosqueTheme.colors.textSecondary,
                    navigationContentColor = MosqueTheme.colors.textPrimary,
                    yearContentColor = MosqueTheme.colors.textPrimary,
                    currentYearContentColor = MosqueTheme.colors.accent,
                    selectedYearContentColor = MosqueTheme.colors.onAccent,
                    selectedYearContainerColor = MosqueTheme.colors.accent,
                    dayContentColor = MosqueTheme.colors.textPrimary,
                    selectedDayContentColor = MosqueTheme.colors.onAccent,
                    selectedDayContainerColor = MosqueTheme.colors.accent,
                    todayContentColor = MosqueTheme.colors.accent,
                    todayDateBorderColor = MosqueTheme.colors.accent
                )
            )
        }
    }
}
