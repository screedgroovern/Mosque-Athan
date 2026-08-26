package com.groovernapp.mosqueathan.ui.design.media

import androidx.compose.runtime.Composable

/*
 * Not built yet. Stated plainly because these three are the difference between a web
 * controller that can change settings and one that can also manage media.
 *
 * A browser can pick files — `<input type="file">` — so this is a gap, not a limit like
 * `decodeImageFile`. What makes it more than an afternoon is that picking is only half of
 * it: on Android the picked file is copied into the *display's* own storage, and from a
 * browser it would have to be uploaded over the network to reach that storage. The display
 * already accepts `POST /upload`, so the mechanism exists; deciding what a takmir sees
 * while several hundred megabytes of video crosses mosque wifi does not.
 *
 * Until then, media management stays on a device with the app installed, and the web
 * controller does everything else.
 */

@Composable
actual fun rememberMediaPicker(kind: MediaKind): MediaPicker = MediaPicker { }

@Composable
actual fun rememberMultipleMediaPicker(kind: MediaKind): MultipleMediaPicker =
    MultipleMediaPicker { }

@Composable
actual fun rememberAudioFilePicker(): AudioFilePicker = AudioFilePicker { }
