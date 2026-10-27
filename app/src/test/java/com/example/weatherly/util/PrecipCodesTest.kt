package com.example.weatherly.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrecipCodesTest {

    @Test
    fun `rain showers 80 to 82 are rain, not snow (R7)`() {
        for (code in 80..82) {
            assertTrue(isRainCode(code))
            assertFalse(isSnowCode(code))
        }
    }

    @Test
    fun `snow 71 to 77 and snow showers 85 to 86 are snow, not rain (R3)`() {
        for (code in listOf(71, 73, 75, 77, 85, 86)) {
            assertTrue(isSnowCode(code))
            assertFalse(isRainCode(code))
        }
    }

    @Test
    fun `drizzle, rain and freezing rain are rain`() {
        for (code in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67)) assertTrue(isRainCode(code))
    }

    @Test
    fun `label word follows forecast snowfall`() {
        assertEquals("rain", precipWord(null))
        assertEquals("rain", precipWord(0.0))
        assertEquals("snow", precipWord(1.2))
    }

    @Test
    fun `rain showers get the rain sky, not the snow sky (R7)`() {
        for (dark in listOf(false, true)) for (day in listOf(false, true)) {
            assertEquals(skyColor(63, day, dark), skyColor(81, day, dark))
            assertNotEquals(skyColor(73, day, dark), skyColor(81, day, dark))
        }
    }
}
