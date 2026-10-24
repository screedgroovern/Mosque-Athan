package com.groovernapp.mosqueathan.domain

/**
 * Which preview the display is showing, if any.
 *
 * It exists so a controller can tell *which* of its four Preview buttons to turn into Stop. A bare
 * "a preview is running" would flip the Adzan and Iqomah buttons together, though they sit side by
 * side and only one was pressed. The display reports it in its snapshot as [wire], because the
 * display ends a preview by itself (a timeout, the end of the countdown, Back on the remote) and a
 * controller that only remembered its own press would be left holding a Stop button for a preview
 * that was already over.
 */
enum class PreviewKind(val wire: String) {
    Adzan("adzan"),
    Iqomah("iqomah"),
    Slideshow("slideshow"),
    FridaySlideshow("friday_slideshow");

    companion object {
        /** Null for the empty string a display with no preview running sends, and for anything unknown. */
        fun fromWire(value: String?): PreviewKind? = entries.firstOrNull { it.wire == value }
    }
}
