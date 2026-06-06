package com.groovernapp.mosqueathan.ui.settings

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
    onAddSlideshowSchedule: (String, String) -> Unit = { _, _ -> },
    onDeleteSlideshowSchedule: (String) -> Unit = {},
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
        mutableStateOf(if (showAllSettings) SettingsCategory.LOCATION else SettingsCategory.WEB_CONTROL) 
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
                        arrayOf(SettingsCategory.WEB_CONTROL)
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
                        onClick = onBack,
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

                        // Category specific UI — each segment is in its own file
                        when (selectedCategory) {
                            SettingsCategory.LOCATION -> {
                                LocationSettingsSegment(
                                    latText = latText,
                                    onLatTextChange = { latText = it },
                                    lngText = lngText,
                                    onLngTextChange = { lngText = it },
                                    calculationMethodName = calculationMethodName,
                                    kemenagCityName = kemenagCityName
                                )
                            }

                            SettingsCategory.IQOMAH -> {
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

                            SettingsCategory.PRAYER_OFFSETS -> {
                                PrayerOffsetsSettingsSegment(
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

                            SettingsCategory.METHOD -> {
                                MethodSettingsSegment(
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
                                    }
                                )
                            }

                            SettingsCategory.ANNOUNCEMENTS -> {
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

                            SettingsCategory.THEME -> {
                                val backgroundPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                    contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                                ) { uri: android.net.Uri? ->
                                    if (uri != null) {
                                        onSelectCustomBackground(uri)
                                    }
                                }
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

                            SettingsCategory.WEB_CONTROL -> {
                                WebControlSettingsSegment(
                                    showSettingsOnTv = config.showSettingsOnTv,
                                    isTv = isTv,
                                    onUpdateShowSettingsOnTv = onUpdateShowSettingsOnTv
                                )
                            }

                            SettingsCategory.DEVICES -> {
                                DevicesSettingsSegment(
                                    config = config,
                                    devices = devices,
                                    onSetDeviceMaster = onSetDeviceMaster,
                                    onDeleteDevice = onDeleteDevice
                                )
                            }

                            SettingsCategory.STANDBY -> {
                                StandbySettingsSegment(
                                    standbyModeEnabled = standbyModeEnabled,
                                    onStandbyModeEnabledChange = { standbyModeEnabled = it },
                                    standbyStartTime = standbyStartTime,
                                    onStandbyStartTimeChange = { standbyStartTime = it },
                                    standbyEndTime = standbyEndTime,
                                    onStandbyEndTimeChange = { standbyEndTime = it }
                                )
                            }

                            SettingsCategory.RESET -> {
                                ResetSettingsSegment(onResetToDefaults = onResetToDefaults)
                            }
                        }
                    }

                    // Save / Apply Button (only for config-saving categories)
                    if (selectedCategory in listOf(
                            SettingsCategory.LOCATION,
                            SettingsCategory.IQOMAH,
                            SettingsCategory.PRAYER_OFFSETS,
                            SettingsCategory.METHOD,
                            SettingsCategory.STANDBY
                        )
                    ) {
                        var isSaving by remember { mutableStateOf(false) }
                        val coroutineScope = rememberCoroutineScope()

                        TvFocusablePrimaryButton(
                            onClick = {
                                if (isSaving) return@TvFocusablePrimaryButton
                                isSaving = true
                                coroutineScope.launch {
                                    if (selectedCategory == SettingsCategory.STANDBY) {
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
        }
    }
}


enum class SettingsCategory(
    val getDisplayName: @Composable () -> String,
    val getDescription: @Composable () -> String
) {
    LOCATION(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("Mosque Coordinates", "Koordinat Masjid") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Adjust the latitude and longitude coordinates of the mosque to calculate prayer times accurately.", "Sesuaikan koordinat lintang (latitude) dan bujur (longitude) masjid untuk menghitung jadwal sholat dengan akurat.") }
    ),
    IQOMAH(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("Duration & Interval", "Durasi & Jeda Waktu") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Set the countdown waiting time (minutes) before iqomah for each prayer, as well as the adzan preparation duration.", "Atur jeda waktu tunggu (menit) sebelum iqomah untuk setiap waktu sholat berjamaah, serta durasi tampilan persiapan adzan sebelum waktu sholat tiba.") }
    ),
    PRAYER_OFFSETS(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("Prayer Time Adjustments", "Penyesuaian Waktu Sholat") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Adjust each prayer time to be a few minutes earlier or later relative to calculations.", "Sesuaikan masing-masing waktu sholat agar beberapa menit lebih cepat atau lebih lambat dari hasil perhitungan.") }
    ),
    METHOD(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("Calculation Method", "Metode Perhitungan") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Select the standard prayer calculation convention to apply in your area.", "Pilih standar konvensi perhitungan waktu sholat yang ingin diterapkan di wilayah masjid Anda.") }
    ),
    ANNOUNCEMENTS(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("Manage Announcements", "Kelola Pengumuman") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Add, manage, or delete scrolling running text shown at the bottom of the main dashboard.", "Tambahkan, kelola, atau hapus teks berjalan yang akan ditampilkan di bagian bawah layar utama.") }
    ),
    THEME(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("Background Theme", "Latar Belakang") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Choose a custom background image or slideshow media from your device.", "Pilih gambar latar belakang layar utama dari penyimpanan perangkat Anda.") }
    ),
    STANDBY(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("Standby & Screen", "Standby & Layar") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Configure automatic standby hours (black screen) and keep the TV screen always active outside those hours.", "Atur jam operasional standby otomatis (layar hitam) dan jaga agar layar TV selalu aktif di luar jam tersebut.") }
    ),
    WEB_CONTROL(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("Web Control (Remote)", "Kontrol Web (Remote)") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Access and configure all application settings from a phone/laptop browser on the same network.", "Akses dan atur seluruh konfigurasi aplikasi dari browser HP atau Laptop Anda melalui koneksi lokal.") }
    ),
    DEVICES(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("Connected Devices", "Perangkat Terhubung") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Manage devices connected to the remote web control, assign Master, and set security PIN.", "Kelola perangkat/HP yang terhubung ke kontrol web remote, tentukan Master, dan atur keamanan PIN.") }
    ),
    RESET(
        getDisplayName = { com.groovernapp.mosqueathan.util.Lang.get("Reset Settings", "Reset Pengaturan") },
        getDescription = { com.groovernapp.mosqueathan.util.Lang.get("Restore all coordinates, iqomah durations, calculation method, and announcements to factory defaults.", "Mengembalikan seluruh nilai koordinat, durasi iqomah, metode, serta running text ke konfigurasi bawaan (default).") }
    )
}
