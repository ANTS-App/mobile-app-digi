package com.example.attendanceapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.attendanceapp.ui.theme.AttendanceAppTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class AttendanceActivity : ComponentActivity() {
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var locationHelper: LocationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseHelper = DatabaseHelper()
        locationHelper = LocationHelper(this)

        val className = intent.getStringExtra("CLASS_NAME") ?: ""

        setContent {
            AttendanceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AttendanceScreen(
                        className = className,
                        databaseHelper = databaseHelper,
                        locationHelper = locationHelper,
                        onAttendanceMarked = { success ->
                            if (success) {
                                Toast.makeText(this, "Attendance marked successfully!", Toast.LENGTH_LONG).show()
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    className: String,
    databaseHelper: DatabaseHelper,
    locationHelper: LocationHelper,
    onAttendanceMarked: (Boolean) -> Unit
) {
    var pinOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var correctPin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // Fetch active session for this class
        fetchActiveSessionForClass(databaseHelper, className) { activeSessionId ->
            if (activeSessionId != null) {
                sessionId = activeSessionId
                fetchAttendanceOptions(databaseHelper, activeSessionId) { options, correct ->
                    pinOptions = options
                    correctPin = correct
                    isLoading = false
                }
            } else {
                statusMessage = "No active attendance session for this class"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mark Attendance: $className") },
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
            if (isLoading) {
                CircularProgressIndicator()
            } else if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    color = Color.Red
                )
            } else {
                Text(
                    text = "Select the correct attendance code:",
                    style = MaterialTheme.typography.titleMedium
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Combine and shuffle the options
                    val allOptions = (pinOptions + correctPin).shuffled()

                    items(allOptions) { pin ->
                        PinOptionItem(
                            pin = pin,
                            onClick = {
                                verifyAndMarkAttendance(
                                    pin = pin,
                                    correctPin = correctPin,
                                    sessionId = sessionId,
                                    className = className,
                                    databaseHelper = databaseHelper,
                                    locationHelper = locationHelper,
                                    onStatusUpdate = { message, success ->
                                        statusMessage = message
                                        if (success) {
                                            onAttendanceMarked(true)
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PinOptionItem(
    pin: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(pin)
    }
}
// These functions should be included in your AttendanceActivity.kt file

private fun fetchActiveSessionForClass(
    databaseHelper: DatabaseHelper,
    className: String,
    callback: (String?) -> Unit
) {
    databaseHelper.database.getReference("attendance_sessions")
        .orderByChild("class_name")
        .equalTo(className)
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var activeSessionId: String? = null
                val currentTime = System.currentTimeMillis()

                for (sessionSnapshot in snapshot.children) {
                    val status = sessionSnapshot.child("status").getValue(String::class.java)
                    val expiryTime = sessionSnapshot.child("expiry_time").getValue(Long::class.java) ?: 0L

                    // Check if session is active and not expired
                    if (status == "active" && expiryTime > currentTime) {
                        activeSessionId = sessionSnapshot.key
                        break
                    }
                }

                callback(activeSessionId)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
}

private fun fetchAttendanceOptions(
    databaseHelper: DatabaseHelper,
    sessionId: String,
    callback: (List<String>, String) -> Unit
) {
    databaseHelper.database.getReference("attendance_sessions")
        .child(sessionId)
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val correctPin = snapshot.child("correct_pin").getValue(String::class.java) ?: ""
                val otherOptions = mutableListOf<String>()

                snapshot.child("decoy_pins").children.forEach {
                    val option = it.getValue(String::class.java)
                    if (option != null) {
                        otherOptions.add(option)
                    }
                }

                callback(otherOptions, correctPin)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList(), "")
            }
        })
}

private fun verifyAndMarkAttendance(
    pin: String,
    correctPin: String,
    sessionId: String,
    className: String,
    databaseHelper: DatabaseHelper,
    locationHelper: LocationHelper,
    onStatusUpdate: (String, Boolean) -> Unit
) {
    if (pin != correctPin) {
        onStatusUpdate("Incorrect PIN selected", false)
        return
    }

    locationHelper.getCurrentLocation { location ->
        if (location == null) {
            onStatusUpdate("Error: Unable to get your location", false)
            return@getCurrentLocation
        }

        val userId = databaseHelper.getCurrentUserId()
        if (userId == null) {
            onStatusUpdate("Error: You are not logged in", false)
            return@getCurrentLocation
        }

        val locationMap = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude
        )

        databaseHelper.markAttendance(
            studentId = userId,
            courseId = className,
            sessionId = sessionId,
            location = locationMap
        ) { success ->
            if (success) {
                onStatusUpdate("Success! Your attendance has been recorded", true)
            } else {
                onStatusUpdate("Error: Failed to record attendance", false)
            }
        }
    }
}