package com.groovernapp.mosqueathan.ui.settings.prayerschedule

import com.groovernapp.mosqueathan.ui.settings.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.groovernapp.mosqueathan.util.Lang

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
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        AdjustmentRow(Lang.get("Imsak", "Imsyak"), offsetImsak, minValue = -60, maxValue = 60, showSign = true) { onOffsetImsakChange(it) }
        AdjustmentRow(Lang.get("Fajr", "Subuh"), offsetSubuh, minValue = -60, maxValue = 60, showSign = true) { onOffsetSubuhChange(it) }
        AdjustmentRow(Lang.get("Sunrise", "Syuruq"), offsetSyuruq, minValue = -60, maxValue = 60, showSign = true) { onOffsetSyuruqChange(it) }
        AdjustmentRow(Lang.get("Dhuhr", "Dzuhur"), offsetDzuhur, minValue = -60, maxValue = 60, showSign = true) { onOffsetDzuhurChange(it) }
        AdjustmentRow(Lang.get("Asr", "Ashar"), offsetAshar, minValue = -60, maxValue = 60, showSign = true) { onOffsetAsharChange(it) }
        AdjustmentRow(Lang.get("Maghrib", "Maghrib"), offsetMaghrib, minValue = -60, maxValue = 60, showSign = true) { onOffsetMaghribChange(it) }
        AdjustmentRow(Lang.get("Isha", "Isya'"), offsetIsya, minValue = -60, maxValue = 60, showSign = true) { onOffsetIsyaChange(it) }
    }
}

