package com.rodvarled.admin.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.rodvarled.admin.core.util.base64ToBitmap
import com.rodvarled.admin.core.util.createTempCameraFile
import com.rodvarled.admin.core.util.toCompressedBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Selector de foto por cámara o galería. Comprime y sube como base64 JPEG. */
@Composable
fun PhotoCaptureField(
    label: String,
    base64Value: String?,
    onValueChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf<String?>(null) }

    fun processImage(uri: Uri) {
        if (isProcessing) return
        isProcessing = true
        scope.launch(Dispatchers.IO) {
            val encoded = uri.toCompressedBase64(context)
            withContext(Dispatchers.Main.immediate) {
                isProcessing = false
                if (encoded != null) onValueChange(encoded)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingCameraUri?.let(::processImage)
        } else {
            captureError = "No se pudo tomar la foto. Inténtalo nuevamente."
        }
        pendingCameraUri = null
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val uri = pendingCameraUri
        if (isGranted && uri != null) {
            captureError = null
            try {
                cameraLauncher.launch(uri)
            } catch (_: ActivityNotFoundException) {
                pendingCameraUri = null
                captureError = "No hay una aplicación de cámara disponible en este dispositivo."
            } catch (_: SecurityException) {
                pendingCameraUri = null
                captureError = "No fue posible abrir la cámara. Revisa el permiso e inténtalo nuevamente."
            }
        } else {
            pendingCameraUri = null
            captureError = "Permite el acceso a la cámara para tomar la evidencia."
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::processImage)
    }

    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = base64Value) {
        value = withContext(Dispatchers.Default) { base64Value?.base64ToBitmap() }
    }

    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(120.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .then(
                    if (base64Value == null)
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    else Modifier
                )
                .clickable { if (base64Value == null && !isProcessing) showMenu = true }
        ) {
            val loadedBitmap = bitmap
            if (loadedBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = loadedBitmap.asImageBitmap(),
                    contentDescription = label,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    IconButton(onClick = { onValueChange(null) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isProcessing) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Procesando", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Agregar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Tomar foto") },
                    leadingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        captureError = null
                        try {
                            val file = createTempCameraFile(context)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            pendingCameraUri = uri

                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        } catch (_: ActivityNotFoundException) {
                            pendingCameraUri = null
                            captureError = "No hay una aplicación de cámara disponible en este dispositivo."
                        } catch (_: IllegalArgumentException) {
                            pendingCameraUri = null
                            captureError = "No se pudo preparar el archivo de la foto. Inténtalo nuevamente."
                        } catch (_: SecurityException) {
                            pendingCameraUri = null
                            captureError = "No fue posible abrir la cámara. Revisa el permiso e inténtalo nuevamente."
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Elegir de galería") },
                    leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }
        }

        captureError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
