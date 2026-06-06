package com.groovernapp.mosqueathan.ui.navigation

import com.groovernapp.mosqueathan.ui.adzan.AdzanUiModel
import com.groovernapp.mosqueathan.ui.dashboard.DashboardUiState
import com.groovernapp.mosqueathan.ui.iqomah.IqomahUiModel

sealed interface AppUiState {

    data class Dashboard(
        val state: DashboardUiState.Dashboard,
        val customBackgroundPath: String,
        val mediaList: List<com.groovernapp.mosqueathan.data.MediaEntity>,
        val transitionType: String,
        val slideIntervalSeconds: Int,
        val videoWaitUntilEnd: Boolean,
        val includeDefaultImage: Boolean
    ) : AppUiState

    data class Adzan(
        val state: AdzanUiModel
    ) : AppUiState

    data class Iqomah(
        val state: IqomahUiModel
    ) : AppUiState

    data class Settings(
        val config: com.groovernapp.mosqueathan.data.MosqueConfigEntity,
        val announcements: List<com.groovernapp.mosqueathan.data.AnnouncementEntity>,
        val mediaList: List<com.groovernapp.mosqueathan.data.MediaEntity>
    ) : AppUiState
}
