package com.groovernapp.mosqueathan.ui.display

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes an image from a filesystem path, or returns null.
 *
 * Null covers every failure the caller treats identically: an empty path, a file that
 * is gone, a file that is not an image, or a decode that runs out of memory. All three
 * dashboard call sites used to repeat the same `exists()` check, `BitmapFactory` call
 * and `catch (e: Throwable)`, and the last of those matters more than it looks — a
 * corrupt or enormous JPEG dropped on the TV by a takmir must fall back to the default
 * background rather than take the display down.
 */
expect fun decodeImageFile(path: String): ImageBitmap?
