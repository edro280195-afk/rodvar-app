package com.rodvarled.admin.ui.appointments

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.AppointmentStatusStepper
import com.rodvarled.admin.ui.components.CancelledBanner
import com.rodvarled.admin.ui.components.ConfirmDialog
import com.rodvarled.admin.ui.components.ErrorState
import com.rodvarled.admin.ui.components.FullScreenLoading
import com.rodvarled.admin.ui.navigation.Screen
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
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    uiState.appointment?.let { appointment ->
                        IconButton(onClick = viewModel::sendItinerary, enabled = !uiState.actionInProgress) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar itinerario")
                        }
                        IconButton(onClick = { navController.navigate(Screen.AppointmentForm.edit(appointment.id)) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
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
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(appointment.customerName, style = MaterialTheme.typography.headlineSmall)
                    OutlinedButton(
                        onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${appointment.customerPhone}"))) } },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.height(16.dp))
                        Text(appointment.customerPhone, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
                    }

                    if (appointment.status == "Cancelada") {
                        CancelledBanner(reason = appointment.cancellationReason, modifier = Modifier.padding(top = 20.dp))
                    } else {
                        AppointmentStatusStepper(appointment.status, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

                    InfoLine("Fecha", formatAppointmentDate(appointment.requestedDate))
                    formatAppointmentTime(appointment.requestedTime)?.let { InfoLine("Hora", it) }
                    InfoLine("Vehículo", appointment.vehicleInfo)
                    InfoLine("Origen", appointment.source)
                    appointment.quoteTotal?.let { InfoLine("Total cotizado", "$${"%.2f".format(it)} MXN") }
                    if (!appointment.notes.isNullOrBlank()) InfoLine("Notas", appointment.notes)
                    if (appointment.itinerarySentAt != null) InfoLine("Itinerario", "Enviado ✓")

                    Spacer(Modifier.height(16.dp))

                    if (appointment.warrantyId != null) {
                        OutlinedButton(
                            onClick = { navController.navigate(Screen.WarrantyDetail.of(appointment.warrantyId)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Ver garantía")
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    when (appointment.status) {
                        "Pendiente" -> {
                            Button(onClick = viewModel::confirm, enabled = !uiState.actionInProgress, modifier = Modifier.fillMaxWidth()) {
                                Text("Confirmar cita")
                            }
                            TextButton(onClick = { showCancelDialog = true }, enabled = !uiState.actionInProgress, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)) {
                                Text("Cancelar cita", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        "Confirmada" -> {
                            Button(
                                onClick = { navController.navigate(Screen.CompleteInstallation.of(appointment.id)) },
                                enabled = !uiState.actionInProgress,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Completar instalación")
                            }
                            TextButton(onClick = { showCancelDialog = true }, enabled = !uiState.actionInProgress, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)) {
                                Text("Cancelar cita", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    if (uiState.actionInProgress) {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally))
                    }

                    Spacer(Modifier.height(24.dp))
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
            title = { Text("Cancelar cita") },
            text = {
                Column {
                    Text("¿Por qué se cancela? (opcional)")
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancel(cancelReason.ifBlank { null })
                    cancelReason = ""
                }) { Text("Cancelar cita") }
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 16.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
}
