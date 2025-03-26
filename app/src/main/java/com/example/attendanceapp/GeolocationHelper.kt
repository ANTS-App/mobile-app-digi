package com.example.attendanceapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.firebase.database.*

class GeolocationHelper(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val database = FirebaseDatabase.getInstance().reference.child("teachers")

    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission") // This is safe because we check permission before calling
    fun verifyAttendance(teacherId: String, callback: (Boolean) -> Unit) {
        if (!hasLocationPermission()) {
            Log.e("GeolocationHelper", "Location permission not granted")
            callback(false)
            return
        }

        database.child(teacherId).get().addOnSuccessListener { snapshot ->
            val teacherLatitude = snapshot.child("latitude").getValue(Double::class.java)
            val teacherLongitude = snapshot.child("longitude").getValue(Double::class.java)

            if (teacherLatitude == null || teacherLongitude == null) {
                Log.e("GeolocationHelper", "Teacher location not found in Firebase")
                callback(false)
                return@addOnSuccessListener
            }

            try {
                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val studentLocation = Location("").apply {
                            latitude = location.latitude
                            longitude = location.longitude
                        }

                        val teacherLocation = Location("").apply {
                            latitude = teacherLatitude
                            longitude = teacherLongitude
                        }

                        val distance = studentLocation.distanceTo(teacherLocation)
                        Log.d("GeolocationHelper", "Distance to teacher: $distance meters")

                        callback(distance <= 10)
                        locationManager.removeUpdates(this)
                    }

                    override fun onProviderDisabled(provider: String) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                }

                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L, // Min time interval in ms
                    1f, // Min distance interval in meters
                    locationListener
                )

            } catch (e: SecurityException) {
                Log.e("GeolocationHelper", "SecurityException: ${e.message}")
                callback(false)
            }

        }.addOnFailureListener { error ->
            Log.e("GeolocationHelper", "Firebase error: ${error.message}")
            callback(false)
        }
    }

    fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates { }
        } catch (e: SecurityException) {
            Log.e("GeolocationHelper", "SecurityException: ${e.message}")
        }
    }
}
