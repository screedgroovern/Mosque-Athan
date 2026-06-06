package com.groovernapp.mosqueathan.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
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
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        item { IqomahAdjustmentRow(Lang.get("Adzan Preparation", "Persiapan Adzan"), adzanLeadMinutes, onAdzanLeadMinutesChange) }
        item { IqomahAdjustmentRow(Lang.get("Fajr", "Subuh"), iqomahSubuh, onIqomahSubuhChange) }
        item { IqomahAdjustmentRow(Lang.get("Dhuhr", "Dzuhur"), iqomahDzuhur, onIqomahDzuhurChange) }
        item { IqomahAdjustmentRow(Lang.get("Asr", "Ashar"), iqomahAshar, onIqomahAsharChange) }
        item { IqomahAdjustmentRow(Lang.get("Maghrib", "Maghrib"), iqomahMaghrib, onIqomahMaghribChange) }
        item { IqomahAdjustmentRow(Lang.get("Isha", "Isya"), iqomahIsya, onIqomahIsyaChange) }
    }
}
