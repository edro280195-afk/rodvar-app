package com.rodvarled.admin.ui.appointments

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.components.PhotoCaptureField
import com.rodvarled.admin.ui.components.SignaturePad
import com.rodvarled.admin.ui.components.rememberSignaturePadState
import com.rodvarled.admin.ui.navigation.Screen
import com.rodvarled.admin.ui.theme.RodvarOutline
import com.rodvarled.admin.ui.theme.RodvarPrimary
import com.rodvarled.admin.ui.theme.RodvarPrimaryContainer
import com.rodvarled.admin.ui.theme.RodvarSurface
import com.rodvarled.admin.ui.theme.RodvarTextSecondary

private val STEP_TITLES = listOf("Vehículo y Pago", "Evidencia Fotográfica", "Firma y Garantía")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteInstallationScreen(
    appointmentId: Int,
    navController: NavHostController,
    viewModel: CompleteInstallationViewModel = hiltViewModel()
        ) {
    val uiState by viewModel.uiState.collectAsState()
    val signatureState = rememberSignaturePadState()
    var step by remember { mutableIntStateOf(0) }
    val scannedCodeState = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("scannedInventoryUnit", null)
        ?.collectAsState()

    LaunchedEffect(scannedCodeState?.value) {
        val code = scannedCodeState?.value
        if (!code.isNullOrBlank()) {
            navController.currentBackStackEntry?.savedStateHandle?.set("scannedInventoryUnit", null)
            viewModel.onInventoryUnitScanned(code)
        }
    }

    LaunchedEffect(uiState.completedWarrantyId) {
        val warrantyId = uiState.completedWarrantyId
        if (warrantyId != null) {
            if (warrantyId > 0) {
                navController.navigate(Screen.WarrantyDetail.of(warrantyId)) { popUpTo(Screen.AppointmentsList.route) }
            } else {
                navController.popBackStack(Screen.AppointmentsList.route, inclusive = false)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Completar Instalación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (step == 0) navController.popBackStack() else step-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            StepProgress(step = step, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = { (fadeIn(tween(250)) togetherWith fadeOut(tween(150))) },
                modifier = Modifier.weight(1f),
                label = "wizardStep"
            ) { currentStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (currentStep) {
                        0 -> VehicleAndPaymentStep(uiState, viewModel, navController)
                        1 -> PhotosStep(uiState, viewModel)
                        else -> SignatureStep(uiState, signatureState, viewModel)
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                if (uiState.error != null) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                }
                if (step < 2) {
                    Button(
                        onClick = { step++ },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Continuar", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { viewModel.submit(signatureState.exportPngBase64()) },
                        enabled = !uiState.isSubmitting,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (uiState.isSubmitting) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                        else Text("Completar Instalación y Generar Garantía", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepProgress(step: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            STEP_TITLES.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(
                            if (index <= step) RodvarPrimary else RodvarOutline,
                            RoundedCornerShape(3.dp)
                        )
                )
            }
        }
        Text(
            "Paso ${step + 1} de 3 · ${STEP_TITLES[step]}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = RodvarTextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun VehicleAndPaymentStep(uiState: CompleteInstallationUiState, viewModel: CompleteInstallationViewModel, navController: NavHostController) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
        modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.extraLarge)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(
                value = uiState.vehiclePlate,
                onValueChange = viewModel::onPlateChange,
                label = { Text("Placas del Vehículo (opcional)") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Control por QR", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        if (uiState.scannedUnits.isEmpty()) "Escanea las cajas que se van a instalar para descontar piezas exactas."
                        else "${uiState.scannedUnits.size} unidad(es) seleccionada(s): ${uiState.scannedUnits.joinToString { it.code }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = RodvarTextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    TextButton(onClick = { navController.navigate(Screen.QrScanner.route) }) {
                        Text("Escanear etiqueta QR")
                    }
                }
            }
            OutlinedTextField(
                value = uiState.vehicleColor,
                onValueChange = viewModel::onColorChange,
                label = { Text("Color del Vehículo (opcional)") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            DropdownSelector(
                label = "Método de Pago",
                items = PAYMENT_METHODS,
                selected = uiState.paymentMethod,
                labelOf = { it },
                onSelect = viewModel::onPaymentMethodChange,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.technicianNotes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notas del Técnico / Instalador (opcional)") },
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PhotosStep(uiState: CompleteInstallationUiState, viewModel: CompleteInstallationViewModel) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
        modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.extraLarge)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Fotos de Evidencia (Antes / Después)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Toma o selecciona fotos para respaldar la calidad de la instalación LED.",
                style = MaterialTheme.typography.bodySmall,
                color = RodvarTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PhotoCaptureField(label = "Antes (Original)", base64Value = uiState.beforePhotoBase64, onValueChange = viewModel::onBeforePhotoCaptured)
                PhotoCaptureField(label = "Después (LED)", base64Value = uiState.afterPhotoBase64, onValueChange = viewModel::onAfterPhotoCaptured)
            }
        }
    }
}

@Composable
private fun SignatureStep(
    uiState: CompleteInstallationUiState,
    signatureState: com.rodvarled.admin.ui.components.SignaturePadState,
    viewModel: CompleteInstallationViewModel
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
        modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.extraLarge)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Firma del Cliente", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(
                "El cliente firma en el recuadro para validar la conformidad e iniciar la garantía.",
                style = MaterialTheme.typography.bodySmall,
                color = RodvarTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = RodvarSurface,
                modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.medium)
            ) {
                SignaturePad(state = signatureState, modifier = Modifier.fillMaxWidth().height(200.dp).padding(8.dp))
            }

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                TextButton(onClick = { signatureState.clear() }) {
                    Text("Limpiar Firma", color = RodvarPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Checkbox(checked = uiState.acceptedTerms, onCheckedChange = viewModel::onAcceptedTermsChange)
                Text(
                    "El cliente acepta los términos y condiciones de la Garantía Rodvar LED.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RodvarTextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
