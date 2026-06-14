package com.groovernapp.mosqueathan.ui.settings.adzaniqomah

import com.groovernapp.mosqueathan.ui.settings.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.groovernapp.mosqueathan.util.Lang

@Composable
fun IqomahSettingsSegment(
    adzanLeadMinutes: Int,
    onAdzanLeadMinutesChange: (Int) -> Unit,
    iqomahSubuh: Int,
    onIqomahSubuhChange: (Int) -> Unit,
    iqomahDzuhur: Int,
    onIqomahDzuhurChange: (Int) -> Unit,
    iqomahAshar: Int,
    onIqomahAsharChange: (Int) -> Unit,
    iqomahMaghrib: Int,
    onIqomahMaghribChange: (Int) -> Unit,
    iqomahIsya: Int,
    onIqomahIsyaChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        IqomahAdjustmentRow(Lang.get("Adzan Preparation", "Persiapan Adzan"), adzanLeadMinutes, onAdzanLeadMinutesChange)
        IqomahAdjustmentRow(Lang.get("Fajr", "Subuh"), iqomahSubuh, onIqomahSubuhChange)
        IqomahAdjustmentRow(Lang.get("Dhuhr", "Dzuhur"), iqomahDzuhur, onIqomahDzuhurChange)
        IqomahAdjustmentRow(Lang.get("Asr", "Ashar"), iqomahAshar, onIqomahAsharChange)
        IqomahAdjustmentRow(Lang.get("Maghrib", "Maghrib"), iqomahMaghrib, onIqomahMaghribChange)
        IqomahAdjustmentRow(Lang.get("Isha", "Isya"), iqomahIsya, onIqomahIsyaChange)
    }
}

