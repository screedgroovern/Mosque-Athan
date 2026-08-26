package com.groovernapp.mosqueathan.util

/**
 * Exchanges the elements at [a] and [b].
 *
 * Replaces `java.util.Collections.swap`, which is JVM-only and was reached for at five
 * reordering call sites — the drag handles in the announcement and media lists.
 */
fun <T> MutableList<T>.swap(a: Int, b: Int) {
    val held = this[a]
    this[a] = this[b]
    this[b] = held
}
