package com.groovernapp.mosqueathan.ui.settings.remotedevices

import com.groovernapp.mosqueathan.ui.settings.*

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.groovernapp.mosqueathan.server.SettingsServer
import androidx.core.graphics.set
import androidx.core.graphics.createBitmap
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun WebControlSettingsSegment(
    showSettingsOnTv: Boolean,
    isTv: Boolean,
    onUpdateShowSettingsOnTv: (Boolean) -> Unit,
    isCompact: Boolean
) {
    val ipAddress = remember { SettingsServer.getIpAddress() }
    val serverUrl = if (ipAddress != null) "http://$ipAddress:8080" else null

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (serverUrl != null) {
            val qrCodeBitmap = remember(serverUrl) {
                try {
                    val writer = QRCodeWriter()
                    val bitMatrix = writer.encode(serverUrl, BarcodeFormat.QR_CODE, 512, 512)
                    val bitmap = createBitmap(512, 512, Bitmap.Config.RGB_565)
                    for (x in 0 until 512) {
                        for (y in 0 until 512) {
                            bitmap[x, y] = if (bitMatrix.get(
                                    x,
                                    y
                                )
                            ) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                        }
                    }
                    bitmap.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }

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
                                        color = MosqueTheme.colors.textTertiary,
                                        fontSize = MosqueTheme.typography.captionSmall
                                    )
                                }
                                Switch(
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
                                        color = MosqueTheme.colors.textTertiary,
                                        fontSize = MosqueTheme.typography.captionSmall
                                    )
                                }
                                Switch(
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
                color = Color.Red.copy(alpha = 0.8f),
                fontSize = MosqueTheme.typography.body,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
