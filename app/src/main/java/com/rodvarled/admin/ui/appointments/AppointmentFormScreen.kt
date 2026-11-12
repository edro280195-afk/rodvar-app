package com.rodvarled.admin.ui.appointments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import com.rodvarled.admin.ui.components.PhoneInputField
import com.rodvarled.admin.ui.components.RodvarDatePickerDialog
import com.rodvarled.admin.ui.components.RodvarListRow
import com.rodvarled.admin.ui.components.RodvarTimePickerDialog
import com.rodvarled.admin.data.remote.dto.SERVICE_TYPE_INSTALLATION
import com.rodvarled.admin.data.remote.dto.SERVICE_TYPE_REVISION
import com.rodvarled.admin.ui.components.MoneyField
import com.rodvarled.admin.ui.components.ProductPickerSheet
import com.rodvarled.admin.ui.components.VehicleSearchField
import com.rodvarled.admin.ui.components.SectionHeader
import com.rodvarled.admin.ui.components.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentFormScreen(
    appointmentId: Int?,
    navController: NavHostController,
    viewModel: AppointmentFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val vehicleSearch by viewModel.vehicleSearcher.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
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
            SectionHeader("Tipo de servicio", modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = !uiState.isRevision,
                    onClick = { viewModel.onServiceTypeChange(SERVICE_TYPE_INSTALLATION) },
                    label = { Text("Instalación") }
                )
                FilterChip(
                    selected = uiState.isRevision,
                    onClick = { viewModel.onServiceTypeChange(SERVICE_TYPE_REVISION) },
                    label = { Text("Revisión a domicilio · ${formatMoney(uiState.homeVisitFee)}") }
                )
            }
            if (uiState.isRevision) {
                MoneyField(
                    value = uiState.visitFee,
                    onValueChange = viewModel::onVisitFeeChange,
                    label = "Costo de la revisión",
                    supportingText = "Se le avisa al cliente en el itinerario. Pon 0 si no se cobrará.",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            SectionHeader("Cliente")
            PhoneInputField(
                phone = uiState.customerPhone,
                onPhoneChange = viewModel::onCustomerPhoneChange,
                label = "Teléfono",
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )

            OutlinedTextField(
                value = uiState.customerName,
                onValueChange = viewModel::onCustomerNameChange,
                label = { Text("Nombre completo") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            if (uiState.customerSuggestions.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    uiState.customerSuggestions.take(4).forEach { suggestion ->
                        RodvarListRow(
                            onClick = { viewModel.pickCustomerSuggestion(suggestion) },
                            leading = { IconBadge(Icons.Filled.Person, modifier = Modifier.size(36.dp)) },
                            headline = { Text(suggestion.name, style = MaterialTheme.typography.bodyMedium) },
                            supporting = {
                                Text(
                                    listOfNotNull(suggestion.phone, suggestion.lastVehicleLabel).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            showChevron = false
                        )
                    }
                }
            }

            SectionHeader("Origen")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                APPOINTMENT_SOURCES.forEach { source ->
                    FilterChip(
                        selected = uiState.source == source,
                        onClick = { viewModel.onSourceChange(source) },
                        label = { Text(source) }
                    )
                }
            }

            SectionHeader("Fecha y hora")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                val today = java.time.LocalDate.now().toString()
                val tomorrow = java.time.LocalDate.now().plusDays(1).toString()
                val dayAfter = java.time.LocalDate.now().plusDays(2).toString()
                FilterChip(selected = uiState.requestedDate == today, onClick = { viewModel.setDateInDays(0) }, label = { Text("Hoy") })
                FilterChip(selected = uiState.requestedDate == tomorrow, onClick = { viewModel.setDateInDays(1) }, label = { Text("Mañana") })
                FilterChip(selected = uiState.requestedDate == dayAfter, onClick = { viewModel.setDateInDays(2) }, label = { Text("Pasado mañana") })
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                QUICK_APPOINTMENT_HOURS.forEach { hour ->
                    FilterChip(
                        selected = uiState.requestedTime.startsWith("%02d:00".format(hour)),
                        onClick = { viewModel.setHour(hour) },
                        label = { Text(if (hour < 12) "$hour am" else if (hour == 12) "12 pm" else "${hour - 12} pm") }
                    )
                }
            }
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

            SectionHeader("Ubicación del servicio")
            OutlinedTextField(
                value = uiState.serviceAddress,
                onValueChange = viewModel::onServiceAddressChange,
                label = { Text("Dirección del domicilio") },
                placeholder = { Text("Busca calle, número, colonia o negocio") },
                singleLine = true,
                trailingIcon = {
                    if (uiState.isAddressSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else if (uiState.serviceLatitude != null && uiState.serviceLongitude != null) {
                        Icon(Icons.Filled.LocationOn, contentDescription = "Ubicación guardada", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )

            if (uiState.addressSuggestions.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    uiState.addressSuggestions.forEach { suggestion ->
                        RodvarListRow(
                            onClick = { viewModel.selectAddress(suggestion) },
                            leading = { IconBadge(Icons.Filled.LocationOn, modifier = Modifier.size(36.dp)) },
                            headline = { Text(suggestion.primaryText, style = MaterialTheme.typography.bodyMedium) },
                            supporting = { Text(suggestion.secondaryText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            showChevron = false
                        )
                    }
                }
            }

            if (uiState.serviceLatitude != null && uiState.serviceLongitude != null) {
                Text(
                    "Ubicación precisa guardada · ${"%.6f".format(uiState.serviceLatitude)}, ${"%.6f".format(uiState.serviceLongitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            } else if (uiState.addressError != null) {
                Text(
                    uiState.addressError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (!uiState.isEditMode) {
                SectionHeader("Vehículo")
                VehicleSearchField(
                    state = vehicleSearch,
                    onQueryChange = viewModel.vehicleSearcher::onQueryChange,
                    onSelect = viewModel.vehicleSearcher::select,
                    onClear = viewModel.vehicleSearcher::clear
                )

                if (!uiState.isRevision) SectionHeader("Productos / servicios")
                if (!uiState.isRevision) Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text(
                        if (uiState.selectedProducts.isEmpty()) "Ninguno agregado" else "${uiState.selectedProducts.sumOf { it.quantity }} pieza(s) · ${formatMoney(uiState.total)} estimado",
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
                            Text(
                                listOfNotNull(selected.variant?.bulbTypeName?.takeIf { it.isNotBlank() }, "${formatMoney(selected.unitPrice)} c/u").joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.changeQuantity(selected, selected.quantity - 1) }, enabled = selected.quantity > 1) {
                            Icon(Icons.Filled.Remove, contentDescription = "Restar")
                        }
                        Text(selected.quantity.toString(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 4.dp))
                        IconButton(onClick = { viewModel.changeQuantity(selected, selected.quantity + 1) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Sumar")
                        }
                        IconButton(onClick = { viewModel.removeProduct(selected) }) {
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

    if (showProductSheet) {
        ProductPickerSheet(
            products = uiState.availableProducts,
            countOf = { productId, variantId ->
                uiState.selectedProducts.filter { it.product.id == productId && it.bulbTypeId == variantId }.sumOf { it.quantity }
            },
            onPick = viewModel::addProduct,
            onDismiss = { showProductSheet = false }
        )
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
