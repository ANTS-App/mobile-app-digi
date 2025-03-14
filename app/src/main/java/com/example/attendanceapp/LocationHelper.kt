package com.example.attendanceapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat as ActivityCompat1

class LocationHelper(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var locationListener: LocationListener? = null

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 10f // 10 meters
        const val MIN_TIME_BETWEEN_UPDATES = 1000L * 30 // 30 seconds
    }

    // Check if location permission is granted
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Request location permission
    fun requestLocationPermission(activity: Activity) {
        ActivityCompat1.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // Get current location once
    fun getCurrentLocation(callback: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }

        try {
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            // Return the most recent location
            when {
                gpsLocation != null && networkLocation != null -> {
                    callback(if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation)
                }
                gpsLocation != null -> {
                    callback(gpsLocation)
                }
                networkLocation != null -> {
                    callback(networkLocation)
                }
                else -> {
                    // If no last known location, request updates and get the first one
                    val tempListener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            callback(location)
                            locationManager.removeUpdates(this)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }

                    locationManager.requestLocationUpdates(
                        /* provider = */ LocationManager.GPS_PROVIDER,
                        /* minTimeMs = */ MIN_TIME_BETWEEN_UPDATES,
                        /* minDistanceM = */ MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        /* listener = */ tempListener
                    )
                }
            }
        } catch (e: SecurityException) {
            callback(null)
        }
    }

    // Start continuous location updates
    fun startLocationUpdates(listener: LocationListener) {
        if (!hasLocationPermission()) {
            return
        }

        try {
            this.locationListener = listener
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BETWEEN_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                listener
            )
        } catch (e: SecurityException) {
            // Handle permission exception
        }
    }

    // Stop location updates
    fun stopLocationUpdates() {
        locationListener?.let {
            locationManager.removeUpdates(it)
            locationListener = null
        }
    }

    // Calculate distance between two locations in meters
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    // Check if student is within valid range of classroom
    fun isWithinValidRange(
        studentLocation: Location,
        classroomLat: Double,
        classroomLng: Double,
        maxDistanceMeters: Float = 100f
    ): Boolean {
        val distance = calculateDistance(
            studentLocation.latitude,
            studentLocation.longitude,
            classroomLat,
            classroomLng
        )
        return distance <= maxDistanceMeters
    }
}