package com.rodvarled.admin.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.CreateCustomerRequest
import com.rodvarled.admin.data.remote.dto.CustomerListItem
import com.rodvarled.admin.data.repository.CustomersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomersListUiState(
    val isLoading: Boolean = true,
    val customers: List<CustomerListItem> = emptyList(),
    val query: String = "",
    val error: String? = null,
    val isCreating: Boolean = false,
    val createdCustomerId: Int? = null
)

@HiltViewModel
class CustomersListViewModel @Inject constructor(
    private val repository: CustomersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomersListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCustomers().collect { customers ->
                if (_uiState.value.query.isBlank()) {
                    _uiState.value = _uiState.value.copy(customers = customers)
                }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.refresh().onFailure { _uiState.value = _uiState.value.copy(error = it.toUserMessage()) }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                repository.refresh()
            } else {
                repository.search(query).onSuccess { _uiState.value = _uiState.value.copy(customers = it) }
            }
        }
    }

    fun createCustomer(name: String, phone: String) {
        if (name.isBlank() || phone.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true)
            repository.createCustomer(CreateCustomerRequest(name, phone))
                .onSuccess { _uiState.value = _uiState.value.copy(isCreating = false, createdCustomerId = it.id) }
                .onFailure { _uiState.value = _uiState.value.copy(isCreating = false, error = it.toUserMessage()) }
        }
    }

    fun consumeCreatedId() { _uiState.value = _uiState.value.copy(createdCustomerId = null) }
}
