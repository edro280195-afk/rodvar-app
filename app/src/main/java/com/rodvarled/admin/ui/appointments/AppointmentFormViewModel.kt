package com.rodvarled.admin.ui.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.CreateAppointmentRequest
import com.rodvarled.admin.data.remote.dto.CreateQuoteItemRequest
import com.rodvarled.admin.data.remote.dto.CustomerListItem
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.data.remote.dto.UpdateAppointmentRequest
import com.rodvarled.admin.data.remote.dto.VehicleMakeDto
import com.rodvarled.admin.data.remote.dto.VehicleModelDto
import com.rodvarled.admin.data.remote.dto.VehicleTrimDto
import com.rodvarled.admin.data.remote.dto.VehicleYearDto
import com.rodvarled.admin.data.repository.AppointmentsRepository
import com.rodvarled.admin.data.repository.CatalogRepository
import com.rodvarled.admin.data.repository.CustomersRepository
import com.rodvarled.admin.data.repository.VehiclesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val APPOINTMENT_SOURCES = listOf("Web", "WhatsApp", "Facebook", "Llamada", "Recomendación", "Local")

data class SelectedProduct(val product: ProductDetail, val quantity: Int)

data class AppointmentFormUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,

    val customerName: String = "",
    val customerPhone: String = "",
    val customerSuggestions: List<CustomerListItem> = emptyList(),
    val source: String = "Local",
    val requestedDate: String = "",
    val requestedTime: String = "",
    val notes: String = "",

    val makes: List<VehicleMakeDto> = emptyList(),
    val models: List<VehicleModelDto> = emptyList(),
    val years: List<VehicleYearDto> = emptyList(),
    val trims: List<VehicleTrimDto> = emptyList(),
    val selectedMake: VehicleMakeDto? = null,
    val selectedModel: VehicleModelDto? = null,
    val selectedYear: VehicleYearDto? = null,
    val selectedTrim: VehicleTrimDto? = null,

    val availableProducts: List<ProductDetail> = emptyList(),
    val selectedProducts: List<SelectedProduct> = emptyList()
) {
    val total: Double get() = selectedProducts.sumOf { it.product.price * it.quantity }
}

@HiltViewModel
class AppointmentFormViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    private val customersRepository: CustomersRepository,
    private val vehiclesRepository: VehiclesRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentFormUiState())
    val uiState = _uiState.asStateFlow()

    private var editingId: Int? = null

    init {
        loadMakes()
        loadProducts()
    }

    fun loadForEdit(id: Int) {
        editingId = id
        _uiState.value = _uiState.value.copy(isEditMode = true, isLoading = true)
        viewModelScope.launch {
            appointmentsRepository.getAppointment(id)
                .onSuccess { appt ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        customerName = appt.customerName,
                        customerPhone = appt.customerPhone,
                        source = appt.source,
                        requestedDate = appt.requestedDate ?: "",
                        requestedTime = appt.requestedTime ?: "",
                        notes = appt.notes ?: ""
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
        }
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

    fun onSourceChange(value: String) { _uiState.value = _uiState.value.copy(source = value) }
    fun onDateChange(value: String) { _uiState.value = _uiState.value.copy(requestedDate = value) }
    fun onTimeChange(value: String) { _uiState.value = _uiState.value.copy(requestedTime = value) }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(notes = value) }

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

    fun toggleProduct(product: ProductDetail) {
        val current = _uiState.value.selectedProducts
        val existing = current.find { it.product.id == product.id }
        _uiState.value = _uiState.value.copy(
            selectedProducts = if (existing != null) current.filter { it.product.id != product.id }
            else current + SelectedProduct(product, 1)
        )
    }

    fun setProductQuantity(productId: Int, quantity: Int) {
        if (quantity < 1) return
        _uiState.value = _uiState.value.copy(
            selectedProducts = _uiState.value.selectedProducts.map {
                if (it.product.id == productId) it.copy(quantity = quantity) else it
            }
        )
    }

    fun save() {
        val state = _uiState.value
        if (state.customerName.isBlank() || state.customerPhone.isBlank()) {
            _uiState.value = state.copy(error = "El nombre y el teléfono del cliente son obligatorios.")
            return
        }

        _uiState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            val trimId = state.selectedTrim?.id
            val items = state.selectedProducts.map { CreateQuoteItemRequest(it.product.id, it.quantity) }.takeIf { it.isNotEmpty() }

            val result = if (editingId != null) {
                appointmentsRepository.updateAppointment(
                    editingId!!,
                    UpdateAppointmentRequest(
                        customerName = state.customerName,
                        customerPhone = state.customerPhone,
                        source = state.source,
                        requestedDate = state.requestedDate.ifBlank { null },
                        requestedTime = state.requestedTime.ifBlank { null },
                        notes = state.notes.ifBlank { null },
                        vehicleTrimId = trimId,
                        status = "Pendiente"
                    )
                )
            } else {
                appointmentsRepository.createAppointment(
                    CreateAppointmentRequest(
                        customerName = state.customerName,
                        customerPhone = state.customerPhone,
                        source = state.source,
                        requestedDate = state.requestedDate.ifBlank { null },
                        requestedTime = state.requestedTime.ifBlank { null },
                        notes = state.notes.ifBlank { null },
                        vehicleTrimId = trimId,
                        items = items
                    )
                )
            }

            result
                .onSuccess { _uiState.value = _uiState.value.copy(isSaving = false, saved = true) }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, error = it.toUserMessage()) }
        }
    }
}
