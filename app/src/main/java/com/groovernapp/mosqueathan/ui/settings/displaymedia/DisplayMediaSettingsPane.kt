package com.groovernapp.mosqueathan.ui.settings.displaymedia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.groovernapp.mosqueathan.data.AnnouncementEntity
import com.groovernapp.mosqueathan.data.MediaEntity
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.data.SlideshowScheduleEntity
import com.groovernapp.mosqueathan.ui.settings.SettingsCard
import com.groovernapp.mosqueathan.util.Lang

@Composable
fun DisplayMediaSettingsPane(
    config: MosqueConfigEntity,
    mediaList: List<MediaEntity>,
    slideshowSchedules: List<SlideshowScheduleEntity>,
    announcements: List<AnnouncementEntity>,
    onUpdateThemeSettings: (String, Int, Boolean, Boolean) -> Unit,
    onAddMediaItems: (List<android.net.Uri>) -> Unit,
    onDeleteMediaItem: (MediaEntity) -> Unit,
    onSelectCustomBackground: (android.net.Uri) -> Unit,
    onClearCustomBackground: () -> Unit,
    onAddSlideshowSchedule: (String, Boolean, String, String) -> Unit,
    onDeleteSlideshowSchedule: (String) -> Unit,
    onPreviewSlideshow: () -> Unit,
    onAddAnnouncement: (String) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onEditAnnouncement: (String, String) -> Unit,
    onReorderAnnouncements: (List<String>) -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val mediaPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<android.net.Uri> ->
        if (uris.isNotEmpty()) {
            onAddMediaItems(uris)
        }
    }

    val backgroundPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onSelectCustomBackground(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SettingsCard(
            title = Lang.get("Background Theme", "Latar Belakang & Media"),
            icon = Icons.Default.Image
        ) {
            ThemeSettingsSegment(
                config = config,
                transitionType = config.transitionType,
                slideIntervalSeconds = config.slideIntervalSeconds,
                videoWaitUntilEnd = config.videoWaitUntilEnd,
                includeDefaultImage = config.includeDefaultImage,
                mediaList = mediaList,
                slideshowSchedules = slideshowSchedules,
                onUpdateThemeSettings = onUpdateThemeSettings,
                onAddMediaItem = {
                    mediaPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        )
                    )
                },
                onDeleteMediaItem = onDeleteMediaItem,
                onSelectCustomBackground = {
                    backgroundPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onClearCustomBackground = onClearCustomBackground,
                onAddSlideshowSchedule = onAddSlideshowSchedule,
                onDeleteSlideshowSchedule = onDeleteSlideshowSchedule,
                onPreviewSlideshow = onPreviewSlideshow
            )
        }

        SettingsCard(
            title = Lang.get("Running Text", "Teks Berjalan"),
            icon = Icons.Default.Campaign
        ) {
            AnnouncementsSettingsSegment(
                config = config,
                announcements = announcements,
                onAddAnnouncement = onAddAnnouncement,
                onDeleteAnnouncement = onDeleteAnnouncement,
                onEditAnnouncement = onEditAnnouncement,
                onReorderAnnouncements = onReorderAnnouncements,
                isCompact = isCompact
            )
        }
    }
}
