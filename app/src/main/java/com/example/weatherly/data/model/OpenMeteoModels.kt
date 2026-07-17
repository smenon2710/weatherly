package com.example.weatherly.data.model

import com.squareup.moshi.Json

/** Open-Meteo Forecast API response. No API key. Data licensed CC BY 4.0. */
data class OpenMeteoResponse(
    @Json(name = "current") val current: CurrentBlock?,
    @Json(name = "hourly") val hourly: HourlyBlock?,
    @Json(name = "daily") val daily: DailyBlock?
)

data class CurrentBlock(
    @Json(name = "time") val time: String?,
    @Json(name = "temperature_2m") val temperature: Double?,
    @Json(name = "relative_humidity_2m") val humidity: Int?,
    @Json(name = "apparent_temperature") val apparentTemperature: Double?,
    @Json(name = "is_day") val isDay: Int?,
    @Json(name = "weather_code") val weatherCode: Int?,
    @Json(name = "wind_speed_10m") val windSpeed: Double?,
    @Json(name = "wind_gusts_10m") val windGusts: Double?,
    @Json(name = "wind_direction_10m") val windDirection: Double?,
    @Json(name = "surface_pressure") val surfacePressure: Double?,
    @Json(name = "cloud_cover") val cloudCover: Int?,
    @Json(name = "precipitation") val precipitation: Double?,
    // Real type-specific fields, distinct from `precipitation` (which is the rain+showers+snow
    // water-equivalent total) — needed to tell users accurately whether it's rain or snow falling
    // right now rather than inferring from the WMO weather_code. Unit follows precipitation_unit
    // for rain/showers (mm/inch); snowfall's own unit is cm/inch — see UnitSystem.snowLabel.
    @Json(name = "rain") val rain: Double?,
    @Json(name = "showers") val showers: Double?,
    @Json(name = "snowfall") val snowfall: Double?
)

data class HourlyBlock(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "temperature_2m") val temperature: List<Double>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int?>?,
    @Json(name = "uv_index") val uvIndex: List<Double?>?,
    @Json(name = "visibility") val visibility: List<Double?>?,
    @Json(name = "wind_speed_10m") val windSpeed: List<Double?>?,
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double?>?,
    @Json(name = "relative_humidity_2m") val humidity: List<Int?>?,
    @Json(name = "surface_pressure") val surfacePressure: List<Double?>?,
    @Json(name = "rain") val rain: List<Double?>?,
    @Json(name = "showers") val showers: List<Double?>?,
    @Json(name = "snowfall") val snowfall: List<Double?>?
)

data class DailyBlock(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?,
    @Json(name = "temperature_2m_max") val tempMax: List<Double>?,
    @Json(name = "temperature_2m_min") val tempMin: List<Double>?,
    @Json(name = "sunrise") val sunrise: List<String>?,
    @Json(name = "sunset") val sunset: List<String>?,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double?>?,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double?>?,
    @Json(name = "precipitation_probability_max") val precipProbMax: List<Int?>?,
    @Json(name = "wind_speed_10m_max") val windSpeedMax: List<Double?>?,
    @Json(name = "snowfall_sum") val snowfallSum: List<Double?>?
)
