package com.groovernapp.mosqueathan.ui.design.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.groovernapp.mosqueathan.domain.PickedFile

/**
 * What the picker will let the user choose.
 *
 * Not a detail to be generalised away: slideshow slides may be video, dashboard
 * backgrounds may not, and offering a takmir a video that the background renderer will
 * never play is a bug the picker can prevent.
 */
enum class MediaKind {
    ImageOnly,
    ImageAndVideo
}

/**
 * Opens the platform's media picker for a single file.
 *
 * The callback is supplied at [launch] rather than when the picker is remembered, and
 * that is the whole point of the shape. Every settings pane has at least one "replace
 * *this* file" action, where the result has to be paired with the row that asked; with
 * a callback fixed at composition each pane had to park the row's id in its own
 * `remember { mutableStateOf<String?>(null) }` and read it back in the result handler.
 * Passing the callback per launch lets the call site capture the id directly, deletes
 * that bookkeeping, and lets one picker serve several actions that want the same
 * [MediaKind].
 */
@Stable
fun interface MediaPicker {
    /** [onPicked] runs only if the user actually chose something. */
    fun launch(onPicked: (PickedFile) -> Unit)
}

/** The same, for the actions that take a whole batch of slides at once. */
@Stable
fun interface MultipleMediaPicker {
    /** [onPicked] runs only if the user actually chose something; never with an empty list. */
    fun launch(onPicked: (List<PickedFile>) -> Unit)
}

/**
 * A picker for one file.
 *
 * `expect` because choosing a file is one of the few things with no multiplatform
 * answer at all — Android hands back a permission grant, a browser hands back bytes.
 * Results arrive as [PickedFile], so a settings composable never names a platform type.
 */
@Composable
expect fun rememberMediaPicker(kind: MediaKind): MediaPicker

/** A picker for several files at once. */
@Composable
expect fun rememberMultipleMediaPicker(kind: MediaKind): MultipleMediaPicker

/**
 * Picks audio files, which are referenced where they lie rather than copied.
 *
 * Separate from [MultipleMediaPicker] only because the chooser has to be filtered to audio —
 * "images and video" is the wrong filter and, on Android, a different picker entirely.
 *
 * This used to hand back the platform's own reference (a content URI) to be stored as the setting
 * verbatim. It hands back [PickedFile] now because that reference is only openable on the device
 * that produced it: `murottalAudioPaths` is read by the **display**, so a file picked on a
 * controller has to reach the display before its path means anything. Handing back a file lets
 * `MediaStore.storeMurottalAudio` decide — copy it, on the display, or upload it, on a controller.
 */
@Stable
fun interface AudioFilePicker {
    /** [onPicked] runs only if the user actually chose something; never with an empty list. */
    fun launch(onPicked: (List<PickedFile>) -> Unit)
}

/** A picker for audio files. See [AudioFilePicker] for why it is not a [MediaKind]. */
@Composable
expect fun rememberAudioFilePicker(): AudioFilePicker
