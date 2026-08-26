package com.groovernapp.mosqueathan.protocol

/**
 * The address a display shows as a QR code and a controller reads back off it.
 *
 * Both halves live here because they are one format with two ends, and the two ends used
 * to be written in different modules with nothing connecting them — the display composed
 * `"http://$ip:8080"` by hand and the controller pulled it apart with `android.net.Uri`.
 * That is the same drift the shared action names in [RemoteActions] were introduced to
 * kill.
 *
 * [addressFrom] is plain Kotlin rather than a URL parser because it needs less than one:
 * the result is written into `remoteTvIp`, and every consumer of that field builds its
 * own `http://…:8080/…` around it. So the only question a scan has to answer is "which
 * host?", and answering it here means the settings pane can move to `commonMain` — and
 * that a hostile or mistyped payload is a test case rather than a crash on a takmir's
 * phone.
 */
object TvPairing {

    /**
     * The port `SettingsServer` listens on.
     *
     * Four other sites still spell it out — `MainActivity.start(8080)`,
     * `SettingsServer.start`, `RemoteActionSender.PORT` and the sync URL in
     * `SettingsViewModel` — and folding those in is a separate change; this constant
     * exists so the QR code and its parser cannot disagree about it.
     */
    const val CONTROL_PORT = 8080

    /** The exact URL the panel is served on, and the exact string the QR code carries. */
    fun url(ipAddress: String): String = "http://$ipAddress:$CONTROL_PORT"

    /**
     * The host an untrusted [payload] points at, or null if it does not name one.
     *
     * Accepts what a takmir can plausibly be holding: the display's own pairing URL, the
     * same with a trailing path, and a bare address read off a router's admin page. Null
     * for everything else, which is a change in behaviour and a deliberate one — the old
     * code fell back to the raw scanned text, so scanning a poster stored the poster's
     * text as the TV's address and the next sync failed with an unexplainable message.
     *
     * IPv6 is rejected rather than handled. Nothing in this app can talk to an IPv6
     * literal anyway: every consumer interpolates the address into `http://$ip:8080`
     * without the brackets that syntax requires.
     */
    fun addressFrom(payload: String): String? {
        var rest = payload.trim()
        if (rest.isEmpty()) return null

        val schemeEnd = rest.indexOf("://")
        if (schemeEnd >= 0) {
            val scheme = rest.substring(0, schemeEnd).lowercase()
            // A `tel:` or `javascript:` payload is a QR code doing something other than
            // naming a display, and following it would be the interesting kind of bug.
            if (scheme != "http" && scheme != "https") return null
            rest = rest.substring(schemeEnd + 3)
        }

        // The authority only: a path, query or fragment is not part of the host.
        rest = rest.takeWhile { it != '/' && it != '?' && it != '#' }
        // `user@host` — the request goes to the half after the `@`, so that is the half
        // worth keeping. Dropping it is what stops `http://tv.local@evil.example` from
        // reading as the mosque's own display.
        rest = rest.substringAfterLast('@')

        val colon = rest.indexOf(':')
        if (colon >= 0) {
            // Digits after the colon means a port, which is dropped: the controller
            // appends CONTROL_PORT itself. Anything else means this was never an
            // address — it is how `javascript:alert(1)` is caught, since that carries no
            // `://` to reject above.
            val port = rest.substring(colon + 1)
            if (port.isEmpty() || !port.all { it.isDigit() }) return null
            rest = rest.substring(0, colon)
        }

        if (rest.isEmpty() || rest.length > MAX_HOST_LENGTH) return null
        return rest.takeIf { host -> host.all(::isHostChar) }
    }

    /** Hostname characters, which covers a dotted-quad IPv4 address as well. */
    private fun isHostChar(ch: Char): Boolean =
        ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch == '.' || ch == '-'

    /** The DNS limit, so that a megabyte-long payload cannot become a setting. */
    private const val MAX_HOST_LENGTH = 253
}
