package com.rodvarled.admin.ui.appointments

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rodvarled.admin.data.remote.dto.AppointmentSummary
import com.rodvarled.admin.ui.components.DateChip
import com.rodvarled.admin.ui.components.RodvarListRow
import com.rodvarled.admin.ui.components.StatusDot
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

/** Encabezado de agrupación relativo: Hoy / Mañana / el nombre del día / la fecha completa si ya pasó la semana. */
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
 * Fila de cita (no tarjeta): chip de fecha a la izquierda como ancla visual, info al centro,
 * indicadores de estado a la derecha. Se apoya en un divisor entre filas, no en cajas anidadas.
 */
@Composable
fun AppointmentCard(
    appointment: AppointmentSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = appointmentStatusStyle(appointment.status)

    RodvarListRow(
        onClick = onClick,
        modifier = modifier,
        leading = { DateChip(dateStr = appointment.requestedDate, accent = style.content) },
        headline = { Text(appointment.customerName, style = MaterialTheme.typography.titleSmall, maxLines = 1) },
        supporting = {
            val time = formatAppointmentTime(appointment.requestedTime)
            Text(
                (time ?: "Sin hora") + (if (appointment.vehicleInfo != "No especificado") "  ·  ${appointment.vehicleInfo}" else ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            if (appointment.itinerarySentAt != null || appointment.warrantyId != null) {
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    if (appointment.itinerarySentAt != null) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Itinerario enviado", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(13.dp))
                    }
                    if (appointment.warrantyId != null) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Con garantía", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 6.dp).size(13.dp))
                    }
                }
            }
        },
        trailing = { StatusDot(appointment.status, style) }
    )
}
