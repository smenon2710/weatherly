package com.example.weatherly.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Weatherly uses a fixed light design, so Material components (text fields,
// chips, bottom sheets) always use a light scheme regardless of system dark mode.
private val LightColors = lightColorScheme(
    primary = Color(0xFF6B86A3),
    onPrimary = Color.White,
    background = Color(0xFFF4F1EB),
    surface = Color.White,
    onSurface = Color(0xFF2B2F36),
    onSurfaceVariant = Color(0xFF828A93)
)

@Composable
fun WeatherlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
