package com.rodvarled.admin.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rodvarled.admin.ui.components.ConfirmDialog
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.components.ErrorState
import com.rodvarled.admin.ui.components.FullScreenLoading
import com.rodvarled.admin.ui.components.SectionHeader
import com.rodvarled.admin.ui.navigation.Screen
import com.rodvarled.admin.ui.theme.RodvarAmber
import com.rodvarled.admin.ui.theme.RodvarEmerald
import com.rodvarled.admin.ui.theme.RodvarOutline
import com.rodvarled.admin.ui.theme.RodvarPrimary
import com.rodvarled.admin.ui.theme.RodvarPrimaryContainer
import com.rodvarled.admin.ui.theme.RodvarRed
import com.rodvarled.admin.ui.theme.RodvarSurface
import com.rodvarled.admin.ui.theme.RodvarTextPrimary
import com.rodvarled.admin.ui.theme.RodvarTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Int,
    navController: NavHostController,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentEntry = navController.currentBackStackEntry
    val productUpdatedState = currentEntry?.savedStateHandle
        ?.getStateFlow("productUpdated", false)
        ?.collectAsState()
    val productUpdated = productUpdatedState?.value ?: false
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAdjustDialog by remember { mutableStateOf<Int?>(null) } // bulbTypeId or -1 for base
    var showDeactivateConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) }; viewModel.consumeSnackbar() }
    }
    LaunchedEffect(productUpdated) {
        if (productUpdated) {
            currentEntry?.savedStateHandle?.set("productUpdated", false)
            viewModel.load(showLoading = false)
        }
    }
    LaunchedEffect(uiState.deactivated) { if (uiState.deactivated) navController.popBackStack() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Producto", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
                actions = {
                    uiState.product?.let {
                        IconButton(onClick = { navController.navigate(Screen.Pricing.of(it.id)) }) {
                            Icon(Icons.Filled.LocalOffer, contentDescription = "Promociones", tint = RodvarPrimary)
                        }
                        IconButton(onClick = { navController.navigate(Screen.ReceiveStock.create(it.id)) }) {
                            Icon(Icons.Filled.Inventory2, contentDescription = "Recibir mercancía", tint = RodvarPrimary)
                        }
                        IconButton(onClick = { navController.navigate(Screen.InventoryLabels.of(it.id)) }) {
                            Icon(Icons.Filled.Print, contentDescription = "Imprimir etiquetas QR", tint = RodvarPrimary)
                        }
                        IconButton(onClick = { navController.navigate(Screen.ProductForm.edit(it.id)) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = RodvarPrimary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> FullScreenLoading(Modifier.padding(padding).fillMaxSize())
            uiState.error != null -> ErrorState(uiState.error!!, onRetry = viewModel::load, modifier = Modifier.padding(padding).fillMaxSize())
            uiState.product != null -> {
                val product = uiState.product!!
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tarjeta Principal de Información
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.extraLarge)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!product.mainImageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = product.mainImageUrl,
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp))
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = RodvarPrimaryContainer,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Inventory2, contentDescription = null, tint = RodvarPrimary, modifier = Modifier.size(32.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = RodvarTextPrimary)
                                Text(product.categoryName, style = MaterialTheme.typography.bodySmall, color = RodvarTextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$${"%.2f".format(product.price)} MXN", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = RodvarPrimary)
                                Text("Garantía de ${product.warrantyMonths} meses", style = MaterialTheme.typography.labelSmall, color = RodvarTextSecondary)
                            }
                        }
                    }

                    // Tarjeta de Control de Existencias
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            SectionHeader("Control de Existencias")
                            Spacer(modifier = Modifier.height(8.dp))

                            if (product.variants.isNotEmpty()) {
                                product.variants.forEach { variant ->
                                    StockRow(
                                        label = variant.bulbTypeName,
                                        stock = variant.stock,
                                        isLow = variant.lowStockThreshold != null && variant.stock <= variant.lowStockThreshold,
                                        enabled = !uiState.actionInProgress,
                                        onMinus = { viewModel.quickAdjust(variant.bulbTypeId, -1) },
                                        onPlus = { viewModel.quickAdjust(variant.bulbTypeId, 1) },
                                        onLongAdjust = { showAdjustDialog = variant.bulbTypeId }
                                    )
                                }
                            } else {
                                StockRow(
                                    label = "Stock Disponible",
                                    stock = product.stock,
                                    isLow = product.lowStockThreshold != null && product.stock <= product.lowStockThreshold,
                                    enabled = !uiState.actionInProgress,
                                    onMinus = { viewModel.quickAdjust(null, -1) },
                                    onPlus = { viewModel.quickAdjust(null, 1) },
                                    onLongAdjust = { showAdjustDialog = -1 }
                                )
                            }
                        }
                    }

                    // Historial de Movimientos
                    if (uiState.movements.isNotEmpty()) {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                SectionHeader("Historial de Movimientos de Stock")
                                Spacer(modifier = Modifier.height(10.dp))

                                uiState.movements.take(15).forEach { movement ->
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(movement.reason + (movement.bulbTypeName?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            if (!movement.note.isNullOrBlank()) Text(movement.note, style = MaterialTheme.typography.bodySmall, color = RodvarTextSecondary)
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = if (movement.delta > 0) RodvarEmerald.copy(alpha = 0.12f) else RodvarRed.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                (if (movement.delta > 0) "+" else "") + movement.delta,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (movement.delta > 0) RodvarEmerald else RodvarRed,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = RodvarOutline.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showDeactivateConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RodvarRed),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Desactivar Producto del Catálogo")
                    }
                }
            }
        }
    }

    if (showAdjustDialog != null) {
        val bulbTypeId = showAdjustDialog!!.takeIf { it != -1 }
        StockAdjustDialog(
            onDismiss = { showAdjustDialog = null },
            onConfirm = { delta, reason, note ->
                viewModel.adjustWithReason(
                    bulbTypeId = bulbTypeId,
                    delta = delta,
                    reason = reason,
                    note = note
                )
                showAdjustDialog = null
            }
        )
    }

    if (showDeactivateConfirm) {
        ConfirmDialog(
            title = "Desactivar producto",
            message = "El producto dejará de mostrarse en el catálogo. ¿Continuar?",
            confirmLabel = "Desactivar",
            isDanger = true,
            onConfirm = { showDeactivateConfirm = false; viewModel.deactivate() },
            onDismiss = { showDeactivateConfirm = false }
        )
    }
}

@Composable
private fun StockRow(
    label: String,
    stock: Int,
    isLow: Boolean,
    enabled: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onLongAdjust: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            if (isLow) Text("⚠️ Stock bajo", style = MaterialTheme.typography.labelSmall, color = RodvarAmber)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = onMinus, enabled = enabled && stock > 0) {
                Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Restar", tint = if (stock > 0) RodvarRed else RodvarOutline)
            }
            Text(stock.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = RodvarTextPrimary, modifier = Modifier.padding(horizontal = 4.dp))
            IconButton(onClick = onPlus, enabled = enabled) {
                Icon(Icons.Filled.AddCircleOutline, contentDescription = "Sumar", tint = RodvarEmerald)
            }
            TextButton(onClick = onLongAdjust) { Text("Ajustar", fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockAdjustDialog(
    onDismiss: () -> Unit,
    onConfirm: (delta: Int, reason: String, note: String?) -> Unit
) {
    var quantity by remember { mutableStateOf("") }
    var isAddition by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf(STOCK_ADJUST_REASONS.first()) }
    var note by remember { mutableStateOf("") }
    val quantityValue = quantity.toIntOrNull() ?: 0
    val canConfirm = quantityValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Corregir inventario", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Para mercancía nueva que llegó usa «Recibir mercancía»: ahí se guarda lo que costó y se imprimen las etiquetas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    FilterChip(selected = !isAddition, onClick = { isAddition = false }, label = { Text("Salida (−)") })
                    FilterChip(selected = isAddition, onClick = { isAddition = true }, label = { Text("Entrada (+)") })
                }
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                    label = { Text("Cantidad de piezas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                DropdownSelector(
                    label = "Motivo",
                    items = STOCK_ADJUST_REASONS,
                    selected = reason,
                    labelOf = { it },
                    onSelect = { reason = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (opcional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canConfirm) onConfirm(if (isAddition) quantityValue else -quantityValue, reason, note.ifBlank { null })
                },
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RodvarPrimary)
            ) { Text("Aplicar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
