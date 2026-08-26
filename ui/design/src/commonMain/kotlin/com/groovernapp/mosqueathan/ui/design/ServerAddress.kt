package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * This device's address on the local network, or null if it has none.
 *
 * Provided by the host, like [LocalAppVersion] and [LocalDisplayBackgrounds]. Enumerating
 * network interfaces needs `java.net.NetworkInterface` on Android, something else on iOS,
 * and is simply unanswerable in a browser — so the screen that displays the address is
 * the wrong place to work it out.
 *
 * Null is a real state, not an error: on a device with no network there is no address to
 * pair against, and the settings pane already says so rather than showing a broken QR
 * code.
 */
val LocalServerAddress: ProvidableCompositionLocal<String?> = staticCompositionLocalOf { null }

/** This device's LAN address. Shorthand for [LocalServerAddress]. */
val serverAddress: String?
    @Composable get() = LocalServerAddress.current
