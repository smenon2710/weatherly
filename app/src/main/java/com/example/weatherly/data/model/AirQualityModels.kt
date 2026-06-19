package com.example.weatherly.data.model

import com.squareup.moshi.Json

/** Open-Meteo air-quality API response (air-quality-api.open-meteo.com). */
data class AirQualityResponse(
    @Json(name = "current") val current: AqCurrent?,
    @Json(name = "hourly") val hourly: AqHourly?
)

data class AqCurrent(@Json(name = "us_aqi") val usAqi: Double?)

data class AqHourly(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "us_aqi") val usAqi: List<Double?>?
)
