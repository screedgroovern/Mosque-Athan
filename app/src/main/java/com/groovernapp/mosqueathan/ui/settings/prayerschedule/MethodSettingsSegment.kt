package com.groovernapp.mosqueathan.ui.settings.prayerschedule

import com.groovernapp.mosqueathan.ui.settings.*

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.key.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.data.KemenagScheduleEntity
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun MethodSettingsSegment(
    calculationMethodName: String,
    kemenagCityId: String,
    kemenagCityName: String,
    kemenagDuration: String,
    kemenagTargetDate: String,
    citySearchResults: List<Pair<String, String>>,
    isSearchingCities: Boolean,
    kemenagSchedules: List<KemenagScheduleEntity>,
    onMethodChange: (String) -> Unit,
    onKemenagCitySelected: (id: String, name: String) -> Unit,
    onKemenagDurationChange: (String) -> Unit,
    onKemenagTargetDateChange: (String) -> Unit,
    onSearchKemenagCities: (String) -> Unit,
    onDownloadKemenagSchedules: () -> Unit = {},
    isCompact: Boolean = false
) {
    val calculationMethods = listOf(
        "SINGAPORE", "UMM_AL_QURA", "MUSLIM_WORLD_LEAGUE", "EGYPTIAN",
        "KARACHI", "DUBAI", "KUWAIT", "QATAR", "KEMENAG"
    )
    val scope = rememberCoroutineScope()
    var showCitySearchDialog by remember { mutableStateOf(false) }
    var showSchedulesDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = if (isCompact) {
                1
            } else {
                val width = maxWidth
                if (width >= 750.dp) 3 else if (width >= 450.dp) 2 else 1
            }

            val rows = calculationMethods.chunked(columns)
            Column(
                verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                    ) {
                        rowItems.forEach { method ->
                            CalculationMethodItem(
                                method = method,
                                isSelected = calculationMethodName == method,
                                onMethodChange = onMethodChange,
                                isCompact = isCompact,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size < columns) {
                            Spacer(modifier = Modifier.weight((columns - rowItems.size).toFloat()))
                        }
                    }
                }
            }
        }

        if (calculationMethodName == "KEMENAG") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = com.groovernapp.mosqueathan.util.Lang.get("Selected City: ", "Kota Terpilih: ") + kemenagCityName,
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.body,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = com.groovernapp.mosqueathan.util.Lang.get("Schedule Duration: ", "Periode Waktu Jadwal: "),
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.body,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                var durationExpanded by remember { mutableStateOf(false) }
                val durationList = listOf(
                    "1_WEEK" to com.groovernapp.mosqueathan.util.Lang.get("1 Week Forward", "1 Minggu ke Depan"),
                    "2_WEEKS" to com.groovernapp.mosqueathan.util.Lang.get("2 Weeks Forward", "2 Minggu ke Depan"),
                    "3_WEEKS" to com.groovernapp.mosqueathan.util.Lang.get("3 Weeks Forward", "3 Minggu ke Depan"),
                    "1_MONTH" to com.groovernapp.mosqueathan.util.Lang.get("1 Month Forward", "1 Bulan ke Depan"),
                    "2_MONTHS" to com.groovernapp.mosqueathan.util.Lang.get("2 Months Forward", "2 Bulan ke Depan"),
                    "3_MONTHS" to com.groovernapp.mosqueathan.util.Lang.get("3 Months Forward", "3 Bulan ke Depan"),
                    "6_MONTHS" to com.groovernapp.mosqueathan.util.Lang.get("6 Months Forward", "6 Bulan ke Depan"),
                    "1_YEAR" to com.groovernapp.mosqueathan.util.Lang.get("1 Year Forward", "1 Tahun ke Depan"),
                    "SPECIFIC_DATE" to com.groovernapp.mosqueathan.util.Lang.get("Until Specific Date", "Hingga Tanggal Tertentu")
                )
                val selectedDurationText = durationList.firstOrNull { it.first == kemenagDuration }?.second ?: kemenagDuration

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(MosqueTheme.radius.md))
                        .background(MosqueTheme.colors.fillSubtle)
                        .clickable { durationExpanded = true }
                        .padding(horizontal = MosqueTheme.spacing.xl),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = selectedDurationText, color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.body)
                    DropdownMenu(
                        expanded = durationExpanded,
                        onDismissRequest = { durationExpanded = false },
                        modifier = Modifier.background(MosqueTheme.colors.surface)
                    ) {
                        durationList.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(text = label, color = MosqueTheme.colors.textPrimary) },
                                onClick = {
                                    onKemenagDurationChange(value)
                                    durationExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
                ) {
                    if (kemenagDuration == "SPECIFIC_DATE") {
                        DatePickerField(
                            value = kemenagTargetDate,
                            onValueChange = { onKemenagTargetDateChange(it) },
                            label = com.groovernapp.mosqueathan.util.Lang.get("Target End Date", "Tanggal Batas Akhir"),
                            placeholder = "2026-12-31",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = kemenagCityName.ifEmpty { "-" },
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                text = com.groovernapp.mosqueathan.util.Lang.get("City", "Kota"),
                                color = MosqueTheme.colors.textSecondary
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MosqueTheme.colors.border,
                            unfocusedBorderColor = MosqueTheme.colors.border,
                            focusedTextColor = MosqueTheme.colors.textPrimary,
                            unfocusedTextColor = MosqueTheme.colors.textPrimary
                        ),
                        trailingIcon = {
                            TextButton(onClick = { showCitySearchDialog = true }) {
                                Text(
                                    text = com.groovernapp.mosqueathan.util.Lang.get("Change", "Ubah"),
                                    color = MosqueTheme.colors.accent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp && (event.key == Key.DirectionCenter || event.key == Key.Enter)) {
                                showCitySearchDialog = true
                                true
                            } else false
                        },
                        singleLine = true
                    )
                }

                var isDownloading by remember { mutableStateOf(false) }


                Spacer(modifier = Modifier.height(16.dp))
                TvFocusablePrimaryButton(
                    onClick = {
                        isDownloading = true
                        onDownloadKemenagSchedules()
                        scope.launch {
                            delay(1500)
                            isDownloading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MosqueTheme.colors.onAccent,
                            strokeWidth = MosqueTheme.stroke.thin
                        )
                    } else {
                        Text(
                            text = com.groovernapp.mosqueathan.util.Lang.get("Download Prayer Times", "Unduh Jadwal Sholat"),
                            color = MosqueTheme.colors.onAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = MosqueTheme.typography.label
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                TvFocusablePrimaryButton(
                    onClick = { showSchedulesDialog = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        text = com.groovernapp.mosqueathan.util.Lang.get("View Downloaded Schedules", "Lihat Jadwal Terunduh"),
                        color = MosqueTheme.colors.onAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = MosqueTheme.typography.label
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showCitySearchDialog) {
        AlertDialog(
            onDismissRequest = { showCitySearchDialog = false },
            containerColor = MosqueTheme.colors.surfaceRaised,
            title = {
                Text(
                    text = com.groovernapp.mosqueathan.util.Lang.get("Search City", "Cari Kota"),
                    color = MosqueTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            onSearchKemenagCities(it)
                        },
                        placeholder = { Text(com.groovernapp.mosqueathan.util.Lang.get("Type city name...", "Ketik nama kota..."), color = MosqueTheme.colors.textMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MosqueTheme.colors.accent,
                            unfocusedBorderColor = MosqueTheme.colors.border,
                            focusedTextColor = MosqueTheme.colors.textPrimary,
                            unfocusedTextColor = MosqueTheme.colors.textPrimary
                        ),
                        trailingIcon = {
                            if (isSearchingCities) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MosqueTheme.colors.accent,
                                    strokeWidth = MosqueTheme.stroke.thin
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                    ) {
                        items(citySearchResults) { (id, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(MosqueTheme.radius.md))
                                    .background(MosqueTheme.colors.fillSubtle)
                                    .clickable {
                                        onKemenagCitySelected(id, name)
                                        showCitySearchDialog = false
                                    }
                                    .padding(horizontal = MosqueTheme.spacing.xl, vertical = MosqueTheme.spacing.lg),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = name, color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.label)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCitySearchDialog = false }) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Close", "Tutup"), color = MosqueTheme.colors.accent)
                }
            }
        )
    }

    if (showSchedulesDialog) {
        AlertDialog(
            onDismissRequest = { showSchedulesDialog = false },
            containerColor = MosqueTheme.colors.surfaceRaised,
            title = {
                Text(
                    text = com.groovernapp.mosqueathan.util.Lang.get("Downloaded Schedules", "Jadwal Terunduh"),
                    color = MosqueTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (kemenagSchedules.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(MosqueTheme.spacing.huge), contentAlignment = Alignment.Center) {
                        Text(
                            text = com.groovernapp.mosqueathan.util.Lang.get("No schedules downloaded yet", "Belum ada jadwal yang diunduh"),
                            color = MosqueTheme.colors.textMuted,
                            fontSize = MosqueTheme.typography.label
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(350.dp),
                        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                    ) {
                        val grouped = kemenagSchedules.groupBy { 
                            val parts = it.dateString.split("-")
                            if (parts.size >= 2) "${parts[1]}-${parts[0]}" else "Unknown"
                        }
                        
                        grouped.forEach { (monthYear, schedules) ->
                            item {
                                val displayMonthYear = try {
                                    val parts = monthYear.split("-")
                                    val cal = java.util.Calendar.getInstance()
                                    cal.set(parts[1].toInt(), parts[0].toInt() - 1, 1)
                                    java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("id")).format(cal.time)
                                } catch (e: Exception) { monthYear }

                                Text(
                                    text = displayMonthYear,
                                    color = MosqueTheme.colors.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = MosqueTheme.typography.body,
                                    modifier = Modifier.padding(top = MosqueTheme.spacing.md, bottom = MosqueTheme.spacing.xs)
                                )
                            }
                            items(schedules) { sch ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(MosqueTheme.radius.md))
                                        .background(MosqueTheme.colors.fillSubtle)
                                        .padding(MosqueTheme.spacing.lg),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = sch.dateString,
                                            color = MosqueTheme.colors.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = MosqueTheme.typography.label
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Imsak: ${sch.imsak} | Subuh: ${sch.subuh} | Terbit: ${sch.terbit}",
                                            color = MosqueTheme.colors.textSecondary,
                                            fontSize = MosqueTheme.typography.caption
                                        )
                                        Text(
                                            text = "Dzuhur: ${sch.dzuhur} | Ashar: ${sch.ashar} | Maghrib: ${sch.maghrib} | Isya': ${sch.isya}",
                                            color = MosqueTheme.colors.textSecondary,
                                            fontSize = MosqueTheme.typography.caption
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSchedulesDialog = false }) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Close", "Tutup"), color = MosqueTheme.colors.accent)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    if (isPressed) {
        LaunchedEffect(isPressed) {
            if (isPressed) {
                showDialog = true
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        interactionSource = interactionSource,
        label = { Text(label, color = MosqueTheme.colors.textSecondary) },
        placeholder = { Text(placeholder, color = MosqueTheme.colors.textDisabled) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MosqueTheme.colors.accent,
            unfocusedBorderColor = MosqueTheme.colors.border,
            focusedTextColor = MosqueTheme.colors.textPrimary,
            unfocusedTextColor = MosqueTheme.colors.textPrimary
        ),
        modifier = modifier.onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && (event.key == Key.DirectionCenter || event.key == Key.Enter)) {
                showDialog = true
                true
            } else {
                false
            }
        }.clickable { showDialog = true }
    )

    if (showDialog) {
        val parts = value.split("-")
        var initialMillis: Long? = null
        if (parts.size == 3) {
            val year = parts[0].toIntOrNull()
            val month = parts[1].toIntOrNull()
            val day = parts[2].toIntOrNull()
            if (year != null && month != null && day != null) {
                val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                cal.set(year, month - 1, day, 0, 0, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                initialMillis = cal.timeInMillis
            }
        }
        
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = millis
                        val y = cal.get(java.util.Calendar.YEAR)
                        val m = (cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
                        val d = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                        onValueChange("$y-$m-$d")
                    }
                    showDialog = false
                }) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("OK", "OK"), color = MosqueTheme.colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Cancel", "Batal"), color = MosqueTheme.colors.textPrimary)
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

@Composable
private fun CalculationMethodItem(
    method: String,
    isSelected: Boolean,
    onMethodChange: (String) -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val horizontalPadding = if (isCompact) 10.dp else 12.dp
    val verticalPadding = if (isCompact) 6.dp else 8.dp
    val radioSize = if (isCompact) 18.dp else 20.dp
    val radioInnerPadding = if (isCompact) 3.dp else 4.dp
    val spacerWidth = if (isCompact) 8.dp else 10.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MosqueTheme.radius.md))
            .background(if (isSelected) MosqueTheme.colors.accent.copy(alpha = 0.15f) else MosqueTheme.colors.fillSubtle)
            .border(
                width = 1.dp,
                color = if (isSelected) MosqueTheme.colors.accent else MosqueTheme.colors.transparent,
                shape = RoundedCornerShape(MosqueTheme.radius.md)
            )
            .clickable { onMethodChange(method) }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(radioSize)
                .border(
                    width = 2.dp,
                    color = if (isSelected) MosqueTheme.colors.accent else MosqueTheme.colors.textMuted,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .padding(radioInnerPadding)
                .background(
                    color = if (isSelected) MosqueTheme.colors.accent else MosqueTheme.colors.transparent,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        Spacer(modifier = Modifier.width(spacerWidth))
        Text(
            text = method,
            color = MosqueTheme.colors.textPrimary,
            fontSize = if (isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.label,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
