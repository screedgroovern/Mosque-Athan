package com.groovernapp.mosqueathan.ui.display

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.File
import javax.imageio.ImageIO

actual fun decodeImageFile(path: String): ImageBitmap? {
    if (path.isEmpty() || !File(path).exists()) return null
    return try {
        ImageIO.read(File(path))?.toComposeImageBitmap()
    } catch (e: Throwable) {
        null
    }
}
