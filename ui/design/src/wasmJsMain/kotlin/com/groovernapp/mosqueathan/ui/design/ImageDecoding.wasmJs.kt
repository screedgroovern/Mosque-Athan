package com.groovernapp.mosqueathan.ui.design

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Always null, and correctly so — this is not a missing implementation.
 *
 * The paths this is called with belong to *the display's* filesystem: they are what the
 * TV wrote when a takmir added media. A browser cannot read them and never will, however
 * much of a decoder it gains.
 *
 * Media previews in the browser therefore have to come over HTTP from the display, which
 * already serves them at `/media`. That is a change to the *caller*, not something an
 * `actual` here can fix, and it is the one piece of the web controller that cannot be
 * ported by writing a platform implementation.
 */
actual fun decodeImageFile(path: String): ImageBitmap? = null
