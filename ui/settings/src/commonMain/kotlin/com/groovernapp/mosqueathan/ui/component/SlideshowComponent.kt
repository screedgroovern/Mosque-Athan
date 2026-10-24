package com.groovernapp.mosqueathan.ui.component

import com.groovernapp.mosqueathan.ui.design.MediaImageState
import com.groovernapp.mosqueathan.ui.design.rememberMediaImage
import com.groovernapp.mosqueathan.util.swap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextAlign
import com.groovernapp.mosqueathan.domain.Media
import com.groovernapp.mosqueathan.ui.settings.displaymedia.TransitionAnimationRow
import com.groovernapp.mosqueathan.ui.settings.displaymedia.DropdownAdjustmentRow
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.launch
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import com.groovernapp.mosqueathan.ui.design.SystemBackHandler
import com.groovernapp.mosqueathan.ui.design.component.MosquePrimaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueSecondaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueIconButton
import com.groovernapp.mosqueathan.ui.design.focusHighlight
import com.groovernapp.mosqueathan.ui.design.MosqueRadius

@Composable
fun <T> SlideshowComponent(
    items: List<T>,
    keySelector: (T) -> String,
    filePathSelector: (T) -> String,
    isVideoSelector: (T) -> Boolean = { false },
    isUsedSelector: ((T) -> Boolean)? = null, // Optional Use Checkbox
    onUpdateUseStatus: ((T, Boolean) -> Unit)? = null,
    
    // Add Button
    showAddButton: Boolean = true,
    addText: String = Lang.get("Add", "Tambah"),
    onAddClicked: () -> Unit = {},

    // Edit/Delete/Reorder
    onReorderItems: ((List<String>) -> Unit)? = null,
    onEditItem: (T) -> Unit = {},
    onDeleteItem: (T) -> Unit = {},
    
    // Empty state text
    emptyText: String = Lang.get("No media added. Slideshow is empty.", "Belum ada media. Slideshow kosong."),
    
    // Transition Animation
    showTransitionAnimation: Boolean = true,
    transitionType: String = "FADE",
    onTransitionTypeChange: ((String) -> Unit)? = null,
    
    // Transition Interval
    showTransitionInterval: Boolean = true,
    transitionIntervalValue: Int = 1,
    transitionIntervalUnitIndex: Int = 0,
    onTransitionIntervalValueChange: ((Int) -> Unit)? = null,
    onTransitionIntervalUnitChange: ((Int) -> Unit)? = null,

    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    // Sorting without a touchscreen. The order is held in `working` and only reported when the
    // person confirms, so a sort is one update to the display rather than one per step, and
    // cancelling leaves nothing to undo. `sortIndex` is tracked here rather than read back from the
    // pager: a second key press can arrive before the pager has settled on the first one's page.
    var isSortMode by remember { mutableStateOf(false) }
    var working by remember(items) { mutableStateOf(items) }
    var sortIndex by remember { mutableIntStateOf(0) }
    // Shown whenever there is something to sort and a way to save it, usable from the second item:
    // a button that only appeared at two items read as a feature that was missing.
    val canSort = onReorderItems != null && items.isNotEmpty()
    val finishSort: (Boolean) -> Unit = { commit ->
        val newIds = working.map(keySelector)
        if (commit && newIds != items.map(keySelector)) onReorderItems?.invoke(newIds)
        working = items
        isSortMode = false
    }
    SystemBackHandler(enabled = isSortMode) { finishSort(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
    ) {
        // Pager
        if (items.isNotEmpty()) {
            val shown = if (isSortMode) working else items
            val pagerState = rememberPagerState(initialPage = 0) { items.size }
            val coroutineScope = rememberCoroutineScope()
            var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
            var dragItemOffsetX by remember { mutableFloatStateOf(0f) }
            var itemCardWidth by remember { mutableIntStateOf(0) }
            val sortFocus = remember { FocusRequester() }

            fun moveCurrent(delta: Int) {
                val to = sortIndex + delta
                if (to !in working.indices) return
                working = working.toMutableList().apply { swap(sortIndex, to) }
                sortIndex = to
                coroutineScope.launch { pagerState.scrollToPage(to) }
            }

            LaunchedEffect(isSortMode) {
                if (isSortMode) {
                    sortIndex = pagerState.currentPage
                    // Guarded like InitialFocus: the node may not be attached on the first frame.
                    runCatching { sortFocus.requestFocus() }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isSortMode) {
                                Modifier
                                    .onKeyEvent { event ->
                                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                                        when (event.key) {
                                            Key.DirectionLeft -> { moveCurrent(-1); true }
                                            Key.DirectionRight -> { moveCurrent(1); true }
                                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                                finishSort(true)
                                                true
                                            }
                                            else -> false
                                        }
                                    }
                                    .focusRequester(sortFocus)
                                    .focusable()
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    HorizontalPager(
                        state = pagerState,
                        // Swiping while sorting would move the page out from under the item being
                        // held; the move buttons and arrow keys are the only way to change it.
                        userScrollEnabled = !isSortMode,
                        contentPadding = PaddingValues(horizontal = MosqueTheme.spacing.xxhuge),
                        pageSpacing = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { index ->
                        val item = shown[index]
                        val filePath = filePathSelector(item)
                        val isVideo = isVideoSelector(item)
                        
                        val image = rememberMediaImage(filePath, load = !isVideo)
                        
                        val isCurrentPageDragged = index == draggedItemIndex

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .onGloballyPositioned { coords ->
                                    if (index == pagerState.currentPage) {
                                        itemCardWidth = coords.size.width
                                    }
                                }
                                .zIndex(if (isCurrentPageDragged) 10f else 1f)
                                .graphicsLayer {
                                    translationX = if (isCurrentPageDragged) dragItemOffsetX else 0f
                                }
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedItemIndex = index
                                            dragItemOffsetX = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragItemOffsetX += dragAmount.x

                                            val draggedIdx = draggedItemIndex
                                            if (draggedIdx != null && itemCardWidth > 0) {
                                                val threshold = itemCardWidth / 2f
                                                if (dragItemOffsetX > threshold && draggedIdx < items.size - 1) {
                                                    val newList = items.toMutableList()
                                                    newList.swap(draggedIdx, draggedIdx + 1)
                                                    onReorderItems?.invoke(newList.map { keySelector(it) })
                                                    dragItemOffsetX -= itemCardWidth
                                                    draggedItemIndex = draggedIdx + 1
                                                    coroutineScope.launch { pagerState.scrollToPage(draggedIdx + 1) }
                                                } else if (dragItemOffsetX < -threshold && draggedIdx > 0) {
                                                    val newList = items.toMutableList()
                                                    newList.swap(draggedIdx, draggedIdx - 1)
                                                    onReorderItems?.invoke(newList.map { keySelector(it) })
                                                    dragItemOffsetX += itemCardWidth
                                                    draggedItemIndex = draggedIdx - 1
                                                    coroutineScope.launch { pagerState.scrollToPage(draggedIdx - 1) }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggedItemIndex = null
                                            dragItemOffsetX = 0f
                                        },
                                        onDragCancel = {
                                            draggedItemIndex = null
                                            dragItemOffsetX = 0f
                                        }
                                    )
                                }
                                .clip(RoundedCornerShape(MosqueTheme.radius.lg))
                                .background(MosqueTheme.colors.fillSubtle)
                                .then(
                                    if (isSortMode && index == sortIndex) {
                                        Modifier.border(
                                            MosqueTheme.stroke.thick,
                                            MosqueTheme.colors.accent,
                                            RoundedCornerShape(MosqueTheme.radius.lg)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            if (isVideo) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Video File",
                                        tint = MosqueTheme.colors.accent,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        text = Lang.get("Video Slide", "Video Slide"),
                                        color = MosqueTheme.colors.textSecondary,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = MosqueTheme.spacing.lg)
                                    )
                                }
                            } else if (image is MediaImageState.Loaded) {
                                Image(
                                    bitmap = image.image,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (image is MediaImageState.Unavailable) {
                                // Only once it has actually failed. While it is still loading the
                                // empty frame stands on its own -- a controller fetches this over
                                // the network, and announcing failure first would be a lie.
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = Lang.get("Failed to load image", "Gagal memuat gambar"),
                                        color = MosqueTheme.colors.textMuted
                                    )
                                }
                            }

                            if (!isSortMode) Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(MosqueTheme.spacing.lg),
                                horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                            ) {
                                MosqueIconButton(
                                    onClick = { onEditItem(item) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MosqueTheme.colors.scrim.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.size(MosqueTheme.size.controlMd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Media",
                                        tint = MosqueTheme.colors.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                MosqueIconButton(
                                    onClick = { onDeleteItem(item) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MosqueTheme.colors.scrim.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.size(MosqueTheme.size.controlMd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Media",
                                        tint = MosqueTheme.colors.danger.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Dot Indicator
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = MosqueTheme.spacing.lg)
                            .height(MosqueTheme.size.iconSm),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(items.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) MosqueTheme.colors.accent else MosqueTheme.colors.textMuted
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

                if (isSortMode) {
                    // Buttons as well as keys: a mouse or a finger has no D-pad, and in sort mode the
                    // edit and delete overlays are gone, so these are the only controls on the card.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
                    ) {
                        MosqueIconButton(
                            onClick = { moveCurrent(-1) },
                            enabled = sortIndex > 0,
                            modifier = Modifier.size(MosqueTheme.size.controlMd)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = Lang.get("Move left", "Geser ke kiri"),
                                tint = MosqueTheme.colors.textPrimary
                            )
                        }
                        Text(
                            text = "${sortIndex + 1} / ${working.size}",
                            color = MosqueTheme.colors.accent,
                            fontSize = MosqueTheme.typography.bodyCompact,
                            fontWeight = FontWeight.Bold
                        )
                        MosqueIconButton(
                            onClick = { moveCurrent(1) },
                            enabled = sortIndex < working.lastIndex,
                            modifier = Modifier.size(MosqueTheme.size.controlMd)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = Lang.get("Move right", "Geser ke kanan"),
                                tint = MosqueTheme.colors.textPrimary
                            )
                        }
                    }
                    Text(
                        text = Lang.get(
                            "Press Left or Right to move this item, then OK to confirm.",
                            "Tekan Kiri atau Kanan untuk menggeser item ini, lalu OK untuk menyimpan."
                        ),
                        color = MosqueTheme.colors.textSecondary,
                        fontSize = MosqueTheme.typography.caption,
                        textAlign = TextAlign.Center
                    )
                }

                // Use Checkbox
                if (isUsedSelector != null && onUpdateUseStatus != null && !isSortMode) {
                    val currentItem = shown.getOrNull(pagerState.currentPage)
                    if (currentItem != null) {
                        val isUsed = isUsedSelector(currentItem)
                        Row(
                            modifier = Modifier
                                .focusHighlight(shape = RoundedCornerShape(MosqueRadius.sm), scaleUp = false)
                                .clickable { onUpdateUseStatus(currentItem, !isUsed) }
                                .padding(vertical = MosqueTheme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                modifier = Modifier.focusHighlight(shape = RoundedCornerShape(MosqueRadius.xs), scaleUp = false),
                                checked = isUsed,
                                onCheckedChange = { onUpdateUseStatus(currentItem, it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MosqueTheme.colors.accent,
                                    checkmarkColor = MosqueTheme.colors.onAccent,
                                    uncheckedColor = MosqueTheme.colors.textSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(MosqueTheme.spacing.sm))
                            Text(
                                text = Lang.get("Use", "Gunakan"),
                                color = MosqueTheme.colors.textPrimary,
                                fontSize = MosqueTheme.typography.label
                            )
                        }
                    }
                }
            }
        } else {
            // Default placeholder card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(MosqueTheme.radius.lg))
                    .background(MosqueTheme.colors.fillSubtle),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyText,
                    color = MosqueTheme.colors.textMuted,
                    fontSize = MosqueTheme.typography.label
                )
            }
        }

        // Add / Sort. Stacked on a phone, where two buttons in a row would leave the Add label
        // too narrow to read; side by side everywhere else.
        if (isSortMode) {
            val done: @Composable (Modifier) -> Unit = { m ->
                MosquePrimaryButton(
                    onClick = { finishSort(true) },
                    modifier = m.heightIn(min = MosqueTheme.size.controlLg)
                ) {
                    Text(
                        text = Lang.get("Done", "Selesai"),
                        color = MosqueTheme.colors.onAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = MosqueTheme.typography.bodyCompact,
                        textAlign = TextAlign.Center
                    )
                }
            }
            val cancel: @Composable (Modifier) -> Unit = { m ->
                MosqueSecondaryButton(
                    onClick = { finishSort(false) },
                    modifier = m.heightIn(min = MosqueTheme.size.controlLg)
                ) {
                    Text(
                        text = Lang.get("Cancel", "Batal"),
                        fontWeight = FontWeight.Bold,
                        fontSize = MosqueTheme.typography.bodyCompact,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg)) {
                done(Modifier.weight(1f))
                cancel(Modifier.weight(1f))
            }
        } else if (showAddButton || canSort) {
            val add: @Composable (Modifier) -> Unit = { m ->
                MosquePrimaryButton(
                    onClick = onAddClicked,
                    modifier = m.heightIn(min = MosqueTheme.size.controlLg)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MosqueTheme.colors.onAccent)
                        Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
                        Text(
                            text = addText,
                            color = MosqueTheme.colors.onAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = MosqueTheme.typography.bodyCompact,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            val sort: @Composable (Modifier) -> Unit = { m ->
                MosqueSecondaryButton(
                    onClick = { isSortMode = true },
                    enabled = items.size > 1,
                    modifier = m.heightIn(min = MosqueTheme.size.controlLg)
                ) {
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
                    Text(
                        text = Lang.get("Sort", "Urutkan"),
                        fontWeight = FontWeight.Bold,
                        fontSize = MosqueTheme.typography.bodyCompact,
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (LocalWindowClass.current.isCompact) {
                Column(verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)) {
                    if (showAddButton) add(Modifier.fillMaxWidth())
                    if (canSort) sort(Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg)) {
                    if (showAddButton) add(Modifier.weight(1f))
                    if (canSort) sort(if (showAddButton) Modifier else Modifier.fillMaxWidth())
                }
            }
        }

        // Transition Settings
        if (showTransitionAnimation && onTransitionTypeChange != null) {
            TransitionAnimationRow(
                title = Lang.get("Transition Animation", "Animasi Transisi"),
                selectedType = transitionType,
                isCompact = isCompact,
                onTypeSelected = onTransitionTypeChange
            )
        }

        if (showTransitionInterval && onTransitionIntervalValueChange != null && onTransitionIntervalUnitChange != null) {
            val unitLabels = listOf(
                Lang.get("seconds", "detik"),
                Lang.get("minutes", "menit"),
                Lang.get("hours", "jam"),
                Lang.get("days", "hari")
            )
            
            val maxValForUnit = when (transitionIntervalUnitIndex) {
                0 -> 60 // detik
                1 -> 60 // menit
                2 -> 24 // jam
                3 -> 30 // hari
                else -> 60
            }

            DropdownAdjustmentRow(
                title = Lang.get("Transition interval", "Interval transisi"),
                value = transitionIntervalValue,
                minValue = 1,
                maxValue = maxValForUnit,
                unitLabels = unitLabels,
                selectedUnitIndex = transitionIntervalUnitIndex,
                onValueChange = onTransitionIntervalValueChange,
                onUnitChange = onTransitionIntervalUnitChange
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun SlideshowComponentPreview() {
    val mockMediaList = listOf(
        Media(id = "1", filePath = "path/to/image1.jpg", isVideo = false, isUsed = true),
        Media(id = "2", filePath = "path/to/video1.mp4", isVideo = true, isUsed = false),
        Media(id = "3", filePath = "path/to/image2.jpg", isVideo = false, isUsed = true)
    )

    MosqueTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SlideshowComponent(
                items = mockMediaList,
                keySelector = { it.id },
                filePathSelector = { it.filePath },
                isVideoSelector = { it.isVideo },
                isUsedSelector = { it.isUsed },
                onUpdateUseStatus = null,
                onAddClicked = {},
                onEditItem = {},
                onDeleteItem = {},
                onReorderItems = {},
                onTransitionTypeChange = {},
                onTransitionIntervalValueChange = {},
                onTransitionIntervalUnitChange = {},
                modifier = Modifier.padding(MosqueTheme.spacing.xl)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun SlideshowComponentEmptyPreview() {
    MosqueTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SlideshowComponent<Media>(
                items = emptyList(),
                keySelector = { it.id },
                filePathSelector = { it.filePath },
                onAddClicked = {},
                modifier = Modifier.padding(MosqueTheme.spacing.xl)
            )
        }
    }
}
