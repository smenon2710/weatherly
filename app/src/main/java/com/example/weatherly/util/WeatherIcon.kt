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

/** Rain, drizzle included: 51–67 and the rain showers 80–82. Not a plain `51..82` range, which
 * also swallows the snow codes 71–77 (R3, R7). */
fun isRainCode(code: Int): Boolean = code in 51..67 || code in 80..82

/** Snow: 71–77 and the snow showers 85–86. */
fun isSnowCode(code: Int): Boolean = code in 71..77 || code in 85..86

/** "snow" when the day has any forecast snowfall, else "rain" — for labels on a precipitation
 * probability, which on its own doesn't say which kind (R7). Same length on purpose: the
 * widget's lines are tuned to fit. */
fun precipWord(snowfall: Double?): String = if ((snowfall ?: 0.0) > 0.0) "snow" else "rain"
