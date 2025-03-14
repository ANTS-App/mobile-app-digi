package com.example.attendanceapp.models

data class Timetable(
    val className: String,
    val day: String,
    val timeSlot: String,
    val instructorName: String,
    val id: String
)