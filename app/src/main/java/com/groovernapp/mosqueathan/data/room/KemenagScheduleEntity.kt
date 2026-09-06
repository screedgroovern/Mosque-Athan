package com.groovernapp.mosqueathan.data.room

import androidx.room.Entity

/**
 * Keyed by date *and* city, which Realm's version was not.
 *
 * There the primary key was `dateString` alone, so two cities could never hold a schedule for the
 * same day — changing city silently overwrote the previous one row at a time as the fetch
 * progressed. Nothing failed and nothing said so. A new schema is the cheapest moment this will
 * ever be to correct, and the migration loses nothing: every row on disk today belongs to whichever
 * city was last fetched.
 */
@Entity(tableName = "kemenag_schedules", primaryKeys = ["dateString", "cityId"])
data class KemenagScheduleEntity(
    val dateString: String = "",
    val cityId: String = "",
    val imsak: String = "",
    val subuh: String = "",
    val terbit: String = "",
    val dhuha: String = "",
    val dzuhur: String = "",
    val ashar: String = "",
    val maghrib: String = "",
    val isya: String = "",
    /** Set by whichever device wrote this row; see RoomEntities' note on sync. */
    val lastModified: Long = 0L
)
