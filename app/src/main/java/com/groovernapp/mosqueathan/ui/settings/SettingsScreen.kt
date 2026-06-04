package com.groovernapp.mosqueathan.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.R
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.data.AnnouncementEntity
import com.groovernapp.mosqueathan.data.MediaEntity

@Composable
fun SettingsScreen(
    config: MosqueConfigEntity,
    announcements: List<AnnouncementEntity>,
    onSaveConfig: (
        latitude: Double,
        longitude: Double,
        iqomahSubuh: Int,
        iqomahDzuhur: Int,
        iqomahAshar: Int,
        iqomahMaghrib: Int,
        iqomahIsya: Int,
        calculationMethodName: String,
        adzanLeadMinutes: Int
    ) -> Unit,
    onAddAnnouncement: (String) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onResetToDefaults: () -> Unit,
    onSelectCustomBackground: (android.net.Uri) -> Unit,
    onClearCustomBackground: () -> Unit,
    mediaList: List<MediaEntity>,
    onAddMediaItem: (android.net.Uri) -> Unit,
    onDeleteMediaItem: (MediaEntity) -> Unit,
    onUpdateThemeSettings: (String, Int, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val singleBackgroundLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onSelectCustomBackground(uri)
        }
    }

    val mediaPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onAddMediaItem(uri)
        }
    }

    var selectedCategory by remember { mutableStateOf(SettingsCategory.LOCATION) }

    // Location States
    var latText by remember(config.latitude) { mutableStateOf(config.latitude.toString()) }
    var lngText by remember(config.longitude) { mutableStateOf(config.longitude.toString()) }

    // Iqomah States
    var iqomahSubuh by remember(config.iqomahSubuh) { mutableStateOf(config.iqomahSubuh) }
    var iqomahDzuhur by remember(config.iqomahDzuhur) { mutableStateOf(config.iqomahDzuhur) }
    var iqomahAshar by remember(config.iqomahAshar) { mutableStateOf(config.iqomahAshar) }
    var iqomahMaghrib by remember(config.iqomahMaghrib) { mutableStateOf(config.iqomahMaghrib) }
    var iqomahIsya by remember(config.iqomahIsya) { mutableStateOf(config.iqomahIsya) }
    var adzanLeadMinutes by remember(config.adzanLeadMinutes) { mutableStateOf(config.adzanLeadMinutes) }

    // Calculation Method State
    var calculationMethodName by remember(config.calculationMethodName) { mutableStateOf(config.calculationMethodName) }

    // Announcement input state
    var newAnnouncementText by remember { mutableStateOf("") }

    val calculationMethods = listOf(
        "SINGAPORE", "UMM_AL_QURA", "MUSLIM_WORLD_LEAGUE", "EGYPTIAN", "KARACHI", "DUBAI", "KUWAIT", "QATAR"
    )

    val customBgBitmap = remember(config.customBackgroundPath) {
        if (config.customBackgroundPath.isNotEmpty() && java.io.File(config.customBackgroundPath).exists()) {
            try {
                android.graphics.BitmapFactory.decodeFile(config.customBackgroundPath)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (customBgBitmap != null) {
            Image(
                bitmap = customBgBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(R.drawable.main_bg_default),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color(0xFF101915).copy(alpha = 0.90f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp)
        ) {
            // Sidebar Navigation
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .padding(end = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PENGATURAN",
                    color = Color(0xFFFFD54F),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 12.dp)
                )

                SettingsCategory.values().forEach { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFFFD54F).copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = category.displayName,
                            color = if (isSelected) Color(0xFFFFD54F) else Color.White,
                            fontSize = 18.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Kembali", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Right Pane Settings Editor
            Card(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedCategory.displayName,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedCategory.description,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(28.dp))

                        // Category specific UI
                        when (selectedCategory) {
                            SettingsCategory.LOCATION -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    OutlinedTextField(
                                        value = latText,
                                        onValueChange = { latText = it },
                                        label = { Text("Latitude") },
                                        colors = textFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = lngText,
                                        onValueChange = { lngText = it },
                                        label = { Text("Longitude") },
                                        colors = textFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            SettingsCategory.IQOMAH -> {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxHeight(0.85f)
                                ) {
                                    item { IqomahAdjustmentRow("Subuh", iqomahSubuh) { iqomahSubuh = it } }
                                    item { IqomahAdjustmentRow("Dzuhur", iqomahDzuhur) { iqomahDzuhur = it } }
                                    item { IqomahAdjustmentRow("Ashar", iqomahAshar) { iqomahAshar = it } }
                                    item { IqomahAdjustmentRow("Maghrib", iqomahMaghrib) { iqomahMaghrib = it } }
                                    item { IqomahAdjustmentRow("Isya", iqomahIsya) { iqomahIsya = it } }
                                    item { IqomahAdjustmentRow("Persiapan Adzan", adzanLeadMinutes) { adzanLeadMinutes = it } }
                                }
                            }

                            SettingsCategory.METHOD -> {
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
                                                .clickable { calculationMethodName = method }
                                                .padding(horizontal = 20.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { calculationMethodName = method },
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
                                }
                            }

                            SettingsCategory.ANNOUNCEMENTS -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newAnnouncementText,
                                            onValueChange = { newAnnouncementText = it },
                                            placeholder = { Text("Ketik pengumuman baru di sini...") },
                                            colors = textFieldColors(),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                if (newAnnouncementText.isNotBlank()) {
                                                    onAddAnnouncement(newAnnouncementText.trim())
                                                    newAnnouncementText = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(56.dp)
                                        ) {
                                            Text("Tambah", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        items(announcements) { announcement ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White.copy(alpha = 0.05f))
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = announcement.content,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    modifier = Modifier.weight(0.85f)
                                                )
                                                Button(
                                                    onClick = { onDeleteAnnouncement(announcement.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
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

                            SettingsCategory.THEME -> {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxHeight(0.85f)
                                ) {
                                    // Section 1: Single Background
                                    item {
                                        Text("PILIHAN 1: Gambar Latar Belakang Tunggal", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val hasCustomBackground = config.customBackgroundPath.isNotEmpty()
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)).padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (hasCustomBackground) "Gambar kustom aktif" else "Gambar default aktif (Masjidil Haram)",
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                modifier = Modifier.weight(0.6f)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { singleBackgroundLauncher.launch("image/*") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Text("Pilih Gambar", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                }
                                                if (hasCustomBackground) {
                                                    Button(
                                                        onClick = onClearCustomBackground,
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                        modifier = Modifier.height(36.dp)
                                                    ) {
                                                        Text("Hapus", color = Color.White, fontSize = 13.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Section 2: Slideshow Presentation
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("PILIHAN 2: Presentasi Slideshow (Multi Gambar & Video)", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    // Transition Type
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)).padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Animasi Transisi", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf("FADE", "SLIDE", "ZOOM", "INSTANT").forEach { type ->
                                                    val isSelected = config.transitionType == type
                                                    Button(
                                                        onClick = { onUpdateThemeSettings(type, config.slideIntervalSeconds, config.videoWaitUntilEnd) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.1f)
                                                        ),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Text(type, color = if (isSelected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Interval Selector
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)).padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Interval Slideshow", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        if (config.slideIntervalSeconds > 5) {
                                                            onUpdateThemeSettings(config.transitionType, config.slideIntervalSeconds - 5, config.videoWaitUntilEnd)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(0.dp),
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Text("-", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Text(
                                                    text = "${config.slideIntervalSeconds} detik",
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Button(
                                                    onClick = {
                                                        if (config.slideIntervalSeconds < 120) {
                                                            onUpdateThemeSettings(config.transitionType, config.slideIntervalSeconds + 5, config.videoWaitUntilEnd)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
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
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)).padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Durasi Putar Video", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("Tunggu video selesai diputar atau ikuti interval slide biasa", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(false, true).forEach { waitEnd ->
                                                    val isSelected = config.videoWaitUntilEnd == waitEnd
                                                    val label = if (waitEnd) "Selesai Video" else "Ikuti Interval"
                                                    Button(
                                                        onClick = { onUpdateThemeSettings(config.transitionType, config.slideIntervalSeconds, waitEnd) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.1f)
                                                        ),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Text(label, color = if (isSelected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Add Media Item Button
                                    item {
                                        Button(
                                            onClick = {
                                                mediaPickerLauncher.launch(
                                                    androidx.activity.result.PickVisualMediaRequest(
                                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                                    )
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Text("Tambah Gambar / Video ke Slideshow", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }

                                    // Media playlist items
                                    if (mediaList.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Belum ada media slideshow. Tambahkan file untuk memulai.", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                                            }
                                        }
                                    } else {
                                        items(mediaList) { mediaItem ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)).padding(horizontal = 16.dp, vertical = 10.dp),
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
                                                Button(
                                                    onClick = { onDeleteMediaItem(mediaItem) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
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

                            SettingsCategory.RESET -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = onResetToDefaults,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(56.dp).padding(horizontal = 32.dp)
                                    ) {
                                        Text("RESET SEMUA PENGATURAN KE DEFAULT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Save / Apply Button
                    if (selectedCategory in listOf(SettingsCategory.LOCATION, SettingsCategory.IQOMAH, SettingsCategory.METHOD)) {
                        Button(
                            onClick = {
                                val lat = latText.toDoubleOrNull() ?: config.latitude
                                val lng = lngText.toDoubleOrNull() ?: config.longitude
                                onSaveConfig(
                                    lat,
                                    lng,
                                    iqomahSubuh,
                                    iqomahDzuhur,
                                    iqomahAshar,
                                    iqomahMaghrib,
                                    iqomahIsya,
                                    calculationMethodName,
                                    adzanLeadMinutes
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "Simpan Pengaturan",
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IqomahAdjustmentRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { if (value > 0) onValueChange(value - 1) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "$value menit",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(76.dp)
            )

            Button(
                onClick = { onValueChange(value + 1) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFFFFD54F),
    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
    focusedLabelColor = Color(0xFFFFD54F),
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    cursorColor = Color(0xFFFFD54F)
)

enum class SettingsCategory(
    val displayName: String,
    val description: String
) {
    LOCATION(
        displayName = "Koordinat Masjid",
        description = "Sesuaikan koordinat lintang (latitude) dan bujur (longitude) masjid untuk menghitung jadwal sholat dengan akurat."
    ),
    IQOMAH(
        displayName = "Durasi & Jeda Waktu",
        description = "Atur jeda waktu tunggu (menit) sebelum iqomah untuk setiap waktu sholat berjamaah, serta durasi tampilan persiapan adzan sebelum waktu sholat tiba."
    ),
    METHOD(
        displayName = "Metode Perhitungan",
        description = "Pilih standar konvensi perhitungan waktu sholat yang ingin diterapkan di wilayah masjid Anda."
    ),
    ANNOUNCEMENTS(
        displayName = "Kelola Pengumuman",
        description = "Tambahkan, kelola, atau hapus teks berjalan yang akan ditampilkan di bagian bawah layar utama."
    ),
    THEME(
        displayName = "Latar Belakang",
        description = "Pilih gambar latar belakang layar utama dari penyimpanan perangkat Anda."
    ),
    RESET(
        displayName = "Reset Pengaturan",
        description = "Mengembalikan seluruh nilai koordinat, durasi iqomah, metode, serta running text ke konfigurasi bawaan (default)."
    )
}
