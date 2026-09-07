package com.groovernapp.mosqueathan.data.room

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

/**
 * [MosqueDao] in memory, so [RoomConfigRepository] can be tested without a database.
 *
 * ### Why a fake rather than an in-memory Room
 *
 * Room cannot run as a JVM unit test in this project: there is no Robolectric and no bundled
 * SQLite driver, so `android.database.sqlite` is the Android plugin's throwing stub. Adding either
 * to test a repository would be a large dependency for a small question — and the question here is
 * not whether SQLite works. It is whether the repository stamps every write, primes its cache, and
 * withholds the fields that belong to dedicated setters. None of that is SQL.
 *
 * What this deliberately *does* reproduce, because the repository depends on it:
 *
 * - **Row ordering.** Every `get`/`observe` in the DAO carries an `ORDER BY`, and the repository
 *   hands those lists straight to the display. A fake that returned insertion order would let a
 *   reordering test pass while the display showed the old order.
 * - **Upsert semantics.** `@Upsert` replaces a row with a matching primary key and inserts
 *   otherwise, one row at a time; a fake that appended would make every edit look like an
 *   addition.
 * - **`observeConfig()` emitting null**, which is what an empty table gives, and the case
 *   `RoomConfigRepository`'s collector guards against.
 *
 * [migrateAll] is not overridden on purpose: it is a non-abstract interface method with a body, so
 * this inherits the real ordering — the part that was a data-loss bug — rather than a copy of it.
 *
 * @param observable when false, every `observe*` returns an empty flow. That is how the priming
 *   test proves the synchronous reads are served by the blocking read in `init` and not by a flow
 *   emission that happened to arrive first.
 */
class FakeMosqueDao(private val observable: Boolean = true) : MosqueDao {

    private val config = MutableStateFlow<MosqueConfigEntity?>(null)
    private val announcements = MutableStateFlow<List<AnnouncementEntity>>(emptyList())
    private val media = MutableStateFlow<List<MediaEntity>>(emptyList())
    private val fridayMedia = MutableStateFlow<List<FridayMediaEntity>>(emptyList())
    private val backgrounds = MutableStateFlow<List<DashboardBackgroundEntity>>(emptyList())
    private val slideshow = MutableStateFlow<List<SlideshowScheduleEntity>>(emptyList())
    private val standby = MutableStateFlow<List<StandbyScheduleEntity>>(emptyList())
    private val devices = MutableStateFlow<List<ConnectedDeviceEntity>>(emptyList())
    private val kemenag = MutableStateFlow<List<KemenagScheduleEntity>>(emptyList())

    /**
     * How many times a `suspend` read reached the DAO.
     *
     * The repository's seven synchronous reads are supposed to answer from its cache — the
     * coordinator calls `getConfig()` once a second for the life of the display — and a cache that
     * quietly went to the database on every call would be indistinguishable by value alone.
     */
    var suspendReads = 0
        private set

    // --- seeding -------------------------------------------------------------------------------

    /**
     * Puts rows in place without going through a write.
     *
     * Separate from the upserts on purpose: a test that seeded through `upsert*` could not tell a
     * stamp the repository applied from one the seed carried, which is the whole subject of
     * [RoomWriteStampTest].
     */
    @Suppress("LongParameterList")
    fun seed(
        config: MosqueConfigEntity? = null,
        announcements: List<AnnouncementEntity> = emptyList(),
        media: List<MediaEntity> = emptyList(),
        fridayMedia: List<FridayMediaEntity> = emptyList(),
        backgrounds: List<DashboardBackgroundEntity> = emptyList(),
        slideshow: List<SlideshowScheduleEntity> = emptyList(),
        standby: List<StandbyScheduleEntity> = emptyList(),
        devices: List<ConnectedDeviceEntity> = emptyList(),
        kemenag: List<KemenagScheduleEntity> = emptyList()
    ) {
        this.config.value = config
        this.announcements.value = announcements.sortedBy { it.position }
        this.media.value = media.sortedBy { it.sortOrder }
        this.fridayMedia.value = fridayMedia.sortedBy { it.sortOrder }
        this.backgrounds.value = backgrounds.sortedBy { it.sortOrder }
        this.slideshow.value = slideshow.sortedBy { it.createdAt }
        this.standby.value = standby.sortedBy { it.createdAt }
        this.devices.value = devices.sortedByDescending { it.lastSeen }
        this.kemenag.value = kemenag.sortedBy { it.dateString }
    }

    // --- what the tests read back --------------------------------------------------------------

    val configRow: MosqueConfigEntity? get() = config.value
    val announcementRows: List<AnnouncementEntity> get() = announcements.value
    val mediaRows: List<MediaEntity> get() = media.value
    val fridayMediaRows: List<FridayMediaEntity> get() = fridayMedia.value
    val backgroundRows: List<DashboardBackgroundEntity> get() = backgrounds.value
    val slideshowRows: List<SlideshowScheduleEntity> get() = slideshow.value
    val standbyRows: List<StandbyScheduleEntity> get() = standby.value
    val deviceRows: List<ConnectedDeviceEntity> get() = devices.value
    val kemenagRows: List<KemenagScheduleEntity> get() = kemenag.value

    /**
     * Every `lastModified` on disk, whichever table it is in.
     *
     * Tables rather than rows, because a write only has to stamp the row it touched and a test
     * should not have to know which table that was to notice that nothing was stamped at all.
     */
    fun everyStamp(): List<Long> =
        listOfNotNull(configRow?.lastModified) +
            announcementRows.map { it.lastModified } +
            mediaRows.map { it.lastModified } +
            fridayMediaRows.map { it.lastModified } +
            backgroundRows.map { it.lastModified } +
            slideshowRows.map { it.lastModified } +
            standbyRows.map { it.lastModified } +
            deviceRows.map { it.lastModified } +
            kemenagRows.map { it.lastModified }

    /** Rows in every table, for the reset test. */
    fun rowCounts(): Map<String, Int> = mapOf(
        "mosque_config" to if (configRow == null) 0 else 1,
        "announcements" to announcementRows.size,
        "media" to mediaRows.size,
        "friday_media" to fridayMediaRows.size,
        "dashboard_backgrounds" to backgroundRows.size,
        "slideshow_schedules" to slideshowRows.size,
        "standby_schedules" to standbyRows.size,
        "connected_devices" to deviceRows.size,
        "kemenag_schedules" to kemenagRows.size
    )

    // --- config --------------------------------------------------------------------------------

    override fun observeConfig(): Flow<MosqueConfigEntity?> =
        if (observable) config.asStateFlow() else emptyFlow()

    override suspend fun getConfig(): MosqueConfigEntity? {
        suspendReads++
        return config.value
    }

    override suspend fun upsertConfig(config: MosqueConfigEntity) {
        this.config.value = config
    }

    // --- announcements -------------------------------------------------------------------------

    override fun observeAnnouncements(): Flow<List<AnnouncementEntity>> =
        if (observable) announcements.asStateFlow() else emptyFlow()

    /** Filtered like the real query, which is the point of it being separate. */
    override fun observeActiveAnnouncements(): Flow<List<AnnouncementEntity>> =
        if (observable) announcements.map { rows -> rows.filter { it.isActive } } else emptyFlow()

    override suspend fun getAnnouncements(): List<AnnouncementEntity> {
        suspendReads++
        return announcements.value
    }

    override suspend fun upsertAnnouncements(items: List<AnnouncementEntity>) {
        announcements.value = announcements.value.upsertBy(items) { it.id }.sortedBy { it.position }
    }

    override suspend fun deleteAnnouncement(id: String) {
        announcements.value = announcements.value.filterNot { it.id == id }
    }

    override suspend fun clearAnnouncements() {
        announcements.value = emptyList()
    }

    override suspend fun clearActiveAnnouncements() {
        announcements.value = announcements.value.filterNot { it.isActive }
    }

    // --- slideshow media -----------------------------------------------------------------------

    override fun observeMedia(): Flow<List<MediaEntity>> =
        if (observable) media.asStateFlow() else emptyFlow()

    override suspend fun getMedia(): List<MediaEntity> {
        suspendReads++
        return media.value
    }

    override suspend fun upsertMedia(items: List<MediaEntity>) {
        media.value = media.value.upsertBy(items) { it.id }.sortedBy { it.sortOrder }
    }

    override suspend fun deleteMedia(id: String) {
        media.value = media.value.filterNot { it.id == id }
    }

    // --- Friday media --------------------------------------------------------------------------

    override fun observeFridayMedia(): Flow<List<FridayMediaEntity>> =
        if (observable) fridayMedia.asStateFlow() else emptyFlow()

    override suspend fun getFridayMedia(): List<FridayMediaEntity> {
        suspendReads++
        return fridayMedia.value
    }

    override suspend fun upsertFridayMedia(items: List<FridayMediaEntity>) {
        fridayMedia.value = fridayMedia.value.upsertBy(items) { it.id }.sortedBy { it.sortOrder }
    }

    override suspend fun deleteFridayMedia(id: String) {
        fridayMedia.value = fridayMedia.value.filterNot { it.id == id }
    }

    // --- dashboard backgrounds -----------------------------------------------------------------

    override fun observeDashboardBackgrounds(): Flow<List<DashboardBackgroundEntity>> =
        if (observable) backgrounds.asStateFlow() else emptyFlow()

    override suspend fun getDashboardBackgrounds(): List<DashboardBackgroundEntity> {
        suspendReads++
        return backgrounds.value
    }

    override suspend fun upsertDashboardBackgrounds(items: List<DashboardBackgroundEntity>) {
        backgrounds.value = backgrounds.value.upsertBy(items) { it.id }.sortedBy { it.sortOrder }
    }

    override suspend fun deleteDashboardBackground(id: String) {
        backgrounds.value = backgrounds.value.filterNot { it.id == id }
    }

    // --- slideshow schedules -------------------------------------------------------------------

    override fun observeSlideshowSchedules(): Flow<List<SlideshowScheduleEntity>> =
        if (observable) slideshow.asStateFlow() else emptyFlow()

    override suspend fun getSlideshowSchedules(): List<SlideshowScheduleEntity> {
        suspendReads++
        return slideshow.value
    }

    override suspend fun upsertSlideshowSchedules(items: List<SlideshowScheduleEntity>) {
        slideshow.value = slideshow.value.upsertBy(items) { it.id }.sortedBy { it.createdAt }
    }

    override suspend fun deleteSlideshowSchedule(id: String) {
        slideshow.value = slideshow.value.filterNot { it.id == id }
    }

    // --- standby schedules ---------------------------------------------------------------------

    override fun observeStandbySchedules(): Flow<List<StandbyScheduleEntity>> =
        if (observable) standby.asStateFlow() else emptyFlow()

    override suspend fun getStandbySchedules(): List<StandbyScheduleEntity> {
        suspendReads++
        return standby.value
    }

    override suspend fun upsertStandbySchedules(items: List<StandbyScheduleEntity>) {
        standby.value = standby.value.upsertBy(items) { it.id }.sortedBy { it.createdAt }
    }

    override suspend fun deleteStandbySchedule(id: String) {
        standby.value = standby.value.filterNot { it.id == id }
    }

    // --- connected devices ---------------------------------------------------------------------

    override fun observeDevices(): Flow<List<ConnectedDeviceEntity>> =
        if (observable) devices.asStateFlow() else emptyFlow()

    override suspend fun getDevices(): List<ConnectedDeviceEntity> {
        suspendReads++
        return devices.value
    }

    override suspend fun getDevice(deviceId: String): ConnectedDeviceEntity? {
        suspendReads++
        return devices.value.firstOrNull { it.deviceId == deviceId }
    }

    override suspend fun upsertDevices(items: List<ConnectedDeviceEntity>) {
        devices.value = devices.value.upsertBy(items) { it.deviceId }
            .sortedByDescending { it.lastSeen }
    }

    override suspend fun deleteDevice(deviceId: String) {
        devices.value = devices.value.filterNot { it.deviceId == deviceId }
    }

    // --- Kemenag schedules ---------------------------------------------------------------------

    override fun observeKemenagSchedules(cityId: String): Flow<List<KemenagScheduleEntity>> =
        if (observable) {
            kemenag.map { rows -> rows.filter { it.cityId == cityId } }
        } else {
            emptyFlow()
        }

    override suspend fun getKemenagSchedule(cityId: String, date: String): KemenagScheduleEntity? {
        suspendReads++
        return kemenag.value.firstOrNull { it.cityId == cityId && it.dateString == date }
    }

    override suspend fun getKemenagSchedules(cityId: String): List<KemenagScheduleEntity> {
        suspendReads++
        return kemenag.value.filter { it.cityId == cityId }.sortedBy { it.dateString }
    }

    override suspend fun upsertKemenagSchedules(items: List<KemenagScheduleEntity>) {
        // The composite key is the pair, which is the change this schema made; keying the fake on
        // dateString alone would reproduce the bug it was corrected for.
        kemenag.value = kemenag.value
            .upsertBy(items) { it.dateString to it.cityId }
            .sortedBy { it.dateString }
    }

    override suspend fun clearKemenagSchedules(cityId: String) {
        kemenag.value = kemenag.value.filterNot { it.cityId == cityId }
    }

    override suspend fun clearAllKemenagSchedules() {
        kemenag.value = emptyList()
    }

    /**
     * Reads the stamps the repository wrote, exactly as the real table does.
     *
     * It used to compute `MAX(lastModified)` over the rows, which is what the production query did
     * — and that could not see a deletion. Keeping the fake honest about that is the point: a fake
     * that still computed a maximum would pass a test the real database would fail.
     */
    override suspend fun tableTimestamps(): List<TableStampEntity> = tableStamps.values.toList()

    override suspend fun stampTable(stamp: TableStampEntity) {
        tableStamps[stamp.tableName] = stamp
    }

    private val tableStamps = linkedMapOf<String, TableStampEntity>()

    private fun stampsIn(table: String): List<Long> = when (table) {
        "mosque_config" -> listOfNotNull(configRow?.lastModified)
        "announcements" -> announcementRows.map { it.lastModified }
        "media" -> mediaRows.map { it.lastModified }
        "friday_media" -> fridayMediaRows.map { it.lastModified }
        "dashboard_backgrounds" -> backgroundRows.map { it.lastModified }
        "slideshow_schedules" -> slideshowRows.map { it.lastModified }
        "standby_schedules" -> standbyRows.map { it.lastModified }
        "connected_devices" -> deviceRows.map { it.lastModified }
        "kemenag_schedules" -> kemenagRows.map { it.lastModified }
        else -> emptyList()
    }
}

/** `@Upsert`: replace the row with this key, or add it. One at a time, and order-preserving. */
private fun <T, K> List<T>.upsertBy(items: List<T>, key: (T) -> K): List<T> {
    var result = this
    items.forEach { incoming ->
        val at = result.indexOfFirst { key(it) == key(incoming) }
        result = if (at >= 0) {
            result.toMutableList().also { it[at] = incoming }
        } else {
            result + incoming
        }
    }
    return result
}
