package com.groovernapp.mosqueathan.data

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class ConnectedDeviceEntity : RealmObject {
    @PrimaryKey
    var deviceId: String = ""
    var deviceName: String = ""
    var isMaster: Boolean = false
    var isAllowed: Boolean = false
    var authenticatedAt: Long = 0
    var lastSeen: Long = 0
    var isOnline: Boolean = false
}
