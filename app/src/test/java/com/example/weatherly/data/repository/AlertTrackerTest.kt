package com.example.weatherly.data.repository

import com.example.weatherly.data.model.AlertSeverity
import com.example.weatherly.data.model.TrackedAlert
import com.example.weatherly.data.model.WeatherAlert
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AlertTrackerTest {

    private var stored: List<TrackedAlert> = emptyList()
    private val tracker = AlertTracker({ stored }, { stored = it })

    private fun alert(id: String, event: String) = WeatherAlert(
        id = id, event = event, severity = AlertSeverity.SEVERE, headline = event,
        description = "", instruction = null, areaDesc = null, senderName = null,
        effectiveLabel = null, expiresLabel = null, urgency = null, certainty = null
    )

    @Test
    fun `first sighting is reported as new and stored`() {
        val diff = tracker.diffAndUpdate(listOf(alert("a1", "Flood Warning")))
        assertEquals(listOf("a1"), diff.newlyAppeared.map { it.id })
        assertTrue(diff.newlyResolved.isEmpty())
        assertEquals(listOf(TrackedAlert("a1", "Flood Warning")), stored)
    }

    @Test
    fun `unchanged alert is neither new nor resolved`() {
        tracker.diffAndUpdate(listOf(alert("a1", "Flood Warning")))
        val diff = tracker.diffAndUpdate(listOf(alert("a1", "Flood Warning")))
        assertTrue(diff.newlyAppeared.isEmpty())
        assertTrue(diff.newlyResolved.isEmpty())
    }

    @Test
    fun `NWS update under a new id is neither new nor resolved, and the stored id follows it`() {
        tracker.diffAndUpdate(listOf(alert("a1", "Flood Warning")))
        val diff = tracker.diffAndUpdate(listOf(alert("a2", "Flood Warning")))
        assertTrue(diff.newlyAppeared.isEmpty())
        assertTrue(diff.newlyResolved.isEmpty())
        assertEquals(listOf(TrackedAlert("a2", "Flood Warning")), stored)
    }

    @Test
    fun `alert that disappears is reported as resolved`() {
        tracker.diffAndUpdate(listOf(alert("a1", "Flood Warning")))
        val diff = tracker.diffAndUpdate(emptyList())
        assertEquals(listOf(TrackedAlert("a1", "Flood Warning")), diff.newlyResolved)
        assertTrue(stored.isEmpty())
    }

    @Test
    fun `a different event replacing the old one is one new and one resolved`() {
        tracker.diffAndUpdate(listOf(alert("a1", "Tornado Watch")))
        val diff = tracker.diffAndUpdate(listOf(alert("b1", "Tornado Warning")))
        assertEquals(listOf("b1"), diff.newlyAppeared.map { it.id })
        assertEquals(listOf("a1"), diff.newlyResolved.map { it.id })
    }

    @Test
    fun `failed check reports nothing and keeps the tracked set (R1)`() {
        tracker.diffAndUpdate(listOf(alert("a1", "Flood Warning")))
        val diff = tracker.diffAndUpdate(null)
        assertTrue(diff.newlyAppeared.isEmpty())
        assertTrue(diff.newlyResolved.isEmpty())
        assertEquals(listOf(TrackedAlert("a1", "Flood Warning")), stored)
    }

    @Test
    fun `alert still active after a failed check is not re-announced (R1)`() {
        tracker.diffAndUpdate(listOf(alert("a1", "Flood Warning")))
        tracker.diffAndUpdate(null)
        val diff = tracker.diffAndUpdate(listOf(alert("a2", "Flood Warning")))
        assertTrue(diff.newlyAppeared.isEmpty())
        assertTrue(diff.newlyResolved.isEmpty())
    }

    private fun httpError(code: Int) =
        HttpException(Response.error<Any>(code, "".toResponseBody(null)))

    @Test
    fun `successful NWS fetch counts as checked, even when empty`() {
        assertTrue(WeatherRepository.alertsChecked(Result.success(Unit)))
    }

    @Test
    fun `NWS out-of-coverage 400 counts as checked`() {
        assertTrue(WeatherRepository.alertsChecked(Result.failure<Unit>(httpError(400))))
    }

    @Test
    fun `NWS timeout or server error does not count as checked`() {
        assertFalse(WeatherRepository.alertsChecked(Result.failure<Unit>(IOException("timeout"))))
        assertFalse(WeatherRepository.alertsChecked(Result.failure<Unit>(httpError(503))))
    }
}
