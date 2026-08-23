package com.groovernapp.mosqueathan.data.remote

import android.os.Build
import android.util.Log
import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
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
 * The JSON is assembled by hand here. Step 4 of the KMP migration replaces this
 * with a `kotlinx.serialization` sealed hierarchy shared with the server, which is
 * also what removes `org.json` — an Android-only dependency.
 */
class RemoteActionSender(
    private val repository: MosqueConfigRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    fun send(action: String, build: JSONObject.() -> Unit = {}) {
        val config = repository.getConfig()
        if (config.connectivityMode != REMOTE_MODE || config.remoteTvIp.isEmpty()) return

        val target = config.remoteTvIp
        scope.launch {
            val payload = JSONObject().apply {
                put("action", action)
                put("deviceId", deviceId())
                build()
            }
            post(target, payload)
        }
    }

    private fun post(host: String, payload: JSONObject) {
        try {
            val connection = URL("http://$host:$PORT/api/action").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            if (connection.responseCode != 200) {
                Log.e(TAG, "TV rejected ${payload.optString("action")}: ${connection.responseCode}")
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

/**
 * The `update_config` payload: every field the display needs to mirror a settings
 * change.
 *
 * One definition rather than one per category. When this drifts from what
 * `SettingsServer.handleWebSocketMessage` reads, a setting changes on the
 * controller and silently does not on the TV — which is exactly the failure that
 * hand-copying this list into each ViewModel would have produced.
 */
fun JSONObject.pushConfig(config: com.groovernapp.mosqueathan.domain.MosqueConfig) {
    put("latitude", config.latitude)
    put("longitude", config.longitude)
    put("iqomahSubuh", config.iqomahSubuh)
    put("iqomahDzuhur", config.iqomahDzuhur)
    put("iqomahAshar", config.iqomahAshar)
    put("iqomahMaghrib", config.iqomahMaghrib)
    put("iqomahIsya", config.iqomahIsya)
    put("calculationMethodName", config.calculationMethodName)
    put("adzanLeadMinutes", config.adzanLeadMinutes)
    put("offsetImsak", config.offsetImsak)
    put("offsetSubuh", config.offsetSubuh)
    put("offsetSyuruq", config.offsetSyuruq)
    put("offsetDzuhur", config.offsetDzuhur)
    put("offsetAshar", config.offsetAshar)
    put("offsetMaghrib", config.offsetMaghrib)
    put("offsetIsya", config.offsetIsya)
    put("kemenagCityId", config.kemenagCityId)
    put("kemenagCityName", config.kemenagCityName)
    put("kemenagDuration", config.kemenagDuration)
    put("kemenagTargetDate", config.kemenagTargetDate)
    put("adzanFooterText", config.adzanFooterText)
    put("iqomahFooterText", config.iqomahFooterText)
    put("jumatEnabled", config.jumatEnabled)
    put("jumatStartTime", config.jumatStartTime)
    put("jumatEndTime", config.jumatEndTime)
    put("offsetJumat", config.offsetJumat)
    put("jumatStandbyOnly", config.jumatStandbyOnly)
    put("jumatWarningText", config.jumatWarningText)
    put("murottalEnabled", config.murottalEnabled)
    put("murottalStartDelay", config.murottalStartDelay)
    put("murottalAudioPaths", config.murottalAudioPaths)
    put("jumatSlideshowEnabled", config.jumatSlideshowEnabled)
    put("includeGlobalSlideshow", config.includeGlobalSlideshow)
    put("selectedGlobalSlideIds", config.selectedGlobalSlideIds)
    put("jumatTransitionType", config.jumatTransitionType)
    put("jumatSlideIntervalSeconds", config.jumatSlideIntervalSeconds)
    put("jumatSlideIntervalUnit", config.jumatSlideIntervalUnit)
    put("jumatVideoWaitUntilEnd", config.jumatVideoWaitUntilEnd)
}
