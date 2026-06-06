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
    var showSettingsOnTv: Boolean = false
    var pinCode: String = ""
    var offsetImsak: Int = 0
    var offsetSubuh: Int = 0
    var offsetSyuruq: Int = 0
    var offsetDzuhur: Int = 0
    var offsetAshar: Int = 0
    var offsetMaghrib: Int = 0
    var offsetIsya: Int = 0
    var includeDefaultImage: Boolean = true
    var kemenagCityId: String = "58a2fc6ed39fd083f55d4182bf88826d"
    var kemenagCityName: String = "KOTA JAKARTA"
    var kemenagDuration: String = "1_MONTH"
    var kemenagTargetDate: String = ""
    var adzanFooterText: String = "Mohon bersiap dan hentikan aktivitas sementara."
    var iqomahFooterText: String = "Mari merapatkan dan meluruskan shaf."
    var standbyModeEnabled: Boolean = MosqueConfig.DEFAULT_STANDBY_ENABLED
    var standbyStartTime: String = MosqueConfig.DEFAULT_STANDBY_START_TIME
    var standbyEndTime: String = MosqueConfig.DEFAULT_STANDBY_END_TIME
}
