package com.monsur.config

import kotlinx.serialization.Serializable

@Serializable
data class Config(val spotify: SpotifyConfig, val photos: PhotoConfig)

@Serializable
data class PhotoConfig(val album: String, val pollingMs: Long, val auth: AuthConfig)

@Serializable
data class SpotifyConfig(val pollingMs: Long, val auth: AuthConfig)

@Serializable
data class AuthConfig(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val scope: String,
    val authorizeUri: String,
    val tokenUri: String
)