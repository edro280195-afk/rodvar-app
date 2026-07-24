package com.rodvarled.admin.ui.warranties

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.WarrantyDetail
import com.rodvarled.admin.data.repository.WarrantiesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

const val WARRANTY_PORTAL_BASE_URL = "https://rodvarled.com/garantia/"

data class WarrantyDetailUiState(
    val isLoading: Boolean = true,
    val warranty: WarrantyDetail? = null,
    val error: String? = null,
    val actionInProgress: Boolean = false,
    val snackbarMessage: String? = null,
    val deleted: Boolean = false
)

@HiltViewModel
class WarrantyDetailViewModel @Inject constructor(
    private val repository: WarrantiesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val warrantyId: Int = checkNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow(WarrantyDetailUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getWarranty(warrantyId)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, warranty = it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
        }
    }

    fun revoke() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            repository.revoke(warrantyId)
                .onSuccess { load(); _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = "Garantía revocada.") }
                .onFailure { _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = it.toUserMessage()) }
        }
    }

    fun resetSignature() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            repository.resetSignature(warrantyId)
                .onSuccess { load(); _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = "Firma eliminada. El cliente puede firmar de nuevo.") }
                .onFailure { _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = it.toUserMessage()) }
        }
    }

    fun signWarranty(signatureBase64: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            repository.sign(warrantyId, signatureBase64)
                .onSuccess { load(); _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = "Garantía firmada correctamente.") }
                .onFailure { _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = it.toUserMessage()) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            repository.delete(warrantyId)
                .onSuccess { _uiState.value = _uiState.value.copy(actionInProgress = false, deleted = true) }
                .onFailure { _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = it.toUserMessage()) }
        }
    }

    fun consumeSnackbar() { _uiState.value = _uiState.value.copy(snackbarMessage = null) }
}
