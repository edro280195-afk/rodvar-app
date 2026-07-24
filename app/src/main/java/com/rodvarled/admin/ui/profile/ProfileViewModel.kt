package com.rodvarled.admin.ui.profile

import androidx.lifecycle.ViewModel
import com.rodvarled.admin.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    val userName: String get() = authRepository.userName
    val userRole: String get() = authRepository.userRole

    fun logout() {
        authRepository.logout()
    }
}
