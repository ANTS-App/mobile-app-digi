package com.example.attendanceapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.attendanceapp.utilities.StatusBarUtils;

public class WaitingActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private CheckBox locationCheckBox;
    private CheckBox bluetoothCheckBox;
    private Button continueButton;

    private final String[] requiredPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting);

        // Customize status bar
        StatusBarUtils.customizeStatusBar(this, R.color.white, true);

        // Initialize UI components
        locationCheckBox = findViewById(R.id.locationCheckBox);
        bluetoothCheckBox = findViewById(R.id.bluetoothCheckBox);
        continueButton = findViewById(R.id.continueButton);

        // Check current permission status
        updatePermissionCheckboxes();

        // Set up click listeners
        setupClickListeners();
    }

    private void setupClickListeners() {
        // Location checkbox click listener
        locationCheckBox.setOnClickListener(v -> {
            if (!hasLocationPermissions()) {
                requestLocationPermissions();
                // Uncheck until permissions are granted
                locationCheckBox.setChecked(false);
            }
        });

        // Bluetooth checkbox click listener
        bluetoothCheckBox.setOnClickListener(v -> {
            if (!hasBluetoothPermissions()) {
                requestBluetoothPermissions();
                // Uncheck until permissions are granted
                bluetoothCheckBox.setChecked(false);
            }
        });

        // Continue button click listener
        continueButton.setOnClickListener(v -> {
            if (allPermissionsGranted()) {
                proceedToNextScreen();
            } else {
                Toast.makeText(this, "Please grant all required permissions to continue", Toast.LENGTH_SHORT).show();
                updatePermissionCheckboxes();
            }
        });
    }

    private void updatePermissionCheckboxes() {
        // Update location checkbox
        boolean locationGranted = hasLocationPermissions();
        locationCheckBox.setChecked(locationGranted);

        // Update bluetooth checkbox
        boolean bluetoothGranted = hasBluetoothPermissions();
        bluetoothCheckBox.setChecked(bluetoothGranted);

        // Update button state
        continueButton.setEnabled(locationGranted && bluetoothGranted);
    }

    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBluetoothPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                PERMISSION_REQUEST_CODE
        );
    }

    private void requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                },
                PERMISSION_REQUEST_CODE
        );
    }

    private boolean allPermissionsGranted() {
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void proceedToNextScreen() {
        // Navigate to StudentView
        startActivity(new Intent(WaitingActivity.this, StudentView.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Update UI based on new permission status
            updatePermissionCheckboxes();

            // Check if all permissions are granted now
            if (allPermissionsGranted()) {
                Toast.makeText(this, "All permissions granted! You can proceed.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}