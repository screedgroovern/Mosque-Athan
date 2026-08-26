package com.groovernapp.mosqueathan.data.remote

import android.os.Build
import android.util.Log
import com.groovernapp.mosqueathan.domain.ConfigRepository
import com.groovernapp.mosqueathan.protocol.RemoteActionSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    override fun send(action: String, build: JsonObjectBuilder.() -> Unit) {
        val config = repository.getConfig()
        if (config.connectivityMode != REMOTE_MODE || config.remoteTvIp.isEmpty()) return

        val target = config.remoteTvIp
        scope.launch {
            val payload = buildJsonObject {
                put("action", action)
                put("deviceId", deviceId())
                build()
            }
            post(target, payload)
        }
    }

    private fun post(host: String, payload: JsonObject) {
        try {
            val connection = URL("http://$host:$PORT/api/action").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            if (connection.responseCode != 200) {
                val name = payload["action"]?.jsonPrimitive?.content
                Log.e(TAG, "TV rejected $name: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not reach the TV at $host", e)
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
