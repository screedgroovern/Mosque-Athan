package com.groovernapp.mosqueathan.ui.settings.displaymedia

import com.groovernapp.mosqueathan.ui.settings.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.groovernapp.mosqueathan.data.AnnouncementEntity
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.util.Lang

@Composable
fun AnnouncementsSettingsSegment(
    config: MosqueConfigEntity,
    announcements: List<AnnouncementEntity>,
    onAddAnnouncement: (String) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onEditAnnouncement: (String, String) -> Unit,
    onReorderAnnouncements: (List<String>) -> Unit = {},
    isCompact: Boolean = false
) {
    var newAnnouncementText by remember { mutableStateOf("") }
    var editingAnnouncement by remember { mutableStateOf<AnnouncementEntity?>(null) }
    var editTextValue by remember { mutableStateOf("") }
    var isReorderMode by remember { mutableStateOf(false) }
    var isAddingNew by remember { mutableStateOf(false) }
    var addTextValue by remember { mutableStateOf("") }

    if (editingAnnouncement != null) {
        AlertDialog(
            onDismissRequest = { editingAnnouncement = null },
            title = { Text(Lang.get("Edit Running Text", "Edit Teks Berjalan"), color = Color.White) },
            text = {
                OutlinedTextField(
                    value = editTextValue,
                    onValueChange = { editTextValue = it },
                    colors = textFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TvFocusablePrimaryButton(
                    onClick = {
                        editingAnnouncement?.let {
                            if (editTextValue.isNotBlank()) {
                                onEditAnnouncement(it.id, editTextValue.trim())
                            }
                        }
                        editingAnnouncement = null
                    }
                ) {
                    Text(Lang.get("Save", "Simpan"), color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAnnouncement = null }) {
                    Text(Lang.get("Cancel", "Batal"), color = Color.White)
                }
            }
        )
    }

    if (isAddingNew) {
        AlertDialog(
            onDismissRequest = { isAddingNew = false },
            title = { Text(Lang.get("Add Running Text", "Tambah Teks Berjalan"), color = Color.White) },
            text = {
                OutlinedTextField(
                    value = addTextValue,
                    onValueChange = { addTextValue = it },
                    colors = textFieldColors(),
                    singleLine = true,
                    placeholder = { Text(Lang.get("Type new running text here...", "Ketik teks berjalan baru di sini...")) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TvFocusablePrimaryButton(
                    onClick = {
                        if (addTextValue.isNotBlank()) {
                            onAddAnnouncement(addTextValue.trim())
                        }
                        isAddingNew = false
                    }
                ) {
                    Text(Lang.get("Add", "Tambah"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingNew = false }) {
                    Text(Lang.get("Cancel", "Batal"), color = Color.White)
                }
            }
        )
    }

    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Dashboard Running Text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Lang.get("Dashboard Running Text", "Teks Berjalan Dashboard"),
                color = Color(0xFFFFD54F),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isCompact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Arrange Button aligned left
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (announcements.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { isReorderMode = !isReorderMode },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color(0xFFFFD54F)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = if (isReorderMode) Lang.get("Done", "Selesai") else Lang.get("Arrange", "Urutkan"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Add Button aligned right (smaller)
                TvFocusablePrimaryButton(
                    onClick = {
                        isAddingNew = true
                        addTextValue = ""
                    },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = Lang.get("Add", "Tambah"),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newAnnouncementText,
                    onValueChange = { newAnnouncementText = it },
                    placeholder = { Text(Lang.get("Type new running text here...", "Ketik teks berjalan baru di sini...")) },
                    colors = textFieldColors(),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                TvFocusablePrimaryButton(
                    onClick = {
                        if (newAnnouncementText.isNotBlank()) {
                            onAddAnnouncement(newAnnouncementText.trim())
                            newAnnouncementText = ""
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(Lang.get("Add", "Tambah"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            announcements.forEachIndexed { index, announcement ->
                val isCurrentDragged = index == draggedIndex
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            itemHeights[index] = coords.size.height
                        }
                        .zIndex(if (isCurrentDragged) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isCurrentDragged) dragOffsetY else 0f
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (isCompact) {
                        if (isReorderMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Drag Handle",
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    draggedIndex = index
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount.y
                                                    
                                                    val currentDraggedIdx = draggedIndex
                                                    if (currentDraggedIdx != null) {
                                                        var tempIndex = currentDraggedIdx
                                                        val height = itemHeights[tempIndex] ?: 0
                                                        
                                                        if (dragOffsetY > 0) {
                                                            var accumHeight = height / 2f
                                                            while (tempIndex < announcements.size - 1) {
                                                                val nextHeight = itemHeights[tempIndex + 1] ?: 0
                                                                accumHeight += nextHeight / 2f
                                                                if (dragOffsetY > accumHeight) {
                                                                    val newList = announcements.toMutableList()
                                                                    java.util.Collections.swap(newList, tempIndex, tempIndex + 1)
                                                                    onReorderAnnouncements(newList.map { it.id })
                                                                    dragOffsetY -= nextHeight
                                                                    tempIndex += 1
                                                                    draggedIndex = tempIndex
                                                                    accumHeight = height / 2f
                                                                } else {
                                                                    break
                                                                }
                                                            }
                                                        } else if (dragOffsetY < 0) {
                                                            var accumHeight = -height / 2f
                                                            while (tempIndex > 0) {
                                                                val prevHeight = itemHeights[tempIndex - 1] ?: 0
                                                                accumHeight -= prevHeight / 2f
                                                                if (dragOffsetY < accumHeight) {
                                                                    val newList = announcements.toMutableList()
                                                                    java.util.Collections.swap(newList, tempIndex, tempIndex - 1)
                                                                    onReorderAnnouncements(newList.map { it.id })
                                                                    dragOffsetY += prevHeight
                                                                    tempIndex -= 1
                                                                    draggedIndex = tempIndex
                                                                    accumHeight = -height / 2f
                                                                } else {
                                                                    break
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedIndex = null
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggedIndex = null
                                                    dragOffsetY = 0f
                                                }
                                            )
                                        }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = announcement.content,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = announcement.content,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            editTextValue = announcement.content
                                            editingAnnouncement = announcement
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = "Edit",
                                            tint = Color(0xFF90CAF9),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = Lang.get("Edit", "Edit"),
                                            color = Color(0xFF90CAF9),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { onDeleteAnnouncement(announcement.id) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Remove",
                                            tint = Color(0xFFE57373),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = Lang.get("Delete", "Hapus"),
                                            color = Color(0xFFE57373),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Drag Handle",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                draggedIndex = index
                                                dragOffsetY = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                                
                                                val currentDraggedIdx = draggedIndex
                                                if (currentDraggedIdx != null) {
                                                    var tempIndex = currentDraggedIdx
                                                    val height = itemHeights[tempIndex] ?: 0
                                                    
                                                    if (dragOffsetY > 0) {
                                                        var accumHeight = height / 2f
                                                        while (tempIndex < announcements.size - 1) {
                                                            val nextHeight = itemHeights[tempIndex + 1] ?: 0
                                                            accumHeight += nextHeight / 2f
                                                            if (dragOffsetY > accumHeight) {
                                                                val newList = announcements.toMutableList()
                                                                java.util.Collections.swap(newList, tempIndex, tempIndex + 1)
                                                                onReorderAnnouncements(newList.map { it.id })
                                                                dragOffsetY -= nextHeight
                                                                tempIndex += 1
                                                                draggedIndex = tempIndex
                                                                accumHeight = height / 2f
                                                            } else {
                                                                break
                                                            }
                                                        }
                                                    } else if (dragOffsetY < 0) {
                                                        var accumHeight = -height / 2f
                                                        while (tempIndex > 0) {
                                                            val prevHeight = itemHeights[tempIndex - 1] ?: 0
                                                            accumHeight -= prevHeight / 2f
                                                            if (dragOffsetY < accumHeight) {
                                                                val newList = announcements.toMutableList()
                                                                java.util.Collections.swap(newList, tempIndex, tempIndex - 1)
                                                                onReorderAnnouncements(newList.map { it.id })
                                                                dragOffsetY += prevHeight
                                                                tempIndex -= 1
                                                                draggedIndex = tempIndex
                                                                accumHeight = -height / 2f
                                                            } else {
                                                                break
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                draggedIndex = null
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                draggedIndex = null
                                                dragOffsetY = 0f
                                            }
                                        )
                                    }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = announcement.content,
                                color = Color.White,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                editTextValue = announcement.content
                                editingAnnouncement = announcement
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit",
                                    tint = Color(0xFF90CAF9)
                                )
                            }
                            IconButton(onClick = { onDeleteAnnouncement(announcement.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Remove",
                                    tint = Color(0xFFE57373)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
