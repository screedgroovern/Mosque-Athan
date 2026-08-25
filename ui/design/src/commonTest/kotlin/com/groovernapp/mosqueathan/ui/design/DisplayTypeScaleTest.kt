package com.groovernapp.mosqueathan.ui.design

import androidx.compose.ui.unit.dp
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

/**
 * Display type shrinks on a short window.
 *
 * The bug this fixes was reported from use, not found by reading: on a phone in
 * landscape the clock filled the screen and the prayer times row — the reason the
 * display exists — was pushed out of view.
 */
class DisplayTypeScaleTest {

    // Measured: Xiaomi 25040RP0AG 711dp tall, Realme RMX2061 360dp tall.
    private val tablet = displayTypeScaleFor(711.dp)
    private val phone = displayTypeScaleFor(360.dp)
    private val tv = displayTypeScaleFor(720.dp)

    @Test
    fun aTallWindowGetsTheUnscaledSizes() {
        // Not "approximately equal" — identical. Anything else means the TV and the
        // tablet render differently than they shipped.
        assertEquals(MosqueTypography.clockDisplay, tv.clockDisplay)
        assertEquals(MosqueTypography.clockDisplay, tablet.clockDisplay)
        assertEquals(MosqueTypography.iqomahCountdown, tablet.iqomahCountdown)
        assertEquals(MosqueTypography.prayerName, tablet.prayerName)
    }

    @Test
    fun theBoundaryIsExactlyAtSixHundred() {
        assertEquals(MosqueTypography.clockDisplay, displayTypeScaleFor(600.dp).clockDisplay)
        assertTrue(displayTypeScaleFor(599.dp).clockDisplay.value < MosqueTypography.clockDisplay.value)
    }

    @Test
    fun aShortWindowShrinksTheClockEnoughToLeaveRoom() {
        val clock = phone.clockDisplay.value
        assertTrue(clock < 170f, "clock should shrink from 170sp, saw $clock")
        // The four lines of ClockSection came to roughly 250dp before, in a 360dp
        // window. Everything below was pushed off screen.
        val block = clock + phone.clockMedium.value + phone.prayerNameCompact.value * 2
        assertTrue(block < 200f, "the clock block must leave room for the prayer times, saw $block")
    }

    @Test
    fun settingsSizesNeverShrink() {
        // A phone is held closer, not further away — shrinking body text there makes
        // it harder to read, not easier.
        listOf(
            MosqueTypography.titleLarge to phone.titleLarge,
            MosqueTypography.body to phone.body,
            MosqueTypography.label to phone.label,
            MosqueTypography.captionSmall to phone.captionSmall
        ).forEach { (base, scaled) -> assertEquals(base, scaled) }
    }

    @Test
    fun everyDisplayRoleScalesTogether() {
        val factor = phone.clockDisplay.value / MosqueTypography.clockDisplay.value
        listOf(
            MosqueTypography.iqomahCountdown to phone.iqomahCountdown,
            MosqueTypography.adzanTitle to phone.adzanTitle,
            MosqueTypography.clockLarge to phone.clockLarge,
            MosqueTypography.headline to phone.headline,
            MosqueTypography.prayerName to phone.prayerName
        ).forEach { (base, scaled) ->
            assertEquals(base.value * factor, scaled.value, 0.01f)
        }
    }

    @Test
    fun anAbsurdlyShortWindowStillLeavesReadableType() {
        // A floor rather than an asymptote: a clock nobody can read from the back of
        // the room is no better than one that overflows.
        val tiny = displayTypeScaleFor(80.dp)
        assertTrue(tiny.clockDisplay.value >= MosqueTypography.clockDisplay.value * 0.45f - 0.01f)
    }

    @Test
    fun aTelevisionIsNeverScaled() {
        // The mosque's display is an AQUA/Haier Android TV: 1920x1080 at density 320,
        // so 540dp tall — below the 600dp knee. It had been rendering at 0.9 since the
        // short-window fix, because the code assumed a television was always taller than
        // the knee and nobody had measured one.
        assertEquals(1f, displayScaleFor(540.dp, isTv = true))
        assertEquals(MosqueTypography.clockDisplay, displayTypeScaleFor(540.dp, isTv = true).clockDisplay)
    }

    @Test
    fun aTelevisionIsUnscaledEvenAtAbsurdlyShortHeights() {
        // A television is read from the back of a room. There is no height at which
        // shrinking its type is the right answer, so this holds unconditionally rather
        // than down to some threshold.
        listOf(540, 480, 360, 200).forEach { h ->
            assertEquals(1f, displayScaleFor(h.dp, isTv = true), "TV at ${h}dp")
        }
    }

    @Test
    fun aNonTelevisionOfTheSameHeightStillScales() {
        // Guards the obvious mistake of making the knee unconditional and thereby
        // undoing the phone fix.
        assertEquals(540f / 600f, displayScaleFor(540.dp, isTv = false))
        assertTrue(displayScaleFor(360.dp, isTv = false) < 1f)
    }
}
