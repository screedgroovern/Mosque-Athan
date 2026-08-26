package com.groovernapp.mosqueathan.ui.design.qr

import androidx.compose.runtime.Composable

/**
 * Not built yet, and worth more than the pickers.
 *
 * Scanning is how a takmir pairs a controller without typing an IP address, and the
 * browser can do it: `getUserMedia` plus `BarcodeDetector`, or a JS decoder over a canvas.
 * The obstacle is not the API, it is that Compose for web renders to a canvas — putting a
 * live camera preview *behind* that canvas, or compositing it into the same one, is a
 * layout problem this app has not had to solve anywhere else.
 *
 * Meanwhile the address can be typed, which is what the pane offers beside the scan
 * button on every platform.
 */
@Composable
actual fun rememberQrScanner(prompt: String): QrScanner = QrScanner { }
