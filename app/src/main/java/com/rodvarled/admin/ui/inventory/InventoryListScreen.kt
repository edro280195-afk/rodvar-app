package com.rodvarled.admin.ui.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rodvarled.admin.data.remote.dto.BulbTypeVariant
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.components.EmptyState
import com.rodvarled.admin.ui.components.SearchField
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
import com.rodvarled.admin.ui.theme.staggeredEnter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(navController: NavHostController, viewModel: InventoryListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val pullState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.consumeSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Inventario de Focos LED", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.ProductForm.create()) },
                containerColor = RodvarPrimary,
                contentColor = Color.White,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo producto")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Buscador Instantáneo
            SearchField(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Buscar por nombre, entrada H11, 9005, modelo...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Selector de Categorías / Filtros
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategoryId == null && !uiState.onlyLowStock,
                        onClick = { viewModel.onCategorySelected(null) },
                        label = { Text("Todos (${uiState.totalProductsCount})") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RodvarPrimaryContainer, selectedLabelColor = RodvarPrimary)
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.onlyLowStock,
                        onClick = { viewModel.toggleLowStockOnly() },
                        label = { Text("Stock bajo (${uiState.lowStockCount})") },
                        leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = RodvarAmber) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RodvarAmber.copy(alpha = 0.15f), selectedLabelColor = RodvarAmber)
                    )
                }
                itemsIndexed(uiState.categories, key = { _, category -> category.id }) { _, category ->
                    FilterChip(
                        selected = uiState.selectedCategoryId == category.id,
                        onClick = { viewModel.onCategorySelected(category.id) },
                        label = { Text(category.name) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RodvarPrimaryContainer, selectedLabelColor = RodvarPrimary)
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = viewModel::refresh,
                state = pullState,
                modifier = Modifier.fillMaxSize().padding(top = 4.dp)
            ) {
                if (!uiState.isLoading && uiState.filtered.isEmpty()) {
                    EmptyState(
                        title = "Sin productos en inventario",
                        subtitle = "Presiona el botón + para registrar un nuevo producto o kit de iluminación.",
                        icon = Icons.Filled.Inventory2,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(uiState.filtered, key = { _, item -> item.id }) { index, product ->
                            InventoryProductCard(
                                product = product,
                                onClick = { navController.navigate(Screen.ProductDetail.of(product.id)) },
                                onIncrementStock = { viewModel.adjustStock(product, +1) },
                                onDecrementStock = { viewModel.adjustStock(product, -1) },
                                canDecrementStock = product.variants.isEmpty(),
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }

    val purchaseProduct = uiState.purchaseProduct
    if (purchaseProduct != null) {
        var quantity by remember(purchaseProduct.id) { mutableStateOf("1") }
        var unitCostUsd by remember(purchaseProduct.id) { mutableStateOf("") }
        var exchangeRate by remember(purchaseProduct.id) { mutableStateOf("") }
        var purchaseDate by remember(purchaseProduct.id) { mutableStateOf(java.time.LocalDate.now().toString()) }
        var receivedDate by remember(purchaseProduct.id) { mutableStateOf(java.time.LocalDate.now().toString()) }
        var selectedVariant by remember(purchaseProduct.id) { mutableStateOf<BulbTypeVariant?>(null) }
        val parsedUsd = unitCostUsd.toDoubleOrNull() ?: 0.0
        val parsedRate = exchangeRate.toDoubleOrNull() ?: 0.0
        val canSave = (quantity.toIntOrNull() ?: 0) > 0
            && parsedUsd > 0
            && parsedRate > 0
            && (purchaseProduct.variants.isEmpty() || selectedVariant != null)

        AlertDialog(
            onDismissRequest = viewModel::dismissPurchaseDialog,
            title = { Text("Registrar compra") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${purchaseProduct.name}. Se guardará el costo histórico de esta compra, aunque llegue semanas después.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (purchaseProduct.variants.isNotEmpty()) {
                        DropdownSelector(
                            label = "Variante / socket",
                            items = purchaseProduct.variants,
                            selected = selectedVariant,
                            labelOf = { it.bulbTypeName },
                            onSelect = { selectedVariant = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it.filter(Char::isDigit).take(5) },
                            label = { Text("Piezas") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unitCostUsd,
                            onValueChange = { unitCostUsd = it },
                            label = { Text("USD por foco") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = exchangeRate,
                        onValueChange = { exchangeRate = it },
                        label = { Text("Tipo de cambio MXN/USD") },
                        supportingText = { Text(if (parsedUsd > 0 && parsedRate > 0) "Costo convertido: $${"%.2f".format(parsedUsd * parsedRate)} MXN por foco" else "Ejemplo: 17.25") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = purchaseDate, onValueChange = { purchaseDate = it }, label = { Text("Fecha compra") }, supportingText = { Text("AAAA-MM-DD") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = receivedDate, onValueChange = { receivedDate = it }, label = { Text("Fecha recepción") }, supportingText = { Text("AAAA-MM-DD") }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.registerPurchase(
                            product = purchaseProduct,
                            bulbTypeId = selectedVariant?.bulbTypeId,
                            quantity = quantity.toIntOrNull() ?: 0,
                            unitCostUsd = parsedUsd,
                            exchangeRate = parsedRate,
                            purchaseDate = purchaseDate,
                            receivedDate = receivedDate
                        )
                    },
                    enabled = canSave
                ) { Text("Guardar compra") }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissPurchaseDialog) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun InventoryProductCard(
    product: ProductDetail,
    onClick: () -> Unit,
    onIncrementStock: () -> Unit,
    onDecrementStock: () -> Unit,
    canDecrementStock: Boolean,
    modifier: Modifier = Modifier
) {
    val stock = product.effectiveStock()
    val isLow = product.isLowStock()

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen del Producto o Icono LED
            if (!product.mainImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = product.mainImageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp))
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RodvarPrimaryContainer,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Inventory2,
                            contentDescription = null,
                            tint = RodvarPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Información del Producto
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = RodvarTextPrimary,
                    maxLines = 1
                )

                Text(
                    product.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = RodvarTextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "$${"%.2f".format(product.price)} MXN",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = RodvarPrimary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Control Rápido de Stock Directo Inline (- / Cantidad / +)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when {
                        stock <= 0 -> RodvarRed.copy(alpha = 0.12f)
                        isLow -> RodvarAmber.copy(alpha = 0.14f)
                        else -> RodvarEmerald.copy(alpha = 0.12f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLow) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = RodvarAmber, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (stock <= 0) "Agotado" else "$stock en stock",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                stock <= 0 -> RodvarRed
                                isLow -> RodvarAmber
                                else -> RodvarEmerald
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stepper de Stock (+ / -) Rápido y Fluido
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onDecrementStock,
                        enabled = stock > 0 && canDecrementStock,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Filled.RemoveCircleOutline,
                            contentDescription = "Restar stock",
                            tint = if (stock > 0 && canDecrementStock) RodvarRed else RodvarOutline,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        stock.toString(),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = RodvarTextPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = onIncrementStock,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Filled.AddCircleOutline,
                            contentDescription = "Sumar stock",
                            tint = RodvarEmerald,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
