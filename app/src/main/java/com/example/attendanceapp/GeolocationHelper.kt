package com.example.attendanceapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.firebase.database.*
import kotlin.math.log

class GeolocationHelper(private val context: Context) {
    private val TAG = "GeolocationHelper"
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val database = FirebaseDatabase.getInstance().reference.child("attendance_sessions")
    private var locationListener: LocationListener? = null
    private var timeoutHandler: Handler? = null
    private var timeoutRunnable: Runnable = Runnable { }

    fun hasLocationPermission(): Boolean {
        val hasPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Location permission check: $hasPermission")
        return hasPermission
    }

    private fun isGpsEnabled(): Boolean {
        val isEnabled = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking GPS: ${e.message}", e)
            false
        }
        Log.d(TAG, "GPS enabled: $isEnabled")
        return isEnabled
    }

    @SuppressLint("MissingPermission")
    fun verifyAttendance(teacherId: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Verifying attendance for teacher ID: $teacherId")

        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            callback(false)
            return
        }

        if (!isGpsEnabled()) {
            Log.w(TAG, "GPS is disabled, accuracy may be reduced")
        }

        Log.d(TAG, "Fetching teacher location from active session in Firebase")
        // Loop through all sessions under attendance_sessions to find the active one.
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var activeFound = false
                    for (sessionSnapshot in snapshot.children) {
                        val status = sessionSnapshot.child("status").getValue(String::class.java)
                        if (status != null && status == "active") {
                            activeFound = true
                            // Retrieve teacher location from the active session
                            val teacherLatitude = sessionSnapshot.child("latitude").getValue(Double::class.java)
                            val teacherLongitude = sessionSnapshot.child("longitude").getValue(Double::class.java)
                            if (teacherLatitude == null || teacherLongitude == null) {
                                Log.e(TAG, "Teacher location missing in active session")
                                callback(false)
                                return
                            }
                            Log.d(TAG, "Teacher location: Lat=$teacherLatitude, Long=$teacherLongitude")

                            // Try to get the last known location
                            val lastKnownLocation = getLastKnownLocation()
                            if (lastKnownLocation != null && isLocationFresh(lastKnownLocation)) {
                                Log.d(TAG, "Last known location: Lat=${lastKnownLocation.latitude}, Long=${lastKnownLocation.longitude}")
                                processLocation(lastKnownLocation, teacherLatitude, teacherLongitude, callback)
                                return
                            }
                            // If no fresh last known location, request live location updates.
                            requestLiveLocation(teacherLatitude, teacherLongitude, callback)
                            return
                        }
                    }
                    if (!activeFound) {
                        Log.e(TAG, "No active session found in Firebase")
                        callback(false)
                    }
                } else {
                    Log.e(TAG, "No attendance sessions found")
                    callback(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false)
            }
        })
    }

    private fun isLocationFresh(location: Location): Boolean {
        val timeThreshold =  1000
        val locationAge = System.currentTimeMillis() - location.time
        return locationAge < timeThreshold
    }

    // Changed threshold from 10 meters to 25 meters.
    private fun processLocation(studentLocation: Location, teacherLat: Double, teacherLong: Double, callback: (Boolean) -> Unit) {
        val studentLatitude = studentLocation.latitude
        val studentLongitude = studentLocation.longitude
        Log.d(TAG, "Student location: Lat=$studentLatitude, Long=$studentLongitude")

        val teacherLocation = Location("teacher").apply {
            latitude = teacherLat
            longitude = teacherLong
        }
        val distance = studentLocation.distanceTo(teacherLocation)
        Log.d(TAG, "Distance to teacher: $distance meters")
        callback(distance <= 100)
    }

    @SuppressLint("MissingPermission")
    private fun requestLiveLocation(teacherLat: Double, teacherLong: Double, callback: (Boolean) -> Unit) {
        stopLocationUpdates()
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d(TAG, "Live location update received: Lat=${location.latitude}, Long=${location.longitude}")
                processLocation(location, teacherLat, teacherLong, callback)
                stopLocationUpdates()
            }
            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0L, 0f, locationListener!!, Looper.getMainLooper()
            )
            setLocationTimeout(callback, teacherLat, teacherLong)
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location updates: ${e.message}", e)
            callback(false)
        }
    }

    private fun setLocationTimeout(callback: (Boolean) -> Unit, teacherLat: Double, teacherLong: Double) {
        timeoutHandler?.removeCallbacks(timeoutRunnable)
        timeoutHandler = Handler(Looper.getMainLooper())
        timeoutRunnable = Runnable {
            val lastLocation = getLastKnownLocation()
            if (lastLocation != null) {
                Log.d(TAG, "Using last known location after timeout: Lat=${lastLocation.latitude}, Long=${lastLocation.longitude}")
                processLocation(lastLocation, teacherLat, teacherLong, callback)
            } else {
                stopLocationUpdates()
                callback(false)
            }
        }
        timeoutHandler?.postDelayed(timeoutRunnable, 15000)
    }

    fun stopLocationUpdates() {
        locationListener?.let {
            locationManager.removeUpdates(it)
            locationListener = null
        }
        timeoutHandler?.removeCallbacks(timeoutRunnable)
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null
        return try {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (location != null) {
                Log.d(TAG, "Retrieved last known location: Lat=${location.latitude}, Long=${location.longitude}")
            }
            location
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last known location: ${e.message}", e)
            null
        }
    }
}
