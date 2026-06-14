package com.groovernapp.mosqueathan.data

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class StandbyScheduleEntity : RealmObject {
    @PrimaryKey
    var id: String = ""
    var day: String = "" // Day of week (e.g., Monday, Tuesday...) or Date (YYYY-MM-DD)
    var isSpecificDate: Boolean = false
    var startTime: String = "" // HH:mm
    var endTime: String = "" // HH:mm
}
