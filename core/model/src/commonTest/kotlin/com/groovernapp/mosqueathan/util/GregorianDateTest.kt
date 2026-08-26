package com.groovernapp.mosqueathan.util

import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Date conversion for the settings date pickers.
 *
 * Worth testing rather than eyeballing: this replaces two hand-written
 * `java.util.Calendar` conversions, and getting the boundary wrong shifts a scheduled
 * date by a day. On a Friday schedule that means the Friday layout appears on Thursday.
 */
class GregorianDateTest {

    @Test
    fun everyMonthHasAnIndonesianName() {
        assertEquals("Januari", Month.JANUARY.toIndonesian())
        assertEquals("Agustus", Month.AUGUST.toIndonesian())
        assertEquals("Desember", Month.DECEMBER.toIndonesian())
        // Exhaustive, so a `when` that stops being exhaustive fails here rather than
        // silently returning something odd.
        assertEquals(12, Month.entries.map { it.toIndonesian() }.distinct().size)
    }

    @Test
    fun monthAndYearReadsAsTheScheduleListShowsIt() {
        assertEquals("Agustus 2026", monthAndYear(8, 2026))
        assertEquals("Januari 2027", monthAndYear(1, 2027))
    }

    @Test
    fun anIsoDateRoundTripsThroughMillis() {
        listOf("2026-08-26", "2026-01-01", "2026-12-31", "2024-02-29").forEach { iso ->
            val millis = isoDateToUtcMillis(iso)
            assertEquals(iso, millis?.let { utcMillisToIsoDate(it) }, "round trip of $iso")
        }
    }

    @Test
    fun millisIsUtcMidnightAndNotLocalMidnight() {
        // The picker speaks UTC. Using the device zone here is the bug that shifts a date
        // by a day for anyone east or west of UTC — which is everyone this app serves.
        assertEquals(0L, isoDateToUtcMillis("1970-01-01"))
        assertEquals(86_400_000L, isoDateToUtcMillis("1970-01-02"))
    }

    @Test
    fun singleDigitMonthsAndDaysArePadded() {
        assertEquals("2026-01-05", utcMillisToIsoDate(isoDateToUtcMillis("2026-01-05")!!))
    }

    @Test
    fun malformedInputGivesNullRatherThanThrowing() {
        listOf("", "2026", "2026-08", "not-a-date", "2026-13-01", "2026-02-30", "a-b-c")
            .forEach { assertNull(isoDateToUtcMillis(it), "for: $it") }
    }
}
