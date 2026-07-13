package com.example.weatherly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Resolved light/dark state of the currently applied theme. Composables that
 * branch on dark mode for anything other than `MaterialTheme.colorScheme`
 * (shadow depth, gradient tones, tinted pill colours, etc.) must read this
 * instead of `isSystemInDarkTheme()` directly — otherwise a manual
 * Light/Dark override in Settings would apply the chosen colour scheme while
 * these details kept following the raw system setting, producing a
 * mismatched UI.
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary          = Color(0xFF6B86A3),
    onPrimary        = Color.White,
    background       = Color(0xFFF4F1EB),
    surface          = Color(0xFFFDFCFA),   // barely warm tint — cards read as quality paper, not clinical white
    onBackground     = Color(0xFF2B2F36),
    onSurface        = Color(0xFF2B2F36),
    onSurfaceVariant = Color(0xFF78848F)    // slightly more legible than the previous value
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
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content
        )
    }
}
