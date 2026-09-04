package com.example.weatherly.data.repository

import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.weatherly.data.model.AirQualityResponse
import com.example.weatherly.data.model.AlertSeverity
import com.example.weatherly.data.model.DayEntry
import com.example.weatherly.data.model.HourEntry
import com.example.weatherly.data.model.HourlyBlock
import com.example.weatherly.data.model.NwsAlertProperties
import com.example.weatherly.data.model.NwsAlertsResponse
import com.example.weatherly.data.model.OpenMeteoResponse
import com.example.weatherly.data.model.SavedPlace
import com.example.weatherly.data.model.TidePredictionsResponse
import com.example.weatherly.data.model.TideEvent
import com.example.weatherly.data.model.TideInfo
import com.example.weatherly.data.model.TideStation
import com.example.weatherly.data.model.TideType
import com.example.weatherly.data.model.TipTone
import com.example.weatherly.data.model.UnitSystem
import com.example.weatherly.data.model.WeatherAlert
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.data.model.WeatherTip
import com.example.weatherly.data.remote.NetworkModule
import com.example.weatherly.util.TideStations
import com.example.weatherly.util.wmoText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
    private val nwsApi = NetworkModule.nwsApi
    private val tideApi = NetworkModule.tideApi

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
            val fetched = coroutineScope {
                // Retried, unlike the other three fetches below: a failure here fails the whole
                // screen (see the catch block), so it's worth extra attempts against a transient
                // blip from Open-Meteo's free/keyless tier — a real 5xx, or a malformed (non-JSON)
                // body that surfaces as a Moshi JsonEncodingException, which is an IOException
                // under the hood, not a genuine local parsing bug. Up to 2 retries (3 attempts
                // total) with increasing backoff (1s, 2s) — bumped from a single retry after a
                // real user report of two consecutive failures on a network with measurable packet
                // loss; mirrors ChatRepository's existing retry-on-429 pattern for OpenRouter,
                // just with more attempts since this path has no server-dictated retry signal to
                // respect.
                val f = async {
                    suspend fun fetch() = api.getForecast(
                        latitude = lat, longitude = lon,
                        temperatureUnit = units.apiTemp,
                        windSpeedUnit = units.apiWind,
                        precipitationUnit = units.apiPrecip
                    )
                    var attempt = 0
                    while (true) {
                        try {
                            return@async fetch()
                        } catch (e: Exception) {
                            attempt++
                            if (attempt > 2 || !isTransient(e)) throw e
                            delay(1_000L * attempt)
                        }
                    }
                    @Suppress("UNREACHABLE_CODE") throw IllegalStateException()
                }
                val a = async { runCatching { airQualityApi.get(lat, lon) }.getOrNull() }
                // NWS is US-only and has no key of its own; a failure or an out-of-coverage
                // point (empty features list) must never fail the overall weather fetch.
                val n = async {
                    runCatching { nwsApi.getActiveAlerts("%.4f,%.4f".format(Locale.US, lat, lon)) }.getOrNull()
                }
                // Same defensive shape as NWS above: TideStations.nearest() is the coastal gate
                // (see its own doc comment) — nothing is fetched at all for an inland location,
                // and a station lookup that succeeds but a NOAA fetch that fails must never fail
                // the overall weather fetch either.
                val t = async {
                    TideStations.nearest(context, lat, lon)?.let { station ->
                        runCatching { tideApi.getPredictions(station.id, units.tideApiUnit) }
                            .getOrNull()
                            ?.let { station to it }
                    }
                }
                Fetched(f.await(), a.await(), n.await(), t.await())
            }
            val name = placeName ?: reverseGeocode(lat, lon)
            val data = mapToWeatherData(fetched.forecast, fetched.air, fetched.alerts, fetched.tide, name, units)
            memoryCache = Cached(cacheKey, data, now)
            Result.success(data)
        } catch (e: CancellationException) {
            // Never swallow cancellation — e.g. the screen/ViewModel being torn down mid-fetch, or
            // a newer load() superseding this one. Catching it below (as any Exception subtype)
            // and converting it into Result.failure would surface a fake "Couldn't connect" error
            // for what is actually just a cancelled, no-longer-relevant fetch — confirmed live via
            // the logging added below: a real capture showed "kotlinx.coroutines.
            // JobCancellationException: Job was cancelled" reaching this catch block and being
            // reported to the user as a network failure. Rethrowing lets structured concurrency
            // handle it the way ChatViewModel's own askStreaming()/addLocalExchange() already do.
            throw e
        } catch (e: Exception) {
            // Logged (survives release builds — nothing in proguard-rules.pro strips Log calls)
            // specifically so a real failure has a diagnosable trace in logcat: the friendly
            // message below is deliberately generic for the user, but without this line a
            // release-build failure leaves zero trace of the actual exception type/cause.
            android.util.Log.w("WeatherRepository", "getWeather failed for $lat,$lon", e)
            // WeatherViewModel shows this message verbatim (WeatherUiState.Error) — never let a
            // raw Retrofit/Moshi exception ("HTTP 500 Internal Server Error", "Use
            // JsonReader.setLenient(true)...") reach the screen. Same principle as
            // ChatRepository.httpMessage().
            Result.failure(Exception(friendlyMessage(e), e))
        }
    }

    private fun isTransient(e: Exception): Boolean =
        (e is HttpException && e.code() in 500..599) || e is IOException

    private fun friendlyMessage(e: Throwable): String = when {
        e is HttpException && e.code() in 500..599 ->
            "The weather service is having trouble right now. Please try again in a moment."
        e is HttpException -> "Couldn't fetch the forecast (HTTP ${e.code()}). Please try again."
        e is IOException -> "Couldn't connect. Check your internet connection and try again."
        else -> "Couldn't load the forecast right now. Please try again."
    }

    private data class Fetched(
        val forecast: OpenMeteoResponse,
        val air: AirQualityResponse?,
        val alerts: NwsAlertsResponse?,
        val tide: Pair<TideStation, TidePredictionsResponse>?
    )

    suspend fun searchCity(query: String): List<SavedPlace> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching {
            geocodingApi.search(query.trim()).results.orEmpty().map {
                SavedPlace(it.name, it.admin1, it.country, it.latitude, it.longitude, it.timezone)
            }
        }.getOrDefault(emptyList())
    }

    private fun mapToWeatherData(
        r: OpenMeteoResponse,
        air: AirQualityResponse?,
        alerts: NwsAlertsResponse?,
        tide: Pair<TideStation, TidePredictionsResponse>?,
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

        // Daily arrays — needed here to determine sunrise/sunset for hourly isDay computation.
        val dTimes = r.daily?.time ?: emptyList()
        val dCodes = r.daily?.weatherCode ?: emptyList()
        val dHighs = r.daily?.tempMax ?: emptyList()
        val dLows = r.daily?.tempMin ?: emptyList()
        val dSunrise = r.daily?.sunrise ?: emptyList()
        val dSunset = r.daily?.sunset ?: emptyList()
        val dUv = r.daily?.uvIndexMax ?: emptyList()
        val dPop = r.daily?.precipProbMax ?: emptyList()
        val dWind = r.daily?.windSpeedMax ?: emptyList()
        val dWindGust = r.daily?.windGustsMax ?: emptyList()
        val dPrecip = r.daily?.precipitationSum ?: emptyList()
        val dSnowSum = r.daily?.snowfallSum ?: emptyList()

        val hourly = window.map { i ->
            val hourTime = hourTimes.getOrNull(i) ?: ""
            val hourDate = if (hourTime.length >= 10) hourTime.substring(0, 10) else ""
            val dayIdx = dTimes.indexOf(hourDate).takeIf { it >= 0 }
            val sunriseStr = dayIdx?.let { dSunrise.getOrNull(it) }
            val sunsetStr = dayIdx?.let { dSunset.getOrNull(it) }
            val isHourDay = if (sunriseStr != null && sunsetStr != null) {
                hourTime >= sunriseStr && hourTime < sunsetStr
            } else {
                (current?.isDay ?: 1) == 1
            }
            HourEntry(
                hourLabel = if (i == nowIndex) "Now" else formatHour(hourTime),
                tempC = r.hourly?.temperature?.getOrNull(i)?.roundToInt() ?: 0,
                icon = r.hourly?.weatherCode?.getOrNull(i) ?: 0,
                isDay = isHourDay,
                precipChance = r.hourly?.precipitationProbability?.getOrNull(i),
                feelsLikeC = r.hourly?.apparentTemperature?.getOrNull(i)?.roundToInt()
                    ?: (r.hourly?.temperature?.getOrNull(i)?.roundToInt() ?: 0)
            )
        }
        val hourlyUv = window.map { (r.hourly?.uvIndex?.getOrNull(it) ?: 0.0).roundToInt() }
        val hourlyWind = window.map { (r.hourly?.windSpeed?.getOrNull(it) ?: 0.0).roundToInt() }
        val hourlyWindGust = window.map { (r.hourly?.windGusts?.getOrNull(it) ?: 0.0).roundToInt() }
        val hourlyFeels = window.map { (r.hourly?.apparentTemperature?.getOrNull(it) ?: 0.0).roundToInt() }
        val hourlyHumidity = window.map { r.hourly?.humidity?.getOrNull(it) ?: 0 }
        val hourlyVisibility = window.map { visToUnit(r.hourly?.visibility?.getOrNull(it) ?: 0.0) }
        val hourlyPressure = window.map { (r.hourly?.surfacePressure?.getOrNull(it) ?: 0.0).roundToInt() }
        val hourlyPrecipProb = window.map { r.hourly?.precipitationProbability?.getOrNull(it) ?: 0 }
        val hourlyAqi = window.map { (air?.hourly?.usAqi?.getOrNull(it) ?: 0.0).roundToInt() }
        // Real per-hour amounts, not just probability — rain+showers combined (both are liquid
        // precip; Open-Meteo's rain/showers split is a convective-vs-stratiform distinction, not
        // relevant to the wet-road/umbrella hazard) and snowfall kept separate, since it's a
        // different unit (see UnitSystem.snowLabel) and a different real-world hazard.
        val hourlyPrecipAmount = window.map {
            (r.hourly?.rain?.getOrNull(it) ?: 0.0) + (r.hourly?.showers?.getOrNull(it) ?: 0.0)
        }
        val hourlySnowfall = window.map { r.hourly?.snowfall?.getOrNull(it) ?: 0.0 }

        val todayDate = currentTime?.substring(0, 10)
        val todayIndex = dTimes.indexOf(todayDate).let {
            if (it < 0) (if (dTimes.size > 1) 1 else 0) else it
        }
        val yesterdayHigh = (todayIndex - 1).takeIf { it >= 0 }?.let { dHighs.getOrNull(it)?.roundToInt() }

        // How many of `hourly` (which already starts at "now") still fall within today — reuses
        // the same "first 12 AM label" boundary MetricChart.dayChangeIndex relies on elsewhere.
        // Used below to correct today's own precipProbMax; every other day's own full-day
        // aggregate stays untouched since those days haven't started yet.
        val todayRemainingCount = hourly.indexOfFirst { it.hourLabel == "12 AM" }
            .let { if (it < 0) hourly.size else it }

        val daily = buildList {
            for (i in todayIndex until dTimes.size) {
                val code = dCodes.getOrNull(i) ?: 0
                val dayHigh = dHighs.getOrNull(i)?.roundToInt() ?: 0
                val dayLow = dLows.getOrNull(i)?.roundToInt() ?: 0
                // Open-Meteo's precipitation_probability_max describes the FULL calendar day
                // (00:00–23:59). For today specifically, once part of the day has already
                // elapsed, that full-day max can describe a window that's already passed —
                // confirmed live (real cached data: a 40% daily max and a "Light drizzle" day
                // code from an already-elapsed morning still read as "Rain likely today" in the
                // 7-day list, the detail sheet, and the hero tip, hours after the actual forward
                // forecast had dropped to single digits with zero real rain left). Trust only
                // what's still ahead of "now" for today's own entry; every other day hasn't
                // started yet, so its full-day aggregate is exactly right and stays untouched.
                val dayPop = if (i == todayIndex) {
                    hourly.take(todayRemainingCount).mapNotNull { it.precipChance }.maxOrNull()
                } else {
                    dPop.getOrNull(i)
                }
                val dayWindMax = dWind.getOrNull(i)?.roundToInt()
                // buildDayOutlookTips()'s isSnow/isRain check keys off the code passed in,
                // independent of pop — so today's own dominant code (still the historical
                // full-day value, deliberately left as-is for the displayed icon/phrase above)
                // would keep firing "Rain likely" regardless of how low the corrected dayPop
                // now reads. Use the live current condition for today's tip text specifically,
                // same reasoning as tipsCode below for the hero tip.
                val tipsCodeForDay = if (i == todayIndex) currentCode else code
                add(
                    DayEntry(
                        dayLabel = if (i == todayIndex) "Today" else formatDay(dTimes[i]),
                        fullDateLabel = if (i == todayIndex) "Today" else formatFull(dTimes[i]),
                        highC = dayHigh,
                        lowC = dayLow,
                        icon = code,
                        phrase = wmoText(code),
                        sunrise = clock(dSunrise.getOrNull(i)),
                        sunset = clock(dSunset.getOrNull(i)),
                        uvMax = dUv.getOrNull(i)?.roundToInt(),
                        precipProbMax = dayPop,
                        windMaxKmh = dayWindMax,
                        precipSumMm = dPrecip.getOrNull(i),
                        snowfallSum = dSnowSum.getOrNull(i),
                        windGustMaxKmh = dWindGust.getOrNull(i)?.roundToInt(),
                        tips = buildDayOutlookTips(tipsCodeForDay, dayHigh, dayLow, dayPop, dayWindMax, units)
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

        // At night the day's peak heat is past — use tomorrow's data so tips reflect what's coming next.
        val isCurrentDay = (current?.isDay ?: 1) == 1
        val nextDay = daily.getOrNull(1)
        val tipDay = if (isCurrentDay || nextDay == null) today else nextDay
        val tipsCode: Int
        val tipsPop: Int
        if (isCurrentDay) {
            // today.precipProbMax is already corrected above to reflect only what's left of
            // today, so this just needs to prefer the live current condition over today's own
            // summary icon — a direct "is it raining right now" read is more reliable than a
            // whole-day code even after the probability fix, since a single "dominant" icon for
            // a mix of remaining-hour conditions isn't attempted here (deliberately out of scope
            // — see IMPROVEMENTS.md).
            tipsCode = currentCode
            tipsPop = tipDay?.precipProbMax ?: 0
        } else {
            // tipDay is tomorrow (the night-rollover case) — it hasn't started yet, so its own
            // full-day aggregate legitimately describes what's ahead; no elapsed-time correction
            // is needed here. Merge with the next 12 hourly slots' max in case a short-range spike
            // (tonight, before tomorrow's own block starts) isn't reflected in tomorrow's daily pop yet.
            val nextHoursMaxPop = hourly.drop(1).take(12).mapNotNull { it.precipChance }.maxOrNull() ?: 0
            tipsCode = tipDay?.icon ?: currentCode
            tipsPop = maxOf(tipDay?.precipProbMax ?: 0, nextHoursMaxPop)
        }
        val tips = buildTips(tipsCode, tipDay?.highC ?: highToday, tipDay?.lowC ?: lowToday, tipsPop.takeIf { it > 0 }, tipDay?.windMaxKmh, units)
        val headline = r.hourly?.let { buildUpcomingHeadline(it, nowIndex, units.windLabel, currentCode, units) }

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
            currentRainMm = current?.rain?.let { it + (current.showers ?: 0.0) },
            currentSnowfall = current?.snowfall,
            snowUnit = units.snowLabel,
            uvIndex = uvNow?.roundToInt(),
            uvLabel = uvNow?.let { uvLabel(it) },
            visibility = visMeters?.let { visToUnit(it) },
            visibilityUnit = units.distanceLabel,
            aqi = aqiNow,
            aqiLabel = aqiNow?.let { aqiLabel(it) },
            sunrise = clock(dSunrise.getOrNull(todayIndex)),
            sunset = clock(dSunset.getOrNull(todayIndex)),
            headline = headline,
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
            daily = daily,
            alerts = mapAlerts(alerts),
            hourlyPrecipAmount = hourlyPrecipAmount,
            hourlySnowfall = hourlySnowfall,
            dewPointC = current?.dewPoint?.roundToInt(),
            hourlyWindGust = hourlyWindGust,
            tides = mapTide(tide, units),
            timezone = r.timezone
        )
    }

    private fun mapTide(result: Pair<TideStation, TidePredictionsResponse>?, units: UnitSystem): TideInfo? {
        val (station, response) = result ?: return null
        val events = response.predictions.orEmpty().mapNotNull { p ->
            val timeLabel = p.time?.let { parse(it, "yyyy-MM-dd HH:mm") }
                ?.let { SimpleDateFormat("h:mm a", Locale.getDefault()).format(it) }
                ?: return@mapNotNull null
            val type = when (p.type) {
                "H" -> TideType.HIGH
                "L" -> TideType.LOW
                else -> return@mapNotNull null
            }
            val height = p.height?.toDoubleOrNull()
                ?.let { "%.1f %s".format(Locale.US, it, units.tideHeightLabel) }
                ?: return@mapNotNull null
            TideEvent(timeLabel, type, height)
        }
        // An empty predictions list happens for a station id that's technically valid but
        // doesn't actually publish tide predictions (a real, observed NOAA inconsistency, not
        // hypothetical) — treat the same as "no coastal data" rather than showing an empty tile.
        return if (events.isEmpty()) null else TideInfo(station.name, events)
    }

    private fun mapAlerts(response: NwsAlertsResponse?): List<WeatherAlert> {
        val severityOrder = listOf(
            AlertSeverity.EXTREME, AlertSeverity.SEVERE, AlertSeverity.MODERATE,
            AlertSeverity.MINOR, AlertSeverity.UNKNOWN
        )
        return response?.features.orEmpty()
            .mapNotNull { it.properties }
            // "Actual" excludes Test/Exercise/Draft products NWS also publishes through this feed.
            .filter { it.status == "Actual" }
            .distinctBy { it.id }
            // NWS can independently issue multiple non-linked products for the identical event
            // and area — confirmed live for Franklin Park, NJ: two separate Air Quality Alert
            // issuances covering the exact same 3 zones simultaneously, a day apart, neither
            // referencing the other (empty "references", messageType "Alert" not "Update"). This
            // is a rolling day-ahead reissuance pattern, not an error, but showing both raw CAP
            // products reads as a duplicate to a user — NWS's own consumer-facing tools show only
            // the current one. Collapse to the most-recently-sent entry per (event, area); a
            // missing event/areaDesc falls back to that alert's own id as the group key so two
            // unrelated alerts with incomplete data never accidentally collapse into each other.
            .groupBy { (it.event ?: it.id) to (it.areaDesc ?: it.id) }
            .map { (_, group) -> group.maxByOrNull { parseInstantOrMin(it.sent) } ?: group.first() }
            // Same-severity ties (e.g. two Air Quality Alerts, one for today and one for tomorrow)
            // otherwise sort in whatever order the API happened to return them.
            .sortedWith(
                compareBy<NwsAlertProperties> { severityOrder.indexOf(parseSeverity(it.severity, it.event, it.description)) }
                    .thenBy { parseInstantOrMax(it.effective) }
            )
            .map { p ->
                WeatherAlert(
                    id = p.id ?: p.hashCode().toString(),
                    event = p.event ?: "Weather Alert",
                    severity = parseSeverity(p.severity, p.event, p.description),
                    headline = p.headline ?: p.event ?: "Weather Alert",
                    description = normalizeNwsText(p.description),
                    instruction = p.instruction?.let { normalizeNwsText(it) },
                    areaDesc = p.areaDesc,
                    senderName = p.senderName,
                    effectiveLabel = formatNwsTime(p.effective),
                    // "ends" is when the hazard itself is expected to end; "expires" is only when
                    // the CAP message expires, which for long-duration products (e.g. a multi-day
                    // Flood Watch) is often much sooner than the actual hazard window and reads as
                    // misleadingly early if shown as "expires". Prefer "ends", falling back to
                    // "expires" for simpler/shorter products that don't set it.
                    expiresLabel = formatNwsTime(p.ends ?: p.expires),
                    // NWS sometimes sets these to "Unknown" rather than omitting them — treat that
                    // the same as absent so the UI doesn't render a useless "Unknown" badge.
                    urgency = p.urgency?.takeIf { it != "Unknown" },
                    certainty = p.certainty?.takeIf { it != "Unknown" }
                )
            }
    }

    private fun parseSeverity(raw: String?, event: String?, description: String?): AlertSeverity {
        when (raw) {
            "Extreme" -> return AlertSeverity.EXTREME
            "Severe" -> return AlertSeverity.SEVERE
            "Moderate" -> return AlertSeverity.MODERATE
            "Minor" -> return AlertSeverity.MINOR
        }
        // NWS tags nearly every Air Quality Alert severity "Unknown" — its CAP severity taxonomy
        // has no air-quality category — so without this, a "Code Red...unhealthful for the
        // general population" advisory would render in the calmest visual tier, same as a Small
        // Craft Advisory. State environmental agencies use EPA's standardized "Code <color>" AQI
        // names verbatim nationwide in these alerts, so it's a reliable signal to infer from.
        if (event?.contains("Air Quality", ignoreCase = true) == true) {
            val text = description.orEmpty()
            return when {
                Regex("code\\s+(purple|maroon)", RegexOption.IGNORE_CASE).containsMatchIn(text) -> AlertSeverity.EXTREME
                Regex("code\\s+red", RegexOption.IGNORE_CASE).containsMatchIn(text) -> AlertSeverity.SEVERE
                Regex("code\\s+orange", RegexOption.IGNORE_CASE).containsMatchIn(text) -> AlertSeverity.MODERATE
                else -> AlertSeverity.MODERATE // still an active DEP advisory — never as calm as "Info"
            }
        }
        return AlertSeverity.UNKNOWN
    }

    private fun parseInstantOrMax(iso: String?): java.time.Instant =
        iso?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() } ?: java.time.Instant.MAX

    // Opposite default of parseInstantOrMax: used when picking the *latest* of several timestamps
    // (most-recently-sent alert wins), where a missing/unparseable value should lose, not win.
    private fun parseInstantOrMin(iso: String?): java.time.Instant =
        iso?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() } ?: java.time.Instant.MIN

    // NWS text products are hard-wrapped to ~80 columns with a literal newline at every wrap
    // point, and only a genuine paragraph break uses a blank line (\n\n). Rendered verbatim in a
    // proportional-width Compose Text, those wrap newlines produce short, choppy lines instead of
    // normal paragraph reflow. Collapse single newlines to spaces; keep blank-line paragraph breaks.
    private fun normalizeNwsText(raw: String?): String =
        raw.orEmpty()
            .replace(Regex("(?<!\\n)\\n(?!\\n)"), " ")
            .replace(Regex(" {2,}"), " ")
            .trim()

    // NWS timestamps are ISO-8601 with a UTC offset (e.g. "2026-07-16T14:00:00-05:00"),
    // a different shape than Open-Meteo's local "yyyy-MM-dd'T'HH:mm" — java.time handles the
    // offset directly and is available unconditionally since minSdk 26 meets its API floor.
    // Time-only (no date) was ambiguous: two alerts effective/expiring at the same clock time on
    // different days (a real, observed case — NWS can issue same-named advisories day after day
    // for one location) rendered as literally identical text, e.g. "3:45 PM" for both, with no
    // way to tell them apart short of opening each one's full detail sheet. Day context is now
    // always included except for "today," where it would just be visual noise.
    private fun formatNwsTime(iso: String?): String? = iso?.let {
        try {
            val dt = OffsetDateTime.parse(it)
            val today = OffsetDateTime.now(dt.offset).toLocalDate()
            val timePart = dt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
            when (ChronoUnit.DAYS.between(today, dt.toLocalDate())) {
                0L -> timePart
                1L -> "Tomorrow, $timePart"
                -1L -> "Yesterday, $timePart"
                else -> dt.format(DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault()))
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildUpcomingHeadline(
        rawHourly: HourlyBlock,
        nowIndex: Int,
        windUnit: String,
        currentIcon: Int,
        units: UnitSystem
    ): String {
        val totalHours = rawHourly.time?.size ?: 0
        val end = minOf(nowIndex + 13, totalHours) // scan nowIndex+1 .. nowIndex+12
        val alreadyRain = currentIcon in 61..82
        val alreadySnow = currentIcon in 71..77 || currentIcon in 85..86
        val alreadyThunder = currentIcon in 95..99
        val alreadyFog = currentIcon == 45 || currentIcon == 48

        var eventDesc: String? = null
        var eventTime: String? = null
        for (i in (nowIndex + 1) until end) {
            val code = rawHourly.weatherCode?.getOrNull(i) ?: 0
            val precip = rawHourly.precipitationProbability?.getOrNull(i) ?: 0
            val match = when {
                code in 95..99 && !alreadyThunder -> "Thunderstorm"
                (code in 71..77 || code in 85..86) && !alreadySnow ->
                    if (code in 85..86) "Snow showers" else "Snow"
                code in 66..67 && !alreadyRain -> "Freezing rain"
                code in 61..82 && !alreadyRain -> "Rain"
                code in 51..57 && !alreadyRain && precip >= 30 -> "Drizzle"
                (code == 45 || code == 48) && !alreadyFog -> "Fog"
                else -> null
            }
            if (match != null) {
                eventDesc = match
                eventTime = rawHourly.time?.getOrNull(i)?.let { formatHour(it) }
                break
            }
        }

        val maxWind = ((nowIndex + 1) until end)
            .mapNotNull { rawHourly.windSpeed?.getOrNull(it) }
            .maxOrNull()?.roundToInt() ?: 0
        val windThreshold = if (units == UnitSystem.IMPERIAL) 25 else 40

        return when {
            eventDesc != null && maxWind >= windThreshold ->
                "$eventDesc expected around $eventTime. Winds up to $maxWind $windUnit."
            eventDesc != null -> "$eventDesc expected around $eventTime."
            maxWind >= windThreshold -> "Winds up to $maxWind $windUnit in the next few hours."
            else -> {
                // Summarise the dominant condition over the next 6 hours, but only when one
                // condition actually has a real majority (>=60%) — a bare plurality (e.g. 3 of 6
                // hours, tied with something else) isn't a confident enough basis to assert a
                // specific condition, and previously produced misleading headlines like "Clear
                // skies" during genuinely mixed stretches.
                val nextCodes = (nowIndex until minOf(nowIndex + 6, totalHours))
                    .mapNotNull { rawHourly.weatherCode?.getOrNull(it) }
                val counts = nextCodes.groupingBy { it }.eachCount()
                val topEntry = counts.maxByOrNull { it.value }
                val hasClearMajority = nextCodes.isNotEmpty() &&
                    topEntry != null &&
                    topEntry.value.toDouble() / nextCodes.size >= 0.6
                if (!hasClearMajority) {
                    "Mixed conditions over the next few hours."
                } else when (topEntry!!.key) {
                    0, 1 -> "Clear skies for the next few hours."
                    2 -> "Partly cloudy for the next few hours."
                    3 -> "Overcast for the next few hours."
                    in 45..48 -> "Foggy conditions for the next few hours."
                    in 71..77, in 85..86 -> "Snow expected over the next few hours."
                    in 51..57 -> "Light drizzle over the next few hours."
                    in 61..67, in 80..82 -> "Rain expected over the next few hours."
                    in 95..99 -> "Thunderstorm conditions in the next few hours."
                    else -> "No significant changes in the next few hours."
                }
            }
        }
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

    // Per-day "how to plan around this" advice for the 7-day forecast's own detail sheet
    // (DetailSheet.Day) — deliberately a separate function from buildTips() above, not a shared
    // call with a parameter, even though the threshold logic overlaps: buildTips()'s wording is
    // hardcoded "today"/"tonight"-relative for the hero TipBanner (which only ever shows for the
    // current day, with its own night-rollover-to-tomorrow logic), and reusing it verbatim for an
    // arbitrary future day tapped open from the 7-day list (e.g. "Wed") would read as wrong
    // ("Rain likely today" shown while looking at Wednesday's forecast on a Monday). Kept fully
    // independent — including its own threshold constants — so a future change to one wording set
    // can't silently ship a mismatched change to the other.
    private fun buildDayOutlookTips(
        code: Int, high: Int, low: Int, pop: Int?, windMax: Int?, units: UnitSystem
    ): List<WeatherTip> = buildList {
        val isSnow = code in 71..77 || code in 85..86
        val isRain = code in 51..67 || code in 80..82 || code in 95..99
        // A second, more severe heat tier than buildTips()'s single "hot" threshold — genuine
        // excess-heat/heatwave territory (roughly NWS Excessive Heat Warning range), warranting
        // "stay indoors" rather than just "carry water", per user request to distinguish the two.
        val veryHot = if (units == UnitSystem.IMPERIAL) high >= 100 else high >= 38
        val hot = if (units == UnitSystem.IMPERIAL) high >= 86 else high >= 30
        val cold = if (units == UnitSystem.IMPERIAL) (low <= 32 || high <= 41) else (low <= 0 || high <= 5)
        val windy = if (units == UnitSystem.IMPERIAL) (windMax ?: 0) >= 25 else (windMax ?: 0) >= 40
        val mild = if (units == UnitSystem.IMPERIAL) high in 61..82 else high in 16..28

        when {
            isSnow -> add(WeatherTip("❄️", "Snow expected — plan for slippery, slow-going conditions if you're headed out.", TipTone.SNOW))
            isRain || (pop ?: 0) >= 50 ->
                add(WeatherTip("🌂", "Rain likely — pack an umbrella or raincoat if you'll be outdoors.", TipTone.RAIN))
        }
        when {
            veryHot -> add(WeatherTip("🥵", "Dangerous heat expected — stay indoors during peak hours, stay hydrated, and check on vulnerable neighbors.", TipTone.HOT))
            hot -> add(WeatherTip("💧", "Hot conditions — stay hydrated, carry water, and take breaks in the shade.", TipTone.HOT))
            cold -> add(WeatherTip("🧥", "Cold conditions — dress warmly in layers.", TipTone.COLD))
        }
        if (windy) add(WeatherTip("💨", "Windy conditions — secure loose outdoor items.", TipTone.WIND))
        if (isEmpty() && code in 0..2 && mild) add(WeatherTip("😎", "Good conditions for outdoor plans.", TipTone.NICE))
        if (isEmpty()) add(WeatherTip("🌤️", "No major weather concerns for this day.", TipTone.NEUTRAL))
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
