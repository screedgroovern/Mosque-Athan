package com.groovernapp.mosqueathan.domain

/**
 * One image or video in the Friday slideshow.
 *
 * Plain-data counterpart of `data.FridayMediaEntity`.
 */
data class FridayMedia(
    val id: String = "",
    val filePath: String = "",
    val isVideo: Boolean = false,
    val sortOrder: Int = 0
)
