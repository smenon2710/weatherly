package com.example.weatherly.data.remote

import com.example.weatherly.data.model.NwsAlertsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** NWS active alerts. Base: https://api.weather.gov/ — no API key, US coverage only. */
interface NwsApi {
    @GET("alerts/active")
    suspend fun getActiveAlerts(@Query("point") point: String): NwsAlertsResponse
}
