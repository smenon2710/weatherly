package com.example.weatherly.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/** Manual wiring for the Open-Meteo clients. No authentication needed. */
object NetworkModule {

    private const val FORECAST_URL = "https://api.open-meteo.com/"
    private const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/"
    private const val AIR_QUALITY_URL = "https://air-quality-api.open-meteo.com/"
    private const val OPENROUTER_URL = "https://openrouter.ai/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addDebugLogging()
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: OpenMeteoApi by lazy { retrofit(FORECAST_URL).create(OpenMeteoApi::class.java) }
    val geocodingApi: GeocodingApi by lazy { retrofit(GEOCODING_URL).create(GeocodingApi::class.java) }
    val airQualityApi: AirQualityApi by lazy { retrofit(AIR_QUALITY_URL).create(AirQualityApi::class.java) }

    // LLM responses can take a while, so this client gets a longer read timeout.
    private val openRouterHttp: OkHttpClient = okHttp.newBuilder()
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(70, TimeUnit.SECONDS)
        .build()

    val openRouterApi: OpenRouterApi by lazy {
        Retrofit.Builder()
            .baseUrl(OPENROUTER_URL)
            .client(openRouterHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenRouterApi::class.java)
    }

    fun makeStreamingCall(request: Request): Call = openRouterHttp.newCall(request)
}
