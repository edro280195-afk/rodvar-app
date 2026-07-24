package com.rodvarled.admin.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import com.rodvarled.admin.data.auth.TokenManager
import com.rodvarled.admin.data.remote.api.AuthApi
import com.rodvarled.admin.data.remote.dto.LoginRequest
import com.rodvarled.admin.di.PlainClient
import com.rodvarled.admin.core.util.safeApiCall
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @PlainClient private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val notificationsRepository: NotificationsRepository
) {
    val isLoggedIn = tokenManager.isLoggedIn
    val userName: String get() = tokenManager.getUserName()
    val userRole: String get() = tokenManager.getUserRole()

    suspend fun login(email: String, password: String) = safeApiCall {
        val response = authApi.login(LoginRequest(email.trim(), password))
        tokenManager.saveSession(response)
        registerPushToken()
        response
    }

    private suspend fun registerPushToken() {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            notificationsRepository.registerDevice(token)
        }
    }

    fun logout() {
        tokenManager.clear()
    }
}
