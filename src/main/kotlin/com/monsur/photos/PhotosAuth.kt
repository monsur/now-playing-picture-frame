package com.monsur.photos

import com.monsur.auth.BaseAuth
import com.monsur.auth.AuthorizeResponse
import io.ktor.http.*

class PhotosAuth(
    clientId: String,
    clientSecret: String,
    redirectUri: String,
    scope: String,
    authorizeUri: String,
    tokenUri: String
) : BaseAuth("photos", clientId, clientSecret, redirectUri, scope, authorizeUri, tokenUri) {
    override fun getAuthorizeUrlBuilder(): URLBuilder {
        val url = super.getAuthorizeUrlBuilder()
        url.parameters["access_type"] = "offline"
        url.parameters["prompt"] = "consent"
        return url
    }

    override fun getTokenRequestBody(authResp: AuthorizeResponse): ParametersBuilder {
        val params = super.getTokenRequestBody(authResp)
        params.append("client_id", clientId)
        params.append("client_secret", clientSecret)
        return params
    }

    override fun getRefreshTokenRequestBody(): ParametersBuilder {
        val builder = super.getRefreshTokenRequestBody()
        builder.append("client_secret", clientSecret)
        return builder
    }
}