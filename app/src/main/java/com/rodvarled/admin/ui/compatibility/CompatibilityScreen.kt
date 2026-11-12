package com.rodvarled.admin.ui.compatibility

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.components.SectionHeader
import com.rodvarled.admin.ui.theme.RodvarAmber
import com.rodvarled.admin.ui.theme.RodvarEmerald
import com.rodvarled.admin.ui.theme.RodvarOutline
import com.rodvarled.admin.ui.theme.RodvarPrimary
import com.rodvarled.admin.ui.theme.RodvarSurface
import com.rodvarled.admin.ui.theme.RodvarTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilityScreen(navController: NavHostController, viewModel: CompatibilityViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) }; viewModel.consumeSnackbar() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Buscador de Compatibilidad", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta de Selector de Vehículo
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.extraLarge)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).background(RodvarPrimary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = RodvarPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Selecciona el Vehículo", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    DropdownSelector("Marca", uiState.makes, uiState.selectedMake, { it.name }, onSelect = viewModel::onMakeSelected, modifier = Modifier.fillMaxWidth())
                    DropdownSelector("Modelo", uiState.models, uiState.selectedModel, { it.name }, enabled = uiState.selectedMake != null, onSelect = viewModel::onModelSelected, modifier = Modifier.fillMaxWidth())
                    DropdownSelector("Año", uiState.years, uiState.selectedYear, { it.year.toString() }, enabled = uiState.selectedModel != null, onSelect = viewModel::onYearSelected, modifier = Modifier.fillMaxWidth())
                    DropdownSelector("Versión / Trim", uiState.trims, uiState.selectedTrim, { it.name }, enabled = uiState.selectedYear != null, onSelect = viewModel::onTrimSelected, modifier = Modifier.fillMaxWidth())
                }
            }

            // Resultados por Posición de Luz
            if (uiState.selectedTrim != null) {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.extraLarge)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = RodvarAmber)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Configuración de Focos Requeridos", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        BULB_POSITIONS.forEach { position ->
                            val existing = uiState.results.find { it.positionName == position }
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = RodvarSurface),
                                modifier = Modifier.fillMaxWidth().border(1.dp, RodvarOutline, MaterialTheme.shapes.medium).padding(vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(position, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                        if (existing != null) {
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = RodvarEmerald.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    "Asignado",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = RodvarEmerald,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    DropdownSelector(
                                        label = "Tipo de Foco / Entrada (Socket)",
                                        items = uiState.bulbTypes,
                                        selected = existing?.bulbTypeId?.let { id -> uiState.bulbTypes.find { it.id == id } },
                                        labelOf = { it.name },
                                        onSelect = { viewModel.saveMapping(position, it) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
