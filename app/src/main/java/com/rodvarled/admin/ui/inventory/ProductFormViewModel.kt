package com.rodvarled.admin.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.BulbTypeVariantSaveRequest
import com.rodvarled.admin.data.remote.dto.ProductCategory
import com.rodvarled.admin.data.remote.dto.ProductSaveRequest
import com.rodvarled.admin.data.remote.dto.SimpleLookup
import com.rodvarled.admin.data.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

data class ProductVariantForm(
    val bulbTypeId: Int,
    val bulbTypeName: String,
    val stock: String = "0",
    val priceOverride: String = "",
    val lowStockThreshold: String = ""
)

data class ProductFormUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingImage: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,

    val categories: List<ProductCategory> = emptyList(),
    val bulbTypes: List<SimpleLookup> = emptyList(),
    val selectedCategory: ProductCategory? = null,

    val name: String = "",
    val slug: String = "",
    val slugManuallyEdited: Boolean = false,
    val description: String = "",
    val price: String = "",
    val warrantyMonths: String = "12",
    val lumens: String = "",
    val colorTemperature: String = "",
    val wattage: String = "",
    val voltageRange: String = "9V - 32V",
    val coolingSystem: String = "",
    val mainImageUrl: String? = null,
    val stock: String = "0",
    val lowStockThreshold: String = "",
    val isActive: Boolean = true,
    val variants: List<ProductVariantForm> = emptyList(),

    val productId: Int? = null
)

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val repository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductFormUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch { repository.getCategories().onSuccess { _uiState.value = _uiState.value.copy(categories = it) } }
        viewModelScope.launch { repository.getBulbTypes().onSuccess { _uiState.value = _uiState.value.copy(bulbTypes = it) } }
    }

    fun loadForEdit(id: Int) {
        _uiState.value = _uiState.value.copy(isEditMode = true, isLoading = true, productId = id)
        viewModelScope.launch {
            repository.getProduct(id)
                .onSuccess { p ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        name = p.name,
                        slug = p.slug,
                        slugManuallyEdited = true,
                        description = p.description ?: "",
                        price = p.price.toString(),
                        warrantyMonths = p.warrantyMonths.toString(),
                        lumens = p.lumens?.toString() ?: "",
                        colorTemperature = p.colorTemperature?.toString() ?: "",
                        wattage = p.wattage?.toString() ?: "",
                        voltageRange = p.voltageRange ?: "",
                        coolingSystem = p.coolingSystem ?: "",
                        mainImageUrl = p.mainImageUrl,
                        stock = p.stock.toString(),
                        lowStockThreshold = p.lowStockThreshold?.toString() ?: "",
                        selectedCategory = _uiState.value.categories.find { it.id == p.categoryId },
                        variants = p.variants.map {
                            ProductVariantForm(it.bulbTypeId, it.bulbTypeName, it.stock.toString(), it.priceOverride?.toString() ?: "", it.lowStockThreshold?.toString() ?: "")
                        }
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
        }
    }

    fun onNameChange(value: String) {
        val state = _uiState.value
        _uiState.value = state.copy(name = value, slug = if (!state.slugManuallyEdited) slugify(value) else state.slug)
    }

    fun onSlugChange(value: String) { _uiState.value = _uiState.value.copy(slug = value, slugManuallyEdited = true) }
    fun onDescriptionChange(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun onPriceChange(value: String) { _uiState.value = _uiState.value.copy(price = value) }
    fun onWarrantyMonthsChange(value: String) { _uiState.value = _uiState.value.copy(warrantyMonths = value) }
    fun onLumensChange(value: String) { _uiState.value = _uiState.value.copy(lumens = value) }
    fun onColorTemperatureChange(value: String) { _uiState.value = _uiState.value.copy(colorTemperature = value) }
    fun onWattageChange(value: String) { _uiState.value = _uiState.value.copy(wattage = value) }
    fun onVoltageRangeChange(value: String) { _uiState.value = _uiState.value.copy(voltageRange = value) }
    fun onCoolingSystemChange(value: String) { _uiState.value = _uiState.value.copy(coolingSystem = value) }
    fun onStockChange(value: String) { _uiState.value = _uiState.value.copy(stock = value) }
    fun onLowStockThresholdChange(value: String) { _uiState.value = _uiState.value.copy(lowStockThreshold = value) }
    fun onCategorySelected(category: ProductCategory) { _uiState.value = _uiState.value.copy(selectedCategory = category) }
    fun onActiveChange(value: Boolean) { _uiState.value = _uiState.value.copy(isActive = value) }

    fun addVariant(bulbType: SimpleLookup) {
        if (_uiState.value.variants.any { it.bulbTypeId == bulbType.id }) return
        _uiState.value = _uiState.value.copy(variants = _uiState.value.variants + ProductVariantForm(bulbType.id, bulbType.name))
    }

    fun removeVariant(bulbTypeId: Int) {
        _uiState.value = _uiState.value.copy(variants = _uiState.value.variants.filter { it.bulbTypeId != bulbTypeId })
    }

    fun updateVariantStock(bulbTypeId: Int, value: String) {
        _uiState.value = _uiState.value.copy(variants = _uiState.value.variants.map { if (it.bulbTypeId == bulbTypeId) it.copy(stock = value) else it })
    }

    fun updateVariantThreshold(bulbTypeId: Int, value: String) {
        _uiState.value = _uiState.value.copy(variants = _uiState.value.variants.map { if (it.bulbTypeId == bulbTypeId) it.copy(lowStockThreshold = value) else it })
    }

    fun uploadImage(base64: String) {
        val productId = _uiState.value.productId
        if (productId == null) {
            // Producto aún no existe: guardamos el base64 temporalmente como URL de datos y se sube tras crear.
            _uiState.value = _uiState.value.copy(mainImageUrl = base64)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingImage = true)
            repository.uploadProductImage(productId, base64)
                .onSuccess { _uiState.value = _uiState.value.copy(isUploadingImage = false, mainImageUrl = it.imageUrl) }
                .onFailure { _uiState.value = _uiState.value.copy(isUploadingImage = false, error = it.toUserMessage()) }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank() || state.selectedCategory == null || state.price.toDoubleOrNull() == null) {
            _uiState.value = state.copy(error = "Completa nombre, categoría y precio.")
            return
        }

        _uiState.value = state.copy(isSaving = true, error = null)

        val request = ProductSaveRequest(
            categoryId = state.selectedCategory.id,
            bulbTypeId = null,
            name = state.name,
            slug = state.slug.ifBlank { slugify(state.name) },
            description = state.description.ifBlank { null },
            price = state.price.toDoubleOrNull() ?: 0.0,
            warrantyMonths = state.warrantyMonths.toIntOrNull() ?: 12,
            lumens = state.lumens.toIntOrNull(),
            colorTemperature = state.colorTemperature.toIntOrNull(),
            wattage = state.wattage.toIntOrNull(),
            voltageRange = state.voltageRange.ifBlank { null },
            coolingSystem = state.coolingSystem.ifBlank { null },
            mainImageUrl = state.mainImageUrl?.takeUnless { it.startsWith("data:") },
            isActive = state.isActive,
            stock = state.stock.toIntOrNull() ?: 0,
            lowStockThreshold = state.lowStockThreshold.toIntOrNull(),
            variants = state.variants.map {
                BulbTypeVariantSaveRequest(it.bulbTypeId, it.stock.toIntOrNull() ?: 0, it.priceOverride.toDoubleOrNull(), it.lowStockThreshold.toIntOrNull())
            }.takeIf { it.isNotEmpty() }
        )

        viewModelScope.launch {
            val result = if (state.productId != null) {
                repository.updateProduct(state.productId, request)
            } else {
                repository.createProduct(request)
            }

            result
                .onSuccess { created ->
                    val newId = state.productId ?: (created as? com.rodvarled.admin.data.remote.dto.ProductDetail)?.id
                    if (newId != null && state.mainImageUrl?.startsWith("data:") == true) {
                        repository.uploadProductImage(newId, state.mainImageUrl)
                    }
                    _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
                }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, error = it.toUserMessage()) }
        }
    }

    private fun slugify(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
        return normalized.lowercase().trim().replace(Regex("[^a-z0-9\\s-]"), "").replace(Regex("\\s+"), "-")
    }
}
