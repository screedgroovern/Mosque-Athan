package com.groovernapp.mosqueathan.web

import com.groovernapp.mosqueathan.protocol.HttpFetcher
import com.groovernapp.mosqueathan.protocol.HttpResult
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * [HttpFetcher] over the browser's own fetch.
 *
 * Written with an explicit continuation rather than a promise-await helper so it does not depend on
 * which await extensions the wasm coroutines artifact happens to expose.
 *
 * **A browser is not a phone here.** The one caller is the Kemenag city search, which hits
 * `api.myquran.com` — a different origin, so it succeeds only if that API sends CORS headers. The
 * Android app has no such restriction. If it turns out to be blocked, the fix is to route the
 * search through the display (which can reach the API unhindered) rather than to fight CORS from
 * the page; that needs a new action and is not done.
 */
class WebHttpFetcher : HttpFetcher {

    override suspend fun getText(url: String): HttpResult =
        suspendCancellableCoroutine { continuation ->
            fetchText(url) { body, error ->
                if (continuation.isActive) {
                    continuation.resume(
                        if (body != null) HttpResult.Success(body)
                        else HttpResult.Failure(error ?: "the request failed")
                    )
                }
            }
        }
}

private fun fetchText(url: String, onResult: (String?, String?) -> Unit) {
    fetchTextJs(url) { body, error -> onResult(body, error) }
}

@Suppress("UNUSED_PARAMETER")
private fun fetchTextJs(url: String, onResult: (String?, String?) -> Unit): Unit = js(
    """{
        fetch(url)
            .then(function (response) {
                if (!response.ok) { onResult(null, 'HTTP ' + response.status); return null; }
                return response.text();
            })
            .then(function (text) { if (text !== null) onResult(text, null); })
            .catch(function (e) { onResult(null, String(e && e.message ? e.message : e)); });
    }"""
)
