package com.groovernapp.mosqueathan.ui.settings.remotedevices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.groovernapp.mosqueathan.protocol.TvPairing
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import com.groovernapp.mosqueathan.ui.design.MessageHost
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.component.MosquePrimaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueSecondaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueTertiaryButton
import com.groovernapp.mosqueathan.ui.design.component.mosqueTextFieldColors
import com.groovernapp.mosqueathan.ui.design.qr.rememberQrScanner
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.flow.MutableSharedFlow

@Composable
fun RemoteControlSettingsSegment(
    state: RemoteDevicesUiState,
    onEvent: (RemoteDevicesEvent) -> Unit
) {
    val isCompact = LocalWindowClass.current.isCompact
    val onUpdateRemoteTvIp: (String) -> Unit = { onEvent(RemoteDevicesEvent.RemoteTvIpEntered(it)) }
    val onUpdatePinCode: (String) -> Unit = { onEvent(RemoteDevicesEvent.PinCodeEntered(it)) }
    val onSyncWithTv: () -> Unit = { onEvent(RemoteDevicesEvent.SyncRequested) }
    var ipText by remember(state.remoteTvIp) { mutableStateOf(state.remoteTvIp) }
    var pinText by remember { mutableStateOf("") }

    // The pane's own transient feedback, through the MessageHost seam rather than a
    // Toast: a Toast needs a Context, and that Context was the last Android type in this
    // file. `tryEmit` so a click handler stays a plain lambda; a dropped message when
    // two arrive in the same frame is the right trade for that.
    //
    // Owned here rather than by RemoteDevicesViewModel deliberately. Every message below
    // answers a press this composable validated itself ("the IP is empty", "connecting
    // to X"), while the *outcome* of a sync already arrives on
    // SettingsViewModel.messages. Moving these onto the category ViewModel means moving
    // the validation with them, and SyncRequested is routed to SettingsViewModel at the
    // SettingsScreen call site — so the pane would have to raise two events per press
    // and split one validation across two files.
    val messages = remember { MutableSharedFlow<String>(extraBufferCapacity = 1) }
    fun say(message: String) {
        messages.tryEmit(message)
    }

    val scanner = rememberQrScanner(
        prompt = Lang.get(
            "Point the camera at the QR code on the TV",
            "Arahkan kamera ke QR Code TV"
        )
    )
    // One scanner for both layouts' buttons, which is what a callback passed at launch
    // buys: the compact and wide branches below call the same instance.
    // Null where no camera can be reached — the browser, permanently, because getUserMedia needs a
    // secure context and this panel is served over plain http on a LAN. The control is then not
    // drawn: a "Scan QR" button that cannot open a camera is the same defect as a Back button that
    // goes nowhere.
    val onScanQr: (() -> Unit)? = scanner?.let { ready ->
        {
            ready.launch { payload ->
                val address = TvPairing.addressFrom(payload)
                if (address == null) {
                    // The payload used to be stored raw when it did not parse, so scanning a
                    // notice board set the TV's address to the notice board's text.
                    say(
                        Lang.get(
                            "That QR code does not carry a display address.",
                            "Kode QR itu tidak memuat alamat layar."
                        )
                    )
                } else {
                    onUpdateRemoteTvIp(address)
                    say(
                        Lang.get(
                            "Connected to the TV via QR: $address",
                            "Terhubung ke TV via QR: $address"
                        )
                    )
                    onSyncWithTv()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isCompact) 0.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xxxl)
    ) {
        // Section 1: Sambungkan & Sinkronisasi TV (Sinkronisasi)
        Column(
            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = Lang.get("Sync", "Sinkronisasi"),
                color = MosqueTheme.colors.accent,
                fontSize = MosqueTheme.typography.body,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = Lang.get("Connect to the Android TV to sync schedule, adzan and display settings in real time.", "Hubungkan ke Android TV untuk sinkronisasi setelan jadwal, adzan, dan tampilan media secara realtime."),
                color = MosqueTheme.colors.textSecondary,
                fontSize = MosqueTheme.typography.labelSmall
            )

            OutlinedTextField(
                value = ipText,
                onValueChange = { ipText = it },
                label = { Text(Lang.get("TV IP address", "IP Address TV")) },
                placeholder = { Text("Contoh: 192.168.1.15") },
                singleLine = true,
                colors = mosqueTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            if (isCompact) {
                // Stack vertically on compact screens
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MosquePrimaryButton(
                        onClick = {
                            if (ipText.trim().isEmpty()) {
                                say(Lang.get("The TV IP address cannot be empty.", "IP TV tidak boleh kosong!"))
                            } else {
                                onUpdateRemoteTvIp(ipText.trim())
                                say(Lang.get("Connecting to the TV at ${ipText.trim()}\u2026", "Menghubungkan ke TV ${ipText.trim()}..."))
                                onSyncWithTv()
                            }
                        },
                        shape = RoundedCornerShape(MosqueTheme.radius.md),
                        modifier = Modifier.fillMaxWidth().height(MosqueTheme.size.controlLg)
                    ) {
                        Icon(imageVector = Icons.Default.Tv, contentDescription = null)
                        Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
                        Text(Lang.get("Connect", "Hubungkan"), fontWeight = FontWeight.Bold)
                    }

                    if (onScanQr != null) MosqueSecondaryButton(
                        onClick = onScanQr,
                        shape = RoundedCornerShape(MosqueTheme.radius.md),
                        modifier = Modifier.fillMaxWidth().height(MosqueTheme.size.controlLg)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
                        Text(Lang.get("Scan QR", "Pindai QR"), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Side-by-side on larger screens
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg)
                ) {
                    MosquePrimaryButton(
                        onClick = {
                            if (ipText.trim().isEmpty()) {
                                say(Lang.get("The TV IP address cannot be empty.", "IP TV tidak boleh kosong!"))
                            } else {
                                onUpdateRemoteTvIp(ipText.trim())
                                say(Lang.get("Connecting to the TV at ${ipText.trim()}\u2026", "Menghubungkan ke TV ${ipText.trim()}..."))
                                onSyncWithTv()
                            }
                        },
                        shape = RoundedCornerShape(MosqueTheme.radius.md),
                        modifier = Modifier.weight(1f).height(MosqueTheme.size.controlLg)
                    ) {
                        Icon(imageVector = Icons.Default.Tv, contentDescription = null)
                        Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
                        Text(Lang.get("Connect", "Hubungkan"), fontWeight = FontWeight.Bold)
                    }

                    if (onScanQr != null) MosqueSecondaryButton(
                        onClick = onScanQr,
                        shape = RoundedCornerShape(MosqueTheme.radius.md),
                        modifier = Modifier.weight(1f).height(MosqueTheme.size.controlLg)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
                        Text(Lang.get("Scan QR", "Pindai QR"), fontWeight = FontWeight.Bold)
                    }
                }
            }

            MosqueTertiaryButton(
                onClick = {
                    if (state.remoteTvIp.isEmpty()) {
                        say(Lang.get("Connect to the TV first.", "Hubungkan ke TV terlebih dahulu!"))
                    } else {
                        say(Lang.get("Syncing with the TV\u2026", "Menyinkronkan data dengan TV..."))
                        onSyncWithTv()
                    }
                },
                shape = RoundedCornerShape(MosqueTheme.radius.md),
                modifier = Modifier.fillMaxWidth().height(MosqueTheme.size.controlLg)
            ) {
                Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = MosqueTheme.colors.accent)
                Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
                Text(Lang.get("Sync now", "Sinkronkan Sekarang"), fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = MosqueTheme.colors.fill, thickness = MosqueTheme.stroke.hairline)

        // Section 2: PIN Remote Kontrol
        Column(
            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = Lang.get("Remote control PIN", "PIN Remote Kontrol"),
                color = MosqueTheme.colors.accent,
                fontSize = MosqueTheme.typography.body,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = Lang.get("Require a PIN so other remote controls cannot change settings.", "Gunakan PIN keamanan untuk membatasi perangkat remote control lain agar tidak dapat mengubah setelan sembarangan."),
                color = MosqueTheme.colors.textSecondary,
                fontSize = MosqueTheme.typography.labelSmall
            )

            val hasPin = state.pinCode.isNotEmpty()
            val badgeBg = if (hasPin) MosqueTheme.colors.success.copy(alpha = 0.15f) else MosqueTheme.colors.danger.copy(alpha = 0.15f)
            val badgeColor = if (hasPin) MosqueTheme.colors.success else MosqueTheme.colors.danger
            val badgeText = if (hasPin) Lang.get("ON (PIN REQUIRED)", "AKTIF (MEMAKAI PIN)") else Lang.get("OFF", "TIDAK AKTIF")

            val statusBadgeContent = @Composable {
                Text(
                    text = Lang.get("PIN status:", "Status PIN Keamanan:"),
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.label
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(MosqueTheme.radius.xs))
                        .background(badgeBg)
                        .padding(horizontal = MosqueTheme.spacing.md, vertical = MosqueTheme.spacing.xs)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = MosqueTheme.typography.captionSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (isCompact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MosqueTheme.radius.md))
                        .background(MosqueTheme.colors.fillSubtle)
                        .padding(horizontal = MosqueTheme.spacing.xl, vertical = MosqueTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md),
                    horizontalAlignment = Alignment.Start
                ) {
                    statusBadgeContent()
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MosqueTheme.radius.md))
                        .background(MosqueTheme.colors.fillSubtle)
                        .padding(horizontal = MosqueTheme.spacing.xl, vertical = MosqueTheme.spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    statusBadgeContent()
                }
            }

            OutlinedTextField(
                value = pinText,
                onValueChange = { pinText = it },
                label = { Text(Lang.get("New PIN (letters or digits)", "PIN Baru (Hanya Angka/Karakter)")) },
                placeholder = { Text("Masukkan PIN baru...") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = mosqueTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = Lang.get("Leave the PIN empty and save to remove it.", "Kosongkan PIN lalu simpan untuk menghapus pengaman PIN."),
                color = MosqueTheme.colors.textMuted,
                fontSize = MosqueTheme.typography.captionSmall
            )

            MosquePrimaryButton(
                onClick = {
                    onUpdatePinCode(pinText.trim())
                    say(Lang.get("PIN saved.", "PIN keamanan berhasil disimpan!"))
                    pinText = ""
                },
                shape = RoundedCornerShape(MosqueTheme.radius.md),
                modifier = if (isCompact) {
                    Modifier.fillMaxWidth().height(MosqueTheme.size.controlLg)
                } else {
                    Modifier.align(Alignment.Start).height(MosqueTheme.size.controlLg)
                }
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(MosqueTheme.spacing.md))
                Text(Lang.get("Save PIN", "Simpan PIN"), fontWeight = FontWeight.Bold)
            }
        }

        // Last child of the pane, which is where it belongs on both platforms for
        // different reasons: on Android a Toast is its own window so this places no node
        // at all, and the Snackbar the jvm actual would draw wants the bottom edge.
        MessageHost(messages)
    }
}
