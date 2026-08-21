package com.groovernapp.mosqueathan.domain

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DayOfWeek

enum class PrayerType {
    IMSAK,
    SUBUH,
    SYURUQ,
    DZUHUR,
    ASHAR,
    MAGHRIB,
    ISYA;

    val displayName: String
        get() {
            val isIndonesian = com.groovernapp.mosqueathan.util.Lang.isIndonesian
            return when (this) {
                IMSAK -> if (isIndonesian) "IMSAK" else "IMSAK"
                SUBUH -> if (isIndonesian) "SUBUH" else "FAJR"
                SYURUQ -> if (isIndonesian) "SYURUQ" else "SUNRISE"
                DZUHUR -> {
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    if (now.dayOfWeek == DayOfWeek.FRIDAY) {
                        if (isIndonesian) "JUM'AT" else "FRIDAY"
                    } else {
                        if (isIndonesian) "DZUHUR" else "DHUHR"
                    }
                }
                ASHAR -> if (isIndonesian) "ASHAR" else "ASR"
                MAGHRIB -> if (isIndonesian) "MAGHRIB" else "MAGHRIB"
                ISYA -> if (isIndonesian) "ISYA'" else "ISHA'"
            }
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