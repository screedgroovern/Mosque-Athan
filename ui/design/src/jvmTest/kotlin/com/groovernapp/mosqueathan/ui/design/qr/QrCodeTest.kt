package com.groovernapp.mosqueathan.ui.design.qr

import kotlin.test.Test
import kotlin.test.assertNull

/**
 * The null contract, which is the half the pane actually branches on: it draws the
 * address as text beside the code, so every null has to be a fallback rather than a
 * crash.
 *
 * `jvmTest` rather than `commonTest` for a reason worth knowing before adding to this
 * file. In `commonTest` these would also run as Android unit tests, where
 * `android.graphics.Bitmap` is an unmocked stub that throws.
 *
 * **The successful path is deliberately not asserted here, and cannot be.** Building any
 * `ImageBitmap` on the jvm target goes through Skia, whose native library is not on this
 * module's test classpath — `BufferedImage(8, 8, TYPE_INT_RGB).toComposeImageBitmap()`
 * fails with `ExceptionInInitializerError`, which the actual catches and reports as null,
 * exactly as it would report a platform with no encoder. So a "it encoded something" test
 * would pass on a developer's desktop app and fail here for a reason that has nothing to
 * do with QR codes. `ImageDecoding.jvm` has the same property and the same absent test.
 * Adding `implementation(compose.desktop.currentOs)` to `jvmTest` would fix it, at the
 * cost of an OS-specific native download in the guardrail's test classpath; that trade
 * has not been taken. Whether the code renders is a device check either way.
 */
class QrCodeTest {

    @Test
    fun nothingToEncodeIsNull() {
        assertNull(encodeQrCode("", 512))
    }

    @Test
    fun anImpossibleSizeIsNullRatherThanAThrow() {
        assertNull(encodeQrCode("http://192.168.1.15:8080", 0))
        assertNull(encodeQrCode("http://192.168.1.15:8080", -1))
    }

    @Test
    fun contentTooLongForTheFormatIsNull() {
        // Past the ~2,953-byte ceiling of the format: zxing throws WriterException, and
        // the pane must fall back to printing the address rather than take the screen
        // down.
        assertNull(encodeQrCode("a".repeat(5000), 512))
    }
}
