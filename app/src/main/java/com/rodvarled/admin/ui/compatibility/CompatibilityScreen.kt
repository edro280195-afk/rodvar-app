package com.rodvarled.admin.ui.compatibility

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.components.DropdownSelector
import com.rodvarled.admin.ui.components.SectionHeader
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
        topBar = { TopAppBar(title = { Text("Compatibilidad de focos") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DropdownSelector("Marca", uiState.makes, uiState.selectedMake, { it.name }, onSelect = viewModel::onMakeSelected, modifier = Modifier.fillMaxWidth())
            DropdownSelector("Modelo", uiState.models, uiState.selectedModel, { it.name }, enabled = uiState.selectedMake != null, onSelect = viewModel::onModelSelected, modifier = Modifier.fillMaxWidth())
            DropdownSelector("Año", uiState.years, uiState.selectedYear, { it.year.toString() }, enabled = uiState.selectedModel != null, onSelect = viewModel::onYearSelected, modifier = Modifier.fillMaxWidth())
            DropdownSelector("Versión", uiState.trims, uiState.selectedTrim, { it.name }, enabled = uiState.selectedYear != null, onSelect = viewModel::onTrimSelected, modifier = Modifier.fillMaxWidth())

            if (uiState.selectedTrim != null) {
                SectionHeader("Focos por posición")

                BULB_POSITIONS.forEachIndexed { index, position ->
                    val existing = uiState.results.find { it.positionName == position }
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Text(position, style = MaterialTheme.typography.bodyMedium)
                        DropdownSelector(
                            label = "Tipo de foco",
                            items = uiState.bulbTypes,
                            selected = existing?.bulbTypeId?.let { id -> uiState.bulbTypes.find { it.id == id } },
                            labelOf = { it.name },
                            onSelect = { viewModel.saveMapping(position, it) },
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
