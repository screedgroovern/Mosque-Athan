package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.Composable

/**
 * Always false. The television role is permanently Android-only — a desktop build is a
 * controller, never the mosque display — so there is nothing to detect here.
 */
@Composable
actual fun rememberIsTv(): Boolean = false
