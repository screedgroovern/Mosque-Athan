@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.component.MosqueSectionCard

@Composable
fun PrayerScheduleSettingsSegment(
    state: PrayerScheduleUiState,
    onEvent: (PrayerScheduleEvent) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()
    val tabHeight = if (LocalWindowClass.current.isCompact) 72.dp else 48.dp

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
                            modifier = Modifier.size(MosqueTheme.size.iconMd)
                        )
                        Text(
                            text = Lang.get("Calculation Method", "Metode Perhitungan"),
                            fontSize = if (LocalWindowClass.current.isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.bodyCompact,
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
                            modifier = Modifier.size(MosqueTheme.size.iconMd)
                        )
                        Text(
                            text = Lang.get("Prayer Time Adjustments", "Koreksi Waktu Shalat"),
                            fontSize = if (LocalWindowClass.current.isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.bodyCompact,
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
                    MosqueSectionCard(
                        title = Lang.get("Calculation Method", "Metode Perhitungan"),
                        icon = Icons.Default.Calculate
                    ) {
                        MethodSettingsSegment(state = state, onEvent = onEvent)
                    }

                    // CARD 2: Lokasi GPS
                    MosqueSectionCard(
                        title = Lang.get("Mosque Coordinates", "Titik Lokasi Masjid"),
                        icon = Icons.Default.LocationOn,
                        dimmed = state.calculationMethod == "KEMENAG"
                    ) {
                        LocationSettingsSegment(state = state, onEvent = onEvent)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // CARD 3: Koreksi Waktu (Offsets)
                    MosqueSectionCard(
                        title = Lang.get("Prayer Time Adjustments", "Koreksi Waktu Shalat"),
                        icon = Icons.Default.Tune
                    ) {
                        PrayerOffsetsSettingsSegment(state = state, onEvent = onEvent)
                    }
                }
            }
        }
    }
}



