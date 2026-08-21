package com.groovernapp.mosqueathan.ui.settings.displaymedia

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.groovernapp.mosqueathan.R
import com.groovernapp.mosqueathan.data.DashboardBackgroundEntity
import com.groovernapp.mosqueathan.data.MediaEntity
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.data.SlideshowScheduleEntity
import com.groovernapp.mosqueathan.ui.component.Schedule
import com.groovernapp.mosqueathan.ui.component.SlideshowComponent
import com.groovernapp.mosqueathan.ui.settings.TvFocusableButton
import com.groovernapp.mosqueathan.ui.settings.TvFocusablePrimaryButton
import com.groovernapp.mosqueathan.ui.settings.AdjustmentRow
import com.groovernapp.mosqueathan.ui.theme.MosqueAthanTheme
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.launch
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun ThemeSettingsSegment(
    config: MosqueConfigEntity,
    transitionType: String,
    slideIntervalSeconds: Int,
    videoWaitUntilEnd: Boolean,
    includeDefaultImage: Boolean,
    mediaList: List<MediaEntity>,
    slideshowSchedules: List<SlideshowScheduleEntity>,
    dashboardBackgrounds: List<DashboardBackgroundEntity>,
    announcements: List<com.groovernapp.mosqueathan.data.AnnouncementEntity>,
    onUpdateThemeSettings: (String, Int, Boolean, Boolean) -> Unit,
    onAddMediaItem: () -> Unit,
    onUpdateMediaItemPath: (String) -> Unit,
    onUpdateMediaItemUsed: (String, Boolean) -> Unit,
    onDeleteMediaItem: (MediaEntity) -> Unit,
    onReorderMediaItems: (List<String>) -> Unit,
    onSelectCustomBackground: () -> Unit,
    onClearCustomBackground: () -> Unit,
    onAddSlideshowSchedule: (String, Boolean, String, String) -> Unit,
    onUpdateSlideshowScheduleTime: (com.groovernapp.mosqueathan.data.ScheduleItem, String, String) -> Unit,
    onUpdateSlideshowScheduleDays: (com.groovernapp.mosqueathan.data.ScheduleItem, String) -> Unit,
    onUpdateSlideshowScheduleDates: (com.groovernapp.mosqueathan.data.ScheduleItem, String) -> Unit,
    onDeleteSlideshowSchedule: (String) -> Unit,
    onPreviewSlideshow: () -> Unit,
    onAddDashboardBackground: () -> Unit,
    onUpdateDashboardBackgroundPath: (String) -> Unit,
    onDeleteDashboardBackground: (String) -> Unit,
    onUpdateDashboardBackgroundUsed: (String, Boolean) -> Unit,
    onUpdateDashboardBackgroundOrder: (List<String>) -> Unit,
    onUpdateDashboardSettings: (Boolean, String, Int) -> Unit,
    onUpdateSlideshowEnabled: (Boolean) -> Unit,
    onAddAnnouncement: (String) -> Unit,
    onUpdateAnnouncement: (String, String) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onReorderAnnouncements: (List<String>) -> Unit,
    isCompact: Boolean
) {
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
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = Lang.get("Dashboard", "Dashboard"),
                            fontSize = MosqueTheme.typography.bodyCompact,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                selectedContentColor = MosqueTheme.colors.accent,
                unselectedContentColor = MosqueTheme.colors.textTertiary
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
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = Lang.get("Slideshow", "Slideshow"),
                            fontSize = MosqueTheme.typography.bodyCompact,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                selectedContentColor = MosqueTheme.colors.accent,
                unselectedContentColor = MosqueTheme.colors.textTertiary
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            if (page == 0) {
                DashboardTabContent(
                    config = config,
                    dashboardBackgrounds = dashboardBackgrounds,
                    announcements = announcements,
                    onAddDashboardBackground = onAddDashboardBackground,
                    onUpdateDashboardBackgroundPath = onUpdateDashboardBackgroundPath,
                    onDeleteDashboardBackground = onDeleteDashboardBackground,
                    onUpdateDashboardBackgroundUsed = onUpdateDashboardBackgroundUsed,
                    onUpdateDashboardBackgroundOrder = onUpdateDashboardBackgroundOrder,
                    onUpdateDashboardSettings = onUpdateDashboardSettings,
                    onAddAnnouncement = onAddAnnouncement,
                    onUpdateAnnouncement = onUpdateAnnouncement,
                    onDeleteAnnouncement = onDeleteAnnouncement,
                    onReorderAnnouncements = onReorderAnnouncements,
                    isCompact = isCompact
                )
            } else {
                SlideshowTabContent(
                    transitionType = transitionType,
                    slideIntervalSeconds = slideIntervalSeconds,
                    videoWaitUntilEnd = videoWaitUntilEnd,
                    includeDefaultImage = includeDefaultImage,
                    mediaList = mediaList,
                    slideshowSchedules = slideshowSchedules,
                    slideshowEnabled = config.slideshowEnabled,
                    onUpdateThemeSettings = onUpdateThemeSettings,
                    onAddMediaItem = onAddMediaItem,
                    onUpdateMediaItemPath = onUpdateMediaItemPath,
                    onUpdateMediaItemUsed = onUpdateMediaItemUsed,
                    onDeleteMediaItem = onDeleteMediaItem,
                    onReorderMediaItems = onReorderMediaItems,
                    onAddSlideshowSchedule = onAddSlideshowSchedule,
                    onUpdateSlideshowScheduleTime = onUpdateSlideshowScheduleTime,
                    onUpdateSlideshowScheduleDays = onUpdateSlideshowScheduleDays,
                    onUpdateSlideshowScheduleDates = onUpdateSlideshowScheduleDates,
                    onDeleteSlideshowSchedule = onDeleteSlideshowSchedule,
                    onUpdateSlideshowEnabled = onUpdateSlideshowEnabled,
                    onPreviewSlideshow = onPreviewSlideshow,
                    isCompact = isCompact
                )
            }
        }
    }
}

@Composable
fun DashboardTabContent(
    config: MosqueConfigEntity,
    dashboardBackgrounds: List<DashboardBackgroundEntity>,
    announcements: List<com.groovernapp.mosqueathan.data.AnnouncementEntity>,
    onAddDashboardBackground: () -> Unit,
    onUpdateDashboardBackgroundPath: (String) -> Unit,
    onDeleteDashboardBackground: (String) -> Unit,
    onUpdateDashboardBackgroundUsed: (String, Boolean) -> Unit,
    onUpdateDashboardBackgroundOrder: (List<String>) -> Unit,
    onUpdateDashboardSettings: (Boolean, String, Int) -> Unit,
    onAddAnnouncement: (String) -> Unit,
    onUpdateAnnouncement: (String, String) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onReorderAnnouncements: (List<String>) -> Unit,
    isCompact: Boolean
) {
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
                text = Lang.get("Dashboard Background List", "Daftar Latar Belakang Dashboard"),
                color = MosqueTheme.colors.textTertiary,
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
                    modifier = Modifier.scale(0.8f),
                    checked = config.dashboardRandomize,
                    onCheckedChange = { checked ->
                        onUpdateDashboardSettings(
                            checked,
                            config.dashboardTransitionType,
                            config.dashboardTransitionInterval
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MosqueTheme.colors.accent,
                        checkedTrackColor = MosqueTheme.colors.accent.copy(alpha = 0.5f)
                    )
                )
            }
        }

        val (displayValue, unitIndex) = remember(config.dashboardTransitionInterval) {
            val interval = config.dashboardTransitionInterval
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
            transitionType = config.dashboardTransitionType,
            onTransitionTypeChange = { type ->
                onUpdateDashboardSettings(
                    config.dashboardRandomize,
                    type,
                    config.dashboardTransitionInterval
                )
            },
            showTransitionInterval = true,
            transitionIntervalValue = displayValue,
            transitionIntervalUnitIndex = unitIndex,
            onTransitionIntervalValueChange = { newVal ->
                onUpdateDashboardSettings(
                    config.dashboardRandomize,
                    config.dashboardTransitionType,
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
                    config.dashboardRandomize,
                    config.dashboardTransitionType,
                    clampedVal * multipliers[index]
                )
            },
            isCompact = isCompact
        )

        // Running Text Management
        Spacer(modifier = Modifier.height(8.dp))
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
    transitionType: String,
    slideIntervalSeconds: Int,
    videoWaitUntilEnd: Boolean,
    includeDefaultImage: Boolean,
    mediaList: List<MediaEntity>,
    slideshowSchedules: List<SlideshowScheduleEntity>,
    slideshowEnabled: Boolean,
    onUpdateThemeSettings: (String, Int, Boolean, Boolean) -> Unit,
    onAddMediaItem: () -> Unit,
    onUpdateMediaItemPath: (String) -> Unit,
    onUpdateMediaItemUsed: (String, Boolean) -> Unit,
    onDeleteMediaItem: (MediaEntity) -> Unit,
    onReorderMediaItems: (List<String>) -> Unit,
    onAddSlideshowSchedule: (String, Boolean, String, String) -> Unit,
    onUpdateSlideshowScheduleTime: (com.groovernapp.mosqueathan.data.ScheduleItem, String, String) -> Unit,
    onUpdateSlideshowScheduleDays: (com.groovernapp.mosqueathan.data.ScheduleItem, String) -> Unit,
    onUpdateSlideshowScheduleDates: (com.groovernapp.mosqueathan.data.ScheduleItem, String) -> Unit,
    onDeleteSlideshowSchedule: (String) -> Unit,
    onUpdateSlideshowEnabled: (Boolean) -> Unit,
    onPreviewSlideshow: () -> Unit,
    isCompact: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
    ) {
        Text(
            text = Lang.get("Slideshow Media Items", "Daftar Media Slideshow"),
            color = MosqueTheme.colors.textTertiary,
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
        Spacer(modifier = Modifier.height(8.dp))
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
            scheduleListButtonText = Lang.get("Slide show Schedules", "Jadwal Slide Show"),
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
        Spacer(modifier = Modifier.height(8.dp))
        TvFocusableButton(
            onClick = onPreviewSlideshow,
            shape = RoundedCornerShape(MosqueTheme.radius.md),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                text = Lang.get("Preview Slideshow", "Preview Slideshow"),
                color = MosqueTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = MosqueTheme.typography.bodyCompact
            )
        }

        // Include Default Image switch (Placed at the very bottom, default off)
        Spacer(modifier = Modifier.height(16.dp))
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Lang.get(
                        "Include or exclude the default image in the slideshow rotation",
                        "Sertakan atau kecualikan gambar default dalam rotasi slideshow"
                    ),
                    color = MosqueTheme.colors.textTertiary,
                    fontSize = MosqueTheme.typography.caption
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("FADE", "SLIDE", "ZOOM", "INSTANT").forEach { type ->
                    val isSelected = selectedType == type
                    TvFocusableButton(
                        onClick = { onTypeSelected(type) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MosqueTheme.colors.accent else MosqueTheme.colors.fill
                        ),
                        shape = RoundedCornerShape(MosqueTheme.radius.sm),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = MosqueTheme.spacing.sm),
                        modifier = Modifier.height(32.dp)
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
                    TvFocusableButton(
                        onClick = { onTypeSelected(type) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MosqueTheme.colors.accent else MosqueTheme.colors.fill
                        ),
                        shape = RoundedCornerShape(MosqueTheme.radius.sm),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = MosqueTheme.spacing.sm),
                        modifier = Modifier.height(32.dp)
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
    val density = androidx.compose.ui.platform.LocalDensity.current
    val containerSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val screenWidthDp = with(density) { containerSize.width.toDp() }
    val isCompact = screenWidthDp < 600.dp
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
                TvFocusableButton(
                    onClick = { if (value > minValue) onValueChange(value - 1) },
                    shape = RoundedCornerShape(MosqueTheme.radius.sm),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("-", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
     
                Text(
                    text = value.toString(),
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.bodyCompact,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
     
                TvFocusableButton(
                    onClick = { if (value < maxValue) onValueChange(value + 1) },
                    shape = RoundedCornerShape(MosqueTheme.radius.sm),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("+", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                Box {
                    TvFocusableButton(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(MosqueTheme.radius.sm),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MosqueTheme.colors.fillSoft
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = MosqueTheme.spacing.sm),
                        modifier = Modifier.height(32.dp)
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
                        modifier = Modifier.background(MosqueTheme.colors.surfaceNeutral)
                    ) {
                        unitLabels.forEachIndexed { index, label ->
                            DropdownMenuItem(
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
                TvFocusableButton(
                    onClick = { if (value > minValue) onValueChange(value - 1) },
                    shape = RoundedCornerShape(MosqueTheme.radius.sm),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("-", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
     
                Text(
                    text = value.toString(),
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
     
                TvFocusableButton(
                    onClick = { if (value < maxValue) onValueChange(value + 1) },
                    shape = RoundedCornerShape(MosqueTheme.radius.sm),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("+", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                Box {
                    TvFocusableButton(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(MosqueTheme.radius.sm),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MosqueTheme.colors.fillSoft
                        ),
                        contentPadding = PaddingValues(horizontal = MosqueTheme.spacing.lg, vertical = MosqueTheme.spacing.sm),
                        modifier = Modifier.height(36.dp)
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
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MosqueTheme.colors.surfaceNeutral)
                    ) {
                        unitLabels.forEachIndexed { index, label ->
                            DropdownMenuItem(
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



@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ThemeSettingsSegmentPreview() {
    val mockConfig = MosqueConfigEntity().apply {
        transitionType = "FADE"
        slideIntervalSeconds = 10
        videoWaitUntilEnd = false
        includeDefaultImage = true
        dashboardRandomize = false
        dashboardTransitionType = "FADE"
        dashboardTransitionInterval = 86400
    }

    val mockMediaList = listOf(
        MediaEntity().apply {
            id = "1"
            filePath = "path/to/image1.jpg"
            isVideo = false
        },
        MediaEntity().apply {
            id = "2"
            filePath = "path/to/video1.mp4"
            isVideo = true
        }
    )

    val mockSlideshowSchedules = listOf(
        SlideshowScheduleEntity().apply {
            id = "1"
            day = "Monday"
            isSpecificDate = false
            startTime = "08:00"
            endTime = "17:00"
        }
    )

    val mockDashboardBackgrounds = listOf(
        DashboardBackgroundEntity().apply {
            id = "bg1"
            filePath = "path/to/bg1.jpg"
            isUsed = true
            sortOrder = 0
        },
        DashboardBackgroundEntity().apply {
            id = "bg2"
            filePath = "path/to/bg2.jpg"
            isUsed = false
            sortOrder = 1
        }
    )

    MosqueAthanTheme {
        ThemeSettingsSegment(
            config = mockConfig,
            transitionType = "FADE",
            slideIntervalSeconds = 10,
            videoWaitUntilEnd = false,
            includeDefaultImage = true,
            mediaList = mockMediaList,
            slideshowSchedules = emptyList(),
            dashboardBackgrounds = emptyList(),
            announcements = emptyList(),
            onUpdateThemeSettings = { _, _, _, _ -> },
            onAddMediaItem = {},
            onUpdateMediaItemPath = {},
            onUpdateMediaItemUsed = { _, _ -> },
            onDeleteMediaItem = {},
            onReorderMediaItems = {},
            onSelectCustomBackground = {},
            onClearCustomBackground = {},
            onAddSlideshowSchedule = { _, _, _, _ -> },
            onUpdateSlideshowScheduleTime = { _, _, _ -> },
            onUpdateSlideshowScheduleDays = { _, _ -> },
            onUpdateSlideshowScheduleDates = { _, _ -> },
            onDeleteSlideshowSchedule = {},
            onUpdateSlideshowEnabled = {},
            onPreviewSlideshow = {},
            onAddDashboardBackground = {},
            onUpdateDashboardBackgroundPath = {},
            onDeleteDashboardBackground = {},
            onUpdateDashboardBackgroundUsed = { _, _ -> },
            onUpdateDashboardBackgroundOrder = {},
            onUpdateDashboardSettings = { _, _, _ -> },
            onAddAnnouncement = {},
            onUpdateAnnouncement = { _, _ -> },
            onDeleteAnnouncement = {},
            onReorderAnnouncements = {},
            isCompact = false
        )
    }
}

@Composable
fun RunningTextSettingsCard(
    announcements: List<com.groovernapp.mosqueathan.data.AnnouncementEntity>,
    onAdd: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onReorder: (List<String>) -> Unit
) {
    var newText by remember { mutableStateOf("") }
    
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
                color = MosqueTheme.colors.textTertiary,
                fontSize = MosqueTheme.typography.caption
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = newText,
                onValueChange = { newText = it },
                placeholder = { Text(Lang.get("Enter new running text...", "Masukkan teks running text baru..."), color = MosqueTheme.colors.textMuted) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MosqueTheme.colors.accent,
                    unfocusedBorderColor = MosqueTheme.colors.border,
                    focusedTextColor = MosqueTheme.colors.textPrimary,
                    unfocusedTextColor = MosqueTheme.colors.textPrimary
                )
            )
            TvFocusablePrimaryButton(
                onClick = {
                    if (newText.isNotBlank()) {
                        onAdd(newText)
                        newText = ""
                    }
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(MosqueTheme.radius.md)
            ) {
                Text(Lang.get("Add", "Tambah"), color = MosqueTheme.colors.onAccent, fontWeight = FontWeight.Bold)
            }
        }
        
        if (announcements.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
            ) {
                announcements.sortedBy { it.position }.forEach { item ->
                    var isEditing by remember(item.id) { mutableStateOf(false) }
                    var editText by remember(item.id) { mutableStateOf(item.content) }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MosqueTheme.radius.md))
                            .background(MosqueTheme.colors.scrim.copy(alpha = 0.3f))
                            .padding(MosqueTheme.spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                    ) {
                        if (isEditing) {
                            androidx.compose.material3.OutlinedTextField(
                                value = editText,
                                onValueChange = { editText = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MosqueTheme.colors.accent,
                                    unfocusedBorderColor = MosqueTheme.colors.border,
                                    focusedTextColor = MosqueTheme.colors.textPrimary,
                                    unfocusedTextColor = MosqueTheme.colors.textPrimary
                                )
                            )
                            IconButton(onClick = {
                                onEdit(item.id, editText)
                                isEditing = false
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Save", tint = MosqueTheme.colors.accent)
                            }
                        } else {
                            Text(
                                text = item.content,
                                color = MosqueTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f),
                                fontSize = MosqueTheme.typography.label
                            )
                            IconButton(onClick = { isEditing = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MosqueTheme.colors.textSecondary, modifier=Modifier.size(20.dp))
                            }
                            IconButton(onClick = { onDelete(item.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MosqueTheme.colors.danger, modifier=Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
