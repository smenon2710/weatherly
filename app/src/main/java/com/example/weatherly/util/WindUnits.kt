package com.example.weatherly.util

import com.example.weatherly.data.model.UnitSystem

/**
 * Wind values in `WeatherData` (`windKmh`, `windGustKmh`, `DayEntry.windGustMaxKmh`, the hourly
 * series) are in the user's unit despite the "Kmh" in their names. Every threshold below is in
 * km/h, so convert first — comparing raw mph against them made imperial users' wind read about
 * 1.6x calmer than the same wind in metric (R2).
 */
fun windToKmh(value: Float, windUnit: String): Float =
    if (windUnit == UnitSystem.IMPERIAL.windLabel) value * 1.609344f else value

/** Short tile-length callout for today's forecast peak gust. */
fun windAdvisory(peakGustKmh: Float): String = when {
    peakGustKmh < 10 -> "Calm all day"
    peakGustKmh < 20 -> "Light breeze"
    peakGustKmh < 40 -> "Breezy"
    peakGustKmh < 60 -> "Windy"
    else -> "Strong gusts"
}

/** Gusts strong enough for the animated background's wind-streak layer. */
fun isSevereWind(gustKmh: Float): Boolean = gustKmh >= 45f
