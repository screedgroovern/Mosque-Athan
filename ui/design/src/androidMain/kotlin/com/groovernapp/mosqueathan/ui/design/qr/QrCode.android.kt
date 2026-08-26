package com.groovernapp.mosqueathan.ui.design.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * The `Int` colours here are pixel values for a [Bitmap], not design-system colours: a
 * QR code is black-on-white because that is what a scanner expects, and tinting it with
 * the theme's accent would stop it scanning.
 */
actual fun encodeQrCode(content: String, sizePx: Int): ImageBitmap? {
    if (content.isEmpty() || sizePx <= 0) return null
    return try {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        // One IntArray handed to createBitmap, rather than the per-pixel writes this
        // replaced: at 512x512 that was 262,144 setPixel calls on the composition
        // thread, each one a JNI hop.
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val row = y * sizePx
            for (x in 0 until sizePx) {
                pixels[row + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        // RGB_565: two bytes per pixel instead of four, for an image with two colours.
        Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.RGB_565).asImageBitmap()
    } catch (e: Throwable) {
        // Deliberately Throwable, as in decodeImageFile: zxing throws WriterException for
        // content that will not fit the format, and OutOfMemoryError is what an absurd
        // size would give. The pane must fall back to its printed URL either way.
        null
    }
}
