package com.example.weatherly.data.advice

import com.example.weatherly.data.model.DayEntry
import com.example.weatherly.data.model.HourEntry
import com.example.weatherly.data.model.TipTone
import com.example.weatherly.data.model.UnitSystem
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.data.model.WeatherTip
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherAdvisorTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun weather(
        temp: Int = 20,
        feels: Int = 20,
        wind: Int = 10,
        gusts: Int = 10,
        code: Int = 0,
        aqi: Int? = null,
        uv: Int? = null,
        isDay: Boolean = true,
        hourly: List<HourEntry> = emptyList(),
        daily: List<DayEntry> = emptyList()
    ) = WeatherData(
        locationName = "Test City",
        currentTempC = temp, highTodayC = temp + 5, lowTodayC = temp - 5,
        realFeelC = feels, condition = "Clear sky", currentIcon = code,
        isDay = isDay, humidity = 50,
        windKmh = wind, windGustKmh = gusts, windDir = "N", windUnit = "km/h",
        pressureHpa = 1013, cloudCoverPct = 0,
        precipMm = 0.0, precipUnit = "mm",
        uvIndex = uv, uvLabel = null,
        visibility = 10, visibilityUnit = "km",
        aqi = aqi, aqiLabel = null,
        sunrise = "6:00 AM", sunset = "8:00 PM",
        headline = null, comparedToYesterday = null,
        tips = emptyList(), weekMinC = 10, weekMaxC = 30,
        hourly = hourly,
        hourlyUv = emptyList(), hourlyWind = emptyList(), hourlyFeels = emptyList(),
        hourlyHumidity = emptyList(), hourlyVisibility = emptyList(),
        hourlyPressure = emptyList(), hourlyPrecipProb = emptyList(),
        hourlyAqi = emptyList(),
        daily = daily
    )

    private fun advise(intent: AdviceIntent, w: WeatherData = weather()) =
        WeatherAdvisor.advise(intent, w, UnitSystem.METRIC)

    // -------------------------------------------------------------------------
    // Umbrella
    // -------------------------------------------------------------------------

    @Test fun `umbrella - heavy rain code returns yes`() {
        assertTrue(advise(AdviceIntent.UMBRELLA, weather(code = 63)).startsWith("Yes"))
    }

    @Test fun `umbrella - drizzle code returns yes`() {
        assertTrue(advise(AdviceIntent.UMBRELLA, weather(code = 51)).startsWith("Yes"))
    }

    @Test fun `umbrella - showers code returns yes`() {
        assertTrue(advise(AdviceIntent.UMBRELLA, weather(code = 80)).startsWith("Yes"))
    }

    @Test fun `umbrella - 50 percent rain chance returns maybe`() {
        val w = weather(
            daily = listOf(
                DayEntry("Today", "Today", 25, 15, 0, "Clear", null, null, null, 50, null, null)
            )
        )
        assertTrue(advise(AdviceIntent.UMBRELLA, w).contains("Maybe"))
    }

    @Test fun `umbrella - clear sky with low rain chance says probably not`() {
        assertTrue(advise(AdviceIntent.UMBRELLA, weather(code = 0)).contains("Probably not"))
    }

    @Test fun `umbrella - thunderstorm warns about wind`() {
        assertTrue(advise(AdviceIntent.UMBRELLA, weather(code = 95)).contains("Thunderstorm"))
    }

    // -------------------------------------------------------------------------
    // Jacket
    // -------------------------------------------------------------------------

    @Test fun `jacket - freezing feels-like recommends heavy coat`() {
        assertTrue(advise(AdviceIntent.JACKET, weather(feels = -5)).contains("heavy coat"))
    }

    @Test fun `jacket - cold feels-like recommends warm jacket`() {
        assertTrue(advise(AdviceIntent.JACKET, weather(feels = 5)).contains("warm jacket"))
    }

    @Test fun `jacket - mild feels-like recommends layers`() {
        assertTrue(advise(AdviceIntent.JACKET, weather(feels = 12)).contains("layers"))
    }

    @Test fun `jacket - warm feels-like says no jacket needed`() {
        assertTrue(advise(AdviceIntent.JACKET, weather(feels = 25)).contains("no jacket"))
    }

    @Test fun `jacket - high rain adds waterproof suggestion`() {
        val w = weather(
            feels = 18,
            daily = listOf(
                DayEntry("Today", "Today", 23, 15, 0, "Clear", null, null, null, 60, null, null)
            )
        )
        assertTrue(advise(AdviceIntent.JACKET, w).contains("water-resistant"))
    }

    // -------------------------------------------------------------------------
    // Walking / jogging
    // -------------------------------------------------------------------------

    @Test fun `walking - thunderstorm blocks walk`() {
        assertTrue(advise(AdviceIntent.WALKING, weather(code = 95)).contains("hold off"))
    }

    @Test fun `walking - clear mild conditions return positive`() {
        assertTrue(advise(AdviceIntent.WALKING, weather(temp = 20, feels = 20, code = 0)).contains("Good conditions"))
    }

    @Test fun `walking - very hot warns about heat`() {
        assertTrue(advise(AdviceIntent.WALKING, weather(temp = 35, feels = 38)).contains("hot"))
    }

    @Test fun `walking - poor air quality warns`() {
        assertTrue(advise(AdviceIntent.WALKING, weather(aqi = 160)).contains("air quality"))
    }

    // -------------------------------------------------------------------------
    // Driving
    // -------------------------------------------------------------------------

    @Test fun `driving - clear conditions are fine`() {
        assertTrue(advise(AdviceIntent.DRIVING, weather(code = 0)).contains("fine for driving"))
    }

    @Test fun `driving - fog triggers visibility warning`() {
        assertTrue(advise(AdviceIntent.DRIVING, weather(code = 45)).contains("visibility"))
    }

    @Test fun `driving - snow triggers slippery warning`() {
        assertTrue(advise(AdviceIntent.DRIVING, weather(code = 73)).contains("slippery"))
    }

    @Test fun `driving - thunderstorm triggers warning`() {
        assertFalse(advise(AdviceIntent.DRIVING, weather(code = 95)).contains("fine for driving"))
    }

    // -------------------------------------------------------------------------
    // Hiking
    // -------------------------------------------------------------------------

    @Test fun `hiking - thunderstorm blocks hike`() {
        assertTrue(advise(AdviceIntent.HIKING, weather(code = 95)).contains("Not a good day"))
    }

    @Test fun `hiking - clear mild day gives positive response`() {
        assertTrue(advise(AdviceIntent.HIKING, weather(temp = 22, feels = 22, code = 0)).contains("Great day"))
    }

    @Test fun `hiking - high UV warns`() {
        assertTrue(advise(AdviceIntent.HIKING, weather(uv = 9, isDay = true)).contains("UV"))
    }

    // -------------------------------------------------------------------------
    // Clothing
    // -------------------------------------------------------------------------

    @Test fun `clothing - below zero recommends bundle up`() {
        assertTrue(advise(AdviceIntent.CLOTHING, weather(feels = -3)).contains("Bundle up"))
    }

    @Test fun `clothing - warm weather recommends light clothing`() {
        assertTrue(advise(AdviceIntent.CLOTHING, weather(feels = 30)).contains("light"))
    }

    // -------------------------------------------------------------------------
    // Imperial unit handling
    // -------------------------------------------------------------------------

    @Test fun `imperial - hot threshold fires at 86F`() {
        val reply = WeatherAdvisor.advise(
            AdviceIntent.WALKING,
            weather(temp = 86, feels = 86),
            UnitSystem.IMPERIAL
        )
        assertTrue(reply.contains("hot"))
    }

    @Test fun `imperial - cold threshold fires at 40F`() {
        val reply = WeatherAdvisor.advise(
            AdviceIntent.JACKET,
            weather(temp = 40, feels = 40),
            UnitSystem.IMPERIAL
        )
        assertTrue(reply.contains("jacket") || reply.contains("coat"))
    }

    // -------------------------------------------------------------------------
    // matchIntent — local-first chat routing
    // -------------------------------------------------------------------------

    @Test fun `matchIntent - umbrella question matches UMBRELLA`() {
        assertTrue(WeatherAdvisor.matchIntent("should I bring an umbrella today?") == AdviceIntent.UMBRELLA)
    }

    @Test fun `matchIntent - jacket question matches JACKET`() {
        assertTrue(WeatherAdvisor.matchIntent("do I need a jacket?") == AdviceIntent.JACKET)
    }

    @Test fun `matchIntent - coat also matches JACKET`() {
        assertTrue(WeatherAdvisor.matchIntent("is a coat necessary?") == AdviceIntent.JACKET)
    }

    @Test fun `matchIntent - jog matches WALKING`() {
        assertTrue(WeatherAdvisor.matchIntent("good weather for a jog?") == AdviceIntent.WALKING)
    }

    @Test fun `matchIntent - driving question matches DRIVING`() {
        assertTrue(WeatherAdvisor.matchIntent("is it safe for driving right now?") == AdviceIntent.DRIVING)
    }

    @Test fun `matchIntent - hiking question matches HIKING`() {
        assertTrue(WeatherAdvisor.matchIntent("is today good for hiking?") == AdviceIntent.HIKING)
    }

    @Test fun `matchIntent - what to wear matches CLOTHING`() {
        assertTrue(WeatherAdvisor.matchIntent("what should I wear today?") == AdviceIntent.CLOTHING)
    }

    @Test fun `matchIntent - is case-insensitive`() {
        assertTrue(WeatherAdvisor.matchIntent("UMBRELLA today?") == AdviceIntent.UMBRELLA)
    }

    @Test fun `matchIntent - unrelated question falls through to null (LLM)`() {
        assertTrue(WeatherAdvisor.matchIntent("what's the capital of France?") == null)
    }

    @Test fun `matchIntent - empty text falls through to null`() {
        assertTrue(WeatherAdvisor.matchIntent("   ") == null)
    }

    @Test fun `matchIntent - long compound question falls through to null even with a keyword`() {
        val longQuestion = "should I bring an umbrella today or just reschedule my " +
            "afternoon plans given how the rest of this week looks so far"
        assertTrue(WeatherAdvisor.matchIntent(longQuestion) == null)
    }

    @Test fun `matchIntent - short umbrella question with extra words still matches`() {
        assertTrue(WeatherAdvisor.matchIntent("hey, umbrella needed today?") == AdviceIntent.UMBRELLA)
    }
}
