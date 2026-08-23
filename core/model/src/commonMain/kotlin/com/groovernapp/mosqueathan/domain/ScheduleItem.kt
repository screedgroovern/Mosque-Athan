package com.groovernapp.mosqueathan.domain

/**
 * What the slideshow and standby schedules have in common, so one set of schedule
 * editing UI can drive both.
 *
 * `val`, not `var`: the Realm entities this mirrors are mutable because Realm requires
 * it, but domain state is immutable and updated with `copy()`.
 */
interface ScheduleItem {
    val id: String
    /** `"HH:mm"`. */
    val startTime: String
    /** `"HH:mm"`. */
    val endTime: String
    /** Comma-separated weekday names, e.g. `"Monday,Wednesday"`. */
    val activeDays: String
    /** Comma-separated ISO dates, e.g. `"2026-06-18,2026-06-19"`. */
    val specificDates: String
    /** Creation timestamp, used only for stable ordering. */
    val createdAt: Long
}
