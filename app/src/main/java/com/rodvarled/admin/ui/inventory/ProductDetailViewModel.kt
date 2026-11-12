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

    fun load(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            repository.getProduct(productId)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, product = it) }
                .onFailure {
                    if (showLoading) {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage())
                    }
                }
            loadMovements()
        }
    }

    private fun loadMovements() {
        viewModelScope.launch {
            repository.getStockMovements(productId).onSuccess { _uiState.value = _uiState.value.copy(movements = it) }
        }
    }

    /** Ajuste rápido de +/-1 optimista en pantalla sin recargar la UI. */
    fun quickAdjust(bulbTypeId: Int?, delta: Int) {
        val currentProduct = _uiState.value.product ?: return
        val updatedVariants = currentProduct.variants.map { v ->
            if (bulbTypeId != null && v.bulbTypeId == bulbTypeId) v.copy(stock = (v.stock + delta).coerceAtLeast(0))
            else v
        }
        val updatedStock = if (currentProduct.variants.isEmpty()) (currentProduct.stock + delta).coerceAtLeast(0) else currentProduct.stock
        val updatedProduct = currentProduct.copy(stock = updatedStock, variants = updatedVariants)

        _uiState.value = _uiState.value.copy(product = updatedProduct, actionInProgress = true)

        viewModelScope.launch {
            repository.adjustStock(productId, bulbTypeId, delta, "Ajuste")
                .onSuccess {
                    load(showLoading = false)
                    _uiState.value = _uiState.value.copy(actionInProgress = false)
                }
                .onFailure { err ->
                    load(showLoading = false)
                    _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = err.toUserMessage())
                }
        }
    }

    /** Ajuste deliberado (recepción de mercancía, merma, conteo) con motivo y nota. */
    fun adjustWithReason(
        bulbTypeId: Int?,
        delta: Int,
        reason: String,
        note: String?,
        unitCostUsd: Double? = null,
        exchangeRate: Double? = null,
        purchaseDate: String? = null,
        receivedDate: String? = null
    ) {
        if (delta == 0) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            repository.adjustStock(
                productId = productId,
                bulbTypeId = bulbTypeId,
                delta = delta,
                reason = reason,
                note = note,
                unitCostUsd = unitCostUsd,
                exchangeRate = exchangeRate,
                purchaseDate = purchaseDate,
                receivedDate = receivedDate
            )
                .onSuccess {
                    load(showLoading = false)
                    _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = "Stock actualizado.")
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = err.toUserMessage())
                }
        }
    }

    fun deactivate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true)
            repository.deactivateProduct(productId)
                .onSuccess { _uiState.value = _uiState.value.copy(actionInProgress = false, deactivated = true) }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(actionInProgress = false, snackbarMessage = err.toUserMessage())
                }
        }
    }

    fun consumeSnackbar() { _uiState.value = _uiState.value.copy(snackbarMessage = null) }
}
