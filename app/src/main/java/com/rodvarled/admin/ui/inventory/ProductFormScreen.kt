package com.rodvarled.admin.ui.inventory

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.material3.TextButton
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.components.PhotoCaptureField
import com.rodvarled.admin.ui.components.SectionHeader
import com.rodvarled.admin.ui.theme.RodvarOutline
import com.rodvarled.admin.ui.theme.RodvarPrimary
import com.rodvarled.admin.ui.theme.RodvarRed
import com.rodvarled.admin.ui.theme.RodvarSurface
import com.rodvarled.admin.ui.theme.RodvarTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: Int?,
    navController: NavHostController,
    viewModel: ProductFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDetails by remember { mutableStateOf(false) }

    LaunchedEffect(productId) { productId?.let { viewModel.loadForEdit(it) } }
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            if (productId != null) {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("productUpdated", true)
            }
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Editar Producto" else "Nuevo Producto LED", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lo básico: con esto alcanza para dar de alta un producto. Lo demás está en "Más detalles".
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.extraLarge)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Lo básico")

                    OutlinedTextField(
                        value = uiState.name, onValueChange = viewModel::onNameChange, label = { Text("Nombre del producto") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )

                    DropdownSelector(
                        label = "Categoría",
                        items = uiState.categories,
                        selected = uiState.selectedCategory,
                        labelOf = { it.name },
                        onSelect = viewModel::onCategorySelected,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = uiState.price, onValueChange = viewModel::onPriceChange, label = { Text("Precio de venta") },
                            prefix = { Text("$") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.cost, onValueChange = viewModel::onCostChange, label = { Text("Costo por foco") },
                            prefix = { Text("$") },
                            supportingText = { Text("Lo que te cuesta; no se muestra") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            TextButton(onClick = { showDetails = !showDetails }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showDetails) "Menos detalles ▴" else "Más detalles: foto, garantía, especificaciones, variantes ▾")
            }

            AnimatedVisibility(visible = showDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Tarjeta 1: Identidad del Producto y Fotografía
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.extraLarge)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Identidad y Fotografía")

                    PhotoCaptureField(label = "Foto principal del producto", base64Value = null, onValueChange = { it?.let(viewModel::uploadImage) })
                    if (uiState.mainImageUrl != null && !uiState.mainImageUrl!!.startsWith("data:")) {
                        AsyncImage(
                            model = uiState.mainImageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(MaterialTheme.shapes.medium)
                        )
                    }

                    OutlinedTextField(value = uiState.slug, onValueChange = viewModel::onSlugChange, label = { Text("Slug (Identificador URL)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    OutlinedTextField(value = uiState.description, onValueChange = viewModel::onDescriptionChange, label = { Text("Descripción comercial") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                }
            }

            // Tarjeta 2: Precios y Garantía
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Garantía")

                    OutlinedTextField(
                        value = uiState.warrantyMonths, onValueChange = viewModel::onWarrantyMonthsChange, label = { Text("Garantía (meses)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Tarjeta 3: Especificaciones Técnicas LED
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Especificaciones Técnicas LED")

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = uiState.lumens, onValueChange = viewModel::onLumensChange, label = { Text("Lúmenes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = uiState.colorTemperature, onValueChange = viewModel::onColorTemperatureChange, label = { Text("Color (K)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = uiState.wattage, onValueChange = viewModel::onWattageChange, label = { Text("Watts") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(value = uiState.voltageRange, onValueChange = viewModel::onVoltageRangeChange, label = { Text("Rango de Voltaje") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = uiState.coolingSystem, onValueChange = viewModel::onCoolingSystemChange, label = { Text("Sistema de Enfriamiento") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }

            // Tarjeta 4: Inventario y Variantes de Foco
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Inventario y Variantes de Socket")

                    if (uiState.variants.isEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = uiState.stock, onValueChange = viewModel::onStockChange, label = { Text("Stock Actual") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = uiState.lowStockThreshold, onValueChange = viewModel::onLowStockThresholdChange, label = { Text("Alerta Stock ≤") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                    }

                    Text("Variantes por Socket / Entrada de Foco", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    DropdownSelector(
                        label = "Agregar variante de foco",
                        items = uiState.bulbTypes.filter { bt -> uiState.variants.none { it.bulbTypeId == bt.id } },
                        selected = null,
                        labelOf = { it.name },
                        onSelect = viewModel::addVariant,
                        modifier = Modifier.fillMaxWidth()
                    )

                    uiState.variants.forEach { variant ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(variant.bulbTypeName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.removeVariant(variant.bulbTypeId) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = RodvarRed)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = variant.stock,
                                    onValueChange = { viewModel.updateVariantStock(variant.bulbTypeId, it) },
                                    label = { Text("Stock") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = variant.costOverride,
                                    onValueChange = { viewModel.updateVariantCost(variant.bulbTypeId, it) },
                                    label = { Text("Costo") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = variant.lowStockThreshold,
                                    onValueChange = { viewModel.updateVariantThreshold(variant.bulbTypeId, it) },
                                    label = { Text("Mín.") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Tarjeta 5: Estado y Visibilidad
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Producto Activo", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = RodvarTextPrimary)
                        Text("Visible para agendar e instalar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = uiState.isActive, onCheckedChange = viewModel::onActiveChange)
                }
            }

                }
            }

            if (uiState.error != null) {
                Text(uiState.error!!, color = RodvarRed, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = viewModel::save,
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = RodvarPrimary),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(if (uiState.isEditMode) "Guardar Cambios" else "Crear Producto", fontWeight = FontWeight.Bold)
            }
        }
    }
}
