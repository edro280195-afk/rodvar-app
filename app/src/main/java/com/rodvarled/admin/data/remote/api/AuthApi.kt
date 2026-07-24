package com.rodvarled.admin.data.remote.api

import com.rodvarled.admin.data.remote.dto.AuthResponse
import com.rodvarled.admin.data.remote.dto.LoginRequest
import com.rodvarled.admin.data.remote.dto.RefreshTokenRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/Auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/Auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): AuthResponse
}
