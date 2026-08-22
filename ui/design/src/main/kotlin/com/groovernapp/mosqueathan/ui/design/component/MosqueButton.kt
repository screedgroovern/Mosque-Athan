package com.groovernapp.mosqueathan.ui.design.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

/**
 * Buttons, in five semantic variants.
 *
 * These replace four `TvFocusable*` wrappers that each carried their own copy of
 * the same focus handling. The variants are unchanged in appearance — this is a
 * de-duplication, not a restyle — with one addition, [MosqueTertiaryButton], for
 * the bordered look that call sites were building out of raw `OutlinedButton`.
 *
 * Every variant handles D-pad focus, which is the whole reason these exist: raw
 * Material3 gives a television user no indication of what is selected. Focused or
 * hovered, a button gains a 3dp border and scales to 1.05.
 */

/** Resting and focused container colours for one variant. */
private data class Fill(val resting: Color, val focused: Color)

@Composable
private fun FocusableButton(
    onClick: () -> Unit,
    fill: Fill,
    contentColor: Color,
    modifier: Modifier,
    shape: Shape,
    contentPadding: PaddingValues,
    enabled: Boolean,
    restingBorder: BorderStroke?,
    focusBorderColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val active = isFocused || isHovered

    Button(
        onClick = onClick,
        modifier = modifier.scale(if (active) FOCUS_SCALE else 1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) fill.focused else fill.resting,
            contentColor = contentColor
        ),
        shape = shape,
        contentPadding = contentPadding,
        enabled = enabled,
        interactionSource = interactionSource,
        border = if (active) {
            BorderStroke(MosqueTheme.stroke.thick, focusBorderColor)
        } else {
            restingBorder
        },
        content = content
    )
}

private const val FOCUS_SCALE = 1.05f

/** The main action on a screen. Gold, with black content. */
@Composable
fun MosquePrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MosqueTheme.radius.md),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) = FocusableButton(
    onClick = onClick,
    fill = Fill(MosqueTheme.colors.accent, MosqueTheme.colors.accent),
    contentColor = MosqueTheme.colors.onAccent,
    modifier = modifier,
    shape = shape,
    contentPadding = contentPadding,
    enabled = enabled,
    restingBorder = null,
    focusBorderColor = MosqueTheme.colors.textPrimary,
    content = content
)

/** Supporting actions. A neutral fill. */
@Composable
fun MosqueSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MosqueTheme.radius.md),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) = FocusableButton(
    onClick = onClick,
    fill = Fill(MosqueTheme.colors.fillStrong, MosqueTheme.colors.accent.copy(alpha = 0.35f)),
    contentColor = MosqueTheme.colors.textPrimary,
    modifier = modifier,
    shape = shape,
    contentPadding = contentPadding,
    enabled = enabled,
    restingBorder = null,
    focusBorderColor = MosqueTheme.colors.accent,
    content = content
)

/**
 * Optional actions — bordered, no fill.
 *
 * New. Call sites were assembling this out of raw `OutlinedButton`, which meant
 * they had no focus handling at all.
 */
@Composable
fun MosqueTertiaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MosqueTheme.radius.md),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) = FocusableButton(
    onClick = onClick,
    fill = Fill(MosqueTheme.colors.transparent, MosqueTheme.colors.fill),
    contentColor = MosqueTheme.colors.textPrimary,
    modifier = modifier,
    shape = shape,
    contentPadding = contentPadding,
    enabled = enabled,
    restingBorder = BorderStroke(MosqueTheme.stroke.hairline, MosqueTheme.colors.border),
    focusBorderColor = MosqueTheme.colors.accent,
    content = content
)

/** Adzan and iqomah preview. Tinted gold, and bordered even at rest. */
@Composable
fun MosquePreviewButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MosqueTheme.radius.md),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) = FocusableButton(
    onClick = onClick,
    fill = Fill(
        MosqueTheme.colors.accent.copy(alpha = 0.08f),
        MosqueTheme.colors.accent.copy(alpha = 0.25f)
    ),
    contentColor = MosqueTheme.colors.textPrimary,
    modifier = modifier,
    shape = shape,
    contentPadding = contentPadding,
    enabled = enabled,
    restingBorder = BorderStroke(
        MosqueTheme.stroke.medium,
        MosqueTheme.colors.accent.copy(alpha = 0.6f)
    ),
    focusBorderColor = MosqueTheme.colors.accent,
    content = content
)

/** Reset, delete, and anything else that cannot be undone. */
@Composable
fun MosqueDangerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MosqueTheme.radius.sm),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) = FocusableButton(
    onClick = onClick,
    fill = Fill(
        MosqueTheme.colors.danger.copy(alpha = 0.7f),
        MosqueTheme.colors.danger.copy(alpha = 0.9f)
    ),
    contentColor = MosqueTheme.colors.textPrimary,
    modifier = modifier,
    shape = shape,
    contentPadding = contentPadding,
    enabled = enabled,
    restingBorder = null,
    focusBorderColor = MosqueTheme.colors.accent,
    content = content
)

/**
 * One option in a segmented choice — transition type, day of week, aspect ratio.
 *
 * Selection is a parameter rather than a colour argument because that is what the
 * call sites were expressing: every one of them wrote
 * `if (isSelected) accent else fill` by hand, and had to remember to flip the
 * content colour to match.
 */
@Composable
fun MosqueSelectableButton(
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MosqueTheme.radius.sm),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) = FocusableButton(
    onClick = onClick,
    fill = if (selected) {
        Fill(MosqueTheme.colors.accent, MosqueTheme.colors.accent)
    } else {
        Fill(MosqueTheme.colors.fill, MosqueTheme.colors.accent.copy(alpha = 0.35f))
    },
    contentColor = if (selected) MosqueTheme.colors.onAccent else MosqueTheme.colors.textPrimary,
    modifier = modifier,
    shape = shape,
    contentPadding = contentPadding,
    enabled = enabled,
    restingBorder = null,
    focusBorderColor = if (selected) MosqueTheme.colors.textPrimary else MosqueTheme.colors.accent,
    content = content
)
