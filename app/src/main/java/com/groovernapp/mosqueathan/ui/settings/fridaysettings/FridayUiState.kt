package com.groovernapp.mosqueathan.ui.settings.fridaysettings

import android.net.Uri
import com.groovernapp.mosqueathan.data.FridayMediaEntity
import com.groovernapp.mosqueathan.data.MediaEntity

/** The Quran recitation played before the Friday sermon. */
data class MurottalSettings(
    val enabled: Boolean = false,
    val startDelayMinutes: Int = 0,
    val audioPaths: String = ""
)

/**
 * The slideshow shown during Friday hours.
 *
 * Separate from the everyday slideshow, but able to borrow from it:
 * [includeGlobalMedia] and [selectedGlobalMediaIds] pick items out of
 * [globalMedia], the ordinary media library.
 */
data class FridaySlideshowSettings(
    val enabled: Boolean = false,
    val includeGlobalMedia: Boolean = false,
    val selectedGlobalMediaIds: String = "",
    val transitionType: String = "",
    val intervalSeconds: Int = 0,
    val intervalUnit: String = "",
    val videoWaitUntilEnd: Boolean = false,
    val media: List<FridayMediaEntity> = emptyList(),
    val globalMedia: List<MediaEntity> = emptyList()
)

/**
 * Everything the Friday pane draws.
 *
 * Replaces forty-one parameters — the worst signature in the app. They grouped
 * cleanly into four concerns once written down, which is most of the argument for
 * having done this: the old list interleaved Friday timing, murottal and slideshow
 * settings with no indication that they were separate things.
 */
data class FridayUiState(
    val enabled: Boolean = false,
    val startTime: String = "",
    val endTime: String = "",
    val dzuhurOffsetMinutes: Int = 0,
    val standbyOnly: Boolean = false,
    val warningText: String = "",
    val murottal: MurottalSettings = MurottalSettings(),
    val slideshow: FridaySlideshowSettings = FridaySlideshowSettings()
)

sealed interface FridayEvent {
    // Friday hours
    data class EnabledChanged(val enabled: Boolean) : FridayEvent
    data class StartTimeChanged(val time: String) : FridayEvent
    data class EndTimeChanged(val time: String) : FridayEvent
    data class DzuhurOffsetChanged(val minutes: Int) : FridayEvent
    data class StandbyOnlyChanged(val standbyOnly: Boolean) : FridayEvent
    data class WarningTextChanged(val text: String) : FridayEvent

    // Murottal
    data class MurottalEnabledChanged(val enabled: Boolean) : FridayEvent
    data class MurottalStartDelayChanged(val minutes: Int) : FridayEvent
    data class MurottalAudioPathsChanged(val paths: String) : FridayEvent

    // Friday slideshow
    data class SlideshowEnabledChanged(val enabled: Boolean) : FridayEvent
    data class IncludeGlobalMediaChanged(val include: Boolean) : FridayEvent
    data class SelectedGlobalMediaChanged(val ids: String) : FridayEvent
    data class TransitionTypeChanged(val type: String) : FridayEvent
    data class IntervalSecondsChanged(val seconds: Int) : FridayEvent
    data class IntervalUnitChanged(val unit: String) : FridayEvent
    data class VideoWaitUntilEndChanged(val wait: Boolean) : FridayEvent

    // Friday media
    data class MediaAdded(val uris: List<Uri>) : FridayEvent
    data class MediaFileReplaced(val id: String, val uri: Uri) : FridayEvent
    data class MediaDeleted(val item: FridayMediaEntity) : FridayEvent
    data class MediaReordered(val ids: List<String>) : FridayEvent

    /** Opens the picker; handled by the pane, which owns the launcher. */
    data object MediaPickRequested : FridayEvent
    data class MediaFilePickRequested(val id: String) : FridayEvent

    /** Navigation, handled by the caller. */
    data object SlideshowPreviewRequested : FridayEvent
}
