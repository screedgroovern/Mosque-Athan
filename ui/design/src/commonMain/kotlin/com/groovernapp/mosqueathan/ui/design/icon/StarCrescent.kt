package com.groovernapp.mosqueathan.ui.design.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The crescent-and-star that marks the Friday settings category.
 *
 * Material's icon set has no crescent, so this was an Android vector drawable — and
 * `R.drawable` is the one thing a `commonMain` composable cannot reach. The path data is
 * carried over verbatim from `res/drawable/ic_star_crescent.xml` and parsed by Compose's
 * own `PathParser`, so nothing was redrawn or approximated by hand; the same 512x512
 * viewport, the same single path.
 *
 * Having it here also let `SettingsCategory.icon` stop being typed `Any` — it was `Any`
 * only so that one entry could hold a drawable resource id while the rest held
 * `ImageVector`s.
 */
val StarCrescent: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "StarCrescent",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 512f,
        viewportHeight = 512f
    ).addPath(
        pathData = PathParser().parsePathString(PATH_DATA).toNodes(),
        fill = SolidColor(Color.White)
    ).build()
}

private const val PATH_DATA =
    "M239.5,419.6C128.3,428.1 32.8,340 32,228.6 31.4,142.1 82.6,66 161.4,36.9c16.3-6 34.6,5.3 35.8,22.8 1.4,20 1.2,57-19,103.7-27.4,63.2-12,139.7 40.5,185.3 43.1,37.3 103,46.7 154.6,26.5 16-6.3 33.7,3.5 36.6,20.4 2.1,12.5 1.5,25.2-1.2,37.6-21.7,98-111.4,171-213.9,166z m256.3-157.9l-49.1-13c-4.2-1.1-7.8-3.7-9.9-7.5l-23.7-44.5c-4.3-8.1-16.1-8.1-20.4,0l-23.7,44.5c-2.1,3.9-5.7,6.5-9.9,7.5l-49.1,13c-8.8,2.3-12.4,13.4-6,19.7l35.6,34.7c3,3 5.3,7.1 4.6,11.4l-8.4,50.7c-1.5,9 7.8,15.8 15.6,11.7l43.9-23.1c3.8-2 8.4-2 12.1,0l43.9,23.1c7.8,4.1 17.2-2.6 15.6-11.7l-8.4-50.7c-0.7-4.3 1.6-8.4 4.6-11.4l35.6-34.7c6.5-6.3 2.9-17.4-5.9-19.7z"
