package com.rodvarled.admin.ui.quotes

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.ErrorState
import com.rodvarled.admin.ui.components.FullScreenLoading
import com.rodvarled.admin.ui.components.SectionHeader
import com.rodvarled.admin.ui.components.StatusBadge
import com.rodvarled.admin.ui.theme.quoteStatusStyle
import com.rodvarled.admin.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailScreen(
    quoteId: Int,
    navController: NavHostController,
    viewModel: QuoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) }; viewModel.consumeSnackbar() }
    }
    LaunchedEffect(uiState.convertedAppointmentId) {
        uiState.convertedAppointmentId?.let { navController.navigate(Screen.AppointmentDetail.of(it)) { popUpTo(Screen.QuotesList.route) } }
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
            uiState.quote != null -> {
                val quote = uiState.quote!!
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(quote.customerName, style = MaterialTheme.typography.headlineSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        StatusBadge(quote.status, quoteStatusStyle(quote.status))
                        Text(quote.folio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(quote.customerPhone, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
                    if (!quote.vehicleInfo.isNullOrBlank()) Text(quote.vehicleInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

                    SectionHeader("Productos")
                    uiState.items.forEach { item ->
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text("${item.quantity}x ${item.productName}", style = MaterialTheme.typography.bodyMedium)
                            Text("$${"%.2f".format(item.subtotal)}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text("Total: $${"%.2f".format(quote.total)} MXN", style = MaterialTheme.typography.titleMedium)

                    Spacer(Modifier.height(20.dp))

                    if (quote.status != "Agendado" && quote.status != "Instalado") {
                        Button(onClick = viewModel::convertToAppointment, enabled = !uiState.actionInProgress, modifier = Modifier.fillMaxWidth()) {
                            Text("Convertir en cita")
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                        OutlinedButton(onClick = { viewModel.updateStatus("Pendiente") }, enabled = !uiState.actionInProgress, modifier = Modifier.weight(1f)) { Text("Pendiente") }
                        OutlinedButton(onClick = { viewModel.updateStatus("Cancelado") }, enabled = !uiState.actionInProgress, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    }
                }
            }
        }
    }
}
