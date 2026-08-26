package com.groovernapp.mosqueathan.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * The month's name in Indonesian.
 *
 * One copy. This mapping existed three times — in `DashboardViewModel`, in the settings
 * schedule list via `SimpleDateFormat("MMMM yyyy", Locale("id"))`, and again in the web
 * panel's JavaScript as a literal array. `SimpleDateFormat` is also JVM-only, and relies
 * on the platform actually shipping an `id` locale.
 */
fun Month.toIndonesian(): String = when (this) {
    Month.JANUARY -> "Januari"
    Month.FEBRUARY -> "Februari"
    Month.MARCH -> "Maret"
    Month.APRIL -> "April"
    Month.MAY -> "Mei"
    Month.JUNE -> "Juni"
    Month.JULY -> "Juli"
    Month.AUGUST -> "Agustus"
    Month.SEPTEMBER -> "September"
    Month.OCTOBER -> "Oktober"
    Month.NOVEMBER -> "November"
    Month.DECEMBER -> "Desember"
}

/** `"Agustus 2026"`, for grouping a schedule list by month. */
fun monthAndYear(month: Int, year: Int): String =
    "${Month(month).toIndonesian()} $year"

/*
 * Material's `DatePicker` speaks epoch milliseconds at UTC midnight; this app stores
 * dates as `"yyyy-MM-dd"`. These two convert between them.
 *
 * They exist because the conversion was written twice, with `java.util.Calendar`, in two
 * files that both show a date picker — and getting it subtly wrong shifts a scheduled
 * date by a day, which on a Friday schedule means the Friday layout appears on Thursday.
 */

/** `"2026-08-26"` to the epoch millis Material's date picker expects, or null. */
fun isoDateToUtcMillis(iso: String): Long? {
    val parts = iso.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return try {
        LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    } catch (e: IllegalArgumentException) {
        null
    }
}

/** The reverse: what the picker hands back, as `"2026-08-26"`. */
fun utcMillisToIsoDate(millis: Long): String {
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
    return "${date.year}-${date.month.number.pad(2)}-${date.day.pad(2)}"
}
