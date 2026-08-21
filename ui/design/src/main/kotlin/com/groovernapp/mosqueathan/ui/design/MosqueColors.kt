package com.groovernapp.mosqueathan.ui.design

import androidx.compose.ui.graphics.Color

/**
 * Every colour currently used by the app, named.
 *
 * This is a **1:1 extraction**: each token holds the exact value that was previously
 * inlined, so swapping a literal for its token cannot change a pixel. The names
 * describe how each value is actually used today, not how it ideally would be.
 *
 * That means some tokens are near-duplicates of each other — four golds, nine dark
 * surfaces, seventeen white-alpha steps. That duplication is real and inherited, and
 * collapsing it is a separate, pixel-moving change that needs sign-off. The value of
 * naming first is that the duplication becomes countable.
 */
@Suppress("MemberVisibilityCanBePrivate")
object MosqueColors {

    // --- Accent -------------------------------------------------------------
    /** The app's gold. By far the most-used colour: 135 inlined occurrences. */
    val accent = Color(0xFFFFD54F)
    /** A lighter gold, used for exactly one button container. */
    val accentLight = Color(0xFFFFE082)
    /** Amber, used for the running-text bar's text. */
    val accentAmber = Color(0xFFFFC107)
    /** A muted gold, used for one countdown caption. */
    val accentMuted = Color(0xFFE7C66A)

    // --- Surfaces -----------------------------------------------------------
    // Nine distinct near-blacks. They read as elevation levels that were each
    // invented at the call site rather than chosen from a scale.
    /** Dialog and date-picker containers. */
    val surfaceDialog = Color(0xFF101915)
    /** Card containers in schedule lists. */
    val surfaceCard = Color(0xFF181C1A)
    /** Raised containers in the calculation-method pane. */
    val surfaceRaised = Color(0xFF1E2622)
    /** The settings navigation drawer. */
    val surfaceDrawer = Color(0xFF121212)
    /** Recessed background behind the method dropdown. */
    val surfaceSunken = Color(0xFF0C120F)
    /** Neutral background in the theme pane. */
    val surfaceNeutral = Color(0xFF1E1E1E)
    /** Neutral raised background in the Friday pane. */
    val surfaceNeutralRaised = Color(0xFF2A2A2A)

    /** Adzan screen backdrop; always drawn at 82% alpha. */
    val backdropAdzan = Color(0xFF142018)
    /** Iqomah and countdown backdrop; always drawn at 86% alpha. */
    val backdropIqomah = Color(0xFF18231B)
    /** The running-text bar's green background. */
    val runningTextBackground = Color(0xFF0F3316)

    // --- Status -------------------------------------------------------------
    /** Destructive actions — delete icons and labels. */
    val danger = Color(0xFFE57373)
    /** A second, more saturated red used only for the unpaired-device badge. */
    val dangerStrong = Color(0xFFEF4444)
    /** Success — the paired-device badge. */
    val success = Color(0xFF10B981)
    /** Informational icons and labels. */
    val info = Color(0xFF90CAF9)

    // --- Foreground on dark -------------------------------------------------
    // Derived from Color.White at seventeen distinct alpha levels (296 occurrences).
    // Grouped by the role each level actually plays.
    val textPrimary = Color.White
    val textStrong = Color.White.copy(alpha = 0.8f)
    val textSecondary = Color.White.copy(alpha = 0.7f)
    val textTertiary = Color.White.copy(alpha = 0.6f)
    val textMuted = Color.White.copy(alpha = 0.5f)
    val textFaint = Color.White.copy(alpha = 0.4f)
    val textDisabled = Color.White.copy(alpha = 0.38f)
    val textVeryFaint = Color.White.copy(alpha = 0.35f)
    val textGhost = Color.White.copy(alpha = 0.3f)
    val textGhostSoft = Color.White.copy(alpha = 0.28f)

    // --- Fills and borders on dark -----------------------------------------
    val borderStrong = Color.White.copy(alpha = 0.25f)
    val border = Color.White.copy(alpha = 0.2f)
    val fillStrong = Color.White.copy(alpha = 0.15f)
    val fillMedium = Color.White.copy(alpha = 0.12f)
    val fill = Color.White.copy(alpha = 0.1f)
    val fillSoft = Color.White.copy(alpha = 0.08f)
    val fillSubtle = Color.White.copy(alpha = 0.05f)
    val fillFaint = Color.White.copy(alpha = 0.03f)

    // --- On accent ----------------------------------------------------------
    /**
     * Foreground on the gold accent — black text and icons on a gold button.
     * Distinct from [scrim] despite the same underlying colour: this one is a
     * foreground role, and 35 call sites use it that way.
     */
    val onAccent = Color.Black

    // --- Absolutes ----------------------------------------------------------
    /**
     * Overlays and gradient stops. Call sites keep their own `.copy(alpha = …)`:
     * the thirteen black alpha levels in this app are almost all one-off gradient
     * stops rather than reusable steps, so naming each would invent a scale that
     * does not exist. The white alpha levels above *are* named, because those are
     * genuinely reused — 0.6f alone appears 41 times as secondary text.
     */
    val scrim = Color.Black
    val transparent = Color.Transparent
}
