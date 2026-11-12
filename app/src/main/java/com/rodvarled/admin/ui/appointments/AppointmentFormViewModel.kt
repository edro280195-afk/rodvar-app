package com.rodvarled.admin.ui.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.pricing.listPriceFor
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.BulbTypeVariant
import com.rodvarled.admin.data.remote.dto.CreateAppointmentRequest
import com.rodvarled.admin.data.remote.dto.CreateQuoteItemRequest
import com.rodvarled.admin.data.remote.dto.CustomerLookup
import com.rodvarled.admin.data.remote.dto.DEFAULT_HOME_VISIT_FEE
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.data.remote.dto.SERVICE_TYPE_INSTALLATION
import com.rodvarled.admin.data.remote.dto.SERVICE_TYPE_REVISION
import com.rodvarled.admin.data.remote.dto.UpdateAppointmentRequest
import com.rodvarled.admin.data.repository.AddressSuggestion
import com.rodvarled.admin.data.repository.AppointmentsRepository
import com.rodvarled.admin.data.repository.CatalogRepository
import com.rodvarled.admin.data.repository.CustomersRepository
import com.rodvarled.admin.data.repository.GooglePlacesRepository
import com.rodvarled.admin.data.repository.SettingsRepository
import com.rodvarled.admin.data.repository.VehiclesRepository
import com.rodvarled.admin.ui.components.VehicleSearcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

val APPOINTMENT_SOURCES = listOf("Web", "WhatsApp", "Facebook", "Llamada", "Recomendación", "Local")

/** Horas más comunes para agendar con un toque. */
val QUICK_APPOINTMENT_HOURS = listOf(9, 10, 11, 12, 13, 15, 16, 17, 18)

/** Producto elegido para la cita. `bulbTypeId` es el id de la variante (el que usa la API como `productBulbTypeId`). */
data class SelectedProduct(
    val product: ProductDetail,
    val quantity: Int,
    val bulbTypeId: Int? = null,
    val inventoryUnitCodes: List<String> = emptyList()
) {
    val variant: BulbTypeVariant? get() = bulbTypeId?.let { id -> product.variants.firstOrNull { it.id == id } }
    val unitPrice: Double get() = product.listPriceFor(variant)
}

data class AppointmentFormUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,

    val customerName: String = "",
    val customerPhone: String = "",
    val customerSuggestions: List<CustomerLookup> = emptyList(),
    val source: String = "Local",
    /** "Instalacion" o "Revision" (visita a domicilio sin instalar productos, con costo). */
    val serviceType: String = SERVICE_TYPE_INSTALLATION,
    /** Tarifa general vigente de la revisión y la que se acordó para esta cita. */
    val homeVisitFee: Double = DEFAULT_HOME_VISIT_FEE,
    val visitFee: Double = DEFAULT_HOME_VISIT_FEE,
    val requestedDate: String = "",
    val requestedTime: String = "",
    val notes: String = "",
    val serviceAddress: String = "",
    val serviceLatitude: Double? = null,
    val serviceLongitude: Double? = null,
    val googlePlaceId: String? = null,
    val addressSuggestions: List<AddressSuggestion> = emptyList(),
    val isAddressSearching: Boolean = false,
    val addressError: String? = null,

    val availableProducts: List<ProductDetail> = emptyList(),
    val selectedProducts: List<SelectedProduct> = emptyList()
) {
    val total: Double get() = selectedProducts.sumOf { it.unitPrice * it.quantity }
    val isRevision: Boolean get() = serviceType == SERVICE_TYPE_REVISION
}

@HiltViewModel
class AppointmentFormViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    private val customersRepository: CustomersRepository,
    vehiclesRepository: VehiclesRepository,
    private val catalogRepository: CatalogRepository,
    private val googlePlacesRepository: GooglePlacesRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentFormUiState())
    val uiState = _uiState.asStateFlow()

    /** Búsqueda de vehículo por texto ("civic 2018"). */
    val vehicleSearcher = VehicleSearcher(viewModelScope, vehiclesRepository)

    private var editingId: Int? = null
    private var addressSearchJob: Job? = null
    private var customerLookupJob: Job? = null

    init {
        loadProducts()
        loadHomeVisitFee()
    }

    private fun loadHomeVisitFee() {
        viewModelScope.launch {
            val fee = settingsRepository.getHomeVisitFee()
            val state = _uiState.value
            // Si todavía no se tocó el monto de la revisión, se actualiza a la tarifa vigente.
            _uiState.value = state.copy(
                homeVisitFee = fee,
                visitFee = if (state.visitFee == state.homeVisitFee) fee else state.visitFee
            )
        }
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
                        serviceType = appt.serviceType,
                        visitFee = appt.visitFeeAmount ?: _uiState.value.homeVisitFee,
                        requestedDate = appt.requestedDate ?: "",
                        requestedTime = appt.requestedTime ?: "",
                        notes = appt.notes ?: "",
                        serviceAddress = appt.serviceAddress ?: "",
                        serviceLatitude = appt.serviceLatitude,
                        serviceLongitude = appt.serviceLongitude,
                        googlePlaceId = appt.googlePlaceId
                    )
                    // Editar una cita conserva su vehículo: se muestra el que ya tenía.
                    appt.vehicleTrimId?.let { vehicleSearcher.selectKnown(it, appt.vehicleInfo) }
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
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

    // ----- Cliente -----

    fun onCustomerNameChange(value: String) {
        _uiState.value = _uiState.value.copy(customerName = value)
        lookupCustomers(value, minLength = 2)
    }

    fun onCustomerPhoneChange(value: String) {
        _uiState.value = _uiState.value.copy(customerPhone = value)
        lookupCustomers(value, minLength = 3)
    }

    private fun lookupCustomers(term: String, minLength: Int) {
        customerLookupJob?.cancel()
        if (term.trim().length < minLength) {
            _uiState.value = _uiState.value.copy(customerSuggestions = emptyList())
            return
        }
        customerLookupJob = viewModelScope.launch {
            delay(250)
            customersRepository.lookup(term.trim()).onSuccess {
                _uiState.value = _uiState.value.copy(customerSuggestions = it)
            }
        }
    }

    /** Al elegir un cliente conocido se rellenan solos su nombre, teléfono, último vehículo y domicilio. */
    fun pickCustomerSuggestion(customer: CustomerLookup) {
        customerLookupJob?.cancel()
        val state = _uiState.value
        val fillAddress = state.serviceAddress.isBlank() && !customer.lastServiceAddress.isNullOrBlank()

        _uiState.value = state.copy(
            customerName = customer.name,
            customerPhone = customer.phone,
            customerSuggestions = emptyList(),
            serviceAddress = if (fillAddress) customer.lastServiceAddress!! else state.serviceAddress,
            serviceLatitude = if (fillAddress) customer.lastServiceLatitude else state.serviceLatitude,
            serviceLongitude = if (fillAddress) customer.lastServiceLongitude else state.serviceLongitude,
            googlePlaceId = if (fillAddress) customer.lastGooglePlaceId else state.googlePlaceId
        )

        if (vehicleSearcher.state.value.selected == null && customer.lastVehicleTrimId != null && !customer.lastVehicleLabel.isNullOrBlank()) {
            vehicleSearcher.selectKnown(customer.lastVehicleTrimId, customer.lastVehicleLabel)
        }
    }

    // ----- Datos de la cita -----

    fun onSourceChange(value: String) { _uiState.value = _uiState.value.copy(source = value) }

    fun onServiceTypeChange(value: String) {
        val state = _uiState.value
        if (state.serviceType == value) return
        // Una revisión no lleva productos: se limpian para que no viajen en la cotización.
        _uiState.value = state.copy(
            serviceType = value,
            visitFee = if (value == SERVICE_TYPE_REVISION) state.homeVisitFee else state.visitFee,
            selectedProducts = if (value == SERVICE_TYPE_REVISION) emptyList() else state.selectedProducts
        )
    }

    fun onVisitFeeChange(value: Double) { _uiState.value = _uiState.value.copy(visitFee = value) }
    fun onDateChange(value: String) { _uiState.value = _uiState.value.copy(requestedDate = value) }
    fun onTimeChange(value: String) { _uiState.value = _uiState.value.copy(requestedTime = value) }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(notes = value) }

    /** Hoy (0), mañana (1), pasado mañana (2)… */
    fun setDateInDays(days: Long) {
        _uiState.value = _uiState.value.copy(requestedDate = LocalDate.now().plusDays(days).toString())
    }

    fun setHour(hour: Int) {
        _uiState.value = _uiState.value.copy(requestedTime = "%02d:00:00".format(hour))
    }

    // ----- Domicilio -----

    fun onServiceAddressChange(value: String) {
        _uiState.value = _uiState.value.copy(
            serviceAddress = value,
            serviceLatitude = null,
            serviceLongitude = null,
            googlePlaceId = null,
            addressSuggestions = emptyList(),
            addressError = null
        )
        addressSearchJob?.cancel()
        if (value.trim().length < 3) return

        addressSearchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = _uiState.value.copy(isAddressSearching = true)
            runCatching { googlePlacesRepository.search(value) }
                .onSuccess { suggestions ->
                    _uiState.value = _uiState.value.copy(
                        addressSuggestions = suggestions,
                        isAddressSearching = false
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        addressSuggestions = emptyList(),
                        isAddressSearching = false,
                        addressError = exception.toUserMessage()
                    )
                }
        }
    }

    fun selectAddress(suggestion: AddressSuggestion) {
        addressSearchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isAddressSearching = true,
            addressSuggestions = emptyList(),
            addressError = null
        )
        viewModelScope.launch {
            runCatching { googlePlacesRepository.getAddress(suggestion.placeId) }
                .onSuccess { address ->
                    _uiState.value = _uiState.value.copy(
                        serviceAddress = address.address.ifBlank { suggestion.fullText },
                        serviceLatitude = address.latitude,
                        serviceLongitude = address.longitude,
                        googlePlaceId = address.placeId,
                        isAddressSearching = false
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isAddressSearching = false,
                        addressError = exception.toUserMessage()
                    )
                }
        }
    }

    // ----- Productos -----

    /** Agrega una pieza del producto/variante; si ya estaba en la lista, solo suma. */
    fun addProduct(product: ProductDetail, variant: BulbTypeVariant?) {
        val state = _uiState.value
        val existing = state.selectedProducts.firstOrNull { it.product.id == product.id && it.bulbTypeId == variant?.id }
        _uiState.value = state.copy(
            selectedProducts = if (existing != null) {
                state.selectedProducts.map { if (it === existing) it.copy(quantity = it.quantity + 1) else it }
            } else {
                state.selectedProducts + SelectedProduct(product, 1, variant?.id)
            }
        )
    }

    fun changeQuantity(selected: SelectedProduct, quantity: Int) {
        if (quantity < 1) return
        _uiState.value = _uiState.value.let { state ->
            state.copy(selectedProducts = state.selectedProducts.map { if (it === selected) it.copy(quantity = quantity) else it })
        }
    }

    fun removeProduct(selected: SelectedProduct) {
        _uiState.value = _uiState.value.let { state ->
            state.copy(selectedProducts = state.selectedProducts.filterNot { it === selected })
        }
    }

    // ----- Guardar -----

    fun save() {
        val state = _uiState.value
        if (state.customerName.isBlank() || state.customerPhone.isBlank()) {
            _uiState.value = state.copy(error = "El nombre y el teléfono del cliente son obligatorios.")
            return
        }

        _uiState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            val trimId = vehicleSearcher.selectedTrimId
            val items = state.selectedProducts
                .map { CreateQuoteItemRequest(it.product.id, it.quantity, it.bulbTypeId) }
                .takeIf { it.isNotEmpty() && !state.isRevision }
            val visitFeeAmount = if (state.isRevision) state.visitFee else null

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
                        serviceAddress = state.serviceAddress.ifBlank { null },
                        serviceLatitude = state.serviceLatitude,
                        serviceLongitude = state.serviceLongitude,
                        googlePlaceId = state.googlePlaceId,
                        vehicleTrimId = trimId,
                        serviceType = state.serviceType,
                        visitFeeAmount = visitFeeAmount,
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
                        serviceAddress = state.serviceAddress.ifBlank { null },
                        serviceLatitude = state.serviceLatitude,
                        serviceLongitude = state.serviceLongitude,
                        googlePlaceId = state.googlePlaceId,
                        vehicleTrimId = trimId,
                        serviceType = state.serviceType,
                        visitFeeAmount = visitFeeAmount,
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
