package com.groovernapp.mosqueathan.ui.settings.fridaysettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import com.groovernapp.mosqueathan.data.media.MediaFileStore
import com.groovernapp.mosqueathan.data.remote.RemoteActionSender
import com.groovernapp.mosqueathan.data.remote.pushConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The Friday category: Friday hours, the pre-adzan murottal, and the Friday-only
 * slideshow.
 *
 * Every setting here is a field of the one config row, so all of them go through
 * [edit], which writes *and* pushes. The Friday slides are the exception: they are
 * their own table, and they are files, so they go through [MediaFileStore] first.
 */
class FridayViewModel(
    private val repository: MosqueConfigRepository,
    private val remote: RemoteActionSender,
    private val files: MediaFileStore
) : ViewModel() {

    val uiState: StateFlow<FridayUiState> =
        combine(
            repository.getConfigFlow(),
            repository.observeFridayMedia(),
            repository.getMediaListFlow()
        ) { config, fridayMedia, globalMedia ->
            FridayUiState(
                enabled = config.jumatEnabled,
                startTime = config.jumatStartTime,
                endTime = config.jumatEndTime,
                dzuhurOffsetMinutes = config.offsetJumat,
                standbyOnly = config.jumatStandbyOnly,
                warningText = config.jumatWarningText,
                murottal = MurottalSettings(
                    enabled = config.murottalEnabled,
                    startDelayMinutes = config.murottalStartDelay,
                    audioPaths = config.murottalAudioPaths
                ),
                slideshow = FridaySlideshowSettings(
                    enabled = config.jumatSlideshowEnabled,
                    includeGlobalMedia = config.includeGlobalSlideshow,
                    selectedGlobalMediaIds = config.selectedGlobalSlideIds,
                    transitionType = config.jumatTransitionType,
                    intervalSeconds = config.jumatSlideIntervalSeconds,
                    intervalUnit = config.jumatSlideIntervalUnit,
                    videoWaitUntilEnd = config.jumatVideoWaitUntilEnd,
                    media = fridayMedia,
                    globalMedia = globalMedia
                )
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FridayUiState()
        )

    fun onEvent(event: FridayEvent) {
        when (event) {
            // --- Friday hours ---
            is FridayEvent.EnabledChanged -> edit { it.copy(jumatEnabled = event.enabled) }
            is FridayEvent.StartTimeChanged -> edit { it.copy(jumatStartTime = event.time) }
            is FridayEvent.EndTimeChanged -> edit { it.copy(jumatEndTime = event.time) }
            is FridayEvent.DzuhurOffsetChanged -> edit { it.copy(offsetJumat = event.minutes) }
            is FridayEvent.StandbyOnlyChanged -> edit { it.copy(jumatStandbyOnly = event.standbyOnly) }
            is FridayEvent.WarningTextChanged -> edit { it.copy(jumatWarningText = event.text) }

            // --- murottal ---
            is FridayEvent.MurottalEnabledChanged -> edit { it.copy(murottalEnabled = event.enabled) }
            is FridayEvent.MurottalStartDelayChanged -> edit { it.copy(murottalStartDelay = event.minutes) }
            is FridayEvent.MurottalAudioPathsChanged -> edit { it.copy(murottalAudioPaths = event.paths) }

            // --- Friday slideshow ---
            is FridayEvent.SlideshowEnabledChanged -> edit { it.copy(jumatSlideshowEnabled = event.enabled) }
            is FridayEvent.IncludeGlobalMediaChanged -> edit { it.copy(includeGlobalSlideshow = event.include) }
            is FridayEvent.SelectedGlobalMediaChanged -> edit { it.copy(selectedGlobalSlideIds = event.ids) }
            is FridayEvent.TransitionTypeChanged -> edit { it.copy(jumatTransitionType = event.type) }
            is FridayEvent.IntervalSecondsChanged -> edit { it.copy(jumatSlideIntervalSeconds = event.seconds) }
            is FridayEvent.IntervalUnitChanged -> edit { it.copy(jumatSlideIntervalUnit = event.unit) }
            is FridayEvent.VideoWaitUntilEndChanged -> edit { it.copy(jumatVideoWaitUntilEnd = event.wait) }

            // --- Friday media ---
            is FridayEvent.MediaAdded -> launch {
                event.uris.forEach { uri ->
                    files.storeFridayMedia(uri)?.let { stored ->
                        repository.addFridayMedia(stored.path, stored.isVideo)
                    }
                }
            }

            is FridayEvent.MediaFileReplaced -> launch {
                repository.getFridayMedia().firstOrNull { it.id == event.id }?.let { item ->
                    if (files.replaceFile(item.filePath, event.uri)) {
                        repository.updateFridayMediaItemPath(event.id, item.filePath)
                    }
                }
            }

            is FridayEvent.MediaDeleted -> launch {
                files.delete(event.item.filePath)
                repository.deleteFridayMedia(event.item.id)
            }

            is FridayEvent.MediaReordered -> launch { repository.updateFridayMediaOrder(event.ids) }

            // Handled elsewhere: the picker launchers live in the pane, because
            // `rememberLauncherForActivityResult` is a composable, and preview swaps
            // which screen `AppViewModel` drives, which is navigation. Listed rather
            // than swept into an `else`, so a genuinely new event still fails to
            // compile until somebody handles it.
            FridayEvent.MediaPickRequested,
            is FridayEvent.MediaFilePickRequested,
            FridayEvent.SlideshowPreviewRequested -> Unit
        }
    }

    /**
     * Writes the change and tells the TV, if this device is controlling one.
     *
     * The push is not optional: on a controller, a setting that only lands locally
     * looks saved and never takes effect. `pushConfig` carries every `jumat*` and
     * `murottal*` field, so one call covers this whole category.
     */
    private fun edit(change: (MosqueConfig) -> MosqueConfig) {
        viewModelScope.launch {
            val updated = change(repository.getConfig())
            repository.updateConfig(updated)
            remote.send("update_config") { pushConfig(updated) }
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
