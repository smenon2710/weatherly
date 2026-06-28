package com.example.weatherly.data.model

/** Clean domain model the UI renders. Built by the repository. */
data class WeatherData(
    val locationName: String,
    val currentTempC: Int,
    val highTodayC: Int,
    val lowTodayC: Int,
    val realFeelC: Int?,
    val condition: String,
    val currentIcon: Int,
    val isDay: Boolean,
    val humidity: Int?,
    val windKmh: Int?,
    val windGustKmh: Int?,
    val windDir: String?,
    val windUnit: String,
    val pressureHpa: Int?,
    val cloudCoverPct: Int?,
    val precipMm: Double?,
    val precipUnit: String,
    val uvIndex: Int?,
    val uvLabel: String?,
    val visibility: Int?,
    val visibilityUnit: String,
    val aqi: Int?,
    val aqiLabel: String?,
    val sunrise: String?,
    val sunset: String?,
    val headline: String?,
    val comparedToYesterday: String?,
    val tips: List<WeatherTip>,
    val weekMinC: Int,
    val weekMaxC: Int,
    val hourly: List<HourEntry>,
    // Aligned hourly series (same length/labels as `hourly`) for the detail charts.
    val hourlyUv: List<Int>,
    val hourlyWind: List<Int>,
    val hourlyFeels: List<Int>,
    val hourlyHumidity: List<Int>,
    val hourlyVisibility: List<Int>,
    val hourlyPressure: List<Int>,
    val hourlyPrecipProb: List<Int>,
    val hourlyAqi: List<Int>,
    val daily: List<DayEntry>
) {
    val hourLabels: List<String> get() = hourly.map { it.hourLabel }
}

/** Data for a metric's hourly bar chart in its detail popup. */
data class MetricChart(val labels: List<String>, val values: List<Float>, val unit: String)

data class WeatherTip(val emoji: String, val text: String, val tone: TipTone)

enum class TipTone { HOT, COLD, RAIN, SNOW, WIND, NICE, NEUTRAL }

data class HourEntry(
    val hourLabel: String,
    val tempC: Int,
    val icon: Int,
    val isDay: Boolean,
    val precipChance: Int?
)

data class DayEntry(
    val dayLabel: String,
    val fullDateLabel: String,
    val highC: Int,
    val lowC: Int,
    val icon: Int,
    val phrase: String?,
    val sunrise: String?,
    val sunset: String?,
    val uvMax: Int?,
    val precipProbMax: Int?,
    val windMaxKmh: Int?,
    val precipSumMm: Double?
)
