package com.groovernapp.mosqueathan.ui.settings.remotedevices

import com.groovernapp.mosqueathan.ui.settings.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.domain.ConnectedDevice
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import androidx.compose.runtime.Composable
import com.groovernapp.mosqueathan.util.Lang
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.component.MosquePrimaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueDangerButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueSecondaryButton
import com.groovernapp.mosqueathan.ui.design.component.mosqueTextFieldColors
import com.groovernapp.mosqueathan.ui.design.component.MosqueTextButton
import com.groovernapp.mosqueathan.ui.design.initialFocus

@Composable
fun DevicesSettingsSegment(
    state: RemoteDevicesUiState,
    onEvent: (RemoteDevicesEvent) -> Unit
) {
    val devices = state.devices
    val onSetDeviceMaster: (String) -> Unit = { onEvent(RemoteDevicesEvent.DeviceMadeMaster(it)) }
    val onDeleteDevice: (String) -> Unit = { onEvent(RemoteDevicesEvent.DeviceRemoved(it)) }
    var showTransferMasterDialog by remember { mutableStateOf<ConnectedDevice?>(null) }
    var transferPinInput by remember { mutableStateOf("") }
    var pinErrorMsg by remember { mutableStateOf<String?>(null) }

    val hasMaster = devices.any { it.isMaster }

    // Transfer Master Dialog
    showTransferMasterDialog?.let { targetDevice ->
        AlertDialog(
            onDismissRequest = {
                showTransferMasterDialog = null
                transferPinInput = ""
                pinErrorMsg = null
            },
            title = {
                Text(
                    com.groovernapp.mosqueathan.util.Lang.get("Transfer Master Access?", "Pindahkan Hak Master?"),
                    color = MosqueTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)) {
                    Text(
                        text = com.groovernapp.mosqueathan.util.Lang.get(
                            "WARNING: Master access privileges will be transferred to '${targetDevice.deviceName}'. Once transferred, you can no longer change the PIN or assign Master from this device.",
                            "PERINGATAN: Hak akses Master akan dialihkan ke '${targetDevice.deviceName}'. Setelah dipindahkan, Anda tidak dapat lagi mengubah PIN atau menunjuk Master dari perangkat ini."
                        ),
                        color = MosqueTheme.colors.textPrimary
                    )
                    if (state.pinCode.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(MosqueTheme.spacing.md))
                        Text(
                            com.groovernapp.mosqueathan.util.Lang.get("Enter current PIN to confirm:", "Masukkan PIN saat ini untuk konfirmasi:"),
                            color = MosqueTheme.colors.accent,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = transferPinInput,
                            onValueChange = { transferPinInput = it },
                            placeholder = { Text(com.groovernapp.mosqueathan.util.Lang.get("Security PIN", "PIN Keamanan")) },
                            colors = mosqueTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (pinErrorMsg != null) {
                            Text(pinErrorMsg!!, color = MosqueTheme.colors.danger, fontSize = MosqueTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                MosquePrimaryButton(
                    modifier = Modifier.initialFocus(),
                    onClick = {
                        if (state.pinCode.isEmpty() || transferPinInput == state.pinCode) {
                            onSetDeviceMaster(targetDevice.deviceId)
                            showTransferMasterDialog = null
                            transferPinInput = ""
                            pinErrorMsg = null
                        } else {
                            pinErrorMsg = com.groovernapp.mosqueathan.util.Lang.get(
                                "Incorrect PIN. Transfer cancelled.",
                                "PIN salah. Pengalihan dibatalkan."
                            )
                        }
                    }
                ) {
                    Text(
                        com.groovernapp.mosqueathan.util.Lang.get("Confirm", "Konfirmasi"),
                        color = MosqueTheme.colors.onAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                MosqueTextButton(onClick = {
                    showTransferMasterDialog = null
                    transferPinInput = ""
                    pinErrorMsg = null
                }) {
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Cancel", "Batal"), color = MosqueTheme.colors.textPrimary)
                }
            },
            containerColor = MosqueTheme.colors.surface,
            shape = RoundedCornerShape(MosqueTheme.radius.lg)
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = Lang.get("Registered / connected devices", "Daftar Perangkat terdaftar / terhubung"),
            color = MosqueTheme.colors.accent,
            fontSize = MosqueTheme.typography.bodyCompact,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = MosqueTheme.spacing.md)
        )

        if (devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = MosqueTheme.spacing.huge),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Belum ada perangkat yang terhubung.",
                    color = MosqueTheme.colors.textMuted
                )
            }
        } else {
            val isCompact = LocalWindowClass.current.isCompact
    val sortedDevices = remember(devices) {
                devices.sortedWith(
                    compareByDescending<ConnectedDevice> { it.isOnline }
                        .thenByDescending { it.lastSeen }
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sortedDevices.forEach { device ->
                    // Stacked on a phone, side by side otherwise.
                    //
                    // The two action buttons do not shrink, so in one Row they took the width their
                    // labels needed and left the name column about two characters wide — "MacOS
                    // Device" rendered as M / ac / O / S / D / ev / ic / e, one fragment per line.
                    // Only the compact branch is new; a television and a tablet lay out exactly as
                    // before.
                    if (isCompact) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(MosqueTheme.radius.md))
                                .background(MosqueTheme.colors.fillSubtle)
                                .padding(
                                    horizontal = MosqueTheme.spacing.lg,
                                    vertical = MosqueTheme.spacing.lg
                                ),
                            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                        ) {
                            DeviceIdentity(device, modifier = Modifier.fillMaxWidth())
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DeviceActions(
                                    device = device,
                                    hasMaster = hasMaster,
                                    onRequestTransfer = { showTransferMasterDialog = device },
                                    onSetDeviceMaster = onSetDeviceMaster,
                                    onDeleteDevice = onDeleteDevice
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(MosqueTheme.radius.md))
                                .background(MosqueTheme.colors.fillSubtle)
                                .padding(
                                    horizontal = MosqueTheme.spacing.xl,
                                    vertical = MosqueTheme.spacing.lg
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DeviceIdentity(device, modifier = Modifier.weight(1f))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DeviceActions(
                                    device = device,
                                    hasMaster = hasMaster,
                                    onRequestTransfer = { showTransferMasterDialog = device },
                                    onSetDeviceMaster = onSetDeviceMaster,
                                    onDeleteDevice = onDeleteDevice
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The dot, the name and the connection state — the part that identifies a paired device. */
@Composable
private fun DeviceIdentity(device: ConnectedDevice, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    if (device.isOnline) MosqueTheme.colors.success else MosqueTheme.colors.textMuted
                )
        )
        Column {
            Text(
                text = device.deviceName,
                color = MosqueTheme.colors.textPrimary,
                fontSize = MosqueTheme.typography.bodyCompact,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (device.isOnline) {
                    Lang.get("Connected", "Terhubung")
                } else {
                    Lang.get("Disconnected", "Terputus")
                },
                color = MosqueTheme.colors.textSecondary,
                fontSize = MosqueTheme.typography.caption
            )
        }
    }
}

/** The master badge, or the two actions available on a device that is not the master. */
@Composable
private fun DeviceActions(
    device: ConnectedDevice,
    hasMaster: Boolean,
    onRequestTransfer: () -> Unit,
    onSetDeviceMaster: (String) -> Unit,
    onDeleteDevice: (String) -> Unit
) {
    if (device.isMaster) {
        Text(
            text = Lang.get("Master", "Master"),
            color = MosqueTheme.colors.accent,
            fontSize = MosqueTheme.typography.label,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = MosqueTheme.spacing.md)
        )
        return
    }

    MosqueSecondaryButton(
        onClick = { if (hasMaster) onRequestTransfer() else onSetDeviceMaster(device.deviceId) },
        shape = RoundedCornerShape(MosqueTheme.radius.sm),
        contentPadding = PaddingValues(
            horizontal = MosqueTheme.spacing.lg,
            vertical = MosqueTheme.spacing.xs
        ),
        modifier = Modifier.heightIn(min = MosqueTheme.size.controlSm)
    ) {
        Text(
            text = Lang.get("Make Master", "Jadikan Master"),
            color = MosqueTheme.colors.textPrimary,
            fontSize = MosqueTheme.typography.caption
        )
    }
    MosqueDangerButton(
        onClick = { onDeleteDevice(device.deviceId) },
        shape = RoundedCornerShape(MosqueTheme.radius.sm),
        contentPadding = PaddingValues(
            horizontal = MosqueTheme.spacing.lg,
            vertical = MosqueTheme.spacing.xs
        ),
        modifier = Modifier.heightIn(min = MosqueTheme.size.controlSm)
    ) {
        Text(
            text = Lang.get("Delete", "Hapus"),
            color = MosqueTheme.colors.textPrimary,
            fontSize = MosqueTheme.typography.caption
        )
    }
}
