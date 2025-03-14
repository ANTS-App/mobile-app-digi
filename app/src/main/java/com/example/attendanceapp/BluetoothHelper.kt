@file:Suppress("DEPRECATION")

package com.example.attendanceapp

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class BluetoothHelper(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var deviceDiscoveryReceiver: BroadcastReceiver? = null
    private var discoveredDevices = mutableSetOf<BluetoothDevice>()

    companion object {
        const val BLUETOOTH_PERMISSION_REQUEST_CODE = 1002
        const val BLUETOOTH_ENABLE_REQUEST_CODE = 1003

        fun getRequiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun hasBluetoothPermissions(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestBluetoothPermissions(activity: Activity) {
        if (!hasBluetoothPermissions()) {
            ActivityCompat.requestPermissions(
                activity,
                getRequiredPermissions(),
                BLUETOOTH_PERMISSION_REQUEST_CODE
            )
        }
    }

    fun requestEnableBluetooth(activity: Activity) {
        if (!isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            try {
                activity.startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE_REQUEST_CODE)
            } catch (e: SecurityException) {
                Log.e("BluetoothHelper", "SecurityException when enabling Bluetooth", e)
            }
        }
    }

    fun startDiscovery(onDeviceFound: (BluetoothDevice) -> Unit) {
        if (!hasBluetoothPermissions() || !isBluetoothEnabled()) {
            Log.w("BluetoothHelper", "Permissions not granted or Bluetooth not enabled")
            return
        }

        discoveredDevices.clear()

        stopDiscovery()

        deviceDiscoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            if (discoveredDevices.add(it)) {
                                onDeviceFound(it)
                            }
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(deviceDiscoveryReceiver, filter)

        try {
            bluetoothAdapter?.startDiscovery()
        } catch (e: SecurityException) {
            Log.e("BluetoothHelper", "SecurityException when starting discovery", e)
        }
    }

    private fun stopDiscovery() {
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: SecurityException) {
            Log.e("BluetoothHelper", "SecurityException when stopping discovery", e)
        }

        deviceDiscoveryReceiver?.let {
            context.unregisterReceiver(it)
            deviceDiscoveryReceiver = null
        }
    }

    fun isDeviceInRange(deviceAddress: String): Boolean {
        return discoveredDevices.any { it.address == deviceAddress }
    }

    fun cleanup() {
        stopDiscovery()
    }
}