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
import com.groovernapp.mosqueathan.ui.settings.TvFocusableButton
import com.groovernapp.mosqueathan.ui.settings.TvFocusablePrimaryButton
import com.groovernapp.mosqueathan.ui.settings.AdjustmentRow
import com.groovernapp.mosqueathan.ui.theme.MosqueAthanTheme
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.launch

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
    onUpdateThemeSettings: (String, Int, Boolean, Boolean) -> Unit,
    onAddMediaItem: () -> Unit,
    onUpdateMediaItemPath: (String) -> Unit,
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
    isCompact: Boolean
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TabRow header for Dashboard & Slide Show segments
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Dashboard",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = Lang.get("Dashboard", "Dashboard"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                selectedContentColor = Color(0xFFFFD54F),
                unselectedContentColor = Color.White.copy(alpha = 0.6f)
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Slide Show",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = Lang.get("Slide Show", "Slide Show"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                DashboardTabContent(
                    config = config,
                    dashboardBackgrounds = dashboardBackgrounds,
                    onAddDashboardBackground = onAddDashboardBackground,
                    onUpdateDashboardBackgroundPath = onUpdateDashboardBackgroundPath,
                    onDeleteDashboardBackground = onDeleteDashboardBackground,
                    onUpdateDashboardBackgroundUsed = onUpdateDashboardBackgroundUsed,
                    onUpdateDashboardBackgroundOrder = onUpdateDashboardBackgroundOrder,
                    onUpdateDashboardSettings = onUpdateDashboardSettings,
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
    onAddDashboardBackground: () -> Unit,
    onUpdateDashboardBackgroundPath: (String) -> Unit,
    onDeleteDashboardBackground: (String) -> Unit,
    onUpdateDashboardBackgroundUsed: (String, Boolean) -> Unit,
    onUpdateDashboardBackgroundOrder: (List<String>) -> Unit,
    onUpdateDashboardSettings: (Boolean, String, Int) -> Unit,
    isCompact: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Row for Randomize Switch (Aligned Top-Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Lang.get("Dashboard Background List", "Daftar Latar Belakang Dashboard"),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = Lang.get("Randomize", "Tampilkan random"),
                    color = Color.White,
                    fontSize = 14.sp
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
                        checkedThumbColor = Color(0xFFFFD54F),
                        checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.5f)
                    )
                )
            }
        }

        // Preview Pager with peeking previous/next items AND long-press drag-to-reorder support
        if (dashboardBackgrounds.isNotEmpty()) {
            val pagerState = rememberPagerState(initialPage = 0) { dashboardBackgrounds.size }
            val coroutineScope = rememberCoroutineScope()
            var draggedBgPageIndex by remember { mutableStateOf<Int?>(null) }
            var dragBgPageOffsetX by remember { mutableStateOf(0f) }
            var bgPageCardWidth by remember { mutableStateOf(0) }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        pageSpacing = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { index ->
                        val bg = dashboardBackgrounds[index]
                        val bitmap = remember(bg.filePath) {
                            try {
                                android.graphics.BitmapFactory.decodeFile(bg.filePath)?.asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        }
                        val isCurrentPageDragged = index == draggedBgPageIndex

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .onGloballyPositioned { coords ->
                                    if (index == pagerState.currentPage) {
                                        bgPageCardWidth = coords.size.width
                                    }
                                }
                                .zIndex(if (isCurrentPageDragged) 10f else 1f)
                                .graphicsLayer {
                                    translationX = if (isCurrentPageDragged) dragBgPageOffsetX else 0f
                                }
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedBgPageIndex = index
                                            dragBgPageOffsetX = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragBgPageOffsetX += dragAmount.x

                                            val draggedIdx = draggedBgPageIndex
                                            if (draggedIdx != null && bgPageCardWidth > 0) {
                                                val threshold = bgPageCardWidth / 2f
                                                if (dragBgPageOffsetX > threshold && draggedIdx < dashboardBackgrounds.size - 1) {
                                                    val newList = dashboardBackgrounds.toMutableList()
                                                    java.util.Collections.swap(newList, draggedIdx, draggedIdx + 1)
                                                    onUpdateDashboardBackgroundOrder(newList.map { it.id })
                                                    dragBgPageOffsetX -= bgPageCardWidth
                                                    draggedBgPageIndex = draggedIdx + 1
                                                    coroutineScope.launch {
                                                        pagerState.scrollToPage(draggedIdx + 1)
                                                    }
                                                } else if (dragBgPageOffsetX < -threshold && draggedIdx > 0) {
                                                    val newList = dashboardBackgrounds.toMutableList()
                                                    java.util.Collections.swap(newList, draggedIdx, draggedIdx - 1)
                                                    onUpdateDashboardBackgroundOrder(newList.map { it.id })
                                                    dragBgPageOffsetX += bgPageCardWidth
                                                    draggedBgPageIndex = draggedIdx - 1
                                                    coroutineScope.launch {
                                                        pagerState.scrollToPage(draggedIdx - 1)
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggedBgPageIndex = null
                                            dragBgPageOffsetX = 0f
                                        },
                                        onDragCancel = {
                                            draggedBgPageIndex = null
                                            dragBgPageOffsetX = 0f
                                        }
                                    )
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = Lang.get("Failed to load image", "Gagal memuat gambar"),
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            // Edit / Delete icons overlaid at the top
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { onUpdateDashboardBackgroundPath(bg.id) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Black.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Background",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteDashboardBackground(bg.id) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Black.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Background",
                                        tint = Color.Red.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Dot Indicator stationary inside the pager overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .height(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(dashboardBackgrounds.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.4f)
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(6.dp)
                            )
                        }
                    }
                }

                // Checkbox "Use / Gunakan" outside pager, tied dynamically to current page
                val currentBg = dashboardBackgrounds.getOrNull(pagerState.currentPage)
                if (currentBg != null) {
                    Row(
                        modifier = Modifier
                            .clickable { onUpdateDashboardBackgroundUsed(currentBg.id, !currentBg.isUsed) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = currentBg.isUsed,
                            onCheckedChange = { onUpdateDashboardBackgroundUsed(currentBg.id, it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFFFFD54F),
                                checkmarkColor = Color.Black,
                                uncheckedColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Lang.get("Use", "Gunakan"),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            // Default placeholder card (16:9)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.main_bg_default),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Lang.get("Using Default Background", "Menggunakan Latar Belakang Default"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Add Background Button
        TvFocusablePrimaryButton(
            onClick = onAddDashboardBackground,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Lang.get("Add Dashboard Background", "Tambah Latar Belakang Dashboard"),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // Transition selector
        TransitionAnimationRow(
            title = Lang.get("Transition Animation", "Animasi Transisi"),
            selectedType = config.dashboardTransitionType,
            isCompact = isCompact,
            onTypeSelected = { type ->
                onUpdateDashboardSettings(
                    config.dashboardRandomize,
                    type,
                    config.dashboardTransitionInterval
                )
            }
        )

        val (displayValue, unitIndex) = remember(config.dashboardTransitionInterval) {
            val interval = config.dashboardTransitionInterval
            when {
                interval % 86400 == 0 -> Pair(interval / 86400, 3) // Hari
                interval % 3600 == 0 -> Pair(interval / 3600, 2)  // Jam
                interval % 60 == 0 -> Pair(interval / 60, 1)    // Menit
                else -> Pair(interval, 0)                      // Detik
            }
        }

        val unitLabels = listOf(
            Lang.get("seconds", "detik"),
            Lang.get("minutes", "menit"),
            Lang.get("hours", "jam"),
            Lang.get("days", "hari")
        )

        val multipliers = listOf(1, 60, 3600, 86400)
        val maxValForUnit = when (unitIndex) {
            0 -> 60 // detik
            1 -> 60 // menit
            2 -> 24 // jam
            3 -> 30 // hari
            else -> 60
        }

        DropdownAdjustmentRow(
            title = Lang.get("Transition interval", "Interval transisi"),
            value = displayValue,
            minValue = 1,
            maxValue = maxValForUnit,
            unitLabels = unitLabels,
            selectedUnitIndex = unitIndex,
            onValueChange = { newVal ->
                onUpdateDashboardSettings(
                    config.dashboardRandomize,
                    config.dashboardTransitionType,
                    newVal * multipliers[unitIndex]
                )
            },
            onUnitChange = { index ->
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
            }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = Lang.get("Slideshow Media Items", "Daftar Media Slideshow"),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Slideshow Carousel with peeking previous/next items AND long-press drag-to-reorder support
        if (mediaList.isNotEmpty()) {
            val pagerState = rememberPagerState(initialPage = 0) { mediaList.size }
            val coroutineScope = rememberCoroutineScope()
            var draggedMediaPageIndex by remember { mutableStateOf<Int?>(null) }
            var dragMediaPageOffsetX by remember { mutableStateOf(0f) }
            var mediaPageCardWidth by remember { mutableStateOf(0) }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    pageSpacing = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { index ->
                    val media = mediaList[index]
                    val bitmap = remember(media.filePath) {
                        try {
                            if (!media.isVideo) {
                                android.graphics.BitmapFactory.decodeFile(media.filePath)?.asImageBitmap()
                            } else {
                                null
                            }
                        } catch (_: Exception) {
                            null
                        }
                    }
                    val isCurrentPageDragged = index == draggedMediaPageIndex

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .onGloballyPositioned { coords ->
                                if (index == pagerState.currentPage) {
                                    mediaPageCardWidth = coords.size.width
                                }
                            }
                            .zIndex(if (isCurrentPageDragged) 10f else 1f)
                            .graphicsLayer {
                                translationX = if (isCurrentPageDragged) dragMediaPageOffsetX else 0f
                            }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedMediaPageIndex = index
                                        dragMediaPageOffsetX = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragMediaPageOffsetX += dragAmount.x

                                        val draggedIdx = draggedMediaPageIndex
                                        if (draggedIdx != null && mediaPageCardWidth > 0) {
                                            val threshold = mediaPageCardWidth / 2f
                                            if (dragMediaPageOffsetX > threshold && draggedIdx < mediaList.size - 1) {
                                                val newList = mediaList.toMutableList()
                                                java.util.Collections.swap(newList, draggedIdx, draggedIdx + 1)
                                                onReorderMediaItems(newList.map { it.id })
                                                dragMediaPageOffsetX -= mediaPageCardWidth
                                                draggedMediaPageIndex = draggedIdx + 1
                                                coroutineScope.launch {
                                                    pagerState.scrollToPage(draggedIdx + 1)
                                                }
                                            } else if (dragMediaPageOffsetX < -threshold && draggedIdx > 0) {
                                                val newList = mediaList.toMutableList()
                                                java.util.Collections.swap(newList, draggedIdx, draggedIdx - 1)
                                                onReorderMediaItems(newList.map { it.id })
                                                dragMediaPageOffsetX += mediaPageCardWidth
                                                draggedMediaPageIndex = draggedIdx - 1
                                                coroutineScope.launch {
                                                    pagerState.scrollToPage(draggedIdx - 1)
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggedMediaPageIndex = null
                                        dragMediaPageOffsetX = 0f
                                    },
                                    onDragCancel = {
                                        draggedMediaPageIndex = null
                                        dragMediaPageOffsetX = 0f
                                    }
                                )
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        if (media.isVideo) {
                            // Video placeholder
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Video File",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = Lang.get("Video Slide", "Video Slide"),
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 12.dp)
                                )
                            }
                        } else if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = Lang.get("Failed to load image", "Gagal memuat gambar"),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Overlaid edit/delete icons
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { onUpdateMediaItemPath(media.id) },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Media",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDeleteMediaItem(media) },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Media",
                                    tint = Color.Red.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Stationary Dot Indicator overlaid at the bottom of the image area
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .height(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(mediaList.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.4f)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(6.dp)
                        )
                    }
                }
            }
        } else {
            // Default placeholder card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Lang.get("No media added. Slideshow is empty.", "Belum ada media. Slideshow kosong."),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }

        // Add Media Button (Filled Style)
        TvFocusablePrimaryButton(
            onClick = onAddMediaItem,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Lang.get("Add Image / Video to Slideshow", "Tambah Gambar / Video ke Slideshow"),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        TransitionAnimationRow(
            title = Lang.get("Transition Animation", "Animasi Transisi"),
            selectedType = transitionType,
            isCompact = isCompact,
            onTypeSelected = { type ->
                onUpdateThemeSettings(
                    type,
                    slideIntervalSeconds,
                    videoWaitUntilEnd,
                    includeDefaultImage
                )
            }
        )

        // Responsive Transition Interval Counter
        AdjustmentRow(
            title = Lang.get("Transition interval", "Interval transisi"),
            value = slideIntervalSeconds,
            minValue = 5,
            maxValue = 120,
            showSign = false,
            unitSuffix = Lang.get("seconds", "detik"),
            onValueChange = { interval ->
                onUpdateThemeSettings(
                    transitionType,
                    interval,
                    videoWaitUntilEnd,
                    includeDefaultImage
                )
            }
        )

        // Video Playback behavior duration options (Wait for video / Follow interval)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = Lang.get("Video Playback Duration", "Durasi Putar Video"),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Lang.get(
                        "Wait for video to finish playing or follow regular slide interval",
                        "Tunggu video selesai diputar atau ikuti interval slide biasa"
                    ),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false, true).forEach { waitEnd ->
                    val isSelected = videoWaitUntilEnd == waitEnd
                    val label = if (waitEnd)
                        Lang.get("Wait for Video", "Selesai Video")
                    else
                        Lang.get("Follow Interval", "Ikuti Interval")
                    TvFocusableButton(
                        onClick = {
                            onUpdateThemeSettings(
                                transitionType,
                                slideIntervalSeconds,
                                waitEnd,
                                includeDefaultImage
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Slideshow Schedule section with enable/disable toggle
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = Lang.get("Slide Show Schedule", "Jadwal Slide Show"),
                color = Color.White,
                fontSize = 15.sp,
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
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFFFFD54F),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
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
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                text = Lang.get("Preview Slideshow", "Preview Slideshow"),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        // Include Default Image switch (Placed at the very bottom, default off)
        Spacer(modifier = Modifier.height(16.dp))
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
                    text = Lang.get(
                        "Include Default Image (Masjidil Haram)",
                        "Sertakan Gambar Default (Masjidil Haram)"
                    ),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Lang.get(
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
                    onUpdateThemeSettings(
                        transitionType,
                        slideIntervalSeconds,
                        videoWaitUntilEnd,
                        checked
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFFD54F),
                    checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.5f)
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
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("FADE", "SLIDE", "ZOOM", "INSTANT").forEach { type ->
                    val isSelected = selectedType == type
                    TvFocusableButton(
                        onClick = { onTypeSelected(type) },
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
    } else {
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
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("FADE", "SLIDE", "ZOOM", "INSTANT").forEach { type ->
                    val isSelected = selectedType == type
                    TvFocusableButton(
                        onClick = { onTypeSelected(type) },
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
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
     
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvFocusableButton(
                    onClick = { if (value > minValue) onValueChange(value - 1) },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
     
                Text(
                    text = value.toString(),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
     
                TvFocusableButton(
                    onClick = { if (value < maxValue) onValueChange(value + 1) },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                
                Box {
                    TvFocusableButton(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.08f)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = unitLabels[selectedUnitIndex],
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF1E1E1E))
                    ) {
                        unitLabels.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = label, 
                                        color = if (selectedUnitIndex == index) Color(0xFFFFD54F) else Color.White,
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
                TvFocusableButton(
                    onClick = { if (value > minValue) onValueChange(value - 1) },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
     
                Text(
                    text = value.toString(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
     
                TvFocusableButton(
                    onClick = { if (value < maxValue) onValueChange(value + 1) },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                
                Box {
                    TvFocusableButton(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.08f)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = unitLabels[selectedUnitIndex],
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF1E1E1E))
                    ) {
                        unitLabels.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = label, 
                                        color = if (selectedUnitIndex == index) Color(0xFFFFD54F) else Color.White,
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
            transitionType = mockConfig.transitionType,
            slideIntervalSeconds = mockConfig.slideIntervalSeconds,
            videoWaitUntilEnd = mockConfig.videoWaitUntilEnd,
            includeDefaultImage = mockConfig.includeDefaultImage,
            mediaList = mockMediaList,
            slideshowSchedules = mockSlideshowSchedules,
            dashboardBackgrounds = mockDashboardBackgrounds,
            onUpdateThemeSettings = { _, _, _, _ -> },
            onAddMediaItem = {},
            onUpdateMediaItemPath = {},
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
            isCompact = false
        )
    }
}
