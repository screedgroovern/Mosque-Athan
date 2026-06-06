package com.groovernapp.mosqueathan.ui.dashboard

import com.groovernapp.mosqueathan.domain.PrayerType

data class PrayerTimesUiModel(
    val imsak: String,
    val subuh: String,
    val syuruq: String,
    val dzuhur: String,
    val ashar: String,
    val maghrib: String,
    val isya: String,
    val highlightedPrayer: PrayerType?
)