package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether this host can be the display's server.
 *
 * Provided by the host, like [LocalAppVersion] and [LocalLinkStatus], because only the host knows:
 * a browser cannot open a listening socket, and the iPhone case is that same browser panel, so
 * neither can ever serve. Offering "Server Mode" there is a choice that does nothing. Android is
 * the one platform that can, so that is the default and the browser says otherwise.
 */
val LocalCanServe: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { true }

/** Shorthand for [LocalCanServe]. */
val canServe: Boolean
    @Composable get() = LocalCanServe.current
