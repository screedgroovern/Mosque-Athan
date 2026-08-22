package com.groovernapp.mosqueathan.ui.design

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

/**
 * These do not test behaviour — they pin down the shape of the token set, so that
 * the two things phase B was supposed to achieve cannot be quietly undone.
 */
class TokenInvariantsTest {

    @Test
    fun spacingIsAMonotonicScale() {
        val scale = with(MosqueSpacing) {
            listOf(none, hairline, xxs, xs, sm, md, lg, xl, xxl, xxxl, huge, xhuge, xxhuge)
        }
        scale.zipWithNext { a, b ->
            assertTrue(b > a, "spacing must increase: $a then $b")
        }
    }

    @Test
    fun radiusIsAMonotonicScale() {
        val scale = with(MosqueRadius) { listOf(xs, sm, md, lg, xl, xxl, pill) }
        scale.zipWithNext { a, b ->
            assertTrue(b > a, "radius must increase: $a then $b")
        }
    }

    @Test
    fun strokeIsAMonotonicScale() {
        val scale = with(MosqueStroke) { listOf(hairline, medium, thin, thick) }
        scale.zipWithNext { a, b ->
            assertTrue(b > a, "stroke must increase: $a then $b")
        }
    }

    /**
     * The two families overlap at their boundary, and that is intentional rather
     * than a mistake to be asserted away: `prayerNameCompact` is 22sp, used on the
     * TV when the slideshow shrinks the prayer list, and `titleLarge` is 24sp, a
     * settings heading. What matters is that the display family *reaches* sizes the
     * settings scale never does — that is what makes it a display scale.
     */
    @Test
    fun theDisplayFamilyReachesSizesSettingsNeverDoes() {
        val largestSettings = with(MosqueTypography) {
            listOf(titleLarge, titleMedium, titleSmall, bodyLarge, body, bodyCompact,
                label, labelSmall, caption, captionSmall).maxByOrNull { it.value }!!
        }
        with(MosqueTypography) {
            listOf(clockDisplay, countdownDigitsLarge, iqomahCountdown, countdownDigits,
                adzanTitle, clockLarge).forEach {
                assertTrue(
                    it.value > largestSettings.value,
                    "$it should exceed the largest settings size ($largestSettings)"
                )
            }
        }
    }

    @Test
    fun theOnlyOverlapIsTheCompactPrayerName() {
        // If another display size drops into settings territory, the split has
        // eroded and this test should be revisited deliberately.
        val largestSettings = MosqueTypography.titleLarge.value
        val overlapping = with(MosqueTypography) {
            listOf(
                "clockDisplay" to clockDisplay, "countdownDigitsLarge" to countdownDigitsLarge,
                "iqomahCountdown" to iqomahCountdown, "countdownDigits" to countdownDigits,
                "adzanTitle" to adzanTitle, "clockLarge" to clockLarge,
                "countdownTitle" to countdownTitle, "countdownCaption" to countdownCaption,
                "clockMedium" to clockMedium, "headline" to headline,
                "prayerName" to prayerName, "prayerNameCompact" to prayerNameCompact
            ).filter { it.second.value <= largestSettings }.map { it.first }
        }
        assertEquals(listOf("prayerNameCompact"), overlapping)
    }

    @Test
    fun settingsTypographyIsADescendingScale() {
        val scale = with(MosqueTypography) {
            listOf(titleLarge, titleMedium, titleSmall, bodyLarge, body, bodyCompact,
                label, labelSmall, caption, captionSmall)
        }
        scale.zipWithNext { a, b ->
            assertTrue(b.value < a.value, "settings sizes must descend: $a then $b")
        }
    }

    @Test
    fun theFocusScaleIsAVisibleLift() {
        // 1.0 would mean the focus treatment silently does nothing.
        assertTrue(FOCUS_SCALE > 1.0f)
        assertTrue(FOCUS_SCALE < 1.2f, "a lift this large would shove neighbours")
    }

    @Test
    fun disabledTextIsFainterThanMutedText() {
        assertTrue(MosqueColors.textDisabled.alpha < MosqueColors.textMuted.alpha)
    }

    @Test
    fun foregroundEmphasisDescends() {
        val scale = with(MosqueColors) {
            listOf(textPrimary, textSecondary, textMuted, textDisabled)
        }
        scale.zipWithNext { a, b ->
            assertTrue(b.alpha < a.alpha, "emphasis must descend: ${a.alpha} then ${b.alpha}")
        }
    }

    @Test
    fun fillsAreSubtlerThanBorders() {
        assertTrue(MosqueColors.fillSubtle.alpha < MosqueColors.fill.alpha)
        assertTrue(MosqueColors.fill.alpha < MosqueColors.fillStrong.alpha)
        assertTrue(MosqueColors.fillStrong.alpha < MosqueColors.border.alpha)
    }

    @Test
    fun accentAndOnAccentAreOpaque() {
        assertEquals(1f, MosqueColors.accent.alpha, 0f)
        assertEquals(1f, MosqueColors.onAccent.alpha, 0f)
    }
}
