package com.groovernapp.mosqueathan.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * The line between "the edit arrived" and "the edit is gone".
 *
 * `RemoteActionSender` itself cannot be driven from a JVM test — it opens real sockets and reads
 * `android.os.Build` — so the rule that decides whether a takmir is told lives here, where it can.
 * What is guarded is the regression that mattered: `post` used to catch, log and return, and an
 * edit made against an unreachable display vanished without a trace.
 */
class DeliveryNoticeTest {

    private lateinit var original: Locale

    @Before
    fun rememberLocale() {
        original = Locale.getDefault()
    }

    @After
    fun restoreLocale() = Locale.setDefault(original)

    private fun reply(text: String) = Json.parseToJsonElement(text) as JsonObject

    @Test
    fun aMissingReplyIsAnEditThatDidNotArrive() {
        assertNotNull(deliveryNotice(null))
    }

    @Test
    fun anyReplyMeansTheDisplayReceivedIt() {
        assertNull(deliveryNotice(reply("""{"type":"state","config":{}}""")))
        assertNull(deliveryNotice(reply("""{"type":"registration_result","isAllowed":false}""")))
    }

    @Test
    fun theNoticeSaysTheChangeWasNotDelivered() {
        Locale.setDefault(Locale.ENGLISH)
        assertTrue(deliveryNotice(null)!!.startsWith("Not delivered"))
    }

    @Test
    fun theNoticeIsInIndonesianWhenTheDeviceIs() {
        Locale.setDefault(Locale.forLanguageTag("id-ID"))
        assertTrue(deliveryNotice(null)!!.startsWith("Tidak terkirim"))
    }

    @Test
    fun theNoticeNeverPromisesARetry() {
        // The browser's notice says "will be sent when the display is back" because it holds the
        // action. This sender does not, and promising it would be a lie that duplicates rows the
        // day someone adds the queue without an offline replica.
        for (locale in listOf(Locale.ENGLISH, Locale.forLanguageTag("id-ID"))) {
            Locale.setDefault(locale)
            val text = deliveryNotice(null)!!.lowercase()
            assertFalse(text, "will be sent" in text || "akan dikirim" in text)
        }
    }

    @Test
    fun theTwoLanguagesDiffer() {
        Locale.setDefault(Locale.ENGLISH)
        val en = deliveryNotice(null)
        Locale.setDefault(Locale.forLanguageTag("id-ID"))
        assertFalse(en == deliveryNotice(null))
    }
}
