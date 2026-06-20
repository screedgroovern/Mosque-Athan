package com.groovernapp.mosqueathan.domain

enum class PrayerType {
    IMSAK,
    SUBUH,
    SYURUQ,
    DZUHUR,
    ASHAR,
    MAGHRIB,
    ISYA;

    val displayName: String
        get() = when (this) {
            ISYA -> "ISYA'"
            else -> name
        }


    val iqomahMinutes: Int
        get() = when (this) {
            SUBUH -> MosqueConfig.IQOMAH_SUBUH
            DZUHUR -> MosqueConfig.IQOMAH_DZUHUR
            ASHAR -> MosqueConfig.IQOMAH_ASHAR
            MAGHRIB -> MosqueConfig.IQOMAH_MAGHRIB
            ISYA -> MosqueConfig.IQOMAH_ISYA
            IMSAK, SYURUQ -> 0
    }
}