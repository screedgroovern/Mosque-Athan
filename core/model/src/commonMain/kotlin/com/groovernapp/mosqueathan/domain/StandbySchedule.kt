package com.groovernapp.mosqueathan.domain

import kotlinx.serialization.Serializable

/**
 * A window during which the display dims to standby.
 *
 * Plain-data counterpart of `data.StandbyScheduleEntity`.
 */
@Serializable
data class StandbySchedule(
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
