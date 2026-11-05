package com.example.ld7

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Settings + notification logic shared by the Activity and the manifest receiver. */
object BatteryNotifier {
    const val LOW_LEVEL = 20                       // % considered "low"
    private const val INTERVAL_MS = 15 * 60 * 1000L // at most one notification per 15 min
    private const val CHANNEL_ID = "battery_low"
    private const val NOTIFICATION_ID = 1
    private const val PREFS = "prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LAST = "last_notification"

    fun isEnabled(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled).apply()
        if (!enabled) NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /** Shows the notification if tracking is on, battery is low and 15 min passed since the last one. */
    fun notifyIfLow(context: Context, level: Int, charging: Boolean) {
        if (!isEnabled(context) || charging || level > LOW_LEVEL) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST, 0) < INTERVAL_MS) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_DEFAULT)
        )

        // Tapping the notification opens the app, where tracking can be turned off.
        val pending = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text, level))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
        prefs.edit().putLong(KEY_LAST, now).apply()
    }
}
