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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendanceapp.ui.theme.AttendanceAppTheme
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        databaseHelper = DatabaseHelper()

        // Check if user is logged in, if not redirect to login
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            AttendanceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        onNavigateToStudentScreen = {
                            startActivity(Intent(this, StudentActivity::class.java))
                        },
                        onNavigateToTeacherScreen = {
                            startActivity(Intent(this, TeacherActivity::class.java))
                        },
                        onLogout = {
                            auth.signOut()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToStudentScreen: () -> Unit,
    onNavigateToTeacherScreen: () -> Unit,
    onLogout: () -> Unit
) {
    var userRole by remember { mutableStateOf<String?>(null) }
    val databaseHelper = DatabaseHelper()

    LaunchedEffect(Unit) {
        // Fetch user role
        databaseHelper.getCurrentUserId()?.let { userId ->
            databaseHelper.getUserRole(userId) { role ->
                userRole = role
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance App Home") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Attendance App",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You are logged in as: ${userRole ?: "Loading..."}",
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNavigateToStudentScreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Student Dashboard")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToTeacherScreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Teacher Dashboard")
            }
        }
    }
}