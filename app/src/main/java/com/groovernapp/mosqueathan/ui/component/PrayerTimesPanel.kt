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

@Composable
fun PrayerTimesPanel(
    prayerTimes: PrayerTimesUiModel,
    isSlideshowActive: Boolean
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(vertical = if (isSlideshowActive) 16.dp else 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        PrayerItem(
            "IMSAK",
            prayerTimes.imsak,
            prayerTimes.highlightedPrayer == PrayerType.IMSAK,
            isSlideshowActive
        )

        PrayerItem(
            "SUBUH",
            prayerTimes.subuh,
            prayerTimes.highlightedPrayer == PrayerType.SUBUH,
            isSlideshowActive
        )

        PrayerItem(
            "SYURUQ",
            prayerTimes.syuruq,
            prayerTimes.highlightedPrayer == PrayerType.SYURUQ,
            isSlideshowActive
        )

        PrayerItem(
            "DZUHUR",
            prayerTimes.dzuhur,
            prayerTimes.highlightedPrayer == PrayerType.DZUHUR,
            isSlideshowActive
        )

        PrayerItem(
            "ASHAR",
            prayerTimes.ashar,
            prayerTimes.highlightedPrayer == PrayerType.ASHAR,
            isSlideshowActive
        )

        PrayerItem(
            "MAGHRIB",
            prayerTimes.maghrib,
            prayerTimes.highlightedPrayer == PrayerType.MAGHRIB,
            isSlideshowActive
        )

        PrayerItem(
            "ISYA",
            prayerTimes.isya,
            prayerTimes.highlightedPrayer == PrayerType.ISYA,
            isSlideshowActive
        )
    }
}
