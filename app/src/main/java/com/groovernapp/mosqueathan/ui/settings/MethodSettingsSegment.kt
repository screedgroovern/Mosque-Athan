package com.groovernapp.mosqueathan.ui.settings

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
    onDownloadKemenagSchedules: () -> Unit = {}
) {
    val calculationMethods = listOf(
        "SINGAPORE", "UMM_AL_QURA", "MUSLIM_WORLD_LEAGUE", "EGYPTIAN",
        "KARACHI", "DUBAI", "KUWAIT", "QATAR", "KEMENAG"
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxHeight(0.8f)
    ) {
        items(calculationMethods) { method ->
            val isSelected = calculationMethodName == method
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFFFFD54F).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFFFFD54F) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onMethodChange(method) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onMethodChange(method) },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFD54F))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = method,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }

        if (calculationMethodName == "KEMENAG") {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = com.groovernapp.mosqueathan.util.Lang.get("Selected City: ", "Kota Terpilih: ") + kemenagCityName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = com.groovernapp.mosqueathan.util.Lang.get("Schedule Duration: ", "Periode Waktu Jadwal: "),
                    color = Color.White,
                    fontSize = 16.sp,
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { durationExpanded = true }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(text = selectedDurationText, color = Color.White, fontSize = 16.sp)
                    DropdownMenu(
                        expanded = durationExpanded,
                        onDismissRequest = { durationExpanded = false },
                        modifier = Modifier.background(Color(0xFF0C120F))
                    ) {
                        durationList.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(text = label, color = Color.White) },
                                onClick = {
                                    onKemenagDurationChange(value)
                                    durationExpanded = false
                                }
                            )
                        }
                    }
                }

                if (kemenagDuration == "SPECIFIC_DATE") {
                    Spacer(modifier = Modifier.height(16.dp))
                    DatePickerField(
                        value = kemenagTargetDate,
                        onValueChange = { onKemenagTargetDateChange(it) },
                        label = com.groovernapp.mosqueathan.util.Lang.get("Target End Date (YYYY-MM-DD)", "Tanggal Batas Akhir (YYYY-MM-DD)"),
                        placeholder = "2026-12-31",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                var searchQuery by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onSearchKemenagCities(it)
                    },
                    label = {
                        Text(
                            text = com.groovernapp.mosqueathan.util.Lang.get("Search City (Indonesia)", "Cari Kota (Indonesia)"),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFD54F),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                TvFocusablePrimaryButton(
                    onClick = onDownloadKemenagSchedules,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        text = com.groovernapp.mosqueathan.util.Lang.get("Download Prayer Times", "Unduh Jadwal Sholat"),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            if (isSearchingCities) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFFD54F))
                    }
                }
            }

            items(citySearchResults) { (id, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onKemenagCitySelected(id, name) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = name, color = Color.White, fontSize = 14.sp)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = com.groovernapp.mosqueathan.util.Lang.get("Downloaded Schedules Preview", "Pratinjau Jadwal Terunduh"),
                    color = Color(0xFFFFD54F),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (kemenagSchedules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = com.groovernapp.mosqueathan.util.Lang.get("No schedules downloaded yet", "Belum ada jadwal yang diunduh"),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(kemenagSchedules) { sch ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = sch.dateString,
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Imsak: ${sch.imsak} | Subuh: ${sch.subuh} | Terbit: ${sch.terbit}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Dzuhur: ${sch.dzuhur} | Ashar: ${sch.ashar} | Maghrib: ${sch.maghrib} | Isya: ${sch.isya}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
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
        label = { Text(label, color = Color.White.copy(alpha = 0.6f)) },
        placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.3f)) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFFFD54F),
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
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
                    Text(com.groovernapp.mosqueathan.util.Lang.get("OK", "OK"), color = Color(0xFFFFD54F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Cancel", "Batal"), color = Color.White)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF101915))
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    weekdayContentColor = Color.White.copy(alpha = 0.6f),
                    navigationContentColor = Color.White,
                    yearContentColor = Color.White,
                    currentYearContentColor = Color(0xFFFFD54F),
                    selectedYearContentColor = Color.Black,
                    selectedYearContainerColor = Color(0xFFFFD54F),
                    dayContentColor = Color.White,
                    selectedDayContentColor = Color.Black,
                    selectedDayContainerColor = Color(0xFFFFD54F),
                    todayContentColor = Color(0xFFFFD54F),
                    todayDateBorderColor = Color(0xFFFFD54F)
                )
            )
        }
    }
}
