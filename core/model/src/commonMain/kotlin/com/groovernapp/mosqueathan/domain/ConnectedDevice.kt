package com.groovernapp.mosqueathan.domain

import kotlinx.serialization.Serializable

/**
 * A paired controller device. Access control stays server-authoritative, so this
 * is the one table a controller never manages.
 *
 * Plain-data counterpart of `data.ConnectedDeviceEntity`.
 */
@Serializable
data class ConnectedDevice(
    val deviceId: String = "",
    val deviceName: String = "",
    val isMaster: Boolean = false,
    val isAllowed: Boolean = false,
    val authenticatedAt: Long = 0,
    val lastSeen: Long = 0,
    val isOnline: Boolean = false
)
