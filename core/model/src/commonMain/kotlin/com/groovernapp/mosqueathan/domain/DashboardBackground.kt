package com.groovernapp.mosqueathan.domain

/**
 * One background image for the dashboard.
 *
 * Plain-data counterpart of `data.DashboardBackgroundEntity`.
 */
data class DashboardBackground(
    val id: String = "",
    val filePath: String = "",
    val isUsed: Boolean = true,
    val sortOrder: Int = 0
)
