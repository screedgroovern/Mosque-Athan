package com.groovernapp.mosqueathan.ui.settings

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete

@Composable
fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFFFFD54F),
    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
    focusedLabelColor = Color(0xFFFFD54F),
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    cursorColor = Color(0xFFFFD54F),
    disabledTextColor = Color.White.copy(alpha = 0.38f),
    disabledBorderColor = Color.White.copy(alpha = 0.12f),
    disabledLabelColor = Color.White.copy(alpha = 0.38f),
    disabledLeadingIconColor = Color.White.copy(alpha = 0.38f),
    disabledTrailingIconColor = Color.White.copy(alpha = 0.38f)
)

/**
 * A TV-friendly button that provides a highly visible hover/focus effect.
 * When focused or hovered, it shows:
 * - A bright yellow border (3dp)
 * - Brighter container color
 * - Slight scale-up animation (1.05x)
 *
 * This makes it easy for TV users with D-pad/remote to see which button
 * currently has focus.
 */
@Composable
fun TvFocusableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
    shape: Shape = RoundedCornerShape(6.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocusedOrHovered = isFocused || isHovered

    // Determine focus-adjusted colors
    val focusBorder = if (isFocusedOrHovered) {
        BorderStroke(3.dp, Color(0xFFFFD54F))
    } else {
        null
    }

    val focusScale = if (isFocusedOrHovered) 1.05f else 1.0f

    // When focused/hovered, brighten the container color
    val effectiveColors = if (isFocusedOrHovered) {
        ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD54F).copy(alpha = 0.35f)
        )
    } else {
        colors
    }

    Button(
        onClick = onClick,
        modifier = modifier.scale(focusScale),
        colors = effectiveColors,
        shape = shape,
        contentPadding = contentPadding,
        enabled = enabled,
        interactionSource = interactionSource,
        border = focusBorder,
        content = content
    )
}

/**
 * A TV-friendly primary (gold/yellow) button with a visible hover/focus effect.
 * When focused/hovered, it shows a bright white border and slight scale-up.
 */
@Composable
fun TvFocusablePrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocusedOrHovered = isFocused || isHovered

    val focusBorder = if (isFocusedOrHovered) {
        BorderStroke(3.dp, Color.White)
    } else {
        null
    }

    val focusScale = if (isFocusedOrHovered) 1.05f else 1.0f

    val effectiveColors = if (isFocusedOrHovered) {
        ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE082))
    } else {
        ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
    }

    Button(
        onClick = onClick,
        modifier = modifier.scale(focusScale),
        colors = effectiveColors,
        shape = shape,
        contentPadding = contentPadding,
        enabled = enabled,
        interactionSource = interactionSource,
        border = focusBorder,
        content = content
    )
}

@Composable
fun TvFocusablePreviewButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocusedOrHovered = isFocused || isHovered

    val focusBorder = if (isFocusedOrHovered) {
        BorderStroke(3.dp, Color(0xFFFFD54F))
    } else {
        BorderStroke(1.5.dp, Color(0xFFFFD54F).copy(alpha = 0.6f))
    }

    val focusScale = if (isFocusedOrHovered) 1.05f else 1.0f

    val effectiveColors = if (isFocusedOrHovered) {
        ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD54F).copy(alpha = 0.25f)
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD54F).copy(alpha = 0.08f)
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier.scale(focusScale),
        colors = effectiveColors,
        shape = shape,
        contentPadding = contentPadding,
        enabled = enabled,
        interactionSource = interactionSource,
        border = focusBorder,
        content = content
    )
}

/**
 * A TV-friendly danger (red) button with a visible hover/focus effect.
 * When focused/hovered, it shows a bright yellow border and brighter red.
 */
@Composable
fun TvFocusableDangerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocusedOrHovered = isFocused || isHovered

    val focusBorder = if (isFocusedOrHovered) {
        BorderStroke(3.dp, Color(0xFFFFD54F))
    } else {
        null
    }

    val focusScale = if (isFocusedOrHovered) 1.05f else 1.0f

    val effectiveColors = if (isFocusedOrHovered) {
        ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.9f))
    } else {
        ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
    }

    Button(
        onClick = onClick,
        modifier = modifier.scale(focusScale),
        colors = effectiveColors,
        shape = shape,
        contentPadding = contentPadding,
        enabled = enabled,
        interactionSource = interactionSource,
        border = focusBorder,
        content = content
    )
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
    val density = androidx.compose.ui.platform.LocalDensity.current
    val containerSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val screenWidthDp = with(density) { containerSize.width.toDp() }
    val isCompact = screenWidthDp < 600.dp

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
     
                val sign = if (showSign && value > 0) "+" else ""
                Text(
                    text = "$sign$value",
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
                
                if (unitSuffix.isNotEmpty()) {
                    Text(
                        text = unitSuffix,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
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
     
                val sign = if (showSign && value > 0) "+" else ""
                Text(
                    text = "$sign$value",
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
                
                if (unitSuffix.isNotEmpty()) {
                    Text(
                        text = unitSuffix,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

fun parseFooterTexts(raw: String): List<String> {
    if (raw.trim().startsWith("[")) {
        try {
            val arr = org.json.JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            return list
        } catch (e: Exception) {
            // fallback
        }
    }
    return if (raw.isBlank()) emptyList() else listOf(raw)
}

fun formatFooterTexts(list: List<String>): String {
    val arr = org.json.JSONArray()
    list.forEach { arr.put(it) }
    return arr.toString()
}

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
            title = { Text(com.groovernapp.mosqueathan.util.Lang.get("Edit Running Text", "Edit Teks Berjalan"), color = Color.White) },
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
                        editingIndex?.let { index ->
                            if (editTextValue.isNotBlank()) {
                                val updated = items.toMutableList().apply { set(index, editTextValue.trim()) }
                                onItemsChange(updated)
                            }
                        }
                        editingIndex = null
                    }
                ) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Save", "Simpan"), color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingIndex = null }) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Cancel", "Batal"), color = Color.White)
                }
            }
        )
    }

    if (isAddingNew) {
        AlertDialog(
            onDismissRequest = { isAddingNew = false },
            title = { Text(com.groovernapp.mosqueathan.util.Lang.get("Add Running Text", "Tambah Teks Berjalan"), color = Color.White) },
            text = {
                OutlinedTextField(
                    value = addTextValue,
                    onValueChange = { addTextValue = it },
                    colors = textFieldColors(),
                    singleLine = true,
                    placeholder = { Text(placeholderText) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TvFocusablePrimaryButton(
                    onClick = {
                        if (addTextValue.isNotBlank()) {
                            onItemsChange(items + addTextValue.trim())
                        }
                        isAddingNew = false
                    }
                ) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Add", "Tambah"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingNew = false }) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Cancel", "Batal"), color = Color.White)
                }
            }
        )
    }

    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color(0xFFFFD54F),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column {
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (isCompact) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Arrange Button aligned left
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (items.isNotEmpty()) {
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
                                    text = if (isReorderMode) com.groovernapp.mosqueathan.util.Lang.get("Done", "Selesai") else com.groovernapp.mosqueathan.util.Lang.get("Arrange", "Urutkan"),
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
                            text = com.groovernapp.mosqueathan.util.Lang.get("Add", "Tambah"),
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
                        value = newText,
                        onValueChange = { newText = it },
                        placeholder = { Text(placeholderText) },
                        colors = textFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TvFocusablePrimaryButton(
                        onClick = {
                            if (newText.isNotBlank()) {
                                onItemsChange(items + newText.trim())
                                newText = ""
                            }
                        },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(com.groovernapp.mosqueathan.util.Lang.get("Add", "Tambah"), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                                            while (tempIndex < items.size - 1) {
                                                                val nextHeight = itemHeights[tempIndex + 1] ?: 0
                                                                accumHeight += nextHeight / 2f
                                                                if (dragOffsetY > accumHeight) {
                                                                    val newList = items.toMutableList()
                                                                    java.util.Collections.swap(newList, tempIndex, tempIndex + 1)
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
                                                                    java.util.Collections.swap(newList, tempIndex, tempIndex - 1)
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
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = itemText,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = itemText,
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
                                            editTextValue = itemText
                                            editingIndex = index
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
                                            text = com.groovernapp.mosqueathan.util.Lang.get("Edit", "Edit"),
                                            color = Color(0xFF90CAF9),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = {
                                            val updated = items.toMutableList().apply { removeAt(index) }
                                            onItemsChange(updated)
                                        },
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
                                            text = com.groovernapp.mosqueathan.util.Lang.get("Delete", "Hapus"),
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
                                                        while (tempIndex < items.size - 1) {
                                                            val nextHeight = itemHeights[tempIndex + 1] ?: 0
                                                            accumHeight += nextHeight / 2f
                                                            if (dragOffsetY > accumHeight) {
                                                                val newList = items.toMutableList()
                                                                java.util.Collections.swap(newList, tempIndex, tempIndex + 1)
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
                                                                java.util.Collections.swap(newList, tempIndex, tempIndex - 1)
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
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = itemText,
                                color = Color.White,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                editTextValue = itemText
                                editingIndex = index
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit",
                                    tint = Color(0xFF90CAF9)
                                )
                            }
                            IconButton(onClick = {
                                val updated = items.toMutableList().apply { removeAt(index) }
                                onItemsChange(updated)
                            }) {
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
