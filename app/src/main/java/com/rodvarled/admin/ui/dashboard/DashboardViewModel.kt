package com.rodvarled.admin.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.data.remote.dto.AppointmentSummary
import com.rodvarled.admin.data.repository.AppointmentsRepository
import com.rodvarled.admin.data.repository.AuthRepository
import com.rodvarled.admin.data.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val todayAppointments: List<AppointmentSummary> = emptyList(),
    val pendingCount: Int = 0,
    val confirmedCount: Int = 0,
    val lowStockCount: Int = 0,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    private val catalogRepository: CatalogRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(userName = authRepository.userName))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appointmentsRepository.observeAppointments()
                .combine(catalogRepository.observeLowStock()) { appointments, lowStock -> appointments to lowStock }
                .collect { (appointments, lowStock) ->
                    val today = LocalDate.now().toString()
                    _uiState.value = _uiState.value.copy(
                        todayAppointments = appointments.filter { it.requestedDate == today }
                            .sortedBy { it.requestedTime ?: "" },
                        pendingCount = appointments.count { it.status == "Pendiente" },
                        confirmedCount = appointments.count { it.status == "Confirmada" },
                        lowStockCount = lowStock.size
                    )
                }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = appointmentsRepository.refresh()
            catalogRepository.refresh()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.let { "No se pudo actualizar. Mostrando datos guardados." }
            )
        }
    }
}
