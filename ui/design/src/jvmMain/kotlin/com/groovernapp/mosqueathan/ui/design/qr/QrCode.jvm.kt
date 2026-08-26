package com.groovernapp.mosqueathan.ui.design.qr

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage

/**
 * Real, not a no-op, because zxing runs here unchanged — and because an `expect` whose
 * only implementation is Android would be indistinguishable from leaving the code in
 * `:app`. The jvm target is the guardrail, so it has to compile and link the same
 * library the display uses.
 *
 * The encoding is shared with the Android actual; only the bitmap is not. Extracting the
 * loop would mean a common `QrMatrix` type that neither platform wants, so the six lines
 * are duplicated on purpose.
 */
actual fun encodeQrCode(content: String, sizePx: Int): ImageBitmap? {
    if (content.isEmpty() || sizePx <= 0) return null
    return try {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val image = BufferedImage(sizePx, sizePx, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until sizePx) {
            for (x in 0 until sizePx) {
                // Pixel values, not design-system colours: a scanner expects black on
                // white, and a themed QR code is an unreadable one.
                image.setRGB(x, y, if (matrix.get(x, y)) BLACK_RGB else WHITE_RGB)
            }
        }
        image.toComposeImageBitmap()
    } catch (e: Throwable) {
        null
    }
}

private const val BLACK_RGB = 0x000000
private const val WHITE_RGB = 0xFFFFFF
