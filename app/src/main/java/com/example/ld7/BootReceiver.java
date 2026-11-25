package com.example.ld7;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                && MainActivity.isEnabled(context)) {
            ContextCompat.startForegroundService(context, new Intent(context, BatteryService.class));
        }
    }
}
