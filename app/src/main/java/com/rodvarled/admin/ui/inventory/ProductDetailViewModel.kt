package com.rodvarled.admin.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.data.remote.dto.StockMovement
import com.rodvarled.admin.data.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val STOCK_ADJUST_REASONS = listOf("Compra", "Ajuste", "Merma", "Devolucion", "Conteo", "Otro")

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val product: ProductDetail? = null,
    val movements: List<StockMovement> = emptyList(),
    val error: String? = null,
    val actionInProgress: Boolean = false,
    val snackbarMessage: String? = null,
    val deactivated: Boolean = false
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: CatalogRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: Int = checkNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getProduct(productId)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, product = it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
            loadMovements()
        }
    }

    private fun loadMovements() {
        viewModelScope.launch {
            repository.getStockMovements(productId).onSuccess { _uiState.value = _uiState.value.copy(movements = it) }
        }
    }

    /** Ajuste rápido de +/-1 con un toque, para el conteo del día a día. */
    fun quickAdjust(bulbTypeId: Int?, delta: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            repository.adjustStock(productId, bulbTypeId, delta, "Ajuste")
                .onSuccess { load(); _uiState.value = _uiState.value.copy(actionInProgress = false) }
                .onFailure { _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = it.toUserMessage()) }
        }
    }

    /** Ajuste deliberado (recepción de mercancía, merma, conteo) con motivo y nota. */
    fun adjustWithReason(bulbTypeId: Int?, delta: Int, reason: String, note: String?) {
        if (delta == 0) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            repository.adjustStock(productId, bulbTypeId, delta, reason, note)
                .onSuccess {
                    load()
                    _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = "Stock actualizado.")
                }
                .onFailure { _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = it.toUserMessage()) }
        }
    }

    fun deactivate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            repository.deactivateProduct(productId)
                .onSuccess { _uiState.value = _uiState.value.copy(actionInProgress = false, deactivated = true) }
                .onFailure { _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = it.toUserMessage()) }
        }
    }

    fun consumeSnackbar() { _uiState.value = _uiState.value.copy(snackbarMessage = null) }
}
