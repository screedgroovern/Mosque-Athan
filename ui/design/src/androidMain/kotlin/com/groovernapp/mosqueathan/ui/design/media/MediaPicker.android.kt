package com.groovernapp.mosqueathan.ui.design.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.groovernapp.mosqueathan.domain.AndroidPickedFile
import com.groovernapp.mosqueathan.domain.PickedFile

@Composable
actual fun rememberMediaPicker(kind: MediaKind): MediaPicker {
    val pending = remember { PendingPick<PickedFile>() }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val onPicked = pending.take()
        if (uri != null) onPicked?.invoke(AndroidPickedFile(uri))
    }
    return remember(launcher, kind) {
        MediaPicker { onPicked ->
            pending.onPicked = onPicked
            launcher.launch(kind.request())
        }
    }
}

@Composable
actual fun rememberMultipleMediaPicker(kind: MediaKind): MultipleMediaPicker {
    val pending = remember { PendingPick<List<PickedFile>>() }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val onPicked = pending.take()
        if (uris.isNotEmpty()) onPicked?.invoke(uris.map(::AndroidPickedFile))
    }
    return remember(launcher, kind) {
        MultipleMediaPicker { onPicked ->
            pending.onPicked = onPicked
            launcher.launch(kind.request())
        }
    }
}

@Composable
actual fun rememberAudioFilePicker(): AudioFilePicker {
    val pending = remember { PendingPick<List<String>>() }
    // OpenMultipleDocuments, not the visual-media picker, because audio is not visual
    // media — and because the URI is kept as the setting rather than copied.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val onPicked = pending.take()
        if (uris.isNotEmpty()) onPicked?.invoke(uris.map { it.toString() })
    }
    return remember(launcher) {
        AudioFilePicker { onPicked ->
            pending.onPicked = onPicked
            launcher.launch(arrayOf(AUDIO_MIME))
        }
    }
}

/**
 * Holds the callback between `launch` and the activity result.
 *
 * `rememberLauncherForActivityResult` fixes its handler at composition, so the
 * per-launch callback has to be parked somewhere. Deliberately not `mutableStateOf`:
 * nothing reads it during composition, so making it observable would only invalidate
 * the pane twice per pick. Cleared on the way out, so a cancelled launch cannot leave a
 * stale callback for the next one. Like the `remember`ed id it replaces, it does not
 * survive process death — the picker did not survive it before either.
 */
private class PendingPick<T> {
    var onPicked: ((T) -> Unit)? = null

    fun take(): ((T) -> Unit)? = onPicked.also { onPicked = null }
}

private fun MediaKind.request(): PickVisualMediaRequest = PickVisualMediaRequest(
    when (this) {
        MediaKind.ImageOnly -> ActivityResultContracts.PickVisualMedia.ImageOnly
        MediaKind.ImageAndVideo -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
    }
)

private const val AUDIO_MIME = "audio/*"
