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
import com.groovernapp.mosqueathan.data.ConnectedDeviceEntity
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun DevicesSettingsSegment(
    config: com.groovernapp.mosqueathan.data.MosqueConfigEntity,
    devices: List<ConnectedDeviceEntity>,
    onSetDeviceMaster: (String) -> Unit,
    onDeleteDevice: (String) -> Unit
) {
    var showTransferMasterDialog by remember { mutableStateOf<ConnectedDeviceEntity?>(null) }
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
                    if (config.pinCode.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            com.groovernapp.mosqueathan.util.Lang.get("Enter current PIN to confirm:", "Masukkan PIN saat ini untuk konfirmasi:"),
                            color = MosqueTheme.colors.accent,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = transferPinInput,
                            onValueChange = { transferPinInput = it },
                            placeholder = { Text(com.groovernapp.mosqueathan.util.Lang.get("Security PIN", "PIN Keamanan")) },
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (pinErrorMsg != null) {
                            Text(pinErrorMsg!!, color = Color.Red, fontSize = MosqueTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                TvFocusablePrimaryButton(
                    onClick = {
                        if (config.pinCode.isEmpty() || transferPinInput == config.pinCode) {
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
                TextButton(onClick = {
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
            text = "Daftar Perangkat terdaftar / terhubung",
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
            val sortedDevices = remember(devices) {
                devices.sortedWith(
                    compareByDescending<ConnectedDeviceEntity> { it.isOnline }
                        .thenByDescending { it.lastSeen }
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sortedDevices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MosqueTheme.radius.md))
                            .background(MosqueTheme.colors.fillSubtle)
                            .padding(horizontal = MosqueTheme.spacing.xl, vertical = MosqueTheme.spacing.lg),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (device.isOnline) Color.Green else Color.Gray)
                            )
                            Column {
                                Text(
                                    text = device.deviceName,
                                    color = MosqueTheme.colors.textPrimary,
                                    fontSize = MosqueTheme.typography.bodyCompact,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (device.isOnline) "Terhubung" else "Terputus",
                                    color = MosqueTheme.colors.textSecondary,
                                    fontSize = MosqueTheme.typography.caption
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (device.isMaster) {
                                Text(
                                    text = "Master",
                                    color = MosqueTheme.colors.accent,
                                    fontSize = MosqueTheme.typography.label,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = MosqueTheme.spacing.md)
                                )
                            } else {
                                TvFocusableButton(
                                    onClick = {
                                        if (hasMaster) {
                                            showTransferMasterDialog = device
                                        } else {
                                            onSetDeviceMaster(device.deviceId)
                                        }
                                    },
                                    shape = RoundedCornerShape(MosqueTheme.radius.sm),
                                    contentPadding = PaddingValues(horizontal = MosqueTheme.spacing.lg, vertical = MosqueTheme.spacing.xs),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Jadikan Master", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.caption)
                                }
                            }

                            if (!device.isMaster) {
                                TvFocusableDangerButton(
                                    onClick = { onDeleteDevice(device.deviceId) },
                                    shape = RoundedCornerShape(MosqueTheme.radius.sm),
                                    contentPadding = PaddingValues(horizontal = MosqueTheme.spacing.lg, vertical = MosqueTheme.spacing.xs),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Hapus", color = MosqueTheme.colors.textPrimary, fontSize = MosqueTheme.typography.caption)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
