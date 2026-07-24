package com.rodvarled.admin.ui.warranties

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.appointments.formatAppointmentDate
import com.rodvarled.admin.ui.components.CancelledBanner
import com.rodvarled.admin.ui.components.ConfirmDialog
import com.rodvarled.admin.ui.components.DetailRow
import com.rodvarled.admin.ui.components.ErrorState
import com.rodvarled.admin.ui.components.FullScreenLoading
import com.rodvarled.admin.ui.components.SectionHeader
import com.rodvarled.admin.ui.components.SignaturePad
import com.rodvarled.admin.ui.components.StatusBadge
import com.rodvarled.admin.ui.components.rememberSignaturePadState
import com.rodvarled.admin.ui.theme.warrantyStatusStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantyDetailScreen(
    warrantyId: Int,
    navController: NavHostController,
    viewModel: WarrantyDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showRevokeConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val signatureState = rememberSignaturePadState()
    var acceptedTerms by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) }; viewModel.consumeSnackbar() }
    }
    LaunchedEffect(uiState.deleted) { if (uiState.deleted) navController.popBackStack() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
                actions = {
                    if (uiState.warranty != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> FullScreenLoading(Modifier.padding(padding).fillMaxSize())
            uiState.error != null -> ErrorState(uiState.error!!, onRetry = viewModel::load, modifier = Modifier.padding(padding).fillMaxSize())
            uiState.warranty != null -> {
                val warranty = uiState.warranty!!
                val style = warrantyStatusStyle(warranty.isActive, warranty.isSigned)
                val label = if (!warranty.isActive) "Revocada" else if (warranty.isSigned) "Firmada" else "Sin firmar"
                val publicUrl = warranty.publicToken?.let { WARRANTY_PORTAL_BASE_URL + it }

                Column(
                    modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(warranty.customerName, style = MaterialTheme.typography.headlineSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        StatusBadge(label, style)
                        Text(warranty.folio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (!warranty.isActive) {
                        CancelledBanner(title = "Garantía revocada", reason = null, modifier = Modifier.padding(top = 20.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

                    DetailRow("Vehículo", warranty.vehicleInfo)
                    DetailRow(
                        "Vigencia",
                        "${formatAppointmentDate(warranty.warrantyStart)} – ${formatAppointmentDate(warranty.warrantyEnd)}",
                        showDivider = warranty.notes.isNullOrBlank() && warranty.items.isEmpty()
                    )
                    if (!warranty.notes.isNullOrBlank()) {
                        DetailRow("Notas", warranty.notes, showDivider = warranty.items.isEmpty())
                    }

                    if (warranty.items.isNotEmpty()) {
                        SectionHeader("Productos cubiertos")
                        warranty.items.forEach { item ->
                            Text(
                                "• ${item.quantity}x ${item.productName} — ${item.warrantyMonths} meses Rodvar / ${item.manufacturerWarrantyMonths} meses fabricante",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    if (warranty.isActive && !warranty.isSigned) {
                        SectionHeader("Firma del cliente")
                        Surface(
                            tonalElevation = 3.dp,
                            shadowElevation = 2.dp,
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            SignaturePad(state = signatureState, modifier = Modifier.fillMaxWidth().height(220.dp).padding(8.dp))
                        }
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { signatureState.clear() }) { Text("Limpiar firma") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
                            Text("El cliente acepta los términos de garantía de Rodvar LED.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = {
                                val sig = signatureState.exportPngBase64()
                                if (sig != null && acceptedTerms) viewModel.signWarranty(sig)
                            },
                            enabled = !uiState.actionInProgress && signatureState.hasSignature && acceptedTerms,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            if (uiState.actionInProgress) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                            else Text("Firmar garantía")
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    if (publicUrl != null) {
                        Button(
                            onClick = {
                                val message = "Hola ${warranty.customerName}! Aquí está tu garantía digital de Rodvar LED: $publicUrl"
                                val whatsAppUrl = "https://wa.me/52${warranty.customerPhone.filter { it.isDigit() }}?text=${Uri.encode(message)}"
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(whatsAppUrl))) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Compartir por WhatsApp")
                        }

                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(publicUrl)) },
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Copiar enlace")
                        }
                    }

                    if (warranty.isActive) {
                        if (warranty.isSigned) {
                            OutlinedButton(onClick = { showResetConfirm = true }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                Text("Permitir volver a firmar")
                            }
                        }
                        OutlinedButton(
                            onClick = { showRevokeConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 24.dp)
                        ) {
                            Text("Revocar garantía")
                        }
                    } else {
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showRevokeConfirm) {
        ConfirmDialog(
            title = "Revocar garantía",
            message = "La garantía dejará de estar activa. ¿Continuar?",
            confirmLabel = "Revocar",
            isDanger = true,
            onConfirm = { showRevokeConfirm = false; viewModel.revoke() },
            onDismiss = { showRevokeConfirm = false }
        )
    }
    if (showResetConfirm) {
        ConfirmDialog(
            title = "Restablecer firma",
            message = "Se borrará la firma actual y el cliente podrá firmar de nuevo.",
            confirmLabel = "Restablecer",
            onConfirm = { showResetConfirm = false; viewModel.resetSignature() },
            onDismiss = { showResetConfirm = false }
        )
    }
    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Eliminar garantía",
            message = "Esta acción es permanente y no se puede deshacer.",
            confirmLabel = "Eliminar",
            isDanger = true,
            onConfirm = { showDeleteConfirm = false; viewModel.delete() },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
