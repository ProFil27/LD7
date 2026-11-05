package com.example.ld7

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var trackingSwitch: MaterialSwitch
    private lateinit var levelText: TextView
    private lateinit var statusText: TextView
    private lateinit var progress: LinearProgressIndicator

    private var receiverRegistered = false

    // Receives battery updates while tracking is enabled.
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            val percent = level * 100 / scale

            levelText.text = getString(R.string.level_format, percent)
            progress.setProgressCompat(percent, true)
            statusText.text = when {
                charging -> getString(R.string.status_charging)
                percent <= BatteryNotifier.LOW_LEVEL -> getString(R.string.status_low)
                else -> getString(R.string.status_ok)
            }
            BatteryNotifier.notifyIfLow(context, percent, charging)
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        trackingSwitch = findViewById(R.id.trackingSwitch)
        levelText = findViewById(R.id.levelText)
        statusText = findViewById(R.id.statusText)
        progress = findViewById(R.id.progress)

        trackingSwitch.isChecked = BatteryNotifier.isEnabled(this)
        trackingSwitch.setOnCheckedChangeListener { _, checked ->
            BatteryNotifier.setEnabled(this, checked)
            if (checked) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                startListening()
            } else {
                stopListening()
                showDisabled()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Re-read the setting: the user may arrive here from the notification.
        trackingSwitch.isChecked = BatteryNotifier.isEnabled(this)
        if (trackingSwitch.isChecked) startListening() else showDisabled()
    }

    override fun onStop() {
        super.onStop()
        stopListening()
    }

    private fun startListening() {
        if (receiverRegistered) return
        // ACTION_BATTERY_CHANGED is sticky: registering returns the current state immediately.
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        receiverRegistered = true
    }

    private fun stopListening() {
        if (!receiverRegistered) return
        unregisterReceiver(batteryReceiver)
        receiverRegistered = false
    }

    private fun showDisabled() {
        levelText.text = getString(R.string.level_unknown)
        statusText.text = getString(R.string.status_disabled)
        progress.setProgressCompat(0, false)
    }
}
