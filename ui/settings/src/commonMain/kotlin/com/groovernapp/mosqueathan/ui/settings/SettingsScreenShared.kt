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
import androidx.compose.runtime.mutableIntStateOf
import com.groovernapp.mosqueathan.ui.design.component.MosquePreviewButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueDangerButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueSecondaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueSelectableButton
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
/**
 * The Adzan and Iqomah footer lists: a title and subtitle over the shared [RunningTextList].
 *
 * The footers are stored as plain strings, with nothing to tell two lines apart but their text, so
 * each row's id is its text plus how many identical lines came before it. That keeps a line's
 * identity steady while it is moved — an index would change under it on every step and drop the line
 * a D-pad user is holding.
 */
@Composable
fun RunningTextListSettingsComponent(
    title: String,
    subtitle: String,
    items: List<String>,
    onItemsChange: (List<String>) -> Unit,
    placeholderText: String,
    isCompact: Boolean = false
) {
    val ids = remember(items) { items.occurrenceIds() }
    val rows = remember(items) { items.mapIndexed { index, text -> RunningTextItem(ids[index], text) } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg)
    ) {
        Text(
            text = title,
            color = MosqueTheme.colors.accent,
            fontSize = if (isCompact) MosqueTheme.typography.bodyCompact else MosqueTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Column {
            Text(
                text = subtitle,
                color = MosqueTheme.colors.textPrimary,
                fontSize = if (isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.bodyCompact,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(MosqueTheme.spacing.md))
            RunningTextList(
                items = rows,
                placeholderText = placeholderText,
                onAdd = { onItemsChange(items + it) },
                onEdit = { id, text ->
                    val index = ids.indexOf(id)
                    if (index >= 0) onItemsChange(items.toMutableList().apply { set(index, text) })
                },
                onDelete = { id ->
                    val index = ids.indexOf(id)
                    if (index >= 0) onItemsChange(items.toMutableList().apply { removeAt(index) })
                },
                onReorder = { order -> onItemsChange(order.mapNotNull { id -> items.getOrNull(ids.indexOf(id)) }) },
                isCompact = isCompact
            )
        }
    }
}

private fun List<String>.occurrenceIds(): List<String> {
    val seen = mutableMapOf<String, Int>()
    return map { text ->
        val count = seen[text] ?: 0
        seen[text] = count + 1
        "$text#$count"
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
