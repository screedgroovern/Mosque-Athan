package com.groovernapp.mosqueathan.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friday_media")
data class FridayMediaEntity(
    @PrimaryKey
    val id: String = "",
    val filePath: String = "",
    val isVideo: Boolean = false,
    val sortOrder: Int = 0,
    /** Set by whichever device wrote this row; see RoomEntities' note on sync. */
    val lastModified: Long = 0L
)
