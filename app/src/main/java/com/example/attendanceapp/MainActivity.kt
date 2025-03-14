package com.example.attendanceapp

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        bluetoothHelper = BluetoothHelper(this)

        requestPermissions()

        setContent {
            AttendanceAppTheme {
                SplashScreen(onSplashComplete = { navigateBasedOnAuthState() })
            }
        }
    }

    private fun requestPermissions() {
        val requiredPermissions = BluetoothHelper.getRequiredPermissions()
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions.values.any { !it }) {
                    showPermissionDeniedDialog()
                }
            }.launch(missingPermissions.toTypedArray())
        }
    }

    private fun showPermissionDeniedDialog() {
        setContent {
            AttendanceAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Permissions Required")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Please grant all necessary permissions to use the app.")
                    }
                }
            }
        }
    }

    private fun navigateBasedOnAuthState() {
        if (auth.currentUser != null) {
            if (!bluetoothHelper.isBluetoothEnabled()) {
                bluetoothHelper.requestEnableBluetooth(this)
            }
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Attendance App")
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
