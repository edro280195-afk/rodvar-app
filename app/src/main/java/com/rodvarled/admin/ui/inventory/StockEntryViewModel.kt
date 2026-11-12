package com.rodvarled.admin.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodvarled.admin.core.pricing.ChargeMath
import com.rodvarled.admin.core.printing.LabelPrinter
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.BulbTypeVariant
import com.rodvarled.admin.data.remote.dto.InventoryUnit
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.data.remote.dto.ReceiveInventoryRequest
import com.rodvarled.admin.data.repository.CatalogRepository
import com.rodvarled.admin.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** RECEIVE: llega mercancía (stock + costo + etiquetas). LABELS: solo etiquetar piezas que ya están en inventario. */
enum class StockEntryMode { RECEIVE, LABELS }

enum class PrintStatus { IDLE, PRINTING, DONE, FAILED }

data class StockEntryUiState(
    val mode: StockEntryMode = StockEntryMode.RECEIVE,
    val products: List<ProductDetail> = emptyList(),
    val product: ProductDetail? = null,
    val variant: BulbTypeVariant? = null,

    val quantity: Int = 1,
    /** Costo por pieza en pesos: lo único que hay que capturar. */
    val costMxn: Double = 0.0,
    val showUsd: Boolean = false,
    val usd: Double = 0.0,
    val exchangeRate: Double = 0.0,
    val printLabels: Boolean = true,

    val isBusy: Boolean = false,
    val busyMessage: String? = null,
    val error: String? = null,

    /** La entrada o las etiquetas ya quedaron guardadas en el servidor. */
    val saved: Boolean = false,
    val newStock: Int? = null,
    val units: List<InventoryUnit> = emptyList(),
    val printStatus: PrintStatus = PrintStatus.IDLE,
    val printError: String? = null,
    val printerName: String? = null
) {
    val isReceive: Boolean get() = mode == StockEntryMode.RECEIVE

    /** Costo por pieza que se va a guardar: dólares × tipo de cambio si se capturaron, o los pesos directos. */
    val effectiveCostMxn: Double get() =
        if (showUsd && usd > 0.0 && exchangeRate > 0.0) ChargeMath.round2(usd * exchangeRate) else costMxn

    /** Antes de guardar, ¿esta pantalla va a mandar a imprimir? */
    val willPrint: Boolean get() = !isReceive || printLabels

    val variantLabel: String? get() = variant?.bulbTypeName?.takeIf { it.isNotBlank() }
}

@HiltViewModel
class StockEntryViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val inventoryRepository: InventoryRepository,
    private val labelPrinter: LabelPrinter,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Ruta de etiquetas: inventory/{id}/labels. Ruta de entrada: inventory-receive?productId=...
    private val labelsProductId: Int? = savedStateHandle.get<Int>("id")
    private val receiveProductId: Int? = savedStateHandle.get<Int>("productId")?.takeIf { it > 0 }
    private val initialProductId: Int? = labelsProductId ?: receiveProductId

    private val _uiState = MutableStateFlow(
        StockEntryUiState(
            mode = if (labelsProductId != null) StockEntryMode.LABELS else StockEntryMode.RECEIVE,
            printerName = labelPrinter.lastPrinterName
        )
    )
    val uiState = _uiState.asStateFlow()

    private var preselected = false

    init {
        viewModelScope.launch {
            catalogRepository.observeProducts().collect { products ->
                val state = _uiState.value
                _uiState.value = state.copy(products = products)
                if (!preselected && initialProductId != null) {
                    products.firstOrNull { it.id == initialProductId }?.let {
                        preselected = true
                        selectProduct(it, null)
                    }
                }
            }
        }
        viewModelScope.launch { catalogRepository.refresh() }
    }

    fun selectProduct(product: ProductDetail, variant: BulbTypeVariant?) {
        val state = _uiState.value
        // El costo se sugiere con el último que se capturó para ese producto o variante.
        val lastCost = variant?.costOverride ?: product.cost
        _uiState.value = state.copy(
            product = product,
            variant = variant,
            costMxn = if (lastCost > 0.0) lastCost else 0.0,
            error = null
        )
    }

    fun selectVariant(variant: BulbTypeVariant?) {
        val product = _uiState.value.product ?: return
        selectProduct(product, variant)
    }

    fun onQuantityChange(quantity: Int) {
        _uiState.value = _uiState.value.copy(quantity = quantity.coerceIn(1, 500), error = null)
    }

    fun onCostChange(value: Double) { _uiState.value = _uiState.value.copy(costMxn = value, error = null) }
    fun onUsdChange(value: Double) { _uiState.value = _uiState.value.copy(usd = value, error = null) }
    fun onExchangeRateChange(value: Double) { _uiState.value = _uiState.value.copy(exchangeRate = value, error = null) }
    fun onShowUsdChange(show: Boolean) { _uiState.value = _uiState.value.copy(showUsd = show) }
    fun onPrintLabelsChange(print: Boolean) { _uiState.value = _uiState.value.copy(printLabels = print) }

    /** Guarda la entrada (o crea las etiquetas) y, si corresponde, imprime. */
    fun submit() {
        val state = _uiState.value
        val product = state.product
        if (product == null) {
            _uiState.value = state.copy(error = "Elige primero el producto.")
            return
        }
        if (state.isReceive && state.effectiveCostMxn <= 0.0) {
            _uiState.value = state.copy(error = "Captura el costo por pieza.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyMessage = "Guardando…", error = null, printError = null)

            val result = if (state.isReceive) {
                inventoryRepository.receive(
                    ReceiveInventoryRequest(
                        productId = product.id,
                        productBulbTypeId = state.variant?.id,
                        quantity = state.quantity,
                        unitCostMxn = state.effectiveCostMxn,
                        createLabels = state.printLabels
                    )
                ).map { it.units to it.newStock }
            } else {
                inventoryRepository.createLabels(product.id, state.variant?.id, state.quantity)
                    .map { it to null as Int? }
            }

            result
                .onSuccess { (units, newStock) ->
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        busyMessage = null,
                        saved = true,
                        newStock = newStock,
                        units = units
                    )
                    if (units.isNotEmpty()) printUnits()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isBusy = false, busyMessage = null, error = it.toUserMessage())
                }
        }
    }

    /** Reintenta la impresión del lote ya guardado (no vuelve a registrar nada). */
    fun retryPrint() {
        if (_uiState.value.units.isEmpty()) return
        viewModelScope.launch { printUnits() }
    }

    /** Modo etiquetas: vuelve a imprimir las etiquetas que siguen disponibles (por ejemplo, una se despegó). */
    fun reprintAvailable() {
        val state = _uiState.value
        val product = state.product ?: run {
            _uiState.value = state.copy(error = "Elige primero el producto.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, busyMessage = "Buscando etiquetas…", error = null)
            inventoryRepository.getUnits(product.id)
                .onSuccess { all ->
                    val units = all.filter { it.status == "Disponible" && it.productBulbTypeId == state.variant?.id }
                    if (units.isEmpty()) {
                        _uiState.value = _uiState.value.copy(isBusy = false, busyMessage = null, error = "No hay etiquetas disponibles de este producto para reimprimir.")
                    } else {
                        _uiState.value = _uiState.value.copy(isBusy = false, busyMessage = null, saved = true, units = units)
                        printUnits()
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isBusy = false, busyMessage = null, error = it.toUserMessage())
                }
        }
    }

    private suspend fun printUnits() {
        val units = _uiState.value.units
        _uiState.value = _uiState.value.copy(printStatus = PrintStatus.PRINTING, printError = null, busyMessage = "Imprimiendo…", isBusy = true)
        try {
            val printerName = labelPrinter.print(units) { completed, total ->
                _uiState.value = _uiState.value.copy(busyMessage = "Preparando etiqueta $completed de $total…")
            }
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                busyMessage = null,
                printStatus = PrintStatus.DONE,
                printerName = printerName
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                busyMessage = null,
                printStatus = PrintStatus.FAILED,
                printError = error.toUserMessage()
            )
        }
    }

    /** Deja la pantalla lista para registrar otra entrada, conservando el producto. */
    fun startAnother() {
        val state = _uiState.value
        _uiState.value = state.copy(
            quantity = 1,
            saved = false,
            newStock = null,
            units = emptyList(),
            printStatus = PrintStatus.IDLE,
            printError = null,
            error = null
        )
    }
}
