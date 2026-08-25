package com.example.weatherly.data.remote

import com.example.weatherly.data.model.TidePredictionsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** NOAA CO-OPS Tides & Currents. Free, public domain, no key. US coverage only (coastal stations
 * plus tidally-influenced rivers/estuaries) — see util/TideStations.kt for the nearest-station
 * gate that keeps this from ever being called for an inland location. */
interface TideApi {

    @GET("api/prod/datagetter")
    suspend fun getPredictions(
        @Query("station") stationId: String,
        @Query("units") units: String,
        @Query("product") product: String = "predictions",
        @Query("datum") datum: String = "MLLW",
        @Query("time_zone") timeZone: String = "lst_ldt",
        @Query("interval") interval: String = "hilo",
        @Query("format") format: String = "json",
        @Query("date") date: String = "today",
        @Query("application") application: String = "SkySpeakWeatherApp"
    ): TidePredictionsResponse
}
