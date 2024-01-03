package com.monsur.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthToken (
    val access_token: String,
    val token_type: String,
    val scope: String,
    val expires_in: Int,
    var refresh_token: String = ""
)

class AuthorizeResponse(val code: String, val state: String)