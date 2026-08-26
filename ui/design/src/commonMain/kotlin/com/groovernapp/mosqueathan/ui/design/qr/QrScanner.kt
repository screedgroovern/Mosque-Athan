package com.groovernapp.mosqueathan.ui.design.qr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Opens the platform's camera scanner and reads one QR code.
 *
 * The callback is supplied at [launch] rather than when the scanner is remembered, for
 * the same reason as
 * [com.groovernapp.mosqueathan.ui.design.media.MediaPicker]: a launcher fixes its handler
 * at composition, so a callback passed to `remember` cannot see anything the call site
 * captured after that. Here it also means the two scan buttons — the pane draws one for a
 * compact window and one for a wide one — share a single scanner.
 */
@Stable
fun interface QrScanner {
    /**
     * [onScanned] runs only if a code was actually read, and receives the payload
     * **exactly** as it was encoded.
     *
     * Unvalidated on purpose: what a payload is allowed to mean is the app's protocol,
     * not the camera's business, so parsing belongs to the caller — see
     * `TvPairing.addressFrom` in `core:protocol`. A scanner that returned a parsed
     * address would have to know what the app pairs with.
     */
    fun launch(onScanned: (String) -> Unit)
}

/**
 * A scanner for one QR code, showing [prompt] over the camera preview.
 *
 * `expect` because scanning is a whole screen, not a computation: Android launches an
 * activity from `zxing-android-embedded`, a browser would need `getUserMedia` and a
 * canvas, and iOS `AVCaptureSession`. Unlike [encodeQrCode] there is nothing here that
 * one library can do for every platform.
 *
 * [prompt] arrives already translated. Keeping `Lang` out of the seam is what lets the
 * pane say why it is scanning — "point the camera at the QR code on the TV" — where a
 * design-system default could only say "scan a code".
 */
@Composable
expect fun rememberQrScanner(prompt: String): QrScanner
