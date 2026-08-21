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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.ui.settings.textFieldColors
import com.groovernapp.mosqueathan.util.Lang
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun RemoteControlSettingsSegment(
    config: MosqueConfigEntity,
    onUpdateRemoteTvIp: (String) -> Unit,
    onUpdatePinCode: (String) -> Unit,
    onSyncWithTv: () -> Unit,
    isCompact: Boolean
) {
    val context = LocalContext.current
    var ipText by remember(config.remoteTvIp) { mutableStateOf(config.remoteTvIp) }
    var pinText by remember { mutableStateOf("") }

    val barcodeLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            val qrContent = result.contents
            val cleanedIp = try {
                val uri = android.net.Uri.parse(qrContent)
                uri.host ?: qrContent
            } catch (e: Exception) {
                qrContent
            }
            onUpdateRemoteTvIp(cleanedIp)
            Toast.makeText(context, Lang.get("Terhubung ke TV via QR: $cleanedIp", "Terhubung ke TV via QR: $cleanedIp"), Toast.LENGTH_SHORT).show()
            onSyncWithTv()
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
                text = Lang.get("Sinkronisasi", "Sinkronisasi"),
                color = MosqueTheme.colors.accent,
                fontSize = MosqueTheme.typography.body,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = Lang.get(
                    "Hubungkan ke Android TV untuk sinkronisasi setelan jadwal, adzan, dan tampilan media secara realtime.",
                    "Hubungkan ke Android TV untuk sinkronisasi setelan jadwal, adzan, dan tampilan media secara realtime."
                ),
                color = MosqueTheme.colors.textTertiary,
                fontSize = MosqueTheme.typography.labelSmall
            )

            OutlinedTextField(
                value = ipText,
                onValueChange = { ipText = it },
                label = { Text(Lang.get("IP Address TV", "IP Address TV")) },
                placeholder = { Text("Contoh: 192.168.1.15") },
                singleLine = true,
                colors = textFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            if (isCompact) {
                // Stack vertically on compact screens
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (ipText.trim().isEmpty()) {
                                Toast.makeText(context, Lang.get("IP TV tidak boleh kosong!", "IP TV tidak boleh kosong!"), Toast.LENGTH_SHORT).show()
                            } else {
                                onUpdateRemoteTvIp(ipText.trim())
                                Toast.makeText(context, Lang.get("Menghubungkan ke TV ${ipText.trim()}...", "Menghubungkan ke TV ${ipText.trim()}..."), Toast.LENGTH_SHORT).show()
                                onSyncWithTv()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MosqueTheme.colors.accent,
                            contentColor = MosqueTheme.colors.onAccent
                        ),
                        shape = RoundedCornerShape(MosqueTheme.radius.md),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Tv, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Lang.get("Hubungkan", "Hubungkan"), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val options = ScanOptions().apply {
                                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                setPrompt(Lang.get("Arahkan kamera ke QR Code TV", "Arahkan kamera ke QR Code TV"))
                                setCameraId(0)
                                setBeepEnabled(true)
                                setBarcodeImageEnabled(true)
                                setOrientationLocked(false)
                            }
                            barcodeLauncher.launch(options)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MosqueTheme.colors.fill,
                            contentColor = MosqueTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(MosqueTheme.radius.md),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Lang.get("Pindai QR", "Pindai QR"), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Side-by-side on larger screens
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg)
                ) {
                    Button(
                        onClick = {
                            if (ipText.trim().isEmpty()) {
                                Toast.makeText(context, Lang.get("IP TV tidak boleh kosong!", "IP TV tidak boleh kosong!"), Toast.LENGTH_SHORT).show()
                            } else {
                                onUpdateRemoteTvIp(ipText.trim())
                                Toast.makeText(context, Lang.get("Menghubungkan ke TV ${ipText.trim()}...", "Menghubungkan ke TV ${ipText.trim()}..."), Toast.LENGTH_SHORT).show()
                                onSyncWithTv()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MosqueTheme.colors.accent,
                            contentColor = MosqueTheme.colors.onAccent
                        ),
                        shape = RoundedCornerShape(MosqueTheme.radius.md),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Tv, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Lang.get("Hubungkan", "Hubungkan"), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val options = ScanOptions().apply {
                                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                setPrompt(Lang.get("Arahkan kamera ke QR Code TV", "Arahkan kamera ke QR Code TV"))
                                setCameraId(0)
                                setBeepEnabled(true)
                                setBarcodeImageEnabled(true)
                                setOrientationLocked(false)
                            }
                            barcodeLauncher.launch(options)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MosqueTheme.colors.fill,
                            contentColor = MosqueTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(MosqueTheme.radius.md),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Lang.get("Pindai QR", "Pindai QR"), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = {
                    if (config.remoteTvIp.isEmpty()) {
                        Toast.makeText(context, Lang.get("Hubungkan ke TV terlebih dahulu!", "Hubungkan ke TV terlebih dahulu!"), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, Lang.get("Menyinkronkan data dengan TV...", "Menyinkronkan data dengan TV..."), Toast.LENGTH_SHORT).show()
                        onSyncWithTv()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MosqueTheme.colors.fillSubtle,
                    contentColor = MosqueTheme.colors.textPrimary
                ),
                shape = RoundedCornerShape(MosqueTheme.radius.md),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MosqueTheme.colors.fillStrong)),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = MosqueTheme.colors.accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Lang.get("Sinkronkan Sekarang", "Sinkronkan Sekarang"), fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = MosqueTheme.colors.fill, thickness = MosqueTheme.stroke.hairline)

        // Section 2: PIN Remote Kontrol
        Column(
            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = Lang.get("PIN Remote Kontrol", "PIN Remote Kontrol"),
                color = MosqueTheme.colors.accent,
                fontSize = MosqueTheme.typography.body,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = Lang.get(
                    "Gunakan PIN keamanan untuk membatasi perangkat remote control lain agar tidak dapat mengubah setelan sembarangan.",
                    "Gunakan PIN keamanan untuk membatasi perangkat remote control lain agar tidak dapat mengubah setelan sembarangan."
                ),
                color = MosqueTheme.colors.textTertiary,
                fontSize = MosqueTheme.typography.labelSmall
            )

            val hasPin = config.pinCode.isNotEmpty()
            val badgeBg = if (hasPin) MosqueTheme.colors.success.copy(alpha = 0.15f) else MosqueTheme.colors.dangerStrong.copy(alpha = 0.15f)
            val badgeColor = if (hasPin) MosqueTheme.colors.success else MosqueTheme.colors.dangerStrong
            val badgeText = if (hasPin) Lang.get("AKTIF (MEMAKAI PIN)", "AKTIF (MEMAKAI PIN)") else Lang.get("TIDAK AKTIF", "TIDAK AKTIF")

            val statusBadgeContent = @Composable {
                Text(
                    text = Lang.get("Status PIN Keamanan:", "Status PIN Keamanan:"),
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
                        .background(MosqueTheme.colors.fillFaint)
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
                        .background(MosqueTheme.colors.fillFaint)
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
                label = { Text(Lang.get("PIN Baru (Hanya Angka/Karakter)", "PIN Baru (Hanya Angka/Karakter)")) },
                placeholder = { Text("Masukkan PIN baru...") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = textFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = Lang.get(
                    "Kosongkan PIN lalu simpan untuk menghapus pengaman PIN.",
                    "Kosongkan PIN lalu simpan untuk menghapus pengaman PIN."
                ),
                color = MosqueTheme.colors.textFaint,
                fontSize = MosqueTheme.typography.captionSmall
            )

            Button(
                onClick = {
                    onUpdatePinCode(pinText.trim())
                    Toast.makeText(context, Lang.get("PIN keamanan berhasil disimpan!", "PIN keamanan berhasil disimpan!"), Toast.LENGTH_SHORT).show()
                    pinText = ""
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MosqueTheme.colors.accent,
                    contentColor = MosqueTheme.colors.onAccent
                ),
                shape = RoundedCornerShape(MosqueTheme.radius.md),
                modifier = if (isCompact) {
                    Modifier.fillMaxWidth().height(48.dp)
                } else {
                    Modifier.align(Alignment.Start).height(48.dp)
                }
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Lang.get("Simpan PIN", "Simpan PIN"), fontWeight = FontWeight.Bold)
            }
        }
    }
}
