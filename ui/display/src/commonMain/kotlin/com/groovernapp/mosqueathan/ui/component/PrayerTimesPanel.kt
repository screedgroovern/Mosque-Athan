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
import androidx.compose.ui.unit.Dp
import com.groovernapp.mosqueathan.ui.design.isShort

@Composable
fun PrayerTimesPanel(
    prayerTimes: PrayerTimesUiModel,
    isSlideshowActive: Boolean
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MosqueTheme.colors.scrim.copy(alpha = 0.55f))
            .padding(vertical = prayerRowPadding(isSlideshowActive)),
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

/**
 * Vertical padding around the prayer row.
 *
 * Tightened hard on a short window rather than merely scaled. On a phone the row
 * and the running-text bar are the last two things in the column, so every dp of
 * padding here is a dp the running text does not get — and the running text was
 * being clipped at the bottom edge even after the type shrank.
 *
 * On a television and a tablet this is the 20dp (or 16dp during a slideshow) it has
 * always been.
 */
@Composable
private fun prayerRowPadding(isSlideshowActive: Boolean): Dp {
    val base = if (isSlideshowActive) MosqueTheme.spacing.xl else MosqueTheme.spacing.xxl
    return if (MosqueTheme.window.isShort) MosqueTheme.spacing.xs else base
}
