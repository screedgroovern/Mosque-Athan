package com.groovernapp.mosqueathan.web

import com.groovernapp.mosqueathan.domain.MosqueSnapshot
import com.groovernapp.mosqueathan.ui.design.PinResult
import kotlinx.coroutines.CompletableDeferred
import com.groovernapp.mosqueathan.domain.mosqueJson
import com.groovernapp.mosqueathan.protocol.RemoteActions
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.w3c.dom.WebSocket
import com.groovernapp.mosqueathan.util.Lang

/**
 * The browser's link to the display.
 *
 * One WebSocket carries everything, which is why there is no HTTP polling here: on connect this
 * sends `register`, and the display answers with a `registration_result` followed immediately by
 * the full state — then re-sends the full state to every client on any change. So the same socket
 * is the initial read, the live update channel, and the write path. `POST /api/action` exists and
 * works, but it builds a throwaway connection and re-authenticates per request; the socket is
 * already registered.
 *
 * State is never merged. The display is authoritative and sends whole snapshots, so a write is
 * "send the action, wait for the snapshot to come back". That is also what the settings UI already
 * expects — every change writes and `getConfigFlow()` fans the result back out.
 */
class DisplayLink(
    private val repository: InMemoryConfigRepository,
    private val host: String,
    private val deviceId: String,
    private val deviceName: String
) {

    /** What to tell the user. Deliberately more than connected/disconnected — see [Status]. */
    enum class Status {
        /** Opening the socket, or waiting for the display to answer `register`. */
        Connecting,

        /** Registered and receiving state. Everything on screen is what the display has. */
        Connected,

        /**
         * The display has a PIN and this device has not been allowed. Not an error and not
         * retryable by reconnecting — a person has to approve the device or enter the PIN.
         */
        NeedsApproval,

        /** No display reachable. Reconnection is already scheduled. */
        Offline
    }

    private val _status = MutableStateFlow(Status.Connecting)
    val status: StateFlow<Status> = _status.asStateFlow()

    private var socket: WebSocket? = null
    private var retryDelayMs = INITIAL_RETRY_MS
    private var closingDeliberately = false
    private var watchingForeground = false

    /**
     * Actions raised before the socket was ready. Small and bounded: settings changes arrive one
     * control at a time, and dropping them silently would mean a toggle that moved on screen and
     * nowhere else.
     */
    private val pending = mutableListOf<String>()

    /**
     * Told to the user, because a queued action is otherwise indistinguishable from a dead button.
     *
     * The web repository writes nothing locally, so while the display is unreachable a press
     * produces no row, no error and no movement — and the natural response is to press again. That
     * is not hypothetical: eight presses on a silent "Add Standby" became eight identical schedules
     * the moment the link recovered, because queuing is idempotent for "set this value" and is not
     * for "add a row".
     *
     * `extraBufferCapacity` rather than a replay: these are momentary, and a message about a press
     * made minutes ago is noise.
     */
    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    fun connect() {
        if (!watchingForeground) {
            watchingForeground = true
            watchForForeground()
        }
        if (_status.value != Status.NeedsApproval) _status.value = Status.Connecting
        val ws = WebSocket("ws://$host:$PORT/")
        socket = ws
        // A close this object caused is not a lost connection, so it must not also schedule a
        // reconnect — that would race with the one already in flight.
        closingDeliberately = false

        ws.onopen = {
            retryDelayMs = INITIAL_RETRY_MS
            ws.send(
                buildJsonObject {
                    put("action", RemoteActions.REGISTER)
                    put("deviceId", deviceId)
                    put("deviceName", deviceName)
                }.toString()
            )
            null
        }

        ws.onmessage = { event ->
            (event.asMessageData() as? String)?.let(::handleMessage)
            null
        }

        ws.onclose = {
            socket = null
            if (closingDeliberately) {
                closingDeliberately = false
            } else {
                if (_status.value != Status.NeedsApproval) _status.value = Status.Offline
                scheduleReconnect()
            }
            null
        }

        // onerror also fires before onclose on a refused connection; leave the state change to
        // onclose so the two cannot disagree.
        ws.onerror = { null }
    }

    /**
     * Asks the display for everything again, by replacing the socket rather than reusing it.
     *
     * **`readyState` cannot be trusted after the page has been in the background.** Found by
     * testing: choosing a file puts the browser's own picker in front, and when the page came back
     * the socket still reported `OPEN` while the display's broadcasts no longer arrived — an
     * uploaded background was on the display and absent from the panel, under a status chip
     * reading "Connected". Sending `register` into that socket changed nothing.
     *
     * So this closes and reconnects. On a LAN that is one round trip — `register` comes back with a
     * full snapshot — and unlike a re-register it cannot succeed silently against a dead socket.
     */
    fun resync() {
        val existing = socket
        if (existing != null) {
            closingDeliberately = true
            runCatching { existing.close() }
            socket = null
        }
        retryDelayMs = INITIAL_RETRY_MS
        connect()
    }

    /**
     * Revalidates the link whenever the page comes back to the foreground.
     *
     * The file picker is the case that exposed this, but any tab switch or screen lock does the
     * same thing. Called once from [connect]'s owner rather than per connection, so there is only
     * ever one listener.
     */
    private fun watchForForeground() {
        onPageVisible {
            if (_status.value != Status.NeedsApproval) resync()
        }
    }

    /**
     * The PIN attempt currently in flight, waiting for the display to answer.
     *
     * One at a time, because the gate that asks does not let a second attempt start while the
     * first is outstanding. `pin_verification_result` carries no correlation id, so a second
     * concurrent attempt could only be matched by guessing.
     */
    private var pendingPin: CompletableDeferred<PinResult>? = null

    /**
     * Sends [pin] and suspends until the display answers.
     *
     * Not routed through [send], deliberately: [send] queues when the socket is down, and a PIN
     * held for later delivery would leave the gate spinning with nothing to report. If there is no
     * open socket there is no approval to be had right now, and saying so is the useful answer.
     */
    suspend fun verifyPin(pin: String): PinResult {
        val open = socket?.takeIf { it.readyState == WebSocket.OPEN }
            ?: return PinResult.Unreachable(null)

        val awaiting = CompletableDeferred<PinResult>()
        pendingPin = awaiting
        open.send(
            buildJsonObject {
                put("action", RemoteActions.VERIFY_PIN)
                put("deviceId", deviceId)
                put("pin", pin)
            }.toString()
        )
        return awaiting.await()
    }

    /** Sends an action, or holds it until the socket is ready. */
    fun send(action: String, build: JsonObjectBuilder.() -> Unit) {
        val payload = buildJsonObject {
            put("action", action)
            put("deviceId", deviceId)
            build()
        }.toString()
        val open = socket?.takeIf { it.readyState == WebSocket.OPEN }
        if (open != null) open.send(payload) else queue(payload)
    }

    private fun queue(payload: String) {
        if (pending.size >= MAX_PENDING) pending.removeAt(0)
        pending.add(payload)
        _notices.tryEmit(
            Lang.get(
                "Not connected. This will be sent when the display is back.",
                "Belum tersambung. Ini akan dikirim saat display kembali."
            )
        )
    }

    private fun flushPending() {
        val open = socket?.takeIf { it.readyState == WebSocket.OPEN } ?: return
        val count = pending.size
        pending.forEach(open::send)
        pending.clear()
        // Saying how many closes the loop the queue opened. It is also the moment a takmir who
        // pressed a silent button repeatedly finds out how many times it counted.
        if (count > 0) {
            _notices.tryEmit(
                Lang.get(
                    "Sent $count pending change${if (count == 1) "" else "s"}.",
                    "$count perubahan tertunda telah dikirim."
                )
            )
        }
    }

    private fun handleMessage(text: String) {
        val type = runCatching {
            mosqueJson.parseToJsonElement(text)
        }.getOrNull()?.let { element ->
            runCatching { element.asObjectField("type") }.getOrNull()
        }

        when (type) {
            "registration_result" -> {
                val allowed = runCatching {
                    mosqueJson.parseToJsonElement(text).asObjectField("isAllowed") == "true"
                }.getOrDefault(false)
                _status.value = if (allowed) Status.Connected else Status.NeedsApproval
                if (allowed) flushPending()
            }

            "pin_verification_result" -> {
                val success = runCatching {
                    mosqueJson.parseToJsonElement(text).asObjectField("success") == "true"
                }.getOrDefault(false)
                if (success) _status.value = Status.Connected
                val reason = runCatching {
                    mosqueJson.parseToJsonElement(text).asObjectField("error")
                }.getOrNull()
                // Completing the attempt is what releases the gate. A reply with nothing waiting
                // on it is not an error — a reconnect can deliver one late.
                pendingPin?.complete(
                    if (success) PinResult.Accepted else PinResult.Rejected(reason)
                )
                pendingPin = null
            }

            "state" -> {
                // A snapshot arriving is the only proof the display accepted this device, so it
                // also settles the status — a registration_result can be missed on a reconnect.
                runCatching { mosqueJson.decodeFromString<MosqueSnapshot>(text) }
                    .onSuccess {
                        repository.apply(it)
                        _status.value = Status.Connected
                        flushPending()
                        // A snapshot is proof of acceptance, so it also settles a PIN attempt
                        // still waiting. On this socket the result frame does arrive and normally
                        // settles it first; without this, a state delivered *without* it would
                        // leave the gate spinning on a PIN the display had already accepted. The
                        // Android controller has no choice in the matter — over HTTP the state is
                        // the only reply it gets. See DisplayReply in :app.
                        pendingPin?.complete(PinResult.Accepted)
                        pendingPin = null
                    }
            }
        }
    }

    private fun scheduleReconnect() {
        val delay = retryDelayMs
        retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
        window.setTimeout({ connect(); null }, delay)
    }

    private companion object {
        const val PORT = 8080
        const val INITIAL_RETRY_MS = 1_000
        const val MAX_RETRY_MS = 15_000
        const val MAX_PENDING = 64
    }
}

/** Reads one top-level string/boolean field as text, without modelling the whole message. */
private fun kotlinx.serialization.json.JsonElement.asObjectField(name: String): String? =
    (this as? kotlinx.serialization.json.JsonObject)?.get(name)?.jsonPrimitive?.content
