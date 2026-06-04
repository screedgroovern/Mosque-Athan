package com.groovernapp.mosqueathan.domain

enum class PrayerType {
    SUBUH,
    SYURUQ,
    DZUHUR,
    ASHAR,
    MAGHRIB,
    ISYA;


    val iqomahMinutes: Int
        get() = when (this) {
            SUBUH -> MosqueConfig.IQOMAH_SUBUH
            DZUHUR -> MosqueConfig.IQOMAH_DZUHUR
            ASHAR -> MosqueConfig.IQOMAH_ASHAR
            MAGHRIB -> MosqueConfig.IQOMAH_MAGHRIB
            ISYA -> MosqueConfig.IQOMAH_ISYA
            SYURUQ -> 0
    }
}