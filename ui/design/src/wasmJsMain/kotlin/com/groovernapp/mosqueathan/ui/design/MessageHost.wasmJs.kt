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
 * A `Snackbar`, as on the desktop. The browser has no OS-level transient message, and the
 * focus objection that makes a `Toast` right on a television does not apply to a page
 * driven by a pointer.
 */
@Composable
actual fun MessageHost(messages: Flow<String>) {
    val host = remember { SnackbarHostState() }
    LaunchedEffect(messages) {
        messages.collect { host.showSnackbar(it) }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(hostState = host)
    }
}
