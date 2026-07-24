package com.rodvarled.admin.ui.warranties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.WarrantySummary
import com.rodvarled.admin.data.repository.WarrantiesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

val WARRANTY_FILTERS = listOf("Todas", "Sin firmar", "Firmadas", "Vencidas", "Revocadas")

data class WarrantiesListUiState(
    val isLoading: Boolean = true,
    val all: List<WarrantySummary> = emptyList(),
    val filtered: List<WarrantySummary> = emptyList(),
    val filter: String = "Todas",
    val query: String = "",
    val error: String? = null
)

@HiltViewModel
class WarrantiesListViewModel @Inject constructor(
    private val repository: WarrantiesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WarrantiesListUiState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getWarranties()
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(isLoading = false, all = list, filtered = applyFilters(list, _uiState.value.filter, _uiState.value.query))
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
        }
    }

    fun onFilterChange(filter: String) {
        _uiState.value = _uiState.value.copy(filter = filter, filtered = applyFilters(_uiState.value.all, filter, _uiState.value.query))
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query, filtered = applyFilters(_uiState.value.all, _uiState.value.filter, query))
    }

    private fun applyFilters(list: List<WarrantySummary>, filter: String, query: String): List<WarrantySummary> {
        val today = LocalDate.now().toString()
        var result = when (filter) {
            "Sin firmar" -> list.filter { !it.isSigned }
            "Firmadas" -> list.filter { it.isSigned }
            "Vencidas" -> list.filter { it.warrantyEnd < today }
            "Revocadas" -> list.filter { !it.isActive }
            else -> list
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { it.customerName.lowercase().contains(q) || it.customerPhone.contains(q) || it.folio.lowercase().contains(q) }
        }
        return result
    }
}
