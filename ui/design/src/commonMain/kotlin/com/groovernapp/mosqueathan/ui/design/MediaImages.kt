package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Turns a media path held by *the display* into an image, however this host can reach it.
 *
 * The paths in `mediaList`, `fridayMediaList` and `dashboardBackgrounds` name files in the
 * display's private storage. Only the display itself can open them; every controller has to ask
 * the display over HTTP, which it already answers at `/media?path=`.
 *
 * This is deliberately an interface provided by the host rather than an `expect`. Nothing about
 * resolving a path is platform-specific — what differs is *which device holds the file*, and only
 * the host knows whether it is the display or a controller. A browser is always a controller; an
 * Android build is either, depending on `connectivityMode`. An `expect` cannot express that, and
 * would have made a fetch look like a browser quirk rather than what a controller must do.
 */
fun interface MediaImageLoader {
    suspend fun load(path: String): ImageBitmap?
}

/** What [rememberMediaImage] knows so far. */
sealed interface MediaImageState {
    /** Not resolved yet. Draw the empty frame, **not** an error — a fetch takes a moment. */
    data object Loading : MediaImageState

    data class Loaded(val image: ImageBitmap) : MediaImageState

    /** Missing, unreadable, unreachable, or not an image at all. */
    data object Unavailable : MediaImageState
}

/**
 * Reads the file directly, which is right on the display and wrong everywhere else.
 *
 * It is the default so that the display and its unit tests need no provider and behave exactly as
 * they did when these call sites called [decodeImageFile] themselves. A controller must override
 * it; if it forgets, thumbnails are missing rather than the app being broken.
 *
 * Runs on the caller's dispatcher — for a local file that is the composition's, which is what the
 * synchronous `remember { decodeImageFile(path) }` this replaced already did. If decoding ever
 * needs to move off the main thread, that belongs in a platform loader, not here.
 */
private val LocalFileMediaImages = MediaImageLoader { path -> decodeImageFile(path) }

val LocalMediaImageLoader = staticCompositionLocalOf<MediaImageLoader> { LocalFileMediaImages }

/**
 * Resolves [path] through the host's [MediaImageLoader], re-running when the path changes.
 *
 * Pass `load = false` for an item that is not an image at all (a video slide) so no fetch is made.
 */
@Composable
fun rememberMediaImage(path: String, load: Boolean = true): MediaImageState {
    val loader = LocalMediaImageLoader.current
    return produceState<MediaImageState>(MediaImageState.Loading, path, load, loader) {
        value = if (!load || path.isBlank()) {
            MediaImageState.Unavailable
        } else {
            loader.load(path)?.let(MediaImageState::Loaded) ?: MediaImageState.Unavailable
        }
    }.value
}
