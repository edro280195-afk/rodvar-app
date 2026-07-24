package com.rodvarled.admin.ui.appointments

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.components.PhotoCaptureField
import com.rodvarled.admin.ui.components.SignaturePad
import com.rodvarled.admin.ui.components.rememberSignaturePadState
import com.rodvarled.admin.ui.navigation.Screen

private val STEP_TITLES = listOf("Vehículo y pago", "Fotos (opcional)", "Firma del cliente")

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
                title = { Text("Completar instalación") },
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
                        0 -> VehicleAndPaymentStep(uiState, viewModel)
                        1 -> PhotosStep(uiState, viewModel)
                        else -> SignatureStep(uiState, signatureState, viewModel)
                    }
                    Spacer(Modifier.height(90.dp))
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                if (uiState.error != null) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                }
                if (step < 2) {
                    Button(onClick = { step++ }, modifier = Modifier.fillMaxWidth()) {
                        Text("Continuar")
                    }
                } else {
                    Button(
                        onClick = { viewModel.submit(signatureState.exportPngBase64()) },
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isSubmitting) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                        else Text("Completar instalación y generar garantía")
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
                        .height(4.dp)
                        .background(
                            if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        Text(
            "Paso ${step + 1} de 3 · ${STEP_TITLES[step]}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun VehicleAndPaymentStep(uiState: CompleteInstallationUiState, viewModel: CompleteInstallationViewModel) {
    OutlinedTextField(
        value = uiState.vehiclePlate,
        onValueChange = viewModel::onPlateChange,
        label = { Text("Placas (opcional)") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.vehicleColor,
        onValueChange = viewModel::onColorChange,
        label = { Text("Color (opcional)") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    )
    DropdownSelector(
        label = "Método de pago",
        items = PAYMENT_METHODS,
        selected = uiState.paymentMethod,
        labelOf = { it },
        onSelect = viewModel::onPaymentMethodChange,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.technicianNotes,
        onValueChange = viewModel::onNotesChange,
        label = { Text("Notas del técnico (opcional)") },
        minLines = 2,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PhotosStep(uiState: CompleteInstallationUiState, viewModel: CompleteInstallationViewModel) {
    Text(
        "Fotos antes / después",
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        "Importante pero no obligatorio. Puedes continuar sin subir nada.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        PhotoCaptureField(label = "Antes", base64Value = uiState.beforePhotoBase64, onValueChange = viewModel::onBeforePhotoCaptured)
        PhotoCaptureField(label = "Después", base64Value = uiState.afterPhotoBase64, onValueChange = viewModel::onAfterPhotoCaptured)
    }
}

@Composable
private fun SignatureStep(
    uiState: CompleteInstallationUiState,
    signatureState: com.rodvarled.admin.ui.components.SignaturePadState,
    viewModel: CompleteInstallationViewModel
) {
    Text("Firma del cliente", style = MaterialTheme.typography.titleMedium)
    Text(
        "Este documento genera la garantía. El cliente firma para aceptar los términos.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        SignaturePad(state = signatureState, modifier = Modifier.fillMaxWidth().height(220.dp).padding(8.dp))
    }

    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = { signatureState.clear() }) {
            Text("Limpiar firma")
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = uiState.acceptedTerms, onCheckedChange = viewModel::onAcceptedTermsChange)
        Text(
            "El cliente acepta los términos de garantía de Rodvar LED.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

