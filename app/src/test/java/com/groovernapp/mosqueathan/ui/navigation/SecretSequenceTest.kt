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
}
