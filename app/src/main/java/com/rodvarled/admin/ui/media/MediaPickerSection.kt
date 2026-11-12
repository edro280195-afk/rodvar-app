package com.rodvarled.admin.ui.media

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.rodvarled.admin.core.media.LocalMedia
import com.rodvarled.admin.core.media.MediaFiles
import com.rodvarled.admin.core.media.MediaKind
import com.rodvarled.admin.core.media.MediaStage
import com.rodvarled.admin.core.util.createTempCameraFile
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/** Abre la cámara de video del teléfono con tope de duración y de tamaño para que el archivo sea manejable. */
private class CaptureVideoWithLimits : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent =
        Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, input)
            .putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60)
            .putExtra(MediaStore.EXTRA_SIZE_LIMIT, 90L * 1024 * 1024)
            .putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean = resultCode == Activity.RESULT_OK
}

/**
 * Para la primera foto sugiere "Antes", para las siguientes "Después". Se cambia tocando la etiqueta de cada archivo.
 */
private fun suggestedStage(stages: List<MediaStage>): MediaStage =
    if (stages.none { it == MediaStage.BEFORE }) MediaStage.BEFORE else MediaStage.AFTER

/**
 * Evidencia del trabajo: todas las fotos y videos que se quieran, no solo un antes y un después.
 * Los archivos se guardan en el teléfono y se suben solos en segundo plano al terminar el trabajo.
 */
@Composable
fun MediaPickerSection(
    items: List<LocalMedia>,
    onAdd: (List<LocalMedia>) -> Unit,
    onRemove: (LocalMedia) -> Unit,
    onStageChange: (LocalMedia, MediaStage) -> Unit,
    modifier: Modifier = Modifier,
    /** Etapas de archivos que el trabajo ya tiene en el servidor, para sugerir bien la siguiente. */
    knownStages: List<MediaStage> = emptyList()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun importUris(uris: List<Uri>) {
        scope.launch {
            importing = true
            error = null
            val added = mutableListOf<LocalMedia>()
            var firstError: String? = null
            for (uri in uris) {
                val stage = suggestedStage(items.map { it.stage } + knownStages + added.map { it.stage })
                val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
                val result = if (isVideo) MediaFiles.importVideo(context, uri, stage) else MediaFiles.importPhoto(context, uri, stage)
                result
                    .onSuccess { added += it }
                    .onFailure { if (firstError == null) firstError = it.message ?: "No se pudo agregar el archivo." }
            }
            if (added.isNotEmpty()) onAdd(added)
            error = firstError
            importing = false
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) importUris(listOf(uri)) else error = "No se pudo tomar la foto."
    }
    val videoLauncher = rememberLauncherForActivityResult(CaptureVideoWithLimits()) { success ->
        val uri = pendingVideoUri
        pendingVideoUri = null
        if (success && uri != null) importUris(listOf(uri)) else if (!success) error = "No se grabó el video."
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) importUris(uris)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pendingAfterPermission
        pendingAfterPermission = null
        if (granted) action?.invoke() else error = "Permite el acceso a la cámara para tomar la evidencia."
    }

    fun withCamera(action: () -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            pendingAfterPermission = action
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchSafely(block: () -> Unit) {
        try {
            block()
        } catch (_: ActivityNotFoundException) {
            error = "No hay una aplicación de cámara disponible en este teléfono."
        } catch (_: SecurityException) {
            error = "No fue posible abrir la cámara. Revisa el permiso e inténtalo de nuevo."
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    withCamera {
                        val file = createTempCameraFile(context)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        pendingCameraUri = uri
                        launchSafely { photoLauncher.launch(uri) }
                    }
                },
                enabled = !importing,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Foto", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(
                onClick = {
                    withCamera {
                        val file = File(File(context.cacheDir, "camera").apply { mkdirs() }, "video_${System.currentTimeMillis()}.mp4")
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        pendingVideoUri = uri
                        launchSafely { videoLauncher.launch(uri) }
                    }
                },
                enabled = !importing,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Video", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(
                onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                enabled = !importing,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Galería", modifier = Modifier.padding(start = 6.dp))
            }
        }

        if (importing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Preparando archivos…", style = MaterialTheme.typography.bodySmall)
            }
        }

        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }

        if (items.isEmpty()) {
            Text(
                "Agrega todas las fotos y videos que quieras. Se suben solos en segundo plano y los puedes ver después en la cita o en la garantía.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                items.forEach { media ->
                    LocalMediaTile(media = media, onRemove = { onRemove(media) }, onStageClick = { onStageChange(media, media.stage.next()) })
                }
            }
            Text(
                "Toca la etiqueta de cada archivo para cambiarla (Antes, Durante, Después, Otra). Las fotos Antes y Después alimentan la galería pública.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocalMediaTile(media: LocalMedia, onRemove: () -> Unit, onStageClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(112.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (media.kind == MediaKind.PHOTO) {
            AsyncImage(
                model = media.file,
                contentDescription = "Foto",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Box(modifier = Modifier.matchParentSize().background(Color(0xFF1E293B)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PlayCircle, contentDescription = "Video", tint = Color.White, modifier = Modifier.size(38.dp))
                    Text(
                        listOfNotNull(
                            media.durationSeconds?.let { formatDuration(it) },
                            formatSize(media.sizeBytes)
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }

        IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).size(30.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = Color.White, modifier = Modifier.background(Color(0x99000000), RoundedCornerShape(50)).padding(3.dp).size(16.dp))
        }

        Surface(
            onClick = onStageClick,
            shape = RoundedCornerShape(topEnd = 10.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text(
                media.stage.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

internal fun formatDuration(seconds: Int): String = "%d:%02d".format(Locale.US, seconds / 60, seconds % 60)

internal fun formatSize(bytes: Long): String =
    if (bytes >= 1024 * 1024) "%.1f MB".format(Locale.US, bytes / (1024.0 * 1024.0)) else "${bytes / 1024} KB"
