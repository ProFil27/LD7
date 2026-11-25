package com.example.ld7;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/** Settings + notification logic shared by the Activity and the manifest receiver. */
public final class BatteryNotifier {
    public static final int LOW_LEVEL = 20;                  // % considered "low"
    private static final long INTERVAL_MS = 15 * 60 * 1000L; // at most one notification per 15 min
    private static final String CHANNEL_ID = "battery_low";
    private static final int NOTIFICATION_ID = 1;
    private static final String PREFS = "prefs";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_LAST = "last_notification";

    private BatteryNotifier() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (!enabled) NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
    }

    /** Shows the notification if tracking is on, battery is low and 15 min passed since the last one. */
    public static void notifyIfLow(Context context, int level, boolean charging) {
        if (!isEnabled(context) || charging || level > LOW_LEVEL) return;

        SharedPreferences prefs = prefs(context);
        long now = System.currentTimeMillis();
        if (now - prefs.getLong(KEY_LAST, 0) < INTERVAL_MS) return;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(
                new NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT));

        // Tapping the notification opens the app, where tracking can be turned off.
        PendingIntent pending = PendingIntent.getActivity(
                context, 0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_text, level))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build();

        manager.notify(NOTIFICATION_ID, notification);
        prefs.edit().putLong(KEY_LAST, now).apply();
    }
}
