package com.groovernapp.mosqueathan.domain

/**
 * Factory defaults for [MosqueConfig].
 *
 * Was named `MosqueConfig` until that name was needed by the config data class itself.
 * Kept separate rather than folded into the data class's default arguments because the
 * Realm entity needs the same values, and two copies of a default is how a fresh
 * install and a migrated one end up disagreeing.
 */
object MosqueConfigDefaults {

    const val DEFAULT_LATITUDE = -7.713585464212388
    const val DEFAULT_LONGITUDE = 110.37963165899664
    const val DEFAULT_CALCULATION_METHOD = "SINGAPORE"

    const val IQOMAH_SUBUH = 10

    const val IQOMAH_DZUHUR = 10

    const val IQOMAH_ASHAR = 10

    const val IQOMAH_MAGHRIB = 5

    const val IQOMAH_ISYA = 10

    const val DEFAULT_ADZAN_LEAD_MINUTES = 3

    const val DEFAULT_STANDBY_ENABLED = false
    const val DEFAULT_STANDBY_START_TIME = "22:00"
    const val DEFAULT_STANDBY_END_TIME = "04:00"
}