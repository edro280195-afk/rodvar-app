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
import androidx.compose.material.icons.filled.Verified
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
fun MoreScreen(navController: NavHostController) {
    Scaffold(topBar = { TopAppBar(title = { Text("Más Opciones", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
