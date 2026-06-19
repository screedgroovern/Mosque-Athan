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
import com.groovernapp.mosqueathan.data.MediaEntity
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.data.SlideshowScheduleEntity
import com.groovernapp.mosqueathan.ui.theme.MosqueAthanTheme

@Composable
fun DisplayMediaSettingsPane(
    config: MosqueConfigEntity,
    mediaList: List<MediaEntity>,
    slideshowSchedules: List<SlideshowScheduleEntity>,
    dashboardBackgrounds: List<com.groovernapp.mosqueathan.data.DashboardBackgroundEntity>,
    onUpdateThemeSettings: (String, Int, Boolean, Boolean) -> Unit,
    onAddMediaItems: (List<android.net.Uri>) -> Unit,
    onUpdateMediaItemPath: (String, android.net.Uri) -> Unit,
    onDeleteMediaItem: (MediaEntity) -> Unit,
    onReorderMediaItems: (List<String>) -> Unit,
    onSelectCustomBackground: (android.net.Uri) -> Unit,
    onClearCustomBackground: () -> Unit,
    onAddSlideshowSchedule: (String, Boolean, String, String) -> Unit,
    onUpdateSlideshowScheduleTime: (com.groovernapp.mosqueathan.data.ScheduleItem, String, String) -> Unit,
    onUpdateSlideshowScheduleDays: (com.groovernapp.mosqueathan.data.ScheduleItem, String) -> Unit,
    onUpdateSlideshowScheduleDates: (com.groovernapp.mosqueathan.data.ScheduleItem, String) -> Unit,
    onDeleteSlideshowSchedule: (String) -> Unit,
    onPreviewSlideshow: () -> Unit,
    onAddDashboardBackground: (List<android.net.Uri>) -> Unit,
    onUpdateDashboardBackgroundPath: (String, android.net.Uri) -> Unit,
    onDeleteDashboardBackground: (String) -> Unit,
    onUpdateDashboardBackgroundUsed: (String, Boolean) -> Unit,
    onUpdateDashboardBackgroundOrder: (List<String>) -> Unit,
    onUpdateDashboardSettings: (Boolean, String, Int) -> Unit,
    onUpdateSlideshowEnabled: (Boolean) -> Unit,
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

    val dashboardBackgroundAddLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<android.net.Uri> ->
        if (uris.isNotEmpty()) {
            onAddDashboardBackground(uris)
        }
    }

    var editingBgId by remember { mutableStateOf<String?>(null) }
    val dashboardBackgroundEditLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        val id = editingBgId
        if (id != null && uri != null) {
            onUpdateDashboardBackgroundPath(id, uri)
        }
        editingBgId = null
    }

    var editingMediaId by remember { mutableStateOf<String?>(null) }
    val mediaEditLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        val id = editingMediaId
        if (id != null && uri != null) {
            onUpdateMediaItemPath(id, uri)
        }
        editingMediaId = null
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column {
            ThemeSettingsSegment(
                config = config,
                transitionType = config.transitionType,
                slideIntervalSeconds = config.slideIntervalSeconds,
                videoWaitUntilEnd = config.videoWaitUntilEnd,
                includeDefaultImage = config.includeDefaultImage,
                mediaList = mediaList,
                slideshowSchedules = slideshowSchedules,
                dashboardBackgrounds = dashboardBackgrounds,
                onUpdateThemeSettings = onUpdateThemeSettings,
                onAddMediaItem = {
                    mediaPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        )
                    )
                },
                onUpdateMediaItemPath = { id ->
                    editingMediaId = id
                    mediaEditLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        )
                    )
                },
                onDeleteMediaItem = onDeleteMediaItem,
                onReorderMediaItems = onReorderMediaItems,
                onSelectCustomBackground = {
                    backgroundPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onClearCustomBackground = onClearCustomBackground,
                onAddSlideshowSchedule = onAddSlideshowSchedule,
                onUpdateSlideshowScheduleTime = onUpdateSlideshowScheduleTime,
                onUpdateSlideshowScheduleDays = onUpdateSlideshowScheduleDays,
                onUpdateSlideshowScheduleDates = onUpdateSlideshowScheduleDates,
                onDeleteSlideshowSchedule = onDeleteSlideshowSchedule,
                onPreviewSlideshow = onPreviewSlideshow,
                onAddDashboardBackground = {
                    dashboardBackgroundAddLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onUpdateDashboardBackgroundPath = { id ->
                    editingBgId = id
                    dashboardBackgroundEditLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onDeleteDashboardBackground = onDeleteDashboardBackground,
                onUpdateDashboardBackgroundUsed = onUpdateDashboardBackgroundUsed,
                onUpdateDashboardBackgroundOrder = onUpdateDashboardBackgroundOrder,
                onUpdateDashboardSettings = onUpdateDashboardSettings,
                onUpdateSlideshowEnabled = onUpdateSlideshowEnabled,
                isCompact = isCompact
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun DisplayMediaSettingsPanePreview() {
    val mockConfig = MosqueConfigEntity().apply {
        transitionType = "FADE"
        slideIntervalSeconds = 10
        videoWaitUntilEnd = false
        includeDefaultImage = true
    }

    val mockMediaList = listOf(
        MediaEntity().apply {
            id = "1"
            filePath = "path/to/image1.jpg"
            isVideo = false
        },
        MediaEntity().apply {
            id = "2"
            filePath = "path/to/video1.mp4"
            isVideo = true
        }
    )

    val mockSlideshowSchedules = listOf(
        SlideshowScheduleEntity().apply {
            id = "1"
            day = "Monday"
            isSpecificDate = false
            startTime = "08:00"
            endTime = "17:00"
        }
    )


    MosqueAthanTheme {
        DisplayMediaSettingsPane(
            config = mockConfig,
            mediaList = mockMediaList,
            slideshowSchedules = mockSlideshowSchedules,
            dashboardBackgrounds = emptyList(),
            onUpdateThemeSettings = { _, _, _, _ -> },
            onAddMediaItems = {},
            onUpdateMediaItemPath = { _, _ -> },
            onDeleteMediaItem = {},
            onReorderMediaItems = {},
            onSelectCustomBackground = {},
            onClearCustomBackground = {},
            onAddSlideshowSchedule = { _, _, _, _ -> },
            onUpdateSlideshowScheduleTime = { _, _, _ -> },
            onUpdateSlideshowScheduleDays = { _, _ -> },
            onUpdateSlideshowScheduleDates = { _, _ -> },
            onDeleteSlideshowSchedule = {},
            onUpdateSlideshowEnabled = {},
            onPreviewSlideshow = {},
            onAddDashboardBackground = {},
            onUpdateDashboardBackgroundPath = { _, _ -> },
            onDeleteDashboardBackground = {},
            onUpdateDashboardBackgroundUsed = { _, _ -> },
            onUpdateDashboardBackgroundOrder = {},
            onUpdateDashboardSettings = { _, _, _ -> },
            isCompact = false
        )
    }
}
