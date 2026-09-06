package com.groovernapp.mosqueathan.data.room

import com.groovernapp.mosqueathan.data.KemenagFetcher
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
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

/**
 * [ConfigRepository] over Room, replacing the Realm implementation.
 *
 * Written beside `MosqueConfigRepository` rather than in place of it, and the Koin binding is what
 * chooses. Both satisfy the same interface, so switching is one line and reverting is the same
 * line — which is the only responsible shape for replacing the storage under a display that is
 * running in a mosque.
 *
 * ### Why there is a cache
 *
 * Seven members of the interface are not `suspend`: `getConfig()`, `getMediaList()`,
 * `getStandbySchedules()` and friends. Realm could serve those because its reads are synchronous;
 * Room's are not. So this holds the last known value of each table and answers from it.
 *
 * That is not a workaround, it is an improvement: `AppViewModel`'s coordinator calls `getConfig()`
 * once a second for the life of the display, and it now reads a field instead of opening a
 * database. The cache is primed with one blocking read at construction so the first tick sees real
 * prayer times rather than defaults, and kept current by collecting the DAO's flows.
 *
 * ### Why `lastModified` cannot be forgotten
 *
 * Every write goes through [editConfig] or [stamped], and both apply the timestamp *after* the
 * caller's change. There is no path where a caller supplies its own value or omits one, which
 * matters because the sync conflict dialog is only as honest as this column.
 */
class RoomConfigRepository(
    private val dao: MosqueDao,
    private val kemenag: KemenagFetcher = KemenagFetcher(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : ConfigRepository {

    private val configState = MutableStateFlow(MosqueConfig())
    private val announcementsState = MutableStateFlow<List<Announcement>>(emptyList())
    private val mediaState = MutableStateFlow<List<Media>>(emptyList())
    private val fridayMediaState = MutableStateFlow<List<FridayMedia>>(emptyList())
    private val backgroundsState = MutableStateFlow<List<DashboardBackground>>(emptyList())
    private val slideshowState = MutableStateFlow<List<SlideshowSchedule>>(emptyList())
    private val standbyState = MutableStateFlow<List<StandbySchedule>>(emptyList())
    private val devicesState = MutableStateFlow<List<ConnectedDevice>>(emptyList())

    init {
        // One blocking read, once. Without it the display's first coordinator tick would compute
        // prayer times from defaults and briefly show the wrong ones.
        runBlocking {
            dao.getConfig()?.let { configState.value = it.toDomain() }
            announcementsState.value = dao.getAnnouncements().map { it.toDomain() }
            mediaState.value = dao.getMedia().map { it.toDomain() }
            fridayMediaState.value = dao.getFridayMedia().map { it.toDomain() }
            backgroundsState.value = dao.getDashboardBackgrounds().map { it.toDomain() }
            slideshowState.value = dao.getSlideshowSchedules().map { it.toDomain() }
            standbyState.value = dao.getStandbySchedules().map { it.toDomain() }
            devicesState.value = dao.getDevices().map { it.toDomain() }
        }
        scope.launch { dao.observeConfig().collect { e -> e?.let { configState.value = it.toDomain() } } }
        scope.launch { dao.observeAnnouncements().collect { r -> announcementsState.value = r.map { it.toDomain() } } }
        scope.launch { dao.observeMedia().collect { r -> mediaState.value = r.map { it.toDomain() } } }
        scope.launch { dao.observeFridayMedia().collect { r -> fridayMediaState.value = r.map { it.toDomain() } } }
        scope.launch { dao.observeDashboardBackgrounds().collect { r -> backgroundsState.value = r.map { it.toDomain() } } }
        scope.launch { dao.observeSlideshowSchedules().collect { r -> slideshowState.value = r.map { it.toDomain() } } }
        scope.launch { dao.observeStandbySchedules().collect { r -> standbyState.value = r.map { it.toDomain() } } }
        scope.launch { dao.observeDevices().collect { r -> devicesState.value = r.map { it.toDomain() } } }
    }

    // --- reads -------------------------------------------------------------------------------

    override fun getConfigFlow(): Flow<MosqueConfig> = configState.asStateFlow()
    override fun getConfig(): MosqueConfig = configState.value
    override fun getMediaListFlow(): Flow<List<Media>> = mediaState.asStateFlow()
    override fun getMediaList(): List<Media> = mediaState.value
    override fun getDashboardBackgroundsFlow(): Flow<List<DashboardBackground>> = backgroundsState.asStateFlow()
    override fun getDashboardBackgrounds(): List<DashboardBackground> = backgroundsState.value
    override fun observeFridayMedia(): Flow<List<FridayMedia>> = fridayMediaState.asStateFlow()
    override fun getFridayMedia(): List<FridayMedia> = fridayMediaState.value
    override fun getActiveAnnouncementsFlow(): Flow<List<Announcement>> = announcementsState.asStateFlow()
    override fun getSlideshowSchedulesFlow(): Flow<List<SlideshowSchedule>> = slideshowState.asStateFlow()
    override fun getSlideshowSchedules(): List<SlideshowSchedule> = slideshowState.value
    override fun getStandbySchedulesFlow(): Flow<List<StandbySchedule>> = standbyState.asStateFlow()
    override fun getStandbySchedules(): List<StandbySchedule> = standbyState.value
    override fun getConnectedDevicesFlow(): Flow<List<ConnectedDevice>> = devicesState.asStateFlow()

    override fun getKemenagSchedulesFlow(cityId: String): Flow<List<KemenagSchedule>> =
        dao.observeKemenagSchedules(cityId).map { rows -> rows.map { it.toDomain() } }

    override fun getIqomahMinutes(prayerType: PrayerType): Int = with(getConfig()) {
        when (prayerType) {
            PrayerType.SUBUH -> iqomahSubuh
            PrayerType.DZUHUR -> iqomahDzuhur
            PrayerType.ASHAR -> iqomahAshar
            PrayerType.MAGHRIB -> iqomahMaghrib
            PrayerType.ISYA -> iqomahIsya
            // Informational prayers never drive an iqomah countdown; see PrayerType's note.
            PrayerType.IMSAK, PrayerType.SYURUQ -> 0
        }
    }

    // --- the one place a write is stamped ----------------------------------------------------

    /**
     * Applies [change] to the config row and stamps it.
     *
     * The stamp is applied to the *result*, so a caller cannot pass its own or leave it out. That
     * is the whole reason config writes funnel through here rather than each calling `upsertConfig`.
     */
    private suspend fun editConfig(change: (MosqueConfigEntity) -> MosqueConfigEntity) {
        val current = dao.getConfig() ?: MosqueConfigEntity()
        dao.upsertConfig(change(current).copy(lastModified = now()))
    }

    private fun <T> stamped(items: List<T>, stamp: (T, Long) -> T): List<T> {
        val at = now()
        return items.map { stamp(it, at) }
    }

    // --- config writes -----------------------------------------------------------------------

    override suspend fun resetToDefaults() {
        dao.clearAnnouncements()
        dao.getMedia().forEach { dao.deleteMedia(it.id) }
        dao.getFridayMedia().forEach { dao.deleteFridayMedia(it.id) }
        dao.getDashboardBackgrounds().forEach { dao.deleteDashboardBackground(it.id) }
        dao.getSlideshowSchedules().forEach { dao.deleteSlideshowSchedule(it.id) }
        dao.getStandbySchedules().forEach { dao.deleteStandbySchedule(it.id) }
        dao.clearAllKemenagSchedules()
        // A fresh row rather than a field-by-field reset, so a config field added later cannot be
        // left behind holding an old value.
        dao.upsertConfig(MosqueConfigEntity(lastModified = now()))
    }

    override suspend fun updateConfig(config: MosqueConfig) = editConfig { current ->
        // The eight fields this deliberately does not copy belong to dedicated setters; see the
        // note in EntityMappers about why a total write would clobber a theme nobody touched.
        current.copy(
            latitude = config.latitude, longitude = config.longitude,
            iqomahSubuh = config.iqomahSubuh, iqomahDzuhur = config.iqomahDzuhur,
            iqomahAshar = config.iqomahAshar, iqomahMaghrib = config.iqomahMaghrib,
            iqomahIsya = config.iqomahIsya,
            calculationMethodName = config.calculationMethodName,
            adzanLeadMinutes = config.adzanLeadMinutes,
            offsetImsak = config.offsetImsak, offsetSubuh = config.offsetSubuh,
            offsetSyuruq = config.offsetSyuruq, offsetDzuhur = config.offsetDzuhur,
            offsetAshar = config.offsetAshar, offsetMaghrib = config.offsetMaghrib,
            offsetIsya = config.offsetIsya,
            kemenagCityId = config.kemenagCityId, kemenagCityName = config.kemenagCityName,
            kemenagDuration = config.kemenagDuration, kemenagTargetDate = config.kemenagTargetDate,
            adzanFooterText = config.adzanFooterText, iqomahFooterText = config.iqomahFooterText,
            standbyModeEnabled = config.standbyModeEnabled,
            standbyStartTime = config.standbyStartTime, standbyEndTime = config.standbyEndTime,
            dashboardRandomize = config.dashboardRandomize,
            dashboardTransitionType = config.dashboardTransitionType,
            dashboardTransitionInterval = config.dashboardTransitionInterval,
            slideshowEnabled = config.slideshowEnabled,
            jumatEnabled = config.jumatEnabled,
            jumatStartTime = config.jumatStartTime, jumatEndTime = config.jumatEndTime,
            offsetJumat = config.offsetJumat, jumatStandbyOnly = config.jumatStandbyOnly,
            jumatWarningText = config.jumatWarningText,
            murottalEnabled = config.murottalEnabled,
            murottalStartDelay = config.murottalStartDelay,
            murottalAudioPaths = config.murottalAudioPaths,
            jumatSlideshowEnabled = config.jumatSlideshowEnabled,
            includeGlobalSlideshow = config.includeGlobalSlideshow,
            selectedGlobalSlideIds = config.selectedGlobalSlideIds,
            jumatTransitionType = config.jumatTransitionType,
            jumatSlideIntervalSeconds = config.jumatSlideIntervalSeconds,
            jumatSlideIntervalUnit = config.jumatSlideIntervalUnit,
            jumatVideoWaitUntilEnd = config.jumatVideoWaitUntilEnd
        )
    }

    @Suppress("LongParameterList")
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
        editConfig { current ->
            current.copy(
                latitude = latitude, longitude = longitude,
                iqomahSubuh = iqomahSubuh, iqomahDzuhur = iqomahDzuhur,
                iqomahAshar = iqomahAshar, iqomahMaghrib = iqomahMaghrib,
                iqomahIsya = iqomahIsya,
                calculationMethodName = calculationMethodName,
                adzanLeadMinutes = adzanLeadMinutes,
                offsetImsak = offsetImsak, offsetSubuh = offsetSubuh,
                offsetSyuruq = offsetSyuruq, offsetDzuhur = offsetDzuhur,
                offsetAshar = offsetAshar, offsetMaghrib = offsetMaghrib,
                offsetIsya = offsetIsya,
                kemenagCityId = kemenagCityId, kemenagCityName = kemenagCityName,
                kemenagDuration = kemenagDuration, kemenagTargetDate = kemenagTargetDate,
                // Null means "leave it", which is what the nullable parameters are for.
                adzanFooterText = adzanFooterText ?: current.adzanFooterText,
                iqomahFooterText = iqomahFooterText ?: current.iqomahFooterText,
                dashboardRandomize = dashboardRandomize ?: current.dashboardRandomize,
                dashboardTransitionType = dashboardTransitionType ?: current.dashboardTransitionType,
                dashboardTransitionInterval = dashboardTransitionInterval
                    ?: current.dashboardTransitionInterval
            )
        }
        if (triggerFetch) triggerKemenagFetchForDuration(kemenagCityId, kemenagDuration, kemenagTargetDate)
    }

    override suspend fun updateConnectivityMode(mode: String) =
        editConfig { it.copy(connectivityMode = mode) }

    override suspend fun updateRemoteTvIp(ip: String) = editConfig { it.copy(remoteTvIp = ip) }

    override suspend fun updateShowSettingsOnTv(show: Boolean) =
        editConfig { it.copy(showSettingsOnTv = show) }

    override suspend fun updateStandbyEnabled(enabled: Boolean) =
        editConfig { it.copy(standbyModeEnabled = enabled) }

    override suspend fun updateStandbySettings(enabled: Boolean, startTime: String, endTime: String) =
        editConfig {
            it.copy(standbyModeEnabled = enabled, standbyStartTime = startTime, standbyEndTime = endTime)
        }

    override suspend fun updateThemeSettings(
        transitionType: String,
        intervalSeconds: Int,
        videoWaitUntilEnd: Boolean,
        includeDefaultImage: Boolean
    ) = editConfig {
        it.copy(
            transitionType = transitionType,
            slideIntervalSeconds = intervalSeconds,
            videoWaitUntilEnd = videoWaitUntilEnd,
            includeDefaultImage = includeDefaultImage
        )
    }

    override suspend fun updateSlideshowEnabled(enabled: Boolean) =
        editConfig { it.copy(slideshowEnabled = enabled) }

    override suspend fun updateCustomBackground(path: String) {
        val previous = dao.getConfig()?.customBackgroundPath
        editConfig { it.copy(customBackgroundPath = path) }
        if (!previous.isNullOrEmpty() && previous != path) deleteFileIfNoLongerReferenced(previous)
    }

    override suspend fun updatePinCode(newPin: String, changerDeviceId: String?) {
        editConfig { it.copy(pinCode = newPin) }
        // Setting a PIN makes every other device unapproved; the one that set it stays in.
        val devices = dao.getDevices().map { device ->
            device.copy(
                isAllowed = newPin.isEmpty() || device.deviceId == changerDeviceId,
                lastModified = now()
            )
        }
        dao.upsertDevices(devices)
    }

    // --- announcements -----------------------------------------------------------------------

    override suspend fun addAnnouncement(content: String, customId: String) {
        val position = (dao.getAnnouncements().maxOfOrNull { it.position } ?: -1) + 1
        dao.upsertAnnouncements(
            listOf(
                AnnouncementEntity(
                    id = customId.ifEmpty { UUID.randomUUID().toString() },
                    content = content,
                    isActive = true,
                    position = position,
                    lastModified = now()
                )
            )
        )
    }

    override suspend fun clearAnnouncements() = dao.clearAnnouncements()

    override suspend fun deleteAnnouncement(id: String) = dao.deleteAnnouncement(id)

    override suspend fun updateAnnouncement(id: String, content: String, isActive: Boolean) {
        val existing = dao.getAnnouncements().firstOrNull { it.id == id } ?: return
        dao.upsertAnnouncements(
            listOf(existing.copy(content = content, isActive = isActive, lastModified = now()))
        )
    }

    override suspend fun updateAnnouncementsOrder(ids: List<String>) {
        val byId = dao.getAnnouncements().associateBy { it.id }
        dao.upsertAnnouncements(
            stamped(ids.mapIndexedNotNull { index, id -> byId[id]?.copy(position = index) }) { a, at ->
                a.copy(lastModified = at)
            }
        )
    }

    // --- slideshow media ---------------------------------------------------------------------

    override suspend fun addMediaItem(filePath: String, isVideo: Boolean) {
        val sortOrder = (dao.getMedia().maxOfOrNull { it.sortOrder } ?: -1) + 1
        dao.upsertMedia(
            listOf(
                MediaEntity(
                    id = UUID.randomUUID().toString(), filePath = filePath, isVideo = isVideo,
                    isUsed = true, sortOrder = sortOrder, lastModified = now()
                )
            )
        )
    }

    override suspend fun deleteMediaItem(id: String) {
        val path = dao.getMedia().firstOrNull { it.id == id }?.filePath
        dao.deleteMedia(id)
        path?.let { deleteFileIfNoLongerReferenced(it) }
    }

    override suspend fun updateMediaItemPath(id: String, filePath: String) {
        val existing = dao.getMedia().firstOrNull { it.id == id } ?: return
        dao.upsertMedia(listOf(existing.copy(filePath = filePath, lastModified = now())))
    }

    override suspend fun updateMediaItemUsed(id: String, isUsed: Boolean) {
        val existing = dao.getMedia().firstOrNull { it.id == id } ?: return
        dao.upsertMedia(listOf(existing.copy(isUsed = isUsed, lastModified = now())))
    }

    override suspend fun updateMediaListOrder(ids: List<String>) {
        val byId = dao.getMedia().associateBy { it.id }
        dao.upsertMedia(
            stamped(ids.mapIndexedNotNull { index, id -> byId[id]?.copy(sortOrder = index) }) { m, at ->
                m.copy(lastModified = at)
            }
        )
    }

    // --- Friday media ------------------------------------------------------------------------

    override suspend fun addFridayMedia(filePath: String, isVideo: Boolean) {
        val sortOrder = (dao.getFridayMedia().maxOfOrNull { it.sortOrder } ?: -1) + 1
        dao.upsertFridayMedia(
            listOf(
                FridayMediaEntity(
                    id = UUID.randomUUID().toString(), filePath = filePath,
                    isVideo = isVideo, sortOrder = sortOrder, lastModified = now()
                )
            )
        )
    }

    override suspend fun deleteFridayMedia(id: String) {
        val path = dao.getFridayMedia().firstOrNull { it.id == id }?.filePath
        dao.deleteFridayMedia(id)
        path?.let { deleteFileIfNoLongerReferenced(it) }
    }

    override suspend fun updateFridayMediaItemPath(id: String, filePath: String) {
        val existing = dao.getFridayMedia().firstOrNull { it.id == id } ?: return
        dao.upsertFridayMedia(listOf(existing.copy(filePath = filePath, lastModified = now())))
    }

    override suspend fun updateFridayMediaOrder(ids: List<String>) {
        val byId = dao.getFridayMedia().associateBy { it.id }
        dao.upsertFridayMedia(
            stamped(ids.mapIndexedNotNull { index, id -> byId[id]?.copy(sortOrder = index) }) { m, at ->
                m.copy(lastModified = at)
            }
        )
    }

    // --- dashboard backgrounds ---------------------------------------------------------------

    override suspend fun addDashboardBackground(filePath: String, customId: String) {
        val sortOrder = (dao.getDashboardBackgrounds().maxOfOrNull { it.sortOrder } ?: -1) + 1
        dao.upsertDashboardBackgrounds(
            listOf(
                DashboardBackgroundEntity(
                    id = customId.ifEmpty { UUID.randomUUID().toString() },
                    filePath = filePath, isUsed = true, sortOrder = sortOrder, lastModified = now()
                )
            )
        )
    }

    override suspend fun deleteDashboardBackground(id: String) {
        val path = dao.getDashboardBackgrounds().firstOrNull { it.id == id }?.filePath
        dao.deleteDashboardBackground(id)
        path?.let { deleteFileIfNoLongerReferenced(it) }
    }

    override suspend fun updateDashboardBackgroundUsed(id: String, isUsed: Boolean) {
        val existing = dao.getDashboardBackgrounds().firstOrNull { it.id == id } ?: return
        dao.upsertDashboardBackgrounds(listOf(existing.copy(isUsed = isUsed, lastModified = now())))
    }

    override suspend fun updateDashboardBackgroundOrder(ids: List<String>) {
        val byId = dao.getDashboardBackgrounds().associateBy { it.id }
        dao.upsertDashboardBackgrounds(
            stamped(ids.mapIndexedNotNull { index, id -> byId[id]?.copy(sortOrder = index) }) { b, at ->
                b.copy(lastModified = at)
            }
        )
    }

    // --- schedules ---------------------------------------------------------------------------

    override suspend fun addSlideshowSchedule(
        day: String,
        isSpecificDate: Boolean,
        startTime: String,
        endTime: String,
        customId: String
    ) {
        val at = now()
        dao.upsertSlideshowSchedules(
            listOf(
                SlideshowScheduleEntity(
                    id = customId.ifEmpty { UUID.randomUUID().toString() },
                    startTime = startTime, endTime = endTime,
                    activeDays = day, specificDates = if (isSpecificDate) day else "",
                    createdAt = at, day = day, isSpecificDate = isSpecificDate,
                    lastModified = at
                )
            )
        )
    }

    override suspend fun deleteSlideshowSchedule(id: String) = dao.deleteSlideshowSchedule(id)

    override suspend fun updateSlideshowScheduleTime(id: String, startTime: String, endTime: String) =
        editSlideshow(id) { it.copy(startTime = startTime, endTime = endTime) }

    override suspend fun updateSlideshowScheduleDays(id: String, activeDays: String) =
        editSlideshow(id) { it.copy(activeDays = activeDays) }

    override suspend fun updateSlideshowScheduleDates(id: String, specificDates: String) =
        editSlideshow(id) { it.copy(specificDates = specificDates) }

    override suspend fun addStandbySchedule(
        day: String,
        isSpecificDate: Boolean,
        startTime: String,
        endTime: String,
        customId: String
    ) {
        val at = now()
        dao.upsertStandbySchedules(
            listOf(
                StandbyScheduleEntity(
                    id = customId.ifEmpty { UUID.randomUUID().toString() },
                    startTime = startTime, endTime = endTime,
                    activeDays = day, specificDates = if (isSpecificDate) day else "",
                    createdAt = at, day = day, isSpecificDate = isSpecificDate,
                    lastModified = at
                )
            )
        )
    }

    override suspend fun deleteStandbySchedule(id: String) = dao.deleteStandbySchedule(id)

    override suspend fun updateStandbyScheduleTime(id: String, startTime: String, endTime: String) =
        editStandby(id) { it.copy(startTime = startTime, endTime = endTime) }

    override suspend fun updateStandbyScheduleDays(id: String, activeDays: String) =
        editStandby(id) { it.copy(activeDays = activeDays) }

    override suspend fun updateStandbyScheduleDates(id: String, specificDates: String) =
        editStandby(id) { it.copy(specificDates = specificDates) }

    private suspend fun editSlideshow(id: String, change: (SlideshowScheduleEntity) -> SlideshowScheduleEntity) {
        val existing = dao.getSlideshowSchedules().firstOrNull { it.id == id } ?: return
        dao.upsertSlideshowSchedules(listOf(change(existing).copy(lastModified = now())))
    }

    private suspend fun editStandby(id: String, change: (StandbyScheduleEntity) -> StandbyScheduleEntity) {
        val existing = dao.getStandbySchedules().firstOrNull { it.id == id } ?: return
        dao.upsertStandbySchedules(listOf(change(existing).copy(lastModified = now())))
    }

    // --- devices -----------------------------------------------------------------------------

    override suspend fun deleteDevice(id: String) = dao.deleteDevice(id)

    override suspend fun setDeviceMaster(id: String) {
        val at = now()
        dao.upsertDevices(
            dao.getDevices().map { it.copy(isMaster = it.deviceId == id, lastModified = at) }
        )
    }

    // --- Kemenag -----------------------------------------------------------------------------

    /**
     * Not `suspend`, because the interface is not — it starts the work and returns.
     *
     * The month walk is [KemenagFetcher.monthsFor], which is pure and tested; this only stores what
     * comes back. Each month is written as it arrives rather than after all of them, so a schedule
     * half-fetched over a poor connection still leaves the display with the months it did get.
     */
    override fun triggerKemenagFetchForDuration(
        cityId: String,
        duration: String,
        targetDateStr: String
    ) {
        if (cityId.isEmpty()) return
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        scope.launch {
            KemenagFetcher.monthsFor(duration, targetDateStr, today).forEach { (year, month) ->
                val days = kemenag.fetchMonth(cityId, year, month)
                if (days.isNotEmpty()) {
                    val at = now()
                    dao.upsertKemenagSchedules(days.map { it.toEntity(at) })
                }
            }
        }
    }

    /**
     * Deletes a media file once no row names it any more.
     *
     * The same rule the Realm repository grew after media deleted from the display's own UI was
     * found to accumulate in private storage: the file follows the row, in the repository, so every
     * caller inherits it rather than four panes each remembering.
     */
    private suspend fun deleteFileIfNoLongerReferenced(path: String) {
        if (path.isEmpty()) return
        val stillReferenced = dao.getMedia().any { it.filePath == path } ||
            dao.getFridayMedia().any { it.filePath == path } ||
            dao.getDashboardBackgrounds().any { it.filePath == path } ||
            dao.getConfig()?.customBackgroundPath == path
        if (!stillReferenced) {
            runCatching { java.io.File(path).takeIf { it.exists() }?.delete() }
        }
    }
}
