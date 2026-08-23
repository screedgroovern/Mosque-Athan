package com.groovernapp.mosqueathan.domain

/**
 * A line of running text on the dashboard.
 *
 * Plain-data counterpart of `data.AnnouncementEntity`.
 */
data class Announcement(
    val id: String = "",
    val content: String = "",
    val isActive: Boolean = true,
    val position: Int = 0
)
