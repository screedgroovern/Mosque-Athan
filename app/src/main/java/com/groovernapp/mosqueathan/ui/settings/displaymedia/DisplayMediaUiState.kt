package com.groovernapp.mosqueathan.ui.settings.displaymedia

import android.net.Uri
import com.groovernapp.mosqueathan.data.AnnouncementEntity
import com.groovernapp.mosqueathan.data.DashboardBackgroundEntity
import com.groovernapp.mosqueathan.data.MediaEntity
import com.groovernapp.mosqueathan.data.ScheduleItem
import com.groovernapp.mosqueathan.data.SlideshowScheduleEntity

/** The slideshow that replaces the dashboard on a schedule. */
data class SlideshowSettings(
    val enabled: Boolean = false,
    val transitionType: String = "",
    val intervalSeconds: Int = 0,
    val videoWaitUntilEnd: Boolean = false,
    val includeDefaultImage: Boolean = true,
    val media: List<MediaEntity> = emptyList(),
    val schedules: List<SlideshowScheduleEntity> = emptyList()
)

/** The dashboard's rotating background images. */
data class DashboardSettings(
    val randomize: Boolean = false,
    val transitionType: String = "",
    val transitionIntervalSeconds: Int = 0,
    val backgrounds: List<DashboardBackgroundEntity> = emptyList()
)

/**
 * Everything the Display & Media pane draws.
 *
 * Grouped rather than flat: this category really does cover three separate things —
 * the slideshow, the dashboard backgrounds, and the running text — and the old
 * signature interleaved all three across thirty-two parameters.
 */
data class DisplayMediaUiState(
    val slideshow: SlideshowSettings = SlideshowSettings(),
    val dashboard: DashboardSettings = DashboardSettings(),
    val announcements: List<AnnouncementEntity> = emptyList(),
    /** A one-shot message for the user; cleared by [DisplayMediaEvent.NoticeShown]. */
    val notice: String? = null
)

/**
 * Everything the pane can ask for.
 *
 * The events carrying [Uri] are the platform boundary of this category: a picked
 * file only exists as a content URI, and resolving one needs Android. They are
 * handled through `MediaFileStore`, which is what becomes `expect`/`actual` when the
 * UI moves to `commonMain`.
 */
sealed interface DisplayMediaEvent {
    // Slideshow appearance
    data class ThemeChanged(
        val transitionType: String,
        val intervalSeconds: Int,
        val videoWaitUntilEnd: Boolean,
        val includeDefaultImage: Boolean
    ) : DisplayMediaEvent

    data class SlideshowEnabledChanged(val enabled: Boolean) : DisplayMediaEvent
    data object SlideshowPreviewRequested : DisplayMediaEvent

    // Slideshow media
    data class MediaAdded(val uris: List<Uri>) : DisplayMediaEvent
    data class MediaFileReplaced(val id: String, val uri: Uri) : DisplayMediaEvent
    data class MediaUsedToggled(val id: String, val used: Boolean) : DisplayMediaEvent
    data class MediaDeleted(val item: MediaEntity) : DisplayMediaEvent
    data class MediaReordered(val ids: List<String>) : DisplayMediaEvent

    // Slideshow schedules
    data class ScheduleAdded(
        val day: String,
        val isSpecificDate: Boolean,
        val startTime: String,
        val endTime: String
    ) : DisplayMediaEvent

    data class ScheduleTimeChanged(val schedule: ScheduleItem, val startTime: String, val endTime: String) : DisplayMediaEvent
    data class ScheduleDaysChanged(val schedule: ScheduleItem, val activeDays: String) : DisplayMediaEvent
    data class ScheduleDatesChanged(val schedule: ScheduleItem, val specificDates: String) : DisplayMediaEvent
    data class ScheduleDeleted(val id: String) : DisplayMediaEvent

    // Dashboard background
    data class DashboardSettingsChanged(
        val randomize: Boolean,
        val transitionType: String,
        val intervalSeconds: Int
    ) : DisplayMediaEvent

    data class CustomBackgroundSelected(val uri: Uri) : DisplayMediaEvent
    data object CustomBackgroundCleared : DisplayMediaEvent
    data class BackgroundsAdded(val uris: List<Uri>) : DisplayMediaEvent
    data class BackgroundFileReplaced(val id: String, val uri: Uri) : DisplayMediaEvent
    data class BackgroundDeleted(val id: String) : DisplayMediaEvent
    data class BackgroundUsedToggled(val id: String, val used: Boolean) : DisplayMediaEvent
    data class BackgroundsReordered(val ids: List<String>) : DisplayMediaEvent

    // Running text
    data class AnnouncementAdded(val content: String) : DisplayMediaEvent
    data class AnnouncementEdited(val id: String, val content: String) : DisplayMediaEvent
    data class AnnouncementDeleted(val id: String) : DisplayMediaEvent
    data class AnnouncementsReordered(val ids: List<String>) : DisplayMediaEvent

    /**
     * "Open the file picker."
     *
     * These do not reach the ViewModel: the launcher lives in the pane, because
     * `rememberLauncherForActivityResult` is a composable. The pane intercepts them
     * and forwards everything else.
     */
    data object MediaPickRequested : DisplayMediaEvent
    data class MediaFilePickRequested(val id: String) : DisplayMediaEvent
    data object CustomBackgroundPickRequested : DisplayMediaEvent
    data object BackgroundPickRequested : DisplayMediaEvent
    data class BackgroundFilePickRequested(val id: String) : DisplayMediaEvent

    data object NoticeShown : DisplayMediaEvent
}
