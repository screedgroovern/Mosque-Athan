package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How a controller asks a display to let it in.
 *
 * [LinkStatus.NeedsApproval] has existed since the header bar landed, and it has always been a
 * dead end: the UI could say "a person has to approve this device" and offered no way to become
 * that person. `verify_pin` was in the protocol and handled by the display; nothing in the shared
 * settings UI could send it. So on any display with a PIN set, a controller connected, reported
 * that it needed approval, and left the takmir with no route forward except walking to the
 * television. That is the last thing standing between the new panel and deleting the old one.
 *
 * ### Why an interface the host provides, not an `expect`
 *
 * Nothing about sending a PIN is platform-specific — it is one action on a transport this app
 * already has twice over. What differs is *which* transport: the browser holds a registered
 * WebSocket and can await the reply on it, while an Android controller POSTs each action and has
 * to read the response of that one request. The same reasoning as `MediaImageLoader`: the platform
 * is not the variable, the role is.
 *
 * ### Why the local defaults to null
 *
 * A display has nobody to ask. Rather than give it an implementation that always fails, hosts that
 * cannot verify a PIN provide nothing and the entry surface is not drawn at all — the rule this
 * codebase arrived at twice, once for the browser's Back button and once for its QR scanner: a
 * control that cannot act should not exist.
 */
interface DisplayApproval {

    /**
     * Sends [pin] to the display and reports what it said.
     *
     * Suspends until the display answers or the attempt fails, so the caller can keep the takmir's
     * PIN on screen while it is in flight and say something true afterwards.
     */
    suspend fun verifyPin(pin: String): PinResult
}

/** What the display made of a PIN. */
sealed interface PinResult {

    /** Accepted. The display has marked this device allowed and sent its state. */
    data object Accepted : PinResult

    /**
     * Refused, with the display's own wording where it gave any.
     *
     * The display's message is preferred over a local string because it is the side that knows
     * why — today that is only a wrong PIN, but a revoked device would arrive the same way.
     */
    data class Rejected(val message: String?) : PinResult

    /**
     * The attempt never reached the display, so nothing is known about the PIN.
     *
     * Kept separate from [Rejected] for the reason `LinkStatus` separates `NeedsApproval` from
     * `Offline`: telling a takmir their PIN is wrong when the wifi dropped sends them looking for
     * the wrong problem.
     */
    data class Unreachable(val reason: String?) : PinResult
}

/**
 * The host's way in, or null where there is none. See [DisplayApproval].
 *
 * `staticCompositionLocalOf` because it never changes for the life of a host — a display never
 * gains one and a controller never loses one.
 */
val LocalDisplayApproval = staticCompositionLocalOf<DisplayApproval?> { null }
