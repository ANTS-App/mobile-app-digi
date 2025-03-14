package com.example.attendanceapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class BluetoothHelper(private val context: Context) {
    private val bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    companion object {
        fun getRequiredPermissions(): List<String> {
            val permissions = mutableListOf<String>()

            // Basic Bluetooth permissions
            permissions.add(Manifest.permission.BLUETOOTH)

            // Android 12+ specific permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                // Older Android versions
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            }

            return permissions
        }
    }

    fun hasBluetoothPermissions(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun cleanup() {
        // Cleanup any Bluetooth resources if needed
    }
}