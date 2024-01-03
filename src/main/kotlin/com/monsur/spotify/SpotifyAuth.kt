package com.monsur.spotify

import com.monsur.auth.BaseAuth
import com.monsur.auth.AuthorizeResponse
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class SpotifyAuth(
    clientId: String,
    clientSecret: String,
    redirectUri: String,
    scope: String,
    authorizeUri: String,
    tokenUri: String
) : BaseAuth("spotify", clientId, clientSecret, redirectUri, scope, authorizeUri, tokenUri) {

    override fun getTokenRequestBuilder(authResp: AuthorizeResponse): HttpRequestBuilder {
        val builder= super.getTokenRequestBuilder(authResp)
        builder.headers.append(HttpHeaders.Authorization, getAuthHeader())
        return builder
    }

    override fun getRefreshTokenRequestBuilder(): HttpRequestBuilder {
        val builder= super.getRefreshTokenRequestBuilder()
        builder.headers.append(HttpHeaders.Authorization, getAuthHeader())
        return builder
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun getAuthHeader(): String {
        val origStr = "$clientId:$clientSecret"
        val encStr = Base64.encode(origStr.toByteArray())
        return "Basic $encStr"
    }
}