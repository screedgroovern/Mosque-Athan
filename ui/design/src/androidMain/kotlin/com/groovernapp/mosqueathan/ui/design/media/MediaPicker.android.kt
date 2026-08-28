package com.groovernapp.mosqueathan.ui.design.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.groovernapp.mosqueathan.domain.AndroidPickedFile
import com.groovernapp.mosqueathan.domain.PickedFile
import com.groovernapp.mosqueathan.ui.design.PendingCallback

@Composable
actual fun rememberMediaPicker(kind: MediaKind): MediaPicker {
    val pending = remember { PendingCallback<PickedFile>() }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val onPicked = pending.take()
        if (uri != null) onPicked?.invoke(AndroidPickedFile(uri))
    }
    return remember(launcher, kind) {
        MediaPicker { onPicked ->
            pending.callback = onPicked
            launcher.launch(kind.request())
        }
    }
}

@Composable
actual fun rememberMultipleMediaPicker(kind: MediaKind): MultipleMediaPicker {
    val pending = remember { PendingCallback<List<PickedFile>>() }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val onPicked = pending.take()
        if (uris.isNotEmpty()) onPicked?.invoke(uris.map(::AndroidPickedFile))
    }
    return remember(launcher, kind) {
        MultipleMediaPicker { onPicked ->
            pending.callback = onPicked
            launcher.launch(kind.request())
        }
    }
}

@Composable
actual fun rememberAudioFilePicker(): AudioFilePicker {
    val pending = remember { PendingCallback<List<PickedFile>>() }
    // OpenMultipleDocuments, not the visual-media picker, because audio is not visual
    // media — and because the URI is kept as the setting rather than copied.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val onPicked = pending.take()
        // Wrapped rather than stringified: the URI is now something to *read from*, so the store
        // can copy the file into the display's own storage. Keeping the URI as the setting meant
        // the setting depended on a permission grant that outlives nothing.
        if (uris.isNotEmpty()) onPicked?.invoke(uris.map { AndroidPickedFile(it) })
    }
    return remember(launcher) {
        AudioFilePicker { onPicked ->
            pending.callback = onPicked
            launcher.launch(arrayOf(AUDIO_MIME))
        }
    }
}

private fun MediaKind.request(): PickVisualMediaRequest = PickVisualMediaRequest(
    when (this) {
        MediaKind.ImageOnly -> ActivityResultContracts.PickVisualMedia.ImageOnly
        MediaKind.ImageAndVideo -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
    }
)

private const val AUDIO_MIME = "audio/*"
