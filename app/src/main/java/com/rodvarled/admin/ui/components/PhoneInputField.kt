package com.rodvarled.admin.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rodvarled.admin.core.util.CountryCode
import com.rodvarled.admin.core.util.PhoneUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneInputField(
    phone: String,
    onPhoneChange: (String) -> Unit,
    label: String = "Teléfono",
    placeholder: String = "10 dígitos",
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Desglosar el teléfono actual
    val parsed = remember(phone) { PhoneUtils.parsePhoneNumber(phone) }
    var selectedCode by remember(parsed.first) { mutableStateOf(parsed.first) }
    var localNumber by remember(parsed.second) { mutableStateOf(parsed.second) }

    var showBottomSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    OutlinedTextField(
        value = localNumber,
        onValueChange = { newText ->
            // Aceptamos dígitos y caracteres comunes de teléfono
            val filtered = newText.filter { it.isDigit() || it == '-' || it == ' ' }
            localNumber = filtered
            val full = PhoneUtils.formatFullPhone(selectedCode, filtered)
            onPhoneChange(full)
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .padding(start = 8.dp, end = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled) {
                        searchQuery = ""
                        showBottomSheet = true
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = PhoneUtils.getFlagForCode(selectedCode),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedCode,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Cambiar lada",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        trailingIcon = {
            if (localNumber.isNotEmpty() && enabled) {
                IconButton(onClick = {
                    localNumber = ""
                    onPhoneChange("")
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Borrar número", modifier = Modifier.size(18.dp))
                }
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    )

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seleccionar lada / país",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showBottomSheet = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar país o ingresar lada (+1, +52, +44...)") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )

                val cleanQuery = searchQuery.trim()
                val isCustomLadaQuery = cleanQuery.isNotBlank() && cleanQuery.all { it.isDigit() || it == '+' }
                val formattedCustomCode = when {
                    cleanQuery.startsWith("+") -> cleanQuery
                    isCustomLadaQuery -> "+$cleanQuery"
                    else -> null
                }

                // Opción directa para lada personalizada ingresada por el usuario
                if (formattedCustomCode != null && formattedCustomCode.length >= 2) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable {
                                selectedCode = formattedCustomCode
                                val full = PhoneUtils.formatFullPhone(selectedCode, localNumber)
                                onPhoneChange(full)
                                showBottomSheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Usar lada personalizada: $formattedCustomCode",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Pulsa aquí para aplicar esta lada internacional",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                val filteredCountries = remember(searchQuery) {
                    if (searchQuery.isBlank()) {
                        PhoneUtils.POPULAR_COUNTRIES
                    } else {
                        val q = searchQuery.lowercase().trim()
                        PhoneUtils.POPULAR_COUNTRIES.filter {
                            it.countryName.lowercase().contains(q) || it.code.lowercase().contains(q)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    items(filteredCountries, key = { "${it.code}_${it.countryName}" }) { country ->
                        CountryRow(
                            country = country,
                            isSelected = country.code == selectedCode,
                            onSelect = {
                                selectedCode = country.code
                                val full = PhoneUtils.formatFullPhone(selectedCode, localNumber)
                                onPhoneChange(full)
                                showBottomSheet = false
                            }
                        )
                    }

                    if (filteredCountries.isEmpty() && formattedCustomCode == null) {
                        item {
                            Text(
                                text = "No se encontraron coincidencias. Puedes escribir el código con '+' (ej. +44) para asignarlo de forma personalizada.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryRow(
    country: CountryCode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = country.flagEmoji, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = country.countryName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = country.code,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}
