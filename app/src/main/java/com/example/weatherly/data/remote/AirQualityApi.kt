package com.example.weatherly.data.remote

import com.example.weatherly.data.model.AirQualityResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AirQualityApi {
    /** US AQI now + hourly. Base: https://air-quality-api.open-meteo.com/ */
    @GET("v1/air-quality")
    suspend fun get(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "us_aqi",
        @Query("hourly") hourly: String = "us_aqi",
        @Query("timezone") timezone: String = "auto",
        @Query("past_days") pastDays: Int = 1,
        @Query("forecast_days") forecastDays: Int = 2
    ): AirQualityResponse
}
