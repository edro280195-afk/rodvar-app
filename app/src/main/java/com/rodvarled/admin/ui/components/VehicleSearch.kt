package com.rodvarled.admin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rodvarled.admin.core.util.toUserMessage
import com.rodvarled.admin.data.remote.dto.VehicleSearchHit
import com.rodvarled.admin.data.repository.VehiclesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VehicleSearchUiState(
    val query: String = "",
    val results: List<VehicleSearchHit> = emptyList(),
    val isSearching: Boolean = false,
    val selected: VehicleSearchHit? = null,
    val error: String? = null
)

/**
 * Búsqueda de vehículo por texto ("civic 2018") en lugar de cuatro listas en cascada.
 * Cada ViewModel que la necesita crea una con su propio scope y expone `state`.
 */
class VehicleSearcher(
    private val scope: CoroutineScope,
    private val repository: VehiclesRepository
) {
    private val _state = MutableStateFlow(VehicleSearchUiState())
    val state: StateFlow<VehicleSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    val selectedTrimId: Int? get() = _state.value.selected?.trimId

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, error = null) }
        searchJob?.cancel()

        if (query.trim().length < 2) {
            _state.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }

        searchJob = scope.launch {
            delay(300)
            _state.update { it.copy(isSearching = true) }
            repository.search(query.trim())
                .onSuccess { hits -> _state.update { it.copy(results = hits, isSearching = false) } }
                .onFailure { error -> _state.update { it.copy(results = emptyList(), isSearching = false, error = error.toUserMessage()) } }
        }
    }

    fun select(hit: VehicleSearchHit) {
        searchJob?.cancel()
        _state.value = VehicleSearchUiState(selected = hit)
    }

    /** Selecciona un vehículo que ya se conoce por su id (por ejemplo, el último de un cliente). */
    fun selectKnown(trimId: Int, label: String) = select(VehicleSearchHit(trimId = trimId, label = label))

    fun clear() {
        searchJob?.cancel()
        _state.value = VehicleSearchUiState()
    }
}

/** Caja de búsqueda de vehículo. Ya elegido, se muestra como una fila con la opción de cambiarlo. */
@Composable
fun VehicleSearchField(
    state: VehicleSearchUiState,
    onQueryChange: (String) -> Unit,
    onSelect: (VehicleSearchHit) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val selected = state.selected
        if (selected != null) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(
                        selected.label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f).padding(start = 10.dp)
                    )
                    TextButton(onClick = onClear) { Text("Cambiar") }
                }
            }
        } else {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("Vehículo (opcional)") },
                placeholder = { Text("Escribe marca, modelo y año: Civic 2018") },
                leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                trailingIcon = {
                    if (state.isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            if (state.results.isEmpty() && !state.isSearching && state.query.trim().length >= 2 && state.error == null) {
                Text(
                    "Sin resultados. Prueba con menos palabras (por ejemplo, solo el modelo).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            state.results.take(8).forEach { hit ->
                RodvarListRow(
                    onClick = { onSelect(hit) },
                    leading = { IconBadge(Icons.Filled.DirectionsCar, modifier = Modifier.size(36.dp)) },
                    headline = { Text(hit.label, style = MaterialTheme.typography.bodyMedium) },
                    showChevron = false
                )
            }
        }
    }
}
