package com.groovernapp.mosqueathan.ui.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing, corner radii and stroke widths.
 *
 * Unlike the colours and font sizes, the `dp` values in this app were already close to
 * a consistent scale before any of this: 8dp appears 122 times, 16dp 89, 12dp 61, 6dp
 * 36, 24dp 32. There is nothing to consolidate here — only to name, so that the values
 * can be scaled per window class later without touching call sites.
 */
object MosqueSpacing {
    val none: Dp = 0.dp
    val hairline: Dp = 1.dp
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 6.dp
    val md: Dp = 8.dp
    val lg: Dp = 12.dp
    val xl: Dp = 16.dp
    val xxl: Dp = 20.dp
    val xxxl: Dp = 24.dp
    val huge: Dp = 32.dp
    val xhuge: Dp = 36.dp
    val xxhuge: Dp = 48.dp
}

/** Corner radii. `RoundedCornerShape` accounts for 94 inlined `dp` values. */
object MosqueRadius {
    val xs: Dp = 4.dp
    val sm: Dp = 6.dp
    val md: Dp = 8.dp
    val lg: Dp = 12.dp
    val xl: Dp = 16.dp
    val xxl: Dp = 20.dp
    val pill: Dp = 999.dp
}

/** Border and divider widths. */
object MosqueStroke {
    val hairline: Dp = 1.dp
    /** The resting border on a preview button, between [hairline] and [thin]. */
    val medium: Dp = 1.5.dp
    val thin: Dp = 2.dp
    val thick: Dp = 3.dp
}
