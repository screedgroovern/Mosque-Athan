package com.groovernapp.mosqueathan.ui.settings.remotedevices

import com.groovernapp.mosqueathan.protocol.RemoteActions
import kotlinx.serialization.json.put
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import com.groovernapp.mosqueathan.data.remote.RemoteActionSender
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The Remote & Devices category.
 *
 * First of the per-category ViewModels. `SettingsViewModel` is 1028 lines with
 * eleven flows and covers every category at once; this owns one category and
 * exposes one state.
 *
 * [RemoteDevicesEvent.SyncRequested] is deliberately **not** handled here. Syncing
 * still runs through `SettingsViewModel.syncWithTvServer`, which takes an Android
 * `Context`, so the call stays at the UI edge where a Context legitimately exists.
 * Moving it needs the Context dependency removed first.
 */
class RemoteDevicesViewModel(
    private val repository: MosqueConfigRepository,
    private val remote: RemoteActionSender
) : ViewModel() {

    val uiState: StateFlow<RemoteDevicesUiState> =
        combine(
            repository.getConfigFlow(),
            repository.getConnectedDevicesFlow()
        ) { config, devices ->
            RemoteDevicesUiState(
                mode = ConnectivityMode.fromWire(config.connectivityMode),
                showSettingsOnTv = config.showSettingsOnTv,
                remoteTvIp = config.remoteTvIp,
                pinCode = config.pinCode,
                devices = devices
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RemoteDevicesUiState()
        )

    fun onEvent(event: RemoteDevicesEvent) {
        when (event) {
            is RemoteDevicesEvent.ModeSelected ->
                write { repository.updateConnectivityMode(event.mode.wireValue) }

            is RemoteDevicesEvent.ShowSettingsOnTvToggled -> {
                write { repository.updateShowSettingsOnTv(event.show) }
                remote.send(RemoteActions.UPDATE_CONFIG) { put("showSettingsOnTv", event.show) }
            }

            is RemoteDevicesEvent.RemoteTvIpEntered ->
                write { repository.updateRemoteTvIp(event.ip) }

            is RemoteDevicesEvent.PinCodeEntered -> {
                write { repository.updatePinCode(event.pin, changerDeviceId = null) }
                // SET_PIN, not "update_pin": the server has no branch for the latter,
                // so a controller changing the access PIN silently changed nothing.
                remote.send(RemoteActions.SET_PIN) { put("pin", event.pin) }
            }

            is RemoteDevicesEvent.DeviceMadeMaster -> {
                write { repository.setDeviceMaster(event.deviceId) }
                remote.send(RemoteActions.SET_MASTER) { put("deviceId", event.deviceId) }
            }

            is RemoteDevicesEvent.DeviceRemoved -> {
                write { repository.deleteDevice(event.deviceId) }
                remote.send(RemoteActions.DELETE_DEVICE) { put("deviceId", event.deviceId) }
            }

            RemoteDevicesEvent.SyncRequested -> Unit // handled at the call site
        }
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
