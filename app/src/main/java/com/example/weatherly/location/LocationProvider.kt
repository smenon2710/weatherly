package com.example.weatherly.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Thin wrapper around FusedLocationProvider that returns a one-shot location. */
class LocationProvider(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Caller must hold ACCESS_COARSE_LOCATION (or FINE).
     *
     * Tries a fresh fix first; if that comes back null (common on emulators,
     * or right after a cold start), falls back to the last known location,
     * which is what the emulator's "Set Location" control populates.
     * Returns null only if neither is available.
     */
    @SuppressLint("MissingPermission")
    suspend fun currentLatLon(): Pair<Double, Double>? {
        val fresh = client
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .awaitOrNull()
        if (fresh != null) return fresh.latitude to fresh.longitude

        val last = client.lastLocation.awaitOrNull()
        return last?.let { it.latitude to it.longitude }
    }

    /** Await a Google Play Services Task, resolving to null on failure. */
    private suspend fun Task<Location>.awaitOrNull(): Location? =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.resume(it) }
            addOnFailureListener { cont.resume(null) }
        }
}
