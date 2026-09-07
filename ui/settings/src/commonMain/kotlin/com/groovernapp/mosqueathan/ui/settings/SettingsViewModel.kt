package com.groovernapp.mosqueathan.ui.settings

import kotlinx.serialization.json.put
import com.groovernapp.mosqueathan.domain.MediaStore
import com.groovernapp.mosqueathan.protocol.HttpFetcher
import com.groovernapp.mosqueathan.protocol.RemoteActionSink
import com.groovernapp.mosqueathan.protocol.RemoteActions
import com.groovernapp.mosqueathan.protocol.SnapshotPush
import com.groovernapp.mosqueathan.protocol.SyncConflict
import com.groovernapp.mosqueathan.protocol.putSnapshotPush
import com.groovernapp.mosqueathan.protocol.HttpResult
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.MosqueSnapshot
import com.groovernapp.mosqueathan.domain.mosqueJson
import com.groovernapp.mosqueathan.domain.Announcement
import com.groovernapp.mosqueathan.domain.Media
import com.groovernapp.mosqueathan.domain.ConnectedDevice
import com.groovernapp.mosqueathan.domain.FridayMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: ConfigRepository,
    private val remote: RemoteActionSink,
    private val mediaFileStore: MediaStore,
    private val http: HttpFetcher
) : ViewModel() {

    /**
     * Short messages for the user — sync succeeded, sync failed.
     *
     * A flow rather than a `Toast`, because a `Toast` needs a `Context` and that is the
     * single reason `syncWithTvServer` and `resetToDefaults` used to take one. The
     * screen decides how to show these; the ViewModel only says what happened.
     *
     * `SharedFlow` with no replay: a message is an event, and re-showing "sync failed"
     * on every recomposition would be worse than dropping it.
     */
    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()


    private val _configState = MutableStateFlow(MosqueConfig())
    val configState = _configState.asStateFlow()

    private val _announcementsState = MutableStateFlow<List<Announcement>>(emptyList())
    val announcementsState = _announcementsState.asStateFlow()

    private val _devicesState = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val devicesState = _devicesState.asStateFlow()

    private val _mediaListState = MutableStateFlow<List<Media>>(emptyList())
    val mediaListState = _mediaListState.asStateFlow()



    init {
        viewModelScope.launch {
            repository.getConfigFlow().collect { config ->
                _configState.value = config
            }
        }
        viewModelScope.launch {
            repository.getActiveAnnouncementsFlow().collect {
                _announcementsState.value = it
            }
        }
        viewModelScope.launch {
            repository.getConnectedDevicesFlow().collect {
                _devicesState.value = it
            }
        }
        viewModelScope.launch {
            repository.getMediaListFlow().collect {
                _mediaListState.value = it
            }
        }
    }




    fun resetToDefaults() {
        viewModelScope.launch {
            mediaFileStore.deleteCustomBackground()
            repository.resetToDefaults()
        }
        // The display also deletes its custom background file, which no controller can reach.
        remote.send(RemoteActions.RESET) {}
    }

    private val _slideshowSchedulesState = MutableStateFlow<List<com.groovernapp.mosqueathan.domain.SlideshowSchedule>>(emptyList())
    val slideshowSchedulesState = _slideshowSchedulesState.asStateFlow()

    private val _dashboardBackgroundsState = MutableStateFlow<List<com.groovernapp.mosqueathan.domain.DashboardBackground>>(emptyList())
    val dashboardBackgroundsState = _dashboardBackgroundsState.asStateFlow()

    private val _standbySchedulesState = MutableStateFlow<List<com.groovernapp.mosqueathan.domain.StandbySchedule>>(emptyList())
    val standbySchedulesState = _standbySchedulesState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSlideshowSchedulesFlow().collect {
                _slideshowSchedulesState.value = it
            }
        }
        viewModelScope.launch {
            repository.getDashboardBackgroundsFlow().collect {
                _dashboardBackgroundsState.value = it
            }
        }
        viewModelScope.launch {
            repository.getStandbySchedulesFlow().collect {
                _standbySchedulesState.value = it
            }
        }
    }

    fun addStandbySchedule(day: String, isSpecificDate: Boolean, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.addStandbySchedule(day, isSpecificDate, startTime, endTime)
        }
        // No id, as with the slideshow schedules: the display mints it and broadcasts the row.
        remote.send(RemoteActions.ADD_STANDBY_SCHEDULE) {
            put("day", day)
            put("isSpecificDate", isSpecificDate)
            put("startTime", startTime)
            put("endTime", endTime)
        }
    }

    fun deleteStandbySchedule(id: String) {
        viewModelScope.launch {
            repository.deleteStandbySchedule(id)
        }
        remote.send(RemoteActions.DELETE_STANDBY_SCHEDULE) { put("id", id) }
    }

    /**
     * What the takmir is being asked about, or null when nothing is being asked.
     *
     * The snapshot is held here rather than re-fetched on the answer, because between the question
     * and the answer someone may be standing at the television changing things — and then the
     * timestamps the takmir was shown would not be the ones acted on.
     */
    data class SyncConflictPrompt(
        val server: SyncConflict.Stamps,
        val local: SyncConflict.Stamps,
        val snapshot: MosqueSnapshot
    )

    private val _syncConflict = MutableStateFlow<SyncConflictPrompt?>(null)
    val syncConflict = _syncConflict.asStateFlow()

    /**
     * Fetches the display's settings and reconciles them with this device's.
     *
     * ### What changed, and why the old shape could not be kept
     *
     * This used to be one GET followed by writing the display's values over this device's — 23 of
     * the config's 51 syncable fields read key by key, both schedule tables ignored entirely, and
     * `clearAnnouncements()` called unconditionally, so a takmir's own edits were gone with no
     * question asked. The direction only ever ran server → local. There was no way to hand an
     * evening's work at home over to the television except retyping it there.
     *
     * Now the payload decodes as [MosqueSnapshot] — the class the display already serves — so the
     * field list is the class rather than a list to keep in step, and both schedule tables arrive.
     * Then the timestamps decide: if both sides have written since they last agreed, the takmir is
     * asked, and nothing is written until they answer.
     *
     * ### When it does not ask
     *
     * `SyncConflict.needsAsking` is false when only one side has ever written, and then
     * `NewestWins` does the right thing without a dialog: a fresh controller takes everything from
     * an established display, and a display that has never been configured takes this device's
     * copy. Asking there is how a dialog earns the reflex that dismisses it on the one occasion it
     * mattered.
     */
    fun syncWithTvServer() {
        val config = _configState.value
        if (config.connectivityMode != "REMOTE" || config.remoteTvIp.isEmpty()) return

        viewModelScope.launch {
            when (val result = http.getText("http://${config.remoteTvIp}:8080/api/get_config")) {
                is HttpResult.Failure ->
                    _messages.emit(Lang.get("Sync failed: ${result.reason}", "Sinkronisasi gagal: ${result.reason}"))

                is HttpResult.Success -> try {
                    val snapshot = mosqueJson.decodeFromString<MosqueSnapshot>(result.body)
                    val server = SyncConflict.Stamps(snapshot.tableTimestamps)
                    val local = SyncConflict.Stamps(repository.tableTimestamps())

                    if (SyncConflict.needsAsking(server, local)) {
                        _syncConflict.value = SyncConflictPrompt(server, local, snapshot)
                    } else {
                        reconcile(SyncConflict.Choice.NewestWins, SyncConflictPrompt(server, local, snapshot))
                    }
                } catch (e: Exception) {
                    // The fetch succeeded and the body did not parse: a display running
                    // different code, or something else answering on that address.
                    val reason = e.message ?: "unreadable response"
                    _messages.emit(
                        Lang.get("Sync failed: $reason", "Sinkronisasi gagal: $reason")
                    )
                }
            }
        }
    }

    /** The takmir picked one of the three; carry it out and close the dialog. */
    fun onSyncChoice(choice: SyncConflict.Choice) {
        val prompt = _syncConflict.value ?: return
        _syncConflict.value = null
        viewModelScope.launch { reconcile(choice, prompt) }
    }

    /** Cancelled: nothing is written on either side, and the two remain as they were. */
    fun dismissSyncConflict() {
        _syncConflict.value = null
    }

    /**
     * Applies [choice], pulling the tables the display won and pushing the tables this device won.
     *
     * **One mechanism serves all three options**, which is the point of the shape: `TakeServer`
     * pushes nothing, `TakeLocal` pushes every table it can, and `NewestWins` pushes the ones this
     * device wrote more recently — and no branch here has to know which of the three it is doing.
     *
     * The pull runs first. A table this device won is not touched by it (that argument is null),
     * so the order matters only in that a display which rejects the push leaves this device
     * already holding the tables it agreed to take, rather than nothing.
     */
    private suspend fun reconcile(choice: SyncConflict.Choice, prompt: SyncConflictPrompt) {
        val resolution = SyncConflict.resolve(choice, prompt.server, prompt.local)
        fun serverWon(table: String) = resolution.sideFor(table) == SyncConflict.Side.Server
        val snapshot = prompt.snapshot

        repository.applySnapshot(
            config = snapshot.config.takeIf { serverWon(SnapshotPush.TABLE_CONFIG) },
            announcements = snapshot.announcements.takeIf { serverWon(SnapshotPush.TABLE_ANNOUNCEMENTS) },
            slideshowSchedules = snapshot.slideshowSchedules.takeIf { serverWon(SnapshotPush.TABLE_SLIDESHOW) },
            standbySchedules = snapshot.standbySchedules.takeIf { serverWon(SnapshotPush.TABLE_STANDBY) }
        )

        val toPush = SyncConflict.tablesToPush(choice, prompt.server, prompt.local)
        if (toPush.isNotEmpty()) {
            val push = SnapshotPush.of(
                tables = toPush,
                config = { _configState.value },
                announcements = { _announcementsState.value },
                slideshowSchedules = { repository.getSlideshowSchedules() },
                standbySchedules = { repository.getStandbySchedules() }
            )
            remote.send(RemoteActions.APPLY_SNAPSHOT) { putSnapshotPush(push) }
        }

        _messages.emit(
            when {
                toPush.isEmpty() -> Lang.get("Sync complete.", "Sinkronisasi berhasil!")
                else -> Lang.get(
                    "Sync complete. Sent ${toPush.size} section(s) to the display.",
                    "Sinkronisasi berhasil! ${toPush.size} bagian dikirim ke display."
                )
            }
        )
    }

    fun updateStandbyScheduleTime(id: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.updateStandbyScheduleTime(id, startTime, endTime)
        }
        remote.send(RemoteActions.UPDATE_STANDBY_SCHEDULE_TIME) {
            put("id", id)
            put("startTime", startTime)
            put("endTime", endTime)
        }
    }

    fun updateStandbyScheduleDays(id: String, activeDays: String) {
        viewModelScope.launch {
            repository.updateStandbyScheduleDays(id, activeDays)
        }
        remote.send(RemoteActions.UPDATE_STANDBY_SCHEDULE_DAYS) {
            put("id", id)
            put("activeDays", activeDays)
        }
    }

    fun updateStandbyScheduleDates(id: String, specificDates: String) {
        viewModelScope.launch {
            repository.updateStandbyScheduleDates(id, specificDates)
        }
        remote.send(RemoteActions.UPDATE_STANDBY_SCHEDULE_DATES) {
            put("id", id)
            put("specificDates", specificDates)
        }
    }

    fun updateStandbyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateStandbyEnabled(enabled)
        }
        // update_standby is partial on the display: a key left out keeps the value it already
        // holds, so sending only the flag cannot blank the window times.
        remote.send(RemoteActions.UPDATE_STANDBY) { put("standbyModeEnabled", enabled) }
    }

}
