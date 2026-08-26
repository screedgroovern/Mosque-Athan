package com.groovernapp.mosqueathan.util

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `pad` behaves like `%02d`.
 *
 * This matters more than its size suggests: it formats the clock on the mosque's display,
 * and it took that job from `"%02d:%02d:%02d".format(...)` — which resolves to
 * `java.lang.String.format` and does not exist outside the JVM. The wasm target is what
 * surfaced that; these assertions are what stop the replacement drifting from it.
 *
 * Expected values are written as literals rather than compared against `format`, because
 * `format` is exactly what cannot be called from `commonMain`.
 */
class PaddingTest {

    @Test
    fun everyClockValuePadsAsPercentZeroTwoDidWould() {
        assertEquals("00", 0.pad(2))
        assertEquals("07", 7.pad(2))
        assertEquals("09", 9.pad(2))
        assertEquals("10", 10.pad(2))
        assertEquals("23", 23.pad(2))
        assertEquals("59", 59.pad(2))
    }

    @Test
    fun aValueWiderThanThePadIsPrintedInFull() {
        // `%02d` does not truncate, and neither may this — a three-digit countdown of
        // minutes must not become two digits.
        assertEquals("100", 100.pad(2))
        assertEquals("1440", 1440.pad(2))
    }

    @Test
    fun widthsOtherThanTwoWork() {
        assertEquals("0007", 7.pad(4))
        assertEquals("7", 7.pad(1))
        assertEquals("7", 7.pad(0))
    }

    @Test
    fun longsPadTheSameWay() {
        assertEquals("00", 0L.pad(2))
        assertEquals("09", 9L.pad(2))
        assertEquals("9876543210", 9876543210L.pad(2))
    }

    @Test
    fun theClockStringItselfComesOutRight() {
        // The exact composition the display screens build, at the values most likely to
        // be formatted wrongly: midnight, a single-digit minute, and a single-digit second.
        listOf(
            Triple(0, 0, 0) to "00:00:00",
            Triple(1, 2, 3) to "01:02:03",
            Triple(9, 5, 0) to "09:05:00",
            Triple(16, 26, 7) to "16:26:07",
            Triple(23, 59, 59) to "23:59:59"
        ).forEach { (t, expected) ->
            assertEquals(expected, "${t.first.pad(2)}:${t.second.pad(2)}:${t.third.pad(2)}")
        }
    }

    @Test
    fun negativesAreNotPaddedIntoNonsense() {
        // Documented rather than relied on: no caller passes a negative, and `%02d` would
        // have produced "-1" here too, so this records that the two agree even off the
        // path that is actually used.
        assertEquals("-1", (-1).pad(2))
    }
}
