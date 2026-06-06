package com.groovernapp.mosqueathan.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.util.Lang

@Composable
fun LocationSettingsSegment(
    latText: String,
    onLatTextChange: (String) -> Unit,
    lngText: String,
    onLngTextChange: (String) -> Unit,
    calculationMethodName: String,
    kemenagCityName: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = latText,
            onValueChange = onLatTextChange,
            label = { Text("Latitude") },
            colors = textFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = lngText,
            onValueChange = onLngTextChange,
            label = { Text("Longitude") },
            colors = textFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        if (calculationMethodName == "KEMENAG") {
            Text(
                text = Lang.get(
                    "Note: Kemenag method is active. Prayer times are calculated based on the selected city ($kemenagCityName), not the GPS coordinates above.",
                    "Catatan: Metode Kemenag aktif. Waktu sholat dihitung berdasarkan kota yang dipilih ($kemenagCityName), bukan koordinat GPS di atas."
                ),
                color = Color(0xFFFFD54F),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
