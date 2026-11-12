package com.rodvarled.admin.ui.inventory

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.core.printing.NiimbotB1Printer
import com.rodvarled.admin.data.remote.dto.BulbTypeVariant
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.components.MoneyField
import com.rodvarled.admin.ui.components.ProductPickerSheet
import com.rodvarled.admin.ui.components.formatMoney
import com.rodvarled.admin.ui.theme.RodvarPrimary
import com.rodvarled.admin.ui.theme.RodvarPrimaryContainer
import com.rodvarled.admin.ui.theme.RodvarRedContainer
import com.rodvarled.admin.ui.theme.RodvarSecondary
import com.rodvarled.admin.ui.theme.RodvarSecondaryContainer
import com.rodvarled.admin.ui.theme.RodvarTextSecondary

/**
 * Una sola pantalla para dos cosas:
 *  - Recibir mercancía: producto, cuántas piezas y a cómo salió cada una. Un botón guarda la entrada
 *    y manda a imprimir las etiquetas QR.
 *  - Etiquetas: solo imprimir etiquetas de piezas que ya están en el inventario, o reimprimir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockEntryScreen(
    navController: NavHostController,
    viewModel: StockEntryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showProductPicker by remember { mutableStateOf(false) }

    // Si falta el permiso de Bluetooth se pide una vez y, al concederlo, sigue la acción que lo necesitaba.
    var pendingAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val action = pendingAfterPermission
        pendingAfterPermission = null
        if (grants.values.all { it }) action?.invoke()
    }

    fun withBluetooth(action: () -> Unit) {
        if (Build.VERSION.SDK_INT < 31 || NiimbotB1Printer.hasPermission(context)) {
            action()
        } else {
            pendingAfterPermission = action
            bluetoothPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (state.isReceive) "Recibir mercancía" else "Etiquetas QR", fontWeight = FontWeight.Bold)
                        Text(
                            if (state.isReceive) "Entrada de stock, costo y etiquetas en un paso" else "Imprime o reimprime etiquetas de lo que ya tienes",
                            style = MaterialTheme.typography.labelSmall,
                            color = RodvarTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        bottomBar = {
            if (!state.saved) {
                Surface(shadowElevation = 12.dp, tonalElevation = 3.dp) {
                    Column {
                        if (state.isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                if (state.willPrint) withBluetooth { viewModel.submit() } else viewModel.submit()
                            },
                            enabled = !state.isBusy && state.product != null,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(if (state.willPrint) Icons.Filled.Print else Icons.Filled.Inventory2, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    state.isBusy -> state.busyMessage ?: "Trabajando…"
                                    state.isReceive && state.printLabels -> "Guardar e imprimir ${state.quantity} etiqueta(s)"
                                    state.isReceive -> "Guardar entrada"
                                    else -> "Imprimir ${state.quantity} etiqueta(s)"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.saved) {
                ResultCard(
                    state = state,
                    onRetryPrint = { withBluetooth { viewModel.retryPrint() } },
                    onAnother = viewModel::startAnother,
                    onDone = { navController.popBackStack() }
                )
            } else {
                ProductCard(
                    state = state,
                    onPickProduct = { showProductPicker = true },
                    onVariantChange = viewModel::selectVariant
                )

                QuantityCard(
                    label = if (state.isReceive) "Piezas que llegaron" else "Etiquetas a imprimir",
                    quantity = state.quantity,
                    onChange = viewModel::onQuantityChange
                )

                if (state.isReceive) {
                    CostCard(state = state, viewModel = viewModel)
                    PrintToggleCard(state = state, onChange = viewModel::onPrintLabelsChange)
                } else {
                    OutlinedButton(
                        onClick = { withBluetooth { viewModel.reprintAvailable() } },
                        enabled = !state.isBusy && state.product != null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reimprimir las etiquetas que siguen disponibles")
                    }
                    Text(
                        "Para etiquetar piezas nuevas usa «Recibir mercancía»: ahí se guarda también lo que costó cada una.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RodvarTextSecondary
                    )
                }

                state.printerName?.let {
                    Text(
                        "Impresora recordada: $it. Se conecta sola; si no responde, la busca.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RodvarTextSecondary
                    )
                }
            }

            state.error?.let { MessageCard(text = it, positive = false) }
        }
    }

    if (showProductPicker) {
        ProductPickerSheet(
            products = state.products,
            countOf = { _, _ -> 0 },
            onPick = { product, variant ->
                viewModel.selectProduct(product, variant)
                showProductPicker = false
            },
            onDismiss = { showProductPicker = false },
            title = "¿Qué producto es?",
            onlyInStock = false
        )
    }
}

@Composable
private fun ProductCard(
    state: StockEntryUiState,
    onPickProduct: () -> Unit,
    onVariantChange: (BulbTypeVariant?) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = RodvarPrimaryContainer),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPickProduct)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Inventory2, contentDescription = null, tint = RodvarPrimary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val product = state.product
                    Text(
                        product?.name ?: "Elegir producto",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        if (product == null) "Toca para buscar por nombre o socket"
                        else listOfNotNull(
                            state.variantLabel,
                            "Hoy hay ${state.variant?.stock ?: product.stock} pieza(s)"
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = RodvarTextSecondary
                    )
                }
                Text("Cambiar", color = RodvarPrimary, style = MaterialTheme.typography.labelLarge)
            }

            val product = state.product
            if (product != null && product.variants.isNotEmpty()) {
                DropdownSelector(
                    label = "Variante / socket",
                    items = listOf<BulbTypeVariant?>(null) + product.variants,
                    selected = state.variant,
                    labelOf = { it?.bulbTypeName ?: "Producto base" },
                    onSelect = onVariantChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun QuantityCard(label: String, quantity: Int, onChange: (Int) -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { onChange(quantity - 1) }, enabled = quantity > 1) {
                    Icon(Icons.Filled.Remove, contentDescription = "Restar")
                }
                OutlinedTextField(
                    value = quantity.toString(),
                    onValueChange = { text -> text.filter(Char::isDigit).take(3).toIntOrNull()?.let(onChange) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(110.dp)
                )
                IconButton(onClick = { onChange(quantity + 1) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Sumar")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onChange(quantity + 5) }) { Text("+5") }
                TextButton(onClick = { onChange(quantity + 10) }) { Text("+10") }
            }
        }
    }
}

@Composable
private fun CostCard(state: StockEntryUiState, viewModel: StockEntryViewModel) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("¿A cómo salió cada pieza?", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

            if (!state.showUsd) {
                MoneyField(
                    value = state.costMxn,
                    onValueChange = viewModel::onCostChange,
                    label = "Costo por pieza (pesos)",
                    supportingText = "Es lo que te costó a ti; el cliente no lo ve.",
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { viewModel.onShowUsdChange(true) }) { Text("Lo compré en dólares") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MoneyField(
                        value = state.usd,
                        onValueChange = viewModel::onUsdChange,
                        label = "USD por pieza",
                        modifier = Modifier.weight(1f)
                    )
                    MoneyField(
                        value = state.exchangeRate,
                        onValueChange = viewModel::onExchangeRateChange,
                        label = "Tipo de cambio",
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    if (state.effectiveCostMxn > 0.0) "Equivale a ${formatMoney(state.effectiveCostMxn)} por pieza."
                    else "Escribe los dólares y el tipo de cambio de hoy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RodvarTextSecondary
                )
                TextButton(onClick = { viewModel.onShowUsdChange(false) }) { Text("Mejor capturo pesos") }
            }
        }
    }
}

@Composable
private fun PrintToggleCard(state: StockEntryUiState, onChange: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Imprimir etiquetas al guardar", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    "Una etiqueta QR por pieza, para descontar exacto cuando instales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RodvarTextSecondary
                )
            }
            Switch(checked = state.printLabels, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun ResultCard(
    state: StockEntryUiState,
    onRetryPrint: () -> Unit,
    onAnother: () -> Unit,
    onDone: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = RodvarSecondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = RodvarSecondary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    if (state.isReceive) "Entrada guardada" else "Etiquetas listas",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            state.product?.let {
                Text(listOfNotNull(it.name, state.variantLabel).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            }
            if (state.isReceive) {
                Text(
                    "+${state.quantity} pieza(s) a ${formatMoney(state.effectiveCostMxn)} c/u" +
                        (state.newStock?.let { " · ahora hay $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RodvarTextSecondary
                )
            }
            if (state.units.isNotEmpty()) {
                when (state.printStatus) {
                    PrintStatus.PRINTING -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(state.busyMessage ?: "Imprimiendo…", style = MaterialTheme.typography.bodyMedium)
                    }
                    PrintStatus.DONE -> Text(
                        "${state.units.size} etiqueta(s) impresas en ${state.printerName ?: "la NIIMBOT"}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RodvarSecondary
                    )
                    PrintStatus.FAILED -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MessageCard(
                            text = "La entrada ya quedó guardada, pero no se pudo imprimir: ${state.printError ?: "error desconocido"}",
                            positive = false
                        )
                        Button(onClick = onRetryPrint, shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Filled.Print, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reintentar impresión")
                        }
                    }
                    PrintStatus.IDLE -> Unit
                }
            } else if (state.isReceive) {
                Text("No se generaron etiquetas. Puedes hacerlo después desde el producto.", style = MaterialTheme.typography.bodySmall, color = RodvarTextSecondary)
            }

            Spacer(Modifier.size(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onAnother, enabled = state.printStatus != PrintStatus.PRINTING, modifier = Modifier.weight(1f)) {
                    Text(if (state.isReceive) "Registrar otra" else "Otro lote")
                }
                Button(onClick = onDone, enabled = state.printStatus != PrintStatus.PRINTING, modifier = Modifier.weight(1f)) { Text("Listo") }
            }
        }
    }
}

@Composable
private fun MessageCard(text: String, positive: Boolean) {
    val container = if (positive) RodvarSecondaryContainer else RodvarRedContainer
    val tint = if (positive) RodvarSecondary else MaterialTheme.colorScheme.error
    Card(colors = CardDefaults.cardColors(containerColor = container), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (positive) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber, contentDescription = null, tint = tint)
            Spacer(Modifier.width(10.dp))
            Text(text, color = tint, style = MaterialTheme.typography.bodySmall)
        }
    }
}
