package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether this device is currently reaching the display it controls.
 *
 * Provided by the host rather than derived in the UI, for the same reason as [LocalAppVersion]: only
 * a host that *has* a link can answer. The browser holds one WebSocket and knows exactly; the
 * Android app in REMOTE mode posts each action and forgets, so it has nothing to report — hence
 * [Unknown], which renders nothing at all rather than a green light that means "we did not check".
 *
 * A display in SERVER mode does not use this: it is the source of truth and syncs nowhere. What is
 * worth showing there is how many controllers are attached, which comes from the config's device
 * list instead.
 */
enum class LinkStatus {
    /** No link to report on. The status indicator is hidden entirely. */
    Unknown,

    /** Opening the connection, or waiting for the display to answer. */
    Connecting,

    /** Connected, and what is on screen is what the display has. */
    Connected,

    /**
     * The display has a PIN and has not allowed this device. Distinct from [Offline] because
     * reconnecting will not help — a person has to approve it.
     */
    NeedsApproval,

    /** No display reachable. */
    Offline
}

/** See [LinkStatus]. Defaults to [LinkStatus.Unknown], so a host that says nothing shows nothing. */
val LocalLinkStatus: ProvidableCompositionLocal<LinkStatus> =
    staticCompositionLocalOf { LinkStatus.Unknown }

/** Shorthand for [LocalLinkStatus]. */
val linkStatus: LinkStatus
    @Composable get() = LocalLinkStatus.current
