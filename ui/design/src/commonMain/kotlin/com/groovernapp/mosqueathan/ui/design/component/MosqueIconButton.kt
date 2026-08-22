package com.groovernapp.mosqueathan.ui.design.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.groovernapp.mosqueathan.ui.design.MosqueColors
import com.groovernapp.mosqueathan.ui.design.focusHighlight

/**
 * An icon-only button that shows D-pad focus.
 *
 * Material3's `IconButton` indicates press but not focus, so on a television every
 * icon in a row looked identical no matter which one the remote had landed on. This
 * is the same control with [focusHighlight] applied, circular to match the icon
 * button's own ripple bounds.
 */
@Composable
fun MosqueIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(
        contentColor = MosqueColors.textPrimary
    ),
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.focusHighlight(shape = CircleShape, scaleUp = false),
        enabled = enabled,
        colors = colors,
        content = content
    )
}
