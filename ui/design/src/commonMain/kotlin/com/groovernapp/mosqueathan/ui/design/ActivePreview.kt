package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import com.groovernapp.mosqueathan.domain.PreviewKind

/**
 * The preview the *display* is showing, as far as this host can tell.
 *
 * Provided by the host, like [LocalLinkStatus]: only a host that stays on the settings screen while
 * the preview runs elsewhere has anything to report. The browser does — it holds a socket and the
 * display says which preview is up. A device previewing on its own screen leaves the settings screen
 * entirely, so there is nothing left to relabel and it keeps the default, null.
 *
 * Unlike the other host-provided values this one changes while the screen is up, so it is a plain
 * `compositionLocalOf` and only what reads it recomposes.
 */
val LocalActivePreview: ProvidableCompositionLocal<PreviewKind?> = compositionLocalOf { null }

/** Whether this is the preview currently showing; the Preview button for it becomes a Stop button. */
val PreviewKind.isRunning: Boolean
    @Composable get() = LocalActivePreview.current == this
