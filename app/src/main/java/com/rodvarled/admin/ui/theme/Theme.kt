package com.rodvarled.admin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RodvarBlue,
    onPrimary = Color.White,
    primaryContainer = RodvarBlueContainer,
    onPrimaryContainer = Color.White,
    secondary = RodvarEmerald,
    onSecondary = Color.White,
    secondaryContainer = RodvarEmeraldContainer,
    onSecondaryContainer = Color.White,
    tertiary = RodvarAmber,
    error = RodvarRed,
    onError = Color.White,
    background = RodvarBackground,
    onBackground = Color(0xFFE5E7EB),
    surface = RodvarSurface,
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = RodvarSurfaceVariant,
    onSurfaceVariant = RodvarOnSurfaceMuted,
    outline = RodvarOutline,
    surfaceContainer = RodvarSurface,
    surfaceContainerHigh = RodvarSurfaceVariant,
    surfaceContainerLow = RodvarBackground,
)

private val LightColorScheme = lightColorScheme(
    primary = RodvarBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = RodvarBlueDark,
    secondary = Color(0xFF047857),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF047857),
    tertiary = RodvarAmber,
    error = RodvarRed,
    onError = Color.White,
    background = RodvarBackgroundLight,
    onBackground = Color(0xFF111827),
    surface = RodvarSurfaceLight,
    onSurface = Color(0xFF111827),
    surfaceVariant = RodvarSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFD1D5DB),
)

@Composable
fun RodvarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // La identidad de marca de Rodvar es intencionalmente oscura (glassmorphism azul/esmeralda);
    // no usamos dynamicColor para no diluirla con el wallpaper del usuario.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
