package com.groovernapp.mosqueathan.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connected_devices")
data class ConnectedDeviceEntity(
    @PrimaryKey
    val deviceId: String = "",
    val deviceName: String = "",
    val isMaster: Boolean = false,
    val isAllowed: Boolean = false,
    val authenticatedAt: Long = 0,
    val lastSeen: Long = 0,
    val isOnline: Boolean = false,
    /** Set by whichever device wrote this row; see RoomEntities' note on sync. */
    val lastModified: Long = 0L
)
