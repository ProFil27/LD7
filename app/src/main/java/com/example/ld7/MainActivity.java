package com.example.ld7;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "prefs";
    private static final String KEY_ENABLED = "enabled";

    private MaterialSwitch trackingSwitch;

    // Once the permission dialog is answered, ask the service to re-check: the battery may
    // already be low, and the notification could not be shown before.
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (isEnabled(this)) {
                    ContextCompat.startForegroundService(this,
                            new Intent(this, BatteryService.class).setAction(BatteryService.ACTION_CHECK));
                }
            });

    static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ENABLED, false);
    }

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
        trackingSwitch.setChecked(isEnabled(this));
        trackingSwitch.setOnCheckedChangeListener((button, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, checked).apply();
            Intent service = new Intent(this, BatteryService.class);
            if (checked) {
                ContextCompat.startForegroundService(this, service);
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                stopService(service);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Re-read the setting: the user may arrive here from the notification.
        trackingSwitch.setChecked(isEnabled(this));
    }
}
