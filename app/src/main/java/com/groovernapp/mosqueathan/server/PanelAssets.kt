package com.groovernapp.mosqueathan.server

import java.net.URLDecoder

/**
 * Maps a `/panel/...` request onto a file inside the `panel` asset directory.
 *
 * Returns null for anything that tries to leave it. Assets are not a filesystem, but
 * `AssetManager` resolves `..` happily, so this is the only thing standing between a request from
 * the mosque wifi and the rest of the APK's assets — which is why it is a function with tests
 * rather than three lines inside a request handler.
 *
 * Decoding happens *before* the check, deliberately: `%2e%2e%2f` is a traversal that only looks
 * like one after decoding, and checking the raw string would miss it.
 */
internal fun panelAssetPath(path: String): String? {
    val relative = path.substringBefore('?').removePrefix("/panel").removePrefix("/")
    val decoded = try {
        URLDecoder.decode(relative, "UTF-8")
    } catch (_: Exception) {
        return null
    }
    if (decoded.isEmpty()) return "panel/index.html"
    if (decoded.contains("..")) return null
    if (decoded.startsWith("/") || decoded.startsWith("\\")) return null
    // A backslash is a separator on some platforms and a literal on others; a panel asset never
    // contains one, so refusing is free and closes the difference.
    if (decoded.contains("\\")) return null
    return "panel/$decoded"
}
