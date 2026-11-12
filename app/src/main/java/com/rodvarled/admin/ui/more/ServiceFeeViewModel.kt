package com.rodvarled.admin.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.DEFAULT_HOME_VISIT_FEE
import com.rodvarled.admin.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServiceFeeUiState(
    val isLoading: Boolean = true,
    /** Tarifa vigente de la revisión a domicilio (la que ya se publica). */
    val fee: Double = DEFAULT_HOME_VISIT_FEE,
    /** Monto que se está editando en el diálogo. */
    val draft: Double = DEFAULT_HOME_VISIT_FEE,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ServiceFeeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceFeeUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            settingsRepository.getServiceFees()
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, fee = it.homeVisitFee, draft = it.homeVisitFee, error = null) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
        }
    }

    fun startEditing() {
        _uiState.value = _uiState.value.copy(draft = _uiState.value.fee, error = null)
    }

    fun onDraftChange(value: Double) {
        _uiState.value = _uiState.value.copy(draft = value)
    }

    /** Guarda la tarifa y avisa con `onSaved` para cerrar el diálogo. */
    fun save(onSaved: () -> Unit) {
        val draft = _uiState.value.draft
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            settingsRepository.updateHomeVisitFee(draft)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, fee = it.homeVisitFee, draft = it.homeVisitFee)
                    onSaved()
                }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, error = it.toUserMessage()) }
        }
    }
}
