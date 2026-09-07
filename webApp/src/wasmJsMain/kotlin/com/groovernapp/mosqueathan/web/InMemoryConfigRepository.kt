package com.groovernapp.mosqueathan.web

import com.groovernapp.mosqueathan.domain.Announcement
import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.domain.ConnectedDevice
import com.groovernapp.mosqueathan.domain.DashboardBackground
import com.groovernapp.mosqueathan.domain.FridayMedia
import com.groovernapp.mosqueathan.domain.KemenagSchedule
import com.groovernapp.mosqueathan.domain.Media
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.MosqueSnapshot
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.domain.SlideshowSchedule
import com.groovernapp.mosqueathan.domain.StandbySchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A [ConfigRepository] held entirely in memory.
 *
 * It is no longer scaffolding. [DisplayLink] fills it from the snapshots the display pushes, and
 * the settings ViewModels send every change onward through `RemoteActionSink` — so a write updates
 * this copy for immediate feedback and the display's echo then overwrites it. The display stays
 * authoritative; nothing here merges.
 *
 * Still in memory rather than IndexedDB, which is the right shape for now: a *connected*
 * controller is a client. Local storage becomes necessary only for the offline replica — the
 * takmir who edits at home — and that needs the three-snapshot merge model, not just a store.
 */
class InMemoryConfigRepository(
    /**
     * The display this browser controls. Seeded into the config because the *role* is a local fact
     * the display never sends — its snapshot omits `connectivityMode` and `remoteTvIp`, so without
     * this the panel reads back the default and claims to be a server.
     */
    displayAddress: String = ""
) : ConfigRepository {

    private val config = MutableStateFlow(
        MosqueConfig(connectivityMode = REMOTE_MODE, remoteTvIp = displayAddress)
    )
    private val media = MutableStateFlow<List<Media>>(emptyList())
    private val backgrounds = MutableStateFlow<List<DashboardBackground>>(emptyList())
    private val fridayMedia = MutableStateFlow<List<FridayMedia>>(emptyList())
    private val announcements = MutableStateFlow<List<Announcement>>(emptyList())
    private val devices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    private val slideshowSchedules = MutableStateFlow<List<SlideshowSchedule>>(emptyList())
    private val standbySchedules = MutableStateFlow<List<StandbySchedule>>(emptyList())
    private val kemenagSchedules = MutableStateFlow<List<KemenagSchedule>>(emptyList())

    /**
     * Replaces everything with a snapshot from the display.
     *
     * The device-specific fields are carried over rather than taken from the snapshot: which role
     * this device plays, which display it talks to, and its PIN are facts about *this* device. The
     * display does not send them at all, so adopting the decoded defaults would silently flip the
     * panel into "server" mode on the first snapshot.
     */
    fun apply(snapshot: MosqueSnapshot) {
        val local = config.value
        config.value = snapshot.config.copy(
            id = local.id,
            connectivityMode = local.connectivityMode,
            remoteTvIp = local.remoteTvIp,
            pinCode = local.pinCode
        )
        media.value = snapshot.mediaList
        backgrounds.value = snapshot.dashboardBackgrounds
        fridayMedia.value = snapshot.fridayMediaList
        announcements.value = snapshot.announcements
        devices.value = snapshot.devices
        slideshowSchedules.value = snapshot.slideshowSchedules
        standbySchedules.value = snapshot.standbySchedules
    }

    override fun getConfigFlow(): Flow<MosqueConfig> = config.asStateFlow()
    override fun getConfig(): MosqueConfig = config.value
    override fun getMediaListFlow(): Flow<List<Media>> = media.asStateFlow()
    override fun getDashboardBackgroundsFlow(): Flow<List<DashboardBackground>> = backgrounds.asStateFlow()
    override fun observeFridayMedia(): Flow<List<FridayMedia>> = fridayMedia.asStateFlow()
    override fun getActiveAnnouncementsFlow(): Flow<List<Announcement>> = announcements.asStateFlow()
    override fun getStandbySchedules(): List<StandbySchedule> = standbySchedules.value
    override fun getSlideshowSchedules(): List<SlideshowSchedule> = slideshowSchedules.value
    override fun getIqomahMinutes(prayerType: PrayerType): Int = 0
    /**
     * Empty, because this store is not one: the page holds the display's own snapshot and has no
     * local writes of its own to date. A browser cannot meaningfully offer "keep this device's
     * copy" until the offline replica lands, and reporting nothing is what keeps the sync from
     * pretending otherwise.
     */
    override suspend fun tableTimestamps(): Map<String, Long> = emptyMap()

    override suspend fun resetToDefaults() = Unit
    override suspend fun updateConfig(config: MosqueConfig) = Unit
    override suspend fun updateConnectivityMode(mode: String) = Unit
    override suspend fun updateRemoteTvIp(ip: String) = Unit
    override suspend fun updateShowSettingsOnTv(show: Boolean) = Unit
    override suspend fun updateStandbyEnabled(enabled: Boolean) = Unit
    override suspend fun updateStandbySettings(enabled: Boolean, startTime: String, endTime: String) = Unit
    override suspend fun updateThemeSettings(transitionType: String, intervalSeconds: Int, videoWaitUntilEnd: Boolean, includeDefaultImage: Boolean) = Unit
    override suspend fun addAnnouncement(content: String, customId: String) = Unit
    override suspend fun clearAnnouncements() = Unit
    override suspend fun deleteAnnouncement(id: String) = Unit
    override suspend fun updateAnnouncement(id: String, content: String, isActive: Boolean) = Unit
    override suspend fun updateAnnouncementsOrder(ids: List<String>) = Unit
    override suspend fun addDashboardBackground(filePath: String, customId: String) = Unit
    override suspend fun addMediaItem(filePath: String, isVideo: Boolean) = Unit
    override suspend fun deleteDashboardBackground(id: String) = Unit
    override suspend fun deleteMediaItem(id: String) = Unit
    override fun getDashboardBackgrounds(): List<DashboardBackground> = backgrounds.value
    override fun getMediaList(): List<Media> = media.value
    override suspend fun updateCustomBackground(path: String) = Unit
    override suspend fun updateDashboardBackgroundOrder(ids: List<String>) = Unit
    override suspend fun updateDashboardBackgroundUsed(id: String, isUsed: Boolean) = Unit
    override suspend fun updateMediaItemPath(id: String, filePath: String) = Unit
    override suspend fun updateMediaItemUsed(id: String, isUsed: Boolean) = Unit
    override suspend fun updateMediaListOrder(ids: List<String>) = Unit
    override suspend fun updateSlideshowEnabled(enabled: Boolean) = Unit
    override suspend fun addSlideshowSchedule(day: String, isSpecificDate: Boolean, startTime: String, endTime: String, customId: String) = Unit
    override suspend fun addStandbySchedule(day: String, isSpecificDate: Boolean, startTime: String, endTime: String, customId: String) = Unit
    override suspend fun deleteSlideshowSchedule(id: String) = Unit
    override suspend fun deleteStandbySchedule(id: String) = Unit
    override fun getSlideshowSchedulesFlow(): Flow<List<SlideshowSchedule>> = slideshowSchedules.asStateFlow()
    override fun getStandbySchedulesFlow(): Flow<List<StandbySchedule>> = standbySchedules.asStateFlow()
    override suspend fun updateSlideshowScheduleDates(id: String, specificDates: String) = Unit
    override suspend fun updateSlideshowScheduleDays(id: String, activeDays: String) = Unit
    override suspend fun updateSlideshowScheduleTime(id: String, startTime: String, endTime: String) = Unit
    override suspend fun updateStandbyScheduleDates(id: String, specificDates: String) = Unit
    override suspend fun updateStandbyScheduleDays(id: String, activeDays: String) = Unit
    override suspend fun updateStandbyScheduleTime(id: String, startTime: String, endTime: String) = Unit
    override suspend fun addFridayMedia(filePath: String, isVideo: Boolean) = Unit
    override suspend fun deleteFridayMedia(id: String) = Unit
    override fun getFridayMedia(): List<FridayMedia> = fridayMedia.value
    override suspend fun updateFridayMediaItemPath(id: String, filePath: String) = Unit
    override suspend fun updateFridayMediaOrder(ids: List<String>) = Unit
    override fun getKemenagSchedulesFlow(cityId: String): Flow<List<KemenagSchedule>> = kemenagSchedules.asStateFlow()
    override fun triggerKemenagFetchForDuration(cityId: String, duration: String, targetDateStr: String) = Unit
    override suspend fun deleteDevice(id: String) = Unit
    override fun getConnectedDevicesFlow(): Flow<List<ConnectedDevice>> = devices.asStateFlow()
    override suspend fun setDeviceMaster(id: String) = Unit
    override suspend fun updatePinCode(newPin: String, changerDeviceId: String?) = Unit

    override suspend fun updateConfig(
        latitude: Double,
        longitude: Double,
        iqomahSubuh: Int,
        iqomahDzuhur: Int,
        iqomahAshar: Int,
        iqomahMaghrib: Int,
        iqomahIsya: Int,
        calculationMethodName: String,
        adzanLeadMinutes: Int,
        offsetImsak: Int,
        offsetSubuh: Int,
        offsetSyuruq: Int,
        offsetDzuhur: Int,
        offsetAshar: Int,
        offsetMaghrib: Int,
        offsetIsya: Int,
        kemenagCityId: String,
        kemenagCityName: String,
        kemenagDuration: String,
        kemenagTargetDate: String,
        triggerFetch: Boolean,
        adzanFooterText: String?,
        iqomahFooterText: String?,
        dashboardRandomize: Boolean?,
        dashboardTransitionType: String?,
        dashboardTransitionInterval: Int?
    ) {
        val current = config.value
        config.value = current.copy(
        latitude = latitude,
        longitude = longitude,
        iqomahSubuh = iqomahSubuh,
        iqomahDzuhur = iqomahDzuhur,
        iqomahAshar = iqomahAshar,
        iqomahMaghrib = iqomahMaghrib,
        iqomahIsya = iqomahIsya,
        calculationMethodName = calculationMethodName,
        adzanLeadMinutes = adzanLeadMinutes,
        offsetImsak = offsetImsak,
        offsetSubuh = offsetSubuh,
        offsetSyuruq = offsetSyuruq,
        offsetDzuhur = offsetDzuhur,
        offsetAshar = offsetAshar,
        offsetMaghrib = offsetMaghrib,
        offsetIsya = offsetIsya,
        kemenagCityId = kemenagCityId,
        kemenagCityName = kemenagCityName,
        kemenagDuration = kemenagDuration,
        kemenagTargetDate = kemenagTargetDate,
        adzanFooterText = adzanFooterText ?: current.adzanFooterText,
        iqomahFooterText = iqomahFooterText ?: current.iqomahFooterText,
        dashboardRandomize = dashboardRandomize ?: current.dashboardRandomize,
        dashboardTransitionType = dashboardTransitionType ?: current.dashboardTransitionType,
        dashboardTransitionInterval = dashboardTransitionInterval ?: current.dashboardTransitionInterval
        )
    }
}

/** The wire value the display and the Android app both use for a controller. */
private const val REMOTE_MODE = "REMOTE"
