package com.groovernapp.mosqueathan.domain

/**
 * A single day of pre-fetched Kemenag prayer times, as plain data.
 *
 * The persisted form is `data.KemenagScheduleEntity` (a Realm object, Android-only).
 * Prayer-time calculation must not see that type, so it works with this instead.
 * Times are `"HH:mm"`; an empty string means "not supplied".
 */
data class KemenagSchedule(
    val dateString: String,
    val cityId: String,
    val imsak: String,
    val subuh: String,
    val terbit: String,
    val dhuha: String,
    val dzuhur: String,
    val ashar: String,
    val maghrib: String,
    val isya: String
)

/**
 * Where [KemenagSchedule]s come from.
 *
 * This is the seam that keeps `PrayerCalculator` pure: previously it reached for
 * `MosqueConfigRepository.getInstance()` itself, which tied prayer-time maths to
 * Realm and made the Kemenag path untestable without a database.
 */
fun interface KemenagScheduleSource {
    fun scheduleFor(cityId: String, dateString: String): KemenagSchedule?
}
