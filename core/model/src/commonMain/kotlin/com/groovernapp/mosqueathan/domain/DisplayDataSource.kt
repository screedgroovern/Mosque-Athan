package com.groovernapp.mosqueathan.domain

import kotlinx.coroutines.flow.Flow

/**
 * Everything the display screens read, and nothing they can write.
 *
 * The Dashboard, Adzan and Iqomah screens are a pure function of stored state — they
 * show prayer times, media and announcements, and they never change a setting. This
 * interface is that fact expressed so the build enforces it.
 *
 * It is deliberately **not** the whole repository. `MosqueConfigRepository` has 70
 * public members; settings and the web server each use 56 of them, but the display uses
 * these nine, all reads. One combined interface would hand the display screens 61
 * members they never call, every one of them a mutation — so a display bug could always
 * be a write, and a reader of the code could never rule it out. Two interfaces along
 * the boundary the app already has costs nothing and rules it out permanently.
 *
 * Implemented by the Realm-backed repository in `:app`. The dependency points this way
 * round on purpose: `core` must never know about persistence.
 */
interface DisplayDataSource {

    /** The current settings, re-emitted whenever any of them change. */
    fun getConfigFlow(): Flow<MosqueConfig>

    /** The current settings, read once. Returns factory defaults if nothing is stored. */
    fun getConfig(): MosqueConfig

    /** Presentation slideshow media, in display order. */
    fun getMediaListFlow(): Flow<List<Media>>

    /** Dashboard background images, in display order. */
    fun getDashboardBackgroundsFlow(): Flow<List<DashboardBackground>>

    /** Friday slideshow media, in display order. */
    fun observeFridayMedia(): Flow<List<FridayMedia>>

    /** Running-text announcements that are switched on, in display order. */
    fun getActiveAnnouncementsFlow(): Flow<List<Announcement>>

    /** Windows during which the display dims to standby. */
    fun getStandbySchedules(): List<StandbySchedule>

    /** Windows during which the slideshow runs. */
    fun getSlideshowSchedules(): List<SlideshowSchedule>

    /**
     * Minutes between adzan and iqomah for [prayerType].
     *
     * Zero for `IMSAK` and `SYURUQ`: both are informational and never trigger an
     * iqomah countdown.
     */
    fun getIqomahMinutes(prayerType: PrayerType): Int
}
