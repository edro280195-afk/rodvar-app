package com.rodvarled.admin.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.ProductCategory
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.data.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

fun ProductDetail.effectiveStock(): Int = if (variants.isNotEmpty()) variants.sumOf { it.stock } else stock

fun ProductDetail.isLowStock(): Boolean = if (variants.isNotEmpty()) {
    variants.any { it.lowStockThreshold != null && it.stock <= it.lowStockThreshold }
} else {
    lowStockThreshold != null && stock <= lowStockThreshold
}

data class InventoryListUiState(
    val isLoading: Boolean = true,
    val all: List<ProductDetail> = emptyList(),
    val filtered: List<ProductDetail> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val selectedCategoryId: Int? = null,
    val onlyLowStock: Boolean = false,
    val query: String = "",
    val snackbarMessage: String? = null,
    val error: String? = null,
    val purchaseProduct: ProductDetail? = null
) {
    val totalProductsCount: Int get() = all.size
    val totalStockUnits: Int get() = all.sumOf { it.effectiveStock() }
    val lowStockCount: Int get() = all.count { it.isLowStock() }
}

@HiltViewModel
class InventoryListViewModel @Inject constructor(
    private val repository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeProducts().collect { products ->
                _uiState.value = _uiState.value.copy(all = products, filtered = applyFilters(products))
            }
        }
        viewModelScope.launch {
            repository.getCategories().onSuccess { _uiState.value = _uiState.value.copy(categories = it) }
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
        _uiState.value = _uiState.value.copy(query = query, filtered = applyFilters(_uiState.value.all, query))
    }

    fun onCategorySelected(categoryId: Int?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, filtered = applyFilters(_uiState.value.all, categoryId = categoryId))
    }

    fun toggleLowStockOnly() {
        val newValue = !_uiState.value.onlyLowStock
        _uiState.value = _uiState.value.copy(onlyLowStock = newValue, filtered = applyFilters(_uiState.value.all, onlyLowStock = newValue))
    }

    fun adjustStock(product: ProductDetail, delta: Int) {
        val currentStock = product.effectiveStock()
        if (currentStock + delta < 0) return

        if (delta > 0) {
            _uiState.value = _uiState.value.copy(purchaseProduct = product, error = null)
            return
        }

        viewModelScope.launch {
            repository.adjustStock(
                productId = product.id,
                bulbTypeId = null,
                delta = delta,
                reason = "Ajuste",
                note = "Ajuste rápido desde catálogo"
            ).onSuccess {
                _uiState.value = _uiState.value.copy(snackbarMessage = "Stock de '${product.name}' actualizado")
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.toUserMessage())
            }
        }
    }

    fun dismissPurchaseDialog() {
        _uiState.value = _uiState.value.copy(purchaseProduct = null)
    }

    fun registerPurchase(
        product: ProductDetail,
        bulbTypeId: Int?,
        quantity: Int,
        unitCostUsd: Double,
        exchangeRate: Double,
        purchaseDate: String,
        receivedDate: String
    ) {
        if (quantity <= 0 || unitCostUsd <= 0 || exchangeRate <= 0) {
            _uiState.value = _uiState.value.copy(error = "Captura cantidad, costo en USD y tipo de cambio válidos.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(purchaseProduct = null, isLoading = true, error = null)
            repository.adjustStock(
                productId = product.id,
                bulbTypeId = bulbTypeId,
                delta = quantity,
                reason = "Compra",
                note = "Compra registrada con costo histórico en USD",
                unitCostUsd = unitCostUsd,
                exchangeRate = exchangeRate,
                purchaseDate = purchaseDate.ifBlank { null },
                receivedDate = receivedDate.ifBlank { null }
            ).onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, snackbarMessage = "Compra registrada: $quantity pieza(s) a $${"%.2f".format(unitCostUsd)} USD.")
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = err.toUserMessage())
            }
        }
    }

    fun consumeSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    private fun applyFilters(
        list: List<ProductDetail>,
        query: String = _uiState.value.query,
        categoryId: Int? = _uiState.value.selectedCategoryId,
        onlyLowStock: Boolean = _uiState.value.onlyLowStock
    ): List<ProductDetail> {
        var result = list
        if (categoryId != null) result = result.filter { it.categoryId == categoryId }
        if (onlyLowStock) result = result.filter { it.isLowStock() }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { it.name.lowercase().contains(q) }
        }
        return result.sortedBy { it.name }
    }
}
