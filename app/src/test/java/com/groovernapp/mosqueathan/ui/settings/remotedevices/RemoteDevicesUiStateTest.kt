package com.groovernapp.mosqueathan.ui.settings.remotedevices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The state and mode mapping. Testable at all only because they are plain data
 * rather than eleven parameters threaded through a composable.
 */
class RemoteDevicesUiStateTest {

    @Test
    fun modeMapsToAndFromItsWireValue() {
        assertEquals(ConnectivityMode.Server, ConnectivityMode.fromWire("SERVER"))
        assertEquals(ConnectivityMode.Remote, ConnectivityMode.fromWire("REMOTE"))
        assertEquals("SERVER", ConnectivityMode.Server.wireValue)
        assertEquals("REMOTE", ConnectivityMode.Remote.wireValue)
    }

    @Test
    fun anUnrecognisedWireValueFallsBackToServer() {
        // A config written by a newer version, or a blank field on a fresh install.
        // Falling back to Remote would silently stop a display showing prayer times.
        assertEquals(ConnectivityMode.Server, ConnectivityMode.fromWire(""))
        assertEquals(ConnectivityMode.Server, ConnectivityMode.fromWire("something-else"))
    }

    @Test
    fun everyModeRoundTripsThroughItsWireValue() {
        ConnectivityMode.entries.forEach {
            assertEquals(it, ConnectivityMode.fromWire(it.wireValue))
        }
    }

    @Test
    fun isServerFollowsTheMode() {
        assertTrue(RemoteDevicesUiState(mode = ConnectivityMode.Server).isServer)
        assertFalse(RemoteDevicesUiState(mode = ConnectivityMode.Remote).isServer)
    }

    @Test
    fun theDefaultStateIsAServerWithNothingConnected() {
        val state = RemoteDevicesUiState()
        assertTrue(state.isServer)
        assertEquals("", state.remoteTvIp)
        assertEquals("", state.pinCode)
        assertTrue(state.devices.isEmpty())
        assertFalse(state.showSettingsOnTv)
    }
}
