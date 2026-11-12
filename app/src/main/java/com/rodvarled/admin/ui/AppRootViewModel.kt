package com.rodvarled.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.data.auth.BiometricPreferences
import com.rodvarled.admin.data.auth.TokenManager
import com.rodvarled.admin.data.repository.AuthRepository
import com.rodvarled.admin.data.repository.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppRootViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val biometricPreferences: BiometricPreferences,
    private val notificationsRepository: NotificationsRepository
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isBiometricEnabled: StateFlow<Boolean> = biometricPreferences.isBiometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val userName: String get() = tokenManager.getUserName()

    init {
        viewModelScope.launch {
            isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    // Intentar registrar el dispositivo para push notifications al detectar sesión activa
                    try {
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                            viewModelScope.launch {
                                notificationsRepository.registerDevice(token)
                            }
                        }
                    } catch (e: Exception) {
                        // Ignorar en entornos sin Google Play Services
                    }
                } else {
                    _isUnlocked.value = false
                }
            }
        }
    }

    fun onBiometricSuccess() {
        _errorMessage.value = null
        _isUnlocked.value = true
    }

    fun onBiometricError(error: String) {
        _errorMessage.value = error
    }

    fun unlockWithoutBiometrics() {
        _isUnlocked.value = true
    }

    fun logout() {
        authRepository.logout()
        _isUnlocked.value = false
    }
}
