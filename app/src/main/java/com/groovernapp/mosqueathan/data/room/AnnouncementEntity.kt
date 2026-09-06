package com.groovernapp.mosqueathan.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey
    val id: String = "",
    val content: String = "",
    val isActive: Boolean = true,
    val position: Int = 0,
    /** Set by whichever device wrote this row; see RoomEntities' note on sync. */
    val lastModified: Long = 0L
)
