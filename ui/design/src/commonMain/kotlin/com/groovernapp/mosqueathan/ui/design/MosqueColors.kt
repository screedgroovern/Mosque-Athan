package com.groovernapp.mosqueathan.ui.design

import androidx.compose.ui.graphics.Color

/**
 * The app's palette: nineteen tokens.
 *
 * Phase A named all thirty-nine values that were inlined in the code, 1:1, so that
 * every literal-for-token swap was provably neutral. That made the redundancy
 * countable — four golds, ten near-blacks, ten levels of white, eight fill alphas —
 * and phase B collapsed it.
 *
 * Unlike phase A, this set **is not** value-identical to what came before: 61 call
 * sites now draw a slightly different colour. What each merged value used to be is
 * recorded per token, so any single merge can be reverted on its own.
 */
object MosqueColors {

    // --- Accent -------------------------------------------------------------
    /**
     * The app's gold, and its only accent. 135 of 138 accent uses were already this
     * value; the three that were not — #FFE082, #FFC107, #FFE7C66A — appeared once
     * each and now resolve here.
     *
     * The #FFC107 case is the one to check by eye: it sat on the running-text bar,
     * which scrolls across the dashboard all day, and it was the most genuinely
     * different of the three. If it looked deliberate, restore it as its own token.
     */
    val accent = Color(0xFFFFD54F)

    // --- Surfaces -----------------------------------------------------------
    /** Dialogs, cards, drawers, recessed backgrounds. Was #101915 / #181C1A / #0C120F / #121212. */
    val surface = Color(0xFF101915)
    /** Anything sitting above [surface]. Was #1E2622 / #1E1E1E / #2A2A2A. */
    val surfaceRaised = Color(0xFF1E2622)
    /**
     * Full-screen backdrop behind adzan, iqomah and countdown. Was #18231B / #142018.
     * Call sites keep their own alpha — 0.82 on adzan, 0.86 elsewhere — which
     * preserves most of the difference the two colours used to carry.
     */
    val backdrop = Color(0xFF18231B)
    /** The running-text bar's green. Distinct in kind, not a surface level. */
    val runningTextBackground = Color(0xFF0F3316)

    // --- Foreground on dark -------------------------------------------------
    // Ten alpha levels became four. Full white and 0.6 already carried 185 of the
    // 228 uses between them.
    /** Body and heading text. Was 1.0 / 0.8. */
    val textPrimary = Color.White
    /** Supporting text and inactive icons. Was 0.7 / 0.6. */
    val textSecondary = Color.White.copy(alpha = 0.6f)
    /** De-emphasised text. Was 0.5 / 0.4. */
    val textMuted = Color.White.copy(alpha = 0.4f)
    /** Disabled state — Material's value, which is why it survives next to [textMuted]. Was 0.38 / 0.35 / 0.3 / 0.28. */
    val textDisabled = Color.White.copy(alpha = 0.38f)

    // --- Fills and borders on dark -----------------------------------------
    /** Barely-there surface tint. Was 0.05 / 0.03. */
    val fillSubtle = Color.White.copy(alpha = 0.05f)
    /** Standard fill. Was 0.1 / 0.08 / 0.12. */
    val fill = Color.White.copy(alpha = 0.1f)
    /** Emphasised fill — selected rows, active chips. */
    val fillStrong = Color.White.copy(alpha = 0.15f)
    /** Edges and dividers. Was 0.2 / 0.25. */
    val border = Color.White.copy(alpha = 0.2f)

    // --- Status -------------------------------------------------------------
    /** Destructive actions and error states. Was #E57373 / #EF4444. */
    val danger = Color(0xFFE57373)
    val success = Color(0xFF10B981)
    val info = Color(0xFF90CAF9)

    // --- On accent ----------------------------------------------------------
    /** Foreground on gold — black text and icons on a gold button. */
    val onAccent = Color.Black

    // --- Absolutes ----------------------------------------------------------
    /**
     * Overlays and gradient stops; call sites keep their own alpha. The thirteen
     * black alpha levels in this app are one-off gradient stops rather than a reused
     * scale, so naming each would invent a scale that does not exist.
     */
    val scrim = Color.Black
    val transparent = Color.Transparent
}
