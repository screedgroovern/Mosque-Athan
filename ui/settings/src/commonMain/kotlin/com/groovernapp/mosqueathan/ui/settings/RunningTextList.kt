package com.groovernapp.mosqueathan.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.component.MosqueConfirmDialog
import com.groovernapp.mosqueathan.ui.design.component.MosqueIconButton
import com.groovernapp.mosqueathan.ui.design.component.MosquePrimaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueSecondaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueTextButton
import com.groovernapp.mosqueathan.ui.design.component.mosqueTextFieldColors
import com.groovernapp.mosqueathan.ui.design.focusHighlight
import com.groovernapp.mosqueathan.ui.design.initialFocus
import com.groovernapp.mosqueathan.util.Lang
import com.groovernapp.mosqueathan.util.swap

/** One line of running text. The [id] is whatever the owner needs back to tell rows apart. */
data class RunningTextItem(val id: String, val text: String)

/**
 * The editable list of running-text lines shared by the Dashboard, Adzan and Iqomah panes.
 *
 * It reports by row ([onEdit], [onDelete], [onReorder] all name rows by id) rather than handing the
 * whole list back, because the Dashboard keeps its lines as rows with ids and the Adzan and Iqomah
 * footers keep them as plain strings. Each owner adapts to the shape it has.
 *
 * **Sorting works without a touchscreen.** *Sort* switches the list into a mode where each line is
 * a single focus target: OK picks a line up, Up and Down move it, OK puts it down. Dragging the
 * handle still works for touch. The order is held locally and reported on every move, because two
 * quick presses would otherwise both read the same incoming list and the second would be lost —
 * the problem `AdjustmentRow` buffers against.
 */
@Composable
fun RunningTextList(
    items: List<RunningTextItem>,
    placeholderText: String,
    onAdd: (String) -> Unit,
    onEdit: (id: String, text: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onReorder: (ids: List<String>) -> Unit,
    isCompact: Boolean = LocalWindowClass.current.isCompact
) {
    var order by remember(items) { mutableStateOf(items) }
    val latestOrder by rememberUpdatedState(order)
    val commit by rememberUpdatedState<(List<RunningTextItem>) -> Unit> { newOrder ->
        order = newOrder
        onReorder(newOrder.map { it.id })
    }

    var newText by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }
    var addTextValue by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<RunningTextItem?>(null) }
    var editTextValue by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<RunningTextItem?>(null) }

    var isSortMode by remember { mutableStateOf(false) }
    var grabbedId by remember { mutableStateOf<String?>(null) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<String, Int>() }

    // Nothing to sort with fewer than two lines, and a stale mode would hide the edit buttons.
    LaunchedEffect(order.size) {
        if (order.size < 2) {
            isSortMode = false
            grabbedId = null
        }
    }

    val fieldTextSize = if (isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.bodyCompact

    editing?.let { target ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = {
                Text(Lang.get("Edit Running Text", "Edit Teks Berjalan"), color = MosqueTheme.colors.textPrimary)
            },
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
                        if (editTextValue.isNotBlank()) onEdit(target.id, editTextValue.trim())
                        editing = null
                    }
                ) {
                    Text(Lang.get("Save", "Simpan"), color = MosqueTheme.colors.onAccent)
                }
            },
            dismissButton = {
                MosqueTextButton(onClick = { editing = null }) {
                    Text(Lang.get("Cancel", "Batal"), color = MosqueTheme.colors.textPrimary)
                }
            }
        )
    }

    if (isAddingNew) {
        AlertDialog(
            onDismissRequest = { isAddingNew = false },
            title = {
                Text(Lang.get("Add Running Text", "Tambah Teks Berjalan"), color = MosqueTheme.colors.textPrimary)
            },
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
                        if (addTextValue.isNotBlank()) onAdd(addTextValue.trim())
                        isAddingNew = false
                    }
                ) {
                    Text(Lang.get("Add", "Tambah"), color = MosqueTheme.colors.onAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                MosqueTextButton(onClick = { isAddingNew = false }) {
                    Text(Lang.get("Cancel", "Batal"), color = MosqueTheme.colors.textPrimary)
                }
            }
        )
    }

    pendingDelete?.let { target ->
        MosqueConfirmDialog(
            title = Lang.get("Delete this running text?", "Hapus teks berjalan ini?"),
            message = "\"${target.text}\"",
            confirmText = Lang.get("Delete", "Hapus"),
            destructive = true,
            onConfirm = {
                onDelete(target.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg)
    ) {
        val sortButton: @Composable () -> Unit = {
            MosqueSecondaryButton(
                onClick = {
                    isSortMode = !isSortMode
                    grabbedId = null
                },
                // Drawn from the first line on, but only usable from the second: a list that
                // gained a Sort button only when it reached two lines read as a list that had none.
                // Adzan and Iqomah each ship with a single line.
                enabled = order.size > 1 || isSortMode,
                shape = RoundedCornerShape(MosqueTheme.radius.sm),
                modifier = Modifier.heightIn(min = MosqueTheme.size.controlMd),
                contentPadding = PaddingValues(horizontal = MosqueTheme.spacing.lg)
            ) {
                Text(
                    text = if (isSortMode) Lang.get("Done", "Selesai") else Lang.get("Sort", "Urutkan"),
                    fontWeight = FontWeight.Bold,
                    fontSize = MosqueTheme.typography.label
                )
            }
        }
        val hasItems = order.isNotEmpty()

        if (isCompact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (hasItems) sortButton()
                }
                MosquePrimaryButton(
                    onClick = {
                        isAddingNew = true
                        addTextValue = ""
                    },
                    modifier = Modifier.heightIn(min = MosqueTheme.size.controlMd),
                    shape = RoundedCornerShape(MosqueTheme.radius.sm)
                ) {
                    Text(
                        text = Lang.get("Add", "Tambah"),
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
                            onAdd(newText.trim())
                            newText = ""
                        }
                    },
                    modifier = Modifier.heightIn(min = MosqueTheme.size.controlXl)
                ) {
                    Text(Lang.get("Add", "Tambah"), color = MosqueTheme.colors.onAccent, fontWeight = FontWeight.Bold)
                }
            }
            if (hasItems) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    sortButton()
                }
            }
        }

        if (isSortMode) {
            Text(
                text = Lang.get(
                    "Select a line and press OK to pick it up, move it with Up and Down, then press OK to put it down.",
                    "Pilih satu baris dan tekan OK untuk mengangkatnya, geser dengan Atas dan Bawah, lalu tekan OK untuk meletakkannya."
                ),
                color = MosqueTheme.colors.textSecondary,
                fontSize = MosqueTheme.typography.caption
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
        ) {
            order.forEach { item ->
                key(item.id) {
                    val isDragged = item.id == draggedId
                    val isGrabbed = item.id == grabbedId
                    val rowShape = RoundedCornerShape(MosqueTheme.radius.md)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { heights[item.id] = it.size.height }
                            .zIndex(if (isDragged) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragged) dragOffsetY else 0f }
                            .clip(rowShape)
                            .background(
                                if (isGrabbed) MosqueTheme.colors.accent.copy(alpha = 0.25f)
                                else MosqueTheme.colors.fillSubtle
                            )
                            .then(
                                if (isSortMode) {
                                    Modifier
                                        .onKeyEvent { event ->
                                            if (event.type != KeyEventType.KeyDown || grabbedId != item.id) {
                                                return@onKeyEvent false
                                            }
                                            val delta = when (event.key) {
                                                Key.DirectionUp -> -1
                                                Key.DirectionDown -> 1
                                                else -> return@onKeyEvent false
                                            }
                                            // Consumed even at the ends: a held line that lets Up
                                            // fall through would drop focus onto the row above.
                                            val list = latestOrder
                                            val from = list.indexOfFirst { it.id == item.id }
                                            val to = from + delta
                                            if (from >= 0 && to in list.indices) {
                                                commit(list.toMutableList().apply { swap(from, to) })
                                            }
                                            true
                                        }
                                        .focusHighlight(shape = rowShape, scaleUp = false)
                                        .clickable { grabbedId = if (isGrabbed) null else item.id }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = MosqueTheme.spacing.lg, vertical = MosqueTheme.spacing.md)
                    ) {
                        val showHandle = isSortMode || !isCompact
                        val showActions = !isSortMode

                        if (isCompact && showActions) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = item.text,
                                    color = MosqueTheme.colors.textPrimary,
                                    fontSize = fieldTextSize,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(MosqueTheme.spacing.md))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RowActionButton(
                                        label = Lang.get("Edit", "Edit"),
                                        icon = Icons.Outlined.Edit,
                                        tintIsDanger = false
                                    ) {
                                        editTextValue = item.text
                                        editing = item
                                    }
                                    Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
                                    RowActionButton(
                                        label = Lang.get("Delete", "Hapus"),
                                        icon = Icons.Outlined.Delete,
                                        tintIsDanger = true
                                    ) { pendingDelete = item }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showHandle) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Drag Handle",
                                        tint = if (isGrabbed) MosqueTheme.colors.accent else MosqueTheme.colors.textMuted,
                                        modifier = Modifier
                                            .size(MosqueTheme.size.iconLg)
                                            .pointerInput(item.id) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        draggedId = item.id
                                                        dragOffsetY = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount.y
                                                        // Swap past each neighbour the line has
                                                        // travelled over half of. `list` is advanced
                                                        // by hand: `latestOrder` only catches up on
                                                        // the next recomposition.
                                                        var list = latestOrder
                                                        while (true) {
                                                            val idx = list.indexOfFirst { it.id == item.id }
                                                            if (idx < 0) break
                                                            val own = heights[item.id] ?: 0
                                                            if (dragOffsetY > 0 && idx < list.lastIndex) {
                                                                val next = heights[list[idx + 1].id] ?: 0
                                                                if (dragOffsetY <= (own + next) / 2f) break
                                                                list = list.toMutableList().apply { swap(idx, idx + 1) }
                                                                dragOffsetY -= next
                                                            } else if (dragOffsetY < 0 && idx > 0) {
                                                                val prev = heights[list[idx - 1].id] ?: 0
                                                                if (dragOffsetY >= -(own + prev) / 2f) break
                                                                list = list.toMutableList().apply { swap(idx, idx - 1) }
                                                                dragOffsetY += prev
                                                            } else {
                                                                break
                                                            }
                                                            commit(list)
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        draggedId = null
                                                        dragOffsetY = 0f
                                                    },
                                                    onDragCancel = {
                                                        draggedId = null
                                                        dragOffsetY = 0f
                                                    }
                                                )
                                            }
                                    )
                                    Spacer(modifier = Modifier.width(MosqueTheme.spacing.lg))
                                }
                                Text(
                                    text = item.text,
                                    color = MosqueTheme.colors.textPrimary,
                                    fontSize = fieldTextSize,
                                    modifier = Modifier.weight(1f)
                                )
                                if (showActions) {
                                    Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
                                    MosqueIconButton(onClick = {
                                        editTextValue = item.text
                                        editing = item
                                    }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = "Edit",
                                            tint = MosqueTheme.colors.info
                                        )
                                    }
                                    MosqueIconButton(onClick = { pendingDelete = item }) {
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
    }
}

@Composable
private fun RowActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tintIsDanger: Boolean,
    onClick: () -> Unit
) {
    val tint = if (tintIsDanger) MosqueTheme.colors.danger else MosqueTheme.colors.info
    MosqueTextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = MosqueTheme.spacing.lg, vertical = MosqueTheme.spacing.xs),
        modifier = Modifier.heightIn(min = MosqueTheme.size.controlSm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(MosqueTheme.size.iconSm)
        )
        Spacer(modifier = Modifier.width(MosqueTheme.spacing.xs))
        Text(
            text = label,
            color = tint,
            fontSize = MosqueTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}
