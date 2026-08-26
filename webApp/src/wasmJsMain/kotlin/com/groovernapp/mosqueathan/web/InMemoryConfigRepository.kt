package com.groovernapp.mosqueathan.web

import com.groovernapp.mosqueathan.domain.Announcement
import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.domain.ConnectedDevice
import com.groovernapp.mosqueathan.domain.DashboardBackground
import com.groovernapp.mosqueathan.domain.FridayMedia
import com.groovernapp.mosqueathan.domain.KemenagSchedule
import com.groovernapp.mosqueathan.domain.Media
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.domain.SlideshowSchedule
import com.groovernapp.mosqueathan.domain.StandbySchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A [ConfigRepository] held entirely in memory.
 *
 * This is scaffolding, and saying so is the point: it exists so the settings UI can be
 * *seen* in a browser before any transport is written. Reads come from `MutableStateFlow`s
 * and writes update them, so the screen behaves — a toggle stays toggled — while nothing
 * is persisted or sent anywhere.
 *
 * It is replaced by an implementation backed by the display's own HTTP API
 * (`GET /api/get_config`, `POST /api/action`), which is the actual step 7 work. Only after
 * that does IndexedDB matter, and only for the offline replica: a *connected* controller
 * is a client and needs no local store at all.
 */
class InMemoryConfigRepository : ConfigRepository {

    private val config = MutableStateFlow(MosqueConfig())
    private val media = MutableStateFlow<List<Media>>(emptyList())
    private val backgrounds = MutableStateFlow<List<DashboardBackground>>(emptyList())
    private val fridayMedia = MutableStateFlow<List<FridayMedia>>(emptyList())
    private val announcements = MutableStateFlow<List<Announcement>>(emptyList())
    private val devices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    private val slideshowSchedules = MutableStateFlow<List<SlideshowSchedule>>(emptyList())
    private val standbySchedules = MutableStateFlow<List<StandbySchedule>>(emptyList())
    private val kemenagSchedules = MutableStateFlow<List<KemenagSchedule>>(emptyList())

    override fun getConfigFlow(): Flow<MosqueConfig> = config.asStateFlow()
    override fun getConfig(): MosqueConfig = config.value
    override fun getMediaListFlow(): Flow<List<Media>> = media.asStateFlow()
    override fun getDashboardBackgroundsFlow(): Flow<List<DashboardBackground>> = backgrounds.asStateFlow()
    override fun observeFridayMedia(): Flow<List<FridayMedia>> = fridayMedia.asStateFlow()
    override fun getActiveAnnouncementsFlow(): Flow<List<Announcement>> = announcements.asStateFlow()
    override fun getStandbySchedules(): List<StandbySchedule> = standbySchedules.value
    override fun getSlideshowSchedules(): List<SlideshowSchedule> = slideshowSchedules.value
    override fun getIqomahMinutes(prayerType: PrayerType): Int = 0
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
