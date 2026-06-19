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

    // Dashboard slideshow properties
    var dashboardRandomize: Boolean = false
    var dashboardTransitionType: String = "FADE"
    var dashboardTransitionInterval: Int = 1440

    // Slideshow schedule toggle
    var slideshowEnabled: Boolean = false
}
fun MosqueConfigEntity.copy(
    id: String = this.id,
    latitude: Double = this.latitude,
    longitude: Double = this.longitude,
    iqomahSubuh: Int = this.iqomahSubuh,
    iqomahDzuhur: Int = this.iqomahDzuhur,
    iqomahAshar: Int = this.iqomahAshar,
    iqomahMaghrib: Int = this.iqomahMaghrib,
    iqomahIsya: Int = this.iqomahIsya,
    calculationMethodName: String = this.calculationMethodName,
    adzanLeadMinutes: Int = this.adzanLeadMinutes,
    customBackgroundPath: String = this.customBackgroundPath,
    transitionType: String = this.transitionType,
    slideIntervalSeconds: Int = this.slideIntervalSeconds,
    videoWaitUntilEnd: Boolean = this.videoWaitUntilEnd,
    showSettingsOnTv: Boolean = this.showSettingsOnTv,
    pinCode: String = this.pinCode,
    offsetImsak: Int = this.offsetImsak,
    offsetSubuh: Int = this.offsetSubuh,
    offsetSyuruq: Int = this.offsetSyuruq,
    offsetDzuhur: Int = this.offsetDzuhur,
    offsetAshar: Int = this.offsetAshar,
    offsetMaghrib: Int = this.offsetMaghrib,
    offsetIsya: Int = this.offsetIsya,
    includeDefaultImage: Boolean = this.includeDefaultImage,
    kemenagCityId: String = this.kemenagCityId,
    kemenagCityName: String = this.kemenagCityName,
    kemenagDuration: String = this.kemenagDuration,
    kemenagTargetDate: String = this.kemenagTargetDate,
    adzanFooterText: String = this.adzanFooterText,
    iqomahFooterText: String = this.iqomahFooterText,
    standbyModeEnabled: Boolean = this.standbyModeEnabled,
    standbyStartTime: String = this.standbyStartTime,
    standbyEndTime: String = this.standbyEndTime,
    dashboardRandomize: Boolean = this.dashboardRandomize,
    dashboardTransitionType: String = this.dashboardTransitionType,
    dashboardTransitionInterval: Int = this.dashboardTransitionInterval,
    slideshowEnabled: Boolean = this.slideshowEnabled
): MosqueConfigEntity {
    val newEntity = MosqueConfigEntity()
    newEntity.id = id
    newEntity.latitude = latitude
    newEntity.longitude = longitude
    newEntity.iqomahSubuh = iqomahSubuh
    newEntity.iqomahDzuhur = iqomahDzuhur
    newEntity.iqomahAshar = iqomahAshar
    newEntity.iqomahMaghrib = iqomahMaghrib
    newEntity.iqomahIsya = iqomahIsya
    newEntity.calculationMethodName = calculationMethodName
    newEntity.adzanLeadMinutes = adzanLeadMinutes
    newEntity.customBackgroundPath = customBackgroundPath
    newEntity.transitionType = transitionType
    newEntity.slideIntervalSeconds = slideIntervalSeconds
    newEntity.videoWaitUntilEnd = videoWaitUntilEnd
    newEntity.showSettingsOnTv = showSettingsOnTv
    newEntity.pinCode = pinCode
    newEntity.offsetImsak = offsetImsak
    newEntity.offsetSubuh = offsetSubuh
    newEntity.offsetSyuruq = offsetSyuruq
    newEntity.offsetDzuhur = offsetDzuhur
    newEntity.offsetAshar = offsetAshar
    newEntity.offsetMaghrib = offsetMaghrib
    newEntity.offsetIsya = offsetIsya
    newEntity.includeDefaultImage = includeDefaultImage
    newEntity.kemenagCityId = kemenagCityId
    newEntity.kemenagCityName = kemenagCityName
    newEntity.kemenagDuration = kemenagDuration
    newEntity.kemenagTargetDate = kemenagTargetDate
    newEntity.adzanFooterText = adzanFooterText
    newEntity.iqomahFooterText = iqomahFooterText
    newEntity.standbyModeEnabled = standbyModeEnabled
    newEntity.standbyStartTime = standbyStartTime
    newEntity.standbyEndTime = standbyEndTime
    newEntity.dashboardRandomize = dashboardRandomize
    newEntity.dashboardTransitionType = dashboardTransitionType
    newEntity.dashboardTransitionInterval = dashboardTransitionInterval
    newEntity.slideshowEnabled = slideshowEnabled
    return newEntity
}
