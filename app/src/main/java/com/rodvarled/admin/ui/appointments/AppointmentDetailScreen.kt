package com.rodvarled.admin.ui.appointments

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.AppointmentStatusStepper
import com.rodvarled.admin.ui.components.CancelledBanner
import com.rodvarled.admin.ui.components.ConfirmDialog
import com.rodvarled.admin.ui.components.ErrorState
import com.rodvarled.admin.ui.components.FullScreenLoading
import com.rodvarled.admin.ui.components.InitialsAvatar
import com.rodvarled.admin.ui.components.formatMoney
import com.rodvarled.admin.ui.navigation.Screen
import com.rodvarled.admin.core.util.openGoogleMapsNavigation
import com.rodvarled.admin.core.util.PhoneUtils
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
fun AppointmentDetailScreen(
    appointmentId: Int,
    navController: NavHostController,
    viewModel: AppointmentDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    LaunchedEffect(uiState.whatsAppUrlToOpen) {
        uiState.whatsAppUrlToOpen?.let { url ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            viewModel.consumeWhatsAppUrl()
        }
    }
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.consumeSnackbar()
        }
    }
    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) navController.popBackStack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Cita", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    uiState.appointment?.let { appointment ->
                        IconButton(onClick = viewModel::sendItinerary, enabled = !uiState.actionInProgress) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar itinerario", tint = RodvarPrimary)
                        }
                        IconButton(onClick = { navController.navigate(Screen.AppointmentForm.edit(appointment.id)) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = RodvarRed)
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> FullScreenLoading(Modifier.padding(padding).fillMaxSize())
            uiState.error != null -> ErrorState(uiState.error!!, onRetry = viewModel::load, modifier = Modifier.padding(padding).fillMaxSize())
            uiState.appointment != null -> {
                val appointment = uiState.appointment!!
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tarjeta Principal de Cliente
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.extraLarge)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                InitialsAvatar(appointment.customerName, accent = RodvarPrimary)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        appointment.customerName,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = RodvarTextPrimary
                                    )
                                    Text(
                                        appointment.customerPhone,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = RodvarTextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Acciones de Comunicación Directa (Llamada y WhatsApp)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = RodvarEmerald.copy(alpha = 0.12f),
                                    modifier = Modifier.weight(1f).clickable {
                                        val waUrl = PhoneUtils.buildWhatsAppUrl(appointment.customerPhone)
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                                        runCatching { context.startActivity(intent) }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = RodvarEmerald, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("WhatsApp", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = RodvarEmerald)
                                    }
                                }

                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = RodvarPrimaryContainer,
                                    modifier = Modifier.weight(1f).clickable {
                                        val cleanPhone = appointment.customerPhone.replace(Regex("[^0-9+]"), "")
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                                        runCatching { context.startActivity(intent) }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Call, contentDescription = null, tint = RodvarPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Llamar", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = RodvarPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Stepper o Banner de Estado
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            if (appointment.status == "Cancelada") {
                                CancelledBanner(reason = appointment.cancellationReason)
                            } else {
                                AppointmentStatusStepper(appointment.status)
                            }
                        }
                    }

                    // Ficha Informativa del Servicio y Vehículo
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Información de la Cita", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 12.dp))

                            InfoLine("Fecha", formatAppointmentDate(appointment.requestedDate))
                            formatAppointmentTime(appointment.requestedTime)?.let { InfoLine("Hora", it) }
                            InfoLine("Vehículo", appointment.vehicleInfo)
                            InfoLine("Origen", appointment.source)
                            if (appointment.isRevision) {
                                InfoLine(
                                    "Servicio",
                                    "Revisión a domicilio" + (appointment.visitFeeAmount?.let { " · ${formatMoney(it)}" } ?: "")
                                )
                            }
                            appointment.quoteTotal?.let { InfoLine("Total Cotizado", "$${"%.2f".format(it)} MXN") }
                            if (!appointment.notes.isNullOrBlank()) InfoLine("Notas u Observaciones", appointment.notes)
                            if (appointment.itinerarySentAt != null) InfoLine("Itinerario", "Enviado por WhatsApp ✓")

                            if (!appointment.serviceAddress.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Domicilio del servicio", style = MaterialTheme.typography.labelLarge, color = RodvarTextSecondary)
                                Text(
                                    appointment.serviceAddress!!,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = RodvarTextPrimary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                OutlinedButton(
                                    onClick = {
                                        openGoogleMapsNavigation(
                                            context = context,
                                            address = appointment.serviceAddress,
                                            latitude = appointment.serviceLatitude,
                                            longitude = appointment.serviceLongitude
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                                ) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text(if (appointment.serviceLatitude != null) "Navegar con Google Maps" else "Buscar en Google Maps")
                                }
                            }
                        }
                    }

                    // Botones Principales de Acción según estado
                    if (appointment.warrantyId != null) {
                        Button(
                            onClick = { navController.navigate(Screen.WarrantyDetail.of(appointment.warrantyId)) },
                            colors = ButtonDefaults.buttonColors(containerColor = RodvarEmerald),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Ver Garantía Digital Emitida", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (appointment.installationId != null) {
                        OutlinedButton(
                            onClick = { navController.navigate(Screen.InstallationMedia.of(appointment.installationId)) },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Fotos y videos del trabajo")
                        }
                    }

                    when (appointment.status) {
                        "Pendiente" -> {
                            Button(
                                onClick = viewModel::confirm,
                                enabled = !uiState.actionInProgress,
                                colors = ButtonDefaults.buttonColors(containerColor = RodvarPrimary),
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("Confirmar Cita", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                enabled = !uiState.actionInProgress,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RodvarRed),
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancelar Cita")
                            }
                        }
                        "Confirmada" -> {
                            Button(
                                onClick = { navController.navigate(Screen.CompleteInstallation.of(appointment.id)) },
                                enabled = !uiState.actionInProgress,
                                colors = ButtonDefaults.buttonColors(containerColor = RodvarEmerald),
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text(
                                    if (appointment.isRevision) "Completar y Cobrar Revisión" else "Completar Instalación y Crear Garantía",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                enabled = !uiState.actionInProgress,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RodvarRed),
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancelar Cita")
                            }
                        }
                    }

                    if (uiState.actionInProgress) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Eliminar cita",
            message = "Esta acción no se puede deshacer. ¿Eliminar la cita de forma permanente?",
            confirmLabel = "Eliminar",
            isDanger = true,
            onConfirm = { showDeleteConfirm = false; viewModel.delete() },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar Cita", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Indica la razón o motivo de cancelación (opcional):", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        placeholder = { Text("Ej. Cliente reagendó para el próximo mes") },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancel(cancelReason.ifBlank { null })
                        cancelReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RodvarRed)
                ) { Text("Confirmar Cancelación", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Volver") }
            }
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = RodvarTextSecondary, modifier = Modifier.padding(end = 16.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = RodvarTextPrimary)
    }
    HorizontalDivider(color = RodvarOutline.copy(alpha = 0.5f))
}
