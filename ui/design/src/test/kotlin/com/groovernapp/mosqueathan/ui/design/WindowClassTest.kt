package com.groovernapp.mosqueathan.ui.design

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `windowClassFor` decides every adaptive choice in the app, so its boundaries are
 * worth pinning down — particularly that a television is never classified by width.
 */
class WindowClassTest {

    @Test
    fun phoneWidthsAreCompact() {
        assertEquals(WindowClass.Compact, windowClassFor(320.dp, isTv = false))
        assertEquals(WindowClass.Compact, windowClassFor(599.dp, isTv = false))
    }

    @Test
    fun theCompactBoundaryIsExclusive() {
        assertEquals(WindowClass.Compact, windowClassFor(599.dp, isTv = false))
        assertEquals(WindowClass.Medium, windowClassFor(600.dp, isTv = false))
    }

    @Test
    fun tabletPortraitWidthsAreMedium() {
        assertEquals(WindowClass.Medium, windowClassFor(600.dp, isTv = false))
        assertEquals(WindowClass.Medium, windowClassFor(839.dp, isTv = false))
    }

    @Test
    fun theMediumBoundaryIsExclusive() {
        assertEquals(WindowClass.Medium, windowClassFor(839.dp, isTv = false))
        assertEquals(WindowClass.Expanded, windowClassFor(840.dp, isTv = false))
    }

    @Test
    fun largeWidthsAreExpanded() {
        assertEquals(WindowClass.Expanded, windowClassFor(840.dp, isTv = false))
        assertEquals(WindowClass.Expanded, windowClassFor(1920.dp, isTv = false))
    }

    /**
     * The reason `Tv` exists as its own class: a 15-inch monitor and a 43-inch
     * television can both report 1920px, and viewing distance is what actually
     * differs. If this ever regresses, TV type silently shrinks to desktop size.
     */
    @Test
    fun aTelevisionIsTvAtEveryWidth() {
        assertEquals(WindowClass.Tv, windowClassFor(320.dp, isTv = true))
        assertEquals(WindowClass.Tv, windowClassFor(600.dp, isTv = true))
        assertEquals(WindowClass.Tv, windowClassFor(840.dp, isTv = true))
        assertEquals(WindowClass.Tv, windowClassFor(1920.dp, isTv = true))
        assertEquals(WindowClass.Tv, windowClassFor(3840.dp, isTv = true))
    }

    @Test
    fun aTelevisionIsNeverCompact() {
        // The bug this guards: two of the three old isCompact computations omitted
        // the TV check, so a narrow television would have taken the phone layout.
        assertFalse(windowClassFor(400.dp, isTv = true).isCompact)
        assertTrue(windowClassFor(400.dp, isTv = true).isTv)
    }

    @Test
    fun onlyCompactIsCompact() {
        assertTrue(WindowClass.Compact.isCompact)
        assertFalse(WindowClass.Medium.isCompact)
        assertFalse(WindowClass.Expanded.isCompact)
        assertFalse(WindowClass.Tv.isCompact)
    }

    @Test
    fun onlyTvIsTv() {
        assertTrue(WindowClass.Tv.isTv)
        assertFalse(WindowClass.Compact.isTv)
        assertFalse(WindowClass.Medium.isTv)
        assertFalse(WindowClass.Expanded.isTv)
    }

    @Test
    fun zeroWidthDoesNotThrow() {
        assertEquals(WindowClass.Compact, windowClassFor(0.dp, isTv = false))
    }
}
