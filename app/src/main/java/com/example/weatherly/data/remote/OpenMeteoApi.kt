package com.example.weatherly.data.remote

import com.example.weatherly.data.model.OpenMeteoResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("temperature_unit") temperatureUnit: String,
        @Query("wind_speed_unit") windSpeedUnit: String,
        @Query("precipitation_unit") precipitationUnit: String,
        @Query("current") current: String =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code," +
            "wind_speed_10m,wind_gusts_10m,wind_direction_10m,surface_pressure,cloud_cover,precipitation",
        @Query("hourly") hourly: String =
            "temperature_2m,weather_code,precipitation_probability,uv_index,visibility," +
            "wind_speed_10m,apparent_temperature,relative_humidity_2m,surface_pressure",
        @Query("daily") daily: String =
            "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset," +
            "uv_index_max,precipitation_sum,precipitation_probability_max,wind_speed_10m_max",
        @Query("timezone") timezone: String = "auto",
        @Query("past_days") pastDays: Int = 1,
        @Query("forecast_days") forecastDays: Int = 7
    ): OpenMeteoResponse
}
