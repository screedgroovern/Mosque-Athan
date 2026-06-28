package com.groovernapp.mosqueathan.ui.settings

import com.groovernapp.mosqueathan.ui.settings.prayerschedule.*
import com.groovernapp.mosqueathan.ui.settings.adzaniqomah.*
import com.groovernapp.mosqueathan.ui.settings.displaymedia.*
import com.groovernapp.mosqueathan.ui.settings.remotedevices.*
import com.groovernapp.mosqueathan.ui.settings.systemmaintenance.*
import com.groovernapp.mosqueathan.ui.settings.fridaysettings.*

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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import com.groovernapp.mosqueathan.data.copy
import com.groovernapp.mosqueathan.data.AnnouncementEntity
import com.groovernapp.mosqueathan.data.MediaEntity
import com.groovernapp.mosqueathan.data.ConnectedDeviceEntity
import com.groovernapp.mosqueathan.server.SettingsServer
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Close
import com.groovernapp.mosqueathan.util.Lang

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onPreviewAdzan: () -> Unit = {},
    onPreviewIqomah: () -> Unit = {},
    onPreviewSlideshow: () -> Unit = {},
    onPreviewFridaySlideshow: () -> Unit = {},
    onBack: () -> Unit
) {

    val config by viewModel.configState.collectAsState()
    val announcements by viewModel.announcementsState.collectAsState()
    val mediaList by viewModel.mediaListState.collectAsState()
    val fridayMediaList by viewModel.fridayMediaListState.collectAsState()
    val devices by viewModel.devicesState.collectAsState()
    val citySearchResults by viewModel.citySearchResults.collectAsState()
    val isSearchingCities by viewModel.isSearchingCities.collectAsState()
    val kemenagSchedules by viewModel.kemenagSchedulesState.collectAsState()
    val slideshowSchedules by viewModel.slideshowSchedulesState.collectAsState()
    val dashboardBackgrounds by viewModel.dashboardBackgroundsState.collectAsState()
    val standbySchedules by viewModel.standbySchedulesState.collectAsState()

    val showAllSettings = true

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

    // Friday States
    var jumatEnabled by remember(config.jumatEnabled) { mutableStateOf(config.jumatEnabled) }
    var jumatStartTime by remember(config.jumatStartTime) { mutableStateOf(config.jumatStartTime) }
    var jumatEndTime by remember(config.jumatEndTime) { mutableStateOf(config.jumatEndTime) }
    var offsetJumat by remember(config.offsetJumat) { mutableStateOf(config.offsetJumat) }
    var jumatStandbyOnly by remember(config.jumatStandbyOnly) { mutableStateOf(config.jumatStandbyOnly) }
    var jumatWarningText by remember(config.jumatWarningText) { mutableStateOf(config.jumatWarningText) }
    var murottalEnabled by remember(config.murottalEnabled) { mutableStateOf(config.murottalEnabled) }
    var murottalStartDelay by remember(config.murottalStartDelay) { mutableStateOf(config.murottalStartDelay) }
    var murottalAudioPaths by remember(config.murottalAudioPaths) { mutableStateOf(config.murottalAudioPaths) }
    var jumatSlideshowEnabled by remember(config.jumatSlideshowEnabled) { mutableStateOf(config.jumatSlideshowEnabled) }
    var includeGlobalSlideshow by remember(config.includeGlobalSlideshow) { mutableStateOf(config.includeGlobalSlideshow) }
    var selectedGlobalSlideIds by remember(config.selectedGlobalSlideIds) { mutableStateOf(config.selectedGlobalSlideIds) }
    var jumatTransitionType by remember(config.jumatTransitionType) { mutableStateOf(config.jumatTransitionType) }
    var jumatSlideIntervalSeconds by remember(config.jumatSlideIntervalSeconds) { mutableStateOf(config.jumatSlideIntervalSeconds) }
    var jumatSlideIntervalUnit by remember(config.jumatSlideIntervalUnit) { mutableStateOf(config.jumatSlideIntervalUnit) }
    var jumatVideoWaitUntilEnd by remember(config.jumatVideoWaitUntilEnd) { mutableStateOf(config.jumatVideoWaitUntilEnd) }

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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var isSideSheetExpanded by remember { mutableStateOf(!isCompact) }

    androidx.activity.compose.BackHandler(enabled = isCompact && drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    val displayedCategories = remember(showAllSettings) {
        if (showAllSettings) {
            SettingsCategory.values()
        } else {
            arrayOf(SettingsCategory.REMOTE_DEVICES)
        }
    }

    val versionName = remember(context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
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

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = isCompact,
            drawerContent = {
                if (isCompact) {
                    ModalDrawerSheet(
                        drawerContainerColor = Color(0xFF121212),
                        modifier = Modifier.width(280.dp)
                    ) {
                        SideSheetContent(
                            width = 280.dp,
                            isCollapsed = false,
                            selectedCategory = selectedCategory,
                            onCategorySelect = { category ->
                                selectedCategory = category
                                coroutineScope.launch { drawerState.close() }
                            },
                            isSideSheetExpanded = drawerState.isOpen,
                            onToggleExpand = { coroutineScope.launch { drawerState.close() } },
                            isTv = false,
                            onBack = onBack,
                            versionName = versionName,
                            showAllSettings = showAllSettings,
                            displayedCategories = displayedCategories,
                            modifier = Modifier.safeDrawingPadding()
                        )
                    }
                }
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
            // 1. Persistent Side Sheet (only for TV / Tablet)
            if (!isCompact) {
                val sideSheetWidth by animateDpAsState(
                    targetValue = if (isTv) 280.dp else if (isSideSheetExpanded) 280.dp else 72.dp,
                    label = "SideSheetWidth"
                )
                SideSheetContent(
                    width = sideSheetWidth,
                    isCollapsed = !isTv && (sideSheetWidth < 200.dp),
                    selectedCategory = selectedCategory,
                    onCategorySelect = { selectedCategory = it },
                    isSideSheetExpanded = isSideSheetExpanded,
                    onToggleExpand = { isSideSheetExpanded = !isSideSheetExpanded },
                    isTv = isTv,
                    onBack = onBack,
                    versionName = versionName,
                    showAllSettings = showAllSettings,
                    displayedCategories = displayedCategories
                )
            }

            // 2. Main Detail Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(
                        start = if (isCompact) 16.dp else 32.dp,
                        end = if (isCompact) 16.dp else 48.dp,
                        top = if (isCompact) 16.dp else 32.dp,
                        bottom = if (isCompact) 16.dp else 32.dp
                    )
            ) {
                // Details Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Hamburger Menu Button (only on phone)
                        if (isCompact) {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Menu",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = selectedCategory.getDisplayName(),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Back button on top right of detail area (phone)
                    if (isCompact) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Description of the category
                if (!isCompact) {
                    Text(
                        text = selectedCategory.getDescription(),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                // Category specific UI Card
                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.0f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isCompact) 16.dp else 32.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (selectedCategory in listOf(SettingsCategory.PRAYER_SCHEDULE, SettingsCategory.ADZAN_IQOMAH)) {
                                        Modifier.padding(bottom = 16.dp)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            // Category specific UI — each segment is in its own file
                            when (selectedCategory) {
                                SettingsCategory.PRAYER_SCHEDULE -> {
                                    PrayerScheduleSettingsSegment(
                                        latText = latText,
                                        onLatTextChange = {
                                            latText = it
                                            val d = it.toDoubleOrNull()
                                            if (d != null && d != config.latitude) {
                                                viewModel.updateConfig(config.copy(latitude = d))
                                            }
                                        },
                                        lngText = lngText,
                                        onLngTextChange = {
                                            lngText = it
                                            val d = it.toDoubleOrNull()
                                            if (d != null && d != config.longitude) {
                                                viewModel.updateConfig(config.copy(longitude = d))
                                            }
                                        },
                                        calculationMethodName = calculationMethodName,
                                        kemenagCityId = kemenagCityId,
                                        kemenagCityName = kemenagCityName,
                                        kemenagDuration = kemenagDuration,
                                        kemenagTargetDate = kemenagTargetDate,
                                        citySearchResults = citySearchResults,
                                        isSearchingCities = isSearchingCities,
                                        kemenagSchedules = kemenagSchedules,
                                        onMethodChange = {
                                            calculationMethodName = it
                                            viewModel.updateConfig(config.copy(calculationMethodName = it))
                                        },
                                        onKemenagCitySelected = { id, name ->
                                            kemenagCityId = id
                                            kemenagCityName = name
                                            viewModel.updateConfig(config.copy(kemenagCityId = id, kemenagCityName = name))
                                        },
                                        onKemenagDurationChange = {
                                            kemenagDuration = it
                                            viewModel.updateConfig(config.copy(kemenagDuration = it))
                                        },
                                        onKemenagTargetDateChange = {
                                            kemenagTargetDate = it
                                            viewModel.updateConfig(config.copy(kemenagTargetDate = it))
                                        },
                                        onSearchKemenagCities = { query -> viewModel.searchKemenagCities(query) },
                                        onDownloadKemenagSchedules = {
                                            viewModel.updateConfig(
                                                config.copy(
                                                    calculationMethodName = "KEMENAG",
                                                    kemenagCityId = kemenagCityId,
                                                    kemenagCityName = kemenagCityName,
                                                    kemenagDuration = kemenagDuration,
                                                    kemenagTargetDate = kemenagTargetDate
                                                )
                                            )
                                        },
                                        offsetImsak = offsetImsak,
                                        offsetSubuh = offsetSubuh,
                                        offsetSyuruq = offsetSyuruq,
                                        offsetDzuhur = offsetDzuhur,
                                        offsetAshar = offsetAshar,
                                        offsetMaghrib = offsetMaghrib,
                                        offsetIsya = offsetIsya,
                                        onOffsetImsakChange = {
                                            offsetImsak = it
                                            viewModel.updateConfig(config.copy(offsetImsak = it))
                                        },
                                        onOffsetSubuhChange = {
                                            offsetSubuh = it
                                            viewModel.updateConfig(config.copy(offsetSubuh = it))
                                        },
                                        onOffsetSyuruqChange = {
                                            offsetSyuruq = it
                                            viewModel.updateConfig(config.copy(offsetSyuruq = it))
                                        },
                                        onOffsetDzuhurChange = {
                                            offsetDzuhur = it
                                            viewModel.updateConfig(config.copy(offsetDzuhur = it))
                                        },
                                        onOffsetAsharChange = {
                                            offsetAshar = it
                                            viewModel.updateConfig(config.copy(offsetAshar = it))
                                        },
                                        onOffsetMaghribChange = {
                                            offsetMaghrib = it
                                            viewModel.updateConfig(config.copy(offsetMaghrib = it))
                                        },
                                        onOffsetIsyaChange = {
                                            offsetIsya = it
                                            viewModel.updateConfig(config.copy(offsetIsya = it))
                                        },
                                        isCompact = isCompact
                                    )
                                }

                                SettingsCategory.ADZAN_IQOMAH -> {
                                     AdzanIqomahSettingsPane(
                                         adzanLeadMinutes = adzanLeadMinutes,
                                         onAdzanLeadMinutesChange = {
                                             adzanLeadMinutes = it
                                             viewModel.updateConfig(config.copy(adzanLeadMinutes = it))
                                         },
                                         iqomahSubuh = iqomahSubuh,
                                         onIqomahSubuhChange = {
                                             iqomahSubuh = it
                                             viewModel.updateConfig(config.copy(iqomahSubuh = it))
                                         },
                                         iqomahDzuhur = iqomahDzuhur,
                                         onIqomahDzuhurChange = {
                                             iqomahDzuhur = it
                                             viewModel.updateConfig(config.copy(iqomahDzuhur = it))
                                         },
                                         iqomahAshar = iqomahAshar,
                                         onIqomahAsharChange = {
                                             iqomahAshar = it
                                             viewModel.updateConfig(config.copy(iqomahAshar = it))
                                         },
                                         iqomahMaghrib = iqomahMaghrib,
                                         onIqomahMaghribChange = {
                                             iqomahMaghrib = it
                                             viewModel.updateConfig(config.copy(iqomahMaghrib = it))
                                         },
                                         iqomahIsya = iqomahIsya,
                                         onIqomahIsyaChange = {
                                             iqomahIsya = it
                                             viewModel.updateConfig(config.copy(iqomahIsya = it))
                                         },
                                         config = config,
                                         onUpdateFooters = { adzan, iqomah -> viewModel.updateFooters(adzan, iqomah) },
                                         onPreviewAdzan = onPreviewAdzan,
                                         onPreviewIqomah = onPreviewIqomah,
                                         isCompact = isCompact
                                     )
                                 }

                                 SettingsCategory.DISPLAY_MEDIA -> {
                                     DisplayMediaSettingsPane(
                                         config = config,
                                         announcements = announcements,
                                         mediaList = mediaList,
                                         slideshowSchedules = slideshowSchedules,
                                         dashboardBackgrounds = dashboardBackgrounds,
                                         onUpdateThemeSettings = { type, interval, videoWait, defaultImg ->
                                             viewModel.updateThemeSettings(type, interval, videoWait, defaultImg)
                                         },
                                         onAddMediaItems = { uris -> viewModel.addMediaItems(uris, context) },
                                         onUpdateMediaItemPath = { id, uri -> viewModel.updateMediaItemPath(id, uri, context) },
                                         onUpdateMediaItemUsed = { id, isUsed -> viewModel.updateMediaItemUsed(id, isUsed) },
                                         onDeleteMediaItem = { item -> viewModel.deleteMediaItem(item) },
                                         onReorderMediaItems = { ids -> viewModel.updateMediaListOrder(ids) },
                                         onSelectCustomBackground = { uri -> viewModel.updateCustomBackground(uri, context) },
                                         onClearCustomBackground = { viewModel.clearCustomBackground(context) },
                                         onAddSlideshowSchedule = { day, isSpecific, start, end ->
                                             viewModel.addSlideshowSchedule(day, isSpecific, start, end)
                                         },
                                         onUpdateSlideshowScheduleTime = { schedule, startTime, endTime ->
                                             viewModel.updateSlideshowScheduleTime(schedule.id, startTime, endTime)
                                         },
                                         onUpdateSlideshowScheduleDays = { schedule, activeDays ->
                                             viewModel.updateSlideshowScheduleDays(schedule.id, activeDays)
                                         },
                                         onUpdateSlideshowScheduleDates = { schedule, specificDates ->
                                             viewModel.updateSlideshowScheduleDates(schedule.id, specificDates)
                                         },
                                         onDeleteSlideshowSchedule = { id -> viewModel.deleteSlideshowSchedule(id) },
                                         onUpdateSlideshowEnabled = { enabled -> viewModel.updateSlideshowEnabled(enabled) },
                                         onPreviewSlideshow = onPreviewSlideshow,
                                         onAddDashboardBackground = { uris -> viewModel.addDashboardBackgrounds(uris, context) },
                                         onUpdateDashboardBackgroundPath = { id, uri -> viewModel.updateDashboardBackgroundPath(id, uri, context) },
                                         onDeleteDashboardBackground = { id -> viewModel.deleteDashboardBackground(id) },
                                         onUpdateDashboardBackgroundUsed = { id, isUsed -> viewModel.updateDashboardBackgroundUsed(id, isUsed) },
                                         onUpdateDashboardBackgroundOrder = { ids -> viewModel.updateDashboardBackgroundOrder(ids) },
                                         onUpdateDashboardSettings = { randomize, transitionType, intervalSeconds ->
                                             viewModel.updateDashboardSettings(randomize, transitionType, intervalSeconds)
                                         },
                                         onAddAnnouncement = { content -> viewModel.addAnnouncement(content) },
                                         onUpdateAnnouncement = { id, content -> viewModel.editAnnouncement(id, content) },
                                         onDeleteAnnouncement = { id -> viewModel.deleteAnnouncement(id) },
                                         onReorderAnnouncements = { ids -> viewModel.reorderAnnouncements(ids) },
                                         isCompact = isCompact
                                     )
                                 }

                                 SettingsCategory.REMOTE_DEVICES -> {
                                     RemoteDevicesSettingsPane(
                                         config = config,
                                         isTv = isTv,
                                         onUpdateShowSettingsOnTv = { show -> viewModel.updateShowSettingsOnTv(show) },
                                         devices = devices,
                                         onSetDeviceMaster = { deviceId -> viewModel.setDeviceMaster(deviceId) },
                                         onDeleteDevice = { deviceId -> viewModel.deleteDevice(deviceId) },
                                         onUpdateConnectivityMode = { mode -> viewModel.updateConnectivityMode(mode) },
                                         onUpdateRemoteTvIp = { ip -> viewModel.updateRemoteTvIp(ip) },
                                         onUpdatePinCode = { pin -> viewModel.updatePinCode(pin) },
                                         onSyncWithTv = { viewModel.syncWithTvServer(context) },
                                         isCompact = isCompact
                                     )
                                 }

                                 SettingsCategory.FRIDAY_SETTINGS -> {
                                     FridaySettingsPane(
                                         jumatEnabled = jumatEnabled,
                                         onJumatEnabledChange = {
                                             jumatEnabled = it
                                             viewModel.updateConfig(config.copy(jumatEnabled = it))
                                         },
                                         jumatStartTime = jumatStartTime,
                                         onJumatStartTimeChange = {
                                             jumatStartTime = it
                                             viewModel.updateConfig(config.copy(jumatStartTime = it))
                                         },
                                         jumatEndTime = jumatEndTime,
                                         onJumatEndTimeChange = {
                                             jumatEndTime = it
                                             viewModel.updateConfig(config.copy(jumatEndTime = it))
                                         },
                                         offsetJumat = offsetJumat,
                                         onOffsetJumatChange = {
                                             offsetJumat = it
                                             viewModel.updateConfig(config.copy(offsetJumat = it))
                                         },
                                         jumatStandbyOnly = jumatStandbyOnly,
                                         onJumatStandbyOnlyChange = {
                                             jumatStandbyOnly = it
                                             viewModel.updateConfig(config.copy(jumatStandbyOnly = it))
                                         },
                                         jumatWarningText = jumatWarningText,
                                         onJumatWarningTextChange = {
                                             jumatWarningText = it
                                             viewModel.updateConfig(config.copy(jumatWarningText = it))
                                         },
                                         murottalEnabled = murottalEnabled,
                                         onMurottalEnabledChange = {
                                             murottalEnabled = it
                                             viewModel.updateConfig(config.copy(murottalEnabled = it))
                                         },
                                         murottalStartDelay = murottalStartDelay,
                                         onMurottalStartDelayChange = {
                                             murottalStartDelay = it
                                             viewModel.updateConfig(config.copy(murottalStartDelay = it))
                                         },
                                         murottalAudioPaths = murottalAudioPaths,
                                         onMurottalAudioPathsChange = {
                                             murottalAudioPaths = it
                                             viewModel.updateConfig(config.copy(murottalAudioPaths = it))
                                         },
                                         jumatSlideshowEnabled = jumatSlideshowEnabled,
                                         onJumatSlideshowEnabledChange = {
                                             jumatSlideshowEnabled = it
                                             viewModel.updateConfig(config.copy(jumatSlideshowEnabled = it))
                                         },
                                         includeGlobalSlideshow = includeGlobalSlideshow,
                                         onIncludeGlobalSlideshowChange = {
                                             includeGlobalSlideshow = it
                                             viewModel.updateConfig(config.copy(includeGlobalSlideshow = it))
                                         },
                                         selectedGlobalSlideIds = selectedGlobalSlideIds,
                                         onSelectedGlobalSlideIdsChange = {
                                             selectedGlobalSlideIds = it
                                             viewModel.updateConfig(config.copy(selectedGlobalSlideIds = it))
                                         },
                                         jumatTransitionType = jumatTransitionType,
                                         onJumatTransitionTypeChange = {
                                             jumatTransitionType = it
                                             viewModel.updateConfig(config.copy(jumatTransitionType = it))
                                         },
                                         jumatSlideIntervalSeconds = jumatSlideIntervalSeconds,
                                         onJumatSlideIntervalSecondsChange = {
                                             jumatSlideIntervalSeconds = it
                                             viewModel.updateConfig(config.copy(jumatSlideIntervalSeconds = it))
                                         },
                                         jumatSlideIntervalUnit = jumatSlideIntervalUnit,
                                         onJumatSlideIntervalUnitChange = {
                                             jumatSlideIntervalUnit = it
                                             viewModel.updateConfig(config.copy(jumatSlideIntervalUnit = it))
                                         },
                                         jumatVideoWaitUntilEnd = jumatVideoWaitUntilEnd,
                                         onJumatVideoWaitUntilEndChange = {
                                             jumatVideoWaitUntilEnd = it
                                             viewModel.updateConfig(config.copy(jumatVideoWaitUntilEnd = it))
                                         },
                                         fridayMediaList = fridayMediaList,
                                         mediaList = mediaList,
                                         onAddFridayMediaItems = { uris -> viewModel.addFridayMediaItems(uris, context) },
                                         onUpdateFridayMediaItemPath = { id, uri -> viewModel.updateFridayMediaItemPath(id, uri, context) },
                                         onDeleteFridayMediaItem = { item -> viewModel.deleteFridayMediaItem(item) },
                                         onReorderFridayMediaItems = { ids -> viewModel.updateFridayMediaOrder(ids) },
                                         onPreviewFridaySlideshow = onPreviewFridaySlideshow,
                                         isCompact = isCompact
                                     )
                                 }

                                 SettingsCategory.SYSTEM_MAINTENANCE -> {
                                     SystemMaintenanceSettingsPane(
                                         standbyModeEnabled = standbyModeEnabled,
                                         onStandbyModeEnabledChange = {
                                             standbyModeEnabled = it
                                             viewModel.updateStandbyEnabled(it)
                                         },
                                         standbySchedules = standbySchedules,
                                         onAddStandbySchedule = { day, isSpecific, start, end ->
                                             viewModel.addStandbySchedule(day, isSpecific, start, end)
                                         },
                                         onDeleteStandbySchedule = { id -> viewModel.deleteStandbySchedule(id) },
                                         onUpdateStandbyScheduleTime = { schedule, start, end ->
                                             viewModel.updateStandbyScheduleTime(schedule.id, start, end)
                                         },
                                         onUpdateStandbyScheduleDays = { schedule, days ->
                                             viewModel.updateStandbyScheduleDays(schedule.id, days)
                                         },
                                         onUpdateStandbyScheduleDates = { schedule, dates ->
                                             viewModel.updateStandbyScheduleDates(schedule.id, dates)
                                         },
                                         onResetToDefaults = { viewModel.resetToDefaults(context) }
                                     )
                                 }
                            }
                        }


                    }
                }
            }
        }
        }
    }
}

@Composable
fun SideSheetContent(
    width: androidx.compose.ui.unit.Dp,
    isCollapsed: Boolean,
    selectedCategory: SettingsCategory,
    onCategorySelect: (SettingsCategory) -> Unit,
    isSideSheetExpanded: Boolean,
    onToggleExpand: () -> Unit,
    isTv: Boolean,
    onBack: () -> Unit,
    versionName: String,
    showAllSettings: Boolean,
    displayedCategories: Array<SettingsCategory>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(vertical = 24.dp, horizontal = if (isCollapsed) 8.dp else 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = if (isCollapsed) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = if (isCollapsed) Alignment.CenterHorizontally else Alignment.Start
        ) {
            // Header Settings with Gear Icon
            if (isCollapsed) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = Lang.get("Settings", "Pengaturan"),
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (!isTv) Modifier.clickable { onToggleExpand() } else Modifier
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Lang.get("Settings", "Pengaturan"),
                            color = Color(0xFFFFD54F),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isTv) {
                        IconButton(onClick = onToggleExpand) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuOpen,
                                contentDescription = "Collapse Menu",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Categories items list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = if (isCollapsed) Alignment.CenterHorizontally else Alignment.Start
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
                    val tintColor = if (isSelected || isFocusedOrHovered) Color(0xFFFFD54F) else Color.White

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
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
                                onCategorySelect(category)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCollapsed) {
                            if (category.icon is androidx.compose.ui.graphics.vector.ImageVector) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = category.getDisplayName(),
                                    tint = tintColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (category.icon is Int) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = category.icon),
                                    contentDescription = category.getDisplayName(),
                                    tint = tintColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (category.icon is androidx.compose.ui.graphics.vector.ImageVector) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = tintColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else if (category.icon is Int) {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(id = category.icon),
                                        contentDescription = null,
                                        tint = tintColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = category.getDisplayName(),
                                    color = tintColor,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected || isFocusedOrHovered) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom area
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = if (isCollapsed) Alignment.CenterHorizontally else Alignment.Start
        ) {
            if (!isCollapsed) {
                Text(
                    text = Lang.get("Version: v$versionName", "Versi: v$versionName"),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                )
            }

            TvFocusableButton(
                onClick = onBack,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isCollapsed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Lang.get("Back", "Kembali"),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = Lang.get("Back", "Kembali"),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

enum class SettingsCategory(
    val getDisplayName: @Composable () -> String,
    val getDescription: @Composable () -> String,
    val icon: Any // Accepts ImageVector or Drawable Res ID (Int)
) {
    PRAYER_SCHEDULE(
        getDisplayName = { Lang.get("Prayer Schedule", "Jadwal Shalat") },
        getDescription = { Lang.get("Manage coordinates, calculation methods, and manual time corrections.", "Atur koordinat GPS, metode perhitungan, dan penyesuaian waktu (offset) tiap shalat.") },
        icon = Icons.Default.AccessTime
    ),
    ADZAN_IQOMAH(
        getDisplayName = { Lang.get("Adzan & Iqomah", "Adzan & Iqomah") },
        getDescription = { Lang.get("Set the adzan preparation duration and the iqomah wait time for each prayer.", "Atur durasi persiapan adzan dan jeda waktu iqomah untuk masing-masing waktu shalat.") },
        icon = Icons.Default.Campaign
    ),
    DISPLAY_MEDIA(
        getDisplayName = { Lang.get("Display & Media", "Tampilan & Media") },
        getDescription = { Lang.get("Configure background wallpapers, slideshows, and scrolling running texts.", "Atur gambar latar belakang, interval slideshow, dan running text.") },
        icon = Icons.Default.Image
    ),
    REMOTE_DEVICES(
        getDisplayName = { Lang.get("Connectivity", "Konektivitas") },
        getDescription = { Lang.get("Manage web control server access, connected devices, and PIN security.", "Kelola akses server kontrol web, perangkat yang terhubung, PIN, dan otoritas master.") },
        icon = Icons.Default.Devices
    ),
    FRIDAY_SETTINGS(
        getDisplayName = { Lang.get("Friday", "Hari Jum'at") },
        getDescription = { Lang.get("Configure Friday specific schedules, standby modes, khutbah warnings, and murottal audio.", "Atur jadwal khusus hari Jumat, standby, murottal sebelum adzan, dan teks pengumuman khutbah.") },
        icon = com.groovernapp.mosqueathan.R.drawable.ic_star_crescent
    ),
    SYSTEM_MAINTENANCE(
        getDisplayName = { Lang.get("System & Reset", "Sistem & Reset") },
        getDescription = { Lang.get("Configure TV standby hours and restore application settings to defaults.", "Atur jadwal layar hitam (standby) otomatis dan kembalikan pengaturan ke setelan pabrik.") },
        icon = Icons.Default.Restore
    )
}
