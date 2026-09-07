package com.groovernapp.mosqueathan.data.remote

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reading the display's answer to `register` and `verify_pin`.
 *
 * ### Why this is not one line of `optBoolean`
 *
 * `POST /api/action` runs the WebSocket handler against a throwaway connection and returns
 * whatever that handler sent **last**. Both of these handlers send their result and then, on
 * success, the whole state — so the reply an accepted device gets is the *state*, and the
 * `isAllowed` or `success` it was supposed to read is not in it.
 *
 * The four shapes, measured against the television rather than assumed:
 *
 * | Request | Reply |
 * |---|---|
 * | `register`, accepted | `{"type":"state", …}` |
 * | `register`, blocked | `{"type":"registration_result","isAllowed":false,"hasPin":true}` |
 * | `verify_pin`, wrong | `{"type":"pin_verification_result","success":false,"error":"…"}` |
 * | `verify_pin`, correct | `{"type":"state", …}` |
 *
 * So **a snapshot arriving is the proof of acceptance** — the display only ever sends its state to
 * a device it has accepted, the same inference `DisplayLink` makes on the socket. Reading the flag
 * alone reports a blocked device on every successful registration and tells a takmir their correct
 * PIN was wrong, which is the version of this that was written first.
 *
 * Extracted from [RemoteActionSender] so the rule can be tested against those four shapes. It is
 * the kind of decision that is invisible at runtime — a wrongly-blocked controller looks exactly
 * like a display that really did refuse it.
 */
internal object DisplayReply {

    /** The reply type the display labelled its answer with, or null if it did not. */
    fun type(reply: JsonObject): String? =
        runCatching { reply["type"]?.jsonPrimitive?.content }.getOrNull()

    /** A boolean the display sent; false when absent, null, or not a boolean. */
    fun flag(reply: JsonObject, name: String): Boolean =
        runCatching { reply[name]?.jsonPrimitive?.content == "true" }.getOrDefault(false)

    /** The display's own wording for a refusal, where it gave any. */
    fun error(reply: JsonObject): String? =
        runCatching { reply["error"]?.jsonPrimitive?.content }.getOrNull()

    /** Whether [reply] means this device is accepted. See the table above. */
    fun accepted(reply: JsonObject, flagName: String): Boolean =
        type(reply) == STATE || flag(reply, flagName)

    /** The reply type that means accepted, because it is only ever sent to such a device. */
    const val STATE = "state"
}
