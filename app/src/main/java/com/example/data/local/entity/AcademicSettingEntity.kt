package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "academic_settings")
data class AcademicSettingEntity(
    @PrimaryKey val id: Int = 1,
    val institution: String = "",
    val department: String = "",
    val branch: String = "",
    val academicYear: String = "2026-2027",
    val semesterName: String = "Semester 1",
    val semesterNumber: Int = 1,
    val yearOfStudy: Int = 1,
    val section: String = "",
    val batch: String = "",
    val semesterStartDate: String = "2026-08-01", // "YYYY-MM-DD"
    val semesterEndDate: String = "2026-12-31",   // "YYYY-MM-DD"
    val defaultThreshold: Float = 75f,
    val notificationsEnabled: Boolean = false,
    val dailyReminderTime: String = "17:00",
    val isSetupCompleted: Boolean = false
)
