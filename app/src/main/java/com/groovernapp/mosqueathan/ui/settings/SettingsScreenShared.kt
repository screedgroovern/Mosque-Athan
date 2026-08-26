package com.groovernapp.mosqueathan.ui.settings

import com.groovernapp.mosqueathan.util.swap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import com.groovernapp.mosqueathan.ui.design.component.MosquePrimaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosquePreviewButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueDangerButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueSecondaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueSelectableButton
import com.groovernapp.mosqueathan.ui.design.component.mosqueTextFieldColors
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import com.groovernapp.mosqueathan.ui.design.component.MosqueIconButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueTextButton
import com.groovernapp.mosqueathan.ui.design.initialFocus
@Composable
fun RunningTextListSettingsComponent(
    title: String,
    subtitle: String,
    items: List<String>,
    onItemsChange: (List<String>) -> Unit,
    placeholderText: String,
    isCompact: Boolean = false
) {
    var newText by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editTextValue by remember { mutableStateOf("") }
    var isReorderMode by remember { mutableStateOf(false) }
    var isAddingNew by remember { mutableStateOf(false) }
    var addTextValue by remember { mutableStateOf("") }

    if (editingIndex != null) {
        AlertDialog(
            onDismissRequest = { editingIndex = null },
            title = { Text(com.groovernapp.mosqueathan.util.Lang.get("Edit Running Text", "Edit Teks Berjalan"), color = MosqueTheme.colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = editTextValue,
                    onValueChange = { editTextValue = it },
                    colors = mosqueTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                MosquePrimaryButton(
                    modifier = Modifier.initialFocus(),
                    onClick = {
                        editingIndex?.let { index ->
                            if (editTextValue.isNotBlank()) {
                                val updated = items.toMutableList().apply { set(index, editTextValue.trim()) }
                                onItemsChange(updated)
                            }
                        }
                        editingIndex = null
                    }
                ) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Save", "Simpan"), color = MosqueTheme.colors.onAccent)
                }
            },
            dismissButton = {
                MosqueTextButton(onClick = { editingIndex = null }) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Cancel", "Batal"), color = MosqueTheme.colors.textPrimary)
                }
            }
        )
    }

    if (isAddingNew) {
        AlertDialog(
            onDismissRequest = { isAddingNew = false },
            title = { Text(com.groovernapp.mosqueathan.util.Lang.get("Add Running Text", "Tambah Teks Berjalan"), color = MosqueTheme.colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = addTextValue,
                    onValueChange = { addTextValue = it },
                    colors = mosqueTextFieldColors(),
                    singleLine = true,
                    placeholder = { Text(placeholderText) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                MosquePrimaryButton(
                    modifier = Modifier.initialFocus(),
                    onClick = {
                        if (addTextValue.isNotBlank()) {
                            onItemsChange(items + addTextValue.trim())
                        }
                        isAddingNew = false
                    }
                ) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Add", "Tambah"), color = MosqueTheme.colors.onAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                MosqueTextButton(onClick = { isAddingNew = false }) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Cancel", "Batal"), color = MosqueTheme.colors.textPrimary)
                }
            }
        )
    }

    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = MosqueTheme.colors.accent,
                fontSize = if (isCompact) MosqueTheme.typography.bodyCompact else MosqueTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Column {
            Text(
                text = subtitle,
                color = MosqueTheme.colors.textPrimary,
                fontSize = if (isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.bodyCompact,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(MosqueTheme.spacing.md))
            if (isCompact) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Arrange Button aligned left
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (items.isNotEmpty()) {
                            MosqueSecondaryButton(
                                onClick = { isReorderMode = !isReorderMode },
                                shape = RoundedCornerShape(MosqueTheme.radius.sm),
                                modifier = Modifier.height(MosqueTheme.size.controlMd),
                                contentPadding = PaddingValues(horizontal = MosqueTheme.spacing.lg)
                            ) {
                                Text(
                                    text = if (isReorderMode) com.groovernapp.mosqueathan.util.Lang.get("Done", "Selesai") else com.groovernapp.mosqueathan.util.Lang.get("Arrange", "Urutkan"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = MosqueTheme.typography.label
                                )
                            }
                        }
                    }

                    // Add Button aligned right (smaller)
                    MosquePrimaryButton(
                        onClick = {
                            isAddingNew = true
                            addTextValue = ""
                        },
                        modifier = Modifier.height(MosqueTheme.size.controlMd),
                        shape = RoundedCornerShape(MosqueTheme.radius.sm)
                    ) {
                        Text(
                            text = com.groovernapp.mosqueathan.util.Lang.get("Add", "Tambah"),
                            color = MosqueTheme.colors.onAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = MosqueTheme.typography.label
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newText,
                        onValueChange = { newText = it },
                        placeholder = { Text(placeholderText) },
                        colors = mosqueTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    MosquePrimaryButton(
                        onClick = {
                            if (newText.isNotBlank()) {
                                onItemsChange(items + newText.trim())
                                newText = ""
                            }
                        },
                        modifier = Modifier.height(MosqueTheme.size.controlXl)
                    ) {
                        Text(com.groovernapp.mosqueathan.util.Lang.get("Add", "Tambah"), color = MosqueTheme.colors.onAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
        ) {
            items.forEachIndexed { index, itemText ->
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
                        .clip(RoundedCornerShape(MosqueTheme.radius.md))
                        .background(MosqueTheme.colors.fillSubtle)
                        .padding(horizontal = MosqueTheme.spacing.lg, vertical = MosqueTheme.spacing.md)
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
                                    tint = MosqueTheme.colors.textMuted,
                                    modifier = Modifier
                                        .size(MosqueTheme.size.iconLg)
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
                                                            while (tempIndex < items.size - 1) {
                                                                val nextHeight = itemHeights[tempIndex + 1] ?: 0
                                                                accumHeight += nextHeight / 2f
                                                                if (dragOffsetY > accumHeight) {
                                                                    val newList = items.toMutableList()
                                                                    newList.swap(tempIndex, tempIndex + 1)
                                                                    onItemsChange(newList)
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
                                                                    val newList = items.toMutableList()
                                                                    newList.swap(tempIndex, tempIndex - 1)
                                                                    onItemsChange(newList)
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
                                Spacer(modifier = Modifier.width(MosqueTheme.spacing.lg))
                                Text(
                                    text = itemText,
                                    color = MosqueTheme.colors.textPrimary,
                                    fontSize = if (isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.bodyCompact,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = itemText,
                                    color = MosqueTheme.colors.textPrimary,
                                    fontSize = if (isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.bodyCompact,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(MosqueTheme.spacing.md))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MosqueTextButton(
                                        onClick = {
                                            editTextValue = itemText
                                            editingIndex = index
                                        },
                                        contentPadding = PaddingValues(horizontal = MosqueTheme.spacing.lg, vertical = MosqueTheme.spacing.xs),
                                        modifier = Modifier.height(MosqueTheme.size.controlSm)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = "Edit",
                                            tint = MosqueTheme.colors.info,
                                            modifier = Modifier.size(MosqueTheme.size.iconSm)
                                        )
                                        Spacer(modifier = Modifier.width(MosqueTheme.spacing.xs))
                                        Text(
                                            text = com.groovernapp.mosqueathan.util.Lang.get("Edit", "Edit"),
                                            color = MosqueTheme.colors.info,
                                            fontSize = MosqueTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
                                    MosqueTextButton(
                                        onClick = {
                                            val updated = items.toMutableList().apply { removeAt(index) }
                                            onItemsChange(updated)
                                        },
                                        contentPadding = PaddingValues(horizontal = MosqueTheme.spacing.lg, vertical = MosqueTheme.spacing.xs),
                                        modifier = Modifier.height(MosqueTheme.size.controlSm)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Remove",
                                            tint = MosqueTheme.colors.danger,
                                            modifier = Modifier.size(MosqueTheme.size.iconSm)
                                        )
                                        Spacer(modifier = Modifier.width(MosqueTheme.spacing.xs))
                                        Text(
                                            text = com.groovernapp.mosqueathan.util.Lang.get("Delete", "Hapus"),
                                            color = MosqueTheme.colors.danger,
                                            fontSize = MosqueTheme.typography.labelSmall,
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
                                tint = MosqueTheme.colors.textMuted,
                                modifier = Modifier
                                    .size(MosqueTheme.size.iconLg)
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
                                                        while (tempIndex < items.size - 1) {
                                                            val nextHeight = itemHeights[tempIndex + 1] ?: 0
                                                            accumHeight += nextHeight / 2f
                                                            if (dragOffsetY > accumHeight) {
                                                                val newList = items.toMutableList()
                                                                newList.swap(tempIndex, tempIndex + 1)
                                                                onItemsChange(newList)
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
                                                                val newList = items.toMutableList()
                                                                newList.swap(tempIndex, tempIndex - 1)
                                                                onItemsChange(newList)
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
                            Spacer(modifier = Modifier.width(MosqueTheme.spacing.lg))
                            Text(
                                text = itemText,
                                color = MosqueTheme.colors.textPrimary,
                                fontSize = MosqueTheme.typography.bodyCompact,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
                            MosqueIconButton(onClick = {
                                editTextValue = itemText
                                editingIndex = index
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit",
                                    tint = MosqueTheme.colors.info
                                )
                            }
                            MosqueIconButton(onClick = {
                                val updated = items.toMutableList().apply { removeAt(index) }
                                onItemsChange(updated)
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Remove",
                                    tint = MosqueTheme.colors.danger
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdjustmentRow(
    title: String,
    value: Int,
    minValue: Int = 0,
    maxValue: Int = 60,
    showSign: Boolean = false,
    unitSuffix: String = com.groovernapp.mosqueathan.util.Lang.get("minutes", "menit"),
    onValueChange: (Int) -> Unit
) {
    // The displayed value is buffered locally and re-keyed on [value].
    //
    // Without this, two quick taps both read the same incoming `value` — the second
    // recomputes the same result and is lost, because the new value has not made it
    // back through Realm and the state flow yet. Buffering belongs here rather than
    // in each screen: every caller needs it.
    var shown by remember(value) { mutableIntStateOf(value) }

    fun emit(next: Int) {
        shown = next
        onValueChange(next)
    }

    val containerSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val isCompact = LocalWindowClass.current.isCompact

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
                horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
            ) {
                MosqueSecondaryButton(
                    onClick = { if (shown > minValue) emit(shown - 1) },
                    shape = RoundedCornerShape(MosqueTheme.radius.sm),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(MosqueTheme.size.controlMd)
                ) {
                    Text("-", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
     
                val sign = if (showSign && shown > 0) "+" else ""
                Text(
                    text = "$sign$shown",
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.bodyCompact,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(MosqueTheme.size.controlMd),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
     
                MosqueSecondaryButton(
                    onClick = { if (shown < maxValue) emit(shown + 1) },
                    shape = RoundedCornerShape(MosqueTheme.radius.sm),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(MosqueTheme.size.controlMd)
                ) {
                    Text("+", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                if (unitSuffix.isNotEmpty()) {
                    Text(
                        text = unitSuffix,
                        color = MosqueTheme.colors.textSecondary,
                        fontSize = MosqueTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                    )
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
                    onClick = { if (shown > minValue) emit(shown - 1) },
                    shape = RoundedCornerShape(MosqueTheme.radius.sm),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(MosqueTheme.size.controlMd)
                ) {
                    Text("-", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
     
                val sign = if (showSign && shown > 0) "+" else ""
                Text(
                    text = "$sign$shown",
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(MosqueTheme.size.controlMd),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
     
                MosqueSecondaryButton(
                    onClick = { if (shown < maxValue) emit(shown + 1) },
                    shape = RoundedCornerShape(MosqueTheme.radius.sm),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(MosqueTheme.size.controlMd)
                ) {
                    Text("+", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                if (unitSuffix.isNotEmpty()) {
                    Text(
                        text = unitSuffix,
                        color = MosqueTheme.colors.textSecondary,
                        fontSize = MosqueTheme.typography.bodyCompact,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
