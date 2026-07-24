package com.rodvarled.admin.ui.warranties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.appointments.formatAppointmentDate
import com.rodvarled.admin.ui.components.EmptyState
import com.rodvarled.admin.ui.components.InitialsAvatar
import com.rodvarled.admin.ui.components.RodvarListRow
import com.rodvarled.admin.ui.components.SearchField
import com.rodvarled.admin.ui.components.StatusDot
import com.rodvarled.admin.ui.navigation.Screen
import com.rodvarled.admin.ui.theme.warrantyStatusStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantiesListScreen(navController: NavHostController, viewModel: WarrantiesListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val pullState = rememberPullToRefreshState()

    Scaffold(topBar = { TopAppBar(title = { Text("Garantías") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SearchField(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Buscar por folio, nombre o teléfono",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(WARRANTY_FILTERS) { filter ->
                    FilterChip(selected = uiState.filter == filter, onClick = { viewModel.onFilterChange(filter) }, label = { Text(filter) })
                }
            }

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = viewModel::refresh,
                state = pullState,
                modifier = Modifier.fillMaxSize().padding(top = 8.dp)
            ) {
                if (!uiState.isLoading && uiState.filtered.isEmpty()) {
                    EmptyState(
                        title = "No hay garantías",
                        subtitle = "Se generan automáticamente al completar una instalación.",
                        icon = Icons.Filled.VerifiedUser,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), modifier = Modifier.fillMaxSize()) {
                        items(uiState.filtered, key = { it.id }) { warranty ->
                            val style = warrantyStatusStyle(warranty.isActive, warranty.isSigned)
                            val label = if (!warranty.isActive) "Revocada" else if (warranty.isSigned) "Firmada" else "Sin firmar"
                            RodvarListRow(
                                onClick = { navController.navigate(Screen.WarrantyDetail.of(warranty.id)) },
                                modifier = Modifier.animateItem(),
                                leading = { InitialsAvatar(warranty.customerName, accent = style.content) },
                                headline = { Text(warranty.customerName, style = MaterialTheme.typography.titleSmall) },
                                supporting = {
                                    Text(warranty.folio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "Vigencia: ${formatAppointmentDate(warranty.warrantyStart)} – ${formatAppointmentDate(warranty.warrantyEnd)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailing = { StatusDot(label, style) }
                            )
                        }
                    }
                }
            }
        }
    }
}
