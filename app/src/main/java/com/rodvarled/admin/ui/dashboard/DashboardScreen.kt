package com.rodvarled.admin.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.appointments.AppointmentCard
import com.rodvarled.admin.ui.components.EmptyState
import com.rodvarled.admin.ui.navigation.Screen
import com.rodvarled.admin.ui.theme.RodvarAmber
import com.rodvarled.admin.ui.theme.RodvarBlue
import com.rodvarled.admin.ui.theme.RodvarRed
import com.rodvarled.admin.ui.theme.staggeredEnter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController, viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val pullState = rememberPullToRefreshState()
    val today = remember { LocalDate.now() }

    Scaffold { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::refresh,
            state = pullState,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    AnimatedVisibility(visible = true, enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { -it / 4 }) {
                        Column {
                            val dayLabel = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "MX")).replaceFirstChar { it.uppercase() }
                            Text(dayLabel + " " + today.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "MX"))),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Hola, ${uiState.userName.ifBlank { "Rodvar" }}",
                                style = MaterialTheme.typography.displaySmall,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                item {
                    AnimatedVisibility(visible = true, enter = staggeredEnter(index = 1)) {
                        DashboardStatsStrip(uiState)
                    }
                }

                item {
                    Text(
                        "Citas de hoy",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (uiState.todayAppointments.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Sin citas para hoy",
                            subtitle = "Las citas agendadas para hoy aparecerán aquí.",
                            icon = Icons.Filled.EventAvailable,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        )
                    }
                } else {
                    itemsIndexed(uiState.todayAppointments, key = { _, item -> item.id }) { index, appointment ->
                        AnimatedVisibility(
                            visible = true,
                            enter = staggeredEnter(index = index, startDelayMs = 100)
                        ) {
                            AppointmentCard(
                                appointment = appointment,
                                onClick = { navController.navigate(Screen.AppointmentDetail.of(appointment.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class StatSegment(val label: String, val value: Int, val icon: ImageVector, val accent: androidx.compose.ui.graphics.Color)

@Composable
private fun DashboardStatsStrip(uiState: DashboardUiState) {
    val segments = listOf(
        StatSegment("Pendientes", uiState.pendingCount, Icons.Filled.HourglassEmpty, RodvarAmber),
        StatSegment("Confirmadas", uiState.confirmedCount, Icons.Filled.CalendarMonth, RodvarBlue),
        StatSegment("Stock bajo", uiState.lowStockCount, Icons.Filled.Warning, RodvarRed),
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp)) {
            segments.forEachIndexed { index, segment ->
                if (index > 0) {
                    VerticalDivider(modifier = Modifier.height(40.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(segment.icon, contentDescription = null, tint = segment.accent, modifier = Modifier.height(18.dp))
                    Text(
                        segment.value.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        segment.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
