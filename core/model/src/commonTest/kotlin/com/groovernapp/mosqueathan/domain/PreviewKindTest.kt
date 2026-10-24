package com.groovernapp.mosqueathan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The preview a display reports travels as a string, so the mapping both ways is the contract.
 * A controller that cannot read what the display sent would leave a Stop button up for a preview
 * that was over, or never show one for a preview that was running.
 */
class PreviewKindTest {

    @Test
    fun everyKindRoundTripsThroughItsWireValue() {
        PreviewKind.entries.forEach { kind ->
            assertEquals(kind, PreviewKind.fromWire(kind.wire), "${kind.name} did not survive the wire")
        }
    }

    @Test
    fun wireValuesAreDistinct() {
        assertEquals(PreviewKind.entries.size, PreviewKind.entries.map { it.wire }.toSet().size)
    }

    @Test
    fun noPreviewAndAnUnknownOneBothReadAsNone() {
        assertNull(PreviewKind.fromWire(""), "an empty string is how a display says nothing is previewing")
        assertNull(PreviewKind.fromWire("hologram"), "a kind from a newer display must not crash an older controller")
        assertNull(PreviewKind.fromWire(null))
    }
}
