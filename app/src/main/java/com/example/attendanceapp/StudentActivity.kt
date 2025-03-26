package com.example.attendanceapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

import com.example.attendanceapp.ui.theme.AttendanceAppTheme

class StudentActivity : ComponentActivity() {
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var geolocationHelper: GeolocationHelper
    private lateinit var bluetoothHelper: BluetoothHelper

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseHelper = DatabaseHelper()
        geolocationHelper = GeolocationHelper(this)
        bluetoothHelper = BluetoothHelper(this)

        requestPermissions()

        setContent {
            AttendanceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudentScreen(
                        databaseHelper = databaseHelper,
                        geolocationHelper = geolocationHelper,
                        bluetoothHelper = bluetoothHelper,
                        onRequestPermissions = { requestPermissions() },
                        onNavigateToTimetable = { checkConditionsBeforeAttendance() }
                    )
                }
            }
        }
    }

    private fun checkConditionsBeforeAttendance() {
        if (!bluetoothHelper.isBluetoothEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth to mark attendance.", Toast.LENGTH_LONG).show()
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }

        bluetoothHelper.scanForTeacherBluetooth { isInRange ->
            if (isInRange) {
                geolocationHelper.verifyAttendance("3") { isWithinRange ->
                    if (isWithinRange) {
                        startActivity(Intent(this, TimetableSelectionActivity::class.java))
                    } else {
                        Toast.makeText(this, "You are not within 10 meters of the teacher.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Teacher not in Bluetooth range. Cannot mark attendance.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        permissionsToRequest.addAll(
            BluetoothHelper.getRequiredPermissions().filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
        )

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        geolocationHelper.stopLocationUpdates()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScreen(
    databaseHelper: DatabaseHelper,
    geolocationHelper: GeolocationHelper,
    bluetoothHelper: BluetoothHelper,
    onRequestPermissions: () -> Unit,
    onNavigateToTimetable: () -> Unit
) {
    var statusMessage by remember { mutableStateOf("") }
    var bluetoothEnabled by remember { mutableStateOf(bluetoothHelper.isBluetoothEnabled()) }

    LaunchedEffect(Unit) {
        if (!geolocationHelper.hasLocationPermission() || !bluetoothHelper.hasBluetoothPermissions()) {
            onRequestPermissions()
        }

        if (!bluetoothHelper.isBluetoothEnabled()) {
            statusMessage = "Bluetooth is not enabled. Please enable it to continue."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Student Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onNavigateToTimetable,
                modifier = Modifier.fillMaxWidth(),
                enabled = bluetoothEnabled
            ) {
                Text("Mark Attendance")
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
