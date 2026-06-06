package com.groovernapp.mosqueathan.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrayerOffsetsSettingsSegment(
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
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        item { OffsetAdjustmentRow(com.groovernapp.mosqueathan.util.Lang.get("Imsak", "Imsyak"), offsetImsak) { onOffsetImsakChange(it) } }
        item { OffsetAdjustmentRow(com.groovernapp.mosqueathan.util.Lang.get("Fajr", "Subuh"), offsetSubuh) { onOffsetSubuhChange(it) } }
        item { OffsetAdjustmentRow(com.groovernapp.mosqueathan.util.Lang.get("Sunrise", "Syuruq"), offsetSyuruq) { onOffsetSyuruqChange(it) } }
        item { OffsetAdjustmentRow(com.groovernapp.mosqueathan.util.Lang.get("Dhuhr", "Dzuhur"), offsetDzuhur) { onOffsetDzuhurChange(it) } }
        item { OffsetAdjustmentRow(com.groovernapp.mosqueathan.util.Lang.get("Asr", "Ashar"), offsetAshar) { onOffsetAsharChange(it) } }
        item { OffsetAdjustmentRow(com.groovernapp.mosqueathan.util.Lang.get("Maghrib", "Maghrib"), offsetMaghrib) { onOffsetMaghribChange(it) } }
        item { OffsetAdjustmentRow(com.groovernapp.mosqueathan.util.Lang.get("Isha", "Isya"), offsetIsya) { onOffsetIsyaChange(it) } }
    }
}
