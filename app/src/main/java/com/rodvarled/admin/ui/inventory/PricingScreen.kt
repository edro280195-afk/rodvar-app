package com.rodvarled.admin.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.DropdownSelector

private val RULE_TYPES = listOf("Promotion", "Quantity", "Customer")
private val DISCOUNT_TYPES = listOf("FixedPrice", "Percentage", "FixedAmount")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingScreen(
    navController: NavHostController,
    viewModel: PricingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Promociones y precios", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") } }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.padding(padding).fillMaxSize().wrapContentSize(Alignment.Center))
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.product?.let { product ->
                Text(product.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("El precio de lista se conserva. Cada regla queda guardada como historial para reportes.", style = MaterialTheme.typography.bodySmall)
                if (product.variants.isNotEmpty()) {
                    DropdownSelector(
                        label = "Aplicar a variante",
                        items = listOf<Int?>(null) + product.variants.map { it.id },
                        selected = state.selectedVariantId,
                        labelOf = { id -> id?.let { selectedId -> product.variants.firstOrNull { it.id == selectedId }?.bulbTypeName } ?: "Producto base" },
                        onSelect = viewModel::onVariantChange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            DropdownSelector(
                label = "Tipo de regla",
                items = RULE_TYPES,
                selected = state.ruleType,
                labelOf = { it.toRuleTypeLabel() },
                onSelect = viewModel::onRuleTypeChange,
                modifier = Modifier.fillMaxWidth()
            )
            DropdownSelector(
                label = "Forma de ajuste",
                items = DISCOUNT_TYPES,
                selected = state.discountType,
                labelOf = { it.toDiscountTypeLabel() },
                onSelect = viewModel::onDiscountTypeChange,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(state.name, viewModel::onNameChange, label = { Text("Nombre, por ejemplo: Led Ultra mayo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                state.valueText,
                viewModel::onValueChange,
                label = { Text(if (state.discountType == "FixedPrice") "Precio final" else if (state.discountType == "Percentage") "Porcentaje de descuento" else "Importe a descontar") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.ruleType == "Quantity") {
                OutlinedTextField(state.minimumQuantityText, viewModel::onMinimumQuantityChange, label = { Text("Cantidad mínima") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (state.ruleType == "Customer") {
                OutlinedTextField(state.customerIdText, viewModel::onCustomerIdChange, label = { Text("ID del cliente frecuente") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.isActive, onCheckedChange = viewModel::onActiveChange)
                Text("Regla activa")
            }
            Button(onClick = viewModel::saveRule, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                if (state.isSaving) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(end = 8.dp))
                Text("Guardar regla")
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Text("Reglas existentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            state.rules.forEach { rule ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(rule.name, fontWeight = FontWeight.Bold)
                            Text("${rule.ruleType.toRuleTypeLabel()} · ${rule.discountType.toDiscountTypeLabel()} ${rule.value} · ${if (rule.isActive) "Activa" else "Inactiva"}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { viewModel.deleteRule(rule) }, enabled = !state.isSaving) { Icon(Icons.Filled.DeleteOutline, "Eliminar regla") }
                    }
                }
            }
        }
    }
}

private fun String.toRuleTypeLabel(): String = when (this) {
    "Quantity" -> "Por cantidad"
    "Customer" -> "Cliente frecuente"
    else -> "Promoción"
}

private fun String.toDiscountTypeLabel(): String = when (this) {
    "Percentage" -> "%"
    "FixedAmount" -> "Descuento fijo"
    else -> "Precio final"
}
