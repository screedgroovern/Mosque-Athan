package com.groovernapp.mosqueathan.ui.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow

/**
 * A desktop window has no OS-level transient message, so it draws its own.
 *
 * `showSnackbar` suspends until the message is dismissed, which is what serialises a
 * burst of them instead of letting the last one win.
 */
@Composable
actual fun MessageHost(messages: Flow<String>) {
    val hostState = remember { SnackbarHostState() }
    LaunchedEffect(messages, hostState) {
        messages.collect { message ->
            hostState.showSnackbar(message)
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        SnackbarHost(hostState)
    }
}
