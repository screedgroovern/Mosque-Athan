package com.groovernapp.mosqueathan.domain

import kotlinx.serialization.Serializable

/**
 * Every setting, as plain immutable data.
 *
 * The persisted form is `data.MosqueConfigEntity`, a Realm object. Keeping the two
 * apart is what lets the UI and prayer-time code be multiplatform, but it also fixes
 * something that was biting on Android already: a Realm object is not a data class, so
 * the entity carried a hand-written 165-line `copy(...)` extension, and adding a field
 * meant editing the entity, that extension, and the repository's update path. Miss one
 * and the value silently never persists. This class gets `copy()` from the compiler.
 *
 * Field names match the entity exactly, so the mapper is mechanical and a rename here
 * is a compile error there rather than a silent mismatch.
 */
@Serializable
data class MosqueConfig(
    val id: String = "config",
    val latitude: Double = MosqueConfigDefaults.DEFAULT_LATITUDE,
    val longitude: Double = MosqueConfigDefaults.DEFAULT_LONGITUDE,
    val iqomahSubuh: Int = MosqueConfigDefaults.IQOMAH_SUBUH,
    val iqomahDzuhur: Int = MosqueConfigDefaults.IQOMAH_DZUHUR,
    val iqomahAshar: Int = MosqueConfigDefaults.IQOMAH_ASHAR,
    val iqomahMaghrib: Int = MosqueConfigDefaults.IQOMAH_MAGHRIB,
    val iqomahIsya: Int = MosqueConfigDefaults.IQOMAH_ISYA,
    val calculationMethodName: String = MosqueConfigDefaults.DEFAULT_CALCULATION_METHOD,
    val adzanLeadMinutes: Int = MosqueConfigDefaults.DEFAULT_ADZAN_LEAD_MINUTES,
    val customBackgroundPath: String = "",
    val transitionType: String = "FADE",
    val slideIntervalSeconds: Int = 10,
    val videoWaitUntilEnd: Boolean = false,
    val showSettingsOnTv: Boolean = false,
    val pinCode: String = "",
    val offsetImsak: Int = 0,
    val offsetSubuh: Int = 0,
    val offsetSyuruq: Int = 0,
    val offsetDzuhur: Int = 0,
    val offsetAshar: Int = 0,
    val offsetMaghrib: Int = 0,
    val offsetIsya: Int = 0,
    val includeDefaultImage: Boolean = true,
    val kemenagCityId: String = "58a2fc6ed39fd083f55d4182bf88826d",
    val kemenagCityName: String = "KOTA JAKARTA",
    val kemenagDuration: String = "1_MONTH",
    val kemenagTargetDate: String = "",
    val adzanFooterText: String = "Mohon bersiap dan hentikan aktivitas sementara.",
    val iqomahFooterText: String = "Mari merapatkan dan meluruskan shaf.",
    val standbyModeEnabled: Boolean = MosqueConfigDefaults.DEFAULT_STANDBY_ENABLED,
    val standbyStartTime: String = MosqueConfigDefaults.DEFAULT_STANDBY_START_TIME,
    val standbyEndTime: String = MosqueConfigDefaults.DEFAULT_STANDBY_END_TIME,
    /** "SERVER" or "REMOTE" */
    val connectivityMode: String = "SERVER",
    val remoteTvIp: String = "",
    val dashboardRandomize: Boolean = false,
    val dashboardTransitionType: String = "FADE",
    val dashboardTransitionInterval: Int = 86400,
    val slideshowEnabled: Boolean = false,
    val jumatEnabled: Boolean = false,
    val jumatStartTime: String = "11:00",
    val jumatEndTime: String = "13:00",
    val offsetJumat: Int = 0,
    val jumatStandbyOnly: Boolean = false,
    val jumatWarningText: String = "",
    val murottalEnabled: Boolean = false,
    val murottalStartDelay: Int = 0,
    val murottalAudioPaths: String = "",
    val jumatSlideshowEnabled: Boolean = false,
    val includeGlobalSlideshow: Boolean = false,
    val selectedGlobalSlideIds: String = "",
    val jumatTransitionType: String = "FADE",
    val jumatSlideIntervalSeconds: Int = 10,
    val jumatSlideIntervalUnit: String = "SECONDS",
    val jumatVideoWaitUntilEnd: Boolean = true
)
