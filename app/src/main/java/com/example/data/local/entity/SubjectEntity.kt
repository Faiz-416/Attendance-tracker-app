package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val teacher: String = "",
    val room: String = "",
    val colorHex: String = "#10B981", // Emerald
    val targetThreshold: Float = 75f,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
