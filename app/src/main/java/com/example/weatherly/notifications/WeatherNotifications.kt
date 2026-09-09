package com.example.weatherly.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.weatherly.MainActivity
import com.example.weatherly.R
import com.example.weatherly.data.model.TrackedAlert
import com.example.weatherly.data.model.WeatherAlert
import com.example.weatherly.data.model.WeatherData

/**
 * Whether the OS currently exempts this app from Doze/App Standby battery restrictions. Not
 * required for the notification features to work — WorkManager's periodic job runs regardless —
 * but OEM battery managers (Samsung, Xiaomi, and others) are well documented to kill or throttle
 * background work far more aggressively than stock Android's own Doze behavior, independent of
 * anything this app does correctly. Exempting the app materially improves real-world delivery
 * reliability on those devices; see [batteryOptimizationSettingsIntent] for the one-tap request.
 */
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

/** Direct system dialog asking the user to exempt this app from battery optimization — one tap,
 * no navigating through system Settings manually. Requires the normal
 * `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` manifest permission (declared, no runtime prompt of its
 * own); the dialog this intent triggers is the actual user-facing confirmation. */
fun batteryOptimizationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))

/**
 * Notification channels for the background weather-alert feature (see [WeatherAlertWorker]) —
 * one per type, so a user can mute "alert resolved" acknowledgments while keeping severe alerts
 * on, entirely through the system channel settings, without this app needing its own granular
 * settings UI to do that work. `minSdk 26` already meets `NotificationChannel`'s own API floor
 * (26), so no version guard is needed here.
 */
object WeatherNotificationChannels {
    const val SEVERE_ALERTS = "severe_alerts"
    const val ALERTS_RESOLVED = "alerts_resolved"
    const val WEATHER_STATUS = "weather_status"

    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(SEVERE_ALERTS, "Severe weather alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "New severe or extreme National Weather Service advisories for your saved location."
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(ALERTS_RESOLVED, "Alert resolved", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Lets you know when a previously active weather alert has ended."
            }
        )
        // LOW, not DEFAULT/HIGH: this one updates itself silently on every periodic check
        // (WeatherNotifier.notifyWeatherStatus) — anything above LOW would heads-up/alert the
        // user roughly every 30 minutes, which is the opposite of "ambient".
        manager.createNotificationChannel(
            NotificationChannel(WEATHER_STATUS, "Weather status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "An ongoing notification showing current conditions for your saved location, updated periodically."
                setShowBadge(false)
            }
        )
    }
}

/**
 * Posts the two notification types [WeatherAlertWorker] can trigger. Both quietly no-op if
 * `POST_NOTIFICATIONS` isn't granted (API 33+ only — see
 * `PreferencesStore.getAlertNotificationsEnabled`'s doc comment) rather than crashing; the
 * Settings toggle is what actually requests the permission, so a denial here just means the
 * user's already-made choice is respected rather than surfacing a runtime exception.
 *
 * `@SuppressLint("MissingPermission")`: every `notify()` call here is guarded by [hasPermission]
 * immediately above it, but that guard lives in a separate private function — lint's flow
 * analysis only recognizes an inline `checkSelfPermission(...) == PERMISSION_GRANTED` check at
 * the exact call site, not one performed through an extracted helper, so it flags all three calls
 * as unchecked even though they aren't. Confirmed the real guard works correctly via on-device
 * testing (both debug and R8-minified release builds) rather than just suppressing blind.
 */
@SuppressLint("MissingPermission")
object WeatherNotifier {

    fun notifySevereAlert(context: Context, alert: WeatherAlert) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, WeatherNotificationChannels.SEVERE_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(alert.event)
            .setContentText(alert.headline)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.headline))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(alert.id.hashCode(), notification)
    }

    fun notifyAlertResolved(context: Context, resolved: TrackedAlert) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, WeatherNotificationChannels.ALERTS_RESOLVED)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${resolved.event} has ended")
            .setContentText("This advisory is no longer active.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(resolved.id.hashCode(), notification)
    }

    // Fixed, arbitrary id — every call re-posts to this same id so the notification updates in
    // place each periodic check instead of stacking a new one every ~30 minutes.
    private const val WEATHER_STATUS_NOTIFICATION_ID = 778821

    /** Posts (or updates) the ongoing "current conditions" notification. `setOngoing(true)`
     * makes it non-swipeable — a deliberate choice (see NOTIFICATIONS_ROADMAP.md) over a normal
     * dismissible notification that would just silently reappear at the next check anyway;
     * turning the Settings toggle off is what actually removes it, via [cancelWeatherStatus].
     * `setOnlyAlertOnce(true)` is a second belt-and-suspenders guard (alongside the LOW-importance
     * channel) against any sound/vibration/heads-up on the periodic updates. */
    fun notifyWeatherStatus(context: Context, data: WeatherData) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, WeatherNotificationChannels.WEATHER_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${data.currentTempC}° · ${data.condition}")
            .setContentText("${data.locationName} · H:${data.highTodayC}° L:${data.lowTodayC}°")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setContentIntent(contentIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(WEATHER_STATUS_NOTIFICATION_ID, notification)
    }

    /** Removes the ongoing weather-status notification — called when the Settings toggle turns
     * it off, since [notifyWeatherStatus]'s `setOngoing(true)` means the user can't swipe it away
     * themselves. */
    fun cancelWeatherStatus(context: Context) {
        NotificationManagerCompat.from(context).cancel(WEATHER_STATUS_NOTIFICATION_ID)
    }

    private fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
