package com.groovernapp.mosqueathan.ui.dashboard

import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.ui.iqomah.IqomahUiModel

sealed interface DashboardUiState {

    data class Dashboard(
        val currentTime: String,
        val gregorianDate: String,
        val hijriDate: String,
        val prayerTimes: PrayerTimesUiModel,
        val nextPrayer: NextPrayerUiModel,
        val runningText: String
    ) : DashboardUiState

    data class Adzan(
        val prayerType: PrayerType,
        val prayerTime: String
    ) : DashboardUiState

    data class Iqomah(
        val iqomah: IqomahUiModel
    ) : DashboardUiState
}