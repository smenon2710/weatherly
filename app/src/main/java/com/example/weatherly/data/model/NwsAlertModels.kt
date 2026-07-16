package com.example.weatherly.data.model

import com.squareup.moshi.Json

/** National Weather Service active-alerts API response (api.weather.gov). US coverage only. */
data class NwsAlertsResponse(@Json(name = "features") val features: List<NwsAlertFeature>?)

data class NwsAlertFeature(@Json(name = "properties") val properties: NwsAlertProperties?)

data class NwsAlertProperties(
    @Json(name = "id") val id: String?,
    @Json(name = "event") val event: String?,
    @Json(name = "severity") val severity: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "certainty") val certainty: String?,
    @Json(name = "urgency") val urgency: String?,
    @Json(name = "sent") val sent: String?,
    @Json(name = "headline") val headline: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "instruction") val instruction: String?,
    @Json(name = "effective") val effective: String?,
    @Json(name = "expires") val expires: String?,
    @Json(name = "ends") val ends: String?,
    @Json(name = "senderName") val senderName: String?,
    @Json(name = "areaDesc") val areaDesc: String?
)
