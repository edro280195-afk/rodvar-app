package com.rodvarled.admin.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.PricingReport
import com.rodvarled.admin.data.repository.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsUiState(
    val isLoading: Boolean = true,
    val selectedDays: Int = 30,
    val report: PricingReport? = null,
    val error: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportsRepository: ReportsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    fun selectRange(days: Int) {
        if (days == _uiState.value.selectedDays && _uiState.value.report != null) return
        _uiState.value = _uiState.value.copy(selectedDays = days)
        load()
    }

    fun load() {
        val days = _uiState.value.selectedDays
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val today = LocalDate.now(ZoneOffset.UTC)
            val from = today.minusDays((days - 1).toLong()).atStartOfDay().toInstant(ZoneOffset.UTC).toString()
            val to = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toString()
            reportsRepository.getPricingReport(from, to)
                .onSuccess { report ->
                    _uiState.value = _uiState.value.copy(isLoading = false, report = report)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.toUserMessage())
                }
        }
    }
}
