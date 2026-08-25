package com.groovernapp.mosqueathan.util

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/*
 * Readers that behave like `org.json`'s `optInt` / `optString` / `optBoolean` /
 * `optDouble`: a missing key, an explicit null, or a value of the wrong type all yield
 * the default rather than throwing.
 *
 * That leniency is load-bearing rather than lazy, and it is why replacing `org.json`
 * with `Json.decodeFromString<SomeDataClass>` would be a behaviour change rather than a
 * refactor. Two reasons:
 *
 *  - The display's own `/api/get_config` omits keys. `connectivityMode` is absent from
 *    it today, and that is not a bug to be fixed here.
 *  - A controller syncing from a display running slightly older code will meet fields it
 *    does not recognise and miss fields it expects. Failing the whole sync over one of
 *    them would turn a cosmetic mismatch into an outage, on devices that are updated by
 *    hand and not at the same time.
 *
 * Strict deserialization belongs on the typed protocol messages of `core:protocol`, where
 * both ends are versioned together. These are for the untyped edges.
 */

private fun JsonObject.primitiveOrNull(key: String): JsonPrimitive? = this[key] as? JsonPrimitive

/** The string at [key], or `""`. */
fun JsonObject.optionalString(key: String): String = primitiveOrNull(key)?.contentOrNull ?: ""

/** The int at [key], or `0`. */
fun JsonObject.optionalInt(key: String): Int = primitiveOrNull(key)?.intOrNull ?: 0

/** The double at [key], or `0.0`. */
fun JsonObject.optionalDouble(key: String): Double = primitiveOrNull(key)?.doubleOrNull ?: 0.0

/** The boolean at [key], or `false`. */
fun JsonObject.optionalBoolean(key: String): Boolean = primitiveOrNull(key)?.booleanOrNull ?: false
