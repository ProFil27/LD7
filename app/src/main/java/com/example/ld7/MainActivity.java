package com.example.ld7;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class MainActivity extends AppCompatActivity {

    private MaterialSwitch trackingSwitch;
    private TextView levelText;
    private TextView statusText;
    private LinearProgressIndicator progress;

    private boolean receiverRegistered = false;

    // Receives battery updates while tracking is enabled.
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
            int percent = level * 100 / scale;

            levelText.setText(getString(R.string.level_format, percent));
            progress.setProgressCompat(percent, true);
            if (charging) {
                statusText.setText(getString(R.string.status_charging));
            } else if (percent <= BatteryNotifier.LOW_LEVEL) {
                statusText.setText(getString(R.string.status_low));
            } else {
                statusText.setText(getString(R.string.status_ok));
            }
            BatteryNotifier.notifyIfLow(context, percent, charging);
        }
    };

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        trackingSwitch = findViewById(R.id.trackingSwitch);
        levelText = findViewById(R.id.levelText);
        statusText = findViewById(R.id.statusText);
        progress = findViewById(R.id.progress);

        trackingSwitch.setChecked(BatteryNotifier.isEnabled(this));
        trackingSwitch.setOnCheckedChangeListener((button, checked) -> {
            BatteryNotifier.setEnabled(this, checked);
            if (checked) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                startListening();
            } else {
                stopListening();
                showDisabled();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Re-read the setting: the user may arrive here from the notification.
        trackingSwitch.setChecked(BatteryNotifier.isEnabled(this));
        if (trackingSwitch.isChecked()) startListening(); else showDisabled();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopListening();
    }

    private void startListening() {
        if (receiverRegistered) return;
        // ACTION_BATTERY_CHANGED is sticky: registering returns the current state immediately.
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        receiverRegistered = true;
    }

    private void stopListening() {
        if (!receiverRegistered) return;
        unregisterReceiver(batteryReceiver);
        receiverRegistered = false;
    }

    private void showDisabled() {
        levelText.setText(getString(R.string.level_unknown));
        statusText.setText(getString(R.string.status_disabled));
        progress.setProgressCompat(0, false);
    }
}
