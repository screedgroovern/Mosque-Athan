package com.groovernapp.mosqueathan.protocol

import kotlinx.serialization.json.JsonObjectBuilder

/**
 * Where a settings change goes when this device is a controller rather than the display.
 *
 * One method, because that is all the settings ViewModels ever needed — the transport,
 * the device id, and the decision about whether there is even a display to talk to all
 * belong to the implementation.
 *
 * It is an interface so `ui:settings` can be built without an HTTP client. The Android
 * implementation uses `HttpURLConnection`; a web or iOS controller will use something
 * else entirely, and neither belongs in a ViewModel.
 *
 * Doing nothing is a valid implementation: on the display itself there is nobody to tell.
 */
interface RemoteActionSink {
    fun send(action: String, build: JsonObjectBuilder.() -> Unit)
}

/** Sends [action] with no payload beyond what the implementation adds itself. */
fun RemoteActionSink.send(action: String) = send(action) {}
