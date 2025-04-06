package com.example.attendanceapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class BluetoothHelper {
    private static final String TAG = "BluetoothHelper";
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private String teacherMacAddress; // Fetched from Firebase

    public interface TeacherDeviceScanCallback {
        // Callback method returns true if teacher's device is found, false otherwise.
        Void onScanResult(boolean isTeacherInRange);
    }

    public BluetoothHelper(Context context) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }
        // Fetch teacher's MAC address from Firebase
        fetchTeacherMacAddress();
    }

    private void fetchTeacherMacAddress() {
        // Assume the teacher's MAC address is stored at "attendance_sessions/{session_id}/teacher_mac_address"
        // Change the path as per your Firebase database structure.
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("attendance_sessions");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Loop through each session (timestamp nodes)
                    for (DataSnapshot sessionSnapshot : snapshot.getChildren()) {
                        String status = sessionSnapshot.child("status").getValue(String.class);
                        if ("active".equals(status)) {
                            String mac = sessionSnapshot.child("bluetoothSignature").getValue(String.class);
                            teacherMacAddress = mac;
                            Log.d(TAG, "Fetched teacher MAC address from active session: " + teacherMacAddress);
                            // Only one active session is expected; exit the loop
                            break;
                        }
                    }
                } else {
                    Log.w(TAG, "No sessions found in attendance_sessions");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error fetching teacher MAC address: " + error.getMessage());
            }
        });

    }

    // Check if the app has the necessary Bluetooth permissions.
    public boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean scanGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean connectGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            return scanGranted && connectGranted;
        } else {
            // For pre-Android 12, location permission is used for BLE scanning.
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // Request the necessary Bluetooth permissions.
    public void requestBluetoothPermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                    requestCode);
        }
    }

    // Start scanning for nearby BLE devices and check if teacher's device (by MAC address) is found.
    public void scanForTeacherBluetooth(final TeacherDeviceScanCallback callback) {
        if (bluetoothAdapter == null || bluetoothLeScanner == null) {
            Log.e(TAG, "Bluetooth not supported or not enabled");
            callback.onScanResult(false);
            return;
        }
        // Create an empty filter list (MAC address filtering via ScanFilter isn’t allowed for privacy reasons)
        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        // Create scan callback to receive scan results.
        ScanCallback scanCallback = new ScanCallback() {
            private boolean teacherFound = false;

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                String deviceAddress = device.getAddress();
                Log.d(TAG, "Found device: " + deviceAddress);
                if (teacherMacAddress != null && teacherMacAddress.equalsIgnoreCase(deviceAddress)) {
                    teacherFound = true;

                    bluetoothLeScanner.stopScan(this);
                    callback.onScanResult(true);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "BLE scan failed with code " + errorCode);
                callback.onScanResult(false);
            }
        };

        // Start scanning
        bluetoothLeScanner.startScan(filters, settings, scanCallback);

        // Stop scanning after 10 seconds if teacher's device isn't found.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                bluetoothLeScanner.stopScan(scanCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error stopping scan: " + e.getMessage());
            }
            // If not already found, callback with false.
            callback.onScanResult(false);
        }, 10000);
    }
}
