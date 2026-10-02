package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timetable_slots",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"]), Index(value = ["dayOfWeek", "periodNumber"])]
)
data class TimetableSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, ..., 7 = Sunday (matches java.time.DayOfWeek.value)
    val periodNumber: Int = 1,
    val startTime: String = "09:00", // "HH:mm"
    val endTime: String = "10:00",   // "HH:mm"
    val classType: String = "Lecture", // Lecture, Practical, Lab, Tutorial, Seminar, Other
    val room: String = "",
    val teacher: String = "",
    val batch: String = "",
    val sessionId: String = "",
    val notes: String = "",
    val effectiveFrom: String = "", // ISO date e.g. "2026-01-01"
    val effectiveTo: String = ""
)
