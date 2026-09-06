package com.groovernapp.mosqueathan.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "standby_schedules")
data class StandbyScheduleEntity(
    @PrimaryKey
    val id: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val activeDays: String = "",
    val specificDates: String = "",
    val createdAt: Long = 0L,
    val day: String = "",
    val isSpecificDate: Boolean = false,
    /** Set by whichever device wrote this row; see RoomEntities' note on sync. */
    val lastModified: Long = 0L
)
