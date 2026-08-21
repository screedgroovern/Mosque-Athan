package com.groovernapp.mosqueathan.util

fun Int.toCountdownString(): String {
    val minutes = this / 60
    val seconds = this % 60

    return "${minutes.pad(2)}:${seconds.pad(2)}"
}
