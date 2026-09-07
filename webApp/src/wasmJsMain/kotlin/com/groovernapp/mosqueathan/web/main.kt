package com.groovernapp.mosqueathan.web

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.groovernapp.mosqueathan.ui.design.LinkStatus
import com.groovernapp.mosqueathan.ui.design.LocalLinkStatus
import com.groovernapp.mosqueathan.ui.design.DisplayApproval
import com.groovernapp.mosqueathan.ui.design.LocalDisplayApproval
import com.groovernapp.mosqueathan.ui.design.LocalMediaImageLoader
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import com.groovernapp.mosqueathan.protocol.RemoteActionSink
import com.groovernapp.mosqueathan.protocol.RemoteActions
import com.groovernapp.mosqueathan.ui.design.LocalAppVersion
import com.groovernapp.mosqueathan.ui.design.LocalDisplayBackgrounds
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.MessageHost
import com.groovernapp.mosqueathan.ui.settings.SettingsScreen
import kotlinx.browser.document
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize

/**
 * The browser controller's entry point.
 *
 * It renders the settings UI and nothing else, on purpose: settings is what the
 * hand-written web panel duplicates, and sharing one implementation is the whole reason
 * this module exists. The display screens are a later, best-effort preview.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Loading js-joda's timezone database is a side effect of referencing the module, and
    // kotlinx-datetime needs it before anything asks for the current date. See
    // JsJodaTimeZone.kt — without this the settings screen does not compose at all.
    jsJodaTz

    ComposeViewport(document.body!!) {
        KoinApplication(application = { modules(webModule) }) {
            // Open the link once, for the life of the page. DisplayLink reconnects on its own, so
            // there is nothing to tear down and nothing to retry here.
            val link = koinInject<DisplayLink>()
            LaunchedEffect(Unit) { link.connect() }
            val linkStatus by link.status.collectAsState()

            // One loader for the life of the page: the host cannot change without a reload.
            val mediaImages = remember { WebMediaImageLoader(displayHost()) }

            // Preview is the one thing this screen sends without going through a ViewModel: there
            // is no state to write, only a screen for the display to show.
            val remote = koinInject<RemoteActionSink>()

            CompositionLocalProvider(
                LocalDisplayBackgrounds provides webBackgrounds,
                LocalAppVersion provides WEB_APP_VERSION,
                LocalLinkStatus provides linkStatus.toUiStatus(),
                LocalMediaImageLoader provides mediaImages,
                // The way past a display's PIN. The browser holds a registered socket, so it can
                // await `pin_verification_result` on the same connection that asked — see
                // DisplayLink.verifyPin. Before this the panel could report NeedsApproval and
                // offer no way to become the person who approves it.
                LocalDisplayApproval provides remember {
                    object : DisplayApproval {
                        override suspend fun verifyPin(pin: String) = link.verifyPin(pin)
                    }
                }
            ) {
                MosqueTheme {
                    // A second MessageHost, deliberately. SettingsScreen already renders one for
                    // the ViewModel's own messages, and these come from the transport instead —
                    // which shared code cannot reach without teaching ui:settings about
                    // DisplayLink. Two hosts, each owned by the layer that has something to say.
                    Box(Modifier.fillMaxSize()) {
                    SettingsScreen(
                        viewModel = koinViewModel(),
                        // Preview means "show it on the television", which is the only thing it can
                        // mean from here — the takmir is holding the controller precisely to see
                        // the result on the big screen. On the display these same parameters swap
                        // which screen AppViewModel drives; here they are messages. Until now the
                        // browser left all four at their `{}` default, so the buttons drew, took
                        // focus, and did nothing — the same defect as the Back button that went
                        // nowhere, and the server side had been waiting all along.
                        onPreviewAdzan = { remote.send(RemoteActions.PREVIEW_ADZAN) {} },
                        onPreviewIqomah = { remote.send(RemoteActions.PREVIEW_IQOMAH) {} },
                        onPreviewSlideshow = { remote.send(RemoteActions.PREVIEW_SLIDESHOW) {} },
                        onPreviewFridaySlideshow = {
                            remote.send(RemoteActions.PREVIEW_FRIDAY_SLIDESHOW) {}
                        },
                        // Nothing to go back to: in the browser the settings UI *is* the app, where
                        // on the television it is a screen you leave to return to the clock. Null
                        // rather than an empty lambda, so the close and back controls are not drawn
                        // at all — as `{}` they showed a focus ring and then ignored every press.
                        onBack = null,
                        // Something to press only because it does something: re-registering makes
                        // the display send a fresh snapshot.
                        onSync = link::resync
                    )
                        MessageHost(link.notices)
                    }
                }
            }
        }
    }
}

/**
 * The transport's own status, in the vocabulary the UI understands.
 *
 * Two enums rather than one on purpose: [DisplayLink.Status] describes a WebSocket, and
 * [LinkStatus] describes what to tell a person. They happen to line up today; when the offline
 * replica adds "pending changes" that is a UI state with no socket equivalent, and this is where
 * the two are allowed to diverge.
 */
private fun DisplayLink.Status.toUiStatus(): LinkStatus = when (this) {
    DisplayLink.Status.Connecting -> LinkStatus.Connecting
    DisplayLink.Status.Connected -> LinkStatus.Connected
    DisplayLink.Status.NeedsApproval -> LinkStatus.NeedsApproval
    DisplayLink.Status.Offline -> LinkStatus.Offline
}
