package com.example.attendanceapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var cancellationTokenSource = CancellationTokenSource()

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getCurrentLocation(callback: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }

        try {
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                callback(location)
            }.addOnFailureListener {
                callback(null)
            }
        } catch (e: SecurityException) {
            callback(null)
        }
    }

    fun stopLocationUpdates() {
        cancellationTokenSource.cancel()
    }
}