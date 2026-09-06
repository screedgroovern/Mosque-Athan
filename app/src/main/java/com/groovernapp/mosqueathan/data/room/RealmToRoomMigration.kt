package com.groovernapp.mosqueathan.data.room

import android.content.Context
import android.util.Log
import com.groovernapp.mosqueathan.data.AnnouncementEntity as RealmAnnouncement
import com.groovernapp.mosqueathan.data.DashboardBackgroundEntity as RealmBackground
import com.groovernapp.mosqueathan.data.FridayMediaEntity as RealmFridayMedia
import com.groovernapp.mosqueathan.data.KemenagScheduleEntity as RealmKemenag
import com.groovernapp.mosqueathan.data.MediaEntity as RealmMedia
import com.groovernapp.mosqueathan.data.MosqueConfigEntity as RealmConfig
import com.groovernapp.mosqueathan.data.SlideshowScheduleEntity as RealmSlideshow
import com.groovernapp.mosqueathan.data.StandbyScheduleEntity as RealmStandby
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import java.io.File

/**
 * Carries a 1.x device's Realm database into Room, once, on first launch after the upgrade.
 *
 * **There is no bridge release, because there is nothing a bridge could do that this cannot.** A
 * version that wrote to both stores, or exported to a file, would only be preparing data that 2.0
 * can already read directly — Realm is still on the classpath here for exactly this reason, and
 * leaves in 2.1.
 *
 * The population this has to be right for is one mosque television plus a couple of test devices.
 * That is not a fleet rollout; it is a single upgrade that must not lose a takmir's prayer offsets,
 * running text and schedules. Everything below is shaped by that.
 */
object RealmToRoomMigration {

    private const val TAG = "RealmToRoom"
    private const val REALM_NAME = "mosque_athan.realm"
    private const val BACKUP_SUFFIX = ".pre-room-backup"

    sealed interface Result {
        /** No Realm file, or Room already holds data. Nothing to do and nothing was touched. */
        data object NotNeeded : Result

        data class Migrated(val rows: Map<String, Int>) : Result

        /**
         * Realm could not be read. Room is left empty and the Realm file is untouched, so the next
         * launch tries again and the data is still on disk either way.
         */
        data class Failed(val reason: String) : Result
    }

    /**
     * Copies every table Realm holds into Room, unless Room already has a config row.
     *
     * Room having a config row is the marker rather than a separate flag, because a flag can
     * disagree with the thing it describes. It cannot: if the config row is there, this already
     * ran, and if a wipe ever removes it then re-importing from the Realm file that is still on
     * disk is the behaviour worth having anyway.
     */
    suspend fun migrateIfNeeded(context: Context, dao: MosqueDao): Result {
        val realmFile = File(context.filesDir, REALM_NAME)
        if (!realmFile.exists()) return Result.NotNeeded
        if (dao.getConfig() != null) return Result.NotNeeded

        backUp(realmFile)

        val realm = try {
            openReadOnly()
        } catch (e: Throwable) {
            // Deliberately Throwable: a schema mismatch surfaces from Realm's native layer and is
            // not always an Exception. Failing here must cost nothing — the file is still there.
            Log.e(TAG, "could not open the Realm database; leaving it alone", e)
            return Result.Failed(e.message ?: "could not open Realm")
        }

        return try {
            copyInto(realm, dao)
        } catch (e: Throwable) {
            Log.e(TAG, "migration failed part-way; Room may be partially filled", e)
            Result.Failed(e.message ?: "could not read Realm")
        } finally {
            // Realm here is not Closeable, so `use` does not apply; closing by hand is the only
            // option and leaking the instance would keep a lock on a file we may need to reopen.
            runCatching { realm.close() }
        }
    }

    /**
     * Opens the old database **without `deleteRealmIfMigrationNeeded`**, which is the one thing
     * about this function that matters.
     *
     * That flag is on the live 1.x configuration and it is the most dangerous thing that could be
     * copied into this file: it exists to delete the database when the schema does not match, which
     * is precisely the case a rescue has to survive. A device carrying an older schema must make
     * this throw, not lose its data.
     *
     * This build's Realm has no `readOnly()` on the builder, so the guarantee that nothing is
     * written is held by this file never opening a write transaction rather than by the
     * configuration. Keep it that way.
     */
    private fun openReadOnly(): Realm = Realm.open(
        RealmConfiguration.Builder(
            schema = setOf(
                RealmConfig::class,
                RealmAnnouncement::class,
                RealmMedia::class,
                RealmFridayMedia::class,
                RealmBackground::class,
                RealmSlideshow::class,
                RealmStandby::class,
                RealmKemenag::class
            )
        )
            .name(REALM_NAME)
            .schemaVersion(REALM_SCHEMA_VERSION)
            .build()
    )

    /**
     * A copy kept beside the original, never deleted by this code.
     *
     * Cheap — the file is well under a megabyte — and it is what makes the whole operation
     * reversible by hand if the import turns out to be wrong in a way no test caught.
     */
    private fun backUp(realmFile: File) {
        val backup = File(realmFile.parentFile, realmFile.name + BACKUP_SUFFIX)
        if (backup.exists()) return
        runCatching { realmFile.copyTo(backup) }
            .onFailure { Log.w(TAG, "could not back up the Realm file; continuing", it) }
    }

    private suspend fun copyInto(realm: Realm, dao: MosqueDao): Result.Migrated {
        // One timestamp for every migrated row, and it is the migration's own clock rather than
        // anything from the old data. It reads as "this is what the device held at the upgrade",
        // which is exactly true. Mixing in per-row creation times would make a schedule written a
        // year ago look older than a config field nobody has touched since, and an automatic
        // timestamp merge would then quietly prefer a controller's staler copy.
        val now = System.currentTimeMillis()
        val counts = mutableMapOf<String, Int>()

        var config: MosqueConfigEntity? = null
        var announcements = emptyList<AnnouncementEntity>()
        var media = emptyList<MediaEntity>()
        var fridayMedia = emptyList<FridayMediaEntity>()
        var backgrounds = emptyList<DashboardBackgroundEntity>()
        var slideshow = emptyList<SlideshowScheduleEntity>()
        var standby = emptyList<StandbyScheduleEntity>()
        var kemenag = emptyList<KemenagScheduleEntity>()

        realm.query<RealmConfig>().first().find()?.let { c ->
            config = (
                MosqueConfigEntity(
                    id = c.id,
                    latitude = c.latitude, longitude = c.longitude,
                    iqomahSubuh = c.iqomahSubuh, iqomahDzuhur = c.iqomahDzuhur,
                    iqomahAshar = c.iqomahAshar, iqomahMaghrib = c.iqomahMaghrib,
                    iqomahIsya = c.iqomahIsya,
                    calculationMethodName = c.calculationMethodName,
                    adzanLeadMinutes = c.adzanLeadMinutes,
                    customBackgroundPath = c.customBackgroundPath,
                    transitionType = c.transitionType,
                    slideIntervalSeconds = c.slideIntervalSeconds,
                    videoWaitUntilEnd = c.videoWaitUntilEnd,
                    showSettingsOnTv = c.showSettingsOnTv,
                    pinCode = c.pinCode,
                    offsetImsak = c.offsetImsak, offsetSubuh = c.offsetSubuh,
                    offsetSyuruq = c.offsetSyuruq, offsetDzuhur = c.offsetDzuhur,
                    offsetAshar = c.offsetAshar, offsetMaghrib = c.offsetMaghrib,
                    offsetIsya = c.offsetIsya,
                    includeDefaultImage = c.includeDefaultImage,
                    kemenagCityId = c.kemenagCityId, kemenagCityName = c.kemenagCityName,
                    kemenagDuration = c.kemenagDuration, kemenagTargetDate = c.kemenagTargetDate,
                    adzanFooterText = c.adzanFooterText, iqomahFooterText = c.iqomahFooterText,
                    standbyModeEnabled = c.standbyModeEnabled,
                    standbyStartTime = c.standbyStartTime, standbyEndTime = c.standbyEndTime,
                    connectivityMode = c.connectivityMode, remoteTvIp = c.remoteTvIp,
                    dashboardRandomize = c.dashboardRandomize,
                    dashboardTransitionType = c.dashboardTransitionType,
                    dashboardTransitionInterval = c.dashboardTransitionInterval,
                    slideshowEnabled = c.slideshowEnabled,
                    jumatEnabled = c.jumatEnabled,
                    jumatStartTime = c.jumatStartTime, jumatEndTime = c.jumatEndTime,
                    offsetJumat = c.offsetJumat, jumatStandbyOnly = c.jumatStandbyOnly,
                    jumatWarningText = c.jumatWarningText,
                    murottalEnabled = c.murottalEnabled,
                    murottalStartDelay = c.murottalStartDelay,
                    murottalAudioPaths = c.murottalAudioPaths,
                    jumatSlideshowEnabled = c.jumatSlideshowEnabled,
                    includeGlobalSlideshow = c.includeGlobalSlideshow,
                    selectedGlobalSlideIds = c.selectedGlobalSlideIds,
                    jumatTransitionType = c.jumatTransitionType,
                    jumatSlideIntervalSeconds = c.jumatSlideIntervalSeconds,
                    jumatSlideIntervalUnit = c.jumatSlideIntervalUnit,
                    jumatVideoWaitUntilEnd = c.jumatVideoWaitUntilEnd,
                    lastModified = now
                )
            )
            counts["mosque_config"] = 1
        }

        realm.query<RealmAnnouncement>().find().map {
            AnnouncementEntity(
                id = it.id, content = it.content, isActive = it.isActive,
                position = it.position, lastModified = now
            )
        }.let { announcements = it; counts["announcements"] = it.size }

        realm.query<RealmMedia>().find().map {
            MediaEntity(
                id = it.id, filePath = it.filePath, isVideo = it.isVideo,
                isUsed = it.isUsed, sortOrder = it.sortOrder, lastModified = now
            )
        }.let { media = it; counts["media"] = it.size }

        realm.query<RealmFridayMedia>().find().map {
            FridayMediaEntity(
                id = it.id, filePath = it.filePath, isVideo = it.isVideo,
                sortOrder = it.sortOrder, lastModified = now
            )
        }.let { fridayMedia = it; counts["friday_media"] = it.size }

        realm.query<RealmBackground>().find().map {
            DashboardBackgroundEntity(
                id = it.id, filePath = it.filePath, isUsed = it.isUsed,
                sortOrder = it.sortOrder, lastModified = now
            )
        }.let { backgrounds = it; counts["dashboard_backgrounds"] = it.size }

        realm.query<RealmSlideshow>().find().map {
            SlideshowScheduleEntity(
                id = it.id, startTime = it.startTime, endTime = it.endTime,
                activeDays = it.activeDays, specificDates = it.specificDates,
                createdAt = it.createdAt, day = it.day, isSpecificDate = it.isSpecificDate,
                lastModified = now
            )
        }.let { slideshow = it; counts["slideshow_schedules"] = it.size }

        realm.query<RealmStandby>().find().map {
            StandbyScheduleEntity(
                id = it.id, startTime = it.startTime, endTime = it.endTime,
                activeDays = it.activeDays, specificDates = it.specificDates,
                createdAt = it.createdAt, day = it.day, isSpecificDate = it.isSpecificDate,
                lastModified = now
            )
        }.let { standby = it; counts["standby_schedules"] = it.size }

        realm.query<RealmKemenag>().find().map {
            KemenagScheduleEntity(
                dateString = it.dateString, cityId = it.cityId, imsak = it.imsak,
                subuh = it.subuh, terbit = it.terbit, dhuha = it.dhuha, dzuhur = it.dzuhur,
                ashar = it.ashar, maghrib = it.maghrib, isya = it.isya, lastModified = now
            )
        }.let { kemenag = it; counts["kemenag_schedules"] = it.size }

        // ConnectedDeviceEntity is deliberately not carried over. Those rows are access-control
        // bookkeeping the display rebuilds as devices register — the live display is holding
        // eighteen of them, most for browsers that were closed months ago. Migrating them would
        // move a pile of stale entries into a fresh schema for no gain.

        // One transaction, so a failure part-way leaves Room exactly as it was. This used to write
        // as it read, and because the config row doubles as the "already migrated" marker, a throw
        // between it and the side tables lost them permanently.
        dao.migrateAll(
            config = config,
            announcements = announcements,
            media = media,
            fridayMedia = fridayMedia,
            backgrounds = backgrounds,
            slideshow = slideshow,
            standby = standby,
            kemenag = kemenag
        )

        Log.i(TAG, "migrated $counts")
        return Result.Migrated(counts)
    }

    /**
     * The schema the 1.x line shipped. Anything older throws in [openReadOnly] rather than being
     * silently destroyed, which is the whole reason that function does not reuse the live config.
     */
    private const val REALM_SCHEMA_VERSION = 18L
}
