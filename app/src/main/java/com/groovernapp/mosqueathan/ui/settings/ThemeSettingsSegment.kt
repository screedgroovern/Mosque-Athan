package com.groovernapp.mosqueathan.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onAddSlideshowSchedule: (String, String) -> Unit,
    onDeleteSlideshowSchedule: (String) -> Unit,
    onPreviewSlideshow: () -> Unit = {}
) {
    var showScheduleDialog by remember { mutableStateOf(false) }
    var startTimeText by remember { mutableStateOf("") }
    var endTimeText by remember { mutableStateOf("") }
    var validationErrorMsg by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    if (showScheduleDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Tambah Sesi Slideshow", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Masukkan jam mulai dan jam berakhir (Format 24 Jam):",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimePickerField(
                            value = startTimeText,
                            onValueChange = { startTimeText = it },
                            label = "Waktu Mulai",
                            placeholder = "HH:mm",
                            modifier = Modifier.weight(1f)
                        )
                        TimePickerField(
                            value = endTimeText,
                            onValueChange = { endTimeText = it },
                            label = "Waktu Selesai",
                            placeholder = "HH:mm",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    validationErrorMsg?.let {
                        Text(it, color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                TvFocusablePrimaryButton(
                    onClick = {
                        val startParts = startTimeText.split(":")
                        val endParts = endTimeText.split(":")
                        val sh = startParts.getOrNull(0)?.toIntOrNull()
                        val sm = startParts.getOrNull(1)?.toIntOrNull()
                        val eh = endParts.getOrNull(0)?.toIntOrNull()
                        val em = endParts.getOrNull(1)?.toIntOrNull()

                        if (sh == null || sm == null || eh == null || em == null || sh !in 0..23 || sm !in 0..59 || eh !in 0..23 || em !in 0..59) {
                            return@TvFocusablePrimaryButton
                        }

                        val startTimeStr = "%02d:%02d".format(sh, sm)
                        val endTimeStr = "%02d:%02d".format(eh, em)

                        // Calculate today's prayer times to validate collision
                        val calculatedTimes = PrayerCalculator.calculate(
                            latitude = config.latitude,
                            longitude = config.longitude,
                            calculationMethodName = config.calculationMethodName,
                            offsetImsak = config.offsetImsak,
                            offsetSubuh = config.offsetSubuh,
                            offsetSyuruq = config.offsetSyuruq,
                            offsetDzuhur = config.offsetDzuhur,
                            offsetAshar = config.offsetAshar,
                            offsetMaghrib = config.offsetMaghrib,
                            offsetIsya = config.offsetIsya,
                            kemenagCityId = config.kemenagCityId
                        )

                        val startMinutes = sh * 60 + sm
                        val endMinutes = eh * 60 + em

                         val prayers = listOf(
                            "Subuh" to ((calculatedTimes.subuh.hour * 60 + calculatedTimes.subuh.minute) to config.iqomahSubuh),
                            "Dzuhur" to ((calculatedTimes.dzuhur.hour * 60 + calculatedTimes.dzuhur.minute) to config.iqomahDzuhur),
                            "Ashar" to ((calculatedTimes.ashar.hour * 60 + calculatedTimes.ashar.minute) to config.iqomahAshar),
                            "Maghrib" to ((calculatedTimes.maghrib.hour * 60 + calculatedTimes.maghrib.minute) to config.iqomahMaghrib),
                            "Isya" to ((calculatedTimes.isya.hour * 60 + calculatedTimes.isya.minute) to config.iqomahIsya)
                        )

                        var collisionDetected: String? = null
                        for ((name, pair) in prayers) {
                            val prayerMin = pair.first
                            val iqomahDuration = pair.second
                            val startLeadMin = prayerMin - config.adzanLeadMinutes
                            val endBufferMin = prayerMin + iqomahDuration

                            // Check collision in minutes (handling overnight range is optional for validation but standard ranges need it)
                            val isColliding = if (startMinutes <= endMinutes) {
                                // standard range
                                !(endMinutes < startLeadMin || startMinutes > endBufferMin)
                            } else {
                                // overnight range
                                startMinutes <= endBufferMin || endMinutes >= startLeadMin
                            }

                            if (isColliding) {
                                val prayerTimeFormatted = "%02d:%02d".format(prayerMin / 60, prayerMin % 60)
                                val endIqomahFormatted = "%02d:%02d".format(endBufferMin / 60, endBufferMin % 60)
                                collisionDetected = "Waktu tabrakan dengan sesi sholat $name (Adzan $prayerTimeFormatted sampai Iqomah selesai $endIqomahFormatted)."
                                break
                            }
                        }

                        if (collisionDetected == null) {
                            onAddSlideshowSchedule(startTimeStr, endTimeStr)
                            startTimeText = ""
                            endTimeText = ""
                        }
                    }
                ) {
                    Text("Tambah", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) {
                    Text("Batal", color = Color.White)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "JADWAL AKTIF SLIDESHOW",
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvFocusableButton(
                        onClick = onPreviewSlideshow,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Preview Slideshow", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TvFocusablePrimaryButton(
                        onClick = { showScheduleDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Tambah", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (slideshowSchedules.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Slideshow akan aktif jika jadwal di-set. Jika kosong, slideshow tidak pernah berputar.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(slideshowSchedules) { schedule ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⏰  ",
                            color = Color(0xFFFFD54F),
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${schedule.startTime} s/d ${schedule.endTime}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TvFocusableDangerButton(
                        onClick = { onDeleteSlideshowSchedule(schedule.id) },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Hapus", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
