package com.groovernapp.mosqueathan.ui.settings

import com.groovernapp.mosqueathan.ui.settings.prayerschedule.*
import com.groovernapp.mosqueathan.ui.settings.adzaniqomah.*
import com.groovernapp.mosqueathan.ui.settings.displaymedia.*
import com.groovernapp.mosqueathan.ui.settings.remotedevices.*
import com.groovernapp.mosqueathan.ui.settings.systemmaintenance.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.groovernapp.mosqueathan.data.ConnectedDeviceEntity
import com.groovernapp.mosqueathan.server.SettingsServer
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Restore



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
        adzanLeadMinutes: Int,
        offsetImsak: Int,
        offsetSubuh: Int,
        offsetSyuruq: Int,
        offsetDzuhur: Int,
        offsetAshar: Int,
        offsetMaghrib: Int,
        offsetIsya: Int,
        kemenagCityId: String,
        kemenagCityName: String,
        kemenagDuration: String,
        kemenagTargetDate: String
    ) -> Unit,
    onAddAnnouncement: (String) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onEditAnnouncement: (String, String) -> Unit,
    onReorderAnnouncements: (List<String>) -> Unit,
    onResetToDefaults: () -> Unit,
    onSelectCustomBackground: (android.net.Uri) -> Unit,
    onClearCustomBackground: () -> Unit,
    mediaList: List<MediaEntity>,
    onAddMediaItems: (List<android.net.Uri>) -> Unit,
    onDeleteMediaItem: (MediaEntity) -> Unit,
    onUpdateThemeSettings: (String, Int, Boolean, Boolean) -> Unit,
    onUpdateShowSettingsOnTv: (Boolean) -> Unit,
    devices: List<ConnectedDeviceEntity>,
    citySearchResults: List<Pair<String, String>> = emptyList(),
    isSearchingCities: Boolean = false,
    kemenagSchedules: List<com.groovernapp.mosqueathan.data.KemenagScheduleEntity> = emptyList(),
    onSearchKemenagCities: (String) -> Unit = {},
    onSetDeviceMaster: (String) -> Unit,
    onUpdatePinCode: (String) -> Unit,
    onDeleteDevice: (String) -> Unit,
    onUpdateFooters: (String, String) -> Unit = { _, _ -> },
    onPreviewAdzan: () -> Unit = {},
    onPreviewIqomah: () -> Unit = {},
    onPreviewSlideshow: () -> Unit = {},
    onDownloadKemenagSchedules: (cityId: String, cityName: String, duration: String, targetDate: String) -> Unit = { _, _, _, _ -> },
    slideshowSchedules: List<com.groovernapp.mosqueathan.data.SlideshowScheduleEntity> = emptyList(),
    onAddSlideshowSchedule: (String, Boolean, String, String) -> Unit = { _, _, _, _ -> },
    onDeleteSlideshowSchedule: (String) -> Unit = {},
    standbySchedules: List<com.groovernapp.mosqueathan.data.StandbyScheduleEntity> = emptyList(),
    onAddStandbySchedule: (String, Boolean, String, String) -> Unit = { _, _, _, _ -> },
    onDeleteStandbySchedule: (String) -> Unit = {},
    onSaveStandby: (Boolean, String, String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit
) {
    val mediaPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<android.net.Uri> ->
        if (uris.isNotEmpty()) {
            onAddMediaItems(uris)
        }
    }

    val showAllSettings = true // Set to true so settings can be accessed directly on TV devices

    var selectedCategory by remember(showAllSettings) { 
        mutableStateOf(if (showAllSettings) SettingsCategory.PRAYER_SCHEDULE else SettingsCategory.REMOTE_DEVICES) 
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val isTv = remember(context) {
        val uiModeManager = context.getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager
        uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    }

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

    // Offset States
    var offsetImsak by remember(config.offsetImsak) { mutableStateOf(config.offsetImsak) }
    var offsetSubuh by remember(config.offsetSubuh) { mutableStateOf(config.offsetSubuh) }
    var offsetSyuruq by remember(config.offsetSyuruq) { mutableStateOf(config.offsetSyuruq) }
    var offsetDzuhur by remember(config.offsetDzuhur) { mutableStateOf(config.offsetDzuhur) }
    var offsetAshar by remember(config.offsetAshar) { mutableStateOf(config.offsetAshar) }
    var offsetMaghrib by remember(config.offsetMaghrib) { mutableStateOf(config.offsetMaghrib) }
    var offsetIsya by remember(config.offsetIsya) { mutableStateOf(config.offsetIsya) }

    // Calculation Method State
    var calculationMethodName by remember(config.calculationMethodName) { mutableStateOf(config.calculationMethodName) }
    var kemenagCityId by remember(config.kemenagCityId) { mutableStateOf(config.kemenagCityId) }
    var kemenagCityName by remember(config.kemenagCityName) { mutableStateOf(config.kemenagCityName) }
    var kemenagDuration by remember(config.kemenagDuration) { mutableStateOf(config.kemenagDuration) }
    var kemenagTargetDate by remember(config.kemenagTargetDate) { mutableStateOf(config.kemenagTargetDate) }

    // Standby States
    var standbyModeEnabled by remember(config.standbyModeEnabled) { mutableStateOf(config.standbyModeEnabled) }
    var standbyStartTime by remember(config.standbyStartTime) { mutableStateOf(config.standbyStartTime) }
    var standbyEndTime by remember(config.standbyEndTime) { mutableStateOf(config.standbyEndTime) }

    val customBgBitmap = remember(config.customBackgroundPath) {
        if (config.customBackgroundPath.isNotEmpty() && java.io.File(config.customBackgroundPath).exists()) {
            try {
                android.graphics.BitmapFactory.decodeFile(config.customBackgroundPath)?.asImageBitmap()
            } catch (e: Throwable) {
                null
            }
        } else {
            null
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val containerSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val screenWidthDp = with(density) { containerSize.width.toDp() }
    val isCompact = screenWidthDp < 600.dp && !isTv
    var showCategoryDetail by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = isCompact && showCategoryDetail) {
        showCategoryDetail = false
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

        val paddingHorizontal = if (isCompact) 16.dp else 48.dp
        val paddingVertical = if (isCompact) 16.dp else 32.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
        ) {
            // Sidebar Navigation
            if (!isCompact || !showCategoryDetail) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .then(if (isCompact) Modifier.fillMaxWidth() else Modifier.fillMaxWidth(0.3f).padding(end = 32.dp)),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = com.groovernapp.mosqueathan.util.Lang.get("SETTINGS", "PENGATURAN"),
                        color = Color(0xFFFFD54F),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp, start = 12.dp)
                    )

                    val displayedCategories = if (showAllSettings) {
                        SettingsCategory.values()
                    } else {
                        arrayOf(SettingsCategory.REMOTE_DEVICES)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        displayedCategories.forEach { category ->
                            val isSelected = selectedCategory == category
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()
                            val isHovered by interactionSource.collectIsHoveredAsState()
                            val isFocusedOrHovered = isFocused || isHovered

                            val borderColor = when {
                                isSelected -> Color(0xFFFFD54F)
                                isFocusedOrHovered -> Color(0xFFFFD54F).copy(alpha = 0.8f)
                                else -> Color.White.copy(alpha = 0.15f)
                            }
                            val borderWidth = if (isSelected || isFocusedOrHovered) 2.dp else 1.dp
                            val backgroundColor = when {
                                isSelected && isFocusedOrHovered -> Color(0xFFFFD54F).copy(alpha = 0.25f)
                                isSelected -> Color(0xFFFFD54F).copy(alpha = 0.15f)
                                isFocusedOrHovered -> Color.White.copy(alpha = 0.12f)
                                else -> Color.Transparent
                            }
                            val textColor = if (isSelected || isFocusedOrHovered) Color(0xFFFFD54F) else Color.White
                            val textWeight = if (isSelected || isFocusedOrHovered) FontWeight.Bold else FontWeight.Medium

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = borderWidth,
                                        color = borderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(
                                        backgroundColor,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = androidx.compose.foundation.LocalIndication.current
                                    ) {
                                        selectedCategory = category
                                        if (isCompact) showCategoryDetail = true
                                    }
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = category.getDisplayName(),
                                    color = textColor,
                                    fontSize = 18.sp,
                                    fontWeight = textWeight
                                )
                            }
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    val ipAddress = remember { SettingsServer.getIpAddress() }
                    val versionName = remember(context) {
                        try {
                            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                            packageInfo.versionName ?: "Unknown"
                        } catch (e: Exception) {
                            "Unknown"
                        }
                    }

                    Column(
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Text(
                            text = com.groovernapp.mosqueathan.util.Lang.get("App Version: v$versionName", "Versi Aplikasi: v$versionName"),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TvFocusableButton(
                        onClick = {
                            if (isCompact && showCategoryDetail) {
                                showCategoryDetail = false
                            } else {
                                onBack()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            com.groovernapp.mosqueathan.util.Lang.get("Back", "Kembali"),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right Pane Settings Editor
            if (!isCompact || showCategoryDetail) {
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .then(if (isCompact) Modifier.fillMaxWidth() else Modifier.fillMaxWidth(0.7f).align(Alignment.CenterEnd)),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isCompact) 16.dp else 32.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (isCompact) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { showCategoryDetail = false }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = selectedCategory.getDisplayName(),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = selectedCategory.getDisplayName(),
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = selectedCategory.getDescription(),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }

                        // Category specific UI — each segment is in its own file
                        when (selectedCategory) {
                            SettingsCategory.PRAYER_SCHEDULE -> {
                                PrayerScheduleSettingsSegment(
                                    latText = latText,
                                    onLatTextChange = { latText = it },
                                    lngText = lngText,
                                    onLngTextChange = { lngText = it },
                                    calculationMethodName = calculationMethodName,
                                    kemenagCityId = kemenagCityId,
                                    kemenagCityName = kemenagCityName,
                                    kemenagDuration = kemenagDuration,
                                    kemenagTargetDate = kemenagTargetDate,
                                    citySearchResults = citySearchResults,
                                    isSearchingCities = isSearchingCities,
                                    kemenagSchedules = kemenagSchedules,
                                    onMethodChange = { calculationMethodName = it },
                                    onKemenagCitySelected = { id, name ->
                                        kemenagCityId = id
                                        kemenagCityName = name
                                    },
                                    onKemenagDurationChange = { kemenagDuration = it },
                                    onKemenagTargetDateChange = { kemenagTargetDate = it },
                                    onSearchKemenagCities = onSearchKemenagCities,
                                    onDownloadKemenagSchedules = {
                                        onDownloadKemenagSchedules(kemenagCityId, kemenagCityName, kemenagDuration, kemenagTargetDate)
                                    },
                                    offsetImsak = offsetImsak,
                                    offsetSubuh = offsetSubuh,
                                    offsetSyuruq = offsetSyuruq,
                                    offsetDzuhur = offsetDzuhur,
                                    offsetAshar = offsetAshar,
                                    offsetMaghrib = offsetMaghrib,
                                    offsetIsya = offsetIsya,
                                    onOffsetImsakChange = { offsetImsak = it },
                                    onOffsetSubuhChange = { offsetSubuh = it },
                                    onOffsetSyuruqChange = { offsetSyuruq = it },
                                    onOffsetDzuhurChange = { offsetDzuhur = it },
                                    onOffsetAsharChange = { offsetAshar = it },
                                    onOffsetMaghribChange = { offsetMaghrib = it },
                                    onOffsetIsyaChange = { offsetIsya = it }
                                )
                            }

                            SettingsCategory.ADZAN_IQOMAH -> {
                                SettingsCard(
                                    title = com.groovernapp.mosqueathan.util.Lang.get("Adzan & Iqomah", "Adzan & Iqomah"),
                                    icon = Icons.Default.AccessTime
                                ) {
                                    IqomahSettingsSegment(
                                        adzanLeadMinutes = adzanLeadMinutes,
                                        onAdzanLeadMinutesChange = { adzanLeadMinutes = it },
                                        iqomahSubuh = iqomahSubuh,
                                        onIqomahSubuhChange = { iqomahSubuh = it },
                                        iqomahDzuhur = iqomahDzuhur,
                                        onIqomahDzuhurChange = { iqomahDzuhur = it },
                                        iqomahAshar = iqomahAshar,
                                        onIqomahAsharChange = { iqomahAshar = it },
                                        iqomahMaghrib = iqomahMaghrib,
                                        onIqomahMaghribChange = { iqomahMaghrib = it },
                                        iqomahIsya = iqomahIsya,
                                        onIqomahIsyaChange = { iqomahIsya = it }
                                    )
                                }
                            }

                            SettingsCategory.DISPLAY_MEDIA -> {
                                val backgroundPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                    contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                                ) { uri: android.net.Uri? ->
                                    if (uri != null) {
                                        onSelectCustomBackground(uri)
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                    SettingsCard(
                                        title = com.groovernapp.mosqueathan.util.Lang.get("Background Theme", "Latar Belakang & Media"),
                                        icon = Icons.Default.Image
                                    ) {
                                        ThemeSettingsSegment(
                                            config = config,
                                            transitionType = config.transitionType,
                                            slideIntervalSeconds = config.slideIntervalSeconds,
                                            videoWaitUntilEnd = config.videoWaitUntilEnd,
                                            includeDefaultImage = config.includeDefaultImage,
                                            mediaList = mediaList,
                                            slideshowSchedules = slideshowSchedules,
                                            onUpdateThemeSettings = onUpdateThemeSettings,
                                            onAddMediaItem = {
                                                mediaPickerLauncher.launch(
                                                    androidx.activity.result.PickVisualMediaRequest(
                                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                                    )
                                                )
                                            },
                                            onDeleteMediaItem = onDeleteMediaItem,
                                            onSelectCustomBackground = {
                                                backgroundPickerLauncher.launch(
                                                    androidx.activity.result.PickVisualMediaRequest(
                                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            },
                                            onClearCustomBackground = onClearCustomBackground,
                                            onAddSlideshowSchedule = onAddSlideshowSchedule,
                                            onDeleteSlideshowSchedule = onDeleteSlideshowSchedule,
                                            onPreviewSlideshow = onPreviewSlideshow
                                        )
                                    }
                                    SettingsCard(
                                        title = com.groovernapp.mosqueathan.util.Lang.get("Announcements", "Teks Pengumuman"),
                                        icon = Icons.Default.Campaign
                                    ) {
                                        AnnouncementsSettingsSegment(
                                            config = config,
                                            announcements = announcements,
                                            onAddAnnouncement = onAddAnnouncement,
                                            onDeleteAnnouncement = onDeleteAnnouncement,
                                            onEditAnnouncement = onEditAnnouncement,
                                            onUpdateFooters = onUpdateFooters,
                                            onPreviewAdzan = onPreviewAdzan,
                                            onPreviewIqomah = onPreviewIqomah
                                        )
                                    }
                                }
                            }

                            SettingsCategory.REMOTE_DEVICES -> {
                                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                    SettingsCard(
                                        title = com.groovernapp.mosqueathan.util.Lang.get("Web Control Server", "Server Web Remote"),
                                        icon = Icons.Default.Dns
                                    ) {
                                        WebControlSettingsSegment(
                                            showSettingsOnTv = config.showSettingsOnTv,
                                            isTv = isTv,
                                            onUpdateShowSettingsOnTv = onUpdateShowSettingsOnTv
                                        )
                                    }
                                    SettingsCard(
                                        title = com.groovernapp.mosqueathan.util.Lang.get("Connected Devices", "Perangkat Terhubung"),
                                        icon = Icons.Default.Devices
                                    ) {
                                        DevicesSettingsSegment(
                                            config = config,
                                            devices = devices,
                                            onSetDeviceMaster = onSetDeviceMaster,
                                            onDeleteDevice = onDeleteDevice
                                        )
                                    }
                                }
                            }

                            SettingsCategory.SYSTEM_MAINTENANCE -> {
                                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                    SettingsCard(
                                        title = com.groovernapp.mosqueathan.util.Lang.get("Standby & Display", "Standby & Tampilan"),
                                        icon = Icons.Default.Tv
                                    ) {
                                        StandbySettingsSegment(
                                            standbyModeEnabled = standbyModeEnabled,
                                            onStandbyModeEnabledChange = { standbyModeEnabled = it },
                                            standbySchedules = standbySchedules,
                                            onAddStandbySchedule = onAddStandbySchedule,
                                            onDeleteStandbySchedule = onDeleteStandbySchedule
                                        )
                                    }
                                    SettingsCard(
                                        title = com.groovernapp.mosqueathan.util.Lang.get("Reset Configurations", "Reset Konfigurasi"),
                                        icon = Icons.Default.Restore
                                    ) {
                                        ResetSettingsSegment(onResetToDefaults = onResetToDefaults)
                                    }
                                }
                            }
                        }
                    }

                    // Save / Apply Button (only for config-saving categories)
                    if (selectedCategory in listOf(
                            SettingsCategory.PRAYER_SCHEDULE,
                            SettingsCategory.ADZAN_IQOMAH,
                            SettingsCategory.SYSTEM_MAINTENANCE
                        )
                    ) {
                        var isSaving by remember { mutableStateOf(false) }
                        val coroutineScope = rememberCoroutineScope()

                        TvFocusablePrimaryButton(
                            onClick = {
                                if (isSaving) return@TvFocusablePrimaryButton
                                isSaving = true
                                coroutineScope.launch {
                                    if (selectedCategory == SettingsCategory.SYSTEM_MAINTENANCE) {
                                        onSaveStandby(standbyModeEnabled, standbyStartTime, standbyEndTime)
                                    } else {
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
                                            adzanLeadMinutes,
                                            offsetImsak,
                                            offsetSubuh,
                                            offsetSyuruq,
                                            offsetDzuhur,
                                            offsetAshar,
                                            offsetMaghrib,
                                            offsetIsya,
                                            kemenagCityId,
                                            kemenagCityName,
                                            kemenagDuration,
                                            kemenagTargetDate
                                        )
                                    }
                                    delay(600)
                                    isSaving = false
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = com.groovernapp.mosqueathan.util.Lang.get("Save Settings", "Simpan Pengaturan"),
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            } // end if (!isCompact || showCategoryDetail)
        }
    }
}
}



enum class SettingsCategory(
    val getDisplayName: @Composable () -> String,
    val getDescription: @Composable () -> String
) {
    PRAYER_SCHEDULE(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("Prayer Schedule", "Jadwal Shalat") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Manage coordinates, calculation methods, and manual time corrections.", "Atur koordinat GPS, metode perhitungan, dan penyesuaian waktu (offset) tiap shalat.") }
    ),
    ADZAN_IQOMAH(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("Adzan & Iqomah", "Adzan & Iqomah") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Set the adzan preparation duration and the iqomah wait time for each prayer.", "Atur durasi persiapan adzan dan jeda waktu iqomah untuk masing-masing waktu shalat.") }
    ),
    DISPLAY_MEDIA(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("Display & Media", "Tampilan & Media") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Configure background wallpapers, slideshows, and scrolling announcements.", "Atur gambar latar belakang, interval slideshow, dan pengumuman teks berjalan.") }
    ),
    REMOTE_DEVICES(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("Connectivity & Remote", "Konektivitas & Remote") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Manage web control server access, connected devices, and PIN security.", "Kelola akses server kontrol web, perangkat yang terhubung, PIN, dan otoritas master.") }
    ),
    SYSTEM_MAINTENANCE(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("System & Reset", "Sistem & Reset") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Configure TV standby hours and restore application settings to defaults.", "Atur jadwal layar hitam (standby) otomatis dan kembalikan pengaturan ke setelan pabrik.") }
    )
}
