package com.groovernapp.mosqueathan.ui.adzan

import com.groovernapp.mosqueathan.domain.PrayerType

data class AdzanUiModel(
    val prayerType: PrayerType,
    val prayerTime: String,
    val currentTime: String,
    val remainingSeconds: Int,
    val footerText: String = "Mohon bersiap dan hentikan aktivitas sementara."
) {
    val countdown: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60

            return "%02d:%02d".format(minutes, seconds)
        }
}
