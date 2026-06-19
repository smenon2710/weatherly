package com.example.weatherly.data.repository

import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.weatherly.data.model.AirQualityResponse
import com.example.weatherly.data.model.DayEntry
import com.example.weatherly.data.model.HourEntry
import com.example.weatherly.data.model.OpenMeteoResponse
import com.example.weatherly.data.model.SavedPlace
import com.example.weatherly.data.model.TipTone
import com.example.weatherly.data.model.UnitSystem
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.data.model.WeatherTip
import com.example.weatherly.data.remote.NetworkModule
import com.example.weatherly.util.wmoText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Single source of truth, backed by Open-Meteo (no API key). Fetches the
 * forecast and air quality in parallel, in the requested units. A 30-minute
 * in-memory cache (keyed by location + units) avoids repeats.
 */
class WeatherRepository(private val context: Context) {

    private val api = NetworkModule.api
    private val geocodingApi = NetworkModule.geocodingApi
    private val airQualityApi = NetworkModule.airQualityApi

    private data class Cached(val key: String, val data: WeatherData, val timestamp: Long)
    private var memoryCache: Cached? = null

    suspend fun getWeather(
        lat: Double,
        lon: Double,
        units: UnitSystem,
        placeName: String? = null,
        forceRefresh: Boolean = false
    ): Result<WeatherData> = withContext(Dispatchers.IO) {
        val cacheKey = "%.3f,%.3f,%s".format(Locale.US, lat, lon, units.name)
        val now = System.currentTimeMillis()
        memoryCache?.let { c ->
            if (!forceRefresh && c.key == cacheKey && now - c.timestamp < CACHE_TTL_MS) {
                return@withContext Result.success(c.data)
            }
        }
        try {
            val (forecast, air) = coroutineScope {
                val f = async {
                    api.getForecast(
                        latitude = lat, longitude = lon,
                        temperatureUnit = units.apiTemp,
                        windSpeedUnit = units.apiWind,
                        precipitationUnit = units.apiPrecip
                    )
                }
                val a = async { runCatching { airQualityApi.get(lat, lon) }.getOrNull() }
                f.await() to a.await()
            }
            val name = placeName ?: reverseGeocode(lat, lon)
            val data = mapToWeatherData(forecast, air, name, units)
            memoryCache = Cached(cacheKey, data, now)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchCity(query: String): List<SavedPlace> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching {
            geocodingApi.search(query.trim()).results.orEmpty().map {
                SavedPlace(it.name, it.admin1, it.country, it.latitude, it.longitude)
            }
        }.getOrDefault(emptyList())
    }

    private fun mapToWeatherData(
        r: OpenMeteoResponse,
        air: AirQualityResponse?,
        locationName: String,
        units: UnitSystem
    ): WeatherData {
        val current = r.current
        val currentCode = current?.weatherCode ?: 0

        val hourTimes = r.hourly?.time ?: emptyList()
        val currentTime = current?.time
        val nowIndex = if (currentTime != null) {
            hourTimes.indexOfFirst { it >= currentTime }.let { if (it == -1) 0 else it }
        } else 0
        val end = minOf(nowIndex + 24, hourTimes.size)
        val window = (nowIndex until end).toList()

        fun visToUnit(meters: Double): Int =
            if (units == UnitSystem.IMPERIAL) (meters / 1609.34).roundToInt() else (meters / 1000.0).roundToInt()

        val hourly = window.map { i ->
            HourEntry(
                hourLabel = if (i == nowIndex) "Now" else formatHour(hourTimes[i]),
                tempC = r.hourly?.temperature?.getOrNull(i)?.roundToInt() ?: 0,
                icon = r.hourly?.weatherCode?.getOrNull(i) ?: 0,
                precipChance = r.hourly?.precipitationProbability?.getOrNull(i)
            )
        }
        val hourlyUv = window.map { (r.hourly?.uvIndex?.getOrNull(it) ?: 0.0).roundToInt() }
        val hourlyWind = window.map { (r.hourly?.windSpeed?.getOrNull(it) ?: 0.0).roundToInt() }
        val hourlyFeels = window.map { (r.hourly?.apparentTemperature?.getOrNull(it) ?: 0.0).roundToInt() }
        val hourlyHumidity = window.map { r.hourly?.humidity?.getOrNull(it) ?: 0 }
        val hourlyVisibility = window.map { visToUnit(r.hourly?.visibility?.getOrNull(it) ?: 0.0) }
        val hourlyPressure = window.map { (r.hourly?.surfacePressure?.getOrNull(it) ?: 0.0).roundToInt() }
        val hourlyPrecipProb = window.map { r.hourly?.precipitationProbability?.getOrNull(it) ?: 0 }
        val hourlyAqi = window.map { (air?.hourly?.usAqi?.getOrNull(it) ?: 0.0).roundToInt() }

        val dTimes = r.daily?.time ?: emptyList()
        val dCodes = r.daily?.weatherCode ?: emptyList()
        val dHighs = r.daily?.tempMax ?: emptyList()
        val dLows = r.daily?.tempMin ?: emptyList()
        val dSunrise = r.daily?.sunrise ?: emptyList()
        val dSunset = r.daily?.sunset ?: emptyList()
        val dUv = r.daily?.uvIndexMax ?: emptyList()
        val dPop = r.daily?.precipProbMax ?: emptyList()
        val dWind = r.daily?.windSpeedMax ?: emptyList()
        val dPrecip = r.daily?.precipitationSum ?: emptyList()

        val todayDate = currentTime?.substring(0, 10)
        val todayIndex = dTimes.indexOf(todayDate).let {
            if (it < 0) (if (dTimes.size > 1) 1 else 0) else it
        }
        val yesterdayHigh = (todayIndex - 1).takeIf { it >= 0 }?.let { dHighs.getOrNull(it)?.roundToInt() }

        val daily = buildList {
            for (i in todayIndex until dTimes.size) {
                val code = dCodes.getOrNull(i) ?: 0
                add(
                    DayEntry(
                        dayLabel = if (i == todayIndex) "Today" else formatDay(dTimes[i]),
                        fullDateLabel = if (i == todayIndex) "Today" else formatFull(dTimes[i]),
                        highC = dHighs.getOrNull(i)?.roundToInt() ?: 0,
                        lowC = dLows.getOrNull(i)?.roundToInt() ?: 0,
                        icon = code,
                        phrase = wmoText(code),
                        sunrise = clock(dSunrise.getOrNull(i)),
                        sunset = clock(dSunset.getOrNull(i)),
                        uvMax = dUv.getOrNull(i)?.roundToInt(),
                        precipProbMax = dPop.getOrNull(i),
                        windMaxKmh = dWind.getOrNull(i)?.roundToInt(),
                        precipSumMm = dPrecip.getOrNull(i)
                    )
                )
            }
        }

        val weekMin = daily.minOfOrNull { it.lowC } ?: 0
        val weekMax = daily.maxOfOrNull { it.highC } ?: 30
        val today = daily.firstOrNull()
        val highToday = today?.highC ?: (current?.temperature?.roundToInt() ?: 0)
        val lowToday = today?.lowC ?: (current?.temperature?.roundToInt() ?: 0)

        val uvNow = r.hourly?.uvIndex?.getOrNull(nowIndex) ?: dUv.getOrNull(todayIndex)
        val visMeters = r.hourly?.visibility?.getOrNull(nowIndex)
        val aqiNow = air?.current?.usAqi?.roundToInt() ?: air?.hourly?.usAqi?.getOrNull(nowIndex)?.roundToInt()

        val comparedToYesterday = yesterdayHigh?.let { y ->
            val diff = highToday - y
            when {
                abs(diff) < 2 -> "About the same as yesterday"
                diff > 0 -> "$diff° warmer than yesterday"
                else -> "${-diff}° cooler than yesterday"
            }
        }

        val tips = buildTips(today?.icon ?: currentCode, highToday, lowToday, today?.precipProbMax, today?.windMaxKmh, units)

        return WeatherData(
            locationName = locationName,
            currentTempC = current?.temperature?.roundToInt() ?: 0,
            highTodayC = highToday,
            lowTodayC = lowToday,
            realFeelC = current?.apparentTemperature?.roundToInt(),
            condition = wmoText(currentCode),
            currentIcon = currentCode,
            isDay = (current?.isDay ?: 1) == 1,
            humidity = current?.humidity,
            windKmh = current?.windSpeed?.roundToInt(),
            windGustKmh = current?.windGusts?.roundToInt(),
            windDir = current?.windDirection?.let { compass(it) },
            windUnit = units.windLabel,
            pressureHpa = current?.surfacePressure?.roundToInt(),
            cloudCoverPct = current?.cloudCover,
            precipMm = current?.precipitation,
            precipUnit = units.precipLabel,
            uvIndex = uvNow?.roundToInt(),
            uvLabel = uvNow?.let { uvLabel(it) },
            visibility = visMeters?.let { visToUnit(it) },
            visibilityUnit = units.distanceLabel,
            aqi = aqiNow,
            aqiLabel = aqiNow?.let { aqiLabel(it) },
            sunrise = clock(dSunrise.getOrNull(todayIndex)),
            sunset = clock(dSunset.getOrNull(todayIndex)),
            headline = today?.let { "${it.phrase} today. High ${it.highC}°, low ${it.lowC}°." },
            comparedToYesterday = comparedToYesterday,
            tips = tips,
            weekMinC = weekMin,
            weekMaxC = weekMax,
            hourly = hourly,
            hourlyUv = hourlyUv,
            hourlyWind = hourlyWind,
            hourlyFeels = hourlyFeels,
            hourlyHumidity = hourlyHumidity,
            hourlyVisibility = hourlyVisibility,
            hourlyPressure = hourlyPressure,
            hourlyPrecipProb = hourlyPrecipProb,
            hourlyAqi = hourlyAqi,
            daily = daily
        )
    }

    private fun buildTips(
        code: Int, high: Int, low: Int, pop: Int?, windMax: Int?, units: UnitSystem
    ): List<WeatherTip> = buildList {
        val isSnow = code in 71..77 || code in 85..86
        val isRain = code in 51..67 || code in 80..82 || code in 95..99
        val hot = if (units == UnitSystem.IMPERIAL) high >= 86 else high >= 30
        val cold = if (units == UnitSystem.IMPERIAL) (low <= 32 || high <= 41) else (low <= 0 || high <= 5)
        val windy = if (units == UnitSystem.IMPERIAL) (windMax ?: 0) >= 25 else (windMax ?: 0) >= 40
        val mild = if (units == UnitSystem.IMPERIAL) high in 61..82 else high in 16..28

        when {
            isSnow -> add(WeatherTip("❄️", "Snow on the way — bundle up and take it slow.", TipTone.SNOW))
            isRain || (pop ?: 0) >= 50 ->
                add(WeatherTip("🌂", "Rain likely today — grab an umbrella before you head out.", TipTone.RAIN))
        }
        if (hot) add(WeatherTip("💧", "It's going to be hot — carry water and find some shade.", TipTone.HOT))
        else if (cold) add(WeatherTip("🧥", "Cold out there — wear a warm jacket.", TipTone.COLD))
        if (windy) add(WeatherTip("💨", "Quite windy — secure anything loose outside.", TipTone.WIND))
        if (isEmpty() && code in 0..2 && mild) add(WeatherTip("😎", "Lovely day ahead — make the most of it!", TipTone.NICE))
        if (isEmpty()) add(WeatherTip("🌤️", "No major weather to plan around today.", TipTone.NEUTRAL))
    }.take(2)

    private suspend fun reverseGeocode(lat: Double, lon: Double): String {
        return try {
            if (!Geocoder.isPresent()) return "Current location"
            val geocoder = Geocoder(context, Locale.getDefault())
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCoroutine { cont ->
                    geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(results: List<android.location.Address>) {
                            cont.resume(results.firstOrNull())
                        }
                        override fun onError(errorMessage: String?) {
                            cont.resume(null)
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
            }
            address ?: return "Current location"
            listOfNotNull(address.locality ?: address.subAdminArea, address.adminArea)
                .distinct().joinToString(", ").ifBlank { "Current location" }
        } catch (e: Exception) {
            "Current location"
        }
    }

    private fun uvLabel(uv: Double): String = when {
        uv < 3 -> "Low"
        uv < 6 -> "Moderate"
        uv < 8 -> "High"
        uv < 11 -> "Very High"
        else -> "Extreme"
    }

    private fun aqiLabel(aqi: Int): String = when {
        aqi <= 50 -> "Good"
        aqi <= 100 -> "Moderate"
        aqi <= 150 -> "Unhealthy (sensitive)"
        aqi <= 200 -> "Unhealthy"
        aqi <= 300 -> "Very unhealthy"
        else -> "Hazardous"
    }

    private fun compass(deg: Double): String {
        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val idx = ((((deg % 360) + 360) % 360) / 45.0).roundToInt() % 8
        return dirs[idx]
    }

    private fun formatHour(iso: String) =
        parse(iso, "yyyy-MM-dd'T'HH:mm")?.let { SimpleDateFormat("h a", Locale.getDefault()).format(it) } ?: "--"

    private fun formatDay(iso: String) =
        parse(iso, "yyyy-MM-dd")?.let { SimpleDateFormat("EEE", Locale.getDefault()).format(it) } ?: "--"

    private fun formatFull(iso: String) =
        parse(iso, "yyyy-MM-dd")?.let { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(it) } ?: "--"

    private fun clock(iso: String?) =
        iso?.let { parse(it, "yyyy-MM-dd'T'HH:mm") }?.let { SimpleDateFormat("h:mm a", Locale.getDefault()).format(it) }

    private fun parse(value: String, pattern: String): java.util.Date? = try {
        SimpleDateFormat(pattern, Locale.US).parse(value)
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val CACHE_TTL_MS = 30 * 60 * 1000L
    }
}
