package com.groovernapp.mosqueathan.domain

import com.groovernapp.mosqueathan.util.optionalBoolean
import com.groovernapp.mosqueathan.util.optionalString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/*
 * Parsing for the two api.myquran.com responses the app consumes.
 *
 * Here rather than in the repository because it is pure: a string in, data out, no Realm
 * and no platform. That is what makes it testable against a real captured response —
 * previously the only way to exercise it was to let the app hit the network and then
 * look at what landed in the database.
 */

/**
 * The prayer times for one month, from `/sholat/jadwal/{cityId}/{year}-{month}`.
 *
 * `data.jadwal` is an object *keyed by date* rather than a list, so it is a map and not
 * a record — the field names are data. Anything malformed yields an empty list rather
 * than throwing: a failed fetch must leave yesterday's stored schedule in place, because
 * the display still has to show prayer times tomorrow morning.
 */
fun parseKemenagMonth(cityId: String, body: String): List<KemenagSchedule> {
    val root = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return emptyList()
    if (!root.optionalBoolean("status")) return emptyList()
    val jadwal = (root["data"] as? JsonObject)?.get("jadwal") as? JsonObject ?: return emptyList()

    return jadwal.mapNotNull { (dateString, element) ->
        val day = element as? JsonObject ?: return@mapNotNull null
        KemenagSchedule(
            dateString = dateString,
            cityId = cityId,
            imsak = day.optionalString("imsak"),
            subuh = day.optionalString("subuh"),
            terbit = day.optionalString("terbit"),
            dhuha = day.optionalString("dhuha"),
            dzuhur = day.optionalString("dzuhur"),
            ashar = day.optionalString("ashar"),
            maghrib = day.optionalString("maghrib"),
            isya = day.optionalString("isya")
        )
    }
}

/**
 * City id and name pairs, from `/sholat/kabkota/cari/{query}`.
 *
 * Entries missing either field are dropped rather than surfaced as blank rows the takmir
 * could select by accident.
 */
fun parseKemenagCities(body: String): List<Pair<String, String>> {
    val root = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return emptyList()
    if (!root.optionalBoolean("status")) return emptyList()
    val data = root["data"] as? JsonArray ?: return emptyList()

    return data.filterIsInstance<JsonObject>().mapNotNull { item ->
        val id = item.optionalString("id")
        val name = item.optionalString("lokasi")
        if (id.isNotEmpty() && name.isNotEmpty()) id to name else null
    }
}
