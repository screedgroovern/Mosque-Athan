package com.groovernapp.mosqueathan.ui.design

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow

/**
 * Draws nothing: a `Toast` is its own window, so there is no node to place here.
 *
 * `LENGTH_SHORT` and the collection order are the behaviour the settings screen already
 * shipped; the queueing is the framework's.
 */
@Composable
actual fun MessageHost(messages: Flow<String>) {
    val context = LocalContext.current
    LaunchedEffect(messages, context) {
        messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
