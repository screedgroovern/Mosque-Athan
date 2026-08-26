package com.groovernapp.mosqueathan.ui.settings.remotedevices

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.groovernapp.mosqueathan.protocol.TvPairing
import com.groovernapp.mosqueathan.server.SettingsServer
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import com.groovernapp.mosqueathan.ui.design.MosqueRadius
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.focusHighlight
import com.groovernapp.mosqueathan.ui.design.qr.encodeQrCode

@Composable
fun WebControlSettingsSegment(
    state: RemoteDevicesUiState,
    onEvent: (RemoteDevicesEvent) -> Unit
) {
    val windowClass = LocalWindowClass.current
    val isTv = windowClass.isTv
    val isCompact = windowClass.isCompact
    val showSettingsOnTv = state.showSettingsOnTv
    val onUpdateShowSettingsOnTv: (Boolean) -> Unit =
        { onEvent(RemoteDevicesEvent.ShowSettingsOnTvToggled(it)) }
    val ipAddress = remember { SettingsServer.getIpAddress() }
    // TvPairing owns the format so that this string and the scanner that reads it back
    // cannot drift apart; the port lived here as a literal before.
    val serverUrl = ipAddress?.let(TvPairing::url)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (serverUrl != null) {
            // Encoding is not a pane's job, and zxing is not available off the JVM.
            // remember, not derivedStateOf: the address changes when the network does,
            // and 512x512 pixels is not work to repeat per recomposition.
            val qrCodeBitmap = remember(serverUrl) { encodeQrCode(serverUrl, QR_SIZE_PX) }

            if (isCompact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MosqueTheme.spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xxl)
                ) {
                    // Top Section: QR Code + Server URL
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                    ) {
                        if (qrCodeBitmap != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MosqueTheme.colors.textPrimary),
                                shape = RoundedCornerShape(MosqueTheme.radius.lg),
                                modifier = Modifier
                                    .size(160.dp)
                                    .padding(MosqueTheme.spacing.xs)
                            ) {
                                Image(
                                    bitmap = qrCodeBitmap,
                                    contentDescription = "QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Text(
                            text = serverUrl,
                            color = MosqueTheme.colors.accent,
                            fontSize = MosqueTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Bottom Section: Switch & Instructions/Notes
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl),
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (!isTv) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(MosqueTheme.radius.md))
                                    .background(MosqueTheme.colors.fillSubtle)
                                    .border(1.dp, MosqueTheme.colors.fillStrong, RoundedCornerShape(MosqueTheme.radius.md))
                                    .padding(horizontal = MosqueTheme.spacing.xl, vertical = MosqueTheme.spacing.lg),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pengaturan di TV",
                                        color = MosqueTheme.colors.textPrimary,
                                        fontSize = MosqueTheme.typography.label,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tampilkan/sembunyikan menu pengaturan di TV",
                                        color = MosqueTheme.colors.textSecondary,
                                        fontSize = MosqueTheme.typography.captionSmall
                                    )
                                }
                                Switch(
                                    modifier = Modifier.focusHighlight(shape = RoundedCornerShape(MosqueRadius.pill), scaleUp = false),
                                    checked = showSettingsOnTv,
                                    onCheckedChange = { onUpdateShowSettingsOnTv(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MosqueTheme.colors.accent,
                                        checkedTrackColor = MosqueTheme.colors.accent.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Petunjuk Koneksi:",
                                color = MosqueTheme.colors.textPrimary,
                                fontSize = MosqueTheme.typography.label,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "1. Hubungkan HP atau Laptop ke Wi-Fi / jaringan lokal yang sama.",
                                color = MosqueTheme.colors.textSecondary,
                                fontSize = MosqueTheme.typography.caption
                            )
                            Text(
                                text = "2. Scan kode QR di samping atau ketik alamat URL di browser.",
                                color = MosqueTheme.colors.textSecondary,
                                fontSize = MosqueTheme.typography.caption
                            )
                            Text(
                                text = "3. Simpan perubahan pada browser, setelan aplikasi akan ter-update otomatis.",
                                color = MosqueTheme.colors.textSecondary,
                                fontSize = MosqueTheme.typography.caption
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MosqueTheme.spacing.xxxl),
                    horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.huge),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: QR Code + Server URL
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg)
                    ) {
                        if (qrCodeBitmap != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MosqueTheme.colors.textPrimary),
                                shape = RoundedCornerShape(MosqueTheme.radius.lg),
                                modifier = Modifier
                                    .size(180.dp)
                                    .padding(MosqueTheme.spacing.xs)
                            ) {
                                Image(
                                    bitmap = qrCodeBitmap,
                                    contentDescription = "QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Text(
                            text = serverUrl,
                            color = MosqueTheme.colors.accent,
                            fontSize = MosqueTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Right Column: Switch & Instructions/Notes
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl),
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (!isTv) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(MosqueTheme.radius.md))
                                    .background(MosqueTheme.colors.fillSubtle)
                                    .border(1.dp, MosqueTheme.colors.fillStrong, RoundedCornerShape(MosqueTheme.radius.md))
                                    .padding(horizontal = MosqueTheme.spacing.xl, vertical = MosqueTheme.spacing.lg),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pengaturan di TV",
                                        color = MosqueTheme.colors.textPrimary,
                                        fontSize = MosqueTheme.typography.bodyCompact,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tampilkan/sembunyikan menu pengaturan di TV",
                                        color = MosqueTheme.colors.textSecondary,
                                        fontSize = MosqueTheme.typography.captionSmall
                                    )
                                }
                                Switch(
                                    modifier = Modifier.focusHighlight(shape = RoundedCornerShape(MosqueRadius.pill), scaleUp = false),
                                    checked = showSettingsOnTv,
                                    onCheckedChange = { onUpdateShowSettingsOnTv(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MosqueTheme.colors.accent,
                                        checkedTrackColor = MosqueTheme.colors.accent.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Petunjuk Koneksi:",
                                color = MosqueTheme.colors.textPrimary,
                                fontSize = MosqueTheme.typography.bodyCompact,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "1. Hubungkan HP atau Laptop ke Wi-Fi / jaringan lokal yang sama.",
                                color = MosqueTheme.colors.textSecondary,
                                fontSize = MosqueTheme.typography.labelSmall
                            )
                            Text(
                                text = "2. Scan kode QR di samping atau ketik alamat URL di browser.",
                                color = MosqueTheme.colors.textSecondary,
                                fontSize = MosqueTheme.typography.labelSmall
                            )
                            Text(
                                text = "3. Simpan perubahan pada browser, setelan aplikasi akan ter-update otomatis.",
                                color = MosqueTheme.colors.textSecondary,
                                fontSize = MosqueTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = com.groovernapp.mosqueathan.util.Lang.get(
                    "Device is not connected to the local network.\nPlease connect to Wi-Fi or LAN cable first.",
                    "Perangkat tidak terhubung ke jaringan lokal.\nSilakan hubungkan Wi-Fi atau kabel LAN terlebih dahulu."
                ),
                color = MosqueTheme.colors.danger.copy(alpha = 0.8f),
                fontSize = MosqueTheme.typography.body,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Generated at a fixed size rather than the display size: the code is drawn into a 160dp
 * or 180dp card, and re-encoding per window class would produce a different number of
 * modules for the same address.
 */
private const val QR_SIZE_PX = 512
