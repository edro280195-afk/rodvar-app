package com.rodvarled.admin.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Int,
    navController: NavHostController,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAdjustDialog by remember { mutableStateOf<Int?>(null) } // bulbTypeId or -1 for base
    var showDeactivateConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) }; viewModel.consumeSnackbar() }
    }
    LaunchedEffect(uiState.deactivated) { if (uiState.deactivated) navController.popBackStack() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } },
                actions = {
                    uiState.product?.let {
                        IconButton(onClick = { navController.navigate(Screen.ProductForm.edit(it.id)) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
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
                    modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!product.mainImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = product.mainImageUrl,
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))
                            )
                        }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(product.name, style = MaterialTheme.typography.headlineSmall)
                            Text(product.categoryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${"%.2f".format(product.price)} MXN · Garantía ${product.warrantyMonths} meses", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    HorizontalDivider()
                    SectionHeader("Existencias")

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
                            label = "Stock general",
                            stock = product.stock,
                            isLow = product.lowStockThreshold != null && product.stock <= product.lowStockThreshold,
                            enabled = !uiState.actionInProgress,
                            onMinus = { viewModel.quickAdjust(null, -1) },
                            onPlus = { viewModel.quickAdjust(null, 1) },
                            onLongAdjust = { showAdjustDialog = -1 }
                        )
                    }

                    HorizontalDivider()

                    if (uiState.movements.isNotEmpty()) {
                        SectionHeader("Movimientos recientes")
                        uiState.movements.take(15).forEach { movement ->
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column {
                                    Text(movement.reason + (movement.bulbTypeName?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodyMedium)
                                    if (!movement.note.isNullOrBlank()) Text(movement.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text((if (movement.delta > 0) "+" else "") + movement.delta, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        HorizontalDivider()
                    }

                    OutlinedButton(
                        onClick = { showDeactivateConfirm = true },
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Desactivar producto")
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
                viewModel.adjustWithReason(bulbTypeId, delta, reason, note)
                showAdjustDialog = null
            }
        )
    }

    if (showDeactivateConfirm) {
        ConfirmDialog(
            title = "Desactivar producto",
            message = "El producto dejará de mostrarse en el catálogo público. ¿Continuar?",
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (isLow) Text("Stock bajo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMinus, enabled = enabled && stock > 0) { Icon(Icons.Filled.Remove, contentDescription = "Restar") }
            Text(stock.toString(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 4.dp))
            IconButton(onClick = onPlus, enabled = enabled) { Icon(Icons.Filled.Add, contentDescription = "Sumar") }
            TextButton(onClick = onLongAdjust) { Text("Ajustar") }
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
    var isAddition by remember { mutableStateOf(true) }
    var reason by remember { mutableStateOf(STOCK_ADJUST_REASONS.first()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar inventario") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilterChip(selected = isAddition, onClick = { isAddition = true }, label = { Text("Entrada (+)") })
                    androidx.compose.material3.FilterChip(selected = !isAddition, onClick = { isAddition = false }, label = { Text("Salida (-)") })
                }
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                    label = { Text("Cantidad") },
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
            TextButton(onClick = {
                val qty = quantity.toIntOrNull() ?: 0
                if (qty > 0) onConfirm(if (isAddition) qty else -qty, reason, note.ifBlank { null })
                else onDismiss()
            }) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
