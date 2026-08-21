package com.groovernapp.mosqueathan.util

/** Whether the platform's current locale is Indonesian. */
internal expect fun platformIsIndonesian(): Boolean

object Lang {
    val isIndonesian: Boolean
        get() = platformIsIndonesian()

    fun get(en: String, id: String): String {
        return if (isIndonesian) id else en
    }
}
