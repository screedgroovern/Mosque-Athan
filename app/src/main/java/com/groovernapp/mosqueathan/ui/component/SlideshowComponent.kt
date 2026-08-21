package com.groovernapp.mosqueathan.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.groovernapp.mosqueathan.data.MediaEntity
import com.groovernapp.mosqueathan.ui.settings.TvFocusablePrimaryButton
import com.groovernapp.mosqueathan.ui.settings.displaymedia.TransitionAnimationRow
import com.groovernapp.mosqueathan.ui.settings.displaymedia.DropdownAdjustmentRow
import com.groovernapp.mosqueathan.ui.theme.MosqueAthanTheme
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.launch
import java.util.Collections
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

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
    onReorderItems: (List<String>) -> Unit = {},
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pager
        if (items.isNotEmpty()) {
            val pagerState = rememberPagerState(initialPage = 0) { items.size }
            val coroutineScope = rememberCoroutineScope()
            var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
            var dragItemOffsetX by remember { mutableFloatStateOf(0f) }
            var itemCardWidth by remember { mutableIntStateOf(0) }

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
                        val item = items[index]
                        val filePath = filePathSelector(item)
                        val isVideo = isVideoSelector(item)
                        
                        val bitmap = remember(filePath) {
                            try {
                                if (!isVideo) {
                                    BitmapFactory.decodeFile(filePath)?.asImageBitmap()
                                } else null
                            } catch (_: Exception) { null }
                        }
                        
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
                                                    Collections.swap(newList, draggedIdx, draggedIdx + 1)
                                                    onReorderItems(newList.map { keySelector(it) })
                                                    dragItemOffsetX -= itemCardWidth
                                                    draggedItemIndex = draggedIdx + 1
                                                    coroutineScope.launch { pagerState.scrollToPage(draggedIdx + 1) }
                                                } else if (dragItemOffsetX < -threshold && draggedIdx > 0) {
                                                    val newList = items.toMutableList()
                                                    Collections.swap(newList, draggedIdx, draggedIdx - 1)
                                                    onReorderItems(newList.map { keySelector(it) })
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(MosqueTheme.colors.fillSubtle)
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
                                        color = MosqueTheme.colors.textTertiary,
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
                                        color = MosqueTheme.colors.textMuted
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { onEditItem(item) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MosqueTheme.colors.scrim.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Media",
                                        tint = MosqueTheme.colors.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteItem(item) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MosqueTheme.colors.scrim.copy(alpha = 0.6f)
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

                    // Dot Indicator
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .height(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(items.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) MosqueTheme.colors.accent else MosqueTheme.colors.textFaint
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

                // Use Checkbox
                if (isUsedSelector != null && onUpdateUseStatus != null) {
                    val currentItem = items.getOrNull(pagerState.currentPage)
                    if (currentItem != null) {
                        val isUsed = isUsedSelector(currentItem)
                        Row(
                            modifier = Modifier
                                .clickable { onUpdateUseStatus(currentItem, !isUsed) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isUsed,
                                onCheckedChange = { onUpdateUseStatus(currentItem, it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MosqueTheme.colors.accent,
                                    checkmarkColor = MosqueTheme.colors.onAccent,
                                    uncheckedColor = MosqueTheme.colors.textTertiary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
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
                    .clip(RoundedCornerShape(12.dp))
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

        // Add Button
        if (showAddButton) {
            TvFocusablePrimaryButton(
                onClick = onAddClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MosqueTheme.colors.onAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = addText,
                        color = MosqueTheme.colors.onAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = MosqueTheme.typography.bodyCompact
                    )
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
        MediaEntity().apply {
            id = "1"
            filePath = "path/to/image1.jpg"
            isVideo = false
            isUsed = true
        },
        MediaEntity().apply {
            id = "2"
            filePath = "path/to/video1.mp4"
            isVideo = true
            isUsed = false
        },
        MediaEntity().apply {
            id = "3"
            filePath = "path/to/image2.jpg"
            isVideo = false
            isUsed = true
        }
    )

    MosqueAthanTheme {
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
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun SlideshowComponentEmptyPreview() {
    MosqueAthanTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SlideshowComponent<MediaEntity>(
                items = emptyList(),
                keySelector = { it.id },
                filePathSelector = { it.filePath },
                onAddClicked = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
