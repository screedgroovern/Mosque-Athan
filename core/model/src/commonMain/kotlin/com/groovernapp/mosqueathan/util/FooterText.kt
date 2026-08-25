package com.groovernapp.mosqueathan.util

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/*
 * The adzan and iqomah footers are stored either as one line of text or as a JSON array
 * of lines. That is history rather than design: the setting began as a plain string and
 * the web panel later grew a multi-line editor that writes an array. Installed devices
 * still hold the old shape, so both have to keep working, and a leading `[` is what
 * tells them apart.
 *
 * [footerLines] is the only place that decides. The editor reads and writes lines,
 * the display joins them, and neither repeats the format sniffing — which they did,
 * in three separate functions across two modules, two of them using `org.json`.
 */

/**
 * The footer's lines, whichever of the two stored shapes [raw] is in.
 *
 * Anything unparseable comes back as a single line holding [raw] verbatim, rather than
 * throwing or emptying. A takmir who types a stray bracket should see their own words on
 * the wall during adzan, not a blank bar.
 */
fun footerLines(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    if (!raw.trim().startsWith("[")) return listOf(raw)
    return try {
        Json.parseToJsonElement(raw).jsonArray.map { it.jsonPrimitive.content }
    } catch (e: Exception) {
        listOf(raw)
    }
}

/**
 * The stored form of [lines] — always the JSON array shape, since that is what the
 * editor produces and what round-trips without ambiguity.
 */
fun encodeFooterLines(lines: List<String>): String =
    Json.encodeToString(ListSerializer(String.serializer()), lines)

/** The footer as the display shows it: every line on one bar. */
fun joinFooterLines(raw: String): String = footerLines(raw).joinToString(SEPARATOR)

/** Wide spacing so two footer lines do not read as one sentence on a distant screen. */
private const val SEPARATOR = "   •   "
