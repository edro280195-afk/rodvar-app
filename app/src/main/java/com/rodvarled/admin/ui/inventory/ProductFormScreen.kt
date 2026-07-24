package com.rodvarled.admin.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.components.PhotoCaptureField
import com.rodvarled.admin.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: Int?,
    navController: NavHostController,
    viewModel: ProductFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(productId) { productId?.let { viewModel.loadForEdit(it) } }
    LaunchedEffect(uiState.saved) { if (uiState.saved) navController.popBackStack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Editar producto" else "Nuevo producto") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader("Identidad")
            PhotoCaptureField(label = "Foto principal", base64Value = null, onValueChange = { it?.let(viewModel::uploadImage) })
            if (uiState.mainImageUrl != null && !uiState.mainImageUrl!!.startsWith("data:")) {
                coil3.compose.AsyncImage(model = uiState.mainImageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(value = uiState.name, onValueChange = viewModel::onNameChange, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = uiState.slug, onValueChange = viewModel::onSlugChange, label = { Text("Slug (URL)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            DropdownSelector(
                label = "Categoría",
                items = uiState.categories,
                selected = uiState.selectedCategory,
                labelOf = { it.name },
                onSelect = viewModel::onCategorySelected,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(value = uiState.description, onValueChange = viewModel::onDescriptionChange, label = { Text("Descripción") }, minLines = 2, modifier = Modifier.fillMaxWidth())

            SectionHeader("Especificaciones técnicas")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.price, onValueChange = viewModel::onPriceChange, label = { Text("Precio") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.warrantyMonths, onValueChange = viewModel::onWarrantyMonthsChange, label = { Text("Garantía (meses)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = uiState.lumens, onValueChange = viewModel::onLumensChange, label = { Text("Lúmenes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = uiState.colorTemperature, onValueChange = viewModel::onColorTemperatureChange, label = { Text("Temp. color (K)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = uiState.wattage, onValueChange = viewModel::onWattageChange, label = { Text("Watts") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }

            OutlinedTextField(value = uiState.voltageRange, onValueChange = viewModel::onVoltageRangeChange, label = { Text("Rango de voltaje") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = uiState.coolingSystem, onValueChange = viewModel::onCoolingSystemChange, label = { Text("Sistema de enfriamiento") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            SectionHeader("Inventario")

            if (uiState.variants.isEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = uiState.stock, onValueChange = viewModel::onStockChange, label = { Text("Stock") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = uiState.lowStockThreshold, onValueChange = viewModel::onLowStockThresholdChange, label = { Text("Alertar si stock ≤") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }

            Text("Variantes por tipo de foco (opcional)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DropdownSelector(
                label = "Agregar variante",
                items = uiState.bulbTypes.filter { bt -> uiState.variants.none { it.bulbTypeId == bt.id } },
                selected = null,
                labelOf = { it.name },
                onSelect = viewModel::addVariant,
                modifier = Modifier.fillMaxWidth()
            )

            uiState.variants.forEach { variant ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(variant.bulbTypeName, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = variant.stock,
                        onValueChange = { viewModel.updateVariantStock(variant.bulbTypeId, it) },
                        label = { Text("Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )
                    OutlinedTextField(
                        value = variant.lowStockThreshold,
                        onValueChange = { viewModel.updateVariantThreshold(variant.bulbTypeId, it) },
                        label = { Text("Mín.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.removeVariant(variant.bulbTypeId) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            SectionHeader("Estado")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Producto activo", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = uiState.isActive, onCheckedChange = viewModel::onActiveChange)
            }

            if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(onClick = viewModel::save, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth()) {
                if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 2.dp)
                else Text(if (uiState.isEditMode) "Guardar cambios" else "Crear producto")
            }
        }
    }
}
