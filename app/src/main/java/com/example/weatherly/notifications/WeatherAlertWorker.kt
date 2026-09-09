package com.example.weatherly.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.weatherly.data.model.AlertSeverity
import com.example.weatherly.data.prefs.PreferencesStore
import com.example.weatherly.data.repository.AlertTracker
import com.example.weatherly.data.repository.WeatherRepository
import com.example.weatherly.location.LocationProvider

/**
 * Periodic background check (see [WeatherNotificationScheduler]) for new severe/extreme NWS
 * alerts and alerts that have since resolved, surfaced as system notifications — for the users
 * `NOTIFICATIONS_ROADMAP.md` is aimed at, who never open the app or place a widget.
 *
 * Location: **always** the device's live current location via [LocationProvider], regardless of
 * whatever place is selected/saved in-app — deliberately diverging from
 * [com.example.weatherly.ui.WeatherViewModel.load]'s foreground pattern (which does prefer a
 * selected place) per explicit user request: background notifications should reflect wherever
 * the user actually is, not whatever city happened to be last browsed in the app. Requires
 * `ACCESS_BACKGROUND_LOCATION` (Settings → Background Location) — without it, this worker simply
 * has no location to check and both notification features silently do nothing, since there's no
 * selected-place fallback anymore. This permission needs its own "Allow all the time" system flow
 * and, if this ever ships to Production, a Play Console background-location policy declaration
 * this app has never needed before — sideload-testing only for now.
 *
 * Uses its own [PreferencesStore.getBackgroundTrackedAlerts] slot rather than the foreground's
 * [PreferencesStore.getTrackedAlerts] — see that method's doc comment for why sharing one would
 * corrupt the diff when the in-app view and the background-tracked place differ.
 *
 * Also drives the ongoing "current conditions" status notification
 * ([PreferencesStore.getPersistentWeatherEnabled]) on the same fetch/interval — a separate opt-in
 * from the alert notifications above, but sharing this one job rather than scheduling a second
 * periodic worker, since both need the identical data for the identical place.
 */
class WeatherAlertWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesStore(applicationContext)
        val alertsEnabled = prefs.getAlertNotificationsEnabled()
        val statusEnabled = prefs.getPersistentWeatherEnabled()
        if (!alertsEnabled && !statusEnabled) return Result.success()

        if (!hasBackgroundLocationPermission(applicationContext)) return Result.success()
        val (lat, lon) = LocationProvider(applicationContext).currentLatLon() ?: return Result.success()

        val repository = WeatherRepository(applicationContext)
        val fetched = repository.getWeather(
            lat = lat, lon = lon, units = prefs.getUnitSystem()
        )
        val data = fetched.getOrNull() ?: return Result.retry()

        WeatherNotificationChannels.ensureCreated(applicationContext)

        if (alertsEnabled) {
            val tracker = AlertTracker(prefs::getBackgroundTrackedAlerts, prefs::setBackgroundTrackedAlerts)
            val diff = tracker.diffAndUpdate(data.alerts)
            diff.newlyAppeared
                .filter { it.severity == AlertSeverity.EXTREME || it.severity == AlertSeverity.SEVERE }
                .forEach { WeatherNotifier.notifySevereAlert(applicationContext, it) }
            diff.newlyResolved.forEach { WeatherNotifier.notifyAlertResolved(applicationContext, it) }
        }

        if (statusEnabled) {
            WeatherNotifier.notifyWeatherStatus(applicationContext, data)
        }

        return Result.success()
    }
}

/** `ACCESS_BACKGROUND_LOCATION` only exists from API 29 — below that, ordinary foreground
 * location permission already covers background access, so there's nothing separate to check. */
fun hasBackgroundLocationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) true
    else ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
