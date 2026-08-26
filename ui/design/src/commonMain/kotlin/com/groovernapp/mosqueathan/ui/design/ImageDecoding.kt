package com.groovernapp.mosqueathan.ui.design

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes an image from a filesystem path, or returns null.
 *
 * Null covers every failure the caller treats identically: an empty path, a file that
 * is gone, a file that is not an image, or a decode that runs out of memory. The
 * dashboard call sites used to repeat the same `exists()` check, `BitmapFactory` call
 * and `catch (e: Throwable)`, and the last of those matters more than it looks — a
 * corrupt or enormous JPEG dropped on the TV by a takmir must fall back to the default
 * background rather than take the display down.
 *
 * It lives in `:ui:design` rather than `:ui:display` because the settings screen decodes
 * the same custom-background file to preview it, and settings cannot depend on the
 * display module. `:ui:design` is the one module both already share.
 */
expect fun decodeImageFile(path: String): ImageBitmap?
