package com.groovernapp.mosqueathan.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Whether a focus ring actually appears is not something reading the source can
 * settle. `Modifier.onFocusChanged` only observes focus targets that come *after* it
 * in the chain, so getting the order wrong leaves focus working perfectly while the
 * ring never draws — and every static check still passes.
 *
 * So these tests look at pixels.
 */
class FocusHighlightTest {

    @Rule
    @JvmField
    val compose = createComposeRule()

    private val TAG = "target"

    /** Fraction of pixels that are close to the accent colour. */
    private fun accentFraction(): Float {
        val map = compose.onNodeWithTag(TAG).captureToImage().toPixelMap()
        var hits = 0
        for (y in 0 until map.height) {
            for (x in 0 until map.width) {
                val p = map[x, y]
                if (p.red > 0.85f && p.green > 0.7f && p.blue < 0.55f) hits++
            }
        }
        return hits.toFloat() / (map.width * map.height)
    }

    @Test
    fun theRingIsNotDrawnBeforeFocusAndIsDrawnAfter() {
        compose.setContent {
            Box(
                Modifier
                    .testTag(TAG)
                    .size(80.dp)
                    .focusHighlight(shape = RectangleShape, scaleUp = false)
                    .background(Color.Black)
                    .clickable { }
            )
        }

        val unfocused = accentFraction()
        assertTrue("an unfocused element should show no accent, saw $unfocused", unfocused < 0.01f)

        compose.onNodeWithTag(TAG).requestFocus()
        compose.waitForIdle()

        val focused = accentFraction()
        assertTrue(
            "a focused element should draw an accent ring, saw $focused",
            focused > 0.05f
        )
    }

    @Test
    fun theRingDisappearsWhenFocusLeaves() {
        compose.setContent {
            Box(Modifier.size(40.dp).testTag("other").focusHighlight().clickable { })
            Box(
                Modifier
                    .testTag(TAG)
                    .size(80.dp)
                    .focusHighlight(shape = RectangleShape, scaleUp = false)
                    .background(Color.Black)
                    .clickable { }
            )
        }

        compose.onNodeWithTag(TAG).requestFocus()
        compose.waitForIdle()
        assertTrue(accentFraction() > 0.05f)

        compose.onNodeWithTag("other").requestFocus()
        compose.waitForIdle()
        assertTrue("the ring should be gone once focus moves away", accentFraction() < 0.01f)
    }

    @Test
    fun focusHighlightDoesNotStopAnElementBeingFocusable() {
        // The failure mode being guarded: a modifier-order mistake that swallows
        // focus entirely, making the control unreachable by D-pad.
        var clicked = false
        compose.setContent {
            Box(
                Modifier
                    .testTag(TAG)
                    .size(48.dp)
                    .focusHighlight()
                    .clickable { clicked = true }
            )
        }

        compose.onNodeWithTag(TAG).requestFocus()
        compose.onNodeWithTag(TAG).assertIsFocused()
        assertFalse("requesting focus must not click", clicked)
    }
}
