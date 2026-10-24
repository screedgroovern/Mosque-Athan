package com.groovernapp.mosqueathan.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

/**
 * A display this device has connected to before.
 *
 * Kept so a takmir who looks after more than one mosque, or a display and a spare, can switch
 * between them without retyping an address or scanning a QR code again. [name] is what the display
 * called itself the last time (its device model); it may be empty for a display that predates that
 * field, and the address is then all there is to show.
 */
data class KnownServer(
    val host: String,
    val name: String = "",
    val lastConnectedAt: Long = 0L
)

/**
 * The displays this device has connected to, most recent first.
 *
 * An interface because where it is kept is a platform decision: Android uses preferences, and a
 * browser panel is served *by* one display and so has no list to keep.
 */
interface KnownServersStore {
    val servers: StateFlow<List<KnownServer>>

    /** Records a successful connection to [host], moving it to the front. */
    fun remember(host: String, name: String)

    fun forget(host: String)
}

/** The list after connecting to [host] at [at]: first, deduplicated, and never losing a known name. */
fun List<KnownServer>.withVisit(host: String, name: String, at: Long): List<KnownServer> {
    val address = host.trim()
    if (address.isEmpty()) return this
    val previous = firstOrNull { it.host == address }
    val visited = KnownServer(
        host = address,
        // A display that did not send a name this time must not erase the one it sent before.
        name = name.ifBlank { previous?.name.orEmpty() },
        lastConnectedAt = at
    )
    return (listOf(visited) + filterNot { it.host == address }).take(MAX_KNOWN_SERVERS)
}

fun List<KnownServer>.without(host: String): List<KnownServer> = filterNot { it.host == host }

/** More than a person looks after; the cap is so the list cannot grow without bound. */
const val MAX_KNOWN_SERVERS = 12

/** Held in memory only. The browser panel's choice, and what tests use. */
class InMemoryKnownServers(initial: List<KnownServer> = emptyList()) : KnownServersStore {
    private val state = MutableStateFlow(initial)
    override val servers: StateFlow<List<KnownServer>> = state.asStateFlow()

    override fun remember(host: String, name: String) {
        state.value = state.value.withVisit(host, name, Clock.System.now().toEpochMilliseconds())
    }

    override fun forget(host: String) {
        state.value = state.value.without(host)
    }
}
