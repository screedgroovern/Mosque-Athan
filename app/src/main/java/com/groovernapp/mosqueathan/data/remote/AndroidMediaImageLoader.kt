package com.groovernapp.mosqueathan.data.remote

import androidx.compose.ui.graphics.ImageBitmap
import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.ui.design.MediaImageLoader
import com.groovernapp.mosqueathan.ui.design.decodeImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
 *
 * Pictures fetched from the display are kept by [PreviewCache], so a controller that has synced
 * still shows them when it is away from the display.
 */
class AndroidMediaImageLoader(
    private val repository: ConfigRepository,
    private val cache: PreviewCache
) : MediaImageLoader {

    override suspend fun load(path: String): ImageBitmap? {
        if (path.isBlank()) return null
        val config = repository.getConfig()
        val remoteHost = config.remoteTvIp.takeIf {
            config.connectivityMode == REMOTE_MODE && it.isNotEmpty()
        }
        return withContext(Dispatchers.IO) {
            when {
                remoteHost == null -> decodeImageFile(path)
                // A picture added on this controller is a real file here, and the display does not
                // have it, so asking the display would only fail.
                File(path).exists() -> decodeImageFile(path)
                // The saved copy first: it is what keeps a picture showing when the display is
                // away, and a miss falls through to the display and saves what it gets.
                else -> cache.read(path) ?: cache.fetchAndStore(remoteHost, path)
            }
        }
    }

    private companion object {
        const val REMOTE_MODE = "REMOTE"
    }
}
