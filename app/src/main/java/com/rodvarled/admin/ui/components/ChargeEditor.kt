package com.rodvarled.admin.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rodvarled.admin.core.pricing.ChargeLine
import com.rodvarled.admin.core.pricing.ChargeMath
import com.rodvarled.admin.core.pricing.ChargeState
import com.rodvarled.admin.core.pricing.listPriceFor
import com.rodvarled.admin.core.pricing.stockFor
import com.rodvarled.admin.data.remote.dto.BulbTypeVariant
import com.rodvarled.admin.data.remote.dto.ProductDetail
import com.rodvarled.admin.ui.theme.RodvarSecondary

/** "$1,200" o "$1,200.50": pesos sin decimales cuando son exactos. */
fun formatMoney(value: Double): String {
    val rounded = ChargeMath.round2(value)
    return if (rounded == kotlin.math.floor(rounded)) "$${"%,.0f".format(java.util.Locale.US, rounded)}"
    else "$${"%,.2f".format(java.util.Locale.US, rounded)}"
}

/**
 * Campo para capturar dinero sin tanto rollo: solo números y punto, con "$" fijo.
 * Mientras se escribe respeta lo que el usuario teclea; al soltarlo refleja el valor real
 * (por ejemplo, cuando el total se reparte entre los renglones).
 */
@Composable
fun MoneyField(
    value: Double,
    onValueChange: (Double) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    textStyle: TextStyle = LocalTextStyle.current
) {
    var text by remember { mutableStateOf(ChargeMath.formatInput(value)) }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(value, focused) {
        if (!focused) text = ChargeMath.formatInput(value)
    }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val clean = raw.filter { it.isDigit() || it == '.' }
            val parts = clean.split('.')
            // Un solo punto decimal y máximo dos decimales.
            val normalized = if (parts.size > 1) parts[0] + "." + parts.drop(1).joinToString("").take(2) else clean
            text = normalized
            onValueChange(ChargeMath.parseMoney(normalized) ?: 0.0)
        },
        label = { Text(label) },
        prefix = { Text("$") },
        supportingText = supportingText?.let { message -> { Text(message) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = MaterialTheme.shapes.medium,
        textStyle = textStyle,
        modifier = modifier.onFocusChanged { focused = it.isFocused }
    )
}

/** Renglones del cobro: cada uno con su cantidad y su precio editable. */
@Composable
fun ChargeLinesEditor(
    lines: List<ChargeLine>,
    onUnitPriceChange: (key: String, price: Double) -> Unit,
    onQuantityChange: (key: String, quantity: Int) -> Unit,
    onRemove: (key: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        lines.forEach { line ->
            key(line.key) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(line.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                            line.variantName?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (line.inventoryUnitCodes.isNotEmpty()) {
                                Text(
                                    if (line.hasIncompleteScan) "QR: ${line.inventoryUnitCodes.size} de ${line.quantity} piezas escaneadas"
                                    else "QR: ${line.inventoryUnitCodes.joinToString()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (line.hasIncompleteScan) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = { onRemove(line.key) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar", modifier = Modifier.size(18.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MoneyField(
                            value = line.unitPrice,
                            onValueChange = { onUnitPriceChange(line.key, it) },
                            label = "Precio c/u",
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onQuantityChange(line.key, line.quantity - 1) }, enabled = line.quantity > 1) {
                            Icon(Icons.Filled.Remove, contentDescription = "Restar")
                        }
                        Text(line.quantity.toString(), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { onQuantityChange(line.key, line.quantity + 1) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Sumar")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Lista ${formatMoney(line.listUnitPrice)} c/u",
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = if (line.isDiscounted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            formatMoney(line.amount),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }
        }
    }
}

/**
 * Resumen del cobro con el total final editable. Al cambiar el total, los precios de los
 * renglones se reparten solos. Incluye la revisión a domicilio: se cobra cuando el trabajo es
 * solo revisión, o se bonifica cuando el cliente ya la había pagado y ahora instala.
 */
@Composable
fun ChargeSummaryCard(
    state: ChargeState,
    homeVisitFee: Double,
    isRevision: Boolean,
    onTotalChange: (Double) -> Unit,
    onVisitChargedChange: (Double) -> Unit,
    onVisitCreditChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.lines.isNotEmpty()) {
                SummaryRow("Productos", formatMoney(state.linesTotal))
                if (state.discount > 0.0) {
                    SummaryRow("Rebaja sobre lista", "−${formatMoney(state.discount)}", valueColor = RodvarSecondary)
                }
            }

            if (isRevision) {
                MoneyField(
                    value = state.visitFeeCharged,
                    onValueChange = onVisitChargedChange,
                    label = "Revisión a domicilio",
                    supportingText = "Tarifa vigente: ${formatMoney(homeVisitFee)}. Pon 0 si hoy no se cobra.",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bonificar revisión ya pagada", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Resta lo que el cliente pagó por la visita.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.visitFeeCredit > 0.0,
                        onCheckedChange = { enabled -> onVisitCreditChange(if (enabled) homeVisitFee else 0.0) }
                    )
                }
                if (state.visitFeeCredit > 0.0) {
                    MoneyField(
                        value = state.visitFeeCredit,
                        onValueChange = onVisitCreditChange,
                        label = "Revisión bonificada",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            MoneyField(
                value = state.total,
                onValueChange = onTotalChange,
                label = "Total final",
                supportingText = if (state.lines.isNotEmpty()) "Cámbialo para negociar: los precios se reparten solos." else null,
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = valueColor)
    }
}

/**
 * Selector de productos con buscador. Cada toque suma una pieza; el contador del renglón
 * muestra cuántas lleva. Las variantes (H11, 9005…) aparecen como renglones aparte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPickerSheet(
    products: List<ProductDetail>,
    countOf: (productId: Int, variantId: Int?) -> Int,
    onPick: (ProductDetail, BulbTypeVariant?) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Agregar producto",
    /** false al recibir mercancía: también aparecen los productos que hoy no tienen existencia. */
    onlyInStock: Boolean = true
) {
    var query by remember { mutableStateOf("") }

    val rows = remember(products, query, onlyInStock) {
        val needle = query.trim().lowercase()
        products.flatMap { product ->
            val base = if (product.variants.isEmpty() || product.stock > 0 || !onlyInStock) listOf(product to null as BulbTypeVariant?) else emptyList()
            val variants = product.variants.map { product to it as BulbTypeVariant? }
            (base + variants).filter { (p, v) -> !onlyInStock || p.stockFor(v) > 0 }
        }.filter { (p, v) ->
            needle.isEmpty() || "${p.name} ${v?.bulbTypeName.orEmpty()}".lowercase().contains(needle)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Buscar por nombre o socket (H11, 9005…)") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            )
            if (rows.isEmpty()) {
                Text(
                    if (onlyInStock) "No hay productos con existencia que coincidan." else "No hay productos que coincidan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(rows, key = { (p, v) -> "${p.id}-${v?.id}" }) { (product, variant) ->
                    val count = countOf(product.id, variant?.id)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(product, variant) }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                listOfNotNull(
                                    variant?.bulbTypeName?.takeIf { it.isNotBlank() },
                                    formatMoney(product.listPriceFor(variant)),
                                    "stock ${product.stockFor(variant)}"
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (count > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    "×$count",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(Icons.Filled.Add, contentDescription = "Agregar", tint = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(20.dp)) { Text("Listo") }
        }
    }
}
