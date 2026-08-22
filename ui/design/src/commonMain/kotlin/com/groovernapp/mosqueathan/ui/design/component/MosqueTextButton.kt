package com.groovernapp.mosqueathan.ui.design.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.groovernapp.mosqueathan.ui.design.MosqueColors
import com.groovernapp.mosqueathan.ui.design.MosqueRadius
import com.groovernapp.mosqueathan.ui.design.focusHighlight

/**
 * A text-only action — dialog confirm and dismiss, mostly.
 *
 * These are where D-pad focus matters most: a dialog opens with two text actions
 * side by side and, without a highlight, nothing on screen says which one Enter will
 * press. Material3 indicates press, not focus.
 */
@Composable
fun MosqueTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.focusHighlight(
            shape = RoundedCornerShape(MosqueRadius.sm),
            scaleUp = false
        ),
        enabled = enabled,
        contentPadding = contentPadding,
        colors = ButtonDefaults.textButtonColors(contentColor = MosqueColors.accent),
        content = content
    )
}
