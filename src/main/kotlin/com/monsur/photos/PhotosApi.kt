package com.monsur.photos

import com.monsur.auth.BaseAuth
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.logging.*
import kotlinx.serialization.json.Json
import kotlin.random.Random

internal val LOGGER = KtorSimpleLogger("com.monsur.photos.PhotosApi")

class PhotosApi(private val albumId: String, private val auth: BaseAuth) {
    private val json = Json { ignoreUnknownKeys = true }
    private var photos = mutableListOf<MediaItem>()

    suspend fun getPhoto(): MediaItem {
        if (photos.isEmpty()) {
            retrievePhotos()
        }
        return photos.removeFirst()
    }

    private suspend fun retrievePhotos(nextToken: String = "") {
        val client = HttpClient()

        var body = "{\"albumId\": \"$albumId\",\"pageSize\": 100"
        if (nextToken.isNotEmpty()) {
            body += ", \"pageToken\": \"$nextToken\""
        }
        body += "}"

        LOGGER.trace("Retrieving photos, next token = $nextToken")
        val response = client.request("https://photoslibrary.googleapis.com/v1/mediaItems:search") {
            method = HttpMethod.Post
            headers {
                append(HttpHeaders.Authorization, auth.getBearerAuthHeader())
            }
            setBody(body)
        }

        val bodyText = response.bodyAsText()
        if (response.status.value == 401) {
            auth.refreshToken()
            retrievePhotos(nextToken)
            return
        } else if (response.status.value != 200) {
            throw Exception(bodyText)
        }

        val searchResponse = json.decodeFromString<SearchResponse>(bodyText)

        randomizePhotos(searchResponse.mediaItems)

        if (searchResponse.nextPageToken.isNotEmpty()) {
            retrievePhotos(searchResponse.nextPageToken)
        }
    }

    private fun randomizePhotos(originals: List<MediaItem>) {
        val mutableItems = mutableListOf<MediaItem>()
        mutableItems.addAll(originals)
        while (mutableItems.size > 0) {
            val item = if (mutableItems.size == 1) mutableItems.removeFirst() else mutableItems.removeAt(
                Random.nextInt(
                    0,
                    mutableItems.size
                )
            )
            photos.add(item)
        }
    }
}