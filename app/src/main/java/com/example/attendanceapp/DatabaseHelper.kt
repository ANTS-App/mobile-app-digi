package com.example.attendanceapp

import com.example.attendanceapp.models.Timetable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*
import android.util.Log

class DatabaseHelper {
    val database = FirebaseDatabase.getInstance("https://attendanceapp-9c1ee-default-rtdb.firebaseio.com/")
    private val auth = FirebaseAuth.getInstance()

    // Authentication Methods
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun signOut() {
        auth.signOut()
    }

    // This method maps Firebase Auth email to your Student ID system
    fun getCurrentStudentId(callback: (String?) -> Unit) {
        Log.d("StudentIdFetch", "Starting getCurrentStudentId")

        // Check if user is authenticated
        val authUser = auth.currentUser
        if (authUser == null) {
            Log.w("StudentIdFetch", "No authenticated user found")
            return callback(null)
        }

        // Get user email
        val email = authUser.email
        if (email == null) {
            Log.w("StudentIdFetch", "Authenticated user has no email")
            return callback(null)
        }

        Log.d("StudentIdFetch", "Searching for student with email: $email")

        // Find student with this email in database
        val studentsRef = database.getReference("users/Students")
        studentsRef
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("StudentIdFetch", "Total students found: ${snapshot.childrenCount}")

                    // Iterate through each student ID (S12345, S12346, etc.)
                    for (studentSnapshot in snapshot.children) {
                        val studentEmail = studentSnapshot.child("email").getValue(String::class.java)

                        Log.d("StudentIdFetch", "Checking student ID: ${studentSnapshot.key}, Email: $studentEmail")

                        if (studentEmail == email) {
                            Log.i("StudentIdFetch", "Matching student ID found: ${studentSnapshot.key}")
                            return callback(studentSnapshot.key)
                        }
                    }

                    Log.w("StudentIdFetch", "No student found with email: $email")
                    callback(null)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("StudentIdFetch", "Database error: ${error.message}")
                    callback(null)
                }
            })
    }

    // User Profile Methods
    fun getUserProfile(callback: (Map<String, Any>?) -> Unit) {
        getCurrentStudentId { studentId ->
            if (studentId == null) {
                callback(null)
                return@getCurrentStudentId
            }

            database.getReference("users/Students").child(studentId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            @Suppress("UNCHECKED_CAST")
                            val userData = snapshot.value as? Map<String, Any>
                            callback(userData)
                        } else {
                            callback(null)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        callback(null)
                    }
                })
        }
    }

    // Timetable Methods
    fun getStudentTimetable(callback: (List<Timetable>) -> Unit) {
        Log.d("StudentTimetable", "Starting getStudentTimetable function")

        getCurrentStudentId { studentId ->
            Log.d("StudentTimetable", "Retrieved student ID: $studentId")

            if (studentId == null) {
                Log.w("StudentTimetable", "No student ID found. Returning empty list.")
                callback(emptyList())
                return@getCurrentStudentId
            }

            // Get the classes this student is enrolled in
            val classesRef = database.getReference("users/Students")
                .child(studentId)
                .child("classes")

            Log.d("StudentTimetable", "Fetching classes for student at path: ${classesRef.path}")

            classesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("StudentTimetable", "Classes snapshot exists: ${snapshot.exists()}")

                    if (!snapshot.exists()) {
                        Log.w("StudentTimetable", "No classes found for student. Returning empty list.")
                        callback(emptyList())
                        return
                    }

                    val classIds = mutableListOf<String>()
                    for (classSnapshot in snapshot.children) {
                        classSnapshot.getValue(String::class.java)?.let {
                            classIds.add(it)
                            Log.v("StudentTimetable", "Found class ID: $it")
                        }
                    }

                    if (classIds.isEmpty()) {
                        Log.w("StudentTimetable", "No valid class IDs found. Returning empty list.")
                        callback(emptyList())
                        return
                    }

                    Log.d("StudentTimetable", "Total classes found: ${classIds.size}")

                    // Now get details for each class
                    val timetableEntries = mutableListOf<Timetable>()
                    var processedCount = 0

                    for (classId in classIds) {
                        Log.d("StudentTimetable", "Processing class ID: $classId")

                        database.getReference("classes").child(classId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(classSnapshot: DataSnapshot) {
                                    Log.d("StudentTimetable", "Class snapshot retrieved for $classId")

                                    val className = classSnapshot.child("name").getValue(String::class.java)?.trim('"') ?: "Unknown Class"
                                    Log.v("StudentTimetable", "Class name: $className")

                                    // Get schedule info
                                    val day = classSnapshot.child("schedule/day").getValue(String::class.java) ?: ""
                                    val time = classSnapshot.child("schedule/time").getValue(String::class.java) ?: ""
                                    val room = classSnapshot.child("schedule/room").getValue(String::class.java) ?: ""

                                    Log.v("StudentTimetable", "Class schedule - Day: $day, Time: $time, Room: $room")

                                    // Get teacher info
                                    val teacherId = classSnapshot.child("teacherId").getValue(String::class.java) ?: ""
                                    Log.v("StudentTimetable", "Teacher ID: $teacherId")

                                    database.getReference("users/teachers").child(teacherId)
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(teacherSnapshot: DataSnapshot) {
                                                Log.d("StudentTimetable", "Teacher snapshot retrieved for $teacherId")

                                                val instructorName = teacherSnapshot.child("name").getValue(String::class.java) ?: "Unknown Instructor"
                                                Log.v("StudentTimetable", "Instructor name: $instructorName")

                                                // Create timetable entry
                                                val timetable = Timetable(
                                                    id = classId,
                                                    day = day,
                                                    timeSlot = time,
                                                    className = className,
                                                    instructorName = instructorName
                                                )

                                                timetableEntries.add(timetable)
                                                Log.d("StudentTimetable", "Added timetable entry for $className")

                                                // Check if we've processed all classes
                                                processedCount++
                                                Log.d("StudentTimetable", "Processed $processedCount of ${classIds.size} classes")

                                                if (processedCount == classIds.size) {
                                                    Log.i("StudentTimetable", "All classes processed. Returning ${timetableEntries.size} timetable entries")
                                                    callback(timetableEntries)
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                Log.e("StudentTimetable", "Error fetching teacher data: ${error.message}")
                                                processedCount++

                                                if (processedCount == classIds.size) {
                                                    Log.i("StudentTimetable", "All classes processed (with teacher fetch error). Returning ${timetableEntries.size} timetable entries")
                                                    callback(timetableEntries)
                                                }
                                            }
                                        })
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e("StudentTimetable", "Error fetching class data: ${error.message}")
                                    processedCount++

                                    if (processedCount == classIds.size) {
                                        Log.i("StudentTimetable", "All classes processed (with class fetch error). Returning ${timetableEntries.size} timetable entries")
                                        callback(timetableEntries)
                                    }
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("StudentTimetable", "Error fetching student classes: ${error.message}")
                    callback(emptyList())
                }
            })
        }
    }

    // Student Courses Methods
    fun getStudentCourses(callback: (List<Map<String, Any>>) -> Unit) {
        getCurrentStudentId { studentId ->
            if (studentId == null) {
                callback(emptyList())
                return@getCurrentStudentId
            }

            database.getReference("users/Students")
                .child(studentId)
                .child("classes")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            callback(emptyList())
                            return
                        }

                        val courseIds = mutableListOf<String>()
                        for (courseSnapshot in snapshot.children) {
                            courseSnapshot.getValue(String::class.java)?.let {
                                courseIds.add(it)
                            }
                        }

                        if (courseIds.isEmpty()) {
                            callback(emptyList())
                            return
                        }

                        val courses = mutableListOf<Map<String, Any>>()
                        var processedCount = 0

                        for (courseId in courseIds) {
                            database.getReference("classes").child(courseId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(courseSnapshot: DataSnapshot) {
                                        if (courseSnapshot.exists()) {
                                            val name = courseSnapshot.child("name").getValue(String::class.java) ?: "Unknown Course"
                                            val teacherId = courseSnapshot.child("teacherId").getValue(String::class.java) ?: ""

                                            // Get teacher name
                                            database.getReference("users/teachers").child(teacherId)
                                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                                    override fun onDataChange(teacherSnapshot: DataSnapshot) {
                                                        val instructorName = teacherSnapshot.child("name").getValue(String::class.java) ?: "Unknown Instructor"

                                                        val courseMap = mapOf(
                                                            "id" to courseId,
                                                            "name" to name,
                                                            "instructorName" to instructorName,
                                                            "section" to "" // Add section if available in your DB
                                                        )

                                                        courses.add(courseMap)

                                                        processedCount++
                                                        if (processedCount == courseIds.size) {
                                                            callback(courses)
                                                        }
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {
                                                        processedCount++
                                                        if (processedCount == courseIds.size) {
                                                            callback(courses)
                                                        }
                                                    }
                                                })
                                        } else {
                                            processedCount++
                                            if (processedCount == courseIds.size) {
                                                callback(courses)
                                            }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        processedCount++
                                        if (processedCount == courseIds.size) {
                                            callback(courses)
                                        }
                                    }
                                })
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        callback(emptyList())
                    }
                })
        }
    }

    // Attendance Session Methods
    fun getActiveSessions(callback: (List<Map<String, Any>>) -> Unit) {
        database.getReference("attendance_sessions")
            .orderByChild("status")
            .equalTo("active")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sessionsList = mutableListOf<Map<String, Any>>()
                    for (sessionSnapshot in snapshot.children) {
                        @Suppress("UNCHECKED_CAST")
                        val sessionData = sessionSnapshot.value as? Map<String, Any>
                        if (sessionData != null) {
                            val sessionWithId = HashMap(sessionData)
                            sessionWithId["id"] = sessionSnapshot.key ?: ""
                            sessionsList.add(sessionWithId)
                        }
                    }
                    callback(sessionsList)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList())
                }
            })
    }

    // Attendance Recording Methods
    fun markAttendance(
        studentId: String,
        courseId: String,
        sessionId: String,
        location: Map<String, Double>,
        callback: (Boolean) -> Unit
    ) {
        getCurrentStudentId { actualStudentId ->
            if (actualStudentId == null) {
                callback(false)
                return@getCurrentStudentId
            }

            val attendanceRef = database.getReference("attendanceRecords")
                .child(courseId)
                .child(getCurrentDateTime())
                .child(actualStudentId)

            attendanceRef.setValue(true)
                .addOnSuccessListener {
                    callback(true)
                }
                .addOnFailureListener {
                    callback(false)
                }
        }
    }

    // Utility Methods
    fun getCurrentDateTime(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}${
            String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
        }"
    }
}