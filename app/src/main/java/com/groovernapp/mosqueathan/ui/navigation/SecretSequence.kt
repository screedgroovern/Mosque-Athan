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

    /** Whether [keyCode] is one the sequence is made of. */
    operator fun contains(keyCode: Int): Boolean = keyCode in expected

    fun reset() = history.clear()
}

/**
 * Remembers the one key press already counted before it reached `onKeyDown`, so it is not
 * counted twice.
 *
 * On a phone the first D-pad press after a touch is consumed leaving touch input mode, and
 * `Activity.onKeyDown` — the only place the sequence was counted — never sees it. UP UP DOWN
 * DOWN therefore accumulated three entries and settings did not open until it was pressed twice.
 * `MainActivity.dispatchKeyEvent` runs before the view hierarchy and sees every press, so it
 * counts that one; this ledger tells `onKeyDown` it has been counted, in case it arrives there
 * as well.
 *
 * Nothing is ever recorded outside touch mode, which a television is never in, so for a press
 * that was not counted early [has] is false and `onKeyDown` behaves exactly as it always did.
 * A press is identified by key and event time: a held key's repeats carry later times, so they
 * are still counted one by one, as before.
 */
class PressLedger {
    private var keyCode = NONE
    private var eventTime = NONE.toLong()

    fun record(keyCode: Int, eventTime: Long) {
        this.keyCode = keyCode
        this.eventTime = eventTime
    }

    fun has(keyCode: Int, eventTime: Long): Boolean =
        keyCode == this.keyCode && eventTime == this.eventTime

    private companion object {
        const val NONE = -1
    }
}
