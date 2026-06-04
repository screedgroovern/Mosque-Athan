package com.groovernapp.mosqueathan.ui.iqomah

import com.groovernapp.mosqueathan.domain.PrayerType

data class IqomahUiModel(
    val prayerType: PrayerType,
    val iqomahMinutes: Int,
    val remainingSeconds: Int,
    val currentTime: String
) {
    val iqomahCountdown: String
        get() {
            val hours = remainingSeconds / 3600
            val minutes = (remainingSeconds % 3600) / 60
            val seconds = remainingSeconds % 60

            return if (hours > 0) {
                "%02d:%02d:%02d".format(
                    hours,
                    minutes,
                    seconds
                )
            } else {
                "%02d:%02d".format(
                    minutes,
                    seconds
                )
            }
        }
}
