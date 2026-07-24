package com.rodvarled.admin.ui.appointments

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.components.IconBadge
import com.rodvarled.admin.ui.components.RodvarDatePickerDialog
import com.rodvarled.admin.ui.components.RodvarListRow
import com.rodvarled.admin.ui.components.RodvarTimePickerDialog
import com.rodvarled.admin.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentFormScreen(
    appointmentId: Int?,
    navController: NavHostController,
    viewModel: AppointmentFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showVehicleSheet by remember { mutableStateOf(false) }
    var showProductSheet by remember { mutableStateOf(false) }

    LaunchedEffect(appointmentId) {
        appointmentId?.let { viewModel.loadForEdit(it) }
    }
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Editar cita" else "Nueva cita") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 10.dp, color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    if (uiState.error != null) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Button(onClick = viewModel::save, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth()) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (uiState.isEditMode) "Guardar cambios" else "Crear cita")
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

            DropdownSelector(
                label = "Origen",
                items = APPOINTMENT_SOURCES,
                selected = uiState.source,
                labelOf = { it },
                onSelect = viewModel::onSourceChange,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )

            SectionHeader("Fecha y hora")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                DateTimeTile(
                    icon = Icons.Filled.CalendarMonth,
                    label = "Fecha",
                    value = uiState.requestedDate.ifBlank { null }?.let { formatAppointmentDate(it) } ?: "Elegir fecha",
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                )
                DateTimeTile(
                    icon = Icons.Filled.Schedule,
                    label = "Hora",
                    value = uiState.requestedTime.ifBlank { null }?.let { formatAppointmentTime(it) } ?: "Elegir hora",
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            if (!uiState.isEditMode) {
                SectionHeader("Vehículo")
                RodvarListRow(
                    onClick = { showVehicleSheet = true },
                    leading = { IconBadge(Icons.Filled.DirectionsCar) },
                    headline = {
                        Text(
                            uiState.selectedTrim?.let { "${uiState.selectedMake?.name} ${uiState.selectedModel?.name} ${uiState.selectedYear?.year} ${it.name}" }
                                ?: "Sin seleccionar (opcional)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )

                SectionHeader("Productos / servicios")
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text(
                        if (uiState.selectedProducts.isEmpty()) "Ninguno agregado" else "${uiState.selectedProducts.size} producto(s) · $${"%.2f".format(uiState.total)} MXN",
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
            }

            SectionHeader("Notas")
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )

            Spacer(Modifier.height(20.dp))
        }
    }

    if (showDatePicker) {
        RodvarDatePickerDialog(
            initialDate = uiState.requestedDate.ifBlank { null },
            onDismiss = { showDatePicker = false },
            onConfirm = viewModel::onDateChange
        )
    }
    if (showTimePicker) {
        RodvarTimePickerDialog(
            initialTime = uiState.requestedTime.ifBlank { null },
            onDismiss = { showTimePicker = false },
            onConfirm = viewModel::onTimeChange
        )
    }

    if (showVehicleSheet) {
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

    if (showProductSheet) {
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

/** Tile de fecha/hora: ícono + etiqueta arriba, valor grande abajo — reemplaza el selector de texto plano. */
@Composable
private fun DateTimeTile(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
            }
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
