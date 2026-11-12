package com.rodvarled.admin.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.auth.BiometricPreferences
import com.rodvarled.admin.data.repository.AuthRepository
import com.rodvarled.admin.data.repository.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val biometricPreferences: BiometricPreferences,
    private val notificationsRepository: NotificationsRepository
) : ViewModel() {

    val userName: String get() = authRepository.userName
    val userRole: String get() = authRepository.userRole

    val isBiometricEnabled: StateFlow<Boolean> = biometricPreferences.isBiometricEnabledFlow

    private val _testNotificationState = MutableStateFlow<String?>(null)
    val testNotificationState: StateFlow<String?> = _testNotificationState.asStateFlow()

    private val _isSendingNotification = MutableStateFlow(false)
    val isSendingNotification: StateFlow<Boolean> = _isSendingNotification.asStateFlow()

    fun toggleBiometric(enabled: Boolean) {
        biometricPreferences.setBiometricEnabled(enabled)
    }

    fun sendTestPushNotification() {
        viewModelScope.launch {
            _isSendingNotification.value = true
            _testNotificationState.value = null
            val res = notificationsRepository.sendTestNotification()
            if (res.isSuccess) {
                _testNotificationState.value = "¡Notificación de prueba enviada con éxito!"
            } else {
                val err = res.exceptionOrNull()?.toUserMessage() ?: "Error al enviar la notificación."
                _testNotificationState.value = "Error: $err"
            }
            _isSendingNotification.value = false
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
