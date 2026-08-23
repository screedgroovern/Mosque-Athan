package com.groovernapp.mosqueathan.data

import com.groovernapp.mosqueathan.domain.MosqueConfigDefaults
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class MosqueConfigEntity : RealmObject {
    @PrimaryKey
    var id: String = "config"
    var latitude: Double = MosqueConfigDefaults.DEFAULT_LATITUDE
    var longitude: Double = MosqueConfigDefaults.DEFAULT_LONGITUDE
    var iqomahSubuh: Int = MosqueConfigDefaults.IQOMAH_SUBUH
    var iqomahDzuhur: Int = MosqueConfigDefaults.IQOMAH_DZUHUR
    var iqomahAshar: Int = MosqueConfigDefaults.IQOMAH_ASHAR
    var iqomahMaghrib: Int = MosqueConfigDefaults.IQOMAH_MAGHRIB
    var iqomahIsya: Int = MosqueConfigDefaults.IQOMAH_ISYA
    var calculationMethodName: String = MosqueConfigDefaults.DEFAULT_CALCULATION_METHOD
    var adzanLeadMinutes: Int = MosqueConfigDefaults.DEFAULT_ADZAN_LEAD_MINUTES
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
    var standbyModeEnabled: Boolean = MosqueConfigDefaults.DEFAULT_STANDBY_ENABLED
    var standbyStartTime: String = MosqueConfigDefaults.DEFAULT_STANDBY_START_TIME
    var standbyEndTime: String = MosqueConfigDefaults.DEFAULT_STANDBY_END_TIME

    // Connectivity & Remote settings
    var connectivityMode: String = "SERVER" // "SERVER" or "REMOTE"
    var remoteTvIp: String = ""

    // Dashboard slideshow properties
    var dashboardRandomize: Boolean = false
    var dashboardTransitionType: String = "FADE"
    var dashboardTransitionInterval: Int = 86400

    // Slideshow schedule toggle
    var slideshowEnabled: Boolean = false

    // Fitur Jumat
    var jumatEnabled: Boolean = false
    var jumatStartTime: String = "11:00"
    var jumatEndTime: String = "13:00"
    var offsetJumat: Int = 0
    var jumatStandbyOnly: Boolean = false
    var jumatWarningText: String = ""

    // Murottal
    var murottalEnabled: Boolean = false
    var murottalStartDelay: Int = 0
    var murottalAudioPaths: String = ""

    // Slideshow Jumat
    var jumatSlideshowEnabled: Boolean = false
    var includeGlobalSlideshow: Boolean = false
    var selectedGlobalSlideIds: String = ""
    var jumatTransitionType: String = "FADE"
    var jumatSlideIntervalSeconds: Int = 10
    var jumatSlideIntervalUnit: String = "SECONDS"
    var jumatVideoWaitUntilEnd: Boolean = true
}
