package com.groovernapp.mosqueathan.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * The adzan and iqomah footers are stored either as one line of text or as a JSON array
 * of lines, and this renders both as the single line the screen shows.
 *
 * The dual format is not a design choice, it is history: the setting began as a plain
 * string and the web panel later grew a multi-line editor that writes an array. Existing
 * installs still hold the old shape, so both have to keep working — the leading `[` is
 * what tells them apart.
 *
 * Anything unparseable is returned verbatim rather than throwing. A takmir who types a
 * stray bracket should see their own text on the wall, not an empty footer during adzan.
 */
fun joinFooterLines(raw: String): String {
    if (!raw.trim().startsWith("[")) return raw
    return try {
        Json.parseToJsonElement(raw).jsonArray
            .map { it.jsonPrimitive.content }
            .joinToString(SEPARATOR)
    } catch (e: Exception) {
        raw
    }
}

/** Wide spacing so two footer lines do not read as one sentence on a distant screen. */
private const val SEPARATOR = "   •   "
