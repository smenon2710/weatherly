package com.example.weatherly.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import java.util.Calendar

/**
 * Pure (non-@Composable) extraction of `conditionGradient()`'s sky-color selection
 * (`ui/components/WeatherComponents.kt`), shared by the in-app hero background and the
 * home-screen widget so both stay driven by one source of truth for WMO-code thresholds and
 * dark/light calibration instead of drifting via two independently-maintained copies.
 * `conditionGradient()` itself now just calls this and appends the theme's background color as
 * the gradient's second stop.
 */
fun skyColor(code: Int, isDay: Boolean, isDark: Boolean): Color {
    val sky = when {
        code in 95..99 ->
            if (isDark) Color(0xFF241A3D) else Color(0xFF3A2F50)
        // Snow/rain/fog: unlike thunder above (deliberately dark regardless of literal daylight),
        // these three used the same daytime-bright tone for night too — a real gap, not a design
        // choice, since dark theme's own equivalents are already dark enough to read fine at any
        // hour but light theme's pastel daytime tones read as a sunny, jarringly-wrong sky at
        // actual night. Only light theme needs a distinct night variant here; dark theme is
        // unchanged.
        code in 71..86 ->
            if (isDark) Color(0xFF283C52) else if (isDay) Color(0xFFD0E0EE) else Color(0xFF1E2E44)
        code in 51..82 ->
            if (isDark) Color(0xFF1C3348) else if (isDay) Color(0xFFBFD4E6) else Color(0xFF17293C)
        code in 45..48 ->
            if (isDark) Color(0xFF283038) else if (isDay) Color(0xFFCDD3D8) else Color(0xFF20262C)
        !isDay ->
            if (isDark) Color(0xFF04091A) else Color(0xFF1A2448)
        code <= 2 ->
            if (isDark) Color(0xFF16324E) else Color(0xFFB8D8F0)
        else ->
            if (isDark) Color(0xFF212E3A) else Color(0xFFC8D2DC)
    }
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..6   -> lerp(sky, Color(0xFFE8936A), 0.18f)  // dawn — warm apricot
        in 17..18 -> lerp(sky, Color(0xFFD4A44C), 0.15f)  // golden hour — amber
        in 19..20 -> lerp(sky, Color(0xFFA0668A), 0.20f)  // dusk — dusty violet
        else      -> sky
    }
}
