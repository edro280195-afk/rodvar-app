package com.rodvarled.admin.ui.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.rodvarled.admin.core.media.MediaUploadScheduler
import com.rodvarled.admin.core.media.MediaUploadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class MediaUploadViewModel @Inject constructor(
    private val scheduler: MediaUploadScheduler
) : ViewModel() {
    val status: Flow<MediaUploadStatus> = scheduler.status

    fun retryFailed(status: MediaUploadStatus) = scheduler.retry(status.failed)
    fun discardFailed(status: MediaUploadStatus) = scheduler.discard(status.failed)
}

/** Franja delgada arriba de la app: avisa de las fotos y videos que se están subiendo o que fallaron. */
@Composable
fun MediaUploadBanner(modifier: Modifier = Modifier, viewModel: MediaUploadViewModel = hiltViewModel()) {
    val status by viewModel.status.collectAsState(initial = MediaUploadStatus())
    if (status.isIdle) return

    val hasFailures = status.failed.isNotEmpty()
    Surface(
        color = if (hasFailures) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            if (status.pending > 0) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    if (status.uploading > 0) "Subiendo ${status.pending} foto(s) o video(s)…" else "${status.pending} archivo(s) esperando conexión para subir",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
            }
            if (hasFailures) {
                Text(
                    "${status.failed.size} archivo(s) no se pudieron subir",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { viewModel.retryFailed(status) }) { Text("Reintentar") }
                TextButton(onClick = { viewModel.discardFailed(status) }) { Text("Descartar") }
            }
        }
    }
}
