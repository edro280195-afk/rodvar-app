package com.rodvarled.admin.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.rodvarled.admin.ui.components.MoneyField
import com.rodvarled.admin.ui.components.formatMoney
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.IconBadge
import com.rodvarled.admin.ui.components.RodvarListRow
import com.rodvarled.admin.ui.navigation.Screen
import com.rodvarled.admin.ui.theme.RodvarPrimary

private data class MoreItem(val label: String, val subtitle: String, val icon: ImageVector, val screen: Screen)

private val moreItems = listOf(
    MoreItem("Instalación Directa", "Registra un trabajo terminado al instante", Icons.Filled.Build, Screen.DirectInstallation),
    MoreItem("Garantías Digitales", "Certificados con código QR y validación", Icons.Filled.Verified, Screen.WarrantiesList),
    MoreItem("Cotizaciones", "Borradores e itinerarios para clientes", Icons.AutoMirrored.Filled.ReceiptLong, Screen.QuotesList),
    MoreItem("Compatibilidad de Focos", "Consulta de sockets (H11, 9005, H4)", Icons.Filled.Tune, Screen.Compatibility),
    MoreItem("Reseñas de Clientes", "Calificaciones y retroalimentación", Icons.Filled.RateReview, Screen.Reviews),
    MoreItem("Galería de Fotos", "Evidencia de trabajos de iluminación LED", Icons.Filled.PhotoLibrary, Screen.Gallery),
    MoreItem("Reportes del negocio", "Ventas, descuentos y productos con mayor movimiento", Icons.Filled.Assessment, Screen.Reports),
    MoreItem("Perfil y Ajustes", "Biometría, sesión y notificaciones FCM", Icons.Filled.Person, Screen.Profile)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    navController: NavHostController,
    serviceFeeViewModel: ServiceFeeViewModel = hiltViewModel()
) {
    val feeState by serviceFeeViewModel.uiState.collectAsState()
    var showFeeDialog by remember { mutableStateOf(false) }

    if (showFeeDialog) {
        AlertDialog(
            onDismissRequest = { if (!feeState.isSaving) showFeeDialog = false },
            title = { Text("Tarifa de revisión a domicilio") },
            text = {
                MoneyField(
                    value = feeState.draft,
                    onValueChange = serviceFeeViewModel::onDraftChange,
                    label = "Costo de la revisión",
                    supportingText = feeState.error
                        ?: "Es el costo cuando no se instala nada. Se avisa en el WhatsApp, el portal del cliente y el sitio web."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { serviceFeeViewModel.save { showFeeDialog = false } },
                    enabled = !feeState.isSaving
                ) {
                    if (feeState.isSaving) CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
                    else Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeeDialog = false }, enabled = !feeState.isSaving) { Text("Cancelar") }
            }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Más Opciones", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                RodvarListRow(
                    onClick = {
                        serviceFeeViewModel.startEditing()
                        showFeeDialog = true
                    },
                    leading = { IconBadge(Icons.Filled.Payments, accent = RodvarPrimary) },
                    headline = { Text("Tarifa de revisión", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)) },
                    supporting = {
                        Text(
                            if (feeState.isLoading) "Consultando…" else "Hoy: ${formatMoney(feeState.fee)} · se avisa al cliente en WhatsApp, portal y web",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
            items(moreItems) { item ->
                RodvarListRow(
                    onClick = { navController.navigate(item.screen.route) },
                    leading = { IconBadge(item.icon, accent = RodvarPrimary) },
                    headline = { Text(item.label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)) },
                    supporting = { Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }
        }
    }
}
