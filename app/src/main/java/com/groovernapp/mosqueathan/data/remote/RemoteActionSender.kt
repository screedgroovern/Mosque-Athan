package com.groovernapp.mosqueathan.data.remote

import android.os.Build
import android.util.Log
import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.protocol.RemoteActionSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import com.groovernapp.mosqueathan.protocol.RemoteActions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject as KJsonObject
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pushes a settings change to the display this device is controlling.
 *
 * Only does anything in `REMOTE` mode with a known TV address — on the display
 * itself there is nobody to tell.
 *
 * Extracted from `SettingsViewModel`, where it was a method. That mattered: as
 * settings moved into per-category ViewModels, each of those wrote straight to the
 * repository and the push was silently skipped, so on a controller device changing
 * an offset or a PIN stopped reaching the TV. Making it an injectable collaborator
 * is what lets every category keep it.
 *
 * The JSON is assembled by hand, but with kotlinx.serialization rather than
 * `org.json`, which is Android-only and therefore cannot move to `commonMain`. A
 * sealed hierarchy of typed actions is the eventual shape; this is the step that
 * removes the platform dependency without also rewriting all ~50 payloads at once.
 */
class RemoteActionSender(
    private val repository: ConfigRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : RemoteActionSink {

    /**
     * What this device knows about its standing with the display.
     *
     * Mirrors `DisplayLink.Status` in `:webApp` rather than reusing `LinkStatus` from `:ui:design`,
     * so the data layer does not depend on the UI layer; the host maps between them, exactly as
     * `main.kt` does for the browser. Starts [Unknown], which renders no status chip at all — the
     * honest answer before anything has been attempted.
     */
    enum class Status { Unknown, Connected, NeedsApproval, Offline }

    private val _status = MutableStateFlow(Status.Unknown)
    val status: StateFlow<Status> = _status.asStateFlow()

    /** The display this device has registered with, so a changed address registers again. */
    private var registeredWith: String? = null
    private val registration = Mutex()

    /**
     * Registers with the display now, rather than waiting for the first settings change.
     *
     * Without this the gate could not appear at all until the takmir touched something: [status]
     * starts [Status.Unknown], and only [send] and [verifyPin] register. So a controller opened
     * against a PIN-protected display showed the panel full of its own stale values, with no
     * status chip and no PIN prompt — and the first control pressed was refused silently, *then*
     * the gate appeared. Asking on arrival is what makes "this display wants a PIN" the first
     * thing the takmir sees instead of the second.
     *
     * Safe to call repeatedly and on a display: [target] returns null when this device is not a
     * controller, so there is nobody to tell and nothing happens.
     */
    fun connect() {
        val target = target() ?: return
        scope.launch { ensureRegistered(target) }
    }

    /**
     * Edits that never reached the display, as a sentence for [com.groovernapp.mosqueathan.ui.design.MessageHost].
     *
     * Before this, [send] posted, logged a failure to logcat and returned: on a phone in `REMOTE`
     * mode with the display unreachable an edit was lost with nothing on screen, while the browser
     * at least held it. This does not hold anything back — see [deliveryNotice] for why there is
     * deliberately no queue — it only stops the loss being silent.
     */
    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    override fun send(action: String, build: JsonObjectBuilder.() -> Unit) {
        val target = target() ?: return
        scope.launch {
            ensureRegistered(target)
            deliveryNotice(request(target, action) { build() })?.let(_notices::tryEmit)
        }
    }

    /**
     * Registers this device with the display, once per address.
     *
     * **Nothing did this before, and the consequence was invisible until a PIN was set.** The
     * display's authorization gate is `deviceId != null && (pinCode.isEmpty() || the device row
     * says allowed)`, and `POST /api/action` maps the body's `deviceId` onto a throwaway
     * connection — so an unregistered controller was fine against a display with no PIN and had
     * *every* action refused by one with a PIN, each refusal logged on the display and nowhere
     * else. It also meant a device the display had never seen could not be the device an admin
     * approves, which is what made `verify_pin` unreachable from here.
     *
     * Registering is idempotent on the display (`registerDevice` creates or updates one row), so
     * the guard is about not paying for a round trip per action, not about correctness.
     */
    private suspend fun ensureRegistered(target: String) = registration.withLock {
        if (registeredWith == target && _status.value == Status.Connected) return@withLock
        register(target)
    }

    /**
     * Asks the display whether it is still there, and updates [status] with the answer.
     *
     * [status] otherwise only changes when something is sent, so a display that was closed after
     * the last action left a controller showing "Connected" for as long as nobody touched a
     * setting. Registering is idempotent on the display and cheap, which makes it an honest
     * heartbeat: an answer means it is reachable *and* still accepts this device.
     *
     * Called on a timer by the host while settings is open; does nothing on a display.
     */
    suspend fun checkLink() {
        val target = target() ?: return
        registration.withLock { register(target) }
    }

    private suspend fun register(target: String) {
        val reply = request(target, RemoteActions.REGISTER) {
            put("deviceName", Build.MODEL)
        }
        if (reply == null) {
            _status.value = Status.Offline
            registeredWith = null
            return
        }
        registeredWith = target
        _status.value = if (DisplayReply.accepted(reply, "isAllowed")) Status.Connected else Status.NeedsApproval
    }

    /**
     * Sends [pin] to the display and reports what it said.
     *
     * Registers first, because the display reads the device id off the connection this request
     * arrives on and refuses `verify_pin` outright without one — the same gate every other action
     * passes through, and the reason this could not work before registration existed.
     */
    suspend fun verifyPin(pin: String): PinOutcome {
        val target = target() ?: return PinOutcome.Unreachable("no display address")
        ensureRegistered(target)
        val reply = request(target, RemoteActions.VERIFY_PIN) { put("pin", pin) }
            ?: return PinOutcome.Unreachable(null)

        return if (DisplayReply.accepted(reply, "success")) {
            _status.value = Status.Connected
            PinOutcome.Accepted
        } else {
            PinOutcome.Rejected(DisplayReply.error(reply))
        }
    }

    /** What the display made of a PIN. Mapped to the UI's `PinResult` by the host. */
    sealed interface PinOutcome {
        data object Accepted : PinOutcome
        data class Rejected(val message: String?) : PinOutcome
        data class Unreachable(val reason: String?) : PinOutcome
    }

    /** The display's address, or null when this device is not a controller. */
    private fun target(): String? {
        val config = repository.getConfig()
        if (config.connectivityMode != REMOTE_MODE || config.remoteTvIp.isEmpty()) return null
        return config.remoteTvIp
    }

    /**
     * Posts one action and returns the display's reply, or null if it never arrived.
     *
     * This used to discard the body and only log a non-200. Reading it is what makes registration
     * and PIN entry possible at all: `POST /api/action` answers with whatever the handler sent on
     * its throwaway connection, so `registration_result` and `pin_verification_result` come back
     * as the response to the very request that asked.
     */
    private suspend fun request(
        host: String,
        action: String,
        build: JsonObjectBuilder.() -> Unit
    ): KJsonObject? = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("action", action)
            put("deviceId", deviceId())
            build()
        }
        try {
            val connection = URL("http://$host:$PORT/api/action").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            if (connection.responseCode != 200) {
                Log.e(TAG, "TV rejected $action: ${connection.responseCode}")
                return@withContext null
            }
            val body = connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            runCatching { Json.parseToJsonElement(body) as? KJsonObject }.getOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Could not reach the TV at $host", e)
            _status.value = Status.Offline
            null
        }
    }

    private fun deviceId(): String = "remote_${Build.MODEL.replace(" ", "_")}"

    private companion object {
        const val TAG = "RemoteSync"
        const val REMOTE_MODE = "REMOTE"
        const val PORT = 8080
        const val TIMEOUT_MS = 3_000
    }
}
