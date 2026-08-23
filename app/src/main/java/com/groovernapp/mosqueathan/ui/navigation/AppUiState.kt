package com.groovernapp.mosqueathan.ui.navigation

import com.groovernapp.mosqueathan.ui.adzan.AdzanUiModel
import com.groovernapp.mosqueathan.ui.dashboard.DashboardUiState
import com.groovernapp.mosqueathan.ui.iqomah.IqomahUiModel

sealed interface AppUiState {

    data class Dashboard(
        val state: DashboardUiState.Dashboard,
        val customBackgroundPath: String,
        val mediaList: List<com.groovernapp.mosqueathan.domain.Media>,
        val dashboardBackgrounds: List<com.groovernapp.mosqueathan.domain.DashboardBackground> = emptyList(),
        val transitionType: String,
        val slideIntervalSeconds: Int,
        val videoWaitUntilEnd: Boolean,
        val includeDefaultImage: Boolean,
        val isPreviewMode: Boolean = false,
        val dashboardTransitionType: String = "FADE",
        val dashboardTransitionInterval: Int = 86400,
        val dashboardRandomize: Boolean = false,
        val isFridayStandby: Boolean = false,
        val jumatWarningText: String = "",
        val isFridayBeforeAdzan: Boolean = false,
        val isFridayAfterAdzan: Boolean = false
    ) : AppUiState

    data class Adzan(
        val state: AdzanUiModel
    ) : AppUiState

    data class Iqomah(
        val state: IqomahUiModel
    ) : AppUiState

    data class Settings(
        val config: com.groovernapp.mosqueathan.domain.MosqueConfig,
        val announcements: List<com.groovernapp.mosqueathan.domain.Announcement>,
        val mediaList: List<com.groovernapp.mosqueathan.domain.Media>
    ) : AppUiState
}
