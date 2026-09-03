package com.groovernapp.mosqueathan.web

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.groovernapp.mosqueathan.ui.design.MediaImageLoader
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.Image

/**
 * Fetches a media file from the display and decodes it in the page.
 *
 * The display already serves these at `/media?path=`, guarded so the path must resolve inside its
 * own private storage — so this asks for exactly the paths the snapshot handed us and nothing else
 * is reachable through it.
 *
 * **Why the bytes arrive as base64.** A `ByteArray` cannot cross the Kotlin/Wasm JS boundary;
 * strings can. `FileReader.readAsDataURL` is the browser's own encoder, so the conversion costs
 * nothing we would not otherwise pay, and a thumbnail is tens of kilobytes. Doing the loop in
 * Kotlin over an interop array instead would be slower and buy nothing.
 *
 * Failures are all one case here — offline, a deleted file, a path the display refuses, a video
 * mislabelled as an image. The caller only needs "no image", so nothing is logged and nothing is
 * retried: the snapshot that named this path is what would have to change.
 */
class WebMediaImageLoader(private val host: String) : MediaImageLoader {

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun load(path: String): ImageBitmap? {
        if (path.isBlank()) return null
        val base64 = fetchBase64("http://$host:$PORT/media?path=${encodeQueryValue(path)}")
            ?: return null
        return try {
            Image.makeFromEncoded(Base64.decode(base64)).toComposeImageBitmap()
        } catch (_: Throwable) {
            // Deliberately Throwable, as in decodeImageFile: an oversized or corrupt file can
            // fail in the decoder rather than throw an Exception, and a bad file on the display
            // must cost one missing thumbnail, not the panel.
            null
        }
    }

    private suspend fun fetchBase64(url: String): String? =
        suspendCancellableCoroutine { continuation ->
            fetchAsDataUrl(url) { base64, _ ->
                if (continuation.isActive) continuation.resume(base64)
            }
        }

    private companion object {
        const val PORT = 8080
    }
}

@Suppress("UNUSED_PARAMETER")
private fun fetchAsDataUrl(url: String, onResult: (String?, String?) -> Unit): Unit = js(
    """{
        fetch(url)
            .then(function (response) {
                if (!response.ok) { onResult(null, 'HTTP ' + response.status); return null; }
                return response.blob();
            })
            .then(function (blob) {
                if (blob === null) { return; }
                var reader = new FileReader();
                reader.onload = function () {
                    var result = String(reader.result);
                    var comma = result.indexOf(',');
                    if (comma < 0) { onResult(null, 'not a data url'); return; }
                    onResult(result.substring(comma + 1), null);
                };
                reader.onerror = function () { onResult(null, 'could not read the body'); };
                reader.readAsDataURL(blob);
            })
            .catch(function (e) { onResult(null, String(e && e.message ? e.message : e)); });
    }"""
)
