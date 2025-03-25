// TimetableSelectionActivity.kt
package com.example.attendanceapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceapp.models.Timetable
import com.example.attendanceapp.ui.theme.AttendanceAppTheme
import com.google.firebase.auth.FirebaseAuth

class TimetableSelectionActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        databaseHelper = DatabaseHelper()

        setContent {
            AttendanceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TimetableSelectionScreen(
                        databaseHelper = databaseHelper,
                        onClassSelected = { selectedClass ->
                            val intent = Intent(this, AttendanceActivity::class.java)
                            intent.putExtra("CLASS_NAME", selectedClass)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableSelectionScreen(
    databaseHelper: DatabaseHelper,
    onClassSelected: (String) -> Unit
) {
    var timetable by remember { mutableStateOf<List<Timetable>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val currentUserId = databaseHelper.getCurrentUserId()
        if (currentUserId != null) {
            databaseHelper.getStudentTimetable { fetchedTimetable ->
                if (fetchedTimetable.isNotEmpty()) {
                    timetable = fetchedTimetable
                    isLoading = false
                } else {
                    // Handle empty timetable case
                    println("No timetable data found")
                    isLoading = false
                }
            }
        } else {
            // Handle case where no user is logged in
            println("No user ID found")
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Class") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(timetable) { classItem ->
                    TimetableItem(
                        timetableItem = classItem,
                        onClick = { onClassSelected(classItem.className) }
                    )
                }
            }
        }
    }
}

@Composable
fun TimetableItem(
    timetableItem: Timetable,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = timetableItem.className,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${timetableItem.day} - ${timetableItem.timeSlot}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Instructor: ${timetableItem.instructorName}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
