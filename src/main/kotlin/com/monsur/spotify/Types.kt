package com.monsur.spotify

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val name: String,
    val isPlaying: Boolean,
    val album: String,
    val image: String,
    val artists: List<String>
)

class RateLimitException(message: String, val retryAfter: Int) : Exception(message)