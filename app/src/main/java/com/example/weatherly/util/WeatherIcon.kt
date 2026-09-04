package com.example.weatherly.util

/**
 * WMO weather codes -> plain-English phrase. Real icons are `WeatherGlyph`'s hand-drawn Canvas
 * vector shapes (`ui/components/WeatherGlyph.kt`), not emoji — raw emoji rendered inconsistently
 * across OEM launchers, which is why the widget also rasterizes these same vector shapes instead.
 */
fun wmoText(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45 -> "Fog"
    48 -> "Rime fog"
    51 -> "Light drizzle"
    53 -> "Drizzle"
    55 -> "Dense drizzle"
    56, 57 -> "Freezing drizzle"
    61 -> "Slight rain"
    63 -> "Rain"
    65 -> "Heavy rain"
    66, 67 -> "Freezing rain"
    71 -> "Slight snow"
    73 -> "Snow"
    75 -> "Heavy snow"
    77 -> "Snow grains"
    80 -> "Light showers"
    81 -> "Showers"
    82 -> "Violent showers"
    85, 86 -> "Snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm, hail"
    else -> "—"
}
