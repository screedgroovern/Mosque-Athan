package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * Takes focus as soon as it appears.
 *
 * A dialog that opens with nothing focused leaves a D-pad user stuck: the first
 * arrow press has no focused element to move away from, so it either does nothing
 * or jumps somewhere arbitrary behind the dialog. Put this on the confirm action so
 * Enter works immediately and the arrows have a starting point.
 *
 * `requestFocus` throws if the node is not attached yet — which can happen when a
 * dialog is dismissed in the same frame it appears — so the call is guarded rather
 * than crashing the settings screen.
 */
fun Modifier.initialFocus(): Modifier = composed {
    val requester = remember { FocusRequester() }
    LaunchedEffect(requester) {
        runCatching { requester.requestFocus() }
    }
    focusRequester(requester)
}
