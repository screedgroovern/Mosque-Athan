package com.groovernapp.mosqueathan.ui.design

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

/**
 * Renders the short, transient messages a ViewModel emits — "sync complete",
 * "sync failed" — and nothing else.
 *
 * Drop it inside a full-screen container; it draws over the content or draws nothing,
 * depending on what the platform's idea of a transient message is.
 *
 * ### Why this is an `expect` and not a shared `Snackbar`
 *
 * A `Snackbar` in `commonMain` would be less code and would be wrong on the one device
 * that matters. Three differences, in the order they bite:
 *
 * 1. **A `Snackbar` is inside the app's window; a `Toast` is not.** On a television the
 *    settings UI is a side sheet plus a detail pane, and the sync button sits in the
 *    detail pane. A `Snackbar` pinned to the bottom of a 55" screen appears metres from
 *    where the user is looking, and Material's own guidance — one at a time, bottom
 *    edge, thumb-reachable — is written for a phone held in one hand.
 * 2. **A `Snackbar` participates in focus and traversal.** This screen is driven by a
 *    D-pad, where every focusable element changes where the arrow keys go. A message
 *    that appears for four seconds and silently rearranges the focus graph while it is
 *    up is a navigation bug waiting to be reported as "the remote stopped working". A
 *    `Toast` is a separate window and never takes focus.
 * 3. **Android's behaviour must not change.** The app is in daily use in a mosque and
 *    this is a refactor, so the Android actual is the same `Toast.makeText(...).show()`
 *    the screen called inline before — moved, not rewritten.
 *
 * `Snackbar` is still the right answer *off* Android, and that is what the jvm actual
 * uses: a desktop controller has no OS-level toast, and it is a windowed app held at
 * desk distance, which is exactly the case Material designed the component for. Keeping
 * the seam lets each platform be right instead of averaging them into one that is not.
 */
@Composable
expect fun MessageHost(messages: Flow<String>)
