package com.rodvarled.admin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Fila de lista sin Card ni elevación (generaliza AppointmentCard): visual líder a la izquierda,
 * contenido central, trailing opcional a la derecha, separada por divisores entre filas.
 */
@Composable
fun RodvarListRow(
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    headline: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supporting: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showChevron: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()

        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            headline()
            supporting?.invoke()
        }

        trailing?.invoke()

        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/** Visual líder circular con ícono, mismo lenguaje de tinte al 12% que DateChip. */
@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(accent.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = accent)
    }
}

/** Visual líder circular con iniciales, para registros centrados en una persona. */
@Composable
fun InitialsAvatar(name: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    val initials = name.trim().split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
    Box(
        modifier = modifier
            .size(44.dp)
            .background(accent.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials.ifBlank { "?" },
            style = MaterialTheme.typography.titleSmall,
            color = accent,
            textAlign = TextAlign.Center
        )
    }
}
