package com.rodvarled.admin.ui.installations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.media.LocalMedia
import com.rodvarled.admin.core.media.MediaFiles
import com.rodvarled.admin.core.media.MediaStage
import com.rodvarled.admin.core.media.MediaUploadScheduler
import com.rodvarled.admin.core.pricing.ChargeMath
import com.rodvarled.admin.core.pricing.ChargeState
import com.rodvarled.admin.core.pricing.toChargeLine
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.BulbTypeVariant
import com.rodvarled.admin.data.remote.dto.CreateDirectInstallationItem
import com.rodvarled.admin.data.remote.dto.CreateDirectInstallationRequest
import com.rodvarled.admin.data.remote.dto.CustomerLookup
import com.rodvarled.admin.data.remote.dto.DEFAULT_HOME_VISIT_FEE
import com.rodvarled.admin.data.remote.dto.DirectInstallationResponse
import com.rodvarled.admin.data.remote.dto.InventoryUnit
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.data.remote.dto.SERVICE_TYPE_INSTALLATION
import com.rodvarled.admin.data.remote.dto.SERVICE_TYPE_REVISION
import com.rodvarled.admin.data.repository.CatalogRepository
import com.rodvarled.admin.data.repository.CustomersRepository
import com.rodvarled.admin.data.repository.InstallationsRepository
import com.rodvarled.admin.data.repository.InventoryRepository
import com.rodvarled.admin.data.repository.SettingsRepository
import com.rodvarled.admin.data.repository.VehiclesRepository
import com.rodvarled.admin.ui.components.VehicleSearcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DirectInstallationUiState(
    val isSaving: Boolean = false,
    val error: String? = null,

    val customerName: String = "",
    val customerPhone: String = "",
    val customerSuggestions: List<CustomerLookup> = emptyList(),

    val vehiclePlate: String = "",
    val vehicleColor: String = "",

    val availableProducts: List<ProductDetail> = emptyList(),

    /** "Instalacion" o "Revision" (solo se cobra la visita a domicilio). */
    val serviceType: String = SERVICE_TYPE_INSTALLATION,
    /** Renglones, revisión y total. El total sugerido es la suma; se puede cambiar a lo negociado. */
    val charge: ChargeState = ChargeState(),
    val homeVisitFee: Double = DEFAULT_HOME_VISIT_FEE,
    val paymentMethod: String = "Efectivo",

    val technicianNotes: String = "",
    /** Fotos y videos del trabajo: se guardan en el teléfono y se suben solos al registrar. */
    val media: List<LocalMedia> = emptyList(),

    val completed: DirectInstallationResponse? = null
) {
    val isRevision: Boolean get() = serviceType == SERVICE_TYPE_REVISION
}

@HiltViewModel
class DirectInstallationViewModel @Inject constructor(
    private val installationsRepository: InstallationsRepository,
    private val customersRepository: CustomersRepository,
    vehiclesRepository: VehiclesRepository,
    private val catalogRepository: CatalogRepository,
    private val inventoryRepository: InventoryRepository,
    private val settingsRepository: SettingsRepository,
    private val mediaScheduler: MediaUploadScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(DirectInstallationUiState())
    val uiState = _uiState.asStateFlow()

    /** Búsqueda de vehículo por texto ("civic 2018"). */
    val vehicleSearcher = VehicleSearcher(viewModelScope, vehiclesRepository)

    private var customerLookupJob: Job? = null

    init {
        loadProducts()
        loadHomeVisitFee()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            catalogRepository.observeProducts().collect { products ->
                _uiState.value = _uiState.value.copy(availableProducts = products.filter { it.stock > 0 || it.variants.any { v -> v.stock > 0 } })
            }
        }
        viewModelScope.launch { catalogRepository.refresh() }
    }

    private fun loadHomeVisitFee() {
        viewModelScope.launch {
            val fee = settingsRepository.getHomeVisitFee()
            val state = _uiState.value
            // Si ya se eligió "solo revisión" antes de que llegara la tarifa, se ajusta el cobro.
            val charge = if (state.isRevision && state.charge.visitFeeCharged == state.homeVisitFee) {
                ChargeMath.withVisitFeeCharged(state.charge, fee)
            } else state.charge
            _uiState.value = state.copy(homeVisitFee = fee, charge = charge)
        }
    }

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

    /** Al elegir un cliente conocido se rellenan solos su nombre, teléfono y último vehículo. */
    fun pickCustomerSuggestion(customer: CustomerLookup) {
        customerLookupJob?.cancel()
        _uiState.value = _uiState.value.copy(
            customerName = customer.name,
            customerPhone = customer.phone,
            customerSuggestions = emptyList()
        )
        if (vehicleSearcher.state.value.selected == null && customer.lastVehicleTrimId != null && !customer.lastVehicleLabel.isNullOrBlank()) {
            vehicleSearcher.selectKnown(customer.lastVehicleTrimId, customer.lastVehicleLabel)
        }
    }

    fun onPlateChange(value: String) { _uiState.value = _uiState.value.copy(vehiclePlate = value) }
    fun onColorChange(value: String) { _uiState.value = _uiState.value.copy(vehicleColor = value) }
    fun onPaymentMethodChange(value: String) { _uiState.value = _uiState.value.copy(paymentMethod = value) }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(technicianNotes = value) }

    // ----- Fotos y videos -----

    fun addMedia(items: List<LocalMedia>) {
        _uiState.value = _uiState.value.copy(media = _uiState.value.media + items)
    }

    fun removeMedia(item: LocalMedia) {
        MediaFiles.discard(item)
        _uiState.value = _uiState.value.copy(media = _uiState.value.media.filterNot { it.id == item.id })
    }

    fun changeMediaStage(item: LocalMedia, stage: MediaStage) {
        _uiState.value = _uiState.value.copy(media = _uiState.value.media.map { if (it.id == item.id) it.copy(stage = stage) else it })
    }

    private var mediaHandedOff = false

    override fun onCleared() {
        // Si se salió sin registrar el trabajo, los archivos que no se mandaron a subir se borran del teléfono.
        if (!mediaHandedOff) _uiState.value.media.forEach(MediaFiles::discard)
        super.onCleared()
    }

    /** Cambia entre instalar productos y "solo revisión" (se cobra la visita y no hay productos ni garantía). */
    fun onServiceTypeChange(serviceType: String) {
        val state = _uiState.value
        if (state.serviceType == serviceType) return
        val charge = if (serviceType == SERVICE_TYPE_REVISION) {
            ChargeState(visitFeeCharged = state.homeVisitFee)
        } else {
            ChargeState()
        }
        _uiState.value = state.copy(serviceType = serviceType, charge = charge, error = null)
    }

    // ----- Cobro -----

    fun addProduct(product: ProductDetail, variant: BulbTypeVariant?) {
        val state = _uiState.value
        _uiState.value = state.copy(charge = ChargeMath.withAddedLine(state.charge, product.toChargeLine(variant)), error = null)
    }

    fun onUnitPriceChange(key: String, price: Double) = updateCharge { ChargeMath.withUnitPrice(it, key, price) }
    fun onQuantityChange(key: String, quantity: Int) = updateCharge { ChargeMath.withQuantity(it, key, quantity) }
    fun removeLine(key: String) = updateCharge { ChargeMath.withoutLine(it, key) }
    fun onTotalChange(total: Double) = updateCharge { ChargeMath.withTotal(it, total) }
    fun onVisitChargedChange(amount: Double) = updateCharge { ChargeMath.withVisitFeeCharged(it, amount) }
    fun onVisitCreditChange(amount: Double) = updateCharge { ChargeMath.withVisitFeeCredit(it, amount) }

    private fun updateCharge(transform: (ChargeState) -> ChargeState) {
        _uiState.value = _uiState.value.let { it.copy(charge = transform(it.charge)) }
    }

    fun onInventoryUnitScanned(code: String) {
        viewModelScope.launch {
            inventoryRepository.getUnit(code)
                .onSuccess { unit -> addInventoryUnit(unit) }
                .onFailure { error -> _uiState.value = _uiState.value.copy(error = error.toUserMessage()) }
        }
    }

    private suspend fun addInventoryUnit(unit: InventoryUnit) {
        if (unit.status != "Disponible") {
            _uiState.value = _uiState.value.copy(error = "Esta unidad ya está marcada como ${unit.status.lowercase()}.")
            return
        }

        val product = _uiState.value.availableProducts.firstOrNull { it.id == unit.productId }
            ?: catalogRepository.getProduct(unit.productId).getOrNull()
        if (product == null) {
            _uiState.value = _uiState.value.copy(error = "El producto de esta etiqueta ya no está disponible.")
            return
        }

        val variant = unit.productBulbTypeId?.let { id -> product.variants.firstOrNull { it.id == id } }
        val state = _uiState.value
        _uiState.value = state.copy(
            // Escanear una caja implica instalar producto: si estaba en "solo revisión", pasa a instalación.
            serviceType = SERVICE_TYPE_INSTALLATION,
            charge = ChargeMath.withScannedUnit(
                if (state.isRevision) ChargeState() else state.charge,
                unit.code
            ) { product.toChargeLine(variant) },
            error = null
        )
    }

    fun submit() {
        val state = _uiState.value
        if (state.customerName.isBlank() || state.customerPhone.isBlank()) {
            _uiState.value = state.copy(error = "El nombre y el teléfono del cliente son obligatorios.")
            return
        }
        val charge = state.charge
        if (state.isRevision && charge.lines.isNotEmpty()) {
            _uiState.value = state.copy(error = "Una revisión no lleva productos. Cambia a Instalación o quítalos.")
            return
        }

        ChargeMath.scanProblem(charge)?.let {
            _uiState.value = state.copy(error = it)
            return
        }

        _uiState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            val request = CreateDirectInstallationRequest(
                customerName = state.customerName.trim(),
                customerPhone = state.customerPhone.trim(),
                vehicleTrimId = vehicleSearcher.selectedTrimId,
                vehiclePlate = state.vehiclePlate.ifBlank { null },
                vehicleColor = state.vehicleColor.ifBlank { null },
                technicianNotes = state.technicianNotes.ifBlank { null },
                paymentMethod = state.paymentMethod,
                totalAmount = charge.total,
                serviceType = state.serviceType,
                visitFeeCharged = charge.visitFeeCharged.takeIf { it > 0.0 },
                visitFeeCredit = charge.visitFeeCredit.takeIf { it > 0.0 },
                items = charge.lines.map {
                    // El precio que se ve en pantalla es el que se cobra y el que queda registrado.
                    CreateDirectInstallationItem(it.productId, it.quantity, it.variantId, it.unitPrice, it.inventoryUnitCodes)
                }
            )

            installationsRepository.createDirectInstallation(request)
                .onSuccess { response ->
                    // Las fotos y videos (opcionales) se suben en segundo plano, ya con la instalación creada.
                    if (state.media.isNotEmpty()) {
                        mediaScheduler.enqueue(response.installationId, state.media)
                        mediaHandedOff = true
                    }
                    _uiState.value = _uiState.value.copy(isSaving = false, completed = response)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isSaving = false, error = it.toUserMessage())
                }
        }
    }
}
