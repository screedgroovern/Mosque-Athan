package com.groovernapp.mosqueathan.ui.design.qr

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Encodes [content] as a square QR code [sizePx] pixels on a side, or returns null.
 *
 * `expect` because there is no multiplatform QR encoder: zxing is a Java library, so
 * Android and the jvm target share it while a browser or an iOS controller cannot use it
 * at all. That — not a difference in how two JVMs build a bitmap — is what the seam is
 * for, and it is why the jvm actual is a real encoder rather than the no-op that
 * [com.groovernapp.mosqueathan.ui.design.media.rememberMediaPicker] leaves there.
 *
 * Null covers every failure the caller treats the same way, and it can treat them the
 * same way because the pane draws the address as text beside the code: a missing image
 * degrades to "type this URL", not to a blank pane. Empty content, a non-positive size,
 * content too long for the format, and a platform with no encoder are all null.
 *
 * The result is an [ImageBitmap] rather than a matrix of modules because that is what
 * `Image` takes, and because it matches
 * [com.groovernapp.mosqueathan.ui.design.decodeImageFile] — the settings screen then has
 * one way to obtain a bitmap from something that is not one.
 */
expect fun encodeQrCode(content: String, sizePx: Int): ImageBitmap?
