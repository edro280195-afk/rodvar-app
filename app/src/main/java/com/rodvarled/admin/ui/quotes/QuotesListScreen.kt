package com.rodvarled.admin.ui.quotes

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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.EmptyState
import com.rodvarled.admin.ui.components.InitialsAvatar
import com.rodvarled.admin.ui.components.RodvarListRow
import com.rodvarled.admin.ui.components.StatusDot
import com.rodvarled.admin.ui.theme.quoteStatusStyle
import com.rodvarled.admin.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesListScreen(navController: NavHostController, viewModel: QuotesListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val pullState = rememberPullToRefreshState()

    Scaffold(topBar = { TopAppBar(title = { Text("Cotizaciones") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(16.dp)) {
                items(QUOTE_FILTERS) { filter ->
                    FilterChip(selected = uiState.filter == filter, onClick = { viewModel.onFilterChange(filter) }, label = { Text(filter) })
                }
            }

            PullToRefreshBox(isRefreshing = uiState.isLoading, onRefresh = viewModel::refresh, state = pullState, modifier = Modifier.fillMaxSize()) {
                if (!uiState.isLoading && uiState.filtered.isEmpty()) {
                    EmptyState(title = "Sin cotizaciones", icon = Icons.AutoMirrored.Filled.ReceiptLong, modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), modifier = Modifier.fillMaxSize()) {
                        items(uiState.filtered, key = { it.id }) { quote ->
                            RodvarListRow(
                                onClick = { navController.navigate(Screen.QuoteDetail.of(quote.id)) },
                                modifier = Modifier.animateItem(),
                                leading = { InitialsAvatar(quote.customerName) },
                                headline = { Text(quote.customerName, style = MaterialTheme.typography.titleSmall) },
                                supporting = { Text(quote.folio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                trailing = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        StatusDot(quote.status, quoteStatusStyle(quote.status))
                                        Text("$${"%.2f".format(quote.total)} MXN", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
