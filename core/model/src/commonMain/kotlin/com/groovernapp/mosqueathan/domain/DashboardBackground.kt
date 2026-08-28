package com.groovernapp.mosqueathan.domain

import kotlinx.serialization.Serializable

/**
 * One background image for the dashboard.
 *
 * Plain-data counterpart of `data.DashboardBackgroundEntity`.
 */
@Serializable
data class DashboardBackground(
    val id: String = "",
    val filePath: String = "",
    val isUsed: Boolean = true,
    val sortOrder: Int = 0
)
