package com.monsur

import com.monsur.config.ConfigParser
import com.monsur.photos.PhotosApi
import com.monsur.photos.PhotosAuth
import com.monsur.spotify.RateLimitException
import com.monsur.spotify.SpotifyApi
import com.monsur.spotify.SpotifyAuth
import com.monsur.spotify.Track
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration

fun main() {
    val parser = ConfigParser()
    val config = parser.read()

    val photosAuth = PhotosAuth(
        config.photos.auth.clientId,
        config.photos.auth.clientSecret,
        config.photos.auth.redirectUri,
        config.photos.auth.scope,
        config.photos.auth.authorizeUri,
        config.photos.auth.tokenUri
    )

    val spotifyAuth = SpotifyAuth(
        config.spotify.auth.clientId,
        config.spotify.auth.clientSecret,
        config.spotify.auth.redirectUri,
        config.spotify.auth.scope,
        config.spotify.auth.authorizeUri,
        config.spotify.auth.tokenUri
    )

    var prevTrack: Track? = null

    embeddedServer(Netty, port = 5173, host = "localhost") {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
        install(Routing) {
            route("/spotify/login") {
                get {
                    val authorizeUrl = spotifyAuth.getAuthorizeUrl()
                    call.application.environment.log.info("Redirecting to $authorizeUrl")
                    call.respondRedirect(authorizeUrl)
                }
            }

            route("/spotify/callback") {
                get {
                    val authResp = spotifyAuth.parseAuthorizeResponse(call.request.queryParameters)
                    spotifyAuth.retrieveToken(authResp)
                    call.application.environment.log.info("Received Spotify token")
                    call.respondRedirect("/start.html")
                }
            }

            route("/photos/login") {
                get {
                    val authorizeUrl = photosAuth.getAuthorizeUrl()
                    call.application.environment.log.info("Redirecting to $authorizeUrl")
                    call.respondRedirect(authorizeUrl)
                }
            }

            route("/photos/callback") {
                get {
                    val authResp = photosAuth.parseAuthorizeResponse(call.request.queryParameters)
                    photosAuth.retrieveToken(authResp)
                    call.application.environment.log.info("Received Google Photos token")
                    call.respondRedirect("/start.html")
                }
            }

            staticFiles("/", File("static"), index = "start.html") {
            }

            webSocket("/server") {
                call.application.environment.log.info("New websocket connection received.")

                val spotifyApi = SpotifyApi(spotifyAuth)
                val photosApi = PhotosApi(config.photos.album, photosAuth)

                if (prevTrack != null) {
                    call.application.environment.log.info("Sending track: $prevTrack")
                    send(Json.encodeToString(prevTrack))
                }

                while (true) {
                    var track: Track? = null
                    try {
                        track = spotifyApi.getCurrentlyPlayingTrack()
                    } catch (e: RateLimitException) {
                        val retryAfter: Long = e.retryAfter*2000L
                        call.application.environment.log.info("Pausing for {$retryAfter}ms due to rate limit.")
                        delay(retryAfter)
                    }
                    if (track != null) {
                        if (track != prevTrack) {
                            call.application.environment.log.info("Sending track: $track")
                            send(Json.encodeToString(track))
                            prevTrack = track
                        }
                    } else {
                        // Photos
                        prevTrack = null
                        val photo = photosApi.getPhoto()
                        call.application.environment.log.info("Sending photo: $photo")
                        send(Json.encodeToString(photo))
                        delay(config.photos.pollingMs - config.spotify.pollingMs)
                    }
                    delay(config.spotify.pollingMs)
                }
            }
        }
    }.start(wait = true)
}
