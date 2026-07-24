package com.rodvarled.admin.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val refreshToken: String = "",
    val name: String = "",
    val role: String = ""
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)
