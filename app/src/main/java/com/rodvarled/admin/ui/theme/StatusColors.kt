package com.rodvarled.admin.ui.theme

import androidx.compose.ui.graphics.Color
import com.rodvarled.admin.ui.components.StatusStyle

fun appointmentStatusStyle(status: String): StatusStyle = when (status) {
    "Pendiente" -> StatusStyle(Color(0xFFB45309), Color(0xFFFFFBEB))
    "Confirmada" -> StatusStyle(Color(0xFF1E40AF), Color(0xFFEFF6FF))
    "Instalada" -> StatusStyle(Color(0xFF047857), Color(0xFFECFDF5))
    "Cancelada" -> StatusStyle(Color(0xFFB91C1C), Color(0xFFFEF2F2))
    else -> StatusStyle(Color(0xFF475569), Color(0xFFF1F5F9))
}

fun quoteStatusStyle(status: String): StatusStyle = when (status) {
    "Draft", "Borrador de Cita" -> StatusStyle(Color(0xFF475569), Color(0xFFF1F5F9))
    "Pendiente" -> StatusStyle(Color(0xFFB45309), Color(0xFFFFFBEB))
    "Agendado" -> StatusStyle(Color(0xFF1E40AF), Color(0xFFEFF6FF))
    "Instalado" -> StatusStyle(Color(0xFF047857), Color(0xFFECFDF5))
    "Cancelado" -> StatusStyle(Color(0xFFB91C1C), Color(0xFFFEF2F2))
    else -> StatusStyle(Color(0xFF475569), Color(0xFFF1F5F9))
}

/** Unifica el tri-estado de garantías. */
fun warrantyStatusStyle(isActive: Boolean, isSigned: Boolean): StatusStyle = when {
    !isActive -> StatusStyle(Color(0xFF475569), Color(0xFFF1F5F9))
    isSigned -> StatusStyle(Color(0xFF047857), Color(0xFFECFDF5))
    else -> StatusStyle(Color(0xFFB45309), Color(0xFFFFFBEB))
}
