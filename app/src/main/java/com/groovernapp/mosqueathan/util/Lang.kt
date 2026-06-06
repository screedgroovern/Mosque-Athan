package com.groovernapp.mosqueathan.util

import java.util.Locale

object Lang {
    val isIndonesian: Boolean
        get() = Locale.getDefault().language.let { it == "in" || it == "id" }

    fun get(en: String, id: String): String {
        return if (isIndonesian) id else en
    }
}
