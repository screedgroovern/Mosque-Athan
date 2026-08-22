package com.groovernapp.mosqueathan.prayer

import com.groovernapp.mosqueathan.util.PrayerUtils
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Schedule activation decides when standby blanks the display and when the
 * slideshow runs. A mistake here means the screen is dark during a prayer, so the
 * overnight cases matter more than the ordinary ones.
 *
 * `PrayerUtils.countdown` is not covered: it reads `Clock.System.now()` internally,
 * so it cannot be tested without a fixed clock being passed in. That is a
 * refactor, not a test.
 */
class PrayerUtilsTest {

    // 2026-06-05 is a Friday.
    private fun at(hour: Int, minute: Int, day: Int = 5) =
        LocalDateTime(2026, 6, day, hour, minute, 0)

    // --- formatTime ---------------------------------------------------------

    @Test
    fun formatTimePadsBothFields() {
        assertEquals("04:07", PrayerUtils.formatTime(at(4, 7)))
        assertEquals("17:48", PrayerUtils.formatTime(at(17, 48)))
        assertEquals("00:00", PrayerUtils.formatTime(at(0, 0)))
    }

    // --- parseTimeToMinutes -------------------------------------------------

    @Test
    fun parseTimeToMinutesAcceptsWellFormedTimes() {
        assertEquals(0, PrayerUtils.parseTimeToMinutes("00:00"))
        assertEquals(4 * 60 + 37, PrayerUtils.parseTimeToMinutes("04:37"))
        assertEquals(23 * 60 + 59, PrayerUtils.parseTimeToMinutes("23:59"))
    }

    @Test
    fun parseTimeToMinutesToleratesSurroundingSpace() {
        assertEquals(5 * 60 + 30, PrayerUtils.parseTimeToMinutes("  05 : 30 "))
    }

    @Test
    fun parseTimeToMinutesRejectsMalformedInput() {
        assertNull(PrayerUtils.parseTimeToMinutes(""))
        assertNull(PrayerUtils.parseTimeToMinutes("0530"))
        assertNull(PrayerUtils.parseTimeToMinutes("05:30:00"))
        assertNull(PrayerUtils.parseTimeToMinutes("ab:cd"))
    }

    // --- isScheduleActive, same-day ranges ----------------------------------

    @Test
    fun sameDayRangeIsHalfOpen() {
        // 08:00 until 17:00 on a Friday
        fun active(h: Int, m: Int) =
            PrayerUtils.isScheduleActive(at(h, m), "FRIDAY", false, "08:00", "17:00")

        assertFalse(active(7, 59), "a minute before the start")
        assertTrue(active(8, 0), "the start minute is inside")
        assertTrue(active(16, 59))
        assertFalse(active(17, 0), "the end minute is outside")
    }

    @Test
    fun dayNameIsMatchedCaseInsensitively() {
        assertTrue(PrayerUtils.isScheduleActive(at(10, 0), "friday", false, "08:00", "17:00"))
        assertTrue(PrayerUtils.isScheduleActive(at(10, 0), "Friday", false, "08:00", "17:00"))
        assertFalse(PrayerUtils.isScheduleActive(at(10, 0), "THURSDAY", false, "08:00", "17:00"))
    }

    // --- isScheduleActive, overnight ranges --------------------------------
    // This is the standby case: 22:00 to 04:00.

    @Test
    fun overnightRangeCoversBothSidesOfMidnight() {
        fun active(h: Int, m: Int) =
            PrayerUtils.isScheduleActive(at(h, m), "FRIDAY", false, "22:00", "04:00")

        assertFalse(active(21, 59))
        assertTrue(active(22, 0), "just after the start")
        assertTrue(active(23, 59), "before midnight")
        assertTrue(active(0, 0), "midnight itself")
        assertTrue(active(3, 59), "just before the end")
        assertFalse(active(4, 0), "the end minute is outside")
        assertFalse(active(12, 0), "the middle of the day")
    }

    @Test
    fun specificDateMustMatchExactly() {
        // isSpecificDate compares against yyyy-MM-dd, zero-padded
        assertTrue(PrayerUtils.isScheduleActive(at(10, 0), "2026-06-05", true, "08:00", "17:00"))
        assertFalse(PrayerUtils.isScheduleActive(at(10, 0), "2026-06-06", true, "08:00", "17:00"))
        assertFalse(PrayerUtils.isScheduleActive(at(10, 0), "2026-6-5", true, "08:00", "17:00"),
            "an unpadded date is a different string")
    }

    @Test
    fun emptyFieldsAreNeverActive() {
        assertFalse(PrayerUtils.isScheduleActive(at(10, 0), "", false, "08:00", "17:00"))
        assertFalse(PrayerUtils.isScheduleActive(at(10, 0), "FRIDAY", false, "", "17:00"))
        assertFalse(PrayerUtils.isScheduleActive(at(10, 0), "FRIDAY", false, "08:00", ""))
    }

    // --- isScheduleActiveNew ----------------------------------------------

    @Test
    fun multipleActiveDaysAreHonoured() {
        fun active(day: Int) = PrayerUtils.isScheduleActiveNew(
            now = at(10, 0, day = day),
            activeDays = "MONDAY,FRIDAY,SUNDAY",
            specificDates = "",
            startTime = "08:00",
            endTime = "17:00"
        )
        assertTrue(active(5), "Friday is listed")
        assertTrue(active(1), "Monday is listed")
        assertFalse(active(6), "Saturday is not")
    }

    @Test
    fun activeDaysToleratesSpacingAndCase() {
        assertTrue(
            PrayerUtils.isScheduleActiveNew(
                now = at(10, 0),
                activeDays = " monday , friday ",
                specificDates = "",
                startTime = "08:00",
                endTime = "17:00"
            )
        )
    }

    @Test
    fun specificDatesTakePrecedenceOverActiveDays() {
        // specificDates is checked first, so a non-matching date wins even when the
        // weekday would have matched.
        assertFalse(
            PrayerUtils.isScheduleActiveNew(
                now = at(10, 0),
                activeDays = "FRIDAY",
                specificDates = "2026-12-25",
                startTime = "08:00",
                endTime = "17:00"
            )
        )
        assertTrue(
            PrayerUtils.isScheduleActiveNew(
                now = at(10, 0),
                activeDays = "",
                specificDates = "2026-01-01,2026-06-05",
                startTime = "08:00",
                endTime = "17:00"
            )
        )
    }

    @Test
    fun timeIsCheckedBeforeAnyDayMatching() {
        // Right day, wrong time.
        assertFalse(
            PrayerUtils.isScheduleActiveNew(
                now = at(20, 0),
                activeDays = "FRIDAY",
                specificDates = "",
                startTime = "08:00",
                endTime = "17:00"
            )
        )
    }

    @Test
    fun overnightRangeStillWorksThroughTheNewOverload() {
        fun active(h: Int) = PrayerUtils.isScheduleActiveNew(
            now = at(h, 30),
            activeDays = "FRIDAY",
            specificDates = "",
            startTime = "22:00",
            endTime = "04:00"
        )
        assertTrue(active(22))
        assertTrue(active(2), "after midnight, though the weekday is still Friday here")
        assertFalse(active(12))
    }

    @Test
    fun fallsBackToTheLegacySingleDayField() {
        assertTrue(
            PrayerUtils.isScheduleActiveNew(
                now = at(10, 0),
                activeDays = "",
                specificDates = "",
                startTime = "08:00",
                endTime = "17:00",
                legacyDay = "FRIDAY"
            )
        )
        assertFalse(
            PrayerUtils.isScheduleActiveNew(
                now = at(10, 0),
                activeDays = "",
                specificDates = "",
                startTime = "08:00",
                endTime = "17:00",
                legacyDay = "MONDAY"
            )
        )
    }

    @Test
    fun withNoDayInformationAtAllNothingIsActive() {
        assertFalse(
            PrayerUtils.isScheduleActiveNew(
                now = at(10, 0),
                activeDays = "",
                specificDates = "",
                startTime = "08:00",
                endTime = "17:00"
            )
        )
    }
}
