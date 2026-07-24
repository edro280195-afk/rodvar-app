package com.rodvarled.admin.ui.quotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.QuoteSummary
import com.rodvarled.admin.data.repository.QuotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val QUOTE_FILTERS = listOf("Todas", "Draft", "Pendiente", "Agendado", "Instalado", "Cancelado")

data class QuotesListUiState(
    val isLoading: Boolean = true,
    val all: List<QuoteSummary> = emptyList(),
    val filtered: List<QuoteSummary> = emptyList(),
    val filter: String = "Todas",
    val query: String = "",
    val error: String? = null
)

@HiltViewModel
class QuotesListViewModel @Inject constructor(
    private val repository: QuotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuotesListUiState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getQuotes()
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, all = it, filtered = applyFilters(it)) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
        }
    }

    fun onFilterChange(filter: String) {
        _uiState.value = _uiState.value.copy(filter = filter, filtered = applyFilters(_uiState.value.all, filter))
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query, filtered = applyFilters(_uiState.value.all, query = query))
    }

    private fun applyFilters(list: List<QuoteSummary>, filter: String = _uiState.value.filter, query: String = _uiState.value.query): List<QuoteSummary> {
        var result = if (filter == "Todas") list else list.filter { it.status == filter }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { it.customerName.lowercase().contains(q) || it.folio.lowercase().contains(q) }
        }
        return result
    }
}
