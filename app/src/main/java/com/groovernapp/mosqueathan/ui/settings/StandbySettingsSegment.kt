package com.groovernapp.mosqueathan.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.util.Lang

@Composable
fun TimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val parts = value.split(":")
    var hour by remember(value) { mutableIntStateOf(parts.getOrNull(0)?.toIntOrNull() ?: 0) }
    var minute by remember(value) { mutableIntStateOf(parts.getOrNull(1)?.toIntOrNull() ?: 0) }

    fun emitChange() {
        val h = hour.toString().padStart(2, '0')
        val m = minute.toString().padStart(2, '0')
        onValueChange("$h:$m")
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Hour stepper
            TvFocusableButton(
                onClick = { hour = (hour - 1 + 24) % 24; emitChange() },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("−", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = hour.toString().padStart(2, '0'),
                color = Color(0xFFFFD54F),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )

            TvFocusableButton(
                onClick = { hour = (hour + 1) % 24; emitChange() },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = ":",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Minute stepper
            TvFocusableButton(
                onClick = { minute = (minute - 1 + 60) % 60; emitChange() },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("−", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = minute.toString().padStart(2, '0'),
                color = Color(0xFFFFD54F),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )

            TvFocusableButton(
                onClick = { minute = (minute + 1) % 60; emitChange() },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StandbySettingsSegment(
    standbyModeEnabled: Boolean,
    onStandbyModeEnabledChange: (Boolean) -> Unit,
    standbyStartTime: String,
    onStandbyStartTimeChange: (String) -> Unit,
    standbyEndTime: String,
    onStandbyEndTimeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Lang.get("Automatic Standby Mode", "Mode Standby Otomatis"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Lang.get(
                        "Put the TV in standby mode (black screen) outside operational hours.",
                        "Masuk ke mode standby (layar hitam) di luar jam operasional."
                    ),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            Switch(
                checked = standbyModeEnabled,
                onCheckedChange = onStandbyModeEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFFD54F),
                    checkedTrackColor = Color(0xFFFFD54F).copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
        }

        Divider(color = Color.White.copy(alpha = 0.15f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TimePickerField(
                value = standbyStartTime,
                onValueChange = onStandbyStartTimeChange,
                label = Lang.get("Standby Start (HH:mm)", "Mulai Standby (HH:mm)"),
                placeholder = "22:00",
                modifier = Modifier.weight(1f)
            )
            TimePickerField(
                value = standbyEndTime,
                onValueChange = onStandbyEndTimeChange,
                label = Lang.get("Standby End (HH:mm)", "Selesai Standby (HH:mm)"),
                placeholder = "04:00",
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = Lang.get(
                "Use 24-hour format (e.g., 22:00 for standby, 04:30 for wake up). While standby is active, the screen is kept black to save energy and screen life.",
                "Gunakan format 24 jam (misal 22:00 untuk mulai, 04:30 untuk bangun). Selama standby aktif, layar akan berwarna hitam penuh untuk menghemat energi."
            ),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp
        )
    }
}
