package com.example.attendanceapp.models

data class Student(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val studentId: String = "",
    val enrolledCourses: List<String> = emptyList(),
    val attendanceRecords: Map<String, AttendanceRecord> = emptyMap()
) {
    data class AttendanceRecord(
        val sessionId: String = "",
        val courseId: String = "",
        val timestamp: Long = 0,
        val status: String = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    )
}