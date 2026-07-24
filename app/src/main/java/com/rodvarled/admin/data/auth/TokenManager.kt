package com.rodvarled.admin.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.rodvarled.admin.data.remote.dto.AuthResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El token se guarda en EncryptedSharedPreferences (cifrado por Keystore de Android) en vez de
 * SharedPreferences plano: es un salto de seguridad real frente al localStorage que usa el panel web.
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "rodvar_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _isLoggedIn = MutableStateFlow(hasToken())
    val isLoggedIn = _isLoggedIn.asStateFlow()

    fun saveSession(auth: AuthResponse) {
        prefs.edit()
            .putString(KEY_TOKEN, auth.token)
            .putString(KEY_REFRESH_TOKEN, auth.refreshToken)
            .putString(KEY_NAME, auth.name)
            .putString(KEY_ROLE, auth.role)
            .apply()
        _isLoggedIn.value = true
    }

    fun updateTokens(token: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getUserName(): String = prefs.getString(KEY_NAME, null) ?: ""
    fun getUserRole(): String = prefs.getString(KEY_ROLE, null) ?: ""

    private fun hasToken(): Boolean = !getToken().isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_NAME = "name"
        private const val KEY_ROLE = "role"
    }
}
