package com.rodvarled.admin.ui.appointments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.AppointmentSummary
import com.rodvarled.admin.data.repository.AppointmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppointmentDetailUiState(
    val isLoading: Boolean = true,
    val appointment: AppointmentSummary? = null,
    val error: String? = null,
    val actionInProgress: Boolean = false,
    val whatsAppUrlToOpen: String? = null,
    val deleted: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class AppointmentDetailViewModel @Inject constructor(
    private val repository: AppointmentsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val appointmentId: Int = checkNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow(AppointmentDetailUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getAppointment(appointmentId)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, appointment = it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
        }
    }

    fun confirm() = updateStatus("Confirmada")

    fun cancel(reason: String?) = updateStatus("Cancelada", reason)

    private fun updateStatus(status: String, reason: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            repository.updateStatus(appointmentId, status, reason)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(actionInProgress = false, appointment = it, snackbarMessage = "Cita actualizada a $status")
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = it.toUserMessage())
                }
        }
    }

    fun sendItinerary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            repository.sendItinerary(appointmentId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(actionInProgress = false, whatsAppUrlToOpen = it.whatsAppUrl)
                    load()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = it.toUserMessage())
                }
        }
    }

    fun consumeWhatsAppUrl() {
        _uiState.value = _uiState.value.copy(whatsAppUrlToOpen = null)
    }

    fun consumeSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun delete() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            repository.deleteAppointment(appointmentId)
                .onSuccess { _uiState.value = _uiState.value.copy(actionInProgress = false, deleted = true) }
                .onFailure { _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = it.toUserMessage()) }
        }
    }
}
