package com.groovernapp.mosqueathan.web

import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * The browser facts the transport needs, in one file so "what the web build assumes about its
 * environment" is somewhere you can read.
 */

/**
 * Where the display is.
 *
 * In production the display serves this page, so the host it was fetched from *is* the display.
 * `?tv=<address>` overrides that, which is what makes development possible at all: during
 * development the bundle is served from a laptop, and without the override the panel would try to
 * control the laptop.
 */
fun displayHost(): String {
    val override = Regex("[?&]tv=([^&#]+)").find(window.location.search)?.groupValues?.get(1)
    return override?.takeIf { it.isNotBlank() } ?: window.location.hostname
}

/**
 * This browser's identity to the display, stable across reloads.
 *
 * It has to persist: the display keys its allow-list on the device id, so a new id on every reload
 * would ask the takmir to approve the same phone again and again, and would fill the connected
 * devices list with duplicates. localStorage is per-origin and survives a republish, which is
 * exactly the lifetime wanted. A private window gets a fresh id, and that is correct — it is a
 * different client as far as approval goes.
 */
fun storedDeviceId(): String {
    val key = "mosqueathan.deviceId"
    val existing = runCatching { window.localStorage.getItem(key) }.getOrNull()
    if (!existing.isNullOrBlank()) return existing
    val fresh = "web-" + randomToken()
    runCatching { window.localStorage.setItem(key, fresh) }
    return fresh
}

/** A name a person can recognise in the display's device list. */
fun browserDeviceName(): String = "Web Panel"

/**
 * A short random token.
 *
 * **Not `crypto.randomUUID()`.** That is secure-context-only, and this panel is served over plain
 * http on a mosque LAN, so `window.isSecureContext` is false and `crypto.randomUUID` is
 * *undefined* — calling it threw a TypeError before Koin had finished building the graph, which
 * blanked the whole page. Measured on the device, not inferred: `getRandomValues` is available in
 * an insecure context and `randomUUID` is not.
 *
 * `Math.random` is the fallback rather than an error because this identifies a browser to a
 * display on the same wifi; it is not a secret.
 */
private fun randomToken(): String = js(
    """{
        try {
            if (window.crypto && window.crypto.getRandomValues) {
                var bytes = new Uint8Array(4);
                window.crypto.getRandomValues(bytes);
                return Array.prototype.map.call(bytes, function (b) {
                    return ('0' + b.toString(16)).slice(-2);
                }).join('');
            }
        } catch (e) { /* fall through */ }
        return Math.random().toString(36).slice(2, 10);
    }"""
)

/** `MessageEvent.data` without modelling the whole event type. */
internal fun Event.asMessageData(): Any? = messageData(this)

@Suppress("UNUSED_PARAMETER")
private fun messageData(event: Event): String? = js("typeof event.data === 'string' ? event.data : null")

/**
 * Runs [onVisible] each time the page becomes visible again.
 *
 * `visibilitychange` rather than `focus`: on Android the file picker and the app switcher both hide
 * the page without taking window focus in a way `focus` reports reliably, and `pageshow` fires only
 * for navigations. This is the event that actually corresponds to "the user is looking at us again".
 */
fun onPageVisible(onVisible: () -> Unit) {
    registerVisibilityListener(onVisible)
}

@Suppress("UNUSED_PARAMETER")
private fun registerVisibilityListener(onVisible: () -> Unit): Unit = js(
    """{
        document.addEventListener('visibilitychange', function () {
            if (document.visibilityState === 'visible') { onVisible(); }
        });
    }"""
)
