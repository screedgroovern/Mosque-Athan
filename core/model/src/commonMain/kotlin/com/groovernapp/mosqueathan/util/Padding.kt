package com.groovernapp.mosqueathan.util

/**
 * Zero-padding helpers.
 *
 * `"%02d".format(x)` resolves to `java.lang.String.format` and does not exist outside
 * the JVM. These produce byte-identical output for the non-negative values used here:
 * like `%02d`, a value wider than the pad width is printed in full.
 */
fun Int.pad(width: Int): String = toString().padStart(width, '0')

fun Long.pad(width: Int): String = toString().padStart(width, '0')
