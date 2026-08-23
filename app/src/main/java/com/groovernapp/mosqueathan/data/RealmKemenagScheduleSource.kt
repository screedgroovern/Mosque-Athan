package com.groovernapp.mosqueathan.data

import com.groovernapp.mosqueathan.domain.KemenagSchedule
import com.groovernapp.mosqueathan.domain.KemenagScheduleSource

/**
 * Adapts the repository to the platform-agnostic [KemenagScheduleSource] that
 * `PrayerCalculator` consumes.
 *
 * Reduced to a one-line delegation now that the repository maps to domain types at its
 * own boundary — it used to carry its own private entity-to-domain conversion, which
 * was the first instance of a mapping that now exists once, in `EntityMappers`.
 */
class RealmKemenagScheduleSource(
    private val repository: MosqueConfigRepository
) : KemenagScheduleSource {

    override fun scheduleFor(cityId: String, dateString: String): KemenagSchedule? =
        repository.getKemenagSchedule(cityId, dateString)
}
