package com.rodvarled.admin.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rodvarled.admin.ui.components.EmptyState
import com.rodvarled.admin.ui.components.RodvarListRow
import com.rodvarled.admin.ui.components.SearchField
import com.rodvarled.admin.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(navController: NavHostController, viewModel: InventoryListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val pullState = rememberPullToRefreshState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Inventario") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.ProductForm.create()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo producto")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SearchField(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = "Buscar producto",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                item {
                    FilterChip(selected = uiState.selectedCategoryId == null, onClick = { viewModel.onCategorySelected(null) }, label = { Text("Todas") })
                }
                items(uiState.categories) { category ->
                    FilterChip(
                        selected = uiState.selectedCategoryId == category.id,
                        onClick = { viewModel.onCategorySelected(category.id) },
                        label = { Text(category.name) }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.onlyLowStock,
                        onClick = { viewModel.toggleLowStockOnly() },
                        label = { Text("Stock bajo") },
                        leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(16.dp)) }
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
                        title = "Sin productos",
                        subtitle = "Agrega tu primer producto con el botón +.",
                        icon = Icons.Filled.Inventory2,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), modifier = Modifier.fillMaxSize()) {
                        items(uiState.filtered, key = { it.id }) { product ->
                            RodvarListRow(
                                onClick = { navController.navigate(Screen.ProductDetail.of(product.id)) },
                                modifier = Modifier.animateItem(),
                                leading = {
                                    if (!product.mainImageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = product.mainImageUrl,
                                            contentDescription = product.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Inventory2,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp).padding(12.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                headline = { Text(product.name, style = MaterialTheme.typography.titleSmall) },
                                supporting = {
                                    Text(product.categoryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$${"%.2f".format(product.price)} MXN", style = MaterialTheme.typography.bodyMedium)
                                },
                                trailing = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        val stock = product.effectiveStock()
                                        Text(
                                            "$stock en stock",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (product.isLowStock()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (product.isLowStock()) {
                                            Icon(Icons.Filled.Warning, contentDescription = "Stock bajo", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
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
