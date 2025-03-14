package com.example.attendanceapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceapp.ui.theme.AttendanceAppTheme
import kotlinx.coroutines.delay

class HomeActivity : ComponentActivity() {
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseHelper = DatabaseHelper()

        setContent {
            AttendanceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WelcomeScreen(
                        onContinue = { navigateToStudentActivity() }
                    )
                }
            }
        }
    }

    private fun navigateToStudentActivity() {
        startActivity(Intent(this, StudentActivity::class.java))
        finish()
    }
}

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    var showContinueButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        showContinueButton = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Attendance App",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        CircularProgressIndicator()

        Spacer(modifier = Modifier.height(24.dp))

        if (showContinueButton) {
            Button(onClick = onContinue) {
                Text("Continue to Dashboard")
            }
        }
    }
}
