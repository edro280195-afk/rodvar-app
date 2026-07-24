package com.rodvarled.admin.ui.customers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.CustomerDetail
import com.rodvarled.admin.data.repository.CustomersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerDetailUiState(
    val isLoading: Boolean = true,
    val customer: CustomerDetail? = null,
    val error: String? = null,
    val actionInProgress: Boolean = false,
    val snackbarMessage: String? = null,
    val portalUrl: String? = null
)

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    private val repository: CustomersRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val customerId: Int = checkNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow(CustomerDetailUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getCustomer(customerId)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, customer = it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
        }
    }

    fun adjustPoints(delta: Int, reason: String) {
        if (delta == 0 || reason.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            repository.adjustPoints(customerId, delta, reason)
                .onSuccess { load(); _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = "Puntos actualizados.") }
                .onFailure { _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = it.toUserMessage()) }
        }
    }

    fun loadPortalLink() {
        viewModelScope.launch {
            repository.getPortalLink(customerId).onSuccess { _uiState.value = _uiState.value.copy(portalUrl = it.portalUrl) }
        }
    }

    fun consumeSnackbar() { _uiState.value = _uiState.value.copy(snackbarMessage = null) }
}
