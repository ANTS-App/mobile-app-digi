package com.example.attendanceapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.database.*

class BluetoothHelper(private val context: Context) {
    private val bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("teachers")

    companion object {
        fun getRequiredPermissions(): List<String> {
            val permissions = mutableListOf(
                Manifest.permission.BLUETOOTH // Basic Bluetooth permission
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
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

    fun scanForTeacherBluetooth(callback: (Boolean) -> Unit) {
        if (!hasBluetoothPermissions()) {
            Log.e("BluetoothHelper", "Missing Bluetooth permissions!")
            callback(false)
            return
        }

        if (!isBluetoothEnabled()) {
            Log.e("BluetoothHelper", "Bluetooth is disabled!")
            callback(false)
            return
        }

        // Fetch the teacher's Bluetooth MAC address from Firebase
        database.child("bluetoothSignature").get().addOnSuccessListener { snapshot ->
            val teacherMacAddress = snapshot.getValue(String::class.java)
            if (teacherMacAddress.isNullOrEmpty()) {
                Log.e("BluetoothHelper", "Teacher's Bluetooth MAC address not found in Firebase!")
                callback(false)
                return@addOnSuccessListener
            }

            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            val isTeacherInRange = pairedDevices?.any { it.address == teacherMacAddress } ?: false

            callback(isTeacherInRange)
        }.addOnFailureListener {
            Log.e("BluetoothHelper", "Failed to retrieve teacher's Bluetooth MAC address.", it)
            callback(false)
        }
    }
}
