package com.groovernapp.mosqueathan.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The remembered-displays list: most recent first, one entry per address, a name once learned is
 * kept. A list that duplicated an address or forgot its name would be the first thing a takmir
 * notices about it.
 */
class KnownServerTest {

    @Test
    fun aNewVisitGoesFirst() {
        val list = emptyList<KnownServer>()
            .withVisit("192.168.1.3", "Xiaomi", at = 1)
            .withVisit("192.168.1.128", "AQUA", at = 2)

        assertEquals(listOf("192.168.1.128", "192.168.1.3"), list.map { it.host })
    }

    @Test
    fun visitingAgainMovesItToTheFrontWithoutDuplicating() {
        val list = emptyList<KnownServer>()
            .withVisit("a", "A", at = 1)
            .withVisit("b", "B", at = 2)
            .withVisit("a", "A", at = 3)

        assertEquals(listOf("a", "b"), list.map { it.host })
        assertEquals(3L, list.first().lastConnectedAt)
    }

    @Test
    fun aVisitWithoutANameKeepsTheOneAlreadyKnown() {
        val list = emptyList<KnownServer>()
            .withVisit("a", "Xiaomi", at = 1)
            .withVisit("a", "", at = 2)

        assertEquals("Xiaomi", list.single().name)
    }

    @Test
    fun aBlankAddressIsIgnored() {
        assertEquals(emptyList(), emptyList<KnownServer>().withVisit("  ", "x", at = 1))
    }

    @Test
    fun theListIsCapped() {
        var list = emptyList<KnownServer>()
        repeat(MAX_KNOWN_SERVERS + 5) { list = list.withVisit("host$it", "", at = it.toLong()) }

        assertEquals(MAX_KNOWN_SERVERS, list.size)
        assertEquals("host${MAX_KNOWN_SERVERS + 4}", list.first().host)
    }

    @Test
    fun forgettingRemovesOnlyThatAddress() {
        val list = emptyList<KnownServer>().withVisit("a", "", 1).withVisit("b", "", 2).without("a")

        assertEquals(listOf("b"), list.map { it.host })
    }
}
