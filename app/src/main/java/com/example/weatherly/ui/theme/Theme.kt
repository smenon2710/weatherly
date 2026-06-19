package com.example.weatherly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary          = Color(0xFF6B86A3),
    onPrimary        = Color.White,
    background       = Color(0xFFF4F1EB),
    surface          = Color.White,
    onBackground     = Color(0xFF2B2F36),
    onSurface        = Color(0xFF2B2F36),
    onSurfaceVariant = Color(0xFF828A93)
)

private val DarkColors = darkColorScheme(
    primary          = Color(0xFF7FA3C2),
    onPrimary        = Color(0xFF0F1923),
    background       = Color(0xFF0F1923),
    surface          = Color(0xFF1A2530),
    onBackground     = Color(0xFFE0E6ED),
    onSurface        = Color(0xFFE0E6ED),
    onSurfaceVariant = Color(0xFF8A9BAD)
)

@Composable
fun WeatherlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
