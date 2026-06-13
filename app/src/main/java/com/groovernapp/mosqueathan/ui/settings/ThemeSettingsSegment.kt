package com.groovernapp.mosqueathan.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import com.groovernapp.mosqueathan.util.Lang
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.data.MediaEntity
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.data.SlideshowScheduleEntity
import com.groovernapp.mosqueathan.util.PrayerCalculator

@Composable
fun ThemeSettingsSegment(
    config: MosqueConfigEntity,
    transitionType: String,
    slideIntervalSeconds: Int,
    videoWaitUntilEnd: Boolean,
    includeDefaultImage: Boolean,
    mediaList: List<MediaEntity>,
    slideshowSchedules: List<SlideshowScheduleEntity>,
    onUpdateThemeSettings: (String, Int, Boolean, Boolean) -> Unit,
    onAddMediaItem: () -> Unit,
    onDeleteMediaItem: (MediaEntity) -> Unit,
    onSelectCustomBackground: () -> Unit,
    onClearCustomBackground: () -> Unit,
    onAddSlideshowSchedule: (String, Boolean, String, String) -> Unit,
    onDeleteSlideshowSchedule: (String) -> Unit,
    onPreviewSlideshow: () -> Unit = {}
) {
    var isSpecificDate by remember { mutableStateOf(false) }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var specificDateText by remember { mutableStateOf("") }
    var startTimeText by remember { mutableStateOf("00:00") }
    var endTimeText by remember { mutableStateOf("00:00") }
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

    val context = androidx.compose.ui.platform.LocalContext.current

    if (showListDialog) {
        AlertDialog(
            onDismissRequest = { showListDialog = false },
            title = { Text(Lang.get("Slideshow Times List", "Daftar Jadwal Slideshow"), color = Color.White) },
            text = {
                Box(modifier = Modifier.width(400.dp).height(300.dp)) {
                    if (slideshowSchedules.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(Lang.get("No slideshow schedules set.", "Tidak ada jadwal slideshow."), color = Color.White.copy(alpha = 0.5f))
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(slideshowSchedules) { schedule ->
                                val dayLabel = if (schedule.isSpecificDate) {
                                    schedule.day
                                } else {
                                    daysOfWeek.firstOrNull { it.first.equals(schedule.day, ignoreCase = true) }?.second ?: schedule.day
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                                        onClick = { onDeleteSlideshowSchedule(schedule.id) },
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

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        // Section 1: Dashboard Background Settings
        item {
            Text(
                "1. BACKGROUND DASHBOARD",
                color = Color(0xFFFFD54F),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Background Dashboard Kustom",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val activeBg = if (config.customBackgroundPath.isNotEmpty()) {
                        config.customBackgroundPath.substringAfterLast("/")
                    } else {
                        "main_bg_default.png (Bawaan)"
                    }
                    Text(
                        text = "Aktif saat ini: $activeBg",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvFocusableButton(
                        onClick = onSelectCustomBackground,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Ganti", color = Color.White, fontSize = 13.sp)
                    }
                    if (config.customBackgroundPath.isNotEmpty()) {
                        TvFocusableDangerButton(
                            onClick = onClearCustomBackground,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Hapus", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Section 2: Slideshow Mode Settings
        item {
            Text(
                "2. SLIDE SHOW MODE",
                color = Color(0xFFFFD54F),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Include Default Image switch
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = com.groovernapp.mosqueathan.util.Lang.get(
                            "Include Default Image (Masjidil Haram)",
                            "Sertakan Gambar Default (Masjidil Haram)"
                        ),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = com.groovernapp.mosqueathan.util.Lang.get(
                            "Include or exclude the default image in the slideshow rotation",
                            "Sertakan atau kecualikan gambar default dalam rotasi slideshow"
                        ),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = includeDefaultImage,
                    onCheckedChange = { checked ->
                        onUpdateThemeSettings(transitionType, slideIntervalSeconds, videoWaitUntilEnd, checked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFFD54F),
                        checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.5f)
                    )
                )
            }
        }

        // Transition Type
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = com.groovernapp.mosqueathan.util.Lang.get("Transition Animation", "Animasi Transisi"),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("FADE", "SLIDE", "ZOOM", "INSTANT").forEach { type ->
                        val isSelected = transitionType == type
                        TvFocusableButton(
                            onClick = {
                                onUpdateThemeSettings(type, slideIntervalSeconds, videoWaitUntilEnd, includeDefaultImage)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                type,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Slideshow Interval
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = com.groovernapp.mosqueathan.util.Lang.get("Slideshow Interval", "Interval Slideshow"),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TvFocusableButton(
                        onClick = {
                            if (slideIntervalSeconds > 5) {
                                onUpdateThemeSettings(transitionType, slideIntervalSeconds - 5, videoWaitUntilEnd, includeDefaultImage)
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("-", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "$slideIntervalSeconds " + com.groovernapp.mosqueathan.util.Lang.get("seconds", "detik"),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TvFocusableButton(
                        onClick = {
                            if (slideIntervalSeconds < 120) {
                                onUpdateThemeSettings(transitionType, slideIntervalSeconds + 5, videoWaitUntilEnd, includeDefaultImage)
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Video Playback Behavior
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = com.groovernapp.mosqueathan.util.Lang.get("Video Playback Duration", "Durasi Putar Video"),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = com.groovernapp.mosqueathan.util.Lang.get(
                            "Wait for video to finish playing or follow regular slide interval",
                            "Tunggu video selesai diputar atau ikuti interval slide biasa"
                        ),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(false, true).forEach { waitEnd ->
                        val isSelected = videoWaitUntilEnd == waitEnd
                        val label = if (waitEnd)
                            com.groovernapp.mosqueathan.util.Lang.get("Wait for Video", "Selesai Video")
                        else
                            com.groovernapp.mosqueathan.util.Lang.get("Follow Interval", "Ikuti Interval")
                        TvFocusableButton(
                            onClick = {
                                onUpdateThemeSettings(transitionType, slideIntervalSeconds, waitEnd, includeDefaultImage)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                label,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Add Media Button
        item {
            TvFocusablePrimaryButton(
                onClick = onAddMediaItem,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = com.groovernapp.mosqueathan.util.Lang.get("Add Image / Video to Slideshow", "Tambah Gambar / Video ke Slideshow"),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // Media list
        if (mediaList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada media slideshow. Tambahkan file untuk memulai.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(mediaList) { mediaItem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.7f)) {
                        Text(
                            text = if (mediaItem.isVideo) "📹 VIDEO" else "🖼️ GAMBAR",
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = mediaItem.filePath.substringAfterLast("/"),
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                    TvFocusableDangerButton(
                        onClick = { onDeleteMediaItem(mediaItem) },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Hapus", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // Slideshow active schedules section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "2. JADWAL AKTIF SLIDESHOW",
                color = Color(0xFFFFD54F),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            
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
            
            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

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
                    placeholder = "00:00",
                    modifier = Modifier.weight(1f)
                )

                // End Time
                TimePickerField(
                    value = endTimeText,
                    onValueChange = { endTimeText = it },
                    label = Lang.get("End Time (HH:mm)", "Selesai (HH:mm)"),
                    placeholder = "00:00",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons: Add, Slideshow Times, Preview Slideshow
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TvFocusablePrimaryButton(
                    onClick = {
                        val dayVal = if (isSpecificDate) specificDateText else daysOfWeek[selectedDayIndex].first
                        if (dayVal.isNotEmpty() && startTimeText.isNotEmpty() && endTimeText.isNotEmpty()) {
                            onAddSlideshowSchedule(dayVal, isSpecificDate, startTimeText, endTimeText)
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
                    Text(Lang.get("Slideshow Times", "Jadwal Slideshow"), color = Color.White, fontWeight = FontWeight.Bold)
                }

                TvFocusableButton(
                    onClick = onPreviewSlideshow,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(Lang.get("Preview Slideshow", "Preview Slideshow"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
