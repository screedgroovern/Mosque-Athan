package com.groovernapp.mosqueathan.ui.design

/**
 * Holds a per-launch callback between `launch` and the activity result.
 *
 * `rememberLauncherForActivityResult` fixes its handler at composition, but every seam
 * built on it — the media pickers, the QR scanner — hands the callback in at `launch`
 * instead, so that a call site can capture the row it is replacing. The callback has to
 * be parked somewhere in between, and this is that somewhere.
 *
 * Deliberately not `mutableStateOf`: nothing reads it during composition, so making it
 * observable would only invalidate the pane twice per launch. Cleared on the way out, so
 * a cancelled launch cannot leave a stale callback for the next one. It does not survive
 * process death, and neither did the `remember`ed id it replaced.
 *
 * Shared rather than repeated per seam: this is the second launcher-backed seam in the
 * module and both need exactly this, comment included.
 */
internal class PendingCallback<T> {
    var callback: ((T) -> Unit)? = null

    fun take(): ((T) -> Unit)? = callback.also { callback = null }
}
