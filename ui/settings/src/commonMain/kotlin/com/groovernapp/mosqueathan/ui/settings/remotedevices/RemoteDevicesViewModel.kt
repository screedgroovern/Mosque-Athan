package com.groovernapp.mosqueathan.ui.settings.remotedevices

import com.groovernapp.mosqueathan.protocol.RemoteActions
import kotlinx.serialization.json.put
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.domain.KnownServersStore
import com.groovernapp.mosqueathan.protocol.HttpFetcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import com.groovernapp.mosqueathan.protocol.RemoteActionSink
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
    private val repository: ConfigRepository,
    private val remote: RemoteActionSink,
    private val knownServers: KnownServersStore,
    private val http: HttpFetcher
) : ViewModel() {

    /**
     * Which remembered displays answer, refreshed while someone is looking at the list.
     *
     * Part of [uiState], so it runs only while that has a collector — the probe loop stops with the
     * pane instead of polling every address in the background for as long as the app is open. Each
     * round asks every address at once, so a list of five takes as long as the slowest, not the sum.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val reachability: Flow<Map<String, Boolean>> =
        knownServers.servers.flatMapLatest { list ->
            flow {
                while (true) {
                    emit(
                        coroutineScope {
                            list.map { server ->
                                async { server.host to http.isReachable("http://${server.host}:8080/") }
                            }.awaitAll().toMap()
                        }
                    )
                    delay(PROBE_INTERVAL_MS)
                }
            }
        }

    init {
        // A controller that was already pointed at a display before this list existed would
        // otherwise start with an empty one, as if it had never connected anywhere. Seeded once,
        // from the address it already holds, and only while the list is empty.
        viewModelScope.launch {
            val config = repository.getConfig()
            val address = config.remoteTvIp.trim()
            if (config.connectivityMode == "REMOTE" && address.isNotEmpty() && knownServers.servers.value.isEmpty()) {
                knownServers.remember(address, "")
            }
        }
    }

    val uiState: StateFlow<RemoteDevicesUiState> =
        combine(
            repository.getConfigFlow(),
            repository.getConnectedDevicesFlow(),
            knownServers.servers,
            reachability.onStart { emit(emptyMap()) }
        ) { config, devices, servers, reachable ->
            RemoteDevicesUiState(
                mode = ConnectivityMode.fromWire(config.connectivityMode),
                showSettingsOnTv = config.showSettingsOnTv,
                remoteTvIp = config.remoteTvIp,
                pinCode = config.pinCode,
                devices = devices,
                servers = servers.map { ServerEntry(it, reachable[it.host]) }
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

            is RemoteDevicesEvent.ServerForgotten -> knownServers.forget(event.host)

            is RemoteDevicesEvent.SyncRequested -> Unit // handled at the call site
        }
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        /** Often enough that "online" is believable, rare enough not to keep the radio busy. */
        const val PROBE_INTERVAL_MS = 10_000L
    }
}
