package com.rodvarled.admin.ui.reviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(navController: NavHostController, viewModel: ReviewsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val pullState = rememberPullToRefreshState()

    Scaffold(topBar = { TopAppBar(title = { Text("Reseñas") }) }) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::refresh,
            state = pullState,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            if (!uiState.isLoading && uiState.reviews.isEmpty()) {
                EmptyState(title = "Sin reseñas", icon = Icons.Filled.RateReview, modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(uiState.reviews, key = { it.id }) { review ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().animateItem()) {
                            Column(Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        Text(review.customerName, style = MaterialTheme.typography.titleSmall)
                                        Row { repeat(review.rating) { Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 4.dp)) } }
                                    }
                                    IconButton(onClick = { viewModel.delete(review.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
                                }
                                Text(review.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                                Row(modifier = Modifier.padding(top = 8.dp)) {
                                    if (review.isApproved) {
                                        TextButton(onClick = { viewModel.approve(review.id, false) }) { Text("Ocultar") }
                                    } else {
                                        TextButton(onClick = { viewModel.approve(review.id, true) }) { Text("Aprobar") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
