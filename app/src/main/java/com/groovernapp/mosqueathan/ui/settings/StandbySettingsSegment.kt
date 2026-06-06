package com.groovernapp.mosqueathan.ui.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.util.Lang

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    if (isPressed) {
        LaunchedEffect(isPressed) {
            if (isPressed) {
                showDialog = true
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        interactionSource = interactionSource,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        colors = textFieldColors(),
        modifier = modifier
    )

    if (showDialog) {
        val timeParts = value.split(":")
        val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
        val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val hourStr = timePickerState.hour.toString().padStart(2, '0')
                    val minStr = timePickerState.minute.toString().padStart(2, '0')
                    onValueChange("$hourStr:$minStr")
                    showDialog = false
                }) {
                    Text(Lang.get("OK", "OK"), color = Color(0xFFFFD54F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(Lang.get("Cancel", "Batal"), color = Color.White)
                }
            },
            title = {
                Text(text = label, color = Color.White)
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = Color.White.copy(alpha = 0.1f),
                        clockDialSelectedContentColor = Color.Black,
                        clockDialUnselectedContentColor = Color.White,
                        selectorColor = Color(0xFFFFD54F),
                        periodSelectorBorderColor = Color(0xFFFFD54F),
                        periodSelectorSelectedContainerColor = Color(0xFFFFD54F),
                        periodSelectorUnselectedContainerColor = Color.Transparent,
                        periodSelectorSelectedContentColor = Color.Black,
                        periodSelectorUnselectedContentColor = Color.White,
                        timeSelectorSelectedContainerColor = Color(0xFFFFD54F).copy(alpha = 0.3f),
                        timeSelectorUnselectedContainerColor = Color.White.copy(alpha = 0.1f),
                        timeSelectorSelectedContentColor = Color(0xFFFFD54F),
                        timeSelectorUnselectedContentColor = Color.White
                    )
                )
            },
            containerColor = Color(0xFF101915),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
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
