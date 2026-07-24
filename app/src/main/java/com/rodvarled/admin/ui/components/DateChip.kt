package com.rodvarled.admin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Chip tipo "hoja de calendario": día grande + mes abreviado. Ancla visual para filas de citas. */
@Composable
fun DateChip(dateStr: String?, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    val date = remember(dateStr) { runCatching { LocalDate.parse(dateStr) }.getOrNull() }

    Column(
        modifier = modifier
            .size(52.dp)
            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            date?.dayOfMonth?.toString() ?: "-",
            style = MaterialTheme.typography.titleLarge,
            color = accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            date?.month?.getDisplayName(TextStyle.SHORT, Locale("es", "MX"))?.uppercase() ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.weight(1f))
    }
}
