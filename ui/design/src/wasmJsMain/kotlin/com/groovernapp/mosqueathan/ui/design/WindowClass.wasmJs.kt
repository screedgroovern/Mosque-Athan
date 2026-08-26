package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.Composable

/**
 * Always false. The browser build is a controller; the television role is permanently
 * Android-only. A decision, not a stub.
 */
@Composable
actual fun rememberIsTv(): Boolean = false
