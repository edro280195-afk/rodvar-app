package com.rodvarled.admin.ui.appointments

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rodvarled.admin.core.util.PhoneUtils
import com.rodvarled.admin.data.remote.dto.AppointmentSummary
import com.rodvarled.admin.ui.components.StatusBadge
import com.rodvarled.admin.ui.theme.RodvarAmber
import com.rodvarled.admin.ui.theme.RodvarEmerald
import com.rodvarled.admin.ui.theme.RodvarOutline
import com.rodvarled.admin.ui.theme.RodvarPrimary
import com.rodvarled.admin.ui.theme.RodvarPrimaryContainer
import com.rodvarled.admin.ui.theme.RodvarSurface
import com.rodvarled.admin.ui.theme.RodvarTextPrimary
import com.rodvarled.admin.ui.theme.RodvarTextSecondary
import com.rodvarled.admin.ui.theme.appointmentStatusStyle
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun formatAppointmentDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "Sin fecha"
    return runCatching {
        val date = LocalDate.parse(dateStr)
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "MX")).replaceFirstChar { it.uppercase() }
        "$dayName ${date.format(DateTimeFormatter.ofPattern("d MMM", Locale("es", "MX")))}"
    }.getOrDefault(dateStr)
}

fun formatAppointmentTime(timeStr: String?): String? {
    if (timeStr.isNullOrBlank()) return null
    return runCatching { LocalTime.parse(timeStr).format(DateTimeFormatter.ofPattern("h:mm a", Locale("es", "MX"))) }
        .getOrDefault(timeStr)
}

fun dateGroupLabel(dateStr: String?): String {
    val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return "Sin fecha"
    val today = LocalDate.now()
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    return when {
        days == 0L -> "Hoy"
        days == 1L -> "Mañana"
        days == -1L -> "Ayer"
        days in 2..6 -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "MX")).replaceFirstChar { it.uppercase() }
        else -> date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "MX")))
    }
}

/**
 * Tarjeta de cita de nivel Dribbble con bloque de fecha elevado, vehículos en badge y acciones de WhatsApp/Llamada 1-tap.
 */
@Composable
fun AppointmentCard(
    appointment: AppointmentSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val style = appointmentStatusStyle(appointment.status)
    val parsedDate = runCatching { LocalDate.parse(appointment.requestedDate) }.getOrNull()

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = RodvarSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, RodvarOutline, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bloque de fecha tipo calendario Dribbble
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = RodvarPrimaryContainer,
                modifier = Modifier.size(54.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = parsedDate?.format(DateTimeFormatter.ofPattern("MMM", Locale("es", "MX")))?.uppercase() ?: "HOY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = RodvarPrimary
                    )
                    Text(
                        text = parsedDate?.dayOfMonth?.toString() ?: "•",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = RodvarPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Información central de la Cita
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        appointment.customerName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = RodvarTextPrimary,
                        maxLines = 1
                    )
                }

                val time = formatAppointmentTime(appointment.requestedTime)
                Text(
                    time ?: "Hora pendiente",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = RodvarTextSecondary
                )

                if (appointment.vehicleInfo != "No especificado") {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = RodvarOutline.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = RodvarTextSecondary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                appointment.vehicleInfo,
                                style = MaterialTheme.typography.labelSmall,
                                color = RodvarTextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Columna Derecha: Estado + Acciones WhatsApp/Llamada
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBadge(appointment.status, style)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!appointment.customerPhone.isNullOrBlank() && appointment.customerPhone != "N/A") {
                        val cleanPhone = appointment.customerPhone.replace(Regex("[^0-9+]"), "")

                        // Botón WhatsApp con fondo suave
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(RodvarEmerald.copy(alpha = 0.12f), CircleShape)
                                .clickable {
                                    val waUrl = PhoneUtils.buildWhatsAppUrl(appointment.customerPhone)
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                                    runCatching { context.startActivity(intent) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp", tint = RodvarEmerald, modifier = Modifier.size(16.dp))
                        }

                        // Botón Llamada con fondo suave
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(RodvarPrimary.copy(alpha = 0.12f), CircleShape)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                                    runCatching { context.startActivity(intent) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "Llamar", tint = RodvarPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
