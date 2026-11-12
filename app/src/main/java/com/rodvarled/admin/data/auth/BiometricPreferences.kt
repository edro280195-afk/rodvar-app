package com.rodvarled.admin.data.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = createResilientEncryptedPreferences(context, "rodvar_biometric_prefs")

    private val _isBiometricEnabled = MutableStateFlow(isBiometricEnabled())
    val isBiometricEnabledFlow = _isBiometricEnabled.asStateFlow()

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }
}
