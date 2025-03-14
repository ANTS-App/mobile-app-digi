package com.example.attendanceapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    private lateinit var locationHelper: LocationHelper
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

        // Initialize helpers
        databaseHelper = DatabaseHelper()
        locationHelper = LocationHelper(this)
        bluetoothHelper = BluetoothHelper(this)

        // Request necessary permissions
        requestPermissions()

        setContent {
            AttendanceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudentScreen(
                        databaseHelper = databaseHelper,
                        locationHelper = locationHelper,
                        bluetoothHelper = bluetoothHelper,
                        onRequestPermissions = { requestPermissions() },
                        onNavigateToTimetable = { navigateToTimetableSelection() }
                    )
                }
            }
        }
    }

    private fun navigateToTimetableSelection() {
        startActivity(Intent(this, TimetableSelectionActivity::class.java))
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Add location permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Add Bluetooth permissions
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
        bluetoothHelper.cleanup()
        locationHelper.stopLocationUpdates()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScreen(
    databaseHelper: DatabaseHelper,
    locationHelper: LocationHelper,
    bluetoothHelper: BluetoothHelper,
    onRequestPermissions: () -> Unit,
    onNavigateToTimetable: () -> Unit
) {
    var statusMessage by remember { mutableStateOf("") }
    val isLoading by remember { mutableStateOf(false) }
    var courses by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(Unit) {
        // Check for permissions
        if (!locationHelper.hasLocationPermission() || !bluetoothHelper.hasBluetoothPermissions()) {
            onRequestPermissions()
        }

        // Check if Bluetooth is enabled
        if (!bluetoothHelper.isBluetoothEnabled()) {
            statusMessage = "Bluetooth is not enabled. Please enable it to continue."
        }

        // Load student courses if user is logged in
        databaseHelper.getCurrentUserId()?.let { _ ->
            // Assuming DatabaseHelper internally uses the current user ID
            databaseHelper.getStudentCourses { loadedCourses ->
                courses = loadedCourses
            }
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mark Attendance")
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    color = when {
                        statusMessage.contains("Success") -> Color.Green
                        statusMessage.contains("Error") -> Color.Red
                        else -> Color.Gray
                    },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (isLoading) {
                CircularProgressIndicator()
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Your Courses",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            if (courses.isEmpty()) {
                Text(
                    text = "No courses found. Please contact your administrator.",
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(courses) { course ->
                        CourseItem(course)
                    }
                }
            }
        }
    }
}

@Composable
fun CourseItem(course: Map<String, Any>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = course["name"]?.toString() ?: "Unknown Course",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Instructor: ${course["instructorName"]?.toString() ?: "Unknown"}",
                fontSize = 14.sp
            )
            Text(
                text = "Section: ${course["section"]?.toString() ?: "N/A"}",
                fontSize = 14.sp
            )
        }
    }
}
