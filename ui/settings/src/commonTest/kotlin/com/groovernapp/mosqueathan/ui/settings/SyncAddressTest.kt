package com.groovernapp.mosqueathan.ui.settings

import com.groovernapp.mosqueathan.ui.settings.remotedevices.RemoteDevicesEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The sync request carries the address the user just supplied.
 *
 * Measured on the Realme: pressing Connect after typing a new TV address stored the address
 * correctly and then synced against the *old* one, because both happen in one click handler and
 * the repository write has not travelled back through the config flow at that moment. The worse
 * case is the QR scanner — a brand-new controller has no stored address at all, so the sync
 * returned early and the takmir was told "Terhubung ke TV via QR" with nothing having happened.
 *
 * There is no ViewModel here on purpose: `syncWithTvServer` needs an `HttpFetcher` and a
 * repository, and what went wrong was never the fetch. It was which string the fetch was given.
 */
class SyncAddressTest {

    @Test
    fun connectAndTheQrScannerNameTheAddressTheyJustSaved() {
        assertEquals(
            "192.168.1.128",
            RemoteDevicesEvent.SyncRequested("192.168.1.128").address,
            "Connect and the QR scanner must pass what the user supplied, not read it back"
        )
    }

    @Test
    fun syncNowMeansTheDisplayAlreadyConnectedTo() {
        // Null is not an omission: this button has no address of its own to offer, and the stored
        // one is the right answer for it.
        assertNull(RemoteDevicesEvent.SyncRequested().address)
    }
}
