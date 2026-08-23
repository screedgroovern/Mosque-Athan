package com.groovernapp.mosqueathan.ui.settings.displaymedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import com.groovernapp.mosqueathan.data.media.MediaFileStore
import com.groovernapp.mosqueathan.data.remote.RemoteActionSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * The Display & Media category: slideshow, dashboard backgrounds, running text.
 *
 * Five of these actions must reach the display as well as the database —
 * `update_theme` and the four announcement actions. That is not incidental: a
 * controller device that saves an announcement locally and never pushes it shows
 * the takmir a saved announcement the mosque never sees. Every branch below that
 * writes an announcement or the theme sends its action too.
 */
class DisplayMediaViewModel(
    private val repository: MosqueConfigRepository,
    private val remote: RemoteActionSender,
    private val files: MediaFileStore
) : ViewModel() {

    private val notice = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DisplayMediaUiState> =
        combine(
            repository.getConfigFlow(),
            repository.getMediaListFlow(),
            repository.getSlideshowSchedulesFlow(),
            repository.getActiveAnnouncementsFlow(),
            notice
        ) { config, media, schedules, announcements, notice ->
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
                    backgrounds = repository.getDashboardBackgrounds()
                ),
                announcements = announcements,
                notice = notice
            )
        }.stateIn(
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
                remote.send("update_theme") {
                    put("transitionType", event.transitionType)
                    put("slideIntervalSeconds", event.intervalSeconds)
                    put("videoWaitUntilEnd", event.videoWaitUntilEnd)
                    put("includeDefaultImage", event.includeDefaultImage)
                }
            }

            is DisplayMediaEvent.SlideshowEnabledChanged ->
                launch { repository.updateSlideshowEnabled(event.enabled) }

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

            is DisplayMediaEvent.MediaUsedToggled ->
                launch { repository.updateMediaItemUsed(event.id, event.used) }

            is DisplayMediaEvent.MediaDeleted ->
                launch { repository.deleteMediaItem(event.item.id) }

            is DisplayMediaEvent.MediaReordered ->
                launch { repository.updateMediaListOrder(event.ids) }

            // --- schedules ---
            is DisplayMediaEvent.ScheduleAdded -> launch {
                repository.addSlideshowSchedule(
                    event.day, event.isSpecificDate, event.startTime, event.endTime
                )
            }

            is DisplayMediaEvent.ScheduleTimeChanged -> launch {
                repository.updateSlideshowScheduleTime(event.schedule.id, event.startTime, event.endTime)
            }

            is DisplayMediaEvent.ScheduleDaysChanged -> launch {
                repository.updateSlideshowScheduleDays(event.schedule.id, event.activeDays)
            }

            is DisplayMediaEvent.ScheduleDatesChanged -> launch {
                repository.updateSlideshowScheduleDates(event.schedule.id, event.specificDates)
            }

            is DisplayMediaEvent.ScheduleDeleted ->
                launch { repository.deleteSlideshowSchedule(event.id) }

            // --- dashboard background ---
            is DisplayMediaEvent.DashboardSettingsChanged -> launch {
                repository.updateConfig(
                    repository.getConfig().copy(
                        dashboardRandomize = event.randomize,
                        dashboardTransitionType = event.transitionType,
                        dashboardTransitionInterval = event.intervalSeconds
                    )
                )
            }

            is DisplayMediaEvent.CustomBackgroundSelected -> launch {
                files.storeCustomBackground(event.uri)?.let { stored ->
                    repository.updateCustomBackground(stored.path)
                    if (stored.wasLarge) notice.value = LARGE_IMAGE_NOTICE
                }
            }

            DisplayMediaEvent.CustomBackgroundCleared ->
                launch { repository.updateCustomBackground("") }

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

            is DisplayMediaEvent.BackgroundDeleted ->
                launch { repository.deleteDashboardBackground(event.id) }

            is DisplayMediaEvent.BackgroundUsedToggled ->
                launch { repository.updateDashboardBackgroundUsed(event.id, event.used) }

            is DisplayMediaEvent.BackgroundsReordered ->
                launch { repository.updateDashboardBackgroundOrder(event.ids) }

            // --- running text: every one of these also pushes ---
            is DisplayMediaEvent.AnnouncementAdded -> {
                launch { repository.addAnnouncement(event.content) }
                remote.send("add_announcement") { put("content", event.content) }
            }

            is DisplayMediaEvent.AnnouncementEdited -> {
                launch { repository.updateAnnouncement(event.id, event.content, true) }
                remote.send("update_announcement") {
                    put("id", event.id)
                    put("content", event.content)
                    put("isActive", true)
                }
            }

            is DisplayMediaEvent.AnnouncementDeleted -> {
                launch { repository.deleteAnnouncement(event.id) }
                remote.send("delete_announcement") { put("id", event.id) }
            }

            is DisplayMediaEvent.AnnouncementsReordered -> {
                launch { repository.updateAnnouncementsOrder(event.ids) }
                remote.send("update_announcements_order") {
                    put("ids", JSONArray().apply { event.ids.forEach { put(it) } })
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
