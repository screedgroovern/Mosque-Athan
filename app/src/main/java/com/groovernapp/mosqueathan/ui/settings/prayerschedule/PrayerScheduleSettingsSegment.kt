package com.groovernapp.mosqueathan.ui.settings.prayerschedule

import com.groovernapp.mosqueathan.ui.settings.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Tune
import com.groovernapp.mosqueathan.util.Lang

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun PrayerScheduleSettingsSegment(
    latText: String,
    onLatTextChange: (String) -> Unit,
    lngText: String,
    onLngTextChange: (String) -> Unit,
    calculationMethodName: String,
    kemenagCityId: String,
    kemenagCityName: String,
    kemenagDuration: String,
    kemenagTargetDate: String,
    citySearchResults: List<Pair<String, String>>,
    isSearchingCities: Boolean,
    kemenagSchedules: List<com.groovernapp.mosqueathan.data.KemenagScheduleEntity>,
    onMethodChange: (String) -> Unit,
    onKemenagCitySelected: (String, String) -> Unit,
    onKemenagDurationChange: (String) -> Unit,
    onKemenagTargetDateChange: (String) -> Unit,
    onSearchKemenagCities: (String) -> Unit,
    onDownloadKemenagSchedules: () -> Unit,
    offsetImsak: Int,
    offsetSubuh: Int,
    offsetSyuruq: Int,
    offsetDzuhur: Int,
    offsetAshar: Int,
    offsetMaghrib: Int,
    offsetIsya: Int,
    onOffsetImsakChange: (Int) -> Unit,
    onOffsetSubuhChange: (Int) -> Unit,
    onOffsetSyuruqChange: (Int) -> Unit,
    onOffsetDzuhurChange: (Int) -> Unit,
    onOffsetAsharChange: (Int) -> Unit,
    onOffsetMaghribChange: (Int) -> Unit,
    onOffsetIsyaChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // CARD 1: Metode Perhitungan
        SettingsCard(
            title = Lang.get("Calculation Method", "Metode Perhitungan"),
            icon = Icons.Default.Calculate
        ) {
            MethodSettingsSegment(
                calculationMethodName = calculationMethodName,
                kemenagCityId = kemenagCityId,
                kemenagCityName = kemenagCityName,
                kemenagDuration = kemenagDuration,
                kemenagTargetDate = kemenagTargetDate,
                citySearchResults = citySearchResults,
                isSearchingCities = isSearchingCities,
                kemenagSchedules = kemenagSchedules,
                onMethodChange = onMethodChange,
                onKemenagCitySelected = onKemenagCitySelected,
                onKemenagDurationChange = onKemenagDurationChange,
                onKemenagTargetDateChange = onKemenagTargetDateChange,
                onSearchKemenagCities = onSearchKemenagCities,
                onDownloadKemenagSchedules = onDownloadKemenagSchedules
            )
        }

        // CARD 2: Lokasi GPS
        SettingsCard(
            title = Lang.get("Mosque Coordinates", "Titik Lokasi Masjid"),
            icon = Icons.Default.LocationOn,
            dimmed = calculationMethodName == "KEMENAG"
        ) {
            LocationSettingsSegment(
                latText = latText,
                onLatTextChange = onLatTextChange,
                lngText = lngText,
                onLngTextChange = onLngTextChange,
                calculationMethodName = calculationMethodName,
                kemenagCityName = kemenagCityName
            )
        }

        // CARD 3: Koreksi Waktu (Offsets)
        SettingsCard(
            title = Lang.get("Prayer Time Adjustments", "Koreksi Waktu Shalat"),
            icon = Icons.Default.Tune
        ) {
            PrayerOffsetsSettingsSegment(
                offsetImsak = offsetImsak,
                offsetSubuh = offsetSubuh,
                offsetSyuruq = offsetSyuruq,
                offsetDzuhur = offsetDzuhur,
                offsetAshar = offsetAshar,
                offsetMaghrib = offsetMaghrib,
                offsetIsya = offsetIsya,
                onOffsetImsakChange = onOffsetImsakChange,
                onOffsetSubuhChange = onOffsetSubuhChange,
                onOffsetSyuruqChange = onOffsetSyuruqChange,
                onOffsetDzuhurChange = onOffsetDzuhurChange,
                onOffsetAsharChange = onOffsetAsharChange,
                onOffsetMaghribChange = onOffsetMaghribChange,
                onOffsetIsyaChange = onOffsetIsyaChange
            )
        }
    }
}


