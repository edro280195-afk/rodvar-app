package com.rodvarled.admin.ui.more

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.IconBadge
import com.rodvarled.admin.ui.components.RodvarListRow
import com.rodvarled.admin.ui.navigation.Screen

private data class MoreItem(val label: String, val icon: ImageVector, val screen: Screen)

private val moreItems = listOf(
    MoreItem("Instalación directa", Icons.Filled.Build, Screen.DirectInstallation),
    MoreItem("Garantías", Icons.Filled.Verified, Screen.WarrantiesList),
    MoreItem("Cotizaciones", Icons.AutoMirrored.Filled.ReceiptLong, Screen.QuotesList),
    MoreItem("Compatibilidad de focos", Icons.Filled.Tune, Screen.Compatibility),
    MoreItem("Reseñas", Icons.Filled.RateReview, Screen.Reviews),
    MoreItem("Galería", Icons.Filled.PhotoLibrary, Screen.Gallery),
    MoreItem("Perfil", Icons.Filled.Person, Screen.Profile)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(navController: NavHostController) {
    Scaffold(topBar = { TopAppBar(title = { Text("Más") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
        ) {
            items(moreItems) { item ->
                RodvarListRow(
                    onClick = { navController.navigate(item.screen.route) },
                    leading = { IconBadge(item.icon) },
                    headline = { Text(item.label, style = MaterialTheme.typography.bodyLarge) }
                )
            }
        }
    }
}
