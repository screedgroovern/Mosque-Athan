package com.groovernapp.mosqueathan.data.room

import com.groovernapp.mosqueathan.domain.KemenagSchedule
import com.groovernapp.mosqueathan.domain.KemenagScheduleSource

/**
 * Adapts the Room repository to the [KemenagScheduleSource] that `PrayerCalculator` consumes.
 *
 * A one-line delegation, and the same shape as `RealmKemenagScheduleSource` on purpose: the seam
 * exists so the calculator never learns where schedules are stored, and swapping the store should
 * not be visible from here.
 *
 * The lookup is synchronous because `scheduleFor` is, and `PrayerCalculator` is not a suspending
 * function. `RoomConfigRepository` answers it from the cache it keeps of the configured city's
 * days, so this is a map read rather than a database round trip on the display's own clock.
 */
class RoomKemenagScheduleSource(
    private val repository: RoomConfigRepository
) : KemenagScheduleSource {

    override fun scheduleFor(cityId: String, dateString: String): KemenagSchedule? =
        repository.getKemenagSchedule(cityId, dateString)
}
