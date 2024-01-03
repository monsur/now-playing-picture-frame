package com.monsur.auth

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException

open class BaseAuth(
    private val key: String,
    val clientId: String,
    val clientSecret: String,
    private val redirectUri: String,
    private val scope: String,
    private val authorizeUri: String,
    private val tokenUri: String
) {
    private var state = ""
    private var token: AuthToken? = readToken()
    private val httpClient = HttpClient()

    fun getBearerAuthHeader(): String {
        return if (token != null) "Bearer ${token!!.access_token}" else ""
    }

    fun getAuthorizeUrl(): String {
        state = getRandomString(16)
        val url = getAuthorizeUrlBuilder()
        return url.toString()
    }

    fun parseAuthorizeResponse(params: Parameters): AuthorizeResponse {
        if (params.contains("error")) {
            throw Exception(params["error"])
        }

        val resp = AuthorizeResponse(params.get("code") ?: "", params.get("state") ?: "")

        if (resp.state != state) {
            throw Exception("State mismatch: $resp.state != $state")
        }

        state = ""
        return resp
    }

    fun retrieveToken(authResp: AuthorizeResponse) {
        runBlocking {
            val request = getTokenRequestBuilder(authResp)
            val response: HttpResponse = httpClient.request(request)
            setToken(parseTokenResponse(response))
        }
    }

    fun refreshToken() {
        runBlocking {
            val request = getRefreshTokenRequestBuilder()
            val response: HttpResponse = httpClient.request(request)
            val newToken = parseTokenResponse(response)
            if (newToken.refresh_token.isEmpty()) {
                newToken.refresh_token = token!!.refresh_token
            }
            setToken(newToken)
        }
    }

    open fun getAuthorizeUrlBuilder(): URLBuilder {
        val url = URLBuilder(authorizeUri)
        url.parameters["response_type"] = "code"
        url.parameters["client_id"] = clientId
        url.parameters["scope"] = scope
        url.parameters["redirect_uri"] = redirectUri
        url.parameters["state"] = state
        return url
    }

    open fun getRefreshTokenRequestBuilder(): HttpRequestBuilder {
        val builder = HttpRequestBuilder()
        builder.url(tokenUri)
        builder.method = HttpMethod.Post
        builder.headers.append(HttpHeaders.ContentType, "application/x-www-form-urlencoded")
        builder.setBody(FormDataContent(getRefreshTokenRequestBody().build()))
        return builder
    }

    open fun getRefreshTokenRequestBody(): ParametersBuilder {
        val params = ParametersBuilder()
        params.append("refresh_token", token!!.refresh_token)
        params.append("client_id", clientId)
        params.append("grant_type", "refresh_token")
        return params
    }

    open fun getTokenRequestBuilder(authResp: AuthorizeResponse): HttpRequestBuilder {
        val builder = HttpRequestBuilder()
        builder.url(tokenUri)
        builder.method = HttpMethod.Post
        builder.headers.append(HttpHeaders.ContentType, "application/x-www-form-urlencoded")
        builder.setBody(FormDataContent(getTokenRequestBody(authResp).build()))
        return builder
    }

    open fun getTokenRequestBody(authResp: AuthorizeResponse): ParametersBuilder {
        val params = ParametersBuilder()
        params.append("code", authResp.code)
        params.append("redirect_uri", redirectUri)
        params.append("grant_type", "authorization_code")
        return params
    }

    private fun setToken(newToken: AuthToken) {
        token = newToken
        writeToken(newToken)
    }

    private suspend fun parseTokenResponse(response: HttpResponse): AuthToken {
        if (response.status.value != 200) {
            throw Exception("Response $response.status, $response")
        }
        val bodyText = response.bodyAsText()
        return Json.decodeFromString<AuthToken>(bodyText)
    }

    private fun readToken(): AuthToken? {
        try {
            val file = File(getTokenFilePath())
            val bodyText = file.readText()
            return Json.decodeFromString<AuthToken>(bodyText)
        } catch (e: FileNotFoundException) {
            return null
        }
    }

    private fun writeToken(authToken: AuthToken?) {
        if (authToken != null) {
            val file = File(getTokenFilePath())
            file.writeText(Json.encodeToString(authToken))
        }
    }
    private fun getTokenFilePath(): String {
        return "src/main/resources/$key.token.json"
    }

    private fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}