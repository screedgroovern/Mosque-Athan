package com.groovernapp.mosqueathan.ui.settings.remotedevices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.groovernapp.mosqueathan.data.ConnectedDeviceEntity
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.ui.settings.SettingsCard
import com.groovernapp.mosqueathan.util.Lang

@Composable
fun RemoteDevicesSettingsPane(
    config: MosqueConfigEntity,
    isTv: Boolean,
    onUpdateShowSettingsOnTv: (Boolean) -> Unit,
    devices: List<ConnectedDeviceEntity>,
    onSetDeviceMaster: (String) -> Unit,
    onDeleteDevice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SettingsCard(
            title = Lang.get("Web Control Server", "Server Web Remote"),
            icon = Icons.Default.Dns
        ) {
            WebControlSettingsSegment(
                showSettingsOnTv = config.showSettingsOnTv,
                isTv = isTv,
                onUpdateShowSettingsOnTv = onUpdateShowSettingsOnTv
            )
        }

        SettingsCard(
            title = Lang.get("Connected Devices", "Perangkat Terhubung"),
            icon = Icons.Default.Devices
        ) {
            DevicesSettingsSegment(
                config = config,
                devices = devices,
                onSetDeviceMaster = onSetDeviceMaster,
                onDeleteDevice = onDeleteDevice
            )
        }
    }
}
