package com.groovernapp.mosqueathan.ui.settings

import kotlinx.serialization.json.put
import com.groovernapp.mosqueathan.domain.MediaStore
import com.groovernapp.mosqueathan.domain.PictureKind
import com.groovernapp.mosqueathan.domain.PictureToSend
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
    private val http: HttpFetcher,
    private val knownServers: com.groovernapp.mosqueathan.domain.KnownServersStore
) : ViewModel() {

    /**
     * The category the person was last on.
     *
     * Kept here because this ViewModel outlives the settings screen. A preview replaces the screen
     * with the one being previewed, which removes it from composition and with it every `remember`
     * inside — so coming back from a preview landed on the first category, not the one that held
     * the Preview button just pressed. A plain property: nothing observes it, it is only read once
     * when the screen is composed.
     */
    var lastCategory: SettingsCategory = SettingsCategory.PRAYER_SCHEDULE

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
        val snapshot: MosqueSnapshot,
        /** The display this sync was made against; carried because state may not hold it yet. */
        val host: String
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
    fun syncWithTvServer(address: String? = null) {
        val config = _configState.value
        // The caller's address wins, because Connect and the QR scanner save it and sync in the
        // same handler: the repository write has not travelled back through the config flow yet,
        // so reading state here would use the *previous* display — and after a first-ever QR scan
        // there is no previous one, so the sync silently did nothing at all. Measured on the
        // Realme: the address was stored correctly and the sync went to the old one.
        val host = address?.trim()?.takeIf { it.isNotEmpty() } ?: config.remoteTvIp
        if (config.connectivityMode != "REMOTE" || host.isEmpty()) return

        viewModelScope.launch {
            when (val result = http.getText("http://$host:8080/api/get_config")) {
                is HttpResult.Failure ->
                    _messages.emit(Lang.get("Sync failed: ${result.reason}", "Sinkronisasi gagal: ${result.reason}"))

                is HttpResult.Success -> try {
                    val snapshot = mosqueJson.decodeFromString<MosqueSnapshot>(result.body)
                    // Reached it and understood it: that is what makes it a display worth listing.
                    knownServers.remember(host, snapshot.serverName)
                    val server = SyncConflict.Stamps(snapshot.tableTimestamps)
                    val local = SyncConflict.Stamps(repository.tableTimestamps())

                    if (SyncConflict.needsAsking(server, local)) {
                        _syncConflict.value = SyncConflictPrompt(server, local, snapshot, host)
                    } else {
                        reconcile(SyncConflict.Choice.NewestWins, SyncConflictPrompt(server, local, snapshot, host))
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

    private var lastSeenPictures: List<Any>? = null
    private var refreshingPictures = false

    /**
     * Re-reads the display's pictures, quietly, when they have changed.
     *
     * A controller on Android keeps no connection to the display — it posts each action and forgets
     * — so a picture added at the television, or one this device just uploaded, never reached it
     * until someone pressed Sync. The host calls this on a timer while settings is open, and again
     * as soon as an upload lands.
     *
     * **Pictures only, and the display always wins.** Those rows name files that exist on the
     * display and nowhere else, so there is no local edit worth keeping and nothing to ask about —
     * unlike settings, where this device may hold changes made offline. That is also why this does
     * not touch anything else: pulling settings here would overwrite exactly those changes.
     *
     * Skipped when nothing differs. "Differs" includes the display's table stamps, not only the rows:
     * replacing a picture keeps its path, so the rows are identical and only the stamp moves.
     * Away from the display the request fails and this does nothing, which is the point of keeping
     * a local copy.
     */
    fun refreshMediaFromDisplay() {
        val config = _configState.value
        val host = config.remoteTvIp.trim()
        if (config.connectivityMode != "REMOTE" || host.isEmpty() || refreshingPictures) return
        // A question is open about this very display; its answer decides what happens, not a timer.
        if (_syncConflict.value != null) return

        refreshingPictures = true
        viewModelScope.launch {
            try {
                pullPictures(host, force = false)
            } finally {
                refreshingPictures = false
            }
        }
    }

    /**
     * Replaces this device's picture rows with the display's and saves previews of them.
     *
     * [force] skips the two checks the timer needs and a deliberate sync does not: that this is the
     * display the pictures came from, and that anything has changed since last time.
     */
    private suspend fun pullPictures(host: String, force: Boolean) {
        val result = http.getText("http://$host:8080/api/get_config")
        if (result !is HttpResult.Success) return
        val snapshot = runCatching { mosqueJson.decodeFromString<MosqueSnapshot>(result.body) }
            .getOrNull() ?: return

        val seen = listOf(
            snapshot.mediaList,
            snapshot.fridayMediaList,
            snapshot.dashboardBackgrounds,
            listOf(SnapshotPush.TABLE_MEDIA, SnapshotPush.TABLE_FRIDAY_MEDIA, SnapshotPush.TABLE_BACKGROUNDS)
                .map { snapshot.tableTimestamps[it] ?: 0L }
        )
        if (!force) {
            // Only ever the display these pictures came from. Row ids are unique per display, so
            // sharing even one proves it is the same one. Pointing this controller at a *different*
            // display and replacing its pictures with that one's is how a takmir who then chose
            // "use this device's" lost every picture, with no say in it: that choice is made in
            // the sync dialog, and a timer must not pre-empt it. An empty local list has nothing
            // to lose and takes whatever the display has.
            val localIds = (repository.getMediaList().map { it.id } +
                repository.getFridayMedia().map { it.id } +
                repository.getDashboardBackgrounds().map { it.id }).toSet()
            val displayIds = (snapshot.mediaList.map { it.id } +
                snapshot.fridayMediaList.map { it.id } +
                snapshot.dashboardBackgrounds.map { it.id }).toSet()
            if (localIds.isNotEmpty() && localIds.none { it in displayIds }) return
            if (seen == lastSeenPictures) return
        }

        repository.applySnapshot(
            config = null,
            announcements = null,
            slideshowSchedules = null,
            standbySchedules = null,
            media = snapshot.mediaList,
            fridayMedia = snapshot.fridayMediaList,
            dashboardBackgrounds = snapshot.dashboardBackgrounds
        )
        lastSeenPictures = seen
        keepPicturesForOffline(host, announce = false)
    }

    /**
     * Copies this device's pictures to the display when this device's side won them.
     *
     * "Use this device's" used to leave the display without the pictures — the settings crossed, the
     * pictures stayed behind, and the result read as a sync that had half worked. Only pictures the
     * display does not already hold are sent (its row ids are not ours, so an id it lists is one it
     * already has), which is also what stops a second sync from sending them all again. Videos are
     * counted and left: no copy of one is kept here.
     *
     * Returns whether anything was accepted, which is the signal to re-read the display's list —
     * the new pictures arrive there with the display's own ids.
     */
    private suspend fun sendLocalPictures(
        resolution: SyncConflict.Resolution,
        prompt: SyncConflictPrompt
    ): Boolean {
        fun localWon(table: String) = resolution.sideFor(table) == SyncConflict.Side.Local
        val server = prompt.snapshot
        val onDisplay = (server.mediaList.map { it.id } + server.fridayMediaList.map { it.id } +
            server.dashboardBackgrounds.map { it.id }).toSet()

        var videos = 0
        val pictures = buildList {
            if (localWon(SnapshotPush.TABLE_MEDIA)) {
                repository.getMediaList().filter { it.id !in onDisplay }.sortedBy { it.sortOrder }.forEach {
                    if (it.isVideo) videos++ else add(PictureToSend(it.filePath, PictureKind.Slideshow))
                }
            }
            if (localWon(SnapshotPush.TABLE_FRIDAY_MEDIA)) {
                repository.getFridayMedia().filter { it.id !in onDisplay }.sortedBy { it.sortOrder }.forEach {
                    if (it.isVideo) videos++ else add(PictureToSend(it.filePath, PictureKind.Friday))
                }
            }
            if (localWon(SnapshotPush.TABLE_BACKGROUNDS)) {
                repository.getDashboardBackgrounds().filter { it.id !in onDisplay }.sortedBy { it.sortOrder }
                    .forEach { add(PictureToSend(it.filePath, PictureKind.Dashboard)) }
            }
        }
        if (pictures.isEmpty() && videos == 0) return false

        val sent = if (pictures.isEmpty()) 0 else mediaFileStore.sendPicturesToDisplay(prompt.host, pictures)
        _messages.emit(
            Lang.get(
                "Sent $sent of ${pictures.size} picture(s) to the display." +
                    if (videos > 0) " $videos video(s) cannot be copied." else "",
                "$sent dari ${pictures.size} gambar terkirim ke display." +
                    if (videos > 0) " $videos video tidak dapat disalin." else ""
            )
        )
        return sent > 0
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
            standbySchedules = snapshot.standbySchedules.takeIf { serverWon(SnapshotPush.TABLE_STANDBY) },
            // Pulled but never pushed: the display's rows are the only ones that name files the
            // display has. Without these a controller that connected saw none of its pictures.
            media = snapshot.mediaList.takeIf { serverWon(SnapshotPush.TABLE_MEDIA) },
            fridayMedia = snapshot.fridayMediaList.takeIf { serverWon(SnapshotPush.TABLE_FRIDAY_MEDIA) },
            dashboardBackgrounds = snapshot.dashboardBackgrounds.takeIf { serverWon(SnapshotPush.TABLE_BACKGROUNDS) }
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

        val pulledPictures = listOf(
            SnapshotPush.TABLE_MEDIA, SnapshotPush.TABLE_FRIDAY_MEDIA,
            SnapshotPush.TABLE_BACKGROUNDS, SnapshotPush.TABLE_CONFIG
        ).any(::serverWon)
        if (sendLocalPictures(resolution, prompt)) {
            // The display now holds them under its own ids; take its list so the two agree.
            pullPictures(prompt.host, force = true)
        } else if (pulledPictures) {
            keepPicturesForOffline(prompt.host, announce = true)
        }
    }

    /**
     * Saves a preview of every picture the display holds, so they still show away from it.
     *
     * Read back from this device's own tables rather than from the snapshot: after a sync that left
     * some tables alone, the snapshot names pictures this device did not take, and the saved copies
     * of the ones it kept would be discarded as "no longer listed". Runs after the sync's own
     * message, so a slow connection delays only this and never the confirmation.
     */
    private suspend fun keepPicturesForOffline(host: String, announce: Boolean) {
        val paths = buildList {
            repository.getMediaList().filterNot { it.isVideo }.forEach { add(it.filePath) }
            repository.getFridayMedia().filterNot { it.isVideo }.forEach { add(it.filePath) }
            repository.getDashboardBackgrounds().forEach { add(it.filePath) }
            add(repository.getConfig().customBackgroundPath)
        }.filter { it.isNotBlank() }.distinct()

        val saved = mediaFileStore.cacheForOffline(host, paths)
        if (saved > 0 && announce) {
            _messages.emit(
                Lang.get(
                    "Saved $saved picture(s) on this device for offline use.",
                    "$saved gambar disimpan di perangkat ini untuk dipakai offline."
                )
            )
        }
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
