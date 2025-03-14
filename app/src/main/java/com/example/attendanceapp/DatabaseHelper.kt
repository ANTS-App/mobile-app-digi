package com.example.attendanceapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import java.util.*

class DatabaseHelper {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // User Management
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun getUserRole(userId: String, callback: (String?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                callback(document.getString("role"))
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    // Attendance Management
    fun markAttendance(
        studentId: String,
        courseId: String,
        sessionId: String,
        location: Map<String, Double>,
        callback: (Boolean) -> Unit
    ) {
        val attendanceData = hashMapOf(
            "studentId" to studentId,
            "courseId" to courseId,
            "sessionId" to sessionId,
            "timestamp" to Timestamp(Date()),
            "location" to location,
            "status" to "present"
        )

        db.collection("attendance").add(attendanceData)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    // Session Management
    fun createSession(
        teacherId: String,
        courseId: String,
        pin: String,
        expiryTime: Date,
        callback: (String?) -> Unit
    ) {
        val sessionData = hashMapOf(
            "teacherId" to teacherId,
            "courseId" to courseId,
            "pin" to pin,
            "created" to Timestamp(Date()),
            "expiry" to Timestamp(expiryTime),
            "active" to true
        )

        db.collection("sessions").add(sessionData)
            .addOnSuccessListener { documentReference ->
                callback(documentReference.id)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun verifyPin(
        pin: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val currentTime = Timestamp(Date())
        db.collection("sessions")
            .whereEqualTo("pin", pin)
            .whereEqualTo("active", true)
            .whereGreaterThan("expiry", currentTime)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val session = documents.documents[0]
                    callback(true, session.id)
                } else {
                    callback(false, null)
                }
            }
            .addOnFailureListener {
                callback(false, null)
            }
    }

    // Course Management
    fun getTeacherCourses(teacherId: String, callback: (List<Map<String, Any>>) -> Unit) {
        db.collection("courses")
            .whereEqualTo("teacherId", teacherId)
            .get()
            .addOnSuccessListener { documents ->
                val coursesList = documents.map { doc ->
                    val data = doc.data
                    data["id"] = doc.id
                    data
                }
                callback(coursesList)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    fun getStudentCourses(studentId: String, callback: (List<Map<String, Any>>) -> Unit) {
        db.collection("enrollments")
            .whereEqualTo("studentId", studentId)
            .get()
            .addOnSuccessListener { enrollments ->
                val courseIds = enrollments.documents.mapNotNull { it.getString("courseId") }
                if (courseIds.isEmpty()) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                db.collection("courses")
                    .whereIn("id", courseIds)
                    .get()
                    .addOnSuccessListener { courses ->
                        val coursesList = courses.map { doc ->
                            val data = doc.data
                            data["id"] = doc.id
                            data
                        }
                        callback(coursesList)
                    }
                    .addOnFailureListener {
                        callback(emptyList())
                    }
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }
}
