package com.example.weatherly.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.weatherly.MainActivity
import com.example.weatherly.R
import com.example.weatherly.data.model.TrackedAlert
import com.example.weatherly.data.model.WeatherAlert

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
    }
}

/**
 * Posts the two notification types [WeatherAlertWorker] can trigger. Both quietly no-op if
 * `POST_NOTIFICATIONS` isn't granted (API 33+ only — see
 * `PreferencesStore.getAlertNotificationsEnabled`'s doc comment) rather than crashing; the
 * Settings toggle is what actually requests the permission, so a denial here just means the
 * user's already-made choice is respected rather than surfacing a runtime exception.
 */
object WeatherNotifier {

    fun notifySevereAlert(context: Context, alert: WeatherAlert) {
        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, WeatherNotificationChannels.SEVERE_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("${resolved.event} has ended")
            .setContentText("This advisory is no longer active.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(resolved.id.hashCode(), notification)
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
