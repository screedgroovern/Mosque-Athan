package com.groovernapp.mosqueathan.ui.settings.prayerschedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.settings.AdjustmentRow

/**
 * Per-prayer minute corrections.
 *
 * Every prayer gets one, Imsak and Syuruq included — unlike iqomah, an offset is
 * meaningful for a time that is only displayed. So this iterates `PrayerType.entries`
 * rather than a filtered list, and was previously seven near-identical lines.
 */
@Composable
fun PrayerOffsetsSettingsSegment(
    state: PrayerScheduleUiState,
    onEvent: (PrayerScheduleEvent) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        PrayerType.entries.forEach { prayer ->
            AdjustmentRow(
                title = prayer.displayName,
                value = state.offsetFor(prayer),
                minValue = -60,
                maxValue = 60,
                showSign = true,
                onValueChange = { onEvent(PrayerScheduleEvent.OffsetChanged(prayer, it)) }
            )
        }
    }
}
