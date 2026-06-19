package com.groovernapp.mosqueathan.data

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class StandbyScheduleEntity : RealmObject, ScheduleItem {
    @PrimaryKey
    override var id: String = ""
    override var startTime: String = "" // HH:mm
    override var endTime: String = "" // HH:mm
    override var activeDays: String = "" // Comma-separated weekly days
    override var specificDates: String = "" // Comma-separated dates
    override var createdAt: Long = 0L

    // Legacy fields for backward compatibility
    var day: String = ""
    var isSpecificDate: Boolean = false
}
