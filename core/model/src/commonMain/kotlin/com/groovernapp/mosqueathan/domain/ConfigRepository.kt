package com.groovernapp.mosqueathan.domain

import kotlinx.coroutines.flow.Flow

/**
 * Everything the settings UI reads and writes.
 *
 * Extends [DisplayDataSource] rather than repeating its nine reads, which also states the
 * relationship plainly: a controller can do everything a display can, plus change things.
 *
 * This is deliberately one interface rather than one per settings category. Splitting it
 * that way was considered and measured: the five category ViewModels use 3, 5, 8, 10 and
 * 29 members, but 46 of the 55 are also used by the web server, and configuration writes
 * appear in every category. The categories partition the *screens*, not the data, so
 * per-category interfaces would mostly duplicate each other. [DisplayDataSource] is a
 * genuine boundary — read-only versus read-write — and earns its separation; these do not.
 *
 * It exists so `ui:settings` can stop depending on the Realm-backed implementation in
 * `:app` and move to `commonMain`. That is the whole point: the settings UI is what the
 * web panel duplicates, and sharing one Compose implementation is what makes the two stop
 * drifting.
 */
interface ConfigRepository : DisplayDataSource {

    // Configuration
    /**
     * When each table last changed, keyed by table name, for comparing two devices.
     *
     * Empty means "this store cannot say", which is a real answer rather than a failure: the Realm
     * implementation has no such column and never did, so a controller still on Realm reports
     * nothing and a sync falls back to asking the user outright. Deletions are included — the
     * value comes from a table that is written on every mutation rather than from `MAX()` over the
     * rows, because a maximum cannot see a row that is gone.
     *
     * The names are [com.groovernapp.mosqueathan.protocol]'s compared-table names. Kept as strings
     * rather than an enum because they cross the wire and an older peer must be able to send one
     * this build has never heard of without the decode failing.
     */
    suspend fun tableTimestamps(): Map<String, Long>

    suspend fun resetToDefaults()
    suspend fun updateConfig(config: MosqueConfig)

    /**
     * The field-by-field form, used where the caller has loose values rather than a
     * config object — the remote sync, and the panel's own update path.
     */
    suspend fun updateConfig(
        latitude: Double,
        longitude: Double,
        iqomahSubuh: Int,
        iqomahDzuhur: Int,
        iqomahAshar: Int,
        iqomahMaghrib: Int,
        iqomahIsya: Int,
        calculationMethodName: String,
        adzanLeadMinutes: Int,
        offsetImsak: Int = 0,
        offsetSubuh: Int = 0,
        offsetSyuruq: Int = 0,
        offsetDzuhur: Int = 0,
        offsetAshar: Int = 0,
        offsetMaghrib: Int = 0,
        offsetIsya: Int = 0,
        kemenagCityId: String = "",
        kemenagCityName: String = "",
        kemenagDuration: String = "1_MONTH",
        kemenagTargetDate: String = "",
        triggerFetch: Boolean = false,
        adzanFooterText: String? = null,
        iqomahFooterText: String? = null,
        dashboardRandomize: Boolean? = null,
        dashboardTransitionType: String? = null,
        dashboardTransitionInterval: Int? = null
    )
    suspend fun updateConnectivityMode(mode: String)
    suspend fun updateRemoteTvIp(ip: String)
    suspend fun updateShowSettingsOnTv(show: Boolean)
    suspend fun updateStandbyEnabled(enabled: Boolean)
    suspend fun updateStandbySettings(enabled: Boolean, startTime: String, endTime: String)
    suspend fun updateThemeSettings(transitionType: String, intervalSeconds: Int, videoWaitUntilEnd: Boolean, includeDefaultImage: Boolean)

    // Announcements
    suspend fun addAnnouncement(content: String, customId: String = "")
    suspend fun clearAnnouncements()
    suspend fun deleteAnnouncement(id: String)
    suspend fun updateAnnouncement(id: String, content: String, isActive: Boolean)
    suspend fun updateAnnouncementsOrder(ids: List<String>)

    // Media and backgrounds
    suspend fun addDashboardBackground(filePath: String, customId: String = "")
    suspend fun addMediaItem(filePath: String, isVideo: Boolean)
    suspend fun deleteDashboardBackground(id: String)
    suspend fun deleteMediaItem(id: String)
    fun getDashboardBackgrounds(): List<DashboardBackground>
    fun getMediaList(): List<Media>
    suspend fun updateCustomBackground(path: String)
    suspend fun updateDashboardBackgroundOrder(ids: List<String>)
    suspend fun updateDashboardBackgroundUsed(id: String, isUsed: Boolean)
    suspend fun updateMediaItemPath(id: String, filePath: String)
    suspend fun updateMediaItemUsed(id: String, isUsed: Boolean)
    suspend fun updateMediaListOrder(ids: List<String>)
    suspend fun updateSlideshowEnabled(enabled: Boolean)

    // Slideshow and standby schedules
    suspend fun addSlideshowSchedule(day: String, isSpecificDate: Boolean, startTime: String, endTime: String, customId: String = "")
    suspend fun addStandbySchedule(day: String, isSpecificDate: Boolean, startTime: String, endTime: String, customId: String = "")
    suspend fun deleteSlideshowSchedule(id: String)
    suspend fun deleteStandbySchedule(id: String)
    fun getSlideshowSchedulesFlow(): Flow<List<SlideshowSchedule>>
    fun getStandbySchedulesFlow(): Flow<List<StandbySchedule>>
    suspend fun updateSlideshowScheduleDates(id: String, specificDates: String)
    suspend fun updateSlideshowScheduleDays(id: String, activeDays: String)
    suspend fun updateSlideshowScheduleTime(id: String, startTime: String, endTime: String)
    suspend fun updateStandbyScheduleDates(id: String, specificDates: String)
    suspend fun updateStandbyScheduleDays(id: String, activeDays: String)
    suspend fun updateStandbyScheduleTime(id: String, startTime: String, endTime: String)

    // Friday
    suspend fun addFridayMedia(filePath: String, isVideo: Boolean)
    suspend fun deleteFridayMedia(id: String)
    fun getFridayMedia(): List<FridayMedia>
    suspend fun updateFridayMediaItemPath(id: String, filePath: String)
    suspend fun updateFridayMediaOrder(ids: List<String>)

    // Kemenag schedules
    fun getKemenagSchedulesFlow(cityId: String): Flow<List<KemenagSchedule>>
    fun triggerKemenagFetchForDuration(cityId: String, duration: String, targetDateStr: String)

    // Paired devices and access
    suspend fun deleteDevice(id: String)
    fun getConnectedDevicesFlow(): Flow<List<ConnectedDevice>>
    suspend fun setDeviceMaster(id: String)
    suspend fun updatePinCode(newPin: String, changerDeviceId: String?)

    /**
     * Replaces whole tables with another device's copy, all or nothing.
     *
     * **Both directions use this.** A display applies what a controller pushed; a controller
     * applies the snapshot the display just served. That symmetry is why it sits here rather than
     * on [ServerDataSource] — receiving a whole table is not a server's privilege, it is what
     * either end of a sync does.
     *
     * Null means "leave that table alone", which is how one call serves "keep mine wholesale" and
     * "keep whichever side is newer, table by table" without two code paths. An empty *list* is
     * not the same thing: it means empty that table here too, which is how a deletion crosses at
     * all. Additive-only sync is how a running text a takmir removed at home reappears on the
     * television.
     *
     * The three media tables ([media], [fridayMedia], [dashboardBackgrounds]) travel in **one
     * direction only**: a controller takes the display's rows, and never sends its own. Those rows
     * name files, and a file on a phone is not a file on the display — so a display must not accept
     * them (`SyncConflict.PUSHABLE_TABLES` keeps them out of every push, and the server never passes
     * them here). A controller can, because the display owns the files and serves them over HTTP;
     * taking the rows is what lets it list and show them at all. Before this, a controller that
     * connected to a display saw none of its pictures.
     *
     * Four config fields are kept from the row already here whichever way the snapshot travels:
     * `id`, `pinCode` (which never crosses the wire at all), `connectivityMode` and `remoteTvIp`
     * (which describe *this* device). Everything else is taken. The file-bearing fields
     * `customBackgroundPath` and `murottalAudioPaths` are *not* on that list, because which side
     * owns a path depends on the direction — the display owns it, so a push strips them at the
     * boundary while a download adopts them. `SnapshotPush.sanitisedConfig` is that boundary.
     */
    suspend fun applySnapshot(
        config: MosqueConfig?,
        announcements: List<Announcement>?,
        slideshowSchedules: List<SlideshowSchedule>?,
        standbySchedules: List<StandbySchedule>?,
        media: List<Media>? = null,
        fridayMedia: List<FridayMedia>? = null,
        dashboardBackgrounds: List<DashboardBackground>? = null
    )
}
