package com.groovernapp.mosqueathan.util

/**
 * The browser's preferred language.
 *
 * `navigator.language` rather than anything server-side: the web build is a controller a
 * takmir opens on their own phone or laptop, so the browser's own setting is the closest
 * thing to "what language does this person read".
 */
private fun browserLanguage(): String =
    js("(navigator.language || navigator.userLanguage || '').toLowerCase()")

internal actual fun platformIsIndonesian(): Boolean =
    browserLanguage().let { it.startsWith("id") || it.startsWith("in") }
