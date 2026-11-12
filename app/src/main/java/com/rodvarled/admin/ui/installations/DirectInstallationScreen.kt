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
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.LaunchedEffect
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
import com.rodvarled.admin.core.util.PhoneUtils
import com.rodvarled.admin.data.remote.dto.DirectInstallationResponse
import com.rodvarled.admin.ui.appointments.PAYMENT_METHODS
import com.rodvarled.admin.data.remote.dto.SERVICE_TYPE_INSTALLATION
import com.rodvarled.admin.data.remote.dto.SERVICE_TYPE_REVISION
import com.rodvarled.admin.ui.components.ChargeLinesEditor
import com.rodvarled.admin.ui.components.ChargeSummaryCard
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.components.ProductPickerSheet
import com.rodvarled.admin.ui.components.VehicleSearchField
import com.rodvarled.admin.ui.components.formatMoney
import com.rodvarled.admin.ui.components.IconBadge
import com.rodvarled.admin.ui.components.PhoneInputField
import com.rodvarled.admin.ui.media.MediaPickerSection
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
    val vehicleSearch by viewModel.vehicleSearcher.state.collectAsState()
    var showProductSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scannedCodeState = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("scannedInventoryUnit", null)
        ?.collectAsState()
    val scannedCode = scannedCodeState?.value

    LaunchedEffect(scannedCode) {
        if (!scannedCode.isNullOrBlank()) {
            navController.currentBackStackEntry?.savedStateHandle?.set("scannedInventoryUnit", null)
            viewModel.onInventoryUnitScanned(scannedCode)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isRevision) "Revisión a domicilio" else "Instalación directa") },
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
                                Text(if (uiState.isRevision) "Registrar revisión" else "Registrar instalación")
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
                onSignWarranty = completed.warrantyId?.let { warrantyId ->
                    {
                        navController.navigate(Screen.WarrantyDetail.of(warrantyId)) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                },
                onSharePortal = {
                    val message = if (completed.warrantyId != null) {
                        "¡Hola ${uiState.customerName.trim()}! Tu instalación ${completed.folio} quedó registrada. En tu portal de Rodvar LED puedes consultar y firmar tu garantía, ver tus puntos y tu historial: ${completed.portalUrl}"
                    } else {
                        "¡Hola ${uiState.customerName.trim()}! Tu revisión ${completed.folio} quedó registrada. En tu portal de Rodvar LED puedes ver tus puntos y tu historial: ${completed.portalUrl}"
                    }
                    val url = PhoneUtils.buildWhatsAppUrl(uiState.customerPhone, message)
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
                SectionHeader("Tipo de trabajo", modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = !uiState.isRevision,
                        onClick = { viewModel.onServiceTypeChange(SERVICE_TYPE_INSTALLATION) },
                        label = { Text("Instalación") }
                    )
                    FilterChip(
                        selected = uiState.isRevision,
                        onClick = { viewModel.onServiceTypeChange(SERVICE_TYPE_REVISION) },
                        label = { Text("Solo revisión · ${formatMoney(uiState.homeVisitFee)}") }
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

                SectionHeader("Vehículo")
                VehicleSearchField(
                    state = vehicleSearch,
                    onQueryChange = viewModel.vehicleSearcher::onQueryChange,
                    onSelect = viewModel.vehicleSearcher::select,
                    onClear = viewModel.vehicleSearcher::clear
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

                if (!uiState.isRevision) {
                    SectionHeader("Productos instalados")
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text(
                            if (uiState.charge.lines.isEmpty()) "Ninguno (solo mano de obra)"
                            else "${uiState.charge.lines.sumOf { it.quantity }} pieza(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row {
                            TextButton(onClick = { navController.navigate(Screen.QrScanner.route) }) { Text("Escanear QR") }
                            TextButton(onClick = { showProductSheet = true }) { Text("+ Agregar") }
                        }
                    }

                    ChargeLinesEditor(
                        lines = uiState.charge.lines,
                        onUnitPriceChange = viewModel::onUnitPriceChange,
                        onQuantityChange = viewModel::onQuantityChange,
                        onRemove = viewModel::removeLine
                    )
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
                ChargeSummaryCard(
                    state = uiState.charge,
                    homeVisitFee = uiState.homeVisitFee,
                    isRevision = uiState.isRevision,
                    onTotalChange = viewModel::onTotalChange,
                    onVisitChargedChange = viewModel::onVisitChargedChange,
                    onVisitCreditChange = viewModel::onVisitCreditChange,
                    modifier = Modifier.padding(top = 8.dp)
                )

                SectionHeader("Fotos y videos (opcional)")
                MediaPickerSection(
                    items = uiState.media,
                    onAdd = viewModel::addMedia,
                    onRemove = viewModel::removeMedia,
                    onStageChange = viewModel::changeMediaStage,
                    modifier = Modifier.padding(top = 6.dp)
                )

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

    if (showProductSheet && uiState.completed == null) {
        ProductPickerSheet(
            products = uiState.availableProducts,
            countOf = { productId, variantId ->
                uiState.charge.lines.filter { it.productId == productId && it.variantId == variantId }.sumOf { it.quantity }
            },
            onPick = viewModel::addProduct,
            onDismiss = { showProductSheet = false }
        )
    }
}

/** Pantalla de éxito: la instalación ya quedó registrada con su garantía pendiente de firma. */
@Composable
private fun SuccessContent(
    completed: DirectInstallationResponse,
    customerName: String,
    customerPhone: String,
    onSignWarranty: (() -> Unit)?,
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
        Text(
            if (onSignWarranty != null) "Instalación registrada" else "Revisión registrada",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
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

        if (onSignWarranty != null) {
            Button(onClick = onSignWarranty, modifier = Modifier.fillMaxWidth().padding(top = 28.dp)) {
                Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Firmar garantía ahora", modifier = Modifier.padding(start = 8.dp))
            }
        }
        OutlinedButton(onClick = onSharePortal, modifier = Modifier.fillMaxWidth().padding(top = if (onSignWarranty != null) 10.dp else 28.dp)) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Enviar portal por WhatsApp", modifier = Modifier.padding(start = 8.dp))
        }
        TextButton(onClick = onDone, modifier = Modifier.padding(top = 10.dp)) {
            Text("Terminar")
        }
    }
}
