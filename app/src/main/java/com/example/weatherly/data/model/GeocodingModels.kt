package com.example.weatherly.data.model

import com.squareup.moshi.Json

/** Open-Meteo geocoding search response (geocoding-api.open-meteo.com). */
data class GeocodingResponse(
    @Json(name = "results") val results: List<GeoResult>?
)

data class GeoResult(
    @Json(name = "name") val name: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "admin1") val admin1: String?,
    @Json(name = "country") val country: String?
)
