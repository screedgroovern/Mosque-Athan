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
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = com.groovernapp.mosqueathan.util.Lang.get(
                            "WARNING: Master access privileges will be transferred to '${targetDevice.deviceName}'. Once transferred, you can no longer change the PIN or assign Master from this device.",
                            "PERINGATAN: Hak akses Master akan dialihkan ke '${targetDevice.deviceName}'. Setelah dipindahkan, Anda tidak dapat lagi mengubah PIN atau menunjuk Master dari perangkat ini."
                        ),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    if (config.pinCode.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            com.groovernapp.mosqueathan.util.Lang.get("Enter current PIN to confirm:", "Masukkan PIN saat ini untuk konfirmasi:"),
                            color = Color(0xFFFFD54F),
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
                            Text(pinErrorMsg!!, color = Color.Red, fontSize = 13.sp)
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
                        color = Color.Black,
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
                    Text(com.groovernapp.mosqueathan.util.Lang.get("Cancel", "Batal"), color = Color.White)
                }
            },
            containerColor = Color(0xFF101915),
            shape = RoundedCornerShape(12.dp)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Daftar Perangkat terdaftar / terhubung",
            color = Color(0xFFFFD54F),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (devices.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Belum ada perangkat yang terhubung.",
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            val sortedDevices = remember(devices) {
                devices.sortedWith(
                    compareByDescending<ConnectedDeviceEntity> { it.isOnline }
                        .thenByDescending { it.lastSeen }
                )
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(sortedDevices) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (device.isOnline) "Terhubung" else "Terputus",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (device.isMaster) {
                                Text(
                                    text = "Master",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
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
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Jadikan Master", color = Color.White, fontSize = 12.sp)
                                }
                            }

                            if (!device.isMaster) {
                                TvFocusableDangerButton(
                                    onClick = { onDeleteDevice(device.deviceId) },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Hapus", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
