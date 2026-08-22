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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

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
    onOffsetIsyaChange: (Int) -> Unit,
    isCompact: Boolean = false
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()
    val tabHeight = if (isCompact) 72.dp else 48.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl),
        modifier = Modifier.fillMaxSize()
    ) {
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MosqueTheme.colors.transparent,
            contentColor = MosqueTheme.colors.accent,
            divider = {}
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                modifier = Modifier.height(tabHeight),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Method",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = Lang.get("Calculation Method", "Metode Perhitungan"),
                            fontSize = if (isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.bodyCompact,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            modifier = Modifier.padding(vertical = MosqueTheme.spacing.xxs)
                        )
                    }
                },
                selectedContentColor = MosqueTheme.colors.accent,
                unselectedContentColor = MosqueTheme.colors.textSecondary
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                modifier = Modifier.height(tabHeight),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Adjustments",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = Lang.get("Prayer Time Adjustments", "Koreksi Waktu Shalat"),
                            fontSize = if (isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.bodyCompact,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            modifier = Modifier.padding(vertical = MosqueTheme.spacing.xxs)
                        )
                    }
                },
                selectedContentColor = MosqueTheme.colors.accent,
                unselectedContentColor = MosqueTheme.colors.textSecondary
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { page ->
            if (page == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xxxl)
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
                            onDownloadKemenagSchedules = onDownloadKemenagSchedules,
                            isCompact = isCompact
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
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
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
        }
    }
}



