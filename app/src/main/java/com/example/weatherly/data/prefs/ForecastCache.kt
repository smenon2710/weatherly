package com.example.weatherly.data.prefs

import android.content.Context
import com.example.weatherly.data.model.WeatherData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** Persists the last successful forecast so the app can open offline. */
class ForecastCache(context: Context) {

    private val prefs = context.getSharedPreferences("forecast_cache", Context.MODE_PRIVATE)
    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(WeatherData::class.java)

    fun save(data: WeatherData) {
        prefs.edit()
            .putString(KEY_DATA, adapter.toJson(data))
            .putLong(KEY_TS, System.currentTimeMillis())
            .apply()
    }

    /** Returns the cached data + the timestamp it was saved, or null if nothing is cached. */
    fun load(): Pair<WeatherData, Long>? {
        val json = prefs.getString(KEY_DATA, null) ?: return null
        val ts = prefs.getLong(KEY_TS, 0L)
        return runCatching { adapter.fromJson(json)!! to ts }.getOrNull()
    }

    companion object {
        private const val KEY_DATA = "data"
        private const val KEY_TS = "ts"
    }
}
