package com.example.weatherly.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Keeps at most one coroutine running: [launch] cancels whatever the previous call started. For
 * work where only the newest request matters — a forecast load for the place just picked, a city
 * search for the text just typed — so a slow earlier response can never land after a newer one
 * and overwrite it.
 */
class LatestJob(private val scope: CoroutineScope) {
    private var current: Job? = null

    fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        current?.cancel()
        // Started only after `current` points at it, so isCurrent() is already true for code the
        // block runs before its first suspension.
        val job = scope.launch(start = CoroutineStart.LAZY, block = block)
        current = job
        job.start()
        return job
    }

    fun cancel() {
        current?.cancel()
        current = null
    }

    /** True while [job] hasn't been superseded — a cancelled job's `finally` uses this to avoid
     * clearing state (a spinner, say) that the newer job now owns. */
    fun isCurrent(job: Job?): Boolean = job != null && job === current
}
