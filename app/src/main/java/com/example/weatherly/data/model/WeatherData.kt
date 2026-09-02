package com.example.weatherly.data.model

/** Clean domain model the UI renders. Built by the repository. */
data class WeatherData(
    val locationName: String,
    val currentTempC: Int,
    val highTodayC: Int,
    val lowTodayC: Int,
    val realFeelC: Int?,
    val condition: String,
    val currentIcon: Int,
    val isDay: Boolean,
    val humidity: Int?,
    val windKmh: Int?,
    val windGustKmh: Int?,
    val windDir: String?,
    val windUnit: String,
    val pressureHpa: Int?,
    val cloudCoverPct: Int?,
    val precipMm: Double?,
    val precipUnit: String,
    // Real type-specific current-conditions amounts, distinct from `precipMm` (rain+showers+snow
    // water-equivalent combined) — a genuine unit mismatch, not just a naming one: snowfall is in
    // `snowUnit` (cm/in), not `precipUnit` (mm/in) — see UnitSystem.snowLabel. Defaulted to null
    // for the same ForecastCache/Moshi backward-compat reason as `alerts` below: old cached JSON
    // written before these fields existed is missing the keys entirely.
    val currentRainMm: Double? = null,
    val currentSnowfall: Double? = null,
    val snowUnit: String = "cm",
    val uvIndex: Int?,
    val uvLabel: String?,
    val visibility: Int?,
    val visibilityUnit: String,
    val aqi: Int?,
    val aqiLabel: String?,
    val sunrise: String?,
    val sunset: String?,
    val headline: String?,
    val comparedToYesterday: String?,
    val tips: List<WeatherTip>,
    val weekMinC: Int,
    val weekMaxC: Int,
    val hourly: List<HourEntry>,
    // Aligned hourly series (same length/labels as `hourly`) for the detail charts.
    val hourlyUv: List<Int>,
    val hourlyWind: List<Int>,
    val hourlyFeels: List<Int>,
    val hourlyHumidity: List<Int>,
    val hourlyVisibility: List<Int>,
    val hourlyPressure: List<Int>,
    val hourlyPrecipProb: List<Int>,
    val hourlyAqi: List<Int>,
    val daily: List<DayEntry>,
    // Default required: ForecastCache deserializes old cached JSON (written before this field
    // existed) via Moshi's reflective adapter, which only fills in a missing key from a default.
    val alerts: List<WeatherAlert> = emptyList(),
    // Real per-hour amounts (not probability) — rain+showers combined, and snowfall separately.
    // Same defaulting reason as `alerts`.
    val hourlyPrecipAmount: List<Double> = emptyList(),
    val hourlySnowfall: List<Double> = emptyList(),
    // A more accurate "how muggy it'll actually feel" signal than relative humidity alone — see
    // CurrentBlock.dewPoint's doc comment. Same defaulting reason as `alerts`.
    val dewPointC: Int? = null,
    // Forecast gust series, distinct from the current-only `windGustKmh` above — same defaulting
    // reason as `alerts`.
    val hourlyWindGust: List<Int> = emptyList(),
    // Null for every inland location — see TideStations.nearest()'s coastal gate. Present only
    // when a NOAA CO-OPS tide-prediction station is within range of the resolved coordinates.
    val tides: TideInfo? = null,
    // IANA zone id (e.g. "America/New_York") this forecast was resolved to — drives the hero's
    // live local-time display. Same defaulting reason as `alerts`.
    val timezone: String? = null
) {
    val hourLabels: List<String> get() = hourly.map { it.hourLabel }
}

/** Today's high/low tide predictions from the nearest NOAA CO-OPS station within range (see
 * util/TideStations.kt). US coastal/tidally-influenced locations only — NOAA has no coverage
 * elsewhere, and this app has no fallback tide source (no free non-US equivalent fits the app's
 * no-key, no-backend constraints). */
data class TideInfo(
    val stationName: String,
    val events: List<TideEvent>
)

data class TideEvent(
    val timeLabel: String,
    val type: TideType,
    val heightLabel: String
)

enum class TideType { HIGH, LOW }

/** Data for a metric's hourly bar chart in its detail popup. */
data class MetricChart(
    val labels: List<String>,
    val values: List<Float>,
    val unit: String,
    /** Index where the calendar day changes (i.e. "12 AM" entry). Null when no day boundary in range. */
    val dayChangeIndex: Int? = null,
    /** For a chart whose values are on a known fixed scale (e.g. a 0–100 probability) — forces
     * SparklineTile's y-axis to that range instead of auto-scaling to the values' own min/max.
     * Null (the default) preserves auto-scaling, which is correct for unbounded series like
     * temperature or wind where there's no fixed universal range. Without this, a day where
     * precipitation chance only wobbles between 10–25% renders as a dramatic full-height peak,
     * visually indistinguishable from a genuine 80–100% day — the sparkline shape reflects the
     * day's *relative* wiggle, not the *absolute* risk level, which is exactly backwards for a
     * chart someone reads to decide whether to carry an umbrella.
     */
    val fixedRange: ClosedFloatingPointRange<Float>? = null
)

data class WeatherTip(val emoji: String, val text: String, val tone: TipTone)

enum class TipTone { HOT, COLD, RAIN, SNOW, WIND, NICE, NEUTRAL }

/** Official advisory from the National Weather Service (api.weather.gov). US coverage only. */
data class WeatherAlert(
    val id: String,
    val event: String,
    val severity: AlertSeverity,
    val headline: String,
    val description: String,
    val instruction: String?,
    val areaDesc: String?,
    val senderName: String?,
    val effectiveLabel: String?,
    val expiresLabel: String?,
    // Raw NWS values (e.g. "Immediate", "Likely") — null/"Unknown" means NWS didn't set one.
    val urgency: String?,
    val certainty: String?
)

/** Minimal persisted record of an alert that was shown, used to detect when it later clears. */
data class TrackedAlert(val id: String, val event: String)

enum class AlertSeverity { EXTREME, SEVERE, MODERATE, MINOR, UNKNOWN }

data class HourEntry(
    val hourLabel: String,
    val tempC: Int,
    val icon: Int,
    val isDay: Boolean,
    val precipChance: Int?,
    // Defaults to tempC so existing call sites (Previews.kt) that predate this field still
    // compile with a reasonable placeholder — WeatherRepository always passes the real value.
    val feelsLikeC: Int = tempC
)

data class DayEntry(
    val dayLabel: String,
    val fullDateLabel: String,
    val highC: Int,
    val lowC: Int,
    val icon: Int,
    val phrase: String?,
    val sunrise: String?,
    val sunset: String?,
    val uvMax: Int?,
    val precipProbMax: Int?,
    val windMaxKmh: Int?,
    val precipSumMm: Double?,
    val snowfallSum: Double? = null,
    // Today's forecast peak gust — distinct from windMaxKmh (sustained speed). Same defaulting
    // reason as WeatherData.alerts (ForecastCache backward-compat with pre-existing cached JSON).
    val windGustMaxKmh: Int? = null,
    // Practical, day-specific "what should I do" advice for the 7-day forecast's detail sheet —
    // e.g. "pack an umbrella", "stay hydrated", "dangerous heat, stay indoors" — distinct from
    // WeatherData.tips (the hero TipBanner's "right now"-phrased, night-rollover-aware advice for
    // today/tonight only). Same defaulting reason as WeatherData.alerts.
    val tips: List<WeatherTip> = emptyList()
)
