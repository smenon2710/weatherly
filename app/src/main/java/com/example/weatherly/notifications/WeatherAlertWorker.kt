package com.example.weatherly.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.weatherly.data.model.AlertSeverity
import com.example.weatherly.data.prefs.PreferencesStore
import com.example.weatherly.data.repository.AlertTracker
import com.example.weatherly.data.repository.WeatherRepository

/**
 * Periodic background check (see [WeatherNotificationScheduler]) for new severe/extreme NWS
 * alerts and alerts that have since resolved, surfaced as system notifications — for the users
 * `NOTIFICATIONS_ROADMAP.md` is aimed at, who never open the app or place a widget.
 *
 * Deliberately scoped to a saved/selected place ([PreferencesStore.getSelected]) only — **never**
 * the device's live "current location" — so this worker never touches location from a background
 * context at all, sidestepping `ACCESS_BACKGROUND_LOCATION` and the heavier Play Console review
 * it requires entirely (see the roadmap doc's "Avoid the background-location review trap"). A
 * user relying on "current location" mode (the default for a fresh install) simply doesn't get
 * background alerts yet — a known, deliberate v1 limitation, not an oversight. Extending this to
 * device location is a separate decision given the real review-surface cost.
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

        val place = prefs.getSelected() ?: return Result.success()

        val repository = WeatherRepository(applicationContext)
        val fetched = repository.getWeather(
            lat = place.lat, lon = place.lon, units = prefs.getUnitSystem(), placeName = place.name
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
