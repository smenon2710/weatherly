package com.example.weatherly.data.advice

import com.example.weatherly.data.model.AlertSeverity
import com.example.weatherly.data.model.DayEntry
import com.example.weatherly.data.model.HourEntry
import com.example.weatherly.data.model.TipTone
import com.example.weatherly.data.model.WeatherAlert
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.data.model.WeatherTip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastBriefingTest {

    private fun hour(label: String, temp: Int, code: Int = 0, pop: Int = 0) =
        HourEntry(label, temp, code, isDay = true, precipChance = pop)

    private fun day(
        label: String, code: Int, low: Int, high: Int, pop: Int,
        wind: Int? = null, tips: List<WeatherTip> = emptyList(), feels: List<Int> = emptyList()
    ) = DayEntry(
        dayLabel = label, fullDateLabel = label, highC = high, lowC = low, icon = code,
        phrase = null, sunrise = null, sunset = null, uvMax = null, precipProbMax = pop,
        windMaxKmh = wind, precipSumMm = null, tips = tips,
        dayHourly = feels.map { HourEntry("1 PM", it, code, isDay = true, precipChance = pop, feelsLikeC = it) }
    )

    private fun weather(
        temp: Int = 20,
        feels: Int? = 20,
        code: Int = 0,
        hourly: List<HourEntry> = emptyList(),
        daily: List<DayEntry> = emptyList(),
        alerts: List<WeatherAlert> = emptyList(),
        rain: List<Double> = emptyList(),
        snow: List<Double> = emptyList(),
        comparedToYesterday: String? = null
    ) = WeatherData(
        locationName = "Test City",
        currentTempC = temp, highTodayC = temp + 5, lowTodayC = temp - 5,
        realFeelC = feels, condition = "Clear sky", currentIcon = code,
        isDay = true, humidity = 50,
        windKmh = 10, windGustKmh = 10, windDir = "N", windUnit = "km/h",
        pressureHpa = 1013, cloudCoverPct = 0,
        precipMm = 0.0, precipUnit = "mm",
        uvIndex = null, uvLabel = null,
        visibility = 10, visibilityUnit = "km",
        aqi = null, aqiLabel = null,
        sunrise = "6:00 AM", sunset = "8:00 PM",
        headline = null, comparedToYesterday = comparedToYesterday,
        tips = emptyList(), weekMinC = 10, weekMaxC = 30,
        hourly = hourly,
        hourlyUv = emptyList(), hourlyWind = emptyList(), hourlyFeels = emptyList(),
        hourlyHumidity = emptyList(), hourlyVisibility = emptyList(),
        hourlyPressure = emptyList(), hourlyPrecipProb = emptyList(),
        hourlyAqi = emptyList(),
        daily = daily, alerts = alerts,
        hourlyPrecipAmount = rain, hourlySnowfall = snow
    )

    private fun section(w: WeatherData, title: String) =
        ForecastBriefing.build(w).firstOrNull { it.title == title }?.text

    @Test fun `temperature names the feels-like gap, the range ahead and yesterday`() {
        val text = section(
            weather(
                temp = 28, feels = 33, comparedToYesterday = "3° warmer than yesterday",
                hourly = listOf(hour("Now", 28), hour("1 PM", 31), hour("2 PM", 30), hour("9 PM", 22))
            ),
            "Temperature"
        )!!
        assertTrue(text, text.startsWith("It's 28° right now, but it feels warmer — more like 33°."))
        assertTrue(text, "peak near 31° around 1 PM and dip to about 22° around 9 PM" in text)
        assertTrue(text, text.endsWith("Today's high is 3° warmer than yesterday's."))
    }

    @Test fun `sky reports the next change`() {
        val text = section(
            weather(code = 2, hourly = listOf(hour("Now", 20, 2), hour("1 PM", 20, 2), hour("2 PM", 20, 3))),
            "Sky"
        )
        assertEquals("It's partly cloudy right now. Expect it to cloud over around 2 PM.", text)
    }

    @Test fun `dry outlook says so`() {
        val text = section(
            weather(hourly = listOf(hour("Now", 20, pop = 2), hour("1 PM", 20, pop = 4))),
            "Precipitation"
        )
        assertEquals("It looks dry for the next 2 hours — the chance of rain never gets above 4%.", text)
    }

    @Test fun `likely rain gives start, peak and amount`() {
        val text = section(
            weather(
                hourly = listOf(hour("Now", 20, pop = 10), hour("1 PM", 20, 61, pop = 60), hour("2 PM", 20, 63, pop = 85)),
                rain = listOf(0.0, 1.2, 3.1)
            ),
            "Precipitation"
        )
        assertEquals(
            "Rain is likely, starting around 1 PM and most likely around 2 PM (85%). " +
                "About 4.3 mm of rain is expected over that time.",
            text
        )
    }

    @Test fun `snow amounts make it snow, not rain`() {
        val text = section(
            weather(
                hourly = listOf(hour("Now", -2, pop = 20), hour("1 PM", -2, 73, pop = 70)),
                snow = listOf(0.0, 1.5)
            ),
            "Precipitation"
        )!!
        assertTrue(text, text.startsWith("Snow is likely, starting around 1 PM (70%)."))
        assertTrue(text, text.endsWith("About 1.5 cm of snow is expected over that time."))
    }

    @Test fun `falling now reports when it eases`() {
        val text = section(
            weather(
                code = 63,
                hourly = listOf(hour("Now", 15, 63, pop = 90), hour("1 PM", 15, 61, pop = 70), hour("2 PM", 15, 3, pop = 20))
            ),
            "Precipitation"
        )
        assertEquals("It's raining now and should ease off around 2 PM.", text)
    }

    @Test fun `alerts section only when there are alerts`() {
        assertNull(section(weather(), "Alerts"))
        val alert = WeatherAlert(
            id = "a1", event = "Air Quality Alert", severity = AlertSeverity.MODERATE, headline = "",
            description = "", instruction = null, areaDesc = null, senderName = null,
            effectiveLabel = null, expiresLabel = "8:00 PM", urgency = null, certainty = null
        )
        val text = section(weather(alerts = listOf(alert)), "Alerts")!!
        assertTrue(text, text.startsWith("An Air Quality Alert is in effect until 8:00 PM."))
    }

    @Test fun `looking ahead compares tomorrow with today and names the wettest later day`() {
        val text = section(
            weather(
                daily = listOf(
                    day("Today", 0, 15, 25, 5), day("Tue", 3, 14, 22, 40),
                    day("Wed", 63, 13, 20, 82), day("Thu", 61, 12, 19, 60)
                )
            ),
            "Looking ahead"
        )
        assertEquals(
            "Tomorrow looks overcast, ranging from 14° to 22°, with a 40% chance of rain. " +
                "That's about 3° cooler than today. Rain is more likely than today. " +
                "After that, Wed looks like the wettest day (82%).",
            text
        )
    }

    @Test fun `looking ahead - warmer, muggier, windier, with tomorrow's tip`() {
        val tip = WeatherTip("💧", "Hot conditions — stay hydrated, carry water, and take breaks in the shade.", TipTone.HOT)
        val text = section(
            weather(
                daily = listOf(
                    day("Today", 0, 18, 26, 0, wind = 12),
                    day("Tue", 1, 19, 31, 5, wind = 35, tips = listOf(tip), feels = listOf(30, 36))
                )
            ),
            "Looking ahead"
        )
        assertEquals(
            "Tomorrow looks clear, ranging from 19° to 31°. That's about 5° warmer than today. " +
                "It will feel muggier than the numbers suggest — closer to 36° at the peak. " +
                "It will be noticeably windier than today, with winds up to 35 km/h. " +
                "Hot conditions — stay hydrated, carry water, and take breaks in the shade.",
            text
        )
    }

    @Test fun `looking ahead - an unremarkable tomorrow stays short`() {
        val neutral = WeatherTip("🌤️", "No major weather concerns for this day.", TipTone.NEUTRAL)
        val text = section(
            weather(daily = listOf(day("Today", 2, 15, 22, 10, wind = 10), day("Tue", 2, 15, 23, 10, wind = 12, tips = listOf(neutral)))),
            "Looking ahead"
        )
        assertEquals("Tomorrow looks partly cloudy, ranging from 15° to 23°. Temperatures will be much the same as today.", text)
    }

    @Test fun `looking ahead - drier and colder nights`() {
        val text = section(
            weather(daily = listOf(day("Today", 63, 10, 16, 80), day("Tue", 3, 5, 16, 20))),
            "Looking ahead"
        )
        assertEquals(
            "Tomorrow looks overcast, ranging from 5° to 16°, with a 20% chance of rain. " +
                "Daytime will be much like today, but the night turns about 5° colder. It should be drier than today.",
            text
        )
    }
}
