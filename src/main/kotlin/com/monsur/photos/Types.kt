package com.monsur.photos

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(val mediaItems: List<MediaItem>, val nextPageToken: String = "")

@Serializable
data class MediaItem(
    val id: String,
    val description: String = "",
    val productUrl: String,
    val baseUrl: String,
    val filename: String,
    val mediaMetadata: MediaMetadata
)

@Serializable
data class MediaMetadata(
    val creationTime: String,
    val width: Int,
    val height: Int,
    val photo: PhotoMetadata? = null,
    val video: VideoMetadata? = null
)

@Serializable
data class PhotoMetadata(val cameraMake: String = "")

@Serializable
data class VideoMetadata(val cameraMake: String = "")