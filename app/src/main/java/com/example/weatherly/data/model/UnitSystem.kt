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
    // Snowfall does NOT follow precipitation_unit=mm the way rain/showers/precipitation do —
    // verified live against Open-Meteo: with precipitation_unit=mm, snowfall stays in cm (a real
    // 10x unit mismatch, not just a label difference); only precipitation_unit=inch converges
    // snowfall to "inch" too, matching precipLabel. So this needs its own label, not a reuse of
    // precipLabel — showing a snow amount with the "mm" suffix would be off by 10x.
    val snowLabel: String,
    val distanceLabel: String,
    val tempLabel: String,
    // NOAA CO-OPS's `units` query param ("english" = feet, "metric" = meters) and the matching
    // display label for tide-height predictions.
    val tideApiUnit: String,
    val tideHeightLabel: String
) {
    METRIC("celsius", "kmh", "mm", "km/h", "mm", "cm", "km", "°C", "metric", "m"),
    IMPERIAL("fahrenheit", "mph", "inch", "mph", "in", "in", "mi", "°F", "english", "ft")
}
