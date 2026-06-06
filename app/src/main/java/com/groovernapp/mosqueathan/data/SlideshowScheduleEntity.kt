package com.groovernapp.mosqueathan.data

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class SlideshowScheduleEntity : RealmObject {
    @PrimaryKey
    var id: String = ""
    var startTime: String = "" // HH:mm
    var endTime: String = "" // HH:mm
}
