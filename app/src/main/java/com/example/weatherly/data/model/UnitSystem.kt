package com.example.weatherly.data.model

/**
 * Metric vs Imperial. The string fields feed Open-Meteo's query parameters and
 * the display labels used across the UI.
 */
enum class UnitSystem(
    val apiTemp: String,
    val apiWind: String,
    val apiPrecip: String,
    val windLabel: String,
    val precipLabel: String,
    val distanceLabel: String,
    val tempLabel: String
) {
    METRIC("celsius", "kmh", "mm", "km/h", "mm", "km", "°C"),
    IMPERIAL("fahrenheit", "mph", "inch", "mph", "in", "mi", "°F")
}
