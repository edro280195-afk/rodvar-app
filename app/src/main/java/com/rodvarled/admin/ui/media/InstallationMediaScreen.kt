package com.rodvarled.admin.ui.media

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rodvarled.admin.core.media.MediaKind
import com.rodvarled.admin.core.media.MediaStage
import com.rodvarled.admin.data.remote.dto.InstallationMedia
import android.net.Uri

/** Todas las fotos y videos de una instalación: ver, agregar más y borrar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallationMediaScreen(
    navController: NavHostController,
    viewModel: InstallationMediaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var viewingPhoto by remember { mutableStateOf<InstallationMedia?>(null) }
    var confirmDelete by remember { mutableStateOf<InstallationMedia?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Fotos y videos", fontWeight = FontWeight.Bold)
                        val subtitle = listOfNotNull(state.folio, state.customerName).joinToString(" · ")
                        if (subtitle.isNotBlank()) {
                            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Agregar más", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    MediaPickerSection(
                        items = emptyList(),
                        onAdd = viewModel::addAndUpload,
                        onRemove = {},
                        onStageChange = { _, _ -> },
                        knownStages = state.knownStages
                    )
                }
            }

            state.error?.let { message ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (state.media.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Todavía no hay fotos ni videos de este trabajo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            items(state.media, key = { it.id }) { media ->
                RemoteMediaTile(
                    media = media,
                    onOpen = {
                        if (MediaKind.fromApi(media.kind) == MediaKind.VIDEO) {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(media.url), "video/*")
                                )
                            } catch (_: ActivityNotFoundException) {
                                // Sin reproductor instalado no hay nada más que hacer.
                            }
                        } else {
                            viewingPhoto = media
                        }
                    },
                    onDelete = { confirmDelete = media }
                )
            }
        }
    }

    viewingPhoto?.let { photo ->
        Dialog(onDismissRequest = { viewingPhoto = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = photo.url,
                    contentDescription = "Foto",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(onClick = { viewingPhoto = null }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
        }
    }

    confirmDelete?.let { media ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("¿Eliminar este archivo?") },
            text = { Text("Se borra de este trabajo y ya no se podrá recuperar.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(media)
                    confirmDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun RemoteMediaTile(media: InstallationMedia, onOpen: () -> Unit, onDelete: () -> Unit) {
    val isVideo = MediaKind.fromApi(media.kind) == MediaKind.VIDEO

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpen)
    ) {
        AsyncImage(
            model = media.thumbnailUrl ?: media.url,
            contentDescription = if (isVideo) "Video" else "Foto",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        if (isVideo) {
            Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            media.durationSeconds?.let {
                Text(
                    formatDuration(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).background(Color(0x99000000), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Eliminar",
                tint = Color.White,
                modifier = Modifier.background(Color(0x99000000), RoundedCornerShape(50)).padding(4.dp).size(16.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(topEnd = 10.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text(
                MediaStage.fromApi(media.stage).label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
