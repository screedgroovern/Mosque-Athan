package com.groovernapp.mosqueathan.ui.settings.fridaysettings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.groovernapp.mosqueathan.data.FridayMediaEntity
import com.groovernapp.mosqueathan.data.MediaEntity
import com.groovernapp.mosqueathan.ui.settings.AdjustmentRow
import com.groovernapp.mosqueathan.ui.settings.SettingsCard
import com.groovernapp.mosqueathan.ui.settings.TvFocusablePrimaryButton
import com.groovernapp.mosqueathan.ui.settings.textFieldColors
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.launch

@Composable
fun FridaySettingsPane(
    jumatEnabled: Boolean,
    onJumatEnabledChange: (Boolean) -> Unit,
    jumatStartTime: String,
    onJumatStartTimeChange: (String) -> Unit,
    jumatEndTime: String,
    onJumatEndTimeChange: (String) -> Unit,
    offsetJumat: Int,
    onOffsetJumatChange: (Int) -> Unit,
    jumatStandbyOnly: Boolean,
    onJumatStandbyOnlyChange: (Boolean) -> Unit,
    jumatWarningText: String,
    onJumatWarningTextChange: (String) -> Unit,
    murottalEnabled: Boolean,
    onMurottalEnabledChange: (Boolean) -> Unit,
    murottalStartDelay: Int,
    onMurottalStartDelayChange: (Int) -> Unit,
    murottalAudioPaths: String,
    onMurottalAudioPathsChange: (String) -> Unit,
    jumatSlideshowEnabled: Boolean,
    onJumatSlideshowEnabledChange: (Boolean) -> Unit,
    includeGlobalSlideshow: Boolean,
    onIncludeGlobalSlideshowChange: (Boolean) -> Unit,
    selectedGlobalSlideIds: String,
    onSelectedGlobalSlideIdsChange: (String) -> Unit,
    jumatTransitionType: String,
    onJumatTransitionTypeChange: (String) -> Unit,
    jumatSlideIntervalSeconds: Int,
    onJumatSlideIntervalSecondsChange: (Int) -> Unit,
    jumatSlideIntervalUnit: String,
    onJumatSlideIntervalUnitChange: (String) -> Unit,
    jumatVideoWaitUntilEnd: Boolean,
    onJumatVideoWaitUntilEndChange: (Boolean) -> Unit,
    fridayMediaList: List<FridayMediaEntity> = emptyList(),
    mediaList: List<MediaEntity> = emptyList(),
    onAddFridayMediaItems: (List<android.net.Uri>) -> Unit = {},
    onUpdateFridayMediaItemPath: (String, android.net.Uri) -> Unit = { _, _ -> },
    onDeleteFridayMediaItem: (FridayMediaEntity) -> Unit = {},
    onReorderFridayMediaItems: (List<String>) -> Unit = {},
    onPreviewFridaySlideshow: () -> Unit = {},
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {

    val mediaPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<android.net.Uri> ->
        if (uris.isNotEmpty()) {
            onAddFridayMediaItems(uris)
        }
    }

    var mediaIdToUpdate by remember { mutableStateOf<String?>(null) }
    val updateMediaLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null && mediaIdToUpdate != null) {
            onUpdateFridayMediaItemPath(mediaIdToUpdate!!, uri)
            mediaIdToUpdate = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Card 1: Friday Core Settings
        SettingsCard(
            title = "", // No title
            icon = null
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Lang.get("Enable Friday Mode", "Aktifkan Mode Jum'at"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = Lang.get("Enable Friday features and layout adjustments", "Aktifkan fitur khusus dan penyesuaian tata letak untuk hari Jum'at"),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = jumatEnabled,
                        onCheckedChange = onJumatEnabledChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD54F), checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.5f))
                    )
                }

                if (jumatEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = jumatStartTime,
                            onValueChange = onJumatStartTimeChange,
                            label = { Text(Lang.get("Start Time (HH:mm)", "Waktu Mulai (HH:mm)")) },
                            colors = textFieldColors(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = jumatEndTime,
                            onValueChange = onJumatEndTimeChange,
                            label = { Text(Lang.get("End Time (HH:mm)", "Waktu Selesai (HH:mm)")) },
                            colors = textFieldColors(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AdjustmentRow(
                        title = Lang.get("Friday Adzan Offset", "Koreksi Waktu Adzan Jum'at"),
                        value = offsetJumat,
                        minValue = -60,
                        maxValue = 60,
                        showSign = true,
                        onValueChange = onOffsetJumatChange
                    )
                }
            }
        }

        if (jumatEnabled) {
            // Card 2: Silence & Warning Text
            SettingsCard(
                title = Lang.get("Friday Silence Mode", "Mode Ibadah & Khutbah"),
                icon = Icons.Default.Settings,
                dimmed = !jumatEnabled
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Lang.get("Standby Mode Only", "Mode Standby Saja"),
                                color = if (jumatEnabled) Color.White else Color.White.copy(alpha = 0.35f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = Lang.get("Turn screen completely black after Friday Adzan until Friday service ends", "Layar TV akan padam total setelah adzan Jum'at sampai ibadah Jum'at selesai"),
                                color = if (jumatEnabled) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f),
                                fontSize = 13.sp
                            )
                        }
                        Switch(
                            checked = jumatStandbyOnly,
                            onCheckedChange = onJumatStandbyOnlyChange,
                            enabled = jumatEnabled
                        ,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD54F), checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.5f))
                        )
                    }

                    if (jumatStandbyOnly && jumatEnabled) {
                        Text(
                            text = Lang.get(
                                "Note: Other Friday visual features are disabled because Standby Mode is active.",
                                "Catatan: Menu visual Jum'at lainnya dinonaktifkan karena Mode Standby Saja aktif."
                            ),
                            color = Color(0xFFFFD54F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // Card 3: Murottal Pre-Adzan Settings
            SettingsCard(
                title = Lang.get("Pre-Adzan Murottal Quran", "Murottal Sebelum Adzan"),
                icon = Icons.Default.Settings,
                dimmed = !jumatEnabled || jumatStandbyOnly
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Lang.get("Auto Play Murottal", "Putar Murottal Otomatis"),
                                color = if (jumatEnabled && !jumatStandbyOnly) Color.White else Color.White.copy(alpha = 0.35f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = Lang.get("Automatically play Quran audio files before adzan", "Putar berkas audio Al-Qur'an secara otomatis menjelang adzan"),
                                color = if (jumatEnabled && !jumatStandbyOnly) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f),
                                fontSize = 13.sp
                            )
                        }
                        Switch(
                            checked = murottalEnabled,
                            onCheckedChange = onMurottalEnabledChange,
                            enabled = jumatEnabled && !jumatStandbyOnly
                        ,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD54F), checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.5f))
                        )
                    }

                    if (murottalEnabled) {
                        AdjustmentRow(
                            title = Lang.get("Start Delay (Minutes)", "Jeda Mulai (Menit setelah Waktu Mulai)"),
                            value = murottalStartDelay,
                            minValue = 0,
                            maxValue = 120,
                            showSign = false,
                            onValueChange = { if (jumatEnabled && !jumatStandbyOnly) onMurottalStartDelayChange(it) }
                        )

                        MurottalAudioPathsManager(
                            audioPaths = murottalAudioPaths,
                            onAudioPathsChange = onMurottalAudioPathsChange,
                            enabled = jumatEnabled && !jumatStandbyOnly
                        )
                    }
                }
            }

            // Card 4: Friday Slideshow
            SettingsCard(
                title = Lang.get("Friday Specific Slideshow", "Slideshow Khusus Jum'at"),
                icon = Icons.Default.Settings,
                dimmed = !jumatEnabled || jumatStandbyOnly
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Lang.get("Enable Friday Slideshow", "Aktifkan Slideshow Jum'at"),
                                color = if (jumatEnabled && !jumatStandbyOnly) Color.White else Color.White.copy(alpha = 0.35f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = Lang.get("Display different backgrounds/slides on Friday", "Tampilkan gambar/slideshow khusus di hari Jum'at"),
                                color = if (jumatEnabled && !jumatStandbyOnly) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f),
                                fontSize = 13.sp
                            )
                        }
                        Switch(
                            checked = jumatSlideshowEnabled,
                            onCheckedChange = onJumatSlideshowEnabledChange,
                            enabled = jumatEnabled && !jumatStandbyOnly
                        ,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD54F), checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.5f))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Lang.get("Include Global Slideshow", "Sertakan Slideshow Global"),
                                color = if (jumatEnabled && !jumatStandbyOnly && jumatSlideshowEnabled) Color.White else Color.White.copy(alpha = 0.35f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = Lang.get("Mix global slideshow images in rotation", "Sertakan gambar dari slideshow reguler dalam putaran"),
                                color = if (jumatEnabled && !jumatStandbyOnly && jumatSlideshowEnabled) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f),
                                fontSize = 13.sp
                            )
                        }
                        Switch(
                            checked = includeGlobalSlideshow,
                            onCheckedChange = onIncludeGlobalSlideshowChange,
                            enabled = jumatEnabled && !jumatStandbyOnly && jumatSlideshowEnabled
                        ,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD54F), checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.5f))
                        )
                    }

                    if (jumatSlideshowEnabled) {
                        if (includeGlobalSlideshow) {
                            val pagerState = rememberPagerState(initialPage = 0) { 2 }
                            val coroutineScope = rememberCoroutineScope()
                            
                            SecondaryTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFFFFD54F),
                                divider = {}
                            ) {
                                Tab(
                                    selected = pagerState.currentPage == 0,
                                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                                    text = {
                                        Text(
                                            text = Lang.get("Slideshow", "Slideshow"),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    selectedContentColor = Color(0xFFFFD54F),
                                    unselectedContentColor = Color.White.copy(alpha = 0.6f)
                                )
                                Tab(
                                    selected = pagerState.currentPage == 1,
                                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                                    text = {
                                        Text(
                                            text = Lang.get("Global Slideshow", "Slideshow Global"),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    selectedContentColor = Color(0xFFFFD54F),
                                    unselectedContentColor = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                if (page == 0) {
                                    FridayMediaListSection(
                                        fridayMediaList = fridayMediaList,
                                        onAddFridayMediaItems = {
                                        mediaPickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                        onUpdateFridayMediaItemPath = { id ->
                                        mediaIdToUpdate = id
                                        updateMediaLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                        onDeleteFridayMediaItem = onDeleteFridayMediaItem,
                                        onReorderFridayMediaItems = onReorderFridayMediaItems
                                    )
                                } else {
                                    GlobalSlideshowSelectionSection(
                                        mediaList = mediaList,
                                        selectedGlobalSlideIds = selectedGlobalSlideIds,
                                        onSelectedGlobalSlideIdsChange = onSelectedGlobalSlideIdsChange
                                    )
                                }
                            }
                        } else {
                            FridayMediaListSection(
                                fridayMediaList = fridayMediaList,
                                onAddFridayMediaItems = {
                                        mediaPickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                onUpdateFridayMediaItemPath = { id ->
                                        mediaIdToUpdate = id
                                        updateMediaLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                onDeleteFridayMediaItem = onDeleteFridayMediaItem,
                                onReorderFridayMediaItems = onReorderFridayMediaItems
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Transition Settings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(Lang.get("Transition Type", "Tipe Transisi"), color = Color.White, fontWeight = FontWeight.Bold)
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text(jumatTransitionType)
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(Color(0xFF2A2A2A))
                                ) {
                                    listOf("FADE", "SLIDE", "ZOOM", "NONE").forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type, color = Color.White) },
                                            onClick = {
                                                onJumatTransitionTypeChange(type)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                AdjustmentRow(
                                    title = Lang.get("Slide Duration", "Durasi Slide"),
                                    value = jumatSlideIntervalSeconds,
                                    minValue = 1,
                                    maxValue = 300,
                                    showSign = false,
                                    unitSuffix = "",
                                    onValueChange = onJumatSlideIntervalSecondsChange
                                )
                            }
                            var unitExpanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { unitExpanded = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text(if (jumatSlideIntervalUnit == "MINUTES") Lang.get("Minutes", "Menit") else Lang.get("Seconds", "Detik"))
                                }
                                DropdownMenu(
                                    expanded = unitExpanded,
                                    onDismissRequest = { unitExpanded = false },
                                    modifier = Modifier.background(Color(0xFF2A2A2A))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(Lang.get("Seconds", "Detik"), color = Color.White) },
                                        onClick = {
                                            onJumatSlideIntervalUnitChange("SECONDS")
                                            unitExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(Lang.get("Minutes", "Menit"), color = Color.White) },
                                        onClick = {
                                            onJumatSlideIntervalUnitChange("MINUTES")
                                            unitExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Lang.get("Wait Video End", "Tunggu Video Selesai"),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = Lang.get("Wait for video to finish before next slide", "Tunggu video selesai sebelum lanjut slide berikutnya"),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            }
                            Switch(
                                checked = jumatVideoWaitUntilEnd,
                                onCheckedChange = onJumatVideoWaitUntilEndChange,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD54F), checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.5f))
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        TvFocusablePrimaryButton(
                            onClick = onPreviewFridaySlideshow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = Lang.get("Preview Slideshow", "Pratinjau Slideshow"),
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FridayMediaListSection(
    fridayMediaList: List<FridayMediaEntity>,
    onAddFridayMediaItems: () -> Unit,
    onUpdateFridayMediaItemPath: (String) -> Unit,
    onDeleteFridayMediaItem: (FridayMediaEntity) -> Unit,
    onReorderFridayMediaItems: (List<String>) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        com.groovernapp.mosqueathan.ui.component.SlideshowComponent(
            items = fridayMediaList,
            keySelector = { it.id },
            filePathSelector = { it.filePath },
            isVideoSelector = { it.isVideo },
            isUsedSelector = null, // No checkbox for Friday specific media
            showAddButton = true,
            addText = Lang.get("Add Image / Video", "Tambah Gambar / Video"),
            onAddClicked = onAddFridayMediaItems,
            onReorderItems = onReorderFridayMediaItems,
            onEditItem = { onUpdateFridayMediaItemPath(it.id) },
            onDeleteItem = { onDeleteFridayMediaItem(it) },
            emptyText = Lang.get("No media added. Slideshow is empty.", "Belum ada media. Slideshow kosong."),
            showTransitionAnimation = false,
            showTransitionInterval = false
        )
    }
}

@Composable
fun GlobalSlideshowSelectionSection(
    mediaList: List<MediaEntity>,
    selectedGlobalSlideIds: String,
    onSelectedGlobalSlideIdsChange: (String) -> Unit
) {
    val selectedIdsSet = remember(selectedGlobalSlideIds) {
        selectedGlobalSlideIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        com.groovernapp.mosqueathan.ui.component.SlideshowComponent(
            items = mediaList,
            keySelector = { it.id },
            filePathSelector = { it.filePath },
            isVideoSelector = { it.isVideo },
            isUsedSelector = { selectedIdsSet.contains(it.id) },
            onUpdateUseStatus = { item, isUsed ->
                val newSet = if (isUsed) {
                    selectedIdsSet + item.id
                } else {
                    selectedIdsSet - item.id
                }
                onSelectedGlobalSlideIdsChange(newSet.joinToString(","))
            },
            showAddButton = false, // Not adding/editing from this view, just selecting
            onReorderItems = { /* No reorder from here */ },
            onEditItem = { /* No edit from here */ },
            onDeleteItem = { /* No delete from here */ },
            emptyText = Lang.get("No global media found.", "Slideshow global kosong."),
            showTransitionAnimation = false,
            showTransitionInterval = false
        )
    }
}

@Composable
fun MurottalAudioPathsManager(
    audioPaths: String,
    onAudioPathsChange: (String) -> Unit,
    enabled: Boolean
) {
    val paths = remember(audioPaths) {
        audioPaths.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    
    val audioPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<android.net.Uri> ->
        if (uris.isNotEmpty()) {
            val newPaths = (paths + uris.map { it.toString() }).distinct()
            onAudioPathsChange(newPaths.joinToString(","))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(Lang.get("Murottal Audio Files", "File Audio Murottal"), color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        
        if (paths.isNotEmpty()) {
            paths.take(5).forEach { path ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = path.substringAfterLast("/").takeIf { it.isNotEmpty() } ?: path,
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val newPaths = paths.filter { it != path }
                            onAudioPathsChange(newPaths.joinToString(","))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.8f))
                    }
                }
            }
        } else {
            Text(Lang.get("No audio files selected", "Belum ada file audio"), color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White)
            ) {
                Text(Lang.get("Add Audio", "Tambah Audio"))
            }
            if (paths.size > 5) {
                Button(
                    onClick = { /* Open full list dialog or expand */ },
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White)
                ) {
                    Text(Lang.get("List Audio (${paths.size})", "Daftar Audio (${paths.size})"))
                }
            }
        }
    }
}
