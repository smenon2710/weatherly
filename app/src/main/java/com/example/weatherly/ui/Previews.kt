package com.example.weatherly.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.weatherly.data.model.DayEntry
import com.example.weatherly.data.model.HourEntry
import com.example.weatherly.data.model.TipTone
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.data.model.WeatherTip
import com.example.weatherly.ui.theme.WeatherlyTheme

private fun sampleDay(
    label: String, full: String, high: Int, low: Int, icon: Int, phrase: String, pop: Int
) = DayEntry(
    dayLabel = label, fullDateLabel = full, highC = high, lowC = low, icon = icon, phrase = phrase,
    sunrise = "5:32 AM", sunset = "8:24 PM", uvMax = 6, precipProbMax = pop,
    windMaxKmh = 18, precipSumMm = if (pop > 30) 2.4 else 0.0
)

private val sampleWeather = WeatherData(
    locationName = "Franklin Township, NJ",
    currentTempC = 31,
    highTodayC = 34,
    lowTodayC = 22,
    realFeelC = 36,
    condition = "Sunny",
    currentIcon = 0,
    isDay = true,
    humidity = 48,
    windKmh = 12,
    windGustKmh = 24,
    windDir = "SW",
    windUnit = "km/h",
    pressureHpa = 1012,
    cloudCoverPct = 10,
    precipMm = 0.0,
    precipUnit = "mm",
    uvIndex = 8,
    uvLabel = "Very High",
    visibility = 20,
    visibilityUnit = "km",
    sunrise = "5:32 AM",
    sunset = "8:24 PM",
    headline = "Sunny today. High 34°, low 22°.",
    comparedToYesterday = "3° warmer than yesterday",
    tips = listOf(
        WeatherTip("💧", "It's going to be hot — carry water and find some shade.", TipTone.HOT),
        WeatherTip("😎", "Lovely day ahead — make the most of it!", TipTone.NICE),
    ),
    weekMinC = 14,
    weekMaxC = 34,
    hourly = listOf(
        HourEntry("Now",   31, 0,  isDay = true,  precipChance = 0),
        HourEntry("8 AM",  32, 1,  isDay = true,  precipChance = 0),
        HourEntry("9 AM",  33, 0,  isDay = true,  precipChance = 0),
        HourEntry("10 AM", 34, 0,  isDay = true,  precipChance = 0),
        HourEntry("11 AM", 33, 2,  isDay = true,  precipChance = 10),
        HourEntry("12 PM", 31, 3,  isDay = true,  precipChance = 20),
        HourEntry("1 PM",  29, 61, isDay = true,  precipChance = 40),
        HourEntry("2 PM",  27, 80, isDay = false,  precipChance = 55),
    ),
    aqi = 42,
    aqiLabel = "Good",
    hourlyUv = listOf(8, 7, 6, 5, 4, 3, 2, 1),
    hourlyWind = listOf(12, 13, 14, 16, 18, 17, 15, 12),
    hourlyFeels = listOf(36, 37, 38, 38, 36, 33, 30, 28),
    hourlyHumidity = listOf(48, 46, 44, 42, 45, 52, 60, 66),
    hourlyVisibility = listOf(20, 20, 20, 18, 16, 14, 12, 10),
    hourlyPressure = listOf(1012, 1012, 1011, 1011, 1010, 1010, 1009, 1009),
    hourlyPrecipProb = listOf(0, 0, 0, 10, 20, 40, 55, 60),
    hourlyAqi = listOf(42, 44, 46, 48, 50, 47, 45, 43),
    daily = listOf(
        sampleDay("Today", "Today", 34, 22, 0, "Sunny", 10),
        sampleDay("Mon", "Monday, Jun 15", 27, 19, 53, "Drizzle", 50),
        sampleDay("Tue", "Tuesday, Jun 16", 26, 14, 3, "Overcast", 10),
        sampleDay("Wed", "Wednesday, Jun 17", 26, 16, 61, "Slight rain", 60),
        sampleDay("Thu", "Thursday, Jun 18", 32, 19, 80, "Showers", 70),
        sampleDay("Fri", "Friday, Jun 19", 30, 17, 1, "Mainly clear", 5),
        sampleDay("Sat", "Saturday, Jun 20", 28, 15, 0, "Clear sky", 0),
    )
)

@Preview(name = "Weather - Day", showBackground = true, heightDp = 1300)
@Composable
private fun WeatherDayPreview() {
    WeatherlyTheme {
        WeatherContent(sampleWeather, onRefresh = {})
    }
}

@Preview(
    name = "Weather - Night",
    showBackground = true,
    heightDp = 1300,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun WeatherNightPreview() {
    WeatherlyTheme(darkTheme = true) {
        WeatherContent(sampleWeather.copy(isDay = false, currentIcon = 0), onRefresh = {})
    }
}
