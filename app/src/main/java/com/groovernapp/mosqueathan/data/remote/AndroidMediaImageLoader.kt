package com.groovernapp.mosqueathan.data.remote

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.ui.design.MediaImageLoader
import com.groovernapp.mosqueathan.ui.design.decodeImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Resolves a media path either from this device's storage or from the display it is controlling.
 *
 * One build runs in both roles, so the choice is made per call from `connectivityMode` rather than
 * once at startup — the same rule [RemoteActionSender] uses for writes, and for the same reason:
 * the mode is a setting, and it can change while the app is running.
 *
 * **This was broken on Android before it was broken in the browser.** The paths in `mediaList` name
 * files in the *display's* private storage, and a controller's own `filesDir` has the same shape,
 * so `File(path)` there is simply absent — every thumbnail on a paired phone read "failed to load".
 * That is why this is not a web-only fix: `ui:settings` is shared, and so was the defect.
 */
class AndroidMediaImageLoader(private val repository: ConfigRepository) : MediaImageLoader {

    override suspend fun load(path: String): ImageBitmap? {
        if (path.isBlank()) return null
        val config = repository.getConfig()
        val remoteHost = config.remoteTvIp.takeIf {
            config.connectivityMode == REMOTE_MODE && it.isNotEmpty()
        }
        return withContext(Dispatchers.IO) {
            if (remoteHost == null) decodeImageFile(path) else fetch(remoteHost, path)
        }
    }

    /**
     * Asks the display for the file it already serves at `/media?path=`.
     *
     * Returns null on anything at all — no route to the display, a file it has since deleted, a
     * path it refuses, bytes that are not an image. The caller needs only "no thumbnail", and
     * `Throwable` rather than `Exception` because an oversized image fails as `OutOfMemoryError`
     * in the decoder, which must cost one tile and not the settings screen.
     */
    private fun fetch(host: String, path: String): ImageBitmap? {
        return try {
            val encoded = URLEncoder.encode(path, "UTF-8")
            val connection = URL("http://$host:$PORT/media?path=$encoded")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            try {
                if (connection.responseCode !in 200..299) {
                    null
                } else {
                    val bytes = connection.inputStream.use { it.readBytes() }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: Throwable) {
            null
        }
    }

    private companion object {
        const val REMOTE_MODE = "REMOTE"
        const val PORT = 8080
        const val TIMEOUT_MS = 5_000
    }
}
