package com.example.weatherly.data.repository

import com.example.weatherly.data.model.AlertSeverity
import com.example.weatherly.data.model.TrackedAlert
import com.example.weatherly.data.model.WeatherAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
