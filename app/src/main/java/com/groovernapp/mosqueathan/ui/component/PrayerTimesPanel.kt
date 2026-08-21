package com.groovernapp.mosqueathan.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.ui.dashboard.PrayerTimesUiModel
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun PrayerTimesPanel(
    prayerTimes: PrayerTimesUiModel,
    isSlideshowActive: Boolean
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MosqueTheme.colors.scrim.copy(alpha = 0.55f))
            .padding(vertical = if (isSlideshowActive) 16.dp else 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        PrayerItem(
            PrayerType.IMSAK.displayName,
            prayerTimes.imsak,
            prayerTimes.highlightedPrayer == PrayerType.IMSAK,
            isSlideshowActive
        )

        PrayerItem(
            PrayerType.SUBUH.displayName,
            prayerTimes.subuh,
            prayerTimes.highlightedPrayer == PrayerType.SUBUH,
            isSlideshowActive
        )

        PrayerItem(
            PrayerType.SYURUQ.displayName,
            prayerTimes.syuruq,
            prayerTimes.highlightedPrayer == PrayerType.SYURUQ,
            isSlideshowActive
        )

        PrayerItem(
            PrayerType.DZUHUR.displayName,
            prayerTimes.dzuhur,
            prayerTimes.highlightedPrayer == PrayerType.DZUHUR,
            isSlideshowActive
        )

        PrayerItem(
            PrayerType.ASHAR.displayName,
            prayerTimes.ashar,
            prayerTimes.highlightedPrayer == PrayerType.ASHAR,
            isSlideshowActive
        )

        PrayerItem(
            PrayerType.MAGHRIB.displayName,
            prayerTimes.maghrib,
            prayerTimes.highlightedPrayer == PrayerType.MAGHRIB,
            isSlideshowActive
        )

        PrayerItem(
            PrayerType.ISYA.displayName,
            prayerTimes.isya,
            prayerTimes.highlightedPrayer == PrayerType.ISYA,
            isSlideshowActive
        )
    }
}
