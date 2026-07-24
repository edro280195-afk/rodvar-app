package com.rodvarled.admin.data.auth

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        if (path.endsWith("/api/Auth/login") || path.endsWith("/api/Auth/refresh")) {
            return chain.proceed(original)
        }

        val token = tokenManager.getToken()
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}
