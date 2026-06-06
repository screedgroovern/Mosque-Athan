package com.groovernapp.mosqueathan.data

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class KemenagScheduleEntity : RealmObject {
    @PrimaryKey
    var dateString: String = "" // Format "YYYY-MM-DD"
    var cityId: String = ""
    var imsak: String = ""
    var subuh: String = ""
    var terbit: String = ""
    var dhuha: String = ""
    var dzuhur: String = ""
    var ashar: String = ""
    var maghrib: String = ""
    var isya: String = ""
}
