package com.rodvarled.admin.ui.warranties

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.navigation.Screen
import com.rodvarled.admin.core.util.PhoneUtils
import com.rodvarled.admin.ui.appointments.formatAppointmentDate
import com.rodvarled.admin.ui.components.CancelledBanner
import com.rodvarled.admin.ui.components.ConfirmDialog
import com.rodvarled.admin.ui.components.DetailRow
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.components.ErrorState
import com.rodvarled.admin.ui.components.FullScreenLoading
import com.rodvarled.admin.ui.components.QrCodeImage
import com.rodvarled.admin.ui.components.SectionHeader
import com.rodvarled.admin.ui.components.SignaturePad
import com.rodvarled.admin.ui.components.StatusBadge
import com.rodvarled.admin.ui.components.rememberSignaturePadState
import com.rodvarled.admin.ui.theme.RodvarEmerald
import com.rodvarled.admin.ui.theme.RodvarOutline
import com.rodvarled.admin.ui.theme.RodvarPrimary
import com.rodvarled.admin.ui.theme.RodvarSurface
import com.rodvarled.admin.ui.theme.RodvarTextSecondary
import com.rodvarled.admin.ui.theme.warrantyStatusStyle
import kotlinx.coroutines.launch
import com.rodvarled.admin.data.remote.dto.BulbTypeVariant
import com.rodvarled.admin.data.remote.dto.ProductDetail

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
    var showClaimDialog by remember { mutableStateOf(false) }
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
                title = { Text("Garantía Digital Oficial", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
                actions = {
                    if (uiState.warranty != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error) }
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
                val label = if (!warranty.isActive) "Revocada" else if (warranty.isSigned) "Activa / Firmada" else "Pendiente de Firma"
                val publicUrl = warranty.publicToken?.let { WARRANTY_PORTAL_BASE_URL + it }

                Column(
                    modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tarjeta Principal Certificado de Garantía con QR
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                        modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.extraLarge)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(36.dp).background(RodvarPrimary.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = RodvarPrimary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text("RODVAR LED", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = RodvarPrimary)
                                }
                                StatusBadge(label, style)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(warranty.customerName, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                            Text(warranty.folio, style = MaterialTheme.typography.labelLarge, color = RodvarTextSecondary)

                            Spacer(modifier = Modifier.height(20.dp))

                            // Código QR Generado
                            QrCodeImage(
                                content = publicUrl ?: "RODVAR_WARRANTY_${warranty.folio}",
                                sizeDp = 180.dp
                            )

                            Text(
                                "Escanea este QR para validar autenticidad",
                                style = MaterialTheme.typography.labelSmall,
                                color = RodvarTextSecondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    if (!warranty.isActive) {
                        CancelledBanner(title = "Garantía revocada", reason = null)
                    }

                    // Detalle del Vehículo y Vigencia
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                        modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
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
                                SectionHeader("Productos cubiertos por la garantía")
                                warranty.items.forEach { item ->
                                    Text(
                                        "• ${item.quantity}x ${item.productName} — ${item.warrantyMonths} meses Rodvar / ${item.manufacturerWarrantyMonths} meses fabricante",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (warranty.isActive && !warranty.isSigned) {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                            modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                SectionHeader("Firma Digital del Cliente")
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = RodvarSurface,
                                    modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.medium).padding(vertical = 4.dp)
                                ) {
                                    SignaturePad(state = signatureState, modifier = Modifier.fillMaxWidth().height(200.dp).padding(8.dp))
                                }
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { signatureState.clear() }) { Text("Limpiar Firma") }
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
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    if (uiState.actionInProgress) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                                    else Text("Firmar Garantía", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                        modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionHeader("Garantías aplicadas (${warranty.claims.size})")
                            if (warranty.claims.isEmpty()) {
                                Text("Todavía no hay reposiciones o reparaciones registradas.", color = RodvarTextSecondary)
                            } else {
                                warranty.claims.forEach { claim ->
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(claim.folio, fontWeight = FontWeight.Bold)
                                            Text("$${"%.2f".format(claim.totalCost ?: 0.0)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                        }
                                        Text(claim.reason, style = MaterialTheme.typography.bodySmall)
                                        Text("${claim.quantity} pieza(s) · ${claim.productName ?: "Solo mano de obra"}", style = MaterialTheme.typography.bodySmall, color = RodvarTextSecondary)
                                    }
                                    HorizontalDivider()
                                }
                            }
                            OutlinedButton(
                                onClick = { showClaimDialog = true },
                                enabled = !uiState.actionInProgress,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Registrar garantía aplicada")
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { navController.navigate(Screen.InstallationMedia.of(warranty.installationId)) },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fotos y videos del trabajo")
                    }

                    // Acciones de Compartir por WhatsApp
                    if (publicUrl != null) {
                        Button(
                            onClick = {
                                val message = "Hola ${warranty.customerName}! Aquí está tu garantía digital oficial de Rodvar LED: $publicUrl"
                                val whatsAppUrl = PhoneUtils.buildWhatsAppUrl(warranty.customerPhone, message)
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(whatsAppUrl))) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RodvarEmerald),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Compartir por WhatsApp", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(publicUrl)) },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Copiar Enlace Digital")
                        }
                    }

                    if (warranty.isActive) {
                        if (warranty.isSigned) {
                            OutlinedButton(onClick = { showResetConfirm = true }, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                                Text("Permitir Volver a Firmar")
                            }
                        }
                        OutlinedButton(
                            onClick = { showRevokeConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                        ) {
                            Text("Revocar Garantía")
                        }
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
    if (showClaimDialog) {
        var reason by remember { mutableStateOf("") }
        var quantity by remember { mutableStateOf("1") }
        var laborCost by remember { mutableStateOf("0") }
        var selectedProduct by remember { mutableStateOf<ProductDetail?>(null) }
        var selectedVariant by remember { mutableStateOf<BulbTypeVariant?>(null) }

        AlertDialog(
            onDismissRequest = { if (!uiState.actionInProgress) showClaimDialog = false },
            title = { Text("Registrar garantía aplicada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("El costo se congela para que aparezca en tus métricas y no se mezcle con ventas.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Motivo") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    DropdownSelector(
                        label = "Producto repuesto (opcional)",
                        items = uiState.products,
                        selected = selectedProduct,
                        labelOf = { it.name },
                        onSelect = { selectedProduct = it; selectedVariant = null },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (selectedProduct?.variants?.isNotEmpty() == true) {
                        DropdownSelector(
                            label = "Variante / socket",
                            items = selectedProduct!!.variants,
                            selected = selectedVariant,
                            labelOf = { it.bulbTypeName },
                            onSelect = { selectedVariant = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Cantidad") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = laborCost, onValueChange = { laborCost = it }, label = { Text("Mano de obra") }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createClaim(
                            reason = reason.trim(),
                            product = selectedProduct,
                            variant = selectedVariant,
                            quantity = quantity.toIntOrNull() ?: 1,
                            laborCost = laborCost.toDoubleOrNull() ?: 0.0
                        )
                        showClaimDialog = false
                    },
                    enabled = reason.isNotBlank() && (quantity.toIntOrNull() ?: 0) > 0 && !uiState.actionInProgress
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showClaimDialog = false }, enabled = !uiState.actionInProgress) { Text("Cancelar") } }
        )
    }
}
