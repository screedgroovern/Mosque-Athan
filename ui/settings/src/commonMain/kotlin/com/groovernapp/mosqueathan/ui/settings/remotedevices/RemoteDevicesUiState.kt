package com.groovernapp.mosqueathan.ui.settings.remotedevices

import com.groovernapp.mosqueathan.domain.ConnectedDevice

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
    val devices: List<ConnectedDevice> = emptyList()
) {
    val isServer: Boolean get() = mode == ConnectivityMode.Server
}

/** Everything the pane can ask for. */
sealed interface RemoteDevicesEvent {
    data class ModeSelected(val mode: ConnectivityMode) : RemoteDevicesEvent
    data class ShowSettingsOnTvToggled(val show: Boolean) : RemoteDevicesEvent
    data class RemoteTvIpEntered(val ip: String) : RemoteDevicesEvent
    data class PinCodeEntered(val pin: String) : RemoteDevicesEvent
    data class DeviceMadeMaster(val deviceId: String) : RemoteDevicesEvent
    data class DeviceRemoved(val deviceId: String) : RemoteDevicesEvent
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
