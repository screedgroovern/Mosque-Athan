package com.groovernapp.mosqueathan.ui.settings.displaymedia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.media.MediaKind
import com.groovernapp.mosqueathan.ui.design.media.rememberMediaPicker
import com.groovernapp.mosqueathan.ui.design.media.rememberMultipleMediaPicker

@Composable
fun DisplayMediaSettingsPane(
    state: DisplayMediaUiState,
    onEvent: (DisplayMediaEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val slidePicker = rememberMultipleMediaPicker(MediaKind.ImageAndVideo)
    val slideReplacePicker = rememberMediaPicker(MediaKind.ImageAndVideo)
    val backgroundPicker = rememberMultipleMediaPicker(MediaKind.ImageOnly)

    // Serves both single-image actions: with the callback given at launch, the custom
    // background and a dashboard-background replacement no longer need a picker each.
    val singleImagePicker = rememberMediaPicker(MediaKind.ImageOnly)

    /**
     * Opens a picker for the request events and passes everything else on.
     *
     * The pickers have to be remembered here — they are composables — so the pane is
     * the only place that can act on a "pick a file" request. Children stay on
     * `state` + `onEvent` and never see a picker.
     */
    val handleEvent: (DisplayMediaEvent) -> Unit = { event ->
        when (event) {
            DisplayMediaEvent.MediaPickRequested ->
                slidePicker.launch { files -> onEvent(DisplayMediaEvent.MediaAdded(files)) }

            is DisplayMediaEvent.MediaFilePickRequested ->
                slideReplacePicker.launch { file ->
                    onEvent(DisplayMediaEvent.MediaFileReplaced(event.id, file))
                }

            DisplayMediaEvent.CustomBackgroundPickRequested ->
                singleImagePicker.launch { file ->
                    onEvent(DisplayMediaEvent.CustomBackgroundSelected(file))
                }

            DisplayMediaEvent.BackgroundPickRequested ->
                backgroundPicker.launch { files ->
                    onEvent(DisplayMediaEvent.BackgroundsAdded(files))
                }

            is DisplayMediaEvent.BackgroundFilePickRequested ->
                singleImagePicker.launch { file ->
                    onEvent(DisplayMediaEvent.BackgroundFileReplaced(event.id, file))
                }

            else -> onEvent(event)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xxxl)
    ) {
        Column {
            ThemeSettingsSegment(state = state, onEvent = handleEvent)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 1280, heightDp = 720)
@Composable
fun DisplayMediaSettingsPanePreview() {
    // A preview is now four lines rather than a hand-built list of thirty-two
    // arguments — the clearest single demonstration of what state + onEvent buys.
    MosqueTheme {
        DisplayMediaSettingsPane(
            state = DisplayMediaUiState(
                slideshow = SlideshowSettings(
                    enabled = true,
                    transitionType = "FADE",
                    intervalSeconds = 8
                )
            ),
            onEvent = {}
        )
    }
}
