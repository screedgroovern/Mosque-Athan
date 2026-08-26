package com.groovernapp.mosqueathan.ui.design.qr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Never calls back, by decision rather than omission — the same call as
 * [com.groovernapp.mosqueathan.ui.design.media.rememberMediaPicker], and note that
 * [encodeQrCode] went the other way on this target.
 *
 * Generating a code is a computation zxing already does here. Reading one is a camera
 * pipeline plus a preview surface plus a permission prompt: a feature, not a seam, and
 * `zxing-android-embedded` supplies none of it off Android. A desktop controller can
 * always be paired by typing the address, which is the field directly above the button.
 * Wire a webcam scanner here when a desktop controller actually ships.
 */
@Composable
actual fun rememberQrScanner(prompt: String): QrScanner = remember(prompt) {
    QrScanner { _ -> }
}
