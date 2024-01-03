package com.monsur.spotify

import com.monsur.auth.BaseAuth
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class SpotifyApi(private val auth: BaseAuth) {
    private suspend fun retrieveCurrentlyPlayingTrack(): HttpResponse {
        val client = HttpClient()
        val response: HttpResponse = client.get("https://api.spotify.com/v1/me/player/currently-playing") {
            headers {
                append(HttpHeaders.Authorization, auth.getBearerAuthHeader())
            }
        }
        return response
    }

    suspend fun getCurrentlyPlayingTrack(): Track? {
        val response = retrieveCurrentlyPlayingTrack()
        return when (response.status.value) {
            204 -> null
            401 -> {
                auth.refreshToken()
                return getCurrentlyPlayingTrack()
            }
            403 -> throw Exception("Auth error: 403")
            429 -> {
                var retryAfter = 0
                if (response.headers.contains("RetryAfter")) {
                    retryAfter = response.headers.get("Retry-After")?.toInt() ?: 0
                }
                throw RateLimitException("Rate limiting: 429", retryAfter)
            }
            200 -> parseTrack(response)
            else -> throw Exception("Unexpected HTTP response: ${response.status.value}")
        }
    }

    private suspend fun parseTrack(response: HttpResponse): Track? {
        val json = Json.decodeFromString<JsonObject>(response.bodyAsText())
//        val isPlaying = (json.get("is_playing") as JsonPrimitive).content.toBoolean()
//        if (!isPlaying) {
//            return null
//        }

        // TODO: Support other playing types (e.g. podcast)
        val type = (json.get("currently_playing_type") as JsonPrimitive).content
        if (type != "track") {
            return null
        }

        val item = json.get("item") as JsonObject
        val id = (item.get("id") as JsonPrimitive).content
        val name = (item.get("name") as JsonPrimitive).content

        val album = item.get("album") as JsonObject
        val albumName = (album.get("name") as JsonPrimitive).content
        val images = (album.get("images") as JsonArray)
        val image = ((images[0] as JsonObject).get("url") as JsonPrimitive).content

        val artists = item.get("artists") as JsonArray
        val a = mutableListOf<String>()
        artists.forEach {
            val artist = it as JsonObject
            a.add((artist.get("name") as JsonPrimitive).content)
        }

        return Track(id, name, albumName, image, a)
    }
}