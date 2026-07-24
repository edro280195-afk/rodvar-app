package com.rodvarled.admin.data.auth

import com.rodvarled.admin.data.remote.api.AuthApi
import com.rodvarled.admin.data.remote.dto.RefreshTokenRequest
import com.rodvarled.admin.di.PlainClient
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

/**
 * Se dispara automáticamente cuando el backend responde 401. Intenta renovar la sesión con el
 * refresh token sin pedir contraseña; si eso también falla, cierra la sesión localmente para que
 * la UI regrese al login. `Provider<AuthApi>` (no AuthApi directo) evita el ciclo de dependencias
 * con el cliente HTTP autenticado.
 */
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    @PlainClient private val plainAuthApi: Provider<AuthApi>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Evita bucles infinitos de reintento
        if (responseCount(response) >= 2) return null

        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            tokenManager.clear()
            return null
        }

        return synchronized(this) {
            // Otra petición concurrente ya pudo haber renovado el token mientras esperábamos el lock
            val currentToken = tokenManager.getToken()
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (currentToken != null && currentToken != requestToken) {
                return@synchronized response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            try {
                val result = runBlocking { plainAuthApi.get().refresh(RefreshTokenRequest(refreshToken)) }
                tokenManager.updateTokens(result.token, result.refreshToken)
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${result.token}")
                    .build()
            } catch (e: Exception) {
                tokenManager.clear()
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
