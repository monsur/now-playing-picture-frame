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
                val photosApi = PhotosApi(config.photos.album, photosAuth)

                while (true) {
                    val photo = photosApi.getPhoto()
                    call.application.environment.log.info("Sending photo: $photo")
                    send(Json.encodeToString(photo))
                    delay(config.photos.pollingMs)
                }

                /* Uncomment this block too add Spotify support.
                // The code below combines Spotify polling with photos retrieval.
                // Commented out for now while I figure some stuff out.

                val spotifyApi = SpotifyApi(spotifyAuth)
                val photosApi = PhotosApi(config.photos.album, photosAuth)
                val pollingFactor = (config.photos.pollingMs / config.spotify.pollingMs).toInt()
                var i = 0

                // When a client connects for a subsequent time (e.g. page refresh), it may be a while before the next
                // socket response. Send the previous data so that page has some initial data.
                // We only need to do this for Spotify since Spotify polls more often than it sends a response.
                if (prevTrack != null) {
                    call.application.environment.log.info("Sending track: $prevTrack")
                    send(Json.encodeToString(prevTrack))
                }

                // Once the client connection is made, this socket stays open indefinitely
                while (true) {
                    var pollingMs = config.spotify.pollingMs
                    try {
                        var track = spotifyApi.getCurrentlyPlayingTrack()
                        call.application.environment.log.trace("Track heartbeat: {}", track)

                        // Uncomment the line below to show photos when Spotify is paused.
                        //if (track != null && !track!!.isPlaying) {
                        //    track = null
                        //}

                        if (track == null) {
                            // If the track is null, that means Spotify is no longer playing.
                            // Send the next photo.
                            // But since photos are sent less frequently than Spotify is polled, they should be sent
                            // at a multiple of the Spotify polling time.
                            // For example, if Spotify is polled every 5 seconds, and we want a photo every 30 seconds,
                            // send a photo on 6th iteration of the loop (since 30/5 = 6).
                            if (i == 0) {
                                val photo = photosApi.getPhoto()
                                call.application.environment.log.info("Sending photo: $photo")
                                send(Json.encodeToString(photo))
                            }
                            i = (i + 1) % pollingFactor
                        } else if (track != prevTrack) {
                            // Only send track info the client if its different from the previous track.
                            // Note that this sends a track if the player changes playing state, since that affects
                            // object equality.
                            // TODO: Decide what the right thing to do when player state changes; depends on if we want to show play state in the UI.
                            call.application.environment.log.info("Sending track: $track")
                            send(Json.encodeToString(track))
                            i = 0
                        }

                        prevTrack = track
                    } catch (e: RateLimitException) {
                        pollingMs = e.retryAfter * 2000L
                        call.application.environment.log.info("Pausing for {$pollingMs}ms due to rate limit.")
                    }
                    delay(pollingMs)
                }
                 */
            }
        }
    }.start(wait = true)
}
