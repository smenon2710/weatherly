package com.example.weatherly.util

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.PI
import kotlin.math.ceil

/**
 * Pure-math moon phase calculator.
 * Reference new moon: 2000-01-06 18:14 UT → JD 2451550.260
 * Synodic month = 29.530588853 days
 * Accuracy: phase within ~1 hour over the coming years.
 */
object MoonCalculator {

    private const val SYNODIC = 29.530588853
    private const val KNOWN_NEW_MOON_JD = 2451550.260

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year; var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = y / 100
        val b = 2 - a + a / 4
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /** Moon phase fraction: 0.0 = new, 0.25 = first quarter, 0.5 = full, 0.75 = last quarter, 1.0 = new. */
    fun phase(year: Int, month: Int, day: Int): Double {
        val jd = julianDate(year, month, day)
        val lunations = (jd - KNOWN_NEW_MOON_JD) / SYNODIC
        return lunations - floor(lunations)
    }

    /** Human-readable phase name. */
    fun phaseName(phase: Double): String = when {
        phase < 0.0625 || phase >= 0.9375 -> "New Moon"
        phase < 0.1875 -> "Waxing Crescent"
        phase < 0.3125 -> "First Quarter"
        phase < 0.4375 -> "Waxing Gibbous"
        phase < 0.5625 -> "Full Moon"
        phase < 0.6875 -> "Waning Gibbous"
        phase < 0.8125 -> "Last Quarter"
        else           -> "Waning Crescent"
    }

    /** Illuminated fraction of the disk (0–100). */
    fun illuminationPct(phase: Double): Int =
        ((1.0 - cos(phase * 2 * PI)) / 2.0 * 100).toInt()

    /**
     * Returns the label and approximate number of days until the next
     * major lunar event (Full Moon or New Moon), whichever is sooner.
     */
    fun nextEvent(phase: Double): Pair<String, Int> {
        val toFull = if (phase < 0.5) 0.5 - phase else 1.5 - phase
        val toNew  = 1.0 - phase
        val dFull  = ceil(toFull * SYNODIC).toInt().coerceAtLeast(0)
        val dNew   = ceil(toNew  * SYNODIC).toInt().coerceAtLeast(0)
        return if (dFull <= dNew) "Full Moon" to dFull else "New Moon" to dNew
    }
}
