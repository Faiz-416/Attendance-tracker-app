package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["date", "subjectId", "periodNumber"], unique = false),
        Index(value = ["subjectId"]),
        Index(value = ["date"])
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // ISO date "YYYY-MM-DD"
    val subjectId: Long,
    val periodNumber: Int = 1,
    val startTime: String = "", // "HH:mm"
    val endTime: String = "",   // "HH:mm"
    val status: String,        // "PRESENT", "ABSENT", "CANCELLED"
    val notes: String = "",
    val absenceReason: String = "",
    val source: String = "MANUAL", // "MANUAL", "TIMETABLE_AUTO"
    val timetableSlotId: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
