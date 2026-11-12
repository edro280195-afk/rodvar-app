package com.rodvarled.admin.ui.warranties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import com.rodvarled.admin.ui.theme.RodvarPrimary
import com.rodvarled.admin.ui.theme.RodvarPrimaryContainer
import com.rodvarled.admin.ui.theme.warrantyStatusStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantiesListScreen(navController: NavHostController, viewModel: WarrantiesListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val pullState = rememberPullToRefreshState()

    Scaffold(topBar = { TopAppBar(title = { Text("Garantías Digitales", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SearchField(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Buscar por folio #ROD, nombre o teléfono",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                items(WARRANTY_FILTERS) { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { viewModel.onFilterChange(filter) },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RodvarPrimaryContainer, selectedLabelColor = RodvarPrimary)
                    )
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
                        title = "No hay garantías registradas",
                        subtitle = "Se generan automáticamente con su código QR al completar una instalación.",
                        icon = Icons.Filled.VerifiedUser,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.filtered, key = { it.id }) { warranty ->
                            val style = warrantyStatusStyle(warranty.isActive, warranty.isSigned)
                            val label = if (!warranty.isActive) "Revocada" else if (warranty.isSigned) "Activa / Firmada" else "Pendiente de Firma"
                            RodvarListRow(
                                onClick = { navController.navigate(Screen.WarrantyDetail.of(warranty.id)) },
                                modifier = Modifier.animateItem(),
                                leading = { InitialsAvatar(warranty.customerName, accent = RodvarPrimary) },
                                headline = { Text(warranty.customerName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)) },
                                supporting = {
                                    Text(warranty.folio, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = RodvarPrimary)
                                    Text(
                                        "Hasta: ${formatAppointmentDate(warranty.warrantyEnd)}",
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
