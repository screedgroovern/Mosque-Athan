package com.groovernapp.mosqueathan.ui.settings.remotedevices

import com.groovernapp.mosqueathan.domain.ConnectedDevice
import com.groovernapp.mosqueathan.domain.KnownServer

/** Whether this device drives the display or controls another one. */
enum class ConnectivityMode(val wireValue: String) {
    Server("SERVER"),
    Remote("REMOTE");

    companion object {
        fun fromWire(value: String): ConnectivityMode =
            entries.firstOrNull { it.wireValue == value } ?: Server
    }
}

/**
 * Everything the Remote & Devices pane draws.
 *
 * Replaces eleven separate parameters. The pane took `config: MosqueConfig`
 * and used four of its fifty-five fields, so it recomposed on every unrelated
 * setting change; this state carries only those four.
 */
data class RemoteDevicesUiState(
    val mode: ConnectivityMode = ConnectivityMode.Server,
    val showSettingsOnTv: Boolean = false,
    val remoteTvIp: String = "",
    val pinCode: String = "",
    val devices: List<ConnectedDevice> = emptyList(),
    /** Displays this device has connected to, each with whether it answered the last probe. */
    val servers: List<ServerEntry> = emptyList()
) {
    val isServer: Boolean get() = mode == ConnectivityMode.Server

    /** Controllers attached to this display right now; what switching away from Server would cut off. */
    val controllersOnline: Int get() = devices.count { it.isOnline }

    /**
     * Whether leaving the current mode drops something live, so the switch is worth a second
     * thought. A server with nobody attached and a remote that was never pointed at a display lose
     * nothing, and asking would only teach people to dismiss the dialog.
     */
    val switchingModeDropsConnection: Boolean
        get() = if (isServer) controllersOnline > 0 else remoteTvIp.isNotBlank()
}

/** A remembered display and whether it is on: null until the first probe has answered. */
data class ServerEntry(val server: KnownServer, val online: Boolean?)

/** Everything the pane can ask for. */
sealed interface RemoteDevicesEvent {
    data class ModeSelected(val mode: ConnectivityMode) : RemoteDevicesEvent
    data class ShowSettingsOnTvToggled(val show: Boolean) : RemoteDevicesEvent
    data class RemoteTvIpEntered(val ip: String) : RemoteDevicesEvent
    data class PinCodeEntered(val pin: String) : RemoteDevicesEvent
    data class DeviceMadeMaster(val deviceId: String) : RemoteDevicesEvent
    data class DeviceRemoved(val deviceId: String) : RemoteDevicesEvent

    /** Drops a remembered display from the list; the display itself is not touched. */
    data class ServerForgotten(val host: String) : RemoteDevicesEvent
    /**
     * Sync with the display, optionally at an [address] the user has just supplied.
     *
     * The address is carried rather than read back from state because Connect and the QR scanner
     * both save it and sync in the same handler — and the saved value has not reached the state
     * flow yet at that moment. Without this, the first press after changing the address syncs
     * against the *previous* display, and a freshly scanned QR syncs against nothing at all.
     */
    data class SyncRequested(val address: String? = null) : RemoteDevicesEvent
}
