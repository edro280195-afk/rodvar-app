package com.rodvarled.admin.ui

import androidx.lifecycle.ViewModel
import com.rodvarled.admin.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppRootViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {
    val isLoggedIn = authRepository.isLoggedIn
}
