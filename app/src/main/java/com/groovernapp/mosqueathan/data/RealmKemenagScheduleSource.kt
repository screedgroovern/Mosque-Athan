package com.groovernapp.mosqueathan.data

import com.groovernapp.mosqueathan.domain.KemenagSchedule
import com.groovernapp.mosqueathan.domain.KemenagScheduleSource

/**
 * Adapts the Realm-backed repository to the platform-agnostic
 * [KemenagScheduleSource] that `PrayerCalculator` consumes.
 *
 * This is the only place the Realm entity is converted for calculation, which is
 * what keeps `:core:prayer` free of any persistence dependency.
 */
class RealmKemenagScheduleSource(
    private val repository: MosqueConfigRepository
) : KemenagScheduleSource {

    override fun scheduleFor(cityId: String, dateString: String): KemenagSchedule? =
        repository.getKemenagSchedule(cityId, dateString)?.toDomain()
}

private fun KemenagScheduleEntity.toDomain() = KemenagSchedule(
    dateString = dateString,
    cityId = cityId,
    imsak = imsak,
    subuh = subuh,
    terbit = terbit,
    dhuha = dhuha,
    dzuhur = dzuhur,
    ashar = ashar,
    maghrib = maghrib,
    isya = isya
)
