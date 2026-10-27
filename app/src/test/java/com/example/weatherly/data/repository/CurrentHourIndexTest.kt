package com.example.weatherly.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentHourIndexTest {

    // Two days of Open-Meteo-style local hour slots: 2026-10-27T00:00 .. 2026-10-28T23:00.
    private val hours = listOf("2026-10-27", "2026-10-28").flatMap { d ->
        (0..23).map { h -> "%sT%02d:00".format(d, h) }
    }

    private fun idx(current: String?) = WeatherRepository.currentHourIndex(hours, current)

    @Test
    fun `mid-hour current time picks the hour it falls in, not the next (R8)`() {
        assertEquals(21, idx("2026-10-27T21:30"))
        assertEquals(21, idx("2026-10-27T21:45"))
    }

    @Test
    fun `on the hour picks that hour`() {
        assertEquals(21, idx("2026-10-27T21:00"))
    }

    @Test
    fun `late evening stays on today, so today's remaining hours aren't tomorrow's (R8a)`() {
        val i = idx("2026-10-27T23:30")
        assertEquals(23, i)
        assertTrue(hours[i].startsWith("2026-10-27"))
        // The next slot is the day boundary the "12 AM" label marks.
        assertEquals("2026-10-28T00:00", hours[i + 1])
    }

    @Test
    fun `missing or out-of-range current time falls back to the first slot`() {
        assertEquals(0, idx(null))
        assertEquals(0, idx("2026-10-26T23:30"))
    }
}
