package com.groovernapp.mosqueathan.ui.design

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape

/**
 * Makes D-pad focus visible.
 *
 * On a television the pointer is a set of arrow keys, so an element that gives no
 * sign of being focused is effectively unreachable — the user presses right, nothing
 * appears to happen, and they stop trusting the remote. Material3's default
 * indication is a ripple, which only shows on *press*, not on focus.
 *
 * Apply this to anything focusable that is not one of the `Mosque*Button` variants
 * (they carry the same treatment internally): icon buttons, switches, clickable
 * rows, menu items.
 *
 * `OutlinedTextField` does not need it — `mosqueTextFieldColors` already turns the
 * border gold on focus.
 *
 * @param shape must match the element's own shape, or the highlight will not sit on
 *   its edge.
 * @param scaleUp lift the element slightly as well. Leave it off inside a list or a
 *   tight row, where a growing item shoves its neighbours.
 */
fun Modifier.focusHighlight(
    shape: Shape = RoundedCornerShape(MosqueRadius.md),
    scaleUp: Boolean = true
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }

    this
        .onFocusChanged { focused = it.isFocused || it.hasFocus }
        .then(
            if (focused) {
                Modifier.border(MosqueStroke.thick, MosqueColors.accent, shape)
            } else {
                Modifier
            }
        )
        .then(if (focused && scaleUp) Modifier.scale(FOCUS_SCALE) else Modifier)
}

/** Kept in step with the scale used by the `Mosque*Button` variants. */
internal const val FOCUS_SCALE = 1.05f
