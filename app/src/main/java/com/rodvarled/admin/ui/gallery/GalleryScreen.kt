package com.rodvarled.admin.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rodvarled.admin.ui.components.EmptyState
import com.rodvarled.admin.ui.components.PhotoCaptureField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(navController: NavHostController, viewModel: GalleryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val pullState = rememberPullToRefreshState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Galería") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) { Icon(Icons.Filled.Add, contentDescription = "Nueva") }
        }
    ) { padding ->
        PullToRefreshBox(isRefreshing = uiState.isLoading, onRefresh = viewModel::refresh, state = pullState, modifier = Modifier.padding(padding).fillMaxSize()) {
            if (!uiState.isLoading && uiState.items.isEmpty()) {
                EmptyState(title = "Sin fotos en la galería", icon = Icons.Filled.PhotoLibrary, modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(uiState.items, key = { it.id }) { item ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().animateItem()) {
                            Column(Modifier.padding(12.dp)) {
                                AsyncImage(model = item.afterImageUrl, contentDescription = item.title, modifier = Modifier.fillMaxWidth().height(160.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    Text(item.title ?: item.productName ?: "Sin título", style = MaterialTheme.typography.bodyMedium)
                                    Row {
                                        IconButton(onClick = { viewModel.toggleFeatured(item) }) {
                                            Icon(if (item.isFeatured) Icons.Filled.Star else Icons.Filled.StarBorder, contentDescription = "Destacar", tint = MaterialTheme.colorScheme.tertiary)
                                        }
                                        IconButton(onClick = { viewModel.delete(item.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Activa", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                                    Switch(checked = item.isActive, onCheckedChange = { viewModel.toggleActive(item) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        GalleryCreateDialog(
            isSaving = uiState.isSaving,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, before, after ->
                viewModel.create(title, before, after)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun GalleryCreateDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var before by remember { mutableStateOf<String?>(null) }
    var after by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva foto de galería") },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 12.dp)) {
                    PhotoCaptureField(label = "Antes", base64Value = before, onValueChange = { before = it })
                    PhotoCaptureField(label = "Después *", base64Value = after, onValueChange = { after = it })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = after != null && !isSaving, onClick = { after?.let { onConfirm(title.ifBlank { null }, before, it) } }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
