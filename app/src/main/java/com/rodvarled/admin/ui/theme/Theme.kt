package com.rodvarled.admin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = RodvarPrimary,
    onPrimary = Color.White,
    primaryContainer = RodvarPrimaryContainer,
    onPrimaryContainer = RodvarPrimaryDark,
    secondary = RodvarSecondary,
    onSecondary = Color.White,
    secondaryContainer = RodvarSecondaryContainer,
    onSecondaryContainer = RodvarOnSecondaryContainer,
    tertiary = RodvarAmber,
    onTertiary = Color.White,
    tertiaryContainer = RodvarAmberContainer,
    onTertiaryContainer = RodvarAmber,
    error = RodvarRed,
    onError = Color.White,
    errorContainer = RodvarRedContainer,
    onErrorContainer = RodvarRed,
    background = RodvarBackground,
    onBackground = RodvarTextPrimary,
    surface = RodvarSurface,
    onSurface = RodvarTextPrimary,
    surfaceVariant = RodvarSurfaceVariant,
    onSurfaceVariant = RodvarTextSecondary,
    outline = RodvarOutline,
    outlineVariant = RodvarOutlineVariant,
    surfaceContainer = RodvarSurface,
    surfaceContainerHigh = RodvarSurfaceVariant,
    surfaceContainerLow = RodvarBackground,
)

@Composable
fun RodvarTheme(
    darkTheme: Boolean = false, // Forzado a la nueva línea de diseño clara limpia según preferencia explícita
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
