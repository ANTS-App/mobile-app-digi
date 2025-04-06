package com.example.attendanceapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendanceapp.Prevalent.Prevalent
import com.example.attendanceapp.ui.theme.AttendanceAppTheme
import com.example.attendanceapp.utilities.StatusBarUtils
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import io.paperdb.Paper

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        Paper.init(this)

        StatusBarUtils.customizeStatusBar(this, R.color.white, true)

        // Auto redirect if already logged in
        if (auth.currentUser != null) {
            navigateToAppropriateScreen()
            return
        }

        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        setContent {
            StatusBarUtils.customizeStatusBar(this,R.color.white,true)
            AttendanceAppTheme(
                darkTheme = false,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorResource(id = R.color.white)

                ) {
                    LoginScreen {
                        launchGoogleSignIn()
                    }
                }
            }
        }
    }

    private fun launchGoogleSignIn() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                googleLoginLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Google Sign-In failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                Log.e("LoginActivity", "One Tap sign-in failed", e)
            }
    }

    private val googleLoginLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                val email = credential.id

                if (idToken != null && email != null && email.endsWith("@kiit.ac.in")) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser


                                user?.email?.let { email ->
                                    val rollNumber = email.substringBefore("@")
                                    val uid = user.uid

                                    Paper.book().write(Prevalent.UserUid, uid)
                                    Paper.book().write(Prevalent.UserRoll, email)

                                    val databaseRef  = FirebaseDatabase.getInstance()
                                        .getReference("users")
                                        .child("Students")
                                        .child(rollNumber)

                                    val databaseRef1  = FirebaseDatabase.getInstance()
                                        .getReference("users")
                                        .child("Students_uid")
                                        .child(uid)

                                    val studentData = mapOf(
                                        "roll_number" to rollNumber,
                                        "uid" to uid
                                    )

                                    databaseRef.setValue(studentData)
                                        .addOnCompleteListener { dbTask ->
                                            if (dbTask.isSuccessful) {
                                                databaseRef1.setValue(studentData)
                                                    .addOnCompleteListener{ dbTask ->
                                                        if(dbTask.isSuccessful) {
                                                            Log.d("FirebaseDB", "Student data saved successfully")

                                                            Paper.book().write(Prevalent.UserUid, uid)
                                                            Paper.book().write(Prevalent.UserRoll, email)
                                                        } else {
                                                            Log.e("FirebaseDB", "Failed to save student data", dbTask.exception)
                                                        }
                                                    }
                                            } else {
                                                Log.e("FirebaseDB", "Failed to save student data", dbTask.exception)
                                            }
                                        }
                                }
                                navigateToAppropriateScreen()
                            } else {
                                Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Only @kiit.ac.in emails are allowed", Toast.LENGTH_LONG).show()
                    auth.signOut()
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Sign-in failed", e)
                Toast.makeText(this, "Sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }

    private fun navigateToAppropriateScreen() {
        startActivity(Intent(this, WaitingActivity::class.java))
        finish()
    }
}

@Composable
fun LoginScreen(onGoogleLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Attendance App",
            fontSize = 28.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onGoogleLoginClick) {
            Text("Sign in with KIIT Google Account")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Only @kiit.ac.in emails are allowed",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}
