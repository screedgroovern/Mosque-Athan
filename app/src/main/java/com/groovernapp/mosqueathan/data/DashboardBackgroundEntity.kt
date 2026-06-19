package com.groovernapp.mosqueathan.data

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class DashboardBackgroundEntity : RealmObject {
    @PrimaryKey
    var id: String = ""
    var filePath: String = ""
    var isUsed: Boolean = true
    var sortOrder: Int = 0
}
