package com.groovernapp.mosqueathan.ui.design.qr

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Always null, and that costs nothing here.
 *
 * The pairing QR is drawn by the *display*, for a controller to scan. A browser is only
 * ever the controller, so the pane that calls this is one the web build does not show —
 * and where it did, the caller already falls back to printing the address as text, which a
 * person can type.
 *
 * zxing has no wasm target. If this is ever wanted, generating a QR matrix is a few
 * hundred lines of pure Kotlin and would belong in `commonMain`, replacing all three
 * actuals rather than adding a fourth.
 */
actual fun encodeQrCode(content: String, sizePx: Int): ImageBitmap? = null
