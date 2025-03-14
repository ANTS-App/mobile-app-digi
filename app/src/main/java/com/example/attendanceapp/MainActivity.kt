package com.example.attendanceapp

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.attendanceapp.ui.theme.AttendanceAppTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var bluetoothHelper: BluetoothHelper
    private lateinit var databaseHelper: DatabaseHelper

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if critical permissions are granted
        val criticalPermissionsGranted = permissions.entries
            .filter { it.key in criticalPermissions }
            .all { it.value }

        if (criticalPermissionsGranted) {
            // Continue with app flow if critical permissions are granted
            continueToNextScreen()
        } else {
            // Show permission explanation dialog if critical permissions denied
            setContent {
                AttendanceAppTheme {
                    PermissionDeniedScreen {
                        requestPermissions()
                    }
                }
            }
        }
    }

    // Define critical permissions that are absolutely necessary
    private val criticalPermissions = listOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        bluetoothHelper = BluetoothHelper(this)
        databaseHelper = DatabaseHelper()

        setContent {
            AttendanceAppTheme {
                SplashScreen(onSplashComplete = {
                    // Check permissions after splash screen
                    checkAndRequestPermissions()
                })
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Add location permissions
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Add Bluetooth permissions
        permissionsToRequest.addAll(
            BluetoothHelper.getRequiredPermissions().filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
        )

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // All permissions already granted, continue with app flow
            continueToNextScreen()
        }
    }

    private fun requestPermissions() {
        checkAndRequestPermissions()
    }

    private fun continueToNextScreen() {
        // Check if Bluetooth is available and enabled
        if (bluetoothHelper.isBluetoothSupported() && !bluetoothHelper.isBluetoothEnabled()) {
            bluetoothHelper.requestEnableBluetooth(this)
        }

        // Navigate based on auth state
        if (auth.currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500)
        onSplashComplete()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Attendance App")
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
fun PermissionDeniedScreen(onRetryRequest: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This app needs location and Bluetooth permissions to function properly. " +
                        "Please grant all necessary permissions to continue.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onRetryRequest) {
                Text("Grant Permissions")
            }
        }
    }
}