package com.groovernapp.mosqueathan.domain

import kotlinx.serialization.Serializable

/**
 * A window during which the slideshow runs.
 *
 * Plain-data counterpart of `data.SlideshowScheduleEntity`.
 */
@Serializable
data class SlideshowSchedule(
    override val id: String = "",
    /** HH:mm */
    override val startTime: String = "",
    /** HH:mm */
    override val endTime: String = "",
    /** Comma-separated weekly days */
    override val activeDays: String = "",
    /** Comma-separated dates */
    override val specificDates: String = "",
    override val createdAt: Long = 0L,
    val day: String = "",
    val isSpecificDate: Boolean = false
) : ScheduleItem
