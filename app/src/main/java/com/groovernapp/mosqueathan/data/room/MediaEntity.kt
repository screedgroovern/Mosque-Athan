package com.groovernapp.mosqueathan.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey
    val id: String = "",
    val filePath: String = "",
    val isVideo: Boolean = false,
    val isUsed: Boolean = true,
    val sortOrder: Int = 0,
    /** Set by whichever device wrote this row; see RoomEntities' note on sync. */
    val lastModified: Long = 0L
)
