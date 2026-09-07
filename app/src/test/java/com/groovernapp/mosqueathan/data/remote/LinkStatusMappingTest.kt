package com.groovernapp.mosqueathan.data.remote

import com.groovernapp.mosqueathan.ui.design.LinkStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every transport status the Android controller can be in has a name the UI understands.
 *
 * The mapping is a `when` over an enum, so adding a status is a compile error — but *removing* the
 * `Unknown → Unknown` line is not, and neither is collapsing `NeedsApproval` into `Offline`. Both
 * are silent and both matter: `NeedsApproval` is the one state reconnecting cannot fix, and it is
 * what draws the PIN gate. Map it to `Offline` and the takmir sees "no display reachable" forever
 * while the display sits there waiting for a PIN.
 */
class LinkStatusMappingTest {

    @Test
    fun everyTransportStatusMapsToItsOwnUiStatus() {
        val mapped = RemoteActionSender.Status.entries.associateWith { it.toUiStatus() }

        assertEquals(LinkStatus.Unknown, mapped[RemoteActionSender.Status.Unknown])
        assertEquals(LinkStatus.Connected, mapped[RemoteActionSender.Status.Connected])
        assertEquals(LinkStatus.NeedsApproval, mapped[RemoteActionSender.Status.NeedsApproval])
        assertEquals(LinkStatus.Offline, mapped[RemoteActionSender.Status.Offline])
    }

    @Test
    fun noTwoTransportStatusesCollapseOntoOneUiStatus() {
        val mapped = RemoteActionSender.Status.entries.map { it.toUiStatus() }

        // Collapsing any pair loses a distinction the UI acts on, and the dangerous one is
        // NeedsApproval — the state where reconnecting is not the answer and a person is.
        assertEquals(
            "each transport status must keep its own meaning: $mapped",
            mapped.size,
            mapped.toSet().size
        )
    }

    @Test
    fun unknownRendersNothingRatherThanSomethingReassuring() {
        // A display's sender never talks to anybody, so it stays Unknown — and Unknown is the one
        // LinkStatus that draws no chip at all. A green light meaning "we did not check" is the
        // thing LinkStatus was written to avoid.
        assertTrue(RemoteActionSender.Status.Unknown.toUiStatus() == LinkStatus.Unknown)
    }
}
