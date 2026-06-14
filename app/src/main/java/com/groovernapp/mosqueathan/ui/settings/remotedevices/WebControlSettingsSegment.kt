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

@Composable
fun WebControlSettingsSegment(
    showSettingsOnTv: Boolean,
    isTv: Boolean,
    onUpdateShowSettingsOnTv: (Boolean) -> Unit
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: QR Code + Server URL
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (qrCodeBitmap != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .size(180.dp)
                                .padding(4.dp)
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
                        color = Color(0xFFFFD54F),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // Right Column: Switch & Instructions/Notes
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (!isTv) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pengaturan di TV",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tampilkan/sembunyikan menu pengaturan di TV",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = showSettingsOnTv,
                                onCheckedChange = { onUpdateShowSettingsOnTv(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFFD54F),
                                    checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Petunjuk Koneksi:",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "1. Hubungkan HP atau Laptop ke Wi-Fi / jaringan lokal yang sama.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "2. Scan kode QR di samping atau ketik alamat URL di browser.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "3. Simpan perubahan pada browser, setelan aplikasi akan ter-update otomatis.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
