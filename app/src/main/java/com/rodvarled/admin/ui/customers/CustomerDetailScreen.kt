package com.rodvarled.admin.ui.customers

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.appointments.formatAppointmentDate
import com.rodvarled.admin.ui.components.ErrorState
import com.rodvarled.admin.ui.components.FullScreenLoading
import com.rodvarled.admin.ui.components.SectionHeader
import com.rodvarled.admin.ui.components.StatusBadge
import com.rodvarled.admin.ui.theme.appointmentStatusStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Int,
    navController: NavHostController,
    viewModel: CustomerDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPointsDialog by remember { mutableStateOf(false) }
    var pointsDelta by remember { mutableStateOf("") }
    var pointsReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadPortalLink() }
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) }; viewModel.consumeSnackbar() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") } }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> FullScreenLoading(Modifier.padding(padding).fillMaxSize())
            uiState.error != null -> ErrorState(uiState.error!!, onRetry = viewModel::load, modifier = Modifier.padding(padding).fillMaxSize())
            uiState.customer != null -> {
                val customer = uiState.customer!!
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(customer.name, style = MaterialTheme.typography.headlineSmall)
                    Text(customer.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    if (!customer.email.isNullOrBlank()) Text(customer.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!customer.address.isNullOrBlank()) Text(customer.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Text(" ${customer.pointsBalance} puntos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
                        }
                        TextButton(onClick = { showPointsDialog = true }) { Text("Ajustar") }
                    }
                    if (uiState.portalUrl != null) {
                        Row(modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)) {
                            OutlinedButton(onClick = {
                                val message = "Hola ${customer.name}! Aquí está tu enlace para ver tus citas, garantías y puntos: ${uiState.portalUrl}"
                                val url = "https://wa.me/52${customer.phone.filter { it.isDigit() }}?text=${Uri.encode(message)}"
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text("Compartir su portal")
                            }
                        }
                    }

                    if (customer.appointments.isNotEmpty()) {
                        SectionHeader("Citas (${customer.appointments.size})")
                        customer.appointments.forEach { appt ->
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(formatAppointmentDate(appt.requestedDate), style = MaterialTheme.typography.bodyMedium)
                                StatusBadge(appt.status, appointmentStatusStyle(appt.status))
                            }
                        }
                        HorizontalDivider()
                    }

                    if (customer.installations.isNotEmpty()) {
                        SectionHeader("Instalaciones (${customer.installations.size})")
                        customer.installations.forEach { inst ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("${inst.folio} · $${"%.2f".format(inst.totalAmount)} MXN", style = MaterialTheme.typography.bodyMedium)
                                Text(inst.vehicleInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }

                    if (customer.warranties.isNotEmpty()) {
                        SectionHeader("Garantías (${customer.warranties.size})")
                        customer.warranties.forEach { w ->
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(w.folio, style = MaterialTheme.typography.bodyMedium)
                                Text(if (w.isSigned) "Firmada" else "Sin firmar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }

                    if (customer.pointsHistory.isNotEmpty()) {
                        SectionHeader("Historial de puntos")
                        customer.pointsHistory.forEach { p ->
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(p.reason, style = MaterialTheme.typography.bodyMedium)
                                Text((if (p.delta > 0) "+" else "") + p.delta, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPointsDialog) {
        AlertDialog(
            onDismissRequest = { showPointsDialog = false },
            title = { Text("Ajustar puntos") },
            text = {
                Column {
                    OutlinedTextField(
                        value = pointsDelta,
                        onValueChange = { pointsDelta = it },
                        label = { Text("Cantidad (negativo para restar)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pointsReason,
                        onValueChange = { pointsReason = it },
                        label = { Text("Motivo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val delta = pointsDelta.toIntOrNull() ?: 0
                    viewModel.adjustPoints(delta, pointsReason)
                    showPointsDialog = false
                    pointsDelta = ""; pointsReason = ""
                }) { Text("Aplicar") }
            },
            dismissButton = { TextButton(onClick = { showPointsDialog = false }) { Text("Cancelar") } }
        )
    }
}
