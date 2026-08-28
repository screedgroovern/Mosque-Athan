package com.groovernapp.mosqueathan.ui.design.qr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.groovernapp.mosqueathan.ui.design.PendingCallback
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
actual fun rememberQrScanner(prompt: String): QrScanner? {
    val pending = remember { PendingCallback<String>() }
    val launcher = rememberLauncherForActivityResult(contract = ScanContract()) { result ->
        val onScanned = pending.take()
        // Null contents is a cancelled scan — the takmir backed out of the camera — and
        // is not a failure the pane should report.
        result.contents?.let { onScanned?.invoke(it) }
    }
    return remember(launcher, prompt) {
        QrScanner { onScanned ->
            pending.callback = onScanned
            launcher.launch(scanOptions(prompt))
        }
    }
}

/**
 * The options the pane already shipped, moved rather than rewritten: this app is in daily
 * use in a mosque and a refactor must not change what the camera does.
 */
private fun scanOptions(prompt: String): ScanOptions = ScanOptions().apply {
    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    setPrompt(prompt)
    setCameraId(0)
    setBeepEnabled(true)
    setBarcodeImageEnabled(true)
    // The app itself is locked to landscape; the scanner is not, because a phone reading
    // a code off a television is held whichever way the person is standing.
    setOrientationLocked(false)
}
