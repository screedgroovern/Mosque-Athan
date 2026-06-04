package com.groovernapp.mosqueathan.data

import com.groovernapp.mosqueathan.domain.MosqueConfig
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class MosqueConfigEntity : RealmObject {
    @PrimaryKey
    var id: String = "config"
    var latitude: Double = MosqueConfig.DEFAULT_LATITUDE
    var longitude: Double = MosqueConfig.DEFAULT_LONGITUDE
    var iqomahSubuh: Int = MosqueConfig.IQOMAH_SUBUH
    var iqomahDzuhur: Int = MosqueConfig.IQOMAH_DZUHUR
    var iqomahAshar: Int = MosqueConfig.IQOMAH_ASHAR
    var iqomahMaghrib: Int = MosqueConfig.IQOMAH_MAGHRIB
    var iqomahIsya: Int = MosqueConfig.IQOMAH_ISYA
    var calculationMethodName: String = MosqueConfig.DEFAULT_CALCULATION_METHOD
    var adzanLeadMinutes: Int = MosqueConfig.DEFAULT_ADZAN_LEAD_MINUTES
    var customBackgroundPath: String = ""
    var transitionType: String = "FADE"
    var slideIntervalSeconds: Int = 10
    var videoWaitUntilEnd: Boolean = false
}
