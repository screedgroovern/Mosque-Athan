package com.groovernapp.mosqueathan.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The only way into settings on a television. Worth testing directly, because when
 * it fails it presents as "the remote is broken" and gives no other signal.
 */
class SecretSequenceTest {

    private val up = 19
    private val down = 20
    private val enter = 66

    private fun sequence() = SecretSequence(listOf(up, up, down, down))

    private fun SecretSequence.feed(vararg keys: Int): Boolean =
        keys.map { accept(it) }.last()

    @Test
    fun theCorrectSequenceOpensSettings() {
        assertTrue(sequence().feed(up, up, down, down))
    }

    @Test
    fun itOnlyFiresOnTheLastKey() {
        val s = sequence()
        assertFalse(s.accept(up))
        assertFalse(s.accept(up))
        assertFalse(s.accept(down))
        assertTrue(s.accept(down))
    }

    @Test
    fun aWrongOrderDoesNotOpenSettings() {
        assertFalse(sequence().feed(up, down, up, down))
        assertFalse(sequence().feed(down, down, up, up))
    }

    @Test
    fun anUnrelatedKeyResetsProgress() {
        val s = sequence()
        s.accept(up)
        s.accept(up)
        s.accept(enter)          // user did something else
        assertFalse(s.feed(down, down))
    }

    @Test
    fun theWindowSlidesSoAFalseStartStillSucceeds() {
        // Pressing down first, then the sequence, should work: the extra press
        // falls out of the window rather than poisoning it.
        val s = sequence()
        assertFalse(s.accept(down))
        assertTrue(s.feed(up, up, down, down))
    }

    @Test
    fun itFiresAgainAfterSucceedingOnce() {
        // Leaving settings and coming back must work without restarting the app.
        val s = sequence()
        assertTrue(s.feed(up, up, down, down))
        assertTrue(s.feed(up, up, down, down))
    }

    @Test
    fun repeatedPressesDoNotFireEarly() {
        val s = sequence()
        assertFalse(s.feed(up, up, up))
        // ...but the sliding window means the last two ups can still start a run.
        assertTrue(s.feed(down, down))
    }

    @Test
    fun resetClearsProgress() {
        val s = sequence()
        s.accept(up)
        s.accept(up)
        s.reset()
        assertFalse(s.feed(down, down))
    }

    @Test
    fun containsIsTrueOnlyForKeysTheSequenceIsMadeOf() {
        val s = sequence()
        assertTrue(up in s)
        assertTrue(down in s)
        assertFalse(enter in s)
    }

    /**
     * A phone swallows the first D-pad press before `onKeyDown`, so it is counted earlier in
     * `dispatchKeyEvent`; the ledger is what stops it being counted a second time if it arrives
     * in `onKeyDown` too. Modelled here as the two callbacks `MainActivity` has.
     */
    private class Pipeline(private val s: SecretSequence) {
        private val ledger = PressLedger()

        /** `dispatchKeyEvent`: counts in touch mode only. */
        fun dispatch(key: Int, time: Long, touchMode: Boolean): Boolean {
            if (touchMode && key in s) {
                ledger.record(key, time)
                return s.accept(key)
            }
            return false
        }

        /** `onKeyDown`: counts unless the press was already counted. */
        fun onKeyDown(key: Int, time: Long): Boolean =
            if (ledger.has(key, time)) false else s.accept(key)
    }

    @Test
    fun aSwallowedFirstPressIsStillCounted() {
        // Touch mode: press 1 is counted in dispatch and never reaches onKeyDown.
        // Presses 2-4 are in non-touch mode and reach onKeyDown only.
        val p = Pipeline(sequence())
        assertFalse(p.dispatch(up, 1, touchMode = true))
        assertFalse(p.dispatch(up, 2, touchMode = false) || p.onKeyDown(up, 2))
        assertFalse(p.dispatch(down, 3, touchMode = false) || p.onKeyDown(down, 3))
        assertTrue(p.dispatch(down, 4, touchMode = false) || p.onKeyDown(down, 4))
    }

    @Test
    fun aPressCountedEarlyIsNotCountedAgainWhenItAlsoReachesOnKeyDown() {
        // A single UP followed by DOWN DOWN must not open settings. If the one press were
        // counted in both callbacks it would read as UP UP DOWN DOWN and open it.
        val p = Pipeline(sequence())
        assertFalse(p.dispatch(up, 1, touchMode = true))
        assertFalse(p.onKeyDown(up, 1))
        assertFalse(p.onKeyDown(down, 2))
        assertFalse(p.onKeyDown(down, 3))
    }

    @Test
    fun withoutTouchModeNothingIsCountedEarlyAndOnKeyDownIsUnchanged() {
        // The television: dispatch never records, so the ledger never matches and onKeyDown
        // counts every press as it always did.
        val p = Pipeline(sequence())
        assertFalse(p.dispatch(up, 1, touchMode = false))
        assertFalse(p.onKeyDown(up, 1))
        assertFalse(p.onKeyDown(up, 2))
        assertFalse(p.onKeyDown(down, 3))
        assertTrue(p.onKeyDown(down, 4))
    }

    @Test
    fun theLedgerMatchesOnlyTheExactPress() {
        val ledger = PressLedger()
        assertFalse(ledger.has(up, 0))
        ledger.record(up, 100)
        assertTrue(ledger.has(up, 100))
        assertFalse(ledger.has(down, 100))
        assertFalse(ledger.has(up, 101))   // a held key's repeat is a later event
    }
}
