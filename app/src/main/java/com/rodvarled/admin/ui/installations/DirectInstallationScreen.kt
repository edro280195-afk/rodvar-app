package com.rodvarled.admin.ui.installations

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.data.remote.dto.DirectInstallationResponse
import com.rodvarled.admin.ui.appointments.PAYMENT_METHODS
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.components.IconBadge
import com.rodvarled.admin.ui.components.PhotoCaptureField
import com.rodvarled.admin.ui.components.RodvarListRow
import com.rodvarled.admin.ui.components.SectionHeader
import com.rodvarled.admin.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectInstallationScreen(
    navController: NavHostController,
    viewModel: DirectInstallationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showVehicleSheet by remember { mutableStateOf(false) }
    var showProductSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instalación directa") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.completed == null) {
                Surface(shadowElevation = 10.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        if (uiState.error != null) {
                            Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Button(onClick = viewModel::submit, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth()) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Registrar instalación")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        val completed = uiState.completed
        if (completed != null) {
            SuccessContent(
                completed = completed,
                customerName = uiState.customerName,
                customerPhone = uiState.customerPhone,
                onSignWarranty = {
                    navController.navigate(Screen.WarrantyDetail.of(completed.warrantyId)) {
                        popUpTo(Screen.Dashboard.route)
                    }
                },
                onSharePortal = {
                    val cleanPhone = uiState.customerPhone.filter { it.isDigit() }
                    val message = "¡Hola ${uiState.customerName.trim()}! Tu instalación ${completed.folio} quedó registrada. En tu portal de Rodvar LED puedes consultar y firmar tu garantía, ver tus puntos y tu historial: ${completed.portalUrl}"
                    val url = "https://wa.me/52$cleanPhone?text=${Uri.encode(message)}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                onDone = { navController.popBackStack() },
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SectionHeader("Cliente", modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = uiState.customerPhone,
                    onValueChange = viewModel::onCustomerPhoneChange,
                    label = { Text("Teléfono") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )

                if (uiState.customerSuggestions.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        uiState.customerSuggestions.take(4).forEach { suggestion ->
                            RodvarListRow(
                                onClick = { viewModel.pickCustomerSuggestion(suggestion) },
                                leading = { IconBadge(Icons.Filled.Person, modifier = Modifier.size(36.dp)) },
                                headline = { Text(suggestion.name, style = MaterialTheme.typography.bodyMedium) },
                                supporting = { Text(suggestion.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                showChevron = false
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.customerName,
                    onValueChange = viewModel::onCustomerNameChange,
                    label = { Text("Nombre completo") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                SectionHeader("Vehículo (opcional)")
                RodvarListRow(
                    onClick = { showVehicleSheet = true },
                    leading = { IconBadge(Icons.Filled.DirectionsCar) },
                    headline = {
                        Text(
                            uiState.selectedTrim?.let { "${uiState.selectedMake?.name} ${uiState.selectedModel?.name} ${uiState.selectedYear?.year} ${it.name}" }
                                ?: "Sin seleccionar",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = uiState.vehiclePlate,
                        onValueChange = viewModel::onPlateChange,
                        label = { Text("Placas") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.vehicleColor,
                        onValueChange = viewModel::onColorChange,
                        label = { Text("Color") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    )
                }

                SectionHeader("Productos instalados")
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text(
                        if (uiState.selectedProducts.isEmpty()) "Ninguno (solo mano de obra)" else "${uiState.selectedProducts.size} producto(s) · $${"%.2f".format(uiState.suggestedTotal)} MXN",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { showProductSheet = true }) { Text("+ Agregar") }
                }

                uiState.selectedProducts.forEach { selected ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(selected.product.name, style = MaterialTheme.typography.bodyMedium)
                            Text("$${"%.2f".format(selected.product.price)} c/u", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.setProductQuantity(selected.product.id, selected.quantity - 1) }, enabled = selected.quantity > 1) {
                            Icon(Icons.Filled.Remove, contentDescription = "Restar")
                        }
                        Text(selected.quantity.toString(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 4.dp))
                        IconButton(onClick = { viewModel.setProductQuantity(selected.product.id, selected.quantity + 1) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Sumar")
                        }
                        IconButton(onClick = { viewModel.toggleProduct(selected.product) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar", modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }

                SectionHeader("Cobro")
                DropdownSelector(
                    label = "Método de pago",
                    items = PAYMENT_METHODS,
                    selected = uiState.paymentMethod,
                    labelOf = { it },
                    onSelect = viewModel::onPaymentMethodChange,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
                OutlinedTextField(
                    value = uiState.totalText,
                    onValueChange = viewModel::onTotalChange,
                    label = { Text("Total cobrado (MXN)") },
                    supportingText = { Text("Se sugiere la suma de productos; puedes ajustarlo a lo negociado.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                SectionHeader("Fotos de la instalación (opcional)")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    PhotoCaptureField(
                        label = "Antes",
                        base64Value = uiState.beforePhotoBase64,
                        onValueChange = viewModel::onBeforePhotoCaptured,
                        modifier = Modifier.weight(1f)
                    )
                    PhotoCaptureField(
                        label = "Después",
                        base64Value = uiState.afterPhotoBase64,
                        onValueChange = viewModel::onAfterPhotoCaptured,
                        modifier = Modifier.weight(1f)
                    )
                }

                SectionHeader("Notas del técnico")
                OutlinedTextField(
                    value = uiState.technicianNotes,
                    onValueChange = viewModel::onNotesChange,
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )

                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showVehicleSheet && uiState.completed == null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showVehicleSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text("Vehículo", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))
                DropdownSelector("Marca", uiState.makes, uiState.selectedMake, { it.name }, onSelect = viewModel::onMakeSelected, modifier = Modifier.fillMaxWidth())
                DropdownSelector("Modelo", uiState.models, uiState.selectedModel, { it.name }, enabled = uiState.selectedMake != null, onSelect = viewModel::onModelSelected, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                DropdownSelector("Año", uiState.years, uiState.selectedYear, { it.year.toString() }, enabled = uiState.selectedModel != null, onSelect = viewModel::onYearSelected, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                DropdownSelector("Versión", uiState.trims, uiState.selectedTrim, { it.name }, enabled = uiState.selectedYear != null, onSelect = viewModel::onTrimSelected, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                Button(onClick = { showVehicleSheet = false }, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("Listo") }
            }
        }
    }

    if (showProductSheet && uiState.completed == null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showProductSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Agregar productos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                LazyColumn(modifier = Modifier.height(420.dp)) {
                    items(uiState.availableProducts, key = { it.id }) { product ->
                        val checked = uiState.selectedProducts.any { it.product.id == product.id }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleProduct(product) }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Checkbox(checked = checked, onCheckedChange = { viewModel.toggleProduct(product) })
                            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                Text(product.name, style = MaterialTheme.typography.bodyMedium)
                                Text("$${"%.2f".format(product.price)} · stock ${product.stock}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Button(onClick = { showProductSheet = false }, modifier = Modifier.fillMaxWidth().padding(20.dp)) { Text("Listo") }
            }
        }
    }
}

/** Pantalla de éxito: la instalación ya quedó registrada con su garantía pendiente de firma. */
@Composable
private fun SuccessContent(
    completed: DirectInstallationResponse,
    customerName: String,
    customerPhone: String,
    onSignWarranty: () -> Unit,
    onSharePortal: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().padding(28.dp)
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(72.dp)
        )
        Text("Instalación registrada", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        Text(
            "${completed.folio} · $${"%.2f".format(completed.totalAmount)} MXN",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (completed.pointsEarned > 0) {
            Text(
                "+${completed.pointsEarned} puntos para el cliente",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Button(onClick = onSignWarranty, modifier = Modifier.fillMaxWidth().padding(top = 28.dp)) {
            Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Firmar garantía ahora", modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(onClick = onSharePortal, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Enviar portal por WhatsApp", modifier = Modifier.padding(start = 8.dp))
        }
        TextButton(onClick = onDone, modifier = Modifier.padding(top = 10.dp)) {
            Text("Terminar")
        }
    }
}
