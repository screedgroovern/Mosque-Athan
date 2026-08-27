package com.groovernapp.mosqueathan.web

import androidx.compose.ui.graphics.painter.ColorPainter
import com.groovernapp.mosqueathan.ui.design.DisplayBackgrounds
import com.groovernapp.mosqueathan.ui.design.MosqueColors

/**
 * Flat colour where the Android build has photographs.
 *
 * The three background images are Android drawables, and the browser has none of them
 * bundled — Compose Multiplatform resources would be the way to ship them, and that is
 * work this module has not done yet.
 *
 * A flat backdrop is the right placeholder rather than a broken image: the settings UI
 * draws its own surfaces on top, so the background is atmosphere, not information. The
 * dashboard preview will look plainly unfinished, which is honest.
 *
 * Providing *something* is not optional. `displayBackgrounds` throws when the local is
 * missing, deliberately — a blank screen on a mosque wall is a worse way to discover a
 * wiring mistake. That is exactly how this omission announced itself: the first browser
 * load threw before painting anything.
 */
val webBackgrounds = DisplayBackgrounds(
    dashboard = ColorPainter(MosqueColors.backdrop),
    adzan = ColorPainter(MosqueColors.backdrop),
    iqomah = ColorPainter(MosqueColors.backdrop)
)
