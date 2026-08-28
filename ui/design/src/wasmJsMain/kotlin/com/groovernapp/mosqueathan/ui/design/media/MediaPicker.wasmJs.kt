package com.groovernapp.mosqueathan.ui.design.media

import androidx.compose.runtime.Composable
import com.groovernapp.mosqueathan.domain.PickedFile
import com.groovernapp.mosqueathan.domain.WebPickedFile
import kotlin.js.JsAny

/**
 * File pickers backed by a hidden `<input type="file">`.
 *
 * A browser cannot reach the picker Android uses, but it does not need to: `input.click()` opens
 * the platform's own file chooser, and on a phone that chooser offers the camera roll — which is
 * where a takmir's photos are. So this is a real implementation rather than the no-op it replaces.
 *
 * The element is created per launch and removed again. Reusing one is tempting and wrong: Safari
 * does not fire `change` when the same file is chosen twice in a row unless the value is cleared,
 * and a fresh element cannot have that problem.
 *
 * The `accept` attribute is a filter, not a guarantee — every browser lets the user override it —
 * so `isVideo` is decided from the chosen file's MIME type, never from what was asked for.
 */
@Composable
actual fun rememberMediaPicker(kind: MediaKind): MediaPicker = MediaPicker { onPicked ->
    openFileChooser(acceptFor(kind), multiple = false) { files ->
        files.firstOrNull()?.let(onPicked)
    }
}

@Composable
actual fun rememberMultipleMediaPicker(kind: MediaKind): MultipleMediaPicker =
    MultipleMediaPicker { onPicked ->
        openFileChooser(acceptFor(kind), multiple = true) { files ->
            if (files.isNotEmpty()) onPicked(files)
        }
    }

/**
 * The audio chooser, filtered to audio.
 *
 * Same hidden `<input type="file">` as the media pickers, but **not** with a wildcard audio filter.
 *
 * Measured on the device: a wildcard audio accept makes Chrome on Android treat the input as a
 * *media capture* request — it went straight to the microphone and asked for recording permission,
 * which is the opposite of picking an existing recitation. Naming the formats explicitly routes to
 * the document picker instead, which is where a takmir's murottal files are.
 *
 * Extensions as well as MIME types because the two are matched by different pickers, and a recitation
 * downloaded from a website often arrives with a MIME type the platform did not record.
 *
 * What happens to the file afterwards is not this seam's business: `MediaStore.storeMurottalAudio`
 * uploads it to the display, because `murottalAudioPaths` is read there and a browser has no path
 * to give.
 */
@Composable
actual fun rememberAudioFilePicker(): AudioFilePicker = AudioFilePicker { onPicked ->
    openFileChooser(AUDIO_ACCEPT, multiple = true) { files ->
        if (files.isNotEmpty()) onPicked(files)
    }
}

private fun acceptFor(kind: MediaKind): String = when (kind) {
    MediaKind.ImageOnly -> "image/*"
    MediaKind.ImageAndVideo -> "image/*,video/*"
}

private fun openFileChooser(
    accept: String,
    multiple: Boolean,
    onResult: (List<PickedFile>) -> Unit
) {
    // Collected one at a time because Kotlin/Wasm JS interop accepts only primitives, strings,
    // external types and functions — a List cannot cross the boundary, so JS reports each file
    // through a callback and signals the end with another.
    val picked = mutableListOf<PickedFile>()
    showPicker(
        accept = accept,
        multiple = multiple,
        onFile = { handle, name, mimeType ->
            picked.add(
                WebPickedFile(
                    handle = handle,
                    name = name.ifBlank { "upload" },
                    isVideo = mimeType.startsWith("video/")
                )
            )
        },
        onDone = { onResult(picked) }
    )
}

@Suppress("UNUSED_PARAMETER")
private fun showPicker(
    accept: String,
    multiple: Boolean,
    onFile: (JsAny, String, String) -> Unit,
    onDone: () -> Unit
): Unit = js(
    """{
        var input = document.createElement('input');
        input.type = 'file';
        input.accept = accept;
        input.multiple = multiple;
        input.style.position = 'fixed';
        input.style.left = '-10000px';
        document.body.appendChild(input);

        var finish = function () {
            var files = input.files ? Array.prototype.slice.call(input.files) : [];
            if (input.parentNode) { input.parentNode.removeChild(input); }
            for (var i = 0; i < files.length; i++) {
                onFile(files[i], files[i].name || '', files[i].type || '');
            }
            onDone();
        };

        input.addEventListener('change', finish, { once: true });
        // Chrome fires 'cancel' when the chooser is dismissed; Safari does not, which is why the
        // element is single-use — a dismissed picker on Safari leaves one detached input behind
        // rather than a stale one that refuses to fire 'change' for a repeated file.
        input.addEventListener('cancel', finish, { once: true });
        input.click();
    }"""
)

/**
 * The audio formats offered, spelled out rather than wildcarded — see [rememberAudioFilePicker].
 * Built by joining so the value never appears as a literal containing a slash-star sequence, which
 * inside a KDoc would open a nested comment.
 */
private val AUDIO_ACCEPT = listOf(
    ".mp3", ".m4a", ".aac", ".ogg", ".oga", ".wav", ".flac",
    "audio/mpeg", "audio/mp4", "audio/aac", "audio/ogg", "audio/wav", "audio/flac"
).joinToString(",")
