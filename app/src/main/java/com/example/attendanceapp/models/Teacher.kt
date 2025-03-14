package com.example.attendanceapp.models

data class Teacher(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val facultyId: String = "",
    val courses: List<String> = emptyList(),
    val department: String = ""
)