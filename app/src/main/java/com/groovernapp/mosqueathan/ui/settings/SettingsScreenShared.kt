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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

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
