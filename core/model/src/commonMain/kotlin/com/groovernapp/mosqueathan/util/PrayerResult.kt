package com.groovernapp.mosqueathan.util

import com.groovernapp.mosqueathan.domain.PrayerType
import kotlinx.datetime.LocalDateTime

data class PrayerResult(
    val imsak: LocalDateTime,
    val subuh: LocalDateTime,
    val syuruq: LocalDateTime,
    val dzuhur: LocalDateTime,
    val ashar: LocalDateTime,
    val maghrib: LocalDateTime,
    val isya: LocalDateTime,
    val currentPrayer: PrayerType,
    val nextPrayer: PrayerType,
    val nextPrayerTime: LocalDateTime
)