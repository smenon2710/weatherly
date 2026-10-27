package com.example.weatherly.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LatestJobTest {

    @Test
    fun `a slow earlier request can't overwrite a newer one`() = runBlocking {
        val latest = LatestJob(this)
        var shown = ""
        val slowFirst = CompletableDeferred<String>()
        val first = latest.launch { shown = slowFirst.await() }
        yield() // first is now waiting on its response
        latest.launch { shown = "Paris" }
        slowFirst.complete("London") // the older response arrives last
        yield()
        assertEquals("Paris", shown)
        assertTrue(first.isCancelled)
    }

    @Test
    fun `a superseded job is no longer current, the newest one is`() = runBlocking {
        val latest = LatestJob(this)
        var firstWasCurrent: Boolean? = null
        val gate = CompletableDeferred<Unit>()
        latest.launch {
            try { gate.await() } finally { firstWasCurrent = latest.isCurrent(coroutineContext[Job]) }
        }
        yield()
        var secondIsCurrent = false
        latest.launch { secondIsCurrent = latest.isCurrent(coroutineContext[Job]) }
        yield()
        assertEquals(false, firstWasCurrent)
        assertTrue(secondIsCurrent)
    }

    @Test
    fun `cancel stops the running job`() = runBlocking {
        val latest = LatestJob(this)
        val job = latest.launch { CompletableDeferred<Unit>().await() }
        latest.cancel()
        yield()
        assertTrue(job.isCancelled)
        assertFalse(latest.isCurrent(job))
    }
}
