package com.groovernapp.mosqueathan.data.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * One DAO rather than nine, because the repository above it is one object and every screen reaches
 * it through that.
 *
 * Reads are `Flow` so the existing `getConfigFlow()` fan-out keeps working unchanged — that is what
 * pushes a settings change to every screen and every web client today, and it is the part of the
 * Realm arrangement most worth not disturbing.
 *
 * `Upsert` rather than `Insert(REPLACE)`: replace deletes the row and inserts a new one, which
 * fires a delete on every observing flow and would make a single edit look like a removal followed
 * by an addition to anything watching.
 */
@Dao
interface MosqueDao {

    // --- config: exactly one row, id "config" ---

    @Query("SELECT * FROM mosque_config WHERE id = 'config' LIMIT 1")
    fun observeConfig(): Flow<MosqueConfigEntity?>

    @Query("SELECT * FROM mosque_config WHERE id = 'config' LIMIT 1")
    suspend fun getConfig(): MosqueConfigEntity?

    @Upsert
    suspend fun upsertConfig(config: MosqueConfigEntity)

    // --- announcements ---

    @Query("SELECT * FROM announcements ORDER BY position ASC")
    fun observeAnnouncements(): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements ORDER BY position ASC")
    suspend fun getAnnouncements(): List<AnnouncementEntity>

    @Upsert
    suspend fun upsertAnnouncements(items: List<AnnouncementEntity>)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: String)

    @Query("DELETE FROM announcements")
    suspend fun clearAnnouncements()

    // --- slideshow media ---

    @Query("SELECT * FROM media ORDER BY sortOrder ASC")
    fun observeMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media ORDER BY sortOrder ASC")
    suspend fun getMedia(): List<MediaEntity>

    @Upsert
    suspend fun upsertMedia(items: List<MediaEntity>)

    @Query("DELETE FROM media WHERE id = :id")
    suspend fun deleteMedia(id: String)

    // --- Friday media ---

    @Query("SELECT * FROM friday_media ORDER BY sortOrder ASC")
    fun observeFridayMedia(): Flow<List<FridayMediaEntity>>

    @Query("SELECT * FROM friday_media ORDER BY sortOrder ASC")
    suspend fun getFridayMedia(): List<FridayMediaEntity>

    @Upsert
    suspend fun upsertFridayMedia(items: List<FridayMediaEntity>)

    @Query("DELETE FROM friday_media WHERE id = :id")
    suspend fun deleteFridayMedia(id: String)

    // --- dashboard backgrounds ---

    @Query("SELECT * FROM dashboard_backgrounds ORDER BY sortOrder ASC")
    fun observeDashboardBackgrounds(): Flow<List<DashboardBackgroundEntity>>

    @Query("SELECT * FROM dashboard_backgrounds ORDER BY sortOrder ASC")
    suspend fun getDashboardBackgrounds(): List<DashboardBackgroundEntity>

    @Upsert
    suspend fun upsertDashboardBackgrounds(items: List<DashboardBackgroundEntity>)

    @Query("DELETE FROM dashboard_backgrounds WHERE id = :id")
    suspend fun deleteDashboardBackground(id: String)

    // --- slideshow schedules ---

    @Query("SELECT * FROM slideshow_schedules ORDER BY createdAt ASC")
    fun observeSlideshowSchedules(): Flow<List<SlideshowScheduleEntity>>

    @Query("SELECT * FROM slideshow_schedules ORDER BY createdAt ASC")
    suspend fun getSlideshowSchedules(): List<SlideshowScheduleEntity>

    @Upsert
    suspend fun upsertSlideshowSchedules(items: List<SlideshowScheduleEntity>)

    @Query("DELETE FROM slideshow_schedules WHERE id = :id")
    suspend fun deleteSlideshowSchedule(id: String)

    // --- standby schedules ---

    @Query("SELECT * FROM standby_schedules ORDER BY createdAt ASC")
    fun observeStandbySchedules(): Flow<List<StandbyScheduleEntity>>

    @Query("SELECT * FROM standby_schedules ORDER BY createdAt ASC")
    suspend fun getStandbySchedules(): List<StandbyScheduleEntity>

    @Upsert
    suspend fun upsertStandbySchedules(items: List<StandbyScheduleEntity>)

    @Query("DELETE FROM standby_schedules WHERE id = :id")
    suspend fun deleteStandbySchedule(id: String)

    // --- connected devices ---

    @Query("SELECT * FROM connected_devices ORDER BY lastSeen DESC")
    fun observeDevices(): Flow<List<ConnectedDeviceEntity>>

    @Query("SELECT * FROM connected_devices ORDER BY lastSeen DESC")
    suspend fun getDevices(): List<ConnectedDeviceEntity>

    @Query("SELECT * FROM connected_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDevice(deviceId: String): ConnectedDeviceEntity?

    @Upsert
    suspend fun upsertDevices(items: List<ConnectedDeviceEntity>)

    @Query("DELETE FROM connected_devices WHERE deviceId = :deviceId")
    suspend fun deleteDevice(deviceId: String)

    // --- Kemenag schedules ---

    @Query("SELECT * FROM kemenag_schedules WHERE cityId = :cityId ORDER BY dateString ASC")
    fun observeKemenagSchedules(cityId: String): Flow<List<KemenagScheduleEntity>>

    @Query("SELECT * FROM kemenag_schedules WHERE cityId = :cityId AND dateString = :date LIMIT 1")
    suspend fun getKemenagSchedule(cityId: String, date: String): KemenagScheduleEntity?

    @Upsert
    suspend fun upsertKemenagSchedules(items: List<KemenagScheduleEntity>)

    /**
     * One city's cached schedule.
     *
     * Scoped, because the key became composite so that two cities *can* coexist. Under Realm's
     * single-column key "clear the schedules" and "clear this city's schedules" were the same
     * statement; here they are not, and the caller almost always means the narrower one.
     */
    @Query("DELETE FROM kemenag_schedules WHERE cityId = :cityId")
    suspend fun clearKemenagSchedules(cityId: String)

    /** Every city. Only for a reset. */
    @Query("DELETE FROM kemenag_schedules")
    suspend fun clearAllKemenagSchedules()

    /**
     * The whole migration, as one transaction.
     *
     * Ordering used to matter here and that was a data-loss bug: the config row was written first
     * and is also what marks the migration as done, so a failure part-way left the marker set and
     * the seven side tables never arrived — permanently, because the next launch saw the marker and
     * skipped. All or nothing removes the question rather than answering it.
     */
    @Transaction
    suspend fun migrateAll(
        config: MosqueConfigEntity?,
        announcements: List<AnnouncementEntity>,
        media: List<MediaEntity>,
        fridayMedia: List<FridayMediaEntity>,
        backgrounds: List<DashboardBackgroundEntity>,
        slideshow: List<SlideshowScheduleEntity>,
        standby: List<StandbyScheduleEntity>,
        kemenag: List<KemenagScheduleEntity>
    ) {
        upsertAnnouncements(announcements)
        upsertMedia(media)
        upsertFridayMedia(fridayMedia)
        upsertDashboardBackgrounds(backgrounds)
        upsertSlideshowSchedules(slideshow)
        upsertStandbySchedules(standby)
        upsertKemenagSchedules(kemenag)
        // Last, deliberately: it is the marker, so even if the transaction guarantee were ever lost
        // the failure mode becomes "migrate again" rather than "never migrate the rest".
        if (config != null) upsertConfig(config)
    }

    /**
     * The newest write in each table, which is what a sync compares.
     *
     * One query rather than nine round trips, and it returns a row per table so a caller can see
     * *which* table moved rather than only that something did — the difference between "the display
     * and this phone disagree" and "you changed the Friday schedule; the display changed the
     * running text".
     */
    @Query(
        """
        SELECT 'mosque_config' AS tableName, COALESCE(MAX(lastModified), 0) AS lastModified FROM mosque_config
        UNION ALL SELECT 'announcements', COALESCE(MAX(lastModified), 0) FROM announcements
        UNION ALL SELECT 'media', COALESCE(MAX(lastModified), 0) FROM media
        UNION ALL SELECT 'friday_media', COALESCE(MAX(lastModified), 0) FROM friday_media
        UNION ALL SELECT 'dashboard_backgrounds', COALESCE(MAX(lastModified), 0) FROM dashboard_backgrounds
        UNION ALL SELECT 'slideshow_schedules', COALESCE(MAX(lastModified), 0) FROM slideshow_schedules
        UNION ALL SELECT 'standby_schedules', COALESCE(MAX(lastModified), 0) FROM standby_schedules
        """
    )
    suspend fun tableTimestamps(): List<TableTimestamp>
}

/** One table's newest write. `connected_devices` and `kemenag_schedules` are excluded: access
 *  control is server-authoritative and Kemenag rows are a cache that is re-fetched, so neither is
 *  something two devices need to agree about. */
data class TableTimestamp(
    val tableName: String,
    val lastModified: Long
)
