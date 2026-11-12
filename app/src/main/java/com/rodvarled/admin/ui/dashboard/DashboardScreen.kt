package com.rodvarled.admin.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rodvarled.admin.ui.appointments.AppointmentCard
import com.rodvarled.admin.ui.components.EmptyState
import com.rodvarled.admin.ui.navigation.Screen
import com.rodvarled.admin.ui.theme.RodvarAmber
import com.rodvarled.admin.ui.theme.RodvarBlue
import com.rodvarled.admin.ui.theme.RodvarEmerald
import com.rodvarled.admin.ui.theme.RodvarOutline
import com.rodvarled.admin.ui.theme.RodvarPrimary
import com.rodvarled.admin.ui.theme.RodvarPrimaryContainer
import com.rodvarled.admin.ui.theme.RodvarRed
import com.rodvarled.admin.ui.theme.RodvarSurface
import com.rodvarled.admin.ui.theme.RodvarTextPrimary
import com.rodvarled.admin.ui.theme.RodvarTextSecondary
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
                contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Hero Card Dribbble
                item {
                    AnimatedVisibility(visible = true, enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { -it / 4 }) {
                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(containerColor = RodvarPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("RODVAR LED", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        val dayLabel = today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "MX")).replaceFirstChar { it.uppercase() }
                                        Text(
                                            "$dayLabel ${today.format(DateTimeFormatter.ofPattern("d MMM", Locale("es", "MX")))}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    "¡Hola, ${uiState.userName.ifBlank { "Rodvar" }}! 👋",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    "Gestor de Citas, Instalaciones y Garantías",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Tarjetas de Métricas de 3 Bloques
                item {
                    AnimatedVisibility(visible = true, enter = staggeredEnter(index = 1)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatCard(
                                title = "Pendientes",
                                count = uiState.pendingCount,
                                icon = Icons.Filled.HourglassEmpty,
                                accentColor = RodvarAmber,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Confirmadas",
                                count = uiState.confirmedCount,
                                icon = Icons.Filled.CalendarMonth,
                                accentColor = RodvarPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Stock Bajo",
                                count = uiState.lowStockCount,
                                icon = Icons.Filled.Warning,
                                accentColor = RodvarRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Accesos Rápidos Modulares
                item {
                    Column {
                        Text(
                            "Acciones Rápidas",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = RodvarTextPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            QuickActionCard(
                                label = "Nueva Cita",
                                icon = Icons.Filled.Add,
                                accentColor = RodvarPrimary,
                                onClick = { navController.navigate(Screen.AppointmentForm.create()) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionCard(
                                label = "Instalación",
                                icon = Icons.Filled.Build,
                                accentColor = RodvarEmerald,
                                onClick = { navController.navigate(Screen.DirectInstallation.route) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionCard(
                                label = "Compatibilidad",
                                icon = Icons.Filled.Search,
                                accentColor = RodvarAmber,
                                onClick = { navController.navigate(Screen.Compatibility.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Título de Citas de Hoy
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Citas Agendadas Hoy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = RodvarTextPrimary
                        )

                        Surface(
                            shape = CircleShape,
                            color = RodvarPrimaryContainer
                        ) {
                            Text(
                                "${uiState.todayAppointments.size} activas",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = RodvarPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (uiState.todayAppointments.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Sin citas para hoy",
                            subtitle = "Las citas programadas para la fecha actual aparecerán listadas aquí con notificaciones FCM automáticas.",
                            icon = Icons.Filled.EventAvailable,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
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

@Composable
private fun StatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(accentColor.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = RodvarTextPrimary
            )
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = RodvarTextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(accentColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = RodvarTextPrimary,
                maxLines = 1
            )
        }
    }
}
