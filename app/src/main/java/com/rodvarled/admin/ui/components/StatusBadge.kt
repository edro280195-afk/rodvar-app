package com.rodvarled.admin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class StatusStyle(val background: Color, val content: Color)

/** Badge tipo píldora — reservado para el encabezado principal de una pantalla de detalle. */
@Composable
fun StatusBadge(text: String, style: StatusStyle, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = style.content,
        modifier = modifier
            .background(style.background, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** Punto + etiqueta — estado "silencioso" para filas de lista, sin el peso visual de una píldora. */
@Composable
fun StatusDot(text: String, style: StatusStyle, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(style.content, CircleShape))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = style.content,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
