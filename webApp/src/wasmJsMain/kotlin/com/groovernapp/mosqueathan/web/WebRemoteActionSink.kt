package com.groovernapp.mosqueathan.web

import com.groovernapp.mosqueathan.protocol.RemoteActionSink
import kotlinx.serialization.json.JsonObjectBuilder

/**
 * Sends settings changes to the display over the already-registered WebSocket.
 *
 * The settings ViewModels have always called this — it is how the Android app works in REMOTE mode
 * — so wiring a real implementation is all the browser needed to become a controller rather than a
 * preview. Nothing in `ui:settings` changes.
 */
class WebRemoteActionSink(private val link: DisplayLink) : RemoteActionSink {
    override fun send(action: String, build: JsonObjectBuilder.() -> Unit) = link.send(action, build)
}
