package com.groovernapp.mosqueathan.data.remote

import com.groovernapp.mosqueathan.ui.design.DisplayApproval
import com.groovernapp.mosqueathan.ui.design.LinkStatus
import com.groovernapp.mosqueathan.ui.design.PinResult

/**
 * The Android controller's way past a display's PIN.
 *
 * Thin on purpose: [RemoteActionSender] already owns the address, the device id and the POST, so
 * all this adds is the mapping between its transport-level answer and the UI's. That mapping is
 * where it belongs — the data layer does not name UI types, and `:webApp`'s `main.kt` does exactly
 * the same job for the browser.
 */
class AndroidDisplayApproval(private val sender: RemoteActionSender) : DisplayApproval {

    override suspend fun verifyPin(pin: String): PinResult =
        when (val outcome = sender.verifyPin(pin)) {
            RemoteActionSender.PinOutcome.Accepted -> PinResult.Accepted
            is RemoteActionSender.PinOutcome.Rejected -> PinResult.Rejected(outcome.message)
            is RemoteActionSender.PinOutcome.Unreachable -> PinResult.Unreachable(outcome.reason)
        }
}

/**
 * The transport's standing, as the UI names it.
 *
 * `Unknown` is deliberately the mapping for `Unknown` rather than something friendlier: it renders
 * *nothing*, which is the honest answer on a display, where this sender never talks to anybody.
 */
fun RemoteActionSender.Status.toUiStatus(): LinkStatus = when (this) {
    RemoteActionSender.Status.Unknown -> LinkStatus.Unknown
    RemoteActionSender.Status.Connected -> LinkStatus.Connected
    RemoteActionSender.Status.NeedsApproval -> LinkStatus.NeedsApproval
    RemoteActionSender.Status.Offline -> LinkStatus.Offline
}
