package com.groovernapp.mosqueathan.ui.design

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Every font size currently used by the app, named.
 *
 * These are **sizes, not `TextStyle`s**, on purpose. Call sites today set `fontSize`
 * alongside their own `fontWeight`, `color` and `letterSpacing`; swapping a full
 * `TextStyle` in would change rendering. Replacing only the size cannot.
 *
 * The sizes split in two — though not quite cleanly: `prayerNameCompact` (22sp)
 * sits just under `titleLarge` (24sp), so the families overlap by one step at the
 * boundary. What distinguishes them is that the display family reaches sizes the
 * settings scale never does.
 *
 * - **Display** — the large sizes on the TV screens. Each appears exactly once and is
 *   deliberate: these *are* the mockup and must not move.
 * - **Settings** — eleven sizes between 11sp and 24sp covering 187 occurrences. That
 *   is the part with no scale behind it, and the main target of the consolidation
 *   proposal.
 */
object MosqueTypography {

    // --- Display: the TV screens -------------------------------------------
    /** The full-screen clock in `ClockSection`. The largest size in the app. */
    val clockDisplay: TextUnit = 170.sp
    /** Countdown digits in the tall layout. */
    val countdownDigitsLarge: TextUnit = 112.sp
    /** Iqomah countdown digits. */
    val iqomahCountdown: TextUnit = 96.sp
    /** Standby/adzan countdown digits. */
    val countdownDigits: TextUnit = 92.sp
    /** Adzan screen prayer name. */
    val adzanTitle: TextUnit = 64.sp
    /** Dashboard clock. */
    val clockLarge: TextUnit = 48.sp
    /** Countdown screen prayer name. */
    val countdownTitle: TextUnit = 40.sp
    /** Countdown screen caption. */
    val countdownCaption: TextUnit = 34.sp
    /** Secondary clock reading. */
    val clockMedium: TextUnit = 32.sp
    /** Next-prayer header and running text. */
    val headline: TextUnit = 30.sp
    /** Prayer name in the times panel. */
    val prayerName: TextUnit = 28.sp
    /** Prayer name while the slideshow is running. */
    val prayerNameCompact: TextUnit = 22.sp

    // --- Settings UI --------------------------------------------------------
    val titleLarge: TextUnit = 24.sp
    val titleMedium: TextUnit = 20.sp
    val titleSmall: TextUnit = 18.sp
    val bodyLarge: TextUnit = 17.sp
    val body: TextUnit = 16.sp
    val bodyCompact: TextUnit = 15.sp
    val label: TextUnit = 14.sp
    val labelSmall: TextUnit = 13.sp
    val caption: TextUnit = 12.sp
    val captionSmall: TextUnit = 11.sp
}
