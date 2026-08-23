package com.groovernapp.mosqueathan.ui.settings.displaymedia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.groovernapp.mosqueathan.domain.Media
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.SlideshowSchedule
import com.groovernapp.mosqueathan.ui.theme.MosqueAthanTheme
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun DisplayMediaSettingsPane(
    state: DisplayMediaUiState,
    onEvent: (DisplayMediaEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<android.net.Uri> ->
        if (uris.isNotEmpty()) {
            onEvent(DisplayMediaEvent.MediaAdded(uris))
        }
    }

    val backgroundPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onEvent(DisplayMediaEvent.CustomBackgroundSelected(uri))
        }
    }

    val dashboardBackgroundAddLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<android.net.Uri> ->
        if (uris.isNotEmpty()) {
            onEvent(DisplayMediaEvent.BackgroundsAdded(uris))
        }
    }

    var editingBgId by remember { mutableStateOf<String?>(null) }
    val dashboardBackgroundEditLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        val id = editingBgId
        if (id != null && uri != null) {
            onEvent(DisplayMediaEvent.BackgroundFileReplaced(id, uri))
        }
        editingBgId = null
    }

    var editingMediaId by remember { mutableStateOf<String?>(null) }
    val mediaEditLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        val id = editingMediaId
        if (id != null && uri != null) {
            onEvent(DisplayMediaEvent.MediaFileReplaced(id, uri))
        }
        editingMediaId = null
    }

    /**
     * Opens a picker for the request events and passes everything else on.
     *
     * The launchers have to live here — `rememberLauncherForActivityResult` is a
     * composable — so the pane is the only place that can act on a "pick a file"
     * request. Children stay on `state` + `onEvent` and never see a launcher.
     */
    val handleEvent: (DisplayMediaEvent) -> Unit = { event ->
        when (event) {
            DisplayMediaEvent.MediaPickRequested ->
                mediaPickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
                    )
                )

            is DisplayMediaEvent.MediaFilePickRequested -> {
                editingMediaId = event.id
                mediaEditLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
                    )
                )
            }

            DisplayMediaEvent.CustomBackgroundPickRequested ->
                backgroundPickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )

            DisplayMediaEvent.BackgroundPickRequested ->
                dashboardBackgroundAddLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )

            is DisplayMediaEvent.BackgroundFilePickRequested -> {
                editingBgId = event.id
                dashboardBackgroundEditLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
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
    MosqueAthanTheme {
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
