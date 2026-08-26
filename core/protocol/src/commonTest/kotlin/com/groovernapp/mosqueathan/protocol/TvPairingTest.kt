package com.groovernapp.mosqueathan.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The scanned payload arrives from outside the app — a camera pointed at whatever is on
 * the wall — so the cases that matter here are the malformed ones. The parser used to be
 * `android.net.Uri.parse(...).host ?: rawText`, which had no tests at all because it
 * needed a device to run.
 */
class TvPairingTest {

    @Test
    fun urlIsTheAddressThePanelIsServedOn() {
        assertEquals("http://192.168.1.15:8080", TvPairing.url("192.168.1.15"))
    }

    @Test
    fun theDisplaysOwnQrCodeRoundTrips() {
        val ip = "192.168.1.15"
        assertEquals(ip, TvPairing.addressFrom(TvPairing.url(ip)))
    }

    @Test
    fun acceptsABareAddressTypedOrReadOffARouter() {
        assertEquals("192.168.1.15", TvPairing.addressFrom("192.168.1.15"))
        assertEquals("192.168.1.15", TvPairing.addressFrom("192.168.1.15:8080"))
    }

    @Test
    fun acceptsAHostnameAndKeepsItsCase() {
        assertEquals("tv-masjid.local", TvPairing.addressFrom("http://tv-masjid.local:8080/"))
        assertEquals("TV.local", TvPairing.addressFrom("HTTP://TV.local"))
    }

    @Test
    fun ignoresSurroundingWhitespaceAndAnyPath() {
        assertEquals("192.168.1.15", TvPairing.addressFrom("  http://192.168.1.15:8080/  "))
        assertEquals("192.168.1.15", TvPairing.addressFrom("http://192.168.1.15:8080/api/get_config"))
        assertEquals("192.168.1.15", TvPairing.addressFrom("http://192.168.1.15?x=1"))
        assertEquals("192.168.1.15", TvPairing.addressFrom("http://192.168.1.15#top"))
    }

    @Test
    fun httpsIsAcceptedEvenThoughTheServerDoesNotSpeakIt() {
        // The address is what is kept, not the scheme, so a display behind a proxy that
        // does terminate TLS still pairs.
        assertEquals("192.168.1.20", TvPairing.addressFrom("https://192.168.1.20:8443"))
    }

    @Test
    fun theRequestGoesToTheHostAfterTheAt() {
        assertEquals("evil.example", TvPairing.addressFrom("http://tv.local@evil.example/"))
    }

    @Test
    fun nothingUsableIsNull() {
        assertNull(TvPairing.addressFrom(""))
        assertNull(TvPairing.addressFrom("   "))
        assertNull(TvPairing.addressFrom("http://"))
        assertNull(TvPairing.addressFrom("http:///api/get_config"))
        assertNull(TvPairing.addressFrom("Jadwal Sholat Masjid Al-Ikhlas"))
        assertNull(TvPairing.addressFrom("192.168.1.15:"))
    }

    @Test
    fun aQrCodeDoingSomethingElseIsNotAnAddress() {
        assertNull(TvPairing.addressFrom("javascript:alert(1)"))
        assertNull(TvPairing.addressFrom("ftp://192.168.1.15"))
        assertNull(TvPairing.addressFrom("tel:+622112345678"))
        assertNull(TvPairing.addressFrom("file:///etc/passwd"))
    }

    @Test
    fun ipv6IsRejectedBecauseNothingDownstreamCouldUseIt() {
        assertNull(TvPairing.addressFrom("http://[fe80::1]:8080"))
    }

    @Test
    fun anAbsurdlyLongPayloadCannotBecomeASetting() {
        assertNull(TvPairing.addressFrom("a".repeat(254)))
        assertEquals("a".repeat(253), TvPairing.addressFrom("a".repeat(253)))
    }
}
