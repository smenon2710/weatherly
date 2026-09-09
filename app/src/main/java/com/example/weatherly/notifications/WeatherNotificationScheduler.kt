package com.example.weatherly.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Starts/stops [WeatherAlertWorker]'s periodic check — called from
 * `SettingsViewModel.setAlertNotificationsEnabled` when the user toggles the feature. `WorkManager`
 * persists a scheduled `PeriodicWorkRequest` across process death and reboot on its own, so no
 * `BOOT_COMPLETED` receiver is needed to keep this running.
 *
 * 30 minutes matches `WeatherRepository`'s existing forecast-cache TTL — a natural anchor rather
 * than an arbitrary pick — though real-world delivery will run coarser than that for
 * infrequently-opened apps once Android's Doze/App Standby Bucket throttling kicks in; see
 * `NOTIFICATIONS_ROADMAP.md`'s polling-vs-push section for why that's a known, accepted v1
 * trade-off rather than a bug to chase.
 */
object WeatherNotificationScheduler {
    private const val UNIQUE_WORK_NAME = "weather_alert_check"
    private const val INTERVAL_MINUTES = 30L

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<WeatherAlertWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
