package com.groovernapp.mosqueathan.protocol

/**
 * Fetches a URL as text.
 *
 * Two things in this app need it — the Kemenag city search and pulling settings from
 * another device — and both want the same thing: a GET, a short timeout, and the body.
 * An interface so the settings ViewModels can be built without an HTTP stack; Android
 * uses `HttpURLConnection`, and a browser controller cannot.
 */
interface HttpFetcher {
    suspend fun getText(url: String): HttpResult
}

/**
 * A result rather than a nullable string, so the reason survives.
 *
 * That matters here specifically: a failed sync shows the takmir a message, and "Sync
 * failed" alone does not distinguish a wrong IP from a display that is switched off.
 * Collapsing this to `String?` would quietly make that message useless.
 */
sealed interface HttpResult {
    data class Success(val body: String) : HttpResult

    /** [reason] is for a person to read, so it goes in the message they are shown. */
    data class Failure(val reason: String) : HttpResult
}

/**
 * Percent-encodes [value] for use in a URL path segment.
 *
 * Hand-written because `java.net.URLEncoder` is JVM-only. It is deliberately *not* that
 * function: `URLEncoder` implements form encoding, which turns a space into `+` — correct
 * in a query string, wrong in a path, and the Kemenag city search puts the query in the
 * path. The previous code used it anyway, so a search for "kota bandung" sent a literal
 * `+`. Encoding the space as %20 is what the API actually expects.
 */
fun urlEncodePathSegment(value: String): String {
    val unreserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"
    return buildString {
        value.encodeToByteArray().forEach { byte ->
            val ch = byte.toInt().toChar()
            if (ch in unreserved) append(ch)
            else append('%').append(HEX[(byte.toInt() shr 4) and 0xF]).append(HEX[byte.toInt() and 0xF])
        }
    }
}

private const val HEX = "0123456789ABCDEF"
