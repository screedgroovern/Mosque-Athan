package com.groovernapp.mosqueathan.domain

/**
 * One image or video in the presentation slideshow.
 *
 * Plain-data counterpart of `data.MediaEntity`.
 */
data class Media(
    val id: String = "",
    val filePath: String = "",
    val isVideo: Boolean = false,
    val isUsed: Boolean = true,
    val sortOrder: Int = 0
)
