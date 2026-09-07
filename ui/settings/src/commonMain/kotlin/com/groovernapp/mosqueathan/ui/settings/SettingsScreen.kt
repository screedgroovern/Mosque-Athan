package com.groovernapp.mosqueathan.ui.settings

import androidx.compose.ui.graphics.vector.ImageVector
import com.groovernapp.mosqueathan.ui.design.displayBackgrounds
import com.groovernapp.mosqueathan.ui.design.icon.StarCrescent
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Refresh
import com.groovernapp.mosqueathan.ui.design.LinkStatus
import com.groovernapp.mosqueathan.ui.design.LocalLinkStatus
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Close
import com.groovernapp.mosqueathan.util.Lang
import com.groovernapp.mosqueathan.ui.design.appVersion
import com.groovernapp.mosqueathan.ui.design.MessageHost
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import com.groovernapp.mosqueathan.ui.design.SystemBackHandler
import com.groovernapp.mosqueathan.ui.design.MediaImageState
import com.groovernapp.mosqueathan.ui.design.rememberMediaImage
import com.groovernapp.mosqueathan.ui.design.component.MosqueSecondaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueIconButton
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.groovernapp.mosqueathan.ui.design.LocalDisplayApproval
import com.groovernapp.mosqueathan.ui.settings.pairing.PinGate
import com.groovernapp.mosqueathan.ui.settings.sync.SyncConflictDialog
import com.groovernapp.mosqueathan.ui.settings.remotedevices.RemoteDevicesEvent
import com.groovernapp.mosqueathan.ui.settings.remotedevices.RemoteDevicesViewModel
import com.groovernapp.mosqueathan.ui.settings.adzaniqomah.AdzanIqomahEvent
import com.groovernapp.mosqueathan.ui.settings.adzaniqomah.AdzanIqomahViewModel
import com.groovernapp.mosqueathan.ui.settings.prayerschedule.PrayerScheduleViewModel
import com.groovernapp.mosqueathan.ui.settings.displaymedia.DisplayMediaEvent
import com.groovernapp.mosqueathan.ui.settings.displaymedia.DisplayMediaViewModel
import com.groovernapp.mosqueathan.ui.settings.fridaysettings.FridayEvent
import com.groovernapp.mosqueathan.ui.settings.fridaysettings.FridayViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onPreviewAdzan: () -> Unit = {},
    onPreviewIqomah: () -> Unit = {},
    onPreviewSlideshow: () -> Unit = {},
    onPreviewFridaySlideshow: () -> Unit = {},
    /**
     * Leaving settings. Null when there is nowhere to go — in the browser the settings UI *is* the
     * app, where on the television it is a screen you leave to return to the clock. The close and
     * back controls are then not drawn at all, rather than drawn and doing nothing: both showed a
     * focus ring on the web build and silently ignored every press.
     */
    onBack: (() -> Unit)?,
    /**
     * Re-pulls everything from the display. Null where there is nothing to pull — the display
     * itself, and the Android controller, which posts each action and keeps no connection to
     * refresh. Null hides the control rather than showing one that does nothing.
     */
    onSync: (() -> Unit)? = null
) {

    val config by viewModel.configState.collectAsState()
    val announcements by viewModel.announcementsState.collectAsState()
    val mediaList by viewModel.mediaListState.collectAsState()
    val devices by viewModel.devicesState.collectAsState()
    val slideshowSchedules by viewModel.slideshowSchedulesState.collectAsState()
    val dashboardBackgrounds by viewModel.dashboardBackgroundsState.collectAsState()
    val standbySchedules by viewModel.standbySchedulesState.collectAsState()
    val syncConflict by viewModel.syncConflict.collectAsState()

    val showAllSettings = true

    var selectedCategory by remember(showAllSettings) {
        mutableStateOf(if (showAllSettings) SettingsCategory.PRAYER_SCHEDULE else SettingsCategory.REMOTE_DEVICES)
    }
    val isTv = LocalWindowClass.current.isTv

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

    // Null while it is still resolving, so the default backdrop shows rather than nothing. On a
    // controller this is a fetch from the display, not a file read.
    val customBgBitmap = (rememberMediaImage(config.customBackgroundPath) as? MediaImageState.Loaded)?.image

    val isCompact = LocalWindowClass.current.isCompact

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    // Keyed on isCompact deliberately. Without the key this is computed once, and on the web the
    // first composition happens before the canvas reports its size — so the window looks Compact,
    // the sheet starts collapsed, and it stays a strip of unlabelled icons however wide the window
    // is. Android never saw this because its first frame already has the real size.
    var isSideSheetExpanded by remember(isCompact) { mutableStateOf(!isCompact) }

    SystemBackHandler(enabled = isCompact && drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    val displayedCategories = remember(showAllSettings) {
        if (showAllSettings) {
            SettingsCategory.values()
        } else {
            arrayOf(SettingsCategory.REMOTE_DEVICES)
        }
    }

    val versionName = appVersion

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
                painter = displayBackgrounds.dashboard,
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
                            MosqueTheme.colors.scrim.copy(alpha = 0.85f),
                            MosqueTheme.colors.surface.copy(alpha = 0.90f),
                            MosqueTheme.colors.scrim.copy(alpha = 0.92f)
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
                        drawerContainerColor = MosqueTheme.colors.surface,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
            SettingsHeaderBar(
                isServer = config.connectivityMode != REMOTE_WIRE_VALUE,
                controllersOnline = devices.count { it.isOnline },
                status = LocalLinkStatus.current,
                onSync = onSync,
                isCompact = isCompact
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                    modifier = Modifier.fillMaxWidth().padding(bottom = MosqueTheme.spacing.xl),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Hamburger Menu Button (only on phone)
                        if (isCompact) {
                            MosqueIconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.padding(end = MosqueTheme.spacing.md)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Menu",
                                    tint = MosqueTheme.colors.textPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = selectedCategory.getDisplayName(),
                            color = MosqueTheme.colors.textPrimary,
                            fontSize = MosqueTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Back button on top right of detail area (phone)
                    if (isCompact && onBack != null) {
                        MosqueIconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MosqueTheme.colors.textPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Description of the category
                if (!isCompact) {
                    Text(
                        text = selectedCategory.getDescription(),
                        color = MosqueTheme.colors.textSecondary,
                        fontSize = MosqueTheme.typography.bodyCompact,
                        modifier = Modifier.padding(bottom = MosqueTheme.spacing.xxxl)
                    )
                }

                // Category specific UI Card
                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MosqueTheme.colors.scrim.copy(alpha = 0.0f)),
                    shape = RoundedCornerShape(MosqueTheme.radius.lg),
                    border = BorderStroke(MosqueTheme.stroke.hairline, MosqueTheme.colors.fillStrong)
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
                                        Modifier.padding(bottom = MosqueTheme.spacing.xl)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            // Category specific UI — each segment is in its own file
                            when (selectedCategory) {
                                SettingsCategory.PRAYER_SCHEDULE -> {
                                    val prayerScheduleViewModel: PrayerScheduleViewModel = koinViewModel()
                                    val prayerScheduleState by prayerScheduleViewModel.uiState.collectAsStateWithLifecycle()
                                    PrayerScheduleSettingsSegment(
                                        state = prayerScheduleState,
                                        onEvent = prayerScheduleViewModel::onEvent
                                    )
                                }

                                SettingsCategory.ADZAN_IQOMAH -> {
                                     val adzanIqomahViewModel: AdzanIqomahViewModel = koinViewModel()
                                     val adzanIqomahState by adzanIqomahViewModel.uiState.collectAsStateWithLifecycle()
                                     AdzanIqomahSettingsPane(
                                         state = adzanIqomahState,
                                         onEvent = { event ->
                                             // Preview swaps which screen AppViewModel drives — navigation,
                                             // not a setting — so it stays with the caller that owns it.
                                             when (event) {
                                                 AdzanIqomahEvent.PreviewAdzanRequested -> onPreviewAdzan()
                                                 AdzanIqomahEvent.PreviewIqomahRequested -> onPreviewIqomah()
                                                 else -> adzanIqomahViewModel.onEvent(event)
                                             }
                                         }
                                     )
                                 }

                                 SettingsCategory.DISPLAY_MEDIA -> {
                                     val displayMediaViewModel: DisplayMediaViewModel = koinViewModel()
                                     val displayMediaState by displayMediaViewModel.uiState.collectAsStateWithLifecycle()
                                     DisplayMediaSettingsPane(
                                         state = displayMediaState,
                                         onEvent = { event ->
                                             // Preview is navigation, not a setting; the pane handles its own
                                             // picker requests. Everything else is the category's business.
                                             if (event is DisplayMediaEvent.SlideshowPreviewRequested) {
                                                 onPreviewSlideshow()
                                             } else {
                                                 displayMediaViewModel.onEvent(event)
                                             }
                                         }
                                     )
                                 }

                                 SettingsCategory.REMOTE_DEVICES -> {
                                     val remoteDevicesViewModel: RemoteDevicesViewModel = koinViewModel()
                                     val remoteDevicesState by remoteDevicesViewModel.uiState.collectAsStateWithLifecycle()
                                     RemoteDevicesSettingsPane(
                                         state = remoteDevicesState,
                                         onEvent = { event ->
                                             // Syncing still needs an Android Context, so it stays here
                                             // rather than moving into the category ViewModel.
                                             if (event is RemoteDevicesEvent.SyncRequested) {
                                                 viewModel.syncWithTvServer(event.address)
                                             } else {
                                                 remoteDevicesViewModel.onEvent(event)
                                             }
                                         }
                                     )
                                 }

                                 SettingsCategory.FRIDAY_SETTINGS -> {
                                     val fridayViewModel: FridayViewModel = koinViewModel()
                                     val fridayState by fridayViewModel.uiState.collectAsStateWithLifecycle()
                                     FridaySettingsPane(
                                         state = fridayState,
                                         onEvent = { event ->
                                             // Preview is navigation, not a setting; the pane handles its own
                                             // picker requests. Everything else is the category's business.
                                             if (event is FridayEvent.SlideshowPreviewRequested) {
                                                 onPreviewFridaySlideshow()
                                             } else {
                                                 fridayViewModel.onEvent(event)
                                             }
                                         }
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
                                         onResetToDefaults = { viewModel.resetToDefaults() }
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

        // Last child of the root Box so it draws over the panes. The ViewModel reports
        // what happened — sync succeeded, sync failed — and this decides how the
        // platform says it; on Android that is still a Toast.
        MessageHost(viewModel.messages)

        // Only present while a sync found changes on both sides. Nothing has been written when
        // this appears: the ViewModel holds the display's snapshot and waits for the answer, so
        // dismissing leaves both devices exactly as they were.
        // Above everything, including the sync dialog: a device the display has not allowed can
        // neither read nor write, so there is nothing underneath worth interacting with. Drawn only
        // when a host can actually verify a PIN — a display has nobody to ask, provides no
        // DisplayApproval, and never sees this.
        val approval = LocalDisplayApproval.current
        if (LocalLinkStatus.current == LinkStatus.NeedsApproval && approval != null) {
            PinGate(approval)
        }

        syncConflict?.let { prompt ->
            SyncConflictDialog(
                server = prompt.server,
                local = prompt.local,
                onChoice = { viewModel.onSyncChoice(it) },
                onDismiss = { viewModel.dismissSyncConflict() }
            )
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
    /** See the same parameter on [SettingsScreen]: null means the Back button is not drawn. */
    onBack: (() -> Unit)?,
    versionName: String,
    showAllSettings: Boolean,
    displayedCategories: Array<SettingsCategory>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .background(MosqueTheme.colors.scrim.copy(alpha = 0.4f))
            .padding(vertical = MosqueTheme.spacing.xxxl, horizontal = if (isCollapsed) 8.dp else 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = if (isCollapsed) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg),
            horizontalAlignment = if (isCollapsed) Alignment.CenterHorizontally else Alignment.Start
        ) {
            // Header Settings with Gear Icon
            if (isCollapsed) {
                MosqueIconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.padding(bottom = MosqueTheme.spacing.xl)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = Lang.get("Settings", "Pengaturan"),
                        tint = MosqueTheme.colors.accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                // The title is drawn only where the bar above does not carry one.
                //
                // On a wide screen that bar reads "MOSQUE ATHAN SETTINGS", so a second gear and a
                // second title stacked directly under it are two headers for one screen. On a
                // phone the bar hides its text to make room for the status chips, so the drawer is
                // the only place the screen names itself and the title stays.
                //
                // What must survive either way is the *control*: this row is how the rail is
                // collapsed, and the collapsed state's gear is the only way to expand it again.
                val titleInDrawer = LocalWindowClass.current.isCompact
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = MosqueTheme.spacing.xl, start = MosqueTheme.spacing.md, end = MosqueTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (titleInDrawer) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = if (!isTv) Modifier.clickable { onToggleExpand() } else Modifier
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MosqueTheme.colors.accent,
                                modifier = Modifier.size(MosqueTheme.size.iconLg)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = Lang.get("Settings", "Pengaturan"),
                                color = MosqueTheme.colors.accent,
                                fontSize = MosqueTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Keeps the collapse control on the right where it has always been.
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (!isTv) {
                        MosqueIconButton(onClick = onToggleExpand) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuOpen,
                                contentDescription = "Collapse Menu",
                                tint = MosqueTheme.colors.accent,
                                modifier = Modifier.size(MosqueTheme.size.iconLg)
                            )
                        }
                    }
                }
            }

            // Categories items list.
            //
            // This list scrolls, and until now nothing said so. With six categories and a short
            // window the last one — Sistem & Reset — sat below the fold, while the version line and
            // the Back button underneath stayed pinned in place, which reads as "the list ends
            // here". The fade is drawn only while there is actually more to reach, so on a window
            // tall enough for all six nothing about the sidebar changes.
            //
            // drawWithContent sits *before* verticalScroll on purpose: that puts it outside the
            // scrolling node, so it paints in viewport coordinates and stays at the bottom edge
            // instead of sliding away with the content.
            val railScroll = rememberScrollState()
            val railFade = MosqueTheme.colors.scrim
            Column(
                modifier = Modifier
                    .weight(1f)
                    .drawWithContent {
                        drawContent()
                        if (railScroll.canScrollForward) {
                            val fadeHeight = 28.dp.toPx().coerceAtMost(size.height)
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, railFade),
                                    startY = size.height - fadeHeight,
                                    endY = size.height
                                ),
                                topLeft = Offset(0f, size.height - fadeHeight),
                                size = Size(size.width, fadeHeight)
                            )
                        }
                    }
                    .verticalScroll(railScroll),
                verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg),
                horizontalAlignment = if (isCollapsed) Alignment.CenterHorizontally else Alignment.Start
            ) {
                displayedCategories.forEach { category ->
                    val isSelected = selectedCategory == category
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    val isFocusedOrHovered = isFocused || isHovered

                    val borderColor = when {
                        isSelected -> MosqueTheme.colors.accent
                        isFocusedOrHovered -> MosqueTheme.colors.accent.copy(alpha = 0.8f)
                        else -> MosqueTheme.colors.fillStrong
                    }
                    val borderWidth = if (isSelected || isFocusedOrHovered) 2.dp else 1.dp
                    val backgroundColor = when {
                        isSelected && isFocusedOrHovered -> MosqueTheme.colors.accent.copy(alpha = 0.25f)
                        isSelected -> MosqueTheme.colors.accent.copy(alpha = 0.15f)
                        isFocusedOrHovered -> MosqueTheme.colors.fill
                        else -> MosqueTheme.colors.transparent
                    }
                    val tintColor = if (isSelected || isFocusedOrHovered) MosqueTheme.colors.accent else MosqueTheme.colors.textPrimary

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = MosqueTheme.size.controlXl)
                            .border(
                                width = borderWidth,
                                color = borderColor,
                                shape = RoundedCornerShape(MosqueTheme.radius.md)
                            )
                            .background(
                                backgroundColor,
                                RoundedCornerShape(MosqueTheme.radius.md)
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
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.getDisplayName(),
                                tint = tintColor,
                                modifier = Modifier.size(MosqueTheme.size.iconLg)
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = MosqueTheme.spacing.xl),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = tintColor,
                                    modifier = Modifier.size(MosqueTheme.size.iconLg)
                                )
                                Spacer(modifier = Modifier.width(MosqueTheme.spacing.lg))
                                Text(
                                    text = category.getDisplayName(),
                                    color = tintColor,
                                    fontSize = MosqueTheme.typography.body,
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
            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MosqueTheme.spacing.xl),
            horizontalAlignment = if (isCollapsed) Alignment.CenterHorizontally else Alignment.Start
        ) {
            if (!isCollapsed) {
                Text(
                    text = Lang.get("Version: v$versionName", "Versi: v$versionName"),
                    color = MosqueTheme.colors.textMuted,
                    fontSize = MosqueTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = MosqueTheme.spacing.lg, bottom = MosqueTheme.spacing.xs)
                )
            }

            if (onBack != null) MosqueSecondaryButton(
                onClick = onBack,
                shape = RoundedCornerShape(MosqueTheme.radius.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MosqueTheme.size.controlLg)
            ) {
                if (isCollapsed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Lang.get("Back", "Kembali"),
                        tint = MosqueTheme.colors.textPrimary,
                        modifier = Modifier.size(MosqueTheme.size.iconLg)
                    )
                } else {
                    Text(
                        text = Lang.get("Back", "Kembali"),
                        color = MosqueTheme.colors.textPrimary,
                        fontSize = MosqueTheme.typography.body,
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
    val icon: ImageVector
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
        icon = StarCrescent
    ),
    SYSTEM_MAINTENANCE(
        getDisplayName = { Lang.get("System & Reset", "Sistem & Reset") },
        getDescription = { Lang.get("Configure TV standby hours and restore application settings to defaults.", "Atur jadwal layar hitam (standby) otomatis dan kembalikan pengaturan ke setelan pabrik.") },
        icon = Icons.Default.Restore
    )
}

/** The wire value for a controller, matching the display and the Android app. */
private const val REMOTE_WIRE_VALUE = "REMOTE"

/**
 * The bar across the top of settings: what this device *is*, and whether it is reaching the display.
 *
 * The role is the part that is always shown, and it is the reason this bar exists rather than a
 * status chip tucked into the sidebar: one build runs on the mosque's display and on a takmir's
 * phone, and until now nothing on screen said which. A controller additionally shows whether it is
 * reaching the display; a display shows how many controllers are attached to it, because a display
 * is the source of truth and syncs nowhere — "Online" would mean nothing there.
 *
 * The status is hidden entirely at [LinkStatus.Unknown]. A host that cannot answer shows nothing
 * rather than a green light meaning "we did not check" — the Android controller posts each action
 * and keeps no connection, so it genuinely does not know.
 */
@Composable
private fun SettingsHeaderBar(
    isServer: Boolean,
    controllersOnline: Int,
    status: LinkStatus,
    onSync: (() -> Unit)?,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MosqueTheme.colors.scrim.copy(alpha = 0.6f))
            .padding(
                horizontal = if (isCompact) MosqueTheme.spacing.lg else MosqueTheme.spacing.xxl,
                vertical = MosqueTheme.spacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = MosqueTheme.colors.accent,
            modifier = Modifier.size(MosqueTheme.size.iconMd)
        )
        if (!isCompact) {
            Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
            Text(
                text = "MOSQUE ATHAN SETTINGS",
                color = MosqueTheme.colors.accent,
                fontSize = MosqueTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        StatusChip(
            label = if (isServer) Lang.get("Server", "Server") else Lang.get("Remote", "Remote"),
            dotColor = MosqueTheme.colors.accent
        )

        if (isServer) {
            Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
            StatusChip(
                label = if (controllersOnline == 0) {
                    Lang.get("No controllers", "Tiada pengendali")
                } else {
                    Lang.get("$controllersOnline connected", "$controllersOnline terhubung")
                },
                dotColor = if (controllersOnline == 0) {
                    MosqueTheme.colors.textMuted
                } else {
                    MosqueTheme.colors.success
                }
            )
        } else if (status != LinkStatus.Unknown) {
            Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
            StatusChip(
                label = when (status) {
                    LinkStatus.Connected -> Lang.get("Connected", "Terhubung")
                    LinkStatus.Connecting -> Lang.get("Connecting", "Menghubungkan")
                    LinkStatus.NeedsApproval -> Lang.get("Needs approval", "Perlu izin")
                    else -> Lang.get("Disconnected", "Terputus")
                },
                dotColor = when (status) {
                    LinkStatus.Connected -> MosqueTheme.colors.success
                    LinkStatus.NeedsApproval -> MosqueTheme.colors.accent
                    else -> MosqueTheme.colors.textMuted
                }
            )
        }

        // Never on a display: it is the source of truth and has nowhere to sync to. The host can
        // still pass a callback — the web build always does — so the decision belongs here.
        if (onSync != null && !isServer) {
            Spacer(modifier = Modifier.width(MosqueTheme.spacing.sm))
            MosqueIconButton(onClick = onSync) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = Lang.get("Sync", "Sinkron"),
                    tint = MosqueTheme.colors.textPrimary,
                    modifier = Modifier.size(MosqueTheme.size.iconMd)
                )
            }
        }
    }
}

/**
 * A dot and a label — the same vocabulary the connected-devices list already uses, deliberately
 * reused rather than reinvented, so "green dot means reachable" means one thing in this app.
 */
@Composable
private fun StatusChip(label: String, dotColor: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(MosqueTheme.spacing.sm)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(MosqueTheme.spacing.xs))
        Text(
            text = label,
            color = MosqueTheme.colors.textSecondary,
            fontSize = MosqueTheme.typography.captionSmall,
            fontWeight = FontWeight.Medium
        )
    }
}
