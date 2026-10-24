package com.groovernapp.mosqueathan.data.remote

import com.groovernapp.mosqueathan.protocol.HttpFetcher
import com.groovernapp.mosqueathan.protocol.HttpResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * The Android [HttpFetcher], on `HttpURLConnection`.
 *
 * Deliberately no dependency: adding OkHttp for two GETs would be more weight than the
 * feature. It switches to IO itself so callers cannot forget, which is what the previous
 * hand-rolled version in `PrayerScheduleViewModel` did forget — it ran the connect on
 * whatever dispatcher the caller happened to be on.
 *
 * The timeout is short on purpose. Both uses are interactive: a takmir typing a city name,
 * or pressing sync and waiting. A minute-long stall is worse than a failure they can
 * retry.
 */
class HttpUrlFetcher : HttpFetcher {

    override suspend fun getText(url: String): HttpResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                connect()
            }
            if (connection.responseCode != 200) {
                HttpResult.Failure("HTTP ${connection.responseCode}")
            } else {
                HttpResult.Success(connection.inputStream.bufferedReader().use { it.readText() })
            }
        } catch (e: Exception) {
            HttpResult.Failure(e.message ?: e::class.simpleName ?: "unknown error")
        } finally {
            connection?.disconnect()
        }
    }

    /** Impatient on purpose: this is asked of every remembered display, most of them probably off. */
    override suspend fun isReachable(url: String): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = PROBE_TIMEOUT_MS
                readTimeout = PROBE_TIMEOUT_MS
            }
            connection.responseCode == 200
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000
        const val PROBE_TIMEOUT_MS = 2_000
    }
}
