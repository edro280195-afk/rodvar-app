package com.rodvarled.admin.ui.appointments

import androidx.lifecycle.SavedStateHandle
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
import com.rodvarled.admin.data.remote.dto.AppointmentSummary
import com.rodvarled.admin.data.remote.dto.BulbTypeVariant
import com.rodvarled.admin.data.remote.dto.DEFAULT_HOME_VISIT_FEE
import com.rodvarled.admin.data.remote.dto.InstallationItemSelectionRequest
import com.rodvarled.admin.data.remote.dto.InventoryUnit
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.data.repository.AppointmentsRepository
import com.rodvarled.admin.data.repository.CatalogRepository
import com.rodvarled.admin.data.repository.InventoryRepository
import com.rodvarled.admin.data.repository.QuotesRepository
import com.rodvarled.admin.data.repository.SettingsRepository
import com.rodvarled.admin.data.repository.WarrantiesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val PAYMENT_METHODS = listOf("Efectivo", "Tarjeta", "Transferencia")

data class CompleteInstallationUiState(
    val isLoading: Boolean = true,
    val appointment: AppointmentSummary? = null,
    /** Falso si no se pudieron cargar los productos de la cotización: no se puede cerrar así. */
    val itemsLoaded: Boolean = false,

    val charge: ChargeState = ChargeState(),
    val homeVisitFee: Double = DEFAULT_HOME_VISIT_FEE,
    val availableProducts: List<ProductDetail> = emptyList(),

    val vehiclePlate: String = "",
    val vehicleColor: String = "",
    val paymentMethod: String = "Efectivo",
    val technicianNotes: String = "",
    val acceptedTerms: Boolean = false,
    /** Fotos y videos del trabajo: se guardan en el teléfono y se suben solos al terminar. */
    val media: List<LocalMedia> = emptyList(),
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val completedWarrantyId: Int? = null
) {
    /** La cita se agendó como revisión a domicilio. */
    val isRevision: Boolean get() = appointment?.isRevision == true

    /** Revisión sin productos: solo se cobra la visita, no hay garantía ni firma. */
    val isVisitOnly: Boolean get() = isRevision && charge.lines.isEmpty()
}

@HiltViewModel
class CompleteInstallationViewModel @Inject constructor(
    private val appointmentsRepository: AppointmentsRepository,
    private val warrantiesRepository: WarrantiesRepository,
    private val mediaScheduler: MediaUploadScheduler,
    private val inventoryRepository: InventoryRepository,
    private val quotesRepository: QuotesRepository,
    private val catalogRepository: CatalogRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val appointmentId: Int = checkNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow(CompleteInstallationUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProducts()
        load()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            catalogRepository.observeProducts().collect { products ->
                _uiState.value = _uiState.value.copy(availableProducts = products.filter { it.stock > 0 || it.variants.any { v -> v.stock > 0 } })
            }
        }
        viewModelScope.launch { catalogRepository.refresh() }
    }

    /** Carga la cita, su cotización y la tarifa de revisión para armar el cobro. */
    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val fee = settingsRepository.getHomeVisitFee()
            val appointment = appointmentsRepository.getAppointment(appointmentId).getOrElse {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage())
                return@launch
            }

            val lines = appointment.quoteId?.let { quoteId ->
                quotesRepository.getItems(quoteId).getOrElse {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        appointment = appointment,
                        homeVisitFee = fee,
                        error = "No se pudieron cargar los productos de la cotización: ${it.toUserMessage()}"
                    )
                    return@launch
                }.map { item ->
                    val variantName = item.productBulbTypeId?.let { variantId ->
                        catalogRepository.getProduct(item.productId).getOrNull()
                            ?.variants?.firstOrNull { it.id == variantId }?.bulbTypeName
                    }
                    item.toChargeLine(variantName)
                }
            }.orEmpty()

            // Una revisión sin productos cobra la visita; con productos, la visita no se suma.
            val visitFee = if (appointment.isRevision && lines.isEmpty()) appointment.visitFeeAmount ?: fee else 0.0

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                itemsLoaded = true,
                appointment = appointment,
                homeVisitFee = fee,
                charge = ChargeState(lines = lines, visitFeeCharged = visitFee),
                error = null
            )
        }
    }

    fun onPlateChange(value: String) { _uiState.value = _uiState.value.copy(vehiclePlate = value) }
    fun onColorChange(value: String) { _uiState.value = _uiState.value.copy(vehicleColor = value) }
    fun onPaymentMethodChange(value: String) { _uiState.value = _uiState.value.copy(paymentMethod = value) }
    fun onNotesChange(value: String) { _uiState.value = _uiState.value.copy(technicianNotes = value) }
    fun onAcceptedTermsChange(value: Boolean) { _uiState.value = _uiState.value.copy(acceptedTerms = value) }

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
        // Si se salió sin terminar el trabajo, los archivos que no se mandaron a subir se borran del teléfono.
        if (!mediaHandedOff) _uiState.value.media.forEach(MediaFiles::discard)
        super.onCleared()
    }

    // ----- Cobro -----

    fun addProduct(product: ProductDetail, variant: BulbTypeVariant?) =
        updateCharge { ChargeMath.withAddedLine(it, product.toChargeLine(variant)) }

    fun onUnitPriceChange(key: String, price: Double) = updateCharge { ChargeMath.withUnitPrice(it, key, price) }
    fun onQuantityChange(key: String, quantity: Int) = updateCharge { ChargeMath.withQuantity(it, key, quantity) }
    fun removeLine(key: String) = updateCharge { ChargeMath.withoutLine(it, key) }
    fun onTotalChange(total: Double) = updateCharge { ChargeMath.withTotal(it, total) }
    fun onVisitChargedChange(amount: Double) = updateCharge { ChargeMath.withVisitFeeCharged(it, amount) }
    fun onVisitCreditChange(amount: Double) = updateCharge { ChargeMath.withVisitFeeCredit(it, amount) }

    private fun updateCharge(transform: (ChargeState) -> ChargeState) {
        val state = _uiState.value
        var charge = transform(state.charge)

        // En una cita de revisión, al agregar el primer producto la visita deja de cobrarse aparte;
        // si se quitan todos, vuelve a cobrarse la tarifa acordada.
        if (state.isRevision) {
            val agreedFee = state.appointment?.visitFeeAmount ?: state.homeVisitFee
            if (state.charge.lines.isEmpty() && charge.lines.isNotEmpty()) {
                charge = charge.copy(visitFeeCharged = 0.0, totalOverride = null)
            } else if (state.charge.lines.isNotEmpty() && charge.lines.isEmpty()) {
                charge = charge.copy(visitFeeCharged = agreedFee, totalOverride = null)
            }
        }
        _uiState.value = state.copy(charge = charge, error = null)
    }

    fun onInventoryUnitScanned(code: String) {
        viewModelScope.launch {
            if (_uiState.value.charge.lines.any { code in it.inventoryUnitCodes }) return@launch
            inventoryRepository.getUnit(code)
                .onSuccess { unit -> addInventoryUnit(unit) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.toUserMessage()) }
        }
    }

    private suspend fun addInventoryUnit(unit: InventoryUnit) {
        if (unit.status != "Disponible") {
            _uiState.value = _uiState.value.copy(error = "La etiqueta ${unit.code} ya está marcada como ${unit.status.lowercase()}.")
            return
        }

        val product = _uiState.value.availableProducts.firstOrNull { it.id == unit.productId }
            ?: catalogRepository.getProduct(unit.productId).getOrNull()
        if (product == null) {
            _uiState.value = _uiState.value.copy(error = "El producto de esta etiqueta ya no está disponible.")
            return
        }

        val variant = unit.productBulbTypeId?.let { id -> product.variants.firstOrNull { it.id == id } }
        updateCharge { ChargeMath.withScannedUnit(it, unit.code) { product.toChargeLine(variant) } }
    }

    fun submit(signatureBase64: String?) {
        val state = _uiState.value

        if (!state.itemsLoaded) {
            _uiState.value = state.copy(error = "Falta cargar los productos de la cotización. Reintenta antes de completar.")
            return
        }
        ChargeMath.scanProblem(state.charge)?.let {
            _uiState.value = state.copy(error = it)
            return
        }
        if (!state.isVisitOnly) {
            if (signatureBase64.isNullOrBlank()) {
                _uiState.value = state.copy(error = "La firma del cliente es obligatoria para completar la instalación.")
                return
            }
            if (!state.acceptedTerms) {
                _uiState.value = state.copy(error = "El cliente debe aceptar los términos de garantía.")
                return
            }
        }
        _uiState.value = state.copy(isSubmitting = true, error = null)

        viewModelScope.launch {
            val charge = state.charge

            // Todo el cobro viaja en una sola llamada: renglones con el precio acordado, total final,
            // revisión y datos del vehículo. Antes se mandaba en pasos sueltos que podían quedar a medias.
            val statusResult = appointmentsRepository.updateStatus(
                id = appointmentId,
                status = "Instalada",
                items = charge.lines.map {
                    InstallationItemSelectionRequest(
                        productId = it.productId,
                        productBulbTypeId = it.variantId,
                        quantity = it.quantity,
                        unitPrice = it.unitPrice,
                        inventoryUnitCodes = it.inventoryUnitCodes
                    )
                },
                itemsAreFinal = true,
                totalAmount = charge.total,
                visitFeeCharged = if (state.isRevision) charge.visitFeeCharged else charge.visitFeeCharged.takeIf { it > 0.0 },
                visitFeeCredit = charge.visitFeeCredit.takeIf { it > 0.0 },
                paymentMethod = state.paymentMethod,
                vehiclePlate = state.vehiclePlate.ifBlank { null },
                vehicleColor = state.vehicleColor.ifBlank { null },
                technicianNotes = state.technicianNotes.ifBlank { null }
            )
            val updatedAppointment = statusResult.getOrNull()
            if (updatedAppointment == null) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = statusResult.exceptionOrNull()?.toUserMessage() ?: "No se pudo marcar la cita como instalada.")
                return@launch
            }

            val warrantyId = updatedAppointment.warrantyId
            if (warrantyId != null && !signatureBase64.isNullOrBlank()) {
                warrantiesRepository.sign(warrantyId, signatureBase64)
                    .onFailure {
                        _uiState.value = _uiState.value.copy(isSubmitting = false, error = "La cita se marcó como instalada, pero la firma falló: ${it.toUserMessage()}")
                        return@launch
                    }
            }

            // Las fotos y videos se suben en segundo plano: no hace falta esperar a que terminen.
            val installationId = updatedAppointment.installationId
            if (installationId != null && state.media.isNotEmpty()) {
                mediaScheduler.enqueue(installationId, state.media)
                mediaHandedOff = true
            }

            // Sin garantía (revisión sola o cita sin productos) se regresa a la lista de citas.
            _uiState.value = _uiState.value.copy(isSubmitting = false, completedWarrantyId = warrantyId ?: -1)
        }
    }
}
