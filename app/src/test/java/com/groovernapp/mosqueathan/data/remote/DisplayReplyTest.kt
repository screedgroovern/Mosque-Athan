package com.groovernapp.mosqueathan.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The four replies a display actually sends, taken verbatim off the television.
 *
 * These are not invented fixtures. Each was captured from `POST /api/action` against the AQUA TV
 * on 2026-09-08, which is the only way this rule could have been got right: the first version read
 * `isAllowed` and `success` directly, and both success paths do not contain them — the handler
 * sends its result and then the whole state, and the HTTP endpoint returns the last message only.
 *
 * The failure that version produced is worth stating, because it is invisible from the inside: a
 * controller the display had just accepted reported that it needed approval, and a takmir typing
 * the correct PIN was told it was wrong.
 */
class DisplayReplyTest {

    private fun reply(text: String) = Json.parseToJsonElement(text) as JsonObject

    private val registerAccepted = reply(
        """{"type":"state","config":{"adzanLeadMinutes":3},"announcements":[],"devices":[]}"""
    )
    private val registerBlocked = reply(
        """{"type":"registration_result","isMaster":false,"isAllowed":false,"hasPin":true}"""
    )
    private val registerAllowedOnSocket = reply(
        """{"type":"registration_result","isMaster":true,"isAllowed":true,"hasPin":false}"""
    )
    private val pinWrong = reply(
        """{"type":"pin_verification_result","success":false,"error":"PIN salah. Silakan coba lagi."}"""
    )
    private val pinCorrect = reply(
        """{"type":"state","config":{"adzanLeadMinutes":3}}"""
    )

    @Test
    fun aStateReplyMeansTheDisplayAcceptedThisDevice() {
        // The reply to a *successful* register, because the registration_result it also sent was
        // overwritten. This is the case the first version got wrong.
        assertTrue(DisplayReply.accepted(registerAccepted, "isAllowed"))
        assertTrue(DisplayReply.accepted(pinCorrect, "success"))
    }

    @Test
    fun aRefusalIsReadFromItsOwnFlag() {
        assertFalse(DisplayReply.accepted(registerBlocked, "isAllowed"))
        assertFalse(DisplayReply.accepted(pinWrong, "success"))
    }

    @Test
    fun anExplicitAllowedFlagStillCounts() {
        // The WebSocket path does deliver the result, so the flag must keep working — this is not
        // a rule that replaces the flag, it is one that covers the case the flag is missing.
        assertTrue(DisplayReply.accepted(registerAllowedOnSocket, "isAllowed"))
    }

    @Test
    fun theDisplaysOwnWordingIsCarriedThrough() {
        // Preferred over a local string because the display is the side that knows why. Today that
        // is only a wrong PIN; a revoked device would arrive the same way.
        assertEquals("PIN salah. Silakan coba lagi.", DisplayReply.error(pinWrong))
        assertNull(DisplayReply.error(pinCorrect))
    }

    @Test
    fun aMissingOrOddlyTypedFlagIsNotAnAcceptance() {
        // The display is on the mosque wifi and this build talks to whatever answers on port 8080.
        assertFalse(DisplayReply.accepted(reply("""{"type":"registration_result"}"""), "isAllowed"))
        assertFalse(DisplayReply.accepted(reply("""{"type":"x","isAllowed":"yes"}"""), "isAllowed"))
        assertFalse(DisplayReply.accepted(reply("""{"type":"x","isAllowed":1}"""), "isAllowed"))
        assertFalse(DisplayReply.accepted(reply("{}"), "isAllowed"))
    }

    @Test
    fun anErrorReplyIsNeverAnAcceptance() {
        // What every action from an unapproved device gets, measured on the television.
        val refused = reply("""{"type":"error","message":"Akses ditolak. Silakan masukkan PIN terlebih dahulu."}""")
        assertFalse(DisplayReply.accepted(refused, "isAllowed"))
        assertFalse(DisplayReply.accepted(refused, "success"))
    }
}
