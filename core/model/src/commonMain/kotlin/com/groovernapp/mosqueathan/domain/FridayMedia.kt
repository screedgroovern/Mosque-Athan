package com.groovernapp.mosqueathan.domain

import kotlinx.serialization.Serializable

/**
 * One image or video in the Friday slideshow.
 *
 * Plain-data counterpart of `data.FridayMediaEntity`.
 */
@Serializable
data class FridayMedia(
    val id: String = "",
    val filePath: String = "",
    val isVideo: Boolean = false,
    val sortOrder: Int = 0
)
