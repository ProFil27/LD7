package com.example.ld7;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

public class BatteryService extends Service {
    public static final String ACTION_CHECK = "com.example.ld7.ACTION_CHECK";

    private static final int LOW_LEVEL = 20;
    private static final long INTERVAL_MS = 15 * 60 * 1000L;
    private static final String CHANNEL_LOW = "battery_low";
    private static final String CHANNEL_MONITOR = "battery_monitor";
    private static final int ID_LOW = 1;
    private static final int ID_MONITOR = 2;

    private int percent = 100;
    private boolean low = false;

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            if (level < 0 || scale <= 0) return;
            percent = level * 100 / scale;

            if (percent > LOW_LEVEL) {
                if (low) {              // recovered: stop repeating, next drop notifies again
                    low = false;
                    alarms().cancel(checkIntent());
                }
            } else if (!low) {          // just dropped: notify now and start the 15 min repeat
                low = true;
                notifyLow();
                scheduleCheck();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        // BATTERY_CHANGED is sticky, so this also delivers the current level immediately.
        ContextCompat.registerReceiver(this, batteryReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL_MONITOR,
                getString(R.string.monitor_channel_name), NotificationManager.IMPORTANCE_MIN));
        Notification ongoing = new NotificationCompat.Builder(this, CHANNEL_MONITOR)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.monitor_text))
                .setContentIntent(openApp())
                .setOngoing(true)
                .build();
        ServiceCompat.startForeground(this, ID_MONITOR, ongoing,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

        if (intent != null && ACTION_CHECK.equals(intent.getAction()) && low) {
            notifyLow();
            scheduleCheck();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(batteryReceiver);
        alarms().cancel(checkIntent());
        getSystemService(NotificationManager.class).cancel(ID_LOW);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void notifyLow() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL_LOW,
                getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT));
        manager.notify(ID_LOW, new NotificationCompat.Builder(this, CHANNEL_LOW)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(openApp())
                .setAutoCancel(true)
                .build());
    }

    private PendingIntent openApp() {
        return PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private AlarmManager alarms() {
        return getSystemService(AlarmManager.class);
    }

    private PendingIntent checkIntent() {
        return PendingIntent.getForegroundService(this, 0,
                new Intent(this, BatteryService.class).setAction(ACTION_CHECK),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Fires in 15 min even in Doze (the system may shift it slightly). */
    private void scheduleCheck() {
        alarms().setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + INTERVAL_MS, checkIntent());
    }
}
