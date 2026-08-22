package com.groovernapp.mosqueathan.ui.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/**
 * Every dialog in the app relies on `initialFocus` to be usable with a remote: open
 * one without it and there is no focused element, so the first arrow press and Enter
 * both do nothing.
 */
class InitialFocusTest {

    @Rule
    @JvmField
    val compose = createComposeRule()

    @Test
    fun theTaggedElementHoldsFocusOnFirstComposition() {
        compose.setContent {
            Column {
                Box(Modifier.testTag("first").size(48.dp).clickable { })
                Box(Modifier.testTag("confirm").size(48.dp).initialFocus().clickable { })
            }
        }

        // v2 test rules queue effects rather than running them immediately, so the
        // focus request has to be allowed to run before it can be observed.
        compose.waitForIdle()

        compose.onNodeWithTag("confirm").assertIsFocused()
        compose.onNodeWithTag("first").assertIsNotFocused()
    }

    @Test
    fun focusIsTakenWhenTheElementAppearsLater() {
        // A dialog is composed after the screen behind it, which is the case that
        // matters: the requester must fire when the node attaches, not at startup.
        compose.setContent {
            val shown = remember { mutableStateOf(false) }
            Column {
                Box(
                    Modifier
                        .testTag("opener")
                        .size(48.dp)
                        .clickable { shown.value = true }
                )
                if (shown.value) {
                    Box(Modifier.testTag("confirm").size(48.dp).initialFocus().clickable { })
                }
            }
        }

        compose.onNodeWithTag("confirm").assertDoesNotExist()
        compose.onNodeWithTag("opener").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("confirm").assertIsFocused()
    }
}
