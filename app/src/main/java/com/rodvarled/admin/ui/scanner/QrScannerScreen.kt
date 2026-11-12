package com.rodvarled.admin.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.rodvarled.admin.ui.navigation.Screen
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

@Composable
fun QrScannerScreen(navController: NavHostController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) cameraError = "Permite la cámara para leer el QR del producto."
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            CameraPreview(
                lifecycleOwner = lifecycleOwner,
                torchEnabled = torchEnabled,
                onError = { cameraError = it },
                onCode = { code ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("scannedInventoryUnit", code)
                    navController.popBackStack()
                }
            )
        }

        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color.White)
                }
                Text("Leer etiqueta Rodvar", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = { torchEnabled = !torchEnabled }) {
                    Icon(Icons.Filled.FlashOn, "Linterna", tint = if (torchEnabled) Color(0xFFFFD54F) else Color.White)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().weight(1f)) {
                ScannerFrame()
                Surface(color = Color.Black.copy(alpha = 0.62f), shape = RoundedCornerShape(18.dp), modifier = Modifier.padding(top = 22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                        Icon(Icons.Filled.QrCodeScanner, null, tint = Color(0xFF69B7FF), modifier = Modifier.size(22.dp))
                        Text("Alinea el QR dentro del recuadro", color = Color.White, modifier = Modifier.padding(start = 10.dp))
                    }
                }
                cameraError?.let { Text(it, color = Color(0xFFFF8A80), modifier = Modifier.padding(top = 14.dp)) }
            }
        }
    }
}

@Composable
private fun ScannerFrame() {
    val transition = rememberInfiniteTransition(label = "scanner")
    val linePosition by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanner-line"
    )
    Canvas(Modifier.size(280.dp)) {
        val stroke = 4.dp.toPx()
        val corner = 28.dp.toPx()
        val width = size.width
        val height = size.height
        val color = Color(0xFF63B3FF)
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, corner), androidx.compose.ui.geometry.Offset(0f, 0f), strokeWidth = stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(corner, 0f), strokeWidth = stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(width - corner, 0f), androidx.compose.ui.geometry.Offset(width, 0f), strokeWidth = stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(width, 0f), androidx.compose.ui.geometry.Offset(width, corner), strokeWidth = stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, height - corner), androidx.compose.ui.geometry.Offset(0f, height), strokeWidth = stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, height), androidx.compose.ui.geometry.Offset(corner, height), strokeWidth = stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(width - corner, height), androidx.compose.ui.geometry.Offset(width, height), strokeWidth = stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(width, height), androidx.compose.ui.geometry.Offset(width, height - corner), strokeWidth = stroke)
        drawLine(Color.White.copy(alpha = 0.9f), androidx.compose.ui.geometry.Offset(12.dp.toPx(), height * linePosition), androidx.compose.ui.geometry.Offset(width - 12.dp.toPx(), height * linePosition), strokeWidth = 2.dp.toPx())
    }
}

@Composable
private fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    torchEnabled: Boolean,
    onError: (String) -> Unit,
    onCode: (String) -> Unit
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val delivered = remember { AtomicBoolean(false) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }

    DisposableEffect(lifecycleOwner, torchEnabled) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null || delivered.get()) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    scanner.process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { barcodes ->
                            val value = barcodes.firstOrNull()?.rawValue?.trim()
                            if (!value.isNullOrBlank() && delivered.compareAndSet(false, true)) {
                                Handler(Looper.getMainLooper()).post { onCode(value) }
                            }
                        }
                        .addOnFailureListener { onError("No se pudo analizar la imagen. Inténtalo nuevamente.") }
                        .addOnCompleteListener { imageProxy.close() }
                }
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                camera.cameraControl.enableTorch(torchEnabled)
            }.onFailure { onError("No se pudo iniciar la cámara. Revisa el permiso e inténtalo nuevamente.") }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}
