package com.rodvarled.admin.ui.appointments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.UpdateInstallationRequest
import com.rodvarled.admin.data.repository.AppointmentsRepository
import com.rodvarled.admin.data.repository.InstallationsRepository
import com.rodvarled.admin.data.repository.WarrantiesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val PAYMENT_METHODS = listOf("Efectivo", "Tarjeta", "Transferencia")

data class CompleteInstallationUiState(
    val vehiclePlate: String = "",
    val vehicleColor: String = "",
    val paymentMethod: String = "Efectivo",
    val technicianNotes: String = "",
    val acceptedTerms: Boolean = false,
    val beforePhotoBase64: String? = null,
    val afterPhotoBase64: String? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val completedWarrantyId: Int? = null
)

@HiltViewModel
class CompleteInstallationViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    private val warrantiesRepository: WarrantiesRepository,
    private val installationsRepository: InstallationsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val appointmentId: Int = checkNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow(CompleteInstallationUiState())
    val uiState = _uiState.asStateFlow()

    fun onPlateChange(value: String) { _uiState.value = _uiState.value.copy(vehiclePlate = value) }
    fun onColorChange(value: String) { _uiState.value = _uiState.value.copy(vehicleColor = value) }
    fun onPaymentMethodChange(value: String) { _uiState.value = _uiState.value.copy(paymentMethod = value) }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(technicianNotes = value) }
    fun onAcceptedTermsChange(value: Boolean) { _uiState.value = _uiState.value.copy(acceptedTerms = value) }
    fun onBeforePhotoCaptured(base64: String?) { _uiState.value = _uiState.value.copy(beforePhotoBase64 = base64) }
    fun onAfterPhotoCaptured(base64: String?) { _uiState.value = _uiState.value.copy(afterPhotoBase64 = base64) }

    fun submit(signatureBase64: String?) {
        val state = _uiState.value

        if (signatureBase64.isNullOrBlank()) {
            _uiState.value = state.copy(error = "La firma del cliente es obligatoria para completar la instalación.")
            return
        }
        if (!state.acceptedTerms) {
            _uiState.value = state.copy(error = "El cliente debe aceptar los términos de garantía.")
            return
        }

        _uiState.value = state.copy(isSubmitting = true, error = null)

        viewModelScope.launch {
            val statusResult = appointmentsRepository.updateStatus(appointmentId, "Instalada")
            val updatedAppointment = statusResult.getOrNull()
            if (updatedAppointment == null) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = statusResult.exceptionOrNull()?.toUserMessage() ?: "No se pudo marcar la cita como instalada.")
                return@launch
            }

            val warrantyId = updatedAppointment.warrantyId
            if (warrantyId == null) {
                // La cita no tenía productos cotizados, por lo que no se generó garantía automáticamente.
                _uiState.value = _uiState.value.copy(isSubmitting = false, completedWarrantyId = -1)
                return@launch
            }

            warrantiesRepository.sign(warrantyId, signatureBase64)
                .onFailure {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, error = "La cita se marcó como instalada, pero la firma falló: ${it.toUserMessage()}")
                    return@launch
                }

            val warrantyDetail = warrantiesRepository.getWarranty(warrantyId).getOrNull()
            val installationId = warrantyDetail?.installationId

            if (installationId != null) {
                installationsRepository.updateInstallation(
                    installationId,
                    UpdateInstallationRequest(
                        vehiclePlate = state.vehiclePlate.ifBlank { null },
                        vehicleColor = state.vehicleColor.ifBlank { null },
                        technicianNotes = state.technicianNotes.ifBlank { null },
                        paymentMethod = state.paymentMethod
                    )
                )

                if (state.afterPhotoBase64 != null) {
                    installationsRepository.addPhoto(installationId, null, state.beforePhotoBase64, state.afterPhotoBase64)
                }
            }

            _uiState.value = _uiState.value.copy(isSubmitting = false, completedWarrantyId = warrantyId)
        }
    }
}
