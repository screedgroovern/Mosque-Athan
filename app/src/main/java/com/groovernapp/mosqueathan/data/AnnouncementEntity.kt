package com.groovernapp.mosqueathan.data

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class AnnouncementEntity : RealmObject {
    @PrimaryKey
    var id: String = ""
    var content: String = ""
    var isActive: Boolean = true
    var position: Int = 0
}
