package com.groovernapp.mosqueathan.ui.iqomah

import com.groovernapp.mosqueathan.util.pad
import com.groovernapp.mosqueathan.domain.PrayerType

data class IqomahUiModel(
    val prayerType: PrayerType,
    val iqomahMinutes: Int,
    val remainingSeconds: Int,
    val currentTime: String,
    val footerText: String = "Mari merapatkan dan meluruskan shaf."
) {
    val iqomahCountdown: String
        get() {
            val hours = remainingSeconds / 3600
            val minutes = (remainingSeconds % 3600) / 60
            val seconds = remainingSeconds % 60

            return if (hours > 0) {
                "${hours.pad(2)}:${minutes.pad(2)}:${seconds.pad(2)}"
            } else {
                "${minutes.pad(2)}:${seconds.pad(2)}"
            }
        }
}
