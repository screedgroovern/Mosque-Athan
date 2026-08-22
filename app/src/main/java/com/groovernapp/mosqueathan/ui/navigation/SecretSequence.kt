package com.groovernapp.mosqueathan.ui.navigation

/**
 * The D-pad sequence that opens settings on a television.
 *
 * Extracted from `MainActivity` so it can be tested. It could not be before, and
 * the bug it now guards against was invisible without a device: during the standby
 * window the sequence never accumulated a single entry, so settings were
 * unreachable for the six hours a day standby is scheduled.
 */
class SecretSequence(private val expected: List<Int>) {

    private val history = mutableListOf<Int>()

    /** Records a key and reports whether the sequence has just completed. */
    fun accept(keyCode: Int): Boolean {
        if (keyCode !in expected) {
            // A key outside the sequence means the user is doing something else.
            history.clear()
            return false
        }
        history.add(keyCode)
        if (history.size > expected.size) history.removeAt(0)
        if (history == expected) {
            history.clear()
            return true
        }
        return false
    }

    fun reset() = history.clear()
}
