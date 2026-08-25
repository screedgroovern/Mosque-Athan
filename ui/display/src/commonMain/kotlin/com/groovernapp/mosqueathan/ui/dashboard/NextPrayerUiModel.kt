package com.groovernapp.mosqueathan.ui.dashboard

import com.groovernapp.mosqueathan.domain.PrayerType

data class NextPrayerUiModel(
    val prayerType: PrayerType,
    val prayerTime: String,
    val countdown: String
)
