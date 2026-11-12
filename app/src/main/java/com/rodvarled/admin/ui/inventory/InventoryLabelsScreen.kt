package com.rodvarled.admin.ui.inventory

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.theme.RodvarPrimary
import com.rodvarled.admin.ui.theme.RodvarPrimaryContainer
import com.rodvarled.admin.ui.theme.RodvarRedContainer
import com.rodvarled.admin.ui.theme.RodvarSecondary
import com.rodvarled.admin.ui.theme.RodvarSecondaryContainer
import com.rodvarled.admin.ui.theme.RodvarTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryLabelsScreen(
    navController: NavHostController,
    viewModel: InventoryLabelsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var bluetoothReady by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 31 || (
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                )
        )
    }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        bluetoothReady = grants.values.all { it }
        if (bluetoothReady) viewModel.scanPrinters(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Etiquetas de inventario", fontWeight = FontWeight.Bold)
                        Text("QR profesional · 50 × 50 mm", style = MaterialTheme.typography.labelSmall, color = RodvarTextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        bottomBar = {
            if (state.previews.isNotEmpty()) {
                Surface(shadowElevation = 12.dp, tonalElevation = 3.dp) {
                    Column {
                        if (state.isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = { viewModel.printLabels(context) },
                            enabled = !state.isBusy && state.selectedPrinter != null,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    state.isBusy -> state.busyMessage ?: "Preparando impresión…"
                                    state.selectedPrinter == null -> "Conecta una NIIMBOT para imprimir"
                                    else -> "Imprimir ${state.previews.size} etiqueta(s)"
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RodvarPrimary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = if (state.previews.isNotEmpty()) 96.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            state.product?.let { product ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = RodvarPrimaryContainer),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Inventory2, contentDescription = null, tint = RodvarPrimary)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(product.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Genera una etiqueta por caja para rastrear entradas y salidas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = RodvarTextSecondary
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("1. Configura el lote", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (product.variants.isNotEmpty()) {
                            DropdownSelector(
                                label = "Variante / socket",
                                items = listOf<Int?>(null) + product.variants.map { it.id },
                                selected = state.selectedVariantId,
                                labelOf = { id -> id?.let { selectedId -> product.variants.firstOrNull { it.id == selectedId }?.bulbTypeName } ?: "Producto base" },
                                onSelect = viewModel::onVariantChange,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = state.quantityText,
                                onValueChange = viewModel::onQuantityChange,
                                label = { Text("Etiquetas nuevas") },
                                supportingText = { Text("1 a 500") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = state.copiesText,
                                onValueChange = viewModel::onCopiesChange,
                                label = { Text("Copias c/u") },
                                supportingText = { Text("1 a 99") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = viewModel::generateLabels,
                            enabled = !state.isBusy,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.QrCode2, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (state.previews.isEmpty()) "Generar lote de etiquetas" else "Regenerar lote")
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = RodvarPrimary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("2. Impresora NIIMBOT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                state.selectedPrinter?.let { "Seleccionada: ${it.name}" } ?: "La conexión se valida justo antes de imprimir",
                                style = MaterialTheme.typography.bodySmall,
                                color = RodvarTextSecondary
                            )
                        }
                        AssistChip(
                            onClick = {},
                            label = { Text(if (state.selectedPrinter != null) "Lista" else "Sin conectar") },
                            leadingIcon = { Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (state.selectedPrinter != null) RodvarSecondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = if (state.selectedPrinter != null) RodvarSecondary else RodvarTextSecondary
                            )
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            if (bluetoothReady) viewModel.scanPrinters(context)
                            else bluetoothPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
                        },
                        enabled = !state.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.BluetoothSearching, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.printers.isEmpty()) "Buscar impresora" else "Volver a buscar")
                    }
                    state.printers.forEach { printer ->
                        val selected = state.selectedPrinter?.address == printer.address
                        Surface(
                            onClick = { viewModel.selectPrinter(printer) },
                            color = if (selected) RodvarPrimaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = if (selected) RodvarPrimary else RodvarTextSecondary)
                                Spacer(Modifier.width(10.dp))
                                Text(printer.name, Modifier.weight(1f), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                if (selected) Icon(Icons.Filled.CheckCircle, contentDescription = "Seleccionada", tint = RodvarSecondary)
                            }
                        }
                    }
                }
            }

            if (state.previews.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("3. Revisa el diseño", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${state.previews.size} QR únicos · listos para caja", style = MaterialTheme.typography.bodySmall, color = RodvarTextSecondary)
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text("384 × 384 px") },
                                leadingIcon = { Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                        HorizontalDivider()
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                bitmap = state.previews.first().asImageBitmap(),
                                contentDescription = "Vista previa de la etiqueta",
                                modifier = Modifier.size(178.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(4.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                Text("Diseño pensado para lectura rápida", fontWeight = FontWeight.Bold)
                                Text("QR grande, margen de seguridad y código visible para identificar la caja aunque la etiqueta se desgaste.", style = MaterialTheme.typography.bodySmall, color = RodvarTextSecondary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = RodvarSecondary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Alta corrección de error", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        if (state.previews.size > 1) {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.previews.drop(1).take(7).forEach { bitmap ->
                                    Image(bitmap.asImageBitmap(), "Etiqueta adicional", Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
                                }
                            }
                        }
                    }
                }
            }

            if (state.isBusy && state.previews.isEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(state.busyMessage ?: "Trabajando…", style = MaterialTheme.typography.bodySmall)
                }
            }
            state.error?.let { StatusCard(text = it, positive = false) }
            state.message?.let { StatusCard(text = it, positive = true) }
        }
    }
}

@Composable
private fun StatusCard(text: String, positive: Boolean) {
    val container = if (positive) RodvarSecondaryContainer else RodvarRedContainer
    val tint = if (positive) RodvarSecondary else MaterialTheme.colorScheme.error
    Card(colors = CardDefaults.cardColors(containerColor = container), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (positive) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber, contentDescription = null, tint = tint)
            Spacer(Modifier.width(10.dp))
            Text(text, color = tint, style = MaterialTheme.typography.bodySmall)
        }
    }
}
