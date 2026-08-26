package com.groovernapp.mosqueathan.ui.design.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Never calls back, by decision rather than omission.
 *
 * The jvm target exists as the guardrail that catches Android code reaching
 * `commonMain`; there is no desktop app yet, and no `MediaStore` implementation on this
 * platform that could copy a chosen file anywhere. A picker that returned a path nothing
 * could store would fail later and less legibly than one that does nothing. Wire an AWT
 * file chooser here when a desktop controller actually ships.
 */
@Composable
actual fun rememberMediaPicker(kind: MediaKind): MediaPicker = remember(kind) {
    MediaPicker { _ -> }
}

/** No-op, for the same reason as [rememberMediaPicker]. */
@Composable
actual fun rememberMultipleMediaPicker(kind: MediaKind): MultipleMediaPicker = remember(kind) {
    MultipleMediaPicker { _ -> }
}

/** No-op, for the same reason as [rememberMediaPicker]. */
@Composable
actual fun rememberAudioFilePicker(): AudioFilePicker = remember {
    AudioFilePicker { _ -> }
}
