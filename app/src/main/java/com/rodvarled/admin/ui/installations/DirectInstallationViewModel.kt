package com.rodvarled.admin.ui.installations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.CreateDirectInstallationRequest
import com.rodvarled.admin.data.remote.dto.CreateQuoteItemRequest
import com.rodvarled.admin.data.remote.dto.CustomerListItem
import com.rodvarled.admin.data.remote.dto.DirectInstallationResponse
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.data.remote.dto.VehicleMakeDto
import com.rodvarled.admin.data.remote.dto.VehicleModelDto
import com.rodvarled.admin.data.remote.dto.VehicleTrimDto
import com.rodvarled.admin.data.remote.dto.VehicleYearDto
import com.rodvarled.admin.data.repository.CatalogRepository
import com.rodvarled.admin.data.repository.CustomersRepository
import com.rodvarled.admin.data.repository.InstallationsRepository
import com.rodvarled.admin.data.repository.VehiclesRepository
import com.rodvarled.admin.ui.appointments.SelectedProduct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DirectInstallationUiState(
    val isSaving: Boolean = false,
    val error: String? = null,

    val customerName: String = "",
    val customerPhone: String = "",
    val customerSuggestions: List<CustomerListItem> = emptyList(),

    val makes: List<VehicleMakeDto> = emptyList(),
    val models: List<VehicleModelDto> = emptyList(),
    val years: List<VehicleYearDto> = emptyList(),
    val trims: List<VehicleTrimDto> = emptyList(),
    val selectedMake: VehicleMakeDto? = null,
    val selectedModel: VehicleModelDto? = null,
    val selectedYear: VehicleYearDto? = null,
    val selectedTrim: VehicleTrimDto? = null,
    val vehiclePlate: String = "",
    val vehicleColor: String = "",

    val availableProducts: List<ProductDetail> = emptyList(),
    val selectedProducts: List<SelectedProduct> = emptyList(),

    val paymentMethod: String = "Efectivo",
    val totalText: String = "",
    // Mientras el usuario no toque el total, se sugiere la suma de los productos;
    // en cuanto lo edita (negociación en el momento), se respeta su número.
    val totalEditedManually: Boolean = false,
    val technicianNotes: String = "",
    val beforePhotoBase64: String? = null,
    val afterPhotoBase64: String? = null,

    val completed: DirectInstallationResponse? = null
) {
    val suggestedTotal: Double get() = selectedProducts.sumOf { it.product.price * it.quantity }
}

@HiltViewModel
class DirectInstallationViewModel @Inject constructor(
    private val installationsRepository: InstallationsRepository,
    private val customersRepository: CustomersRepository,
    private val vehiclesRepository: VehiclesRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DirectInstallationUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadMakes()
        loadProducts()
    }

    private fun loadMakes() {
        viewModelScope.launch {
            vehiclesRepository.getMakes().onSuccess { _uiState.value = _uiState.value.copy(makes = it) }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            catalogRepository.observeProducts().collect { products ->
                _uiState.value = _uiState.value.copy(availableProducts = products.filter { it.stock > 0 || it.variants.any { v -> v.stock > 0 } })
            }
        }
        viewModelScope.launch { catalogRepository.refresh() }
    }

    fun onCustomerNameChange(value: String) {
        _uiState.value = _uiState.value.copy(customerName = value)
    }

    fun onCustomerPhoneChange(value: String) {
        _uiState.value = _uiState.value.copy(customerPhone = value)
        if (value.length >= 3) {
            viewModelScope.launch {
                customersRepository.search(value).onSuccess {
                    _uiState.value = _uiState.value.copy(customerSuggestions = it)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(customerSuggestions = emptyList())
        }
    }

    fun pickCustomerSuggestion(customer: CustomerListItem) {
        _uiState.value = _uiState.value.copy(
            customerName = customer.name,
            customerPhone = customer.phone,
            customerSuggestions = emptyList()
        )
    }

    fun onMakeSelected(make: VehicleMakeDto) {
        _uiState.value = _uiState.value.copy(selectedMake = make, models = emptyList(), years = emptyList(), trims = emptyList(), selectedModel = null, selectedYear = null, selectedTrim = null)
        viewModelScope.launch {
            vehiclesRepository.getModels(make.id).onSuccess { _uiState.value = _uiState.value.copy(models = it) }
        }
    }

    fun onModelSelected(model: VehicleModelDto) {
        _uiState.value = _uiState.value.copy(selectedModel = model, years = emptyList(), trims = emptyList(), selectedYear = null, selectedTrim = null)
        viewModelScope.launch {
            vehiclesRepository.getYears(model.id).onSuccess { _uiState.value = _uiState.value.copy(years = it) }
        }
    }

    fun onYearSelected(year: VehicleYearDto) {
        _uiState.value = _uiState.value.copy(selectedYear = year, trims = emptyList(), selectedTrim = null)
        viewModelScope.launch {
            vehiclesRepository.getTrims(year.id).onSuccess { _uiState.value = _uiState.value.copy(trims = it) }
        }
    }

    fun onTrimSelected(trim: VehicleTrimDto) {
        _uiState.value = _uiState.value.copy(selectedTrim = trim)
    }

    fun onPlateChange(value: String) { _uiState.value = _uiState.value.copy(vehiclePlate = value) }
    fun onColorChange(value: String) { _uiState.value = _uiState.value.copy(vehicleColor = value) }
    fun onPaymentMethodChange(value: String) { _uiState.value = _uiState.value.copy(paymentMethod = value) }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(technicianNotes = value) }
    fun onBeforePhotoCaptured(base64: String?) { _uiState.value = _uiState.value.copy(beforePhotoBase64 = base64) }
    fun onAfterPhotoCaptured(base64: String?) { _uiState.value = _uiState.value.copy(afterPhotoBase64 = base64) }

    fun onTotalChange(value: String) {
        _uiState.value = _uiState.value.copy(totalText = value, totalEditedManually = true)
    }

    fun toggleProduct(product: ProductDetail) {
        val current = _uiState.value.selectedProducts
        val existing = current.find { it.product.id == product.id }
        _uiState.value = _uiState.value.copy(
            selectedProducts = if (existing != null) current.filter { it.product.id != product.id }
            else current + SelectedProduct(product, 1)
        )
        syncSuggestedTotal()
    }

    fun setProductQuantity(productId: Int, quantity: Int) {
        if (quantity < 1) return
        _uiState.value = _uiState.value.copy(
            selectedProducts = _uiState.value.selectedProducts.map {
                if (it.product.id == productId) it.copy(quantity = quantity) else it
            }
        )
        syncSuggestedTotal()
    }

    private fun syncSuggestedTotal() {
        val state = _uiState.value
        if (state.totalEditedManually) return
        val suggested = state.suggestedTotal
        _uiState.value = state.copy(totalText = if (suggested > 0) "%.2f".format(suggested) else "")
    }

    fun submit() {
        val state = _uiState.value
        if (state.customerName.isBlank() || state.customerPhone.isBlank()) {
            _uiState.value = state.copy(error = "El nombre y el teléfono del cliente son obligatorios.")
            return
        }
        val total = state.totalText.replace(",", "").trim().toDoubleOrNull()
        if (total == null || total < 0) {
            _uiState.value = state.copy(error = "Captura el total cobrado (un número válido).")
            return
        }

        _uiState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            val request = CreateDirectInstallationRequest(
                customerName = state.customerName.trim(),
                customerPhone = state.customerPhone.trim(),
                vehicleTrimId = state.selectedTrim?.id,
                vehiclePlate = state.vehiclePlate.ifBlank { null },
                vehicleColor = state.vehicleColor.ifBlank { null },
                technicianNotes = state.technicianNotes.ifBlank { null },
                paymentMethod = state.paymentMethod,
                totalAmount = total,
                items = state.selectedProducts.map { CreateQuoteItemRequest(it.product.id, it.quantity) }
            )

            installationsRepository.createDirectInstallation(request)
                .onSuccess { response ->
                    // Las fotos (opcionales) se suben después de creada la instalación,
                    // igual que en el flujo de cita completada.
                    if (state.afterPhotoBase64 != null) {
                        installationsRepository.addPhoto(response.installationId, null, state.beforePhotoBase64, state.afterPhotoBase64)
                    }
                    _uiState.value = _uiState.value.copy(isSaving = false, completed = response)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isSaving = false, error = it.toUserMessage())
                }
        }
    }
}
