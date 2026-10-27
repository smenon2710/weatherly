package com.example.weatherly.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindUnitsTest {

    @Test
    fun `km per h passes through, mph is converted`() {
        assertEquals(30f, windToKmh(30f, "km/h"), 0.01f)
        assertEquals(48.28f, windToKmh(30f, "mph"), 0.01f)
    }

    @Test
    fun `same gust gives the same advisory in both unit systems (R2)`() {
        // 39 mph is about 63 km/h: used to read "Breezy" for imperial users.
        assertEquals(windAdvisory(63f), windAdvisory(windToKmh(39f, "mph")))
        assertEquals("Strong gusts", windAdvisory(windToKmh(39f, "mph")))
        // Each band boundary, approached from both units.
        for (kmh in listOf(5, 15, 30, 50, 70)) {
            val mph = kmh / 1.609344f
            assertEquals(windAdvisory(kmh.toFloat()), windAdvisory(windToKmh(mph, "mph")))
        }
    }

    @Test
    fun `wind-streak threshold is 45 km per h, about 28 mph, not 45 mph (R2)`() {
        assertTrue(isSevereWind(windToKmh(29f, "mph")))
        assertFalse(isSevereWind(windToKmh(27f, "mph")))
        assertTrue(isSevereWind(windToKmh(45f, "km/h")))
        assertFalse(isSevereWind(windToKmh(44f, "km/h")))
    }
}
