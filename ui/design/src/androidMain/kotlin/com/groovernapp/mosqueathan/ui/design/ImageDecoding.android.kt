package com.groovernapp.mosqueathan.ui.design

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

actual fun decodeImageFile(path: String): ImageBitmap? {
    if (path.isEmpty() || !File(path).exists()) return null
    return try {
        BitmapFactory.decodeFile(path)?.asImageBitmap()
    } catch (e: Throwable) {
        // Deliberately Throwable: OutOfMemoryError on an oversized image is the failure
        // most likely to happen here, and it is an Error rather than an Exception.
        null
    }
}
