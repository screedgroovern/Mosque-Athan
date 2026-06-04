package com.groovernapp.mosqueathan.util

fun Int.toCountdownString(): String {
    val minutes = this / 60
    val seconds = this % 60

    return "%02d:%02d".format(minutes, seconds)
}