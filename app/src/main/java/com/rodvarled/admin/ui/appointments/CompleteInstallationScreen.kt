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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.ChargeLinesEditor
import com.rodvarled.admin.ui.components.ChargeSummaryCard
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.components.ProductPickerSheet
import com.rodvarled.admin.ui.media.MediaPickerSection
import com.rodvarled.admin.ui.components.SignaturePad
import com.rodvarled.admin.ui.components.rememberSignaturePadState
import com.rodvarled.admin.ui.navigation.Screen
import com.rodvarled.admin.ui.theme.RodvarOutline
import com.rodvarled.admin.ui.theme.RodvarPrimary
import com.rodvarled.admin.ui.theme.RodvarPrimaryContainer
import com.rodvarled.admin.ui.theme.RodvarSurface
import com.rodvarled.admin.ui.theme.RodvarTextSecondary

private val INSTALLATION_STEPS = listOf("Cobro", "Fotos y videos", "Firma y Garantía")

// Una revisión sin productos no lleva garantía: no hay nada que firmar.
private val VISIT_ONLY_STEPS = listOf("Cobro", "Fotos y videos")

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
    var showProductSheet by remember { mutableStateOf(false) }
    val stepTitles = if (uiState.isVisitOnly) VISIT_ONLY_STEPS else INSTALLATION_STEPS
    val lastStep = stepTitles.lastIndex
    // Si al agregar un producto desaparece el paso de firma estando en él, se regresa al último válido.
    LaunchedEffect(lastStep) { if (step > lastStep) step = lastStep }
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
                title = { Text(if (uiState.isVisitOnly) "Completar Revisión" else "Completar Instalación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (step == 0) navController.popBackStack() else step-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }
            if (!uiState.itemsLoaded) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        uiState.error ?: "No se pudo cargar la cita.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = viewModel::load, modifier = Modifier.padding(top = 16.dp)) { Text("Reintentar") }
                }
                return@Column
            }

            StepProgress(step = step, titles = stepTitles, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp))

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
                        0 -> ChargeStep(uiState, viewModel, navController, onAddProduct = { showProductSheet = true })
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
                if (step < lastStep) {
                    Button(
                        onClick = { step++ },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Continuar", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { viewModel.submit(if (uiState.isVisitOnly) null else signatureState.exportPngBase64()) },
                        enabled = !uiState.isSubmitting,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (uiState.isSubmitting) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                        else Text(
                            if (uiState.isVisitOnly) "Completar Revisión" else "Completar Instalación y Generar Garantía",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showProductSheet) {
        ProductPickerSheet(
            products = uiState.availableProducts,
            countOf = { productId, variantId ->
                uiState.charge.lines.filter { it.productId == productId && it.variantId == variantId }.sumOf { it.quantity }
            },
            onPick = viewModel::addProduct,
            onDismiss = { showProductSheet = false }
        )
    }
}

@Composable
private fun StepProgress(step: Int, titles: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            titles.forEachIndexed { index, _ ->
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
            "Paso ${step + 1} de ${titles.size} · ${titles[step]}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = RodvarTextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ChargeStep(
    uiState: CompleteInstallationUiState,
    viewModel: CompleteInstallationViewModel,
    navController: NavHostController,
    onAddProduct: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
        modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.extraLarge)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (uiState.isVisitOnly) "Revisión a domicilio" else "Lo que se instaló",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        if (uiState.charge.lines.isEmpty()) "Sin productos"
                        else "${uiState.charge.lines.sumOf { it.quantity }} pieza(s). Cambia precios o agrega otro par.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RodvarTextSecondary
                    )
                }
                TextButton(onClick = { navController.navigate(Screen.QrScanner.route) }) { Text("Escanear QR") }
                TextButton(onClick = onAddProduct) { Text("+ Agregar") }
            }

            ChargeLinesEditor(
                lines = uiState.charge.lines,
                onUnitPriceChange = viewModel::onUnitPriceChange,
                onQuantityChange = viewModel::onQuantityChange,
                onRemove = viewModel::removeLine
            )

            ChargeSummaryCard(
                state = uiState.charge,
                homeVisitFee = uiState.homeVisitFee,
                isRevision = uiState.isVisitOnly,
                onTotalChange = viewModel::onTotalChange,
                onVisitChargedChange = viewModel::onVisitChargedChange,
                onVisitCreditChange = viewModel::onVisitCreditChange
            )

            DropdownSelector(
                label = "Método de Pago",
                items = PAYMENT_METHODS,
                selected = uiState.paymentMethod,
                labelOf = { it },
                onSelect = viewModel::onPaymentMethodChange,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.vehiclePlate,
                    onValueChange = viewModel::onPlateChange,
                    label = { Text("Placas (opcional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.vehicleColor,
                    onValueChange = viewModel::onColorChange,
                    label = { Text("Color (opcional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                )
            }
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
                "Fotos y videos del trabajo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Opcional. Antes, durante y después: todo lo que respalde la calidad de la instalación.",
                style = MaterialTheme.typography.bodySmall,
                color = RodvarTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            MediaPickerSection(
                items = uiState.media,
                onAdd = viewModel::addMedia,
                onRemove = viewModel::removeMedia,
                onStageChange = viewModel::changeMediaStage
            )
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
