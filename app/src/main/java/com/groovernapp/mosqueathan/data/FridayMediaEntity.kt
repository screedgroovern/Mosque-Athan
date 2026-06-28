package com.groovernapp.mosqueathan.data

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class FridayMediaEntity : RealmObject {
    @PrimaryKey
    var id: String = ""
    var filePath: String = ""
    var isVideo: Boolean = false
    var sortOrder: Int = 0
}
