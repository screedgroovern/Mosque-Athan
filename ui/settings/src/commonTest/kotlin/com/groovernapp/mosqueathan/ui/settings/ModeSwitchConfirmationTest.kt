package com.groovernapp.mosqueathan.ui.settings

import com.groovernapp.mosqueathan.domain.ConnectedDevice
import com.groovernapp.mosqueathan.ui.settings.remotedevices.ConnectivityMode
import com.groovernapp.mosqueathan.ui.settings.remotedevices.RemoteDevicesUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * When switching between Server and Remote is worth a confirmation dialog.
 *
 * The dialog is only honest if it appears when something live would be dropped. Asked every time,
 * it becomes a button people dismiss without reading — and then it is gone on the one occasion it
 * mattered. So the rule is pinned: a server asks only when a controller is attached right now, and
 * a remote asks only when it has been pointed at a display.
 */
class ModeSwitchConfirmationTest {

    private fun device(online: Boolean) = ConnectedDevice(deviceId = "d", isOnline = online)

    @Test
    fun serverWithNobodyAttachedSwitchesWithoutAsking() {
        val state = RemoteDevicesUiState(mode = ConnectivityMode.Server, devices = listOf(device(online = false)))

        assertFalse(state.switchingModeDropsConnection, "an offline device has nothing live to cut off")
    }

    @Test
    fun serverWithAnOnlineControllerAsksFirst() {
        val state = RemoteDevicesUiState(
            mode = ConnectivityMode.Server,
            devices = listOf(device(online = true), device(online = false))
        )

        assertTrue(state.switchingModeDropsConnection)
        assertEquals(1, state.controllersOnline)
    }

    @Test
    fun remoteThatWasNeverPointedAtADisplaySwitchesWithoutAsking() {
        assertFalse(RemoteDevicesUiState(mode = ConnectivityMode.Remote, remoteTvIp = "").switchingModeDropsConnection)
        assertFalse(RemoteDevicesUiState(mode = ConnectivityMode.Remote, remoteTvIp = "  ").switchingModeDropsConnection)
    }

    @Test
    fun remoteWithADisplayAddressAsksFirst() {
        assertTrue(
            RemoteDevicesUiState(mode = ConnectivityMode.Remote, remoteTvIp = "192.168.1.3").switchingModeDropsConnection
        )
    }

    @Test
    fun anOnlineControllerDoesNotMakeARemoteAsk() {
        // devices describe controllers of *this* device as a server; as a remote it has none that count.
        val state = RemoteDevicesUiState(
            mode = ConnectivityMode.Remote,
            remoteTvIp = "",
            devices = listOf(device(online = true))
        )

        assertFalse(state.switchingModeDropsConnection)
    }
}
