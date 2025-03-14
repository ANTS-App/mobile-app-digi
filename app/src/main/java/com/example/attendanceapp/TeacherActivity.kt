package com.example.attendanceapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendanceapp.ui.theme.AttendanceAppTheme
import kotlin.text.*

class TeacherActivity : ComponentActivity() {
    private lateinit var databaseHelper: DatabaseHelper
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseHelper = DatabaseHelper()

        setContent {
            AttendanceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TeacherScreen(
                        databaseHelper = databaseHelper,
                        onCreateSession = { courseId, pinDuration ->
                            createAttendanceSession(courseId, pinDuration)
                        }
                    )
                }
            }
        }
    }

    private fun createAttendanceSession(courseId: String, pinDurationMinutes: Int) {
        val teacherId = databaseHelper.getCurrentUserId() ?: return

        // Generate PIN with expiry time
        val (pin, expiryTime) = PinGenerator.generatePinWithExpiry(pinDurationMinutes)

        // Create session in database
        databaseHelper.createSession(
            teacherId = teacherId,
            courseId = courseId,
            pin = pin,
            expiryTime = expiryTime
        ) { sessionId ->
            if (sessionId != null) {
                Toast.makeText(this, "Session created successfully", Toast.LENGTH_SHORT).show()

                // Start countdown timer
                val durationMillis = pinDurationMinutes * 60 * 1000L
                countDownTimer = object : CountDownTimer(durationMillis, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        // Update UI with time remaining
                    }

                    override fun onFinish() {
                        Toast.makeText(this@TeacherActivity, "PIN has expired", Toast.LENGTH_SHORT).show()
                    }
                }.start()
            } else {
                Toast.makeText(this, "Failed to create session", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScreen(
    databaseHelper: DatabaseHelper,
    onCreateSession: (String, Int) -> Unit
) {
    rememberCoroutineScope()
    var courses by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedCourseId by remember { mutableStateOf<String?>(null) }
    var pinDuration by remember { mutableStateOf(1) } // Default 1 minute
    var currentPin by remember { mutableStateOf<String?>(null) }
    var timeRemaining by remember { mutableStateOf<Long>(0) }
    var isSessionActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Load teacher courses
        databaseHelper.getCurrentUserId()?.let { userId ->
            databaseHelper.getTeacherCourses(userId) { loadedCourses ->
                courses = loadedCourses
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher Dashboard") },
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
            // Active Session Display
            if (isSessionActive && currentPin != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Active Attendance Session",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "PIN",
                            fontSize = 16.sp
                        )

                        Text(
                            text = currentPin!!,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { timeRemaining.toFloat() / (pinDuration * 60) },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(text = "Time remaining: ${timeRemaining / 60}:${
                                String.format(
                                    "%02d",
                                    timeRemaining % 60
                                )
                            }", fontSize = 14.sp)
                    }
                }
            }

            // Course Selection
            Text(
                text = "Select a Course",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            if (courses.isEmpty()) {
                Text(
                    text = "No courses found. Please contact administrator.",
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(courses) { course ->
                        CourseSelectionItem(
                            course = course,
                            isSelected = selectedCourseId == course["id"],
                            onSelect = { selectedCourseId = course["id"] as String }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PIN Duration Selection
            Text(
                text = "Select PIN Duration",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 2, 5).forEach { minutes ->
                    OutlinedButton(
                        onClick = { pinDuration = minutes },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (pinDuration == minutes)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text("$minutes min")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Generate PIN Button
            Button(
                onClick = {
                    selectedCourseId?.let { courseId ->
                        onCreateSession(courseId, pinDuration)
                        currentPin = PinGenerator.generatePin()
                        timeRemaining = pinDuration * 60L
                        isSessionActive = true

                        // Start a countdown in the UI
                        object : CountDownTimer(pinDuration * 60 * 1000L, 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                timeRemaining = millisUntilFinished / 1000
                            }

                            override fun onFinish() {
                                isSessionActive = false
                                currentPin = null
                            }
                        }.start()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedCourseId != null && !isSessionActive
            ) {
                Text("Generate Attendance PIN")
            }
        }
    }
}

@Composable
fun CourseSelectionItem(
    course: Map<String, Any>,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = course["name"]?.toString() ?: "Unknown Course",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Section: ${course["section"]?.toString() ?: "N/A"}",
                    fontSize = 14.sp
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
        }
    }
}