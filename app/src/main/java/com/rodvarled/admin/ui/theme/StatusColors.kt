package com.rodvarled.admin.ui.theme

import androidx.compose.ui.graphics.Color
import com.rodvarled.admin.ui.components.StatusStyle

fun appointmentStatusStyle(status: String): StatusStyle = when (status) {
    "Pendiente" -> StatusStyle(Color(0xFF78350F), Color(0xFFFDE68A))
    "Confirmada" -> StatusStyle(Color(0xFF1E3A8A), Color(0xFFBFDBFE))
    "Instalada" -> StatusStyle(Color(0xFF064E3B), Color(0xFF6EE7B7))
    "Cancelada" -> StatusStyle(Color(0xFF7F1D1D), Color(0xFFFCA5A5))
    else -> StatusStyle(Color(0xFF374151), Color(0xFFE5E7EB))
}

fun quoteStatusStyle(status: String): StatusStyle = when (status) {
    "Draft", "Borrador de Cita" -> StatusStyle(Color(0xFF374151), Color(0xFFE5E7EB))
    "Pendiente" -> StatusStyle(Color(0xFF78350F), Color(0xFFFDE68A))
    "Agendado" -> StatusStyle(Color(0xFF1E3A8A), Color(0xFFBFDBFE))
    "Instalado" -> StatusStyle(Color(0xFF064E3B), Color(0xFF6EE7B7))
    "Cancelado" -> StatusStyle(Color(0xFF7F1D1D), Color(0xFFFCA5A5))
    else -> StatusStyle(Color(0xFF374151), Color(0xFFE5E7EB))
}

/** Unifica el tri-estado de garantías (antes duplicado en la lista y en el detalle). */
fun warrantyStatusStyle(isActive: Boolean, isSigned: Boolean): StatusStyle = when {
    !isActive -> StatusStyle(Color(0xFF374151), Color(0xFFE5E7EB))
    isSigned -> StatusStyle(Color(0xFF064E3B), Color(0xFF6EE7B7))
    else -> StatusStyle(Color(0xFF78350F), Color(0xFFFDE68A))
}
