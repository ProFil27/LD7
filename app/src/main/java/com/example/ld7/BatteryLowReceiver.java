package com.example.ld7;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

/**
 * Declared in the manifest: the system delivers ACTION_BATTERY_LOW even when the app
 * is not open. It does not wake the device - it only runs when the system broadcasts.
 */
public class BatteryLowReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
            Intent status = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = status != null ? status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            if (level >= 0) BatteryNotifier.notifyIfLow(context, level, false);
        }
    }
}
