package com.example.ld7

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager

/**
 * Declared in the manifest: the system delivers ACTION_BATTERY_LOW even when the app
 * is not open. It does not wake the device - it only runs when the system broadcasts.
 */
class BatteryLowReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BATTERY_LOW) {
            val status = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            if (level >= 0) BatteryNotifier.notifyIfLow(context, level, charging = false)
        }
    }
}
