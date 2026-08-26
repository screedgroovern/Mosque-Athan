package com.groovernapp.mosqueathan.ui.settings.prayerschedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.component.mosqueTextFieldColors
import com.groovernapp.mosqueathan.util.Lang

/**
 * The mosque's coordinates.
 *
 * The text is buffered here rather than in the state, because a coordinate being
 * typed is not yet a number: "-7." and "" both have to be allowed to sit in the
 * field, and neither is something to store. Only a parsed value is reported
 * upwards, which is why the state holds `Double`.
 */
@Composable
fun LocationSettingsSegment(
    state: PrayerScheduleUiState,
    onEvent: (PrayerScheduleEvent) -> Unit
) {
    // Re-keyed on the stored value, so an update from elsewhere — a sync from the
    // web panel, say — replaces what is shown.
    var latText by remember(state.latitude) { mutableStateOf(state.latitude.toString()) }
    var lngText by remember(state.longitude) { mutableStateOf(state.longitude.toString()) }

    val isEnabled = !state.usesKemenag

    Column(verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)) {
        OutlinedTextField(
            value = latText,
            onValueChange = { entered ->
                latText = entered
                entered.toDoubleOrNull()?.let { onEvent(PrayerScheduleEvent.LatitudeChanged(it)) }
            },
            label = { Text("Latitude") },
            colors = mosqueTextFieldColors(),
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = lngText,
            onValueChange = { entered ->
                lngText = entered
                entered.toDoubleOrNull()?.let { onEvent(PrayerScheduleEvent.LongitudeChanged(it)) }
            },
            label = { Text("Longitude") },
            colors = mosqueTextFieldColors(),
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth()
        )
        if (state.usesKemenag) {
            Text(
                text = Lang.get(
                    "Kemenag method is active, so prayer times come from the selected city (${state.kemenag.cityName}) rather than these coordinates.",
                    "Catatan: Metode Kemenag aktif. Waktu sholat dihitung berdasarkan kota yang dipilih (${state.kemenag.cityName}), bukan koordinat GPS di atas."
                ),
                color = MosqueTheme.colors.accent,
                fontSize = MosqueTheme.typography.label,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = MosqueTheme.spacing.xs)
            )
        }
    }
}
