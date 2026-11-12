package com.rodvarled.admin.ui.inventory

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.printing.InventoryLabelRenderer
import com.rodvarled.admin.core.printing.NiimbotB1Printer
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.InventoryUnit
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.data.repository.CatalogRepository
import com.rodvarled.admin.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryLabelsUiState(
    val isLoading: Boolean = true,
    val product: ProductDetail? = null,
    val selectedVariantId: Int? = null,
    val quantityText: String = "1",
    val copiesText: String = "1",
    val units: List<InventoryUnit> = emptyList(),
    val previews: List<Bitmap> = emptyList(),
    val printers: List<NiimbotB1Printer.Device> = emptyList(),
    val selectedPrinter: NiimbotB1Printer.Device? = null,
    val isBusy: Boolean = false,
    val busyMessage: String? = null,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class InventoryLabelsViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val inventoryRepository: InventoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val productId: Int = checkNotNull(savedStateHandle["id"])
    private val _uiState = MutableStateFlow(InventoryLabelsUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            catalogRepository.getProduct(productId)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, product = it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.toUserMessage()) }
        }
    }

    fun onVariantChange(id: Int?) { _uiState.value = _uiState.value.copy(selectedVariantId = id, units = emptyList(), previews = emptyList()) }
    fun onQuantityChange(value: String) { _uiState.value = _uiState.value.copy(quantityText = value.filter { it.isDigit() }.take(3)) }
    fun onCopiesChange(value: String) { _uiState.value = _uiState.value.copy(copiesText = value.filter { it.isDigit() }.take(2)) }

    fun scanPrinters(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyMessage = "Buscando impresora…", error = null)
            runCatching { NiimbotB1Printer.scan(context) }
                .onSuccess { devices ->
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        busyMessage = null,
                        printers = devices,
                        selectedPrinter = devices.firstOrNull(),
                        message = if (devices.isEmpty()) "No encontramos una NIIMBOT B1 encendida." else "Impresora lista: ${devices.first().name}."
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(isBusy = false, busyMessage = null, error = it.toUserMessage()) }
        }
    }

    fun selectPrinter(device: NiimbotB1Printer.Device) { _uiState.value = _uiState.value.copy(selectedPrinter = device) }

    fun generateLabels() {
        val state = _uiState.value
        val product = state.product ?: return
        val quantity = state.quantityText.toIntOrNull() ?: 0
        if (quantity !in 1..500) {
            _uiState.value = state.copy(error = "Captura una cantidad entre 1 y 500.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyMessage = "Generando etiquetas…", error = null, message = null)
            inventoryRepository.createLabels(product.id, state.selectedVariantId, quantity)
                .onSuccess { units ->
                    val variantName = product.variants.firstOrNull { it.id == state.selectedVariantId }?.bulbTypeName
                    val previews = units.map { unit -> InventoryLabelRenderer.render(product.name, variantName, unit.code) }
                    _uiState.value = _uiState.value.copy(isBusy = false, busyMessage = null, units = units, previews = previews, message = "${units.size} etiqueta(s) listas. Puedes reimprimirlas si lo necesitas.")
                }
                .onFailure { _uiState.value = _uiState.value.copy(isBusy = false, busyMessage = null, error = it.toUserMessage()) }
        }
    }

    fun printLabels(context: android.content.Context) {
        val state = _uiState.value
        val printer = state.selectedPrinter
        if (printer == null) {
            _uiState.value = state.copy(error = "Primero busca y selecciona la NIIMBOT.")
            return
        }
        if (state.previews.isEmpty()) {
            _uiState.value = state.copy(error = "Genera las etiquetas antes de imprimir.")
            return
        }
        val copies = state.copiesText.toIntOrNull() ?: 0
        if (copies !in 1..99) {
            _uiState.value = state.copy(error = "Captura entre 1 y 99 copias por etiqueta.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyMessage = "Imprimiendo…", error = null, message = null)
            runCatching {
                NiimbotB1Printer.print(context, printer.address, state.previews, copies) { completed, total ->
                    _uiState.value = _uiState.value.copy(
                        busyMessage = "Preparando etiqueta $completed de $total…"
                    )
                }
            }
                .onSuccess { _uiState.value = _uiState.value.copy(isBusy = false, busyMessage = null, message = "Impresión enviada a ${printer.name}.") }
                .onFailure { _uiState.value = _uiState.value.copy(isBusy = false, busyMessage = null, error = it.toUserMessage()) }
        }
    }

    fun consumeMessage() { _uiState.value = _uiState.value.copy(message = null) }
}
