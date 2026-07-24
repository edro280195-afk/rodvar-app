package com.rodvarled.admin.ui.appointments

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

val APPOINTMENT_FILTERS = listOf("Todas", "Pendiente", "Confirmada", "Instalada", "Cancelada")

data class AppointmentsListUiState(
    val isLoading: Boolean = true,
    val allAppointments: List<AppointmentSummary> = emptyList(),
    val filtered: List<AppointmentSummary> = emptyList(),
    val filter: String = "Todas",
    val query: String = "",
    val error: String? = null
)

@HiltViewModel
class AppointmentsListViewModel @Inject constructor(
    private val repository: AppointmentsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentsListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAppointments().collect { appointments ->
                _uiState.value = _uiState.value.copy(
                    allAppointments = appointments,
                    filtered = applyFilters(appointments, _uiState.value.filter, _uiState.value.query)
                )
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.refresh()
                .onFailure { _uiState.value = _uiState.value.copy(error = it.toUserMessage()) }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun onFilterChange(filter: String) {
        _uiState.value = _uiState.value.copy(
            filter = filter,
            filtered = applyFilters(_uiState.value.allAppointments, filter, _uiState.value.query)
        )
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            query = query,
            filtered = applyFilters(_uiState.value.allAppointments, _uiState.value.filter, query)
        )
    }

    private fun applyFilters(list: List<AppointmentSummary>, filter: String, query: String): List<AppointmentSummary> {
        var result = if (filter == "Todas") list else list.filter { it.status == filter }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { it.customerName.lowercase().contains(q) || it.customerPhone.contains(q) }
        }
        return result.sortedWith(compareByDescending { it.requestedDate ?: "" })
    }
}
