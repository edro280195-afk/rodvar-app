package com.rodvarled.admin.ui.compatibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.CompatibilityResult
import com.rodvarled.admin.data.remote.dto.SimpleLookup
import com.rodvarled.admin.data.remote.dto.VehicleMakeDto
import com.rodvarled.admin.data.remote.dto.VehicleModelDto
import com.rodvarled.admin.data.remote.dto.VehicleTrimDto
import com.rodvarled.admin.data.remote.dto.VehicleYearDto
import com.rodvarled.admin.data.repository.CatalogRepository
import com.rodvarled.admin.data.repository.CompatibilityRepository
import com.rodvarled.admin.data.repository.VehiclesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val BULB_POSITIONS = listOf("Faros Principales - Baja", "Faros Principales - Alta", "Niebla", "Interiores", "Reversa", "Direccional", "Freno / Stop")

data class CompatibilityUiState(
    val makes: List<VehicleMakeDto> = emptyList(),
    val models: List<VehicleModelDto> = emptyList(),
    val years: List<VehicleYearDto> = emptyList(),
    val trims: List<VehicleTrimDto> = emptyList(),
    val selectedMake: VehicleMakeDto? = null,
    val selectedModel: VehicleModelDto? = null,
    val selectedYear: VehicleYearDto? = null,
    val selectedTrim: VehicleTrimDto? = null,
    val bulbTypes: List<SimpleLookup> = emptyList(),
    val results: List<CompatibilityResult> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class CompatibilityViewModel @Inject constructor(
    private val vehiclesRepository: VehiclesRepository,
    private val compatibilityRepository: CompatibilityRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompatibilityUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch { vehiclesRepository.getMakes().onSuccess { _uiState.value = _uiState.value.copy(makes = it) } }
        viewModelScope.launch { catalogRepository.getBulbTypes().onSuccess { _uiState.value = _uiState.value.copy(bulbTypes = it) } }
    }

    fun onMakeSelected(make: VehicleMakeDto) {
        _uiState.value = _uiState.value.copy(selectedMake = make, models = emptyList(), years = emptyList(), trims = emptyList(), selectedModel = null, selectedYear = null, selectedTrim = null, results = emptyList())
        viewModelScope.launch { vehiclesRepository.getModels(make.id).onSuccess { _uiState.value = _uiState.value.copy(models = it) } }
    }

    fun onModelSelected(model: VehicleModelDto) {
        _uiState.value = _uiState.value.copy(selectedModel = model, years = emptyList(), trims = emptyList(), selectedYear = null, selectedTrim = null, results = emptyList())
        viewModelScope.launch { vehiclesRepository.getYears(model.id).onSuccess { _uiState.value = _uiState.value.copy(years = it) } }
    }

    fun onYearSelected(year: VehicleYearDto) {
        _uiState.value = _uiState.value.copy(selectedYear = year, trims = emptyList(), selectedTrim = null, results = emptyList())
        viewModelScope.launch { vehiclesRepository.getTrims(year.id).onSuccess { _uiState.value = _uiState.value.copy(trims = it) } }
    }

    fun onTrimSelected(trim: VehicleTrimDto) {
        _uiState.value = _uiState.value.copy(selectedTrim = trim, isLoading = true)
        viewModelScope.launch {
            compatibilityRepository.getByTrim(trim.id)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, results = it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, snackbarMessage = it.toUserMessage()) }
        }
    }

    fun saveMapping(positionName: String, bulbType: SimpleLookup) {
        val trimId = _uiState.value.selectedTrim?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            compatibilityRepository.saveMapping(trimId, positionName, bulbType.id)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, snackbarMessage = "Compatibilidad guardada.")
                    onTrimSelected(_uiState.value.selectedTrim!!)
                }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, snackbarMessage = it.toUserMessage()) }
        }
    }

    fun consumeSnackbar() { _uiState.value = _uiState.value.copy(snackbarMessage = null) }
}
