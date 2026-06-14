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
        OffsetAdjustmentRow(Lang.get("Imsak", "Imsyak"), offsetImsak) { onOffsetImsakChange(it) }
        OffsetAdjustmentRow(Lang.get("Fajr", "Subuh"), offsetSubuh) { onOffsetSubuhChange(it) }
        OffsetAdjustmentRow(Lang.get("Sunrise", "Syuruq"), offsetSyuruq) { onOffsetSyuruqChange(it) }
        OffsetAdjustmentRow(Lang.get("Dhuhr", "Dzuhur"), offsetDzuhur) { onOffsetDzuhurChange(it) }
        OffsetAdjustmentRow(Lang.get("Asr", "Ashar"), offsetAshar) { onOffsetAsharChange(it) }
        OffsetAdjustmentRow(Lang.get("Maghrib", "Maghrib"), offsetMaghrib) { onOffsetMaghribChange(it) }
        OffsetAdjustmentRow(Lang.get("Isha", "Isya"), offsetIsya) { onOffsetIsyaChange(it) }
    }
}

