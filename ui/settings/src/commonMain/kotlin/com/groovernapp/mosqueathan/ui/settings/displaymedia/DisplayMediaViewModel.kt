package com.groovernapp.mosqueathan.ui.settings.displaymedia

import com.groovernapp.mosqueathan.protocol.RemoteActions
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.domain.MediaStore
import com.groovernapp.mosqueathan.protocol.RemoteActionSink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The Display & Media category: slideshow, dashboard backgrounds, running text.
 *
 * **Every branch that changes shared state pushes it to the display too.** A controller writes
 * to its own database, which on the web is not even a database — so a branch that only writes
 * shows the takmir a change the mosque never sees. This used to be true of the theme and the four
 * announcement actions alone, and the twelve media, schedule and background branches below were
 * silently local: deleting a slide from a phone did nothing at all, without an error.
 *
 * The exceptions are the ones where the *display* does the writing: adding or replacing a file
 * goes through `MediaStore`, which uploads, and the upload endpoint adds the row itself. Those
 * branches send nothing because the file transfer already carried the change.
 */
class DisplayMediaViewModel(
    private val repository: ConfigRepository,
    private val remote: RemoteActionSink,
    private val files: MediaStore
) : ViewModel() {

    private val notice = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DisplayMediaUiState> =
        combine(
            combine(
                repository.getConfigFlow(),
                repository.getMediaListFlow(),
                repository.getSlideshowSchedulesFlow(),
                repository.getActiveAnnouncementsFlow(),
                // A flow, not `getDashboardBackgrounds()` inside the transform: that read ran only
                // when one of the other sources emitted, so adding, replacing or deleting a
                // background left the list stale until something unrelated changed.
                repository.getDashboardBackgroundsFlow()
            ) { config, media, schedules, announcements, backgrounds ->
                DisplayMediaUiState(
                    slideshow = SlideshowSettings(
                        enabled = config.slideshowEnabled,
                        transitionType = config.transitionType,
                        intervalSeconds = config.slideIntervalSeconds,
                        videoWaitUntilEnd = config.videoWaitUntilEnd,
                        includeDefaultImage = config.includeDefaultImage,
                        media = media,
                        schedules = schedules
                    ),
                    dashboard = DashboardSettings(
                        randomize = config.dashboardRandomize,
                        transitionType = config.dashboardTransitionType,
                        transitionIntervalSeconds = config.dashboardTransitionInterval,
                        backgrounds = backgrounds
                    ),
                    announcements = announcements
                )
            },
            notice
        ) { state, notice -> state.copy(notice = notice) }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DisplayMediaUiState()
        )

    fun onEvent(event: DisplayMediaEvent) {
        when (event) {
            // --- slideshow appearance ---
            is DisplayMediaEvent.ThemeChanged -> {
                launch {
                    repository.updateThemeSettings(
                        event.transitionType,
                        event.intervalSeconds,
                        event.videoWaitUntilEnd,
                        event.includeDefaultImage
                    )
                }
                remote.send(RemoteActions.UPDATE_THEME) {
                    put("transitionType", event.transitionType)
                    put("slideIntervalSeconds", event.intervalSeconds)
                    put("videoWaitUntilEnd", event.videoWaitUntilEnd)
                    put("includeDefaultImage", event.includeDefaultImage)
                }
            }

            is DisplayMediaEvent.SlideshowEnabledChanged -> {
                launch { repository.updateSlideshowEnabled(event.enabled) }
                remote.send(RemoteActions.UPDATE_SLIDESHOW_ENABLED) { put("enabled", event.enabled) }
            }

            DisplayMediaEvent.SlideshowPreviewRequested -> Unit // handled at the call site

            // --- slideshow media ---
            is DisplayMediaEvent.MediaAdded -> launch {
                var sawLarge = false
                event.uris.forEach { uri ->
                    files.storePresentationMedia(uri)?.let { stored ->
                        sawLarge = sawLarge || stored.wasLarge
                        repository.addMediaItem(stored.path, stored.isVideo)
                    }
                }
                if (sawLarge) notice.value = LARGE_IMAGE_NOTICE
            }

            is DisplayMediaEvent.MediaFileReplaced -> launch {
                repository.getMediaList().firstOrNull { it.id == event.id }?.let { item ->
                    if (files.replaceFile(item.filePath, event.uri)) {
                        repository.updateMediaItemPath(event.id, item.filePath)
                    }
                }
            }

            is DisplayMediaEvent.MediaUsedToggled -> {
                launch { repository.updateMediaItemUsed(event.id, event.used) }
                remote.send(RemoteActions.UPDATE_MEDIA_ITEM_USED) {
                    put("id", event.id)
                    put("isUsed", event.used)
                }
            }

            is DisplayMediaEvent.MediaDeleted -> {
                launch { repository.deleteMediaItem(event.item.id) }
                remote.send(RemoteActions.DELETE_MEDIA) { put("id", event.item.id) }
            }

            is DisplayMediaEvent.MediaReordered -> {
                launch { repository.updateMediaListOrder(event.ids) }
                remote.send(RemoteActions.REORDER_MEDIA) {
                    putJsonArray("ids") { event.ids.forEach { add(it) } }
                }
            }

            // --- schedules ---
            is DisplayMediaEvent.ScheduleAdded -> {
                launch {
                    repository.addSlideshowSchedule(
                        event.day, event.isSpecificDate, event.startTime, event.endTime
                    )
                }
                // No id: the display mints one, as the repository does locally. A controller's
                // own row is never created (its writes are no-ops), so there is no id to agree on
                // -- the broadcast that follows carries the display's.
                remote.send(RemoteActions.ADD_SLIDESHOW_SCHEDULE) {
                    put("day", event.day)
                    put("isSpecificDate", event.isSpecificDate)
                    put("startTime", event.startTime)
                    put("endTime", event.endTime)
                }
            }

            is DisplayMediaEvent.ScheduleTimeChanged -> {
                launch {
                    repository.updateSlideshowScheduleTime(event.schedule.id, event.startTime, event.endTime)
                }
                remote.send(RemoteActions.UPDATE_SLIDESHOW_SCHEDULE_TIME) {
                    put("id", event.schedule.id)
                    put("startTime", event.startTime)
                    put("endTime", event.endTime)
                }
            }

            is DisplayMediaEvent.ScheduleDaysChanged -> {
                launch { repository.updateSlideshowScheduleDays(event.schedule.id, event.activeDays) }
                remote.send(RemoteActions.UPDATE_SLIDESHOW_SCHEDULE_DAYS) {
                    put("id", event.schedule.id)
                    put("activeDays", event.activeDays)
                }
            }

            is DisplayMediaEvent.ScheduleDatesChanged -> {
                launch { repository.updateSlideshowScheduleDates(event.schedule.id, event.specificDates) }
                remote.send(RemoteActions.UPDATE_SLIDESHOW_SCHEDULE_DATES) {
                    put("id", event.schedule.id)
                    put("specificDates", event.specificDates)
                }
            }

            is DisplayMediaEvent.ScheduleDeleted -> {
                launch { repository.deleteSlideshowSchedule(event.id) }
                remote.send(RemoteActions.DELETE_SLIDESHOW_SCHEDULE) { put("id", event.id) }
            }

            // --- dashboard background ---
            is DisplayMediaEvent.DashboardSettingsChanged -> {
                launch {
                    repository.updateConfig(
                        repository.getConfig().copy(
                            dashboardRandomize = event.randomize,
                            dashboardTransitionType = event.transitionType,
                            dashboardTransitionInterval = event.intervalSeconds
                        )
                    )
                }
                remote.send(RemoteActions.UPDATE_DASHBOARD_SETTINGS) {
                    put("dashboardRandomize", event.randomize)
                    put("dashboardTransitionType", event.transitionType)
                    put("dashboardTransitionInterval", event.intervalSeconds)
                }
            }

            is DisplayMediaEvent.CustomBackgroundSelected -> launch {
                files.storeCustomBackground(event.uri)?.let { stored ->
                    repository.updateCustomBackground(stored.path)
                    if (stored.wasLarge) notice.value = LARGE_IMAGE_NOTICE
                }
            }

            DisplayMediaEvent.CustomBackgroundCleared -> {
                launch { repository.updateCustomBackground("") }
                // The display deletes the file as well as clearing the path; a controller cannot
                // reach that file, which is exactly why this has to be the display's job.
                remote.send(RemoteActions.CLEAR_BACKGROUND) {}
            }

            is DisplayMediaEvent.BackgroundsAdded -> launch {
                var sawLarge = false
                event.uris.forEach { uri ->
                    files.storeDashboardBackground(uri)?.let { stored ->
                        sawLarge = sawLarge || stored.wasLarge
                        repository.addDashboardBackground(stored.path)
                    }
                }
                if (sawLarge) notice.value = LARGE_IMAGE_NOTICE
            }

            is DisplayMediaEvent.BackgroundFileReplaced -> launch {
                repository.getDashboardBackgrounds().firstOrNull { it.id == event.id }?.let { bg ->
                    files.replaceFile(bg.filePath, event.uri)
                }
            }

            is DisplayMediaEvent.BackgroundDeleted -> {
                launch { repository.deleteDashboardBackground(event.id) }
                remote.send(RemoteActions.DELETE_DASHBOARD_BACKGROUND) { put("id", event.id) }
            }

            is DisplayMediaEvent.BackgroundUsedToggled -> {
                launch { repository.updateDashboardBackgroundUsed(event.id, event.used) }
                remote.send(RemoteActions.UPDATE_DASHBOARD_BACKGROUND_USED) {
                    put("id", event.id)
                    put("isUsed", event.used)
                }
            }

            is DisplayMediaEvent.BackgroundsReordered -> {
                launch { repository.updateDashboardBackgroundOrder(event.ids) }
                remote.send(RemoteActions.REORDER_DASHBOARD_BACKGROUNDS) {
                    putJsonArray("ids") { event.ids.forEach { add(it) } }
                }
            }

            // --- running text: every one of these also pushes ---
            is DisplayMediaEvent.AnnouncementAdded -> {
                launch { repository.addAnnouncement(event.content) }
                remote.send(RemoteActions.ADD_ANNOUNCEMENT) { put("content", event.content) }
            }

            is DisplayMediaEvent.AnnouncementEdited -> {
                launch { repository.updateAnnouncement(event.id, event.content, true) }
                remote.send(RemoteActions.UPDATE_ANNOUNCEMENT) {
                    put("id", event.id)
                    put("content", event.content)
                    put("isActive", true)
                }
            }

            is DisplayMediaEvent.AnnouncementDeleted -> {
                launch { repository.deleteAnnouncement(event.id) }
                remote.send(RemoteActions.DELETE_ANNOUNCEMENT) { put("id", event.id) }
            }

            is DisplayMediaEvent.AnnouncementsReordered -> {
                launch { repository.updateAnnouncementsOrder(event.ids) }
                // REORDER_ANNOUNCEMENTS, not "update_announcements_order": the server
                // never had a branch for that name, so reordering the running text from
                // a controller silently did nothing. Payload shape already matched.
                remote.send(RemoteActions.REORDER_ANNOUNCEMENTS) {
                    putJsonArray("ids") { event.ids.forEach { add(it) } }
                }
            }

            DisplayMediaEvent.NoticeShown -> notice.value = null

            // Handled by the pane, which owns the file-picker launchers. Listed
            // rather than swept into an `else` so that adding a real event still
            // fails to compile until it is handled.
            DisplayMediaEvent.MediaPickRequested,
            is DisplayMediaEvent.MediaFilePickRequested,
            DisplayMediaEvent.CustomBackgroundPickRequested,
            DisplayMediaEvent.BackgroundPickRequested,
            is DisplayMediaEvent.BackgroundFilePickRequested -> Unit
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val LARGE_IMAGE_NOTICE =
            "Gambar besar terdeteksi. Ukurannya dioptimalkan otomatis."
    }
}
