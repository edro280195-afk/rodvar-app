package com.rodvarled.admin.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rodvarled.admin.data.remote.dto.PricingReportLine
import com.rodvarled.admin.ui.components.ErrorState
import com.rodvarled.admin.ui.theme.RodvarAmber
import com.rodvarled.admin.ui.theme.RodvarEmerald
import com.rodvarled.admin.ui.theme.RodvarPrimary
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavHostController,
    viewModel: ReportsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::load, enabled = !state.isLoading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar reporte")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading && state.report == null ->
                CircularProgressIndicator(Modifier.padding(padding).fillMaxSize().wrapContentSize(Alignment.Center))
            state.error != null && state.report == null ->
                ErrorState(state.error!!, onRetry = viewModel::load, modifier = Modifier.padding(padding).fillMaxSize())
            else -> {
                val report = state.report ?: return@Scaffold
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(7, 30, 90).forEach { days ->
                                FilterChip(
                                    selected = state.selectedDays == days,
                                    onClick = { viewModel.selectRange(days) },
                                    label = { Text("${days}d") }
                                )
                            }
                        }
                    }
                    item {
                        Text(
                            "Ventas registradas en ${state.selectedDays} días",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            SummaryCard("Ingreso neto", money(report.netAmount), RodvarPrimary, Modifier.weight(1f))
                            SummaryCard("Utilidad", money(report.netProfitAmount), RodvarEmerald, Modifier.weight(1f))
                            SummaryCard("Trabajos", report.installationCount.toString(), RodvarEmerald, Modifier.weight(1f))
                        }
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Lectura rápida", fontWeight = FontWeight.Bold)
                                Text("Precio de lista: ${money(report.grossAmount)}")
                                Text("Descuentos otorgados: ${money(report.discountAmount)}", color = RodvarAmber)
                                Text("Ingreso neto: ${money(report.netAmount)}", color = RodvarEmerald, fontWeight = FontWeight.Bold)
                                Text("Inversión en focos: ${money(report.costAmount)}")
                                Text("Utilidad bruta: ${money(report.profitAmount)}", color = RodvarEmerald, fontWeight = FontWeight.Bold)
                                Text("Garantías aplicadas: ${report.warrantyClaimCount} · ${money(report.warrantyClaimCostAmount)}", color = MaterialTheme.colorScheme.error)
                                Text("Utilidad después de garantías: ${money(report.netProfitAmount)}", color = RodvarEmerald, fontWeight = FontWeight.Bold)
                                if (report.installationCount > report.installationsWithCost) {
                                    Text("Aviso: ${report.installationCount - report.installationsWithCost} instalación(es) todavía no tienen costo capturado.", style = MaterialTheme.typography.bodySmall, color = RodvarAmber)
                                }
                            }
                        }
                    }
                    if (report.byProduct.isNotEmpty()) {
                        item { SectionTitle("Productos que más vendiste") }
                        items(report.byProduct.take(8), key = { "product-${it.name}" }) { line -> ReportLine(line) }
                    }
                    if (report.byRule.isNotEmpty()) {
                        item { SectionTitle("Descuentos aplicados") }
                        items(report.byRule.take(8), key = { "rule-${it.name}" }) { line -> ReportLine(line, showDiscount = true) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, color: Color, modifier: Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = color)
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun ReportLine(line: PricingReportLine, showDiscount: Boolean = false) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(line.name, fontWeight = FontWeight.Bold)
                Text("${line.quantity} pieza(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(line.netAmount), fontWeight = FontWeight.Bold)
                if (showDiscount && line.discountAmount > 0) {
                    Text("-${money(line.discountAmount)}", style = MaterialTheme.typography.bodySmall, color = RodvarAmber)
                }
            }
        }
    }
}

private fun money(amount: Double): String = NumberFormat.getCurrencyInstance(Locale("es", "MX")).format(amount)
