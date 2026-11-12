package com.rodvarled.admin.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.PriceRule
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.data.remote.dto.SavePriceRuleRequest
import com.rodvarled.admin.data.repository.CatalogRepository
import com.rodvarled.admin.data.repository.PricingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PricingUiState(
    val isLoading: Boolean = true,
    val product: ProductDetail? = null,
    val rules: List<PriceRule> = emptyList(),
    val selectedVariantId: Int? = null,
    val ruleType: String = "Promotion",
    val discountType: String = "FixedPrice",
    val name: String = "",
    val valueText: String = "",
    val minimumQuantityText: String = "1",
    val customerIdText: String = "",
    val priorityText: String = "0",
    val isActive: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class PricingViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val pricingRepository: PricingRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val productId: Int = checkNotNull(savedStateHandle["id"])
    private val _uiState = MutableStateFlow(PricingUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val productResult = catalogRepository.getProduct(productId)
            val rulesResult = pricingRepository.getRules(productId)
            val current = _uiState.value
            _uiState.value = current.copy(
                isLoading = false,
                product = productResult.getOrNull(),
                rules = rulesResult.getOrDefault(emptyList()),
                error = productResult.exceptionOrNull()?.toUserMessage()
                    ?: rulesResult.exceptionOrNull()?.toUserMessage()
            )
        }
    }

    fun onVariantChange(value: Int?) { _uiState.value = _uiState.value.copy(selectedVariantId = value) }
    fun onRuleTypeChange(value: String) { _uiState.value = _uiState.value.copy(ruleType = value) }
    fun onDiscountTypeChange(value: String) { _uiState.value = _uiState.value.copy(discountType = value) }
    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun onValueChange(value: String) { _uiState.value = _uiState.value.copy(valueText = value.filter { it.isDigit() || it == '.' }.take(10)) }
    fun onMinimumQuantityChange(value: String) { _uiState.value = _uiState.value.copy(minimumQuantityText = value.filter { it.isDigit() }.take(4)) }
    fun onCustomerIdChange(value: String) { _uiState.value = _uiState.value.copy(customerIdText = value.filter { it.isDigit() }.take(10)) }
    fun onPriorityChange(value: String) { _uiState.value = _uiState.value.copy(priorityText = value.filter { it.isDigit() || it == '-' }.take(5)) }
    fun onActiveChange(value: Boolean) { _uiState.value = _uiState.value.copy(isActive = value) }

    fun saveRule() {
        val state = _uiState.value
        val value = state.valueText.toDoubleOrNull()
        val minimumQuantity = state.minimumQuantityText.toIntOrNull() ?: 1
        val customerId = state.customerIdText.toIntOrNull()
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Escribe un nombre para la regla.")
            return
        }
        if (value == null || value < 0) {
            _uiState.value = state.copy(error = "Captura un valor válido para el precio o descuento.")
            return
        }
        if (minimumQuantity < 1) {
            _uiState.value = state.copy(error = "La cantidad mínima debe ser al menos 1.")
            return
        }
        if (state.ruleType == "Customer" && customerId == null) {
            _uiState.value = state.copy(error = "Captura el ID del cliente para la regla personalizada.")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null, message = null)
            pricingRepository.createRule(
                SavePriceRuleRequest(
                    productId = productId,
                    productBulbTypeId = state.selectedVariantId,
                    customerId = if (state.ruleType == "Customer") customerId else null,
                    name = state.name.trim(),
                    ruleType = state.ruleType,
                    discountType = state.discountType,
                    value = value,
                    minimumQuantity = minimumQuantity,
                    priority = state.priorityText.toIntOrNull() ?: 0,
                    isActive = state.isActive
                )
            ).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    name = "",
                    valueText = "",
                    customerIdText = "",
                    message = "Regla guardada. Los siguientes presupuestos usarán esta condición."
                )
                load()
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, error = it.toUserMessage())
            }
        }
    }

    fun deleteRule(rule: PriceRule) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            pricingRepository.deleteRule(rule.id)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, message = "Regla eliminada.")
                    load()
                }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, error = it.toUserMessage()) }
        }
    }

    fun consumeFeedback() { _uiState.value = _uiState.value.copy(error = null, message = null) }
}
