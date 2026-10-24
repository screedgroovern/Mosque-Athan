package com.groovernapp.mosqueathan.ui.settings.fridaysettings

import com.groovernapp.mosqueathan.domain.PickedFile
import com.groovernapp.mosqueathan.domain.FridayMedia
import com.groovernapp.mosqueathan.domain.Media

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
    val media: List<FridayMedia> = emptyList(),
    val globalMedia: List<Media> = emptyList()
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
) {
    /**
     * Whether Friday's visual and audio features do anything.
     *
     * [standbyOnly] blacks the screen out for the whole service, so murottal and the
     * Friday slideshow have nothing to play on. The pane asked
     * `enabled && !standbyOnly` in nine places; it is one rule, so it lives here.
     */
    val visualsEnabled: Boolean get() = enabled && !standbyOnly
}

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

    /**
     * Recitations the user just chose in the platform's audio chooser.
     *
     * Files rather than paths: on the display they are copied into its own storage, and from a
     * controller they are uploaded, because the path in `murottalAudioPaths` has to be one the
     * *display* can open. Only the ViewModel can tell those two apart, so the pane raises this and
     * stays out of it.
     */
    data class MurottalAudioPicked(val files: List<PickedFile>) : FridayEvent

    // Friday slideshow
    data class SlideshowEnabledChanged(val enabled: Boolean) : FridayEvent
    data class IncludeGlobalMediaChanged(val include: Boolean) : FridayEvent
    data class SelectedGlobalMediaChanged(val ids: String) : FridayEvent
    data class TransitionTypeChanged(val type: String) : FridayEvent
    data class IntervalSecondsChanged(val seconds: Int) : FridayEvent
    data class VideoWaitUntilEndChanged(val wait: Boolean) : FridayEvent

    // Friday media
    data class MediaAdded(val uris: List<PickedFile>) : FridayEvent
    data class MediaFileReplaced(val id: String, val uri: PickedFile) : FridayEvent
    data class MediaDeleted(val item: FridayMedia) : FridayEvent
    data class MediaReordered(val ids: List<String>) : FridayEvent

    /** Opens the picker; handled by the pane, which owns the launcher. */
    data object MediaPickRequested : FridayEvent
    data class MediaFilePickRequested(val id: String) : FridayEvent

    /** Navigation, handled by the caller. */
    data object SlideshowPreviewRequested : FridayEvent
}
