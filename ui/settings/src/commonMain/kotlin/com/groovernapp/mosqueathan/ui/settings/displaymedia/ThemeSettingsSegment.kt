@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.groovernapp.mosqueathan.ui.settings.displaymedia

import com.groovernapp.mosqueathan.ui.settings.StopPreviewButton
import com.groovernapp.mosqueathan.ui.design.isRunning
import com.groovernapp.mosqueathan.domain.PreviewKind
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.groovernapp.mosqueathan.domain.DashboardBackground
import com.groovernapp.mosqueathan.domain.Media
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.SlideshowSchedule
import com.groovernapp.mosqueathan.ui.component.Schedule
import com.groovernapp.mosqueathan.ui.component.SlideshowComponent
import com.groovernapp.mosqueathan.ui.settings.AdjustmentRow
import com.groovernapp.mosqueathan.ui.settings.RunningTextItem
import com.groovernapp.mosqueathan.ui.settings.RunningTextList
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.launch
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import com.groovernapp.mosqueathan.ui.design.component.MosquePrimaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueSecondaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueSelectableButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueIconButton
import com.groovernapp.mosqueathan.ui.design.focusHighlight
import com.groovernapp.mosqueathan.ui.design.MosqueRadius

@Composable
fun ThemeSettingsSegment(
    state: DisplayMediaUiState,
    onEvent: (DisplayMediaEvent) -> Unit
) {
    val isCompact = LocalWindowClass.current.isCompact
    val transitionType = state.slideshow.transitionType
    val slideIntervalSeconds = state.slideshow.intervalSeconds
    val videoWaitUntilEnd = state.slideshow.videoWaitUntilEnd
    val includeDefaultImage = state.slideshow.includeDefaultImage
    val mediaList = state.slideshow.media
    val slideshowSchedules = state.slideshow.schedules
    val dashboardBackgrounds = state.dashboard.backgrounds
    val announcements = state.announcements
    val onUpdateThemeSettings: (String, Int, Boolean, Boolean) -> Unit = { t, i2, v, d ->
        onEvent(DisplayMediaEvent.ThemeChanged(t, i2, v, d))
    }
    val onUpdateMediaItemUsed: (String, Boolean) -> Unit = { id, used ->
        onEvent(DisplayMediaEvent.MediaUsedToggled(id, used))
    }
    val onDeleteMediaItem: (Media) -> Unit = { onEvent(DisplayMediaEvent.MediaDeleted(it)) }
    val onReorderMediaItems: (List<String>) -> Unit = { onEvent(DisplayMediaEvent.MediaReordered(it)) }
    val onClearCustomBackground: () -> Unit = { onEvent(DisplayMediaEvent.CustomBackgroundCleared) }
    val onAddSlideshowSchedule: (String, Boolean, String, String) -> Unit = { d, sp, st, en ->
        onEvent(DisplayMediaEvent.ScheduleAdded(d, sp, st, en))
    }
    val onUpdateSlideshowScheduleTime: (com.groovernapp.mosqueathan.domain.ScheduleItem, String, String) -> Unit =
        { sc, st, en -> onEvent(DisplayMediaEvent.ScheduleTimeChanged(sc, st, en)) }
    val onUpdateSlideshowScheduleDays: (com.groovernapp.mosqueathan.domain.ScheduleItem, String) -> Unit =
        { sc, d -> onEvent(DisplayMediaEvent.ScheduleDaysChanged(sc, d)) }
    val onUpdateSlideshowScheduleDates: (com.groovernapp.mosqueathan.domain.ScheduleItem, String) -> Unit =
        { sc, d -> onEvent(DisplayMediaEvent.ScheduleDatesChanged(sc, d)) }
    val onDeleteSlideshowSchedule: (String) -> Unit = { onEvent(DisplayMediaEvent.ScheduleDeleted(it)) }
    val onPreviewSlideshow: () -> Unit = { onEvent(DisplayMediaEvent.SlideshowPreviewRequested) }
    val onDeleteDashboardBackground: (String) -> Unit = { onEvent(DisplayMediaEvent.BackgroundDeleted(it)) }
    val onUpdateDashboardBackgroundUsed: (String, Boolean) -> Unit = { id, used ->
        onEvent(DisplayMediaEvent.BackgroundUsedToggled(id, used))
    }
    val onUpdateDashboardBackgroundOrder: (List<String>) -> Unit =
        { onEvent(DisplayMediaEvent.BackgroundsReordered(it)) }
    val onUpdateDashboardSettings: (Boolean, String, Int) -> Unit = { r, t, i2 ->
        onEvent(DisplayMediaEvent.DashboardSettingsChanged(r, t, i2))
    }
    val onUpdateSlideshowEnabled: (Boolean) -> Unit =
        { onEvent(DisplayMediaEvent.SlideshowEnabledChanged(it)) }
    val onAddAnnouncement: (String) -> Unit = { onEvent(DisplayMediaEvent.AnnouncementAdded(it)) }
    val onUpdateAnnouncement: (String, String) -> Unit = { id, c ->
        onEvent(DisplayMediaEvent.AnnouncementEdited(id, c))
    }
    val onDeleteAnnouncement: (String) -> Unit = { onEvent(DisplayMediaEvent.AnnouncementDeleted(it)) }
    val onReorderAnnouncements: (List<String>) -> Unit =
        { onEvent(DisplayMediaEvent.AnnouncementsReordered(it)) }
    val onAddMediaItem: () -> Unit = { onEvent(DisplayMediaEvent.MediaPickRequested) }
    val onUpdateMediaItemPath: (String) -> Unit =
        { onEvent(DisplayMediaEvent.MediaFilePickRequested(it)) }
    val onSelectCustomBackground: () -> Unit =
        { onEvent(DisplayMediaEvent.CustomBackgroundPickRequested) }
    val onAddDashboardBackground: () -> Unit = { onEvent(DisplayMediaEvent.BackgroundPickRequested) }
    val onUpdateDashboardBackgroundPath: (String) -> Unit =
        { onEvent(DisplayMediaEvent.BackgroundFilePickRequested(it)) }
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
    ) {
        // TabRow header for Dashboard & Slideshow segments
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MosqueTheme.colors.transparent,
            contentColor = MosqueTheme.colors.accent,
            divider = {}
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Dashboard",
                            modifier = Modifier.size(MosqueTheme.size.iconMd)
                        )
                        Text(
                            text = Lang.get("Dashboard", "Dashboard"),
                            fontSize = MosqueTheme.typography.bodyCompact,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                selectedContentColor = MosqueTheme.colors.accent,
                unselectedContentColor = MosqueTheme.colors.textSecondary
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Slideshow",
                            modifier = Modifier.size(MosqueTheme.size.iconMd)
                        )
                        Text(
                            text = Lang.get("Slideshow", "Slideshow"),
                            fontSize = MosqueTheme.typography.bodyCompact,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                selectedContentColor = MosqueTheme.colors.accent,
                unselectedContentColor = MosqueTheme.colors.textSecondary
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            if (page == 0) {
                DashboardTabContent(state = state, onEvent = onEvent)
            } else {
                SlideshowTabContent(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
fun DashboardTabContent(
    state: DisplayMediaUiState,
    onEvent: (DisplayMediaEvent) -> Unit
) {
    val isCompact = LocalWindowClass.current.isCompact
    val dashboardBackgrounds = state.dashboard.backgrounds
    val announcements = state.announcements
    val mediaList = state.slideshow.media
    val slideshowSchedules = state.slideshow.schedules
    val transitionType = state.slideshow.transitionType
    val slideIntervalSeconds = state.slideshow.intervalSeconds
    val videoWaitUntilEnd = state.slideshow.videoWaitUntilEnd
    val includeDefaultImage = state.slideshow.includeDefaultImage
    val slideshowEnabled = state.slideshow.enabled
    val onUpdateThemeSettings: (String, Int, Boolean, Boolean) -> Unit = { t, i, v, d ->
        onEvent(DisplayMediaEvent.ThemeChanged(t, i, v, d))
    }
    val onAddMediaItem: () -> Unit = { onEvent(DisplayMediaEvent.MediaPickRequested) }
    val onUpdateMediaItemPath: (String) -> Unit = { onEvent(DisplayMediaEvent.MediaFilePickRequested(it)) }
    val onUpdateMediaItemUsed: (String, Boolean) -> Unit = { id, u -> onEvent(DisplayMediaEvent.MediaUsedToggled(id, u)) }
    val onDeleteMediaItem: (Media) -> Unit = { onEvent(DisplayMediaEvent.MediaDeleted(it)) }
    val onReorderMediaItems: (List<String>) -> Unit = { onEvent(DisplayMediaEvent.MediaReordered(it)) }
    val onSelectCustomBackground: () -> Unit = { onEvent(DisplayMediaEvent.CustomBackgroundPickRequested) }
    val onClearCustomBackground: () -> Unit = { onEvent(DisplayMediaEvent.CustomBackgroundCleared) }
    val onAddSlideshowSchedule: (String, Boolean, String, String) -> Unit = { d, sp, st, en ->
        onEvent(DisplayMediaEvent.ScheduleAdded(d, sp, st, en))
    }
    val onUpdateSlideshowScheduleTime: (com.groovernapp.mosqueathan.domain.ScheduleItem, String, String) -> Unit =
        { sc, st, en -> onEvent(DisplayMediaEvent.ScheduleTimeChanged(sc, st, en)) }
    val onUpdateSlideshowScheduleDays: (com.groovernapp.mosqueathan.domain.ScheduleItem, String) -> Unit =
        { sc, d -> onEvent(DisplayMediaEvent.ScheduleDaysChanged(sc, d)) }
    val onUpdateSlideshowScheduleDates: (com.groovernapp.mosqueathan.domain.ScheduleItem, String) -> Unit =
        { sc, d -> onEvent(DisplayMediaEvent.ScheduleDatesChanged(sc, d)) }
    val onDeleteSlideshowSchedule: (String) -> Unit = { onEvent(DisplayMediaEvent.ScheduleDeleted(it)) }
    val onPreviewSlideshow: () -> Unit = { onEvent(DisplayMediaEvent.SlideshowPreviewRequested) }
    val onUpdateSlideshowEnabled: (Boolean) -> Unit = { onEvent(DisplayMediaEvent.SlideshowEnabledChanged(it)) }
    val onAddDashboardBackground: () -> Unit = { onEvent(DisplayMediaEvent.BackgroundPickRequested) }
    val onUpdateDashboardBackgroundPath: (String) -> Unit = { onEvent(DisplayMediaEvent.BackgroundFilePickRequested(it)) }
    val onDeleteDashboardBackground: (String) -> Unit = { onEvent(DisplayMediaEvent.BackgroundDeleted(it)) }
    val onUpdateDashboardBackgroundUsed: (String, Boolean) -> Unit = { id, u -> onEvent(DisplayMediaEvent.BackgroundUsedToggled(id, u)) }
    val onUpdateDashboardBackgroundOrder: (List<String>) -> Unit = { onEvent(DisplayMediaEvent.BackgroundsReordered(it)) }
    val onUpdateDashboardSettings: (Boolean, String, Int) -> Unit = { r, t, i -> onEvent(DisplayMediaEvent.DashboardSettingsChanged(r, t, i)) }
    val onAddAnnouncement: (String) -> Unit = { onEvent(DisplayMediaEvent.AnnouncementAdded(it)) }
    val onUpdateAnnouncement: (String, String) -> Unit = { id, c -> onEvent(DisplayMediaEvent.AnnouncementEdited(id, c)) }
    val onDeleteAnnouncement: (String) -> Unit = { onEvent(DisplayMediaEvent.AnnouncementDeleted(it)) }
    val onReorderAnnouncements: (List<String>) -> Unit = { onEvent(DisplayMediaEvent.AnnouncementsReordered(it)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
    ) {
        // Row for Randomize Switch (Aligned Top-Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                // weight, so the heading yields space instead of colliding with the
                // switch. Without it SpaceBetween hands the Text its full intrinsic
                // width and the two run together as "ListRandomize" on a phone.
                modifier = Modifier.weight(1f),
                text = Lang.get("Dashboard Background List", "Daftar Latar Belakang Dashboard"),
                color = MosqueTheme.colors.textSecondary,
                fontSize = MosqueTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
            ) {
                Text(
                    text = Lang.get("Randomize", "Tampilkan random"),
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.label
                )
                Switch(
                    modifier = Modifier.scale(0.8f).focusHighlight(shape = RoundedCornerShape(MosqueRadius.pill), scaleUp = false),
                    checked = state.dashboard.randomize,
                    onCheckedChange = { checked ->
                        onUpdateDashboardSettings(
                            checked,
                            state.dashboard.transitionType,
                            state.dashboard.transitionIntervalSeconds
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MosqueTheme.colors.accent,
                        checkedTrackColor = MosqueTheme.colors.accent.copy(alpha = 0.5f)
                    )
                )
            }
        }

        val (displayValue, unitIndex) = remember(state.dashboard.transitionIntervalSeconds) {
            val interval = state.dashboard.transitionIntervalSeconds
            when {
                interval % 86400 == 0 -> Pair(interval / 86400, 3) // Hari
                interval % 3600 == 0 -> Pair(interval / 3600, 2)  // Jam
                interval % 60 == 0 -> Pair(interval / 60, 1)    // Menit
                else -> Pair(interval, 0)                      // Detik
            }
        }

        val multipliers = listOf(1, 60, 3600, 86400)

        SlideshowComponent(
            items = dashboardBackgrounds,
            keySelector = { it.id },
            filePathSelector = { it.filePath },
            isUsedSelector = { it.isUsed },
            onUpdateUseStatus = { item, isUsed -> onUpdateDashboardBackgroundUsed(item.id, isUsed) },
            showAddButton = true,
            addText = Lang.get("Add Dashboard Background", "Tambah Latar Belakang Dashboard"),
            onAddClicked = onAddDashboardBackground,
            onReorderItems = onUpdateDashboardBackgroundOrder,
            onEditItem = { onUpdateDashboardBackgroundPath(it.id) },
            onDeleteItem = { onDeleteDashboardBackground(it.id) },
            emptyText = Lang.get("Using Default Background", "Menggunakan Latar Belakang Default"),
            showTransitionAnimation = true,
            transitionType = state.dashboard.transitionType,
            onTransitionTypeChange = { type ->
                onUpdateDashboardSettings(
                    state.dashboard.randomize,
                    type,
                    state.dashboard.transitionIntervalSeconds
                )
            },
            showTransitionInterval = true,
            transitionIntervalValue = displayValue,
            transitionIntervalUnitIndex = unitIndex,
            onTransitionIntervalValueChange = { newVal ->
                onUpdateDashboardSettings(
                    state.dashboard.randomize,
                    state.dashboard.transitionType,
                    newVal * multipliers[unitIndex]
                )
            },
            onTransitionIntervalUnitChange = { index ->
                val clampedVal = displayValue.coerceIn(1, when (index) {
                    0 -> 60
                    1 -> 60
                    2 -> 24
                    3 -> 30
                    else -> 60
                })
                onUpdateDashboardSettings(
                    state.dashboard.randomize,
                    state.dashboard.transitionType,
                    clampedVal * multipliers[index]
                )
            },
            isCompact = isCompact
        )

        // Running Text Management
        Spacer(modifier = Modifier.height(MosqueTheme.spacing.md))
        RunningTextSettingsCard(
            announcements = announcements,
            onAdd = onAddAnnouncement,
            onEdit = onUpdateAnnouncement,
            onDelete = onDeleteAnnouncement,
            onReorder = onReorderAnnouncements
        )
    }
}

@Composable
fun SlideshowTabContent(
    state: DisplayMediaUiState,
    onEvent: (DisplayMediaEvent) -> Unit
) {
    val isCompact = LocalWindowClass.current.isCompact
    val dashboardBackgrounds = state.dashboard.backgrounds
    val announcements = state.announcements
    val mediaList = state.slideshow.media
    val slideshowSchedules = state.slideshow.schedules
    val transitionType = state.slideshow.transitionType
    val slideIntervalSeconds = state.slideshow.intervalSeconds
    val videoWaitUntilEnd = state.slideshow.videoWaitUntilEnd
    val includeDefaultImage = state.slideshow.includeDefaultImage
    val slideshowEnabled = state.slideshow.enabled
    val onUpdateThemeSettings: (String, Int, Boolean, Boolean) -> Unit = { t, i, v, d ->
        onEvent(DisplayMediaEvent.ThemeChanged(t, i, v, d))
    }
    val onAddMediaItem: () -> Unit = { onEvent(DisplayMediaEvent.MediaPickRequested) }
    val onUpdateMediaItemPath: (String) -> Unit = { onEvent(DisplayMediaEvent.MediaFilePickRequested(it)) }
    val onUpdateMediaItemUsed: (String, Boolean) -> Unit = { id, u -> onEvent(DisplayMediaEvent.MediaUsedToggled(id, u)) }
    val onDeleteMediaItem: (Media) -> Unit = { onEvent(DisplayMediaEvent.MediaDeleted(it)) }
    val onReorderMediaItems: (List<String>) -> Unit = { onEvent(DisplayMediaEvent.MediaReordered(it)) }
    val onSelectCustomBackground: () -> Unit = { onEvent(DisplayMediaEvent.CustomBackgroundPickRequested) }
    val onClearCustomBackground: () -> Unit = { onEvent(DisplayMediaEvent.CustomBackgroundCleared) }
    val onAddSlideshowSchedule: (String, Boolean, String, String) -> Unit = { d, sp, st, en ->
        onEvent(DisplayMediaEvent.ScheduleAdded(d, sp, st, en))
    }
    val onUpdateSlideshowScheduleTime: (com.groovernapp.mosqueathan.domain.ScheduleItem, String, String) -> Unit =
        { sc, st, en -> onEvent(DisplayMediaEvent.ScheduleTimeChanged(sc, st, en)) }
    val onUpdateSlideshowScheduleDays: (com.groovernapp.mosqueathan.domain.ScheduleItem, String) -> Unit =
        { sc, d -> onEvent(DisplayMediaEvent.ScheduleDaysChanged(sc, d)) }
    val onUpdateSlideshowScheduleDates: (com.groovernapp.mosqueathan.domain.ScheduleItem, String) -> Unit =
        { sc, d -> onEvent(DisplayMediaEvent.ScheduleDatesChanged(sc, d)) }
    val onDeleteSlideshowSchedule: (String) -> Unit = { onEvent(DisplayMediaEvent.ScheduleDeleted(it)) }
    val onPreviewSlideshow: () -> Unit = { onEvent(DisplayMediaEvent.SlideshowPreviewRequested) }
    val onUpdateSlideshowEnabled: (Boolean) -> Unit = { onEvent(DisplayMediaEvent.SlideshowEnabledChanged(it)) }
    val onAddDashboardBackground: () -> Unit = { onEvent(DisplayMediaEvent.BackgroundPickRequested) }
    val onUpdateDashboardBackgroundPath: (String) -> Unit = { onEvent(DisplayMediaEvent.BackgroundFilePickRequested(it)) }
    val onDeleteDashboardBackground: (String) -> Unit = { onEvent(DisplayMediaEvent.BackgroundDeleted(it)) }
    val onUpdateDashboardBackgroundUsed: (String, Boolean) -> Unit = { id, u -> onEvent(DisplayMediaEvent.BackgroundUsedToggled(id, u)) }
    val onUpdateDashboardBackgroundOrder: (List<String>) -> Unit = { onEvent(DisplayMediaEvent.BackgroundsReordered(it)) }
    val onUpdateDashboardSettings: (Boolean, String, Int) -> Unit = { r, t, i -> onEvent(DisplayMediaEvent.DashboardSettingsChanged(r, t, i)) }
    val onAddAnnouncement: (String) -> Unit = { onEvent(DisplayMediaEvent.AnnouncementAdded(it)) }
    val onUpdateAnnouncement: (String, String) -> Unit = { id, c -> onEvent(DisplayMediaEvent.AnnouncementEdited(id, c)) }
    val onDeleteAnnouncement: (String) -> Unit = { onEvent(DisplayMediaEvent.AnnouncementDeleted(it)) }
    val onReorderAnnouncements: (List<String>) -> Unit = { onEvent(DisplayMediaEvent.AnnouncementsReordered(it)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
    ) {
        Text(
            text = Lang.get("Slideshow Media Items", "Daftar Media Slideshow"),
            color = MosqueTheme.colors.textSecondary,
            fontSize = MosqueTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )

        val (displayValue, unitIndex) = remember(slideIntervalSeconds) {
            val interval = slideIntervalSeconds
            when {
                interval % 86400 == 0 -> Pair(interval / 86400, 3) // Hari
                interval % 3600 == 0 -> Pair(interval / 3600, 2)  // Jam
                interval % 60 == 0 -> Pair(interval / 60, 1)    // Menit
                else -> Pair(interval, 0)                      // Detik
            }
        }

        val multipliers = listOf(1, 60, 3600, 86400)

        SlideshowComponent(
            items = mediaList,
            keySelector = { it.id },
            filePathSelector = { it.filePath },
            isVideoSelector = { it.isVideo },
            isUsedSelector = { it.isUsed },
            onUpdateUseStatus = { item, isUsed -> onUpdateMediaItemUsed(item.id, isUsed) },
            showAddButton = true,
            addText = Lang.get("Add Image / Video to Slideshow", "Tambah Gambar / Video ke Slideshow"),
            onAddClicked = onAddMediaItem,
            onReorderItems = onReorderMediaItems,
            onEditItem = { onUpdateMediaItemPath(it.id) },
            onDeleteItem = { onDeleteMediaItem(it) },
            emptyText = Lang.get("No media added. Slideshow is empty.", "Belum ada media. Slideshow kosong."),
            showTransitionAnimation = true,
            transitionType = transitionType,
            onTransitionTypeChange = { type ->
                onUpdateThemeSettings(
                    type,
                    slideIntervalSeconds,
                    videoWaitUntilEnd,
                    includeDefaultImage
                )
            },
            showTransitionInterval = true,
            transitionIntervalValue = displayValue,
            transitionIntervalUnitIndex = unitIndex,
            onTransitionIntervalValueChange = { newVal ->
                onUpdateThemeSettings(
                    transitionType,
                    newVal * multipliers[unitIndex],
                    videoWaitUntilEnd,
                    includeDefaultImage
                )
            },
            onTransitionIntervalUnitChange = { index ->
                val clampedVal = displayValue.coerceIn(1, when (index) {
                    0 -> 60
                    1 -> 60
                    2 -> 24
                    3 -> 30
                    else -> 60
                })
                onUpdateThemeSettings(
                    transitionType,
                    clampedVal * multipliers[index],
                    videoWaitUntilEnd,
                    includeDefaultImage
                )
            },
            isCompact = isCompact
        )

        // Slideshow Schedule section with enable/disable toggle
        Spacer(modifier = Modifier.height(MosqueTheme.spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = Lang.get("Slide Show Schedule", "Jadwal Slide Show"),
                color = MosqueTheme.colors.textPrimary,
                fontSize = MosqueTheme.typography.bodyCompact,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Switch(
                modifier = Modifier.focusHighlight(shape = RoundedCornerShape(MosqueRadius.pill), scaleUp = false),
                checked = slideshowEnabled,
                onCheckedChange = { checked ->
                    // If turning ON and list is empty — allowed, just set enabled
                    // If turning OFF — allowed
                    // Auto-logic handled externally (onAdd auto-enables; onDelete auto-disables if empty)
                    if (!checked || slideshowSchedules.isNotEmpty()) {
                        onUpdateSlideshowEnabled(checked)
                    } else {
                        // Enabling while empty — still allow user to do so
                        onUpdateSlideshowEnabled(checked)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MosqueTheme.colors.onAccent,
                    checkedTrackColor = MosqueTheme.colors.accent,
                    uncheckedThumbColor = MosqueTheme.colors.textSecondary,
                    uncheckedTrackColor = MosqueTheme.colors.border
                )
            )
        }
        Schedule(
            title = "",
            schedules = slideshowSchedules,
            addButtonText = Lang.get("Add Schedule", "Tambah Jadwal"),
            scheduleListButtonText = Lang.get("Slideshow Schedules", "Jadwal Slideshow"),
            onAddSchedule = {
                val wasEmpty = slideshowSchedules.isEmpty()
                onAddSlideshowSchedule("Monday", false, "08:00", "17:00")
                if (wasEmpty) {
                    // Auto-enable when first schedule is added
                    onUpdateSlideshowEnabled(true)
                }
            },
            onDeleteSchedule = { schedule ->
                onDeleteSlideshowSchedule(schedule.id)
                // Auto-disable if list will become empty
                if (slideshowSchedules.size <= 1) {
                    onUpdateSlideshowEnabled(false)
                }
            },
            onUpdateScheduleTime = { schedule, start, end ->
                onUpdateSlideshowScheduleTime(schedule, start, end)
            },
            onUpdateScheduleDays = { schedule, days ->
                onUpdateSlideshowScheduleDays(schedule, days)
            },
            onUpdateScheduleDates = { schedule, dates ->
                onUpdateSlideshowScheduleDates(schedule, dates)
            }
        )

        // Preview Slideshow button
        Spacer(modifier = Modifier.height(MosqueTheme.spacing.md))
        if (PreviewKind.Slideshow.isRunning) {
            StopPreviewButton(onClick = onPreviewSlideshow, modifier = Modifier.fillMaxWidth())
        } else MosqueSecondaryButton(
            onClick = onPreviewSlideshow,
            shape = RoundedCornerShape(MosqueTheme.radius.md),
            modifier = Modifier.fillMaxWidth().heightIn(min = MosqueTheme.size.controlLg)
        ) {
            Text(
                text = Lang.get("Preview Slideshow", "Preview Slideshow"),
                color = MosqueTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = MosqueTheme.typography.bodyCompact
            )
        }

        // Include Default Image switch (Placed at the very bottom, default off)
        Spacer(modifier = Modifier.height(MosqueTheme.spacing.xl))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MosqueTheme.radius.md))
                .background(MosqueTheme.colors.fillSubtle)
                .padding(MosqueTheme.spacing.xl),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Lang.get(
                        "Include Default Image (Masjidil Haram)",
                        "Sertakan Gambar Default (Masjidil Haram)"
                    ),
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.bodyCompact,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(MosqueTheme.spacing.xxs))
                Text(
                    text = Lang.get(
                        "Include or exclude the default image in the slideshow rotation",
                        "Sertakan atau kecualikan gambar default dalam rotasi slideshow"
                    ),
                    color = MosqueTheme.colors.textSecondary,
                    fontSize = MosqueTheme.typography.caption
                )
            }
            Spacer(modifier = Modifier.width(MosqueTheme.spacing.xl))
            Switch(
                modifier = Modifier.focusHighlight(shape = RoundedCornerShape(MosqueRadius.pill), scaleUp = false),
                checked = includeDefaultImage,
                onCheckedChange = { checked ->
                    onUpdateThemeSettings(
                        transitionType,
                        slideIntervalSeconds,
                        videoWaitUntilEnd,
                        checked
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MosqueTheme.colors.accent,
                    checkedTrackColor = MosqueTheme.colors.accent.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun TransitionAnimationRow(
    title: String,
    selectedType: String,
    isCompact: Boolean,
    onTypeSelected: (String) -> Unit
) {
    if (isCompact) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MosqueTheme.radius.md))
                .background(MosqueTheme.colors.fillSubtle)
                .padding(MosqueTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = MosqueTheme.colors.textPrimary,
                fontSize = MosqueTheme.typography.label,
                fontWeight = FontWeight.SemiBold
            )
            // FlowRow, not Row: the four labels do not fit a phone's width and the
            // last one was clipped to "INSTAN". Wrapping keeps every option readable
            // at any width, and on a television it still lays out as one line.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.sm)
            ) {
                listOf("FADE", "SLIDE", "ZOOM", "INSTANT").forEach { type ->
                    val isSelected = selectedType == type
                    MosqueSelectableButton(
                        onClick = { onTypeSelected(type) },
                        selected = isSelected,
                        shape = RoundedCornerShape(MosqueTheme.radius.sm),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = MosqueTheme.spacing.sm),
                        modifier = Modifier.heightIn(min = MosqueTheme.size.controlSm)
                    ) {
                        Text(
                            type,
                            color = if (isSelected) MosqueTheme.colors.onAccent else MosqueTheme.colors.textPrimary,
                            fontSize = MosqueTheme.typography.caption,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MosqueTheme.radius.md))
                .background(MosqueTheme.colors.fillSubtle)
                .padding(MosqueTheme.spacing.xl),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = MosqueTheme.colors.textPrimary,
                fontSize = MosqueTheme.typography.bodyCompact,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)) {
                listOf("FADE", "SLIDE", "ZOOM", "INSTANT").forEach { type ->
                    val isSelected = selectedType == type
                    MosqueSelectableButton(
                        onClick = { onTypeSelected(type) },
                        selected = isSelected,
                        shape = RoundedCornerShape(MosqueTheme.radius.sm),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = MosqueTheme.spacing.sm),
                        modifier = Modifier.heightIn(min = MosqueTheme.size.controlSm)
                    ) {
                        Text(
                            type,
                            color = if (isSelected) MosqueTheme.colors.onAccent else MosqueTheme.colors.textPrimary,
                            fontSize = MosqueTheme.typography.caption,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * "Transition interval" for a setting stored as a number of seconds.
 *
 * The Dashboard and Slideshow tabs both do this conversion inline — the stored seconds are shown as
 * the largest unit that divides them evenly, and changing the unit rescales the value — and the
 * Friday slideshow needs the same control. It stores seconds too, which is what the display reads.
 * (Its separate seconds/minutes selector never converted anything: picking "Minutes" with 5 left the
 * display on 5 seconds.)
 */
@Composable
fun TransitionIntervalRow(
    title: String,
    seconds: Int,
    onSecondsChange: (Int) -> Unit
) {
    val (displayValue, unitIndex) = remember(seconds) {
        when {
            seconds > 0 && seconds % 86400 == 0 -> Pair(seconds / 86400, 3) // days
            seconds > 0 && seconds % 3600 == 0 -> Pair(seconds / 3600, 2)   // hours
            seconds > 0 && seconds % 60 == 0 -> Pair(seconds / 60, 1)       // minutes
            else -> Pair(seconds, 0)                                        // seconds
        }
    }
    val multipliers = listOf(1, 60, 3600, 86400)
    val maxPerUnit = listOf(60, 60, 24, 30)

    DropdownAdjustmentRow(
        title = title,
        value = displayValue,
        minValue = 1,
        maxValue = maxPerUnit[unitIndex],
        unitLabels = listOf(
            Lang.get("seconds", "detik"),
            Lang.get("minutes", "menit"),
            Lang.get("hours", "jam"),
            Lang.get("days", "hari")
        ),
        selectedUnitIndex = unitIndex,
        onValueChange = { onSecondsChange(it * multipliers[unitIndex]) },
        onUnitChange = { index ->
            onSecondsChange(displayValue.coerceIn(1, maxPerUnit[index]) * multipliers[index])
        }
    )
}

@Composable
fun DropdownAdjustmentRow(
    title: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
    unitLabels: List<String>,
    selectedUnitIndex: Int,
    onValueChange: (Int) -> Unit,
    onUnitChange: (Int) -> Unit
) {
    val containerSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val isCompact = LocalWindowClass.current.isCompact
    var expanded by remember { mutableStateOf(false) }

    if (isCompact) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MosqueTheme.radius.md))
                .background(MosqueTheme.colors.fillSubtle)
                .padding(horizontal = MosqueTheme.spacing.xl, vertical = MosqueTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
        ) {
            Text(
                text = title,
                color = MosqueTheme.colors.textPrimary,
                fontSize = MosqueTheme.typography.label,
                fontWeight = FontWeight.SemiBold
            )
     
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg)
            ) {
                MosqueSecondaryButton(
                    onClick = { if (value > minValue) onValueChange(value - 1) },
                    shape = RoundedCornerShape(MosqueTheme.radius.sm),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(MosqueTheme.size.controlMd)
                ) {
                    Text("-", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
     
                Text(
                    text = value.toString(),
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.bodyCompact,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(MosqueTheme.size.controlMd),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
     
                MosqueSecondaryButton(
                    onClick = { if (value < maxValue) onValueChange(value + 1) },
                    shape = RoundedCornerShape(MosqueTheme.radius.sm),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(MosqueTheme.size.controlMd)
                ) {
                    Text("+", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                Box {
                    MosqueSecondaryButton(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(MosqueTheme.radius.sm),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = MosqueTheme.spacing.sm),
                        modifier = Modifier.heightIn(min = MosqueTheme.size.controlSm)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.sm)
                        ) {
                            Text(
                                text = unitLabels[selectedUnitIndex],
                                color = MosqueTheme.colors.textPrimary,
                                fontSize = MosqueTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = MosqueTheme.colors.textPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MosqueTheme.colors.surfaceRaised)
                    ) {
                        unitLabels.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                modifier = Modifier.focusHighlight(shape = RoundedCornerShape(MosqueRadius.sm), scaleUp = false),
                                text = { 
                                    Text(
                                        text = label, 
                                        color = if (selectedUnitIndex == index) MosqueTheme.colors.accent else MosqueTheme.colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    ) 
                                },
                                onClick = {
                                    onUnitChange(index)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MosqueTheme.radius.md))
                .background(MosqueTheme.colors.fillSubtle)
                .padding(horizontal = MosqueTheme.spacing.xxl, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = MosqueTheme.colors.textPrimary,
                fontSize = MosqueTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
     
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
            ) {
                MosqueSecondaryButton(
                    onClick = { if (value > minValue) onValueChange(value - 1) },
                    shape = RoundedCornerShape(MosqueTheme.radius.sm),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(MosqueTheme.size.controlMd)
                ) {
                    Text("-", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
     
                Text(
                    text = value.toString(),
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(MosqueTheme.size.controlMd),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
     
                MosqueSecondaryButton(
                    onClick = { if (value < maxValue) onValueChange(value + 1) },
                    shape = RoundedCornerShape(MosqueTheme.radius.sm),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(MosqueTheme.size.controlMd)
                ) {
                    Text("+", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                Box {
                    MosqueSecondaryButton(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(MosqueTheme.radius.sm),
                        contentPadding = PaddingValues(horizontal = MosqueTheme.spacing.lg, vertical = MosqueTheme.spacing.sm),
                        modifier = Modifier.heightIn(min = MosqueTheme.size.controlMd)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.sm)
                        ) {
                            Text(
                                text = unitLabels[selectedUnitIndex],
                                color = MosqueTheme.colors.textPrimary,
                                fontSize = MosqueTheme.typography.bodyCompact,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = MosqueTheme.colors.textPrimary,
                                modifier = Modifier.size(MosqueTheme.size.iconSm)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MosqueTheme.colors.surfaceRaised)
                    ) {
                        unitLabels.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                modifier = Modifier.focusHighlight(shape = RoundedCornerShape(MosqueRadius.sm), scaleUp = false),
                                text = { 
                                    Text(
                                        text = label, 
                                        color = if (selectedUnitIndex == index) MosqueTheme.colors.accent else MosqueTheme.colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    ) 
                                },
                                onClick = {
                                    onUnitChange(index)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}



@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 1280, heightDp = 720)
@Composable
fun ThemeSettingsSegmentPreview() {
    MosqueTheme {
        ThemeSettingsSegment(
            state = DisplayMediaUiState(
                slideshow = SlideshowSettings(transitionType = "FADE", intervalSeconds = 8)
            ),
            onEvent = {}
        )
    }
}

/**
 * The Dashboard's running text: the card around the shared [RunningTextList].
 *
 * This used to be its own copy of the list, with inline editing, a delete that happened on the
 * first press and no way to reorder at all. The Adzan and Iqomah footers had all three; they now
 * share one list so the Dashboard cannot drift from them again.
 */
@Composable
fun RunningTextSettingsCard(
    announcements: List<com.groovernapp.mosqueathan.domain.Announcement>,
    onAdd: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onReorder: (List<String>) -> Unit
) {
    val rows = remember(announcements) {
        announcements
            .sortedBy { it.position }
            .map { RunningTextItem(id = it.id, text = it.content) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MosqueTheme.radius.lg))
            .background(MosqueTheme.colors.fillSubtle)
            .padding(MosqueTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xs)) {
            Text(
                text = Lang.get("Manage Running Text", "Kelola Running Text"),
                color = MosqueTheme.colors.textPrimary,
                fontSize = MosqueTheme.typography.body,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = Lang.get(
                    "Add running text messages that will scroll periodically at the bottom of the main dashboard screen.",
                    "Tambahkan pesan running text yang akan bergulir secara berkala di bagian bawah dashboard layar utama masjid."
                ),
                color = MosqueTheme.colors.textSecondary,
                fontSize = MosqueTheme.typography.caption
            )
        }

        RunningTextList(
            items = rows,
            placeholderText = Lang.get("Enter new running text...", "Masukkan teks running text baru..."),
            onAdd = onAdd,
            onEdit = onEdit,
            onDelete = onDelete,
            onReorder = onReorder
        )
    }
}
