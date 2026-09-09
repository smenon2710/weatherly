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

    /**
     * `REPLACE`, not `KEEP` — deliberate, confirmed necessary via real-device testing.
     * `SettingsViewModel.syncScheduler()` calls this whenever *either* of the two independent
     * notification toggles is on, since both share one job. With `KEEP`, toggling one feature
     * off-and-back-on while the other stayed enabled was a silent no-op — work already existed
     * under this unique name, so the "new" request was discarded and the original ~30-minute
     * countdown just kept running untouched, with no way to force an earlier check short of
     * waiting it out. `REPLACE` guarantees every call actually cancels-and-re-enqueues fresh work,
     * which is also what gives freshly-(re)enabled work its near-immediate first run — the same
     * behavior a genuinely first-time enable already relied on.
     */
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<WeatherAlertWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
