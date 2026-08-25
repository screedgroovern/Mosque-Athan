package com.groovernapp.mosqueathan.ui.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
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
object MosqueTypography : TypeScale {

    // --- Display: the TV screens -------------------------------------------
    /** The full-screen clock in `ClockSection`. The largest size in the app. */
    override val clockDisplay: TextUnit = 170.sp
    /** Countdown digits in the tall layout. */
    override val countdownDigitsLarge: TextUnit = 112.sp
    /** Iqomah countdown digits. */
    override val iqomahCountdown: TextUnit = 96.sp
    /** Standby/adzan countdown digits. */
    override val countdownDigits: TextUnit = 92.sp
    /** Adzan screen prayer name. */
    override val adzanTitle: TextUnit = 64.sp
    /** Dashboard clock. */
    override val clockLarge: TextUnit = 48.sp
    /** Countdown screen prayer name. */
    override val countdownTitle: TextUnit = 40.sp
    /** Countdown screen caption. */
    override val countdownCaption: TextUnit = 34.sp
    /** Secondary clock reading. */
    override val clockMedium: TextUnit = 32.sp
    /** Next-prayer header and running text. */
    override val headline: TextUnit = 30.sp
    /** Prayer name in the times panel. */
    override val prayerName: TextUnit = 28.sp
    /** Prayer name while the slideshow is running. */
    override val prayerNameCompact: TextUnit = 22.sp
    /**
     * The label above each time in the prayer row.
     *
     * Same 20sp it always was, but a *display* role rather than `titleMedium`. It
     * sits on the dashboard, so it has to scale with the window; while it borrowed a
     * settings token it stayed at 20sp on a phone and helped push the running text
     * off screen.
     */
    override val prayerLabel: TextUnit = 20.sp

    // --- Settings UI --------------------------------------------------------
    override val titleLarge: TextUnit = 24.sp
    override val titleMedium: TextUnit = 20.sp
    override val titleSmall: TextUnit = 18.sp
    override val bodyLarge: TextUnit = 17.sp
    override val body: TextUnit = 16.sp
    override val bodyCompact: TextUnit = 15.sp
    override val label: TextUnit = 14.sp
    override val labelSmall: TextUnit = 13.sp
    override val caption: TextUnit = 12.sp
    override val captionSmall: TextUnit = 11.sp
}

/**
 * The set of roles, so a scaled variant can stand in for the base one.
 *
 * Exists because of a real failure: on a phone in landscape the window is 360dp
 * tall, and `clockDisplay` at 170sp plus the three lines under it came to roughly
 * 250dp of text. The clock ate the screen and the prayer times row — the single most
 * important thing on the display — was pushed out of view entirely.
 */
interface TypeScale {
    val clockDisplay: TextUnit
    val countdownDigitsLarge: TextUnit
    val iqomahCountdown: TextUnit
    val countdownDigits: TextUnit
    val adzanTitle: TextUnit
    val clockLarge: TextUnit
    val countdownTitle: TextUnit
    val countdownCaption: TextUnit
    val clockMedium: TextUnit
    val headline: TextUnit
    val prayerName: TextUnit
    val prayerNameCompact: TextUnit
    val prayerLabel: TextUnit
    val titleLarge: TextUnit
    val titleMedium: TextUnit
    val titleSmall: TextUnit
    val bodyLarge: TextUnit
    val body: TextUnit
    val bodyCompact: TextUnit
    val label: TextUnit
    val labelSmall: TextUnit
    val caption: TextUnit
    val captionSmall: TextUnit
}

/**
 * [MosqueTypography] with the display roles multiplied by [factor].
 *
 * Only the display roles scale. The settings roles are already 11–24sp, and
 * shrinking those makes a phone harder to read rather than easier.
 */
private class ScaledTypeScale(private val factor: Float) : TypeScale {
    private fun TextUnit.scaled(): TextUnit = (value * factor).sp

    override val clockDisplay = MosqueTypography.clockDisplay.scaled()
    override val countdownDigitsLarge = MosqueTypography.countdownDigitsLarge.scaled()
    override val iqomahCountdown = MosqueTypography.iqomahCountdown.scaled()
    override val countdownDigits = MosqueTypography.countdownDigits.scaled()
    override val adzanTitle = MosqueTypography.adzanTitle.scaled()
    override val clockLarge = MosqueTypography.clockLarge.scaled()
    override val countdownTitle = MosqueTypography.countdownTitle.scaled()
    override val countdownCaption = MosqueTypography.countdownCaption.scaled()
    override val clockMedium = MosqueTypography.clockMedium.scaled()
    override val headline = MosqueTypography.headline.scaled()
    override val prayerName = MosqueTypography.prayerName.scaled()
    override val prayerNameCompact = MosqueTypography.prayerNameCompact.scaled()
    override val prayerLabel = MosqueTypography.prayerLabel.scaled()

    override val titleLarge = MosqueTypography.titleLarge
    override val titleMedium = MosqueTypography.titleMedium
    override val titleSmall = MosqueTypography.titleSmall
    override val bodyLarge = MosqueTypography.bodyLarge
    override val body = MosqueTypography.body
    override val bodyCompact = MosqueTypography.bodyCompact
    override val label = MosqueTypography.label
    override val labelSmall = MosqueTypography.labelSmall
    override val caption = MosqueTypography.caption
    override val captionSmall = MosqueTypography.captionSmall
}

/**
 * How much to shrink display type for a window this tall.
 *
 * Unscaled on a television at any height, and on any window [FULL_HEIGHT] or taller.
 * Below that it tracks the height, with a floor so the clock never becomes unreadable
 * from across a room.
 */
fun displayTypeScaleFor(height: Dp, isTv: Boolean = false): TypeScale =
    if (isTv || height >= FULL_HEIGHT) MosqueTypography
    else ScaledTypeScale(displayScaleFor(height, isTv))

/**
 * How much display-side vertical space to keep at this height.
 *
 * The same factor the type uses. Fixed padding and fixed row heights have to shrink
 * alongside the text or they eat the room the smaller type just freed: at 360dp the
 * dashboard's paddings and the running-text bar came to 136dp of the 360, which is
 * why shrinking the clock alone was not enough.
 *
 * **A television is never scaled**, whatever its height reports. This is not a special
 * case bolted on: shrinking exists so a phone held at arm's length can still show the
 * prayer times, and a television is the one screen where the opposite is true — it is
 * read from the back of a room and every dp of type is doing work.
 *
 * It is also not hypothetical. The mosque's own display, an AQUA/Haier Android TV, is
 * 1920x1080 at density 320 — **540dp tall**, below the 600dp knee. Without this it
 * rendered at 0.9, and had been doing so since the short-window fix landed. The
 * comment that used to sit here asserted "the television ... 711dp and up", which was
 * an assumption nobody had measured.
 */
fun displayScaleFor(height: Dp, isTv: Boolean = false): Float = when {
    isTv -> 1f
    height >= FULL_HEIGHT -> 1f
    else -> (height / FULL_HEIGHT).coerceIn(MIN_FACTOR, 1f)
}

private val FULL_HEIGHT = 600.dp
private const val MIN_FACTOR = 0.45f
