package com.groovernapp.mosqueathan.util

import java.util.Locale

internal actual fun platformIsIndonesian(): Boolean =
    Locale.getDefault().language.let { it == "in" || it == "id" }
