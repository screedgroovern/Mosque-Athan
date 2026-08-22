package com.groovernapp.mosqueathan.ui.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/**
 * Pins down the input-mode rule that governs all of this app's focus behaviour.
 *
 * Compose deliberately keeps `clickable` nodes out of the focus system while in
 * **touch** input mode — otherwise tapping the screen would leave focus rings
 * everywhere. A television is always in non-touch mode, so on the real device those
 * nodes are focusable; a test on a phone starts in touch mode, so without
 * [RequestDpadInputMode] every focus assertion here fails while the shipped code is
 * perfectly correct.
 *
 * The three layers exist so that a future focus failure can be placed immediately:
 * plain focusability, `clickable` participation, and this project's own modifier on
 * top of it.
 */
class InputModeFocusTest {

    @Rule
    @JvmField
    val compose = createComposeRule()

    @Test
    fun plainFocusableCanTakeFocus() {
        compose.setContent {
            RequestDpadInputMode()
            Box(Modifier.testTag("t").size(48.dp).focusable())
        }
        compose.waitForIdle()
        compose.onNodeWithTag("t").requestFocus()
        compose.waitForIdle()
        compose.onNodeWithTag("t").assertIsFocused()
    }

    @Test
    fun clickableIsFocusableInNonTouchMode() {
        compose.setContent {
            RequestDpadInputMode()
            Box(Modifier.testTag("t").size(48.dp).clickable { })
        }
        compose.waitForIdle()
        compose.onNodeWithTag("t").requestFocus()
        compose.waitForIdle()
        compose.onNodeWithTag("t").assertIsFocused()
    }

    @Test
    fun focusHighlightDoesNotBreakFocusability() {
        compose.setContent {
            RequestDpadInputMode()
            Box(Modifier.testTag("t").size(48.dp).focusHighlight().clickable { })
        }
        compose.waitForIdle()
        compose.onNodeWithTag("t").requestFocus()
        compose.waitForIdle()
        compose.onNodeWithTag("t").assertIsFocused()
    }
}
