package com.example.data.model

import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    CANCELLED
}

data class AttendanceStats(
    val totalConducted: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val cancelledCount: Int = 0,
    val percentage: Float? = null, // null when totalConducted == 0
    val targetThreshold: Float = 75f
) {
    val isAboveThreshold: Boolean
        get() = percentage?.let { it >= targetThreshold } ?: true
}

data class SubjectWithStats(
    val subject: SubjectEntity,
    val stats: AttendanceStats,
    val requiredConsecutiveClasses: Int = 0,
    val safeBunks: Int = 0,
    val scheduledSlotsCount: Int = 0
)

data class OverallAttendanceStats(
    val totalConducted: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val cancelledCount: Int = 0,
    val percentage: Float? = null,
    val targetThreshold: Float = 75f,
    val requiredConsecutiveClasses: Int = 0,
    val safeBunks: Int = 0,
    val subjectsBelowThresholdCount: Int = 0,
    val totalSubjectsCount: Int = 0,
    val todayCompleted: Int = 0,
    val todayPending: Int = 0,
    val todayTotal: Int = 0
) {
    val isAboveThreshold: Boolean
        get() = percentage?.let { it >= targetThreshold } ?: true
}

data class TodayLectureItem(
    val slot: TimetableSlotEntity?,
    val subject: SubjectEntity,
    val record: AttendanceRecordEntity?,
    val isExtra: Boolean = false
) {
    val currentStatus: AttendanceStatus?
        get() = record?.status?.let {
            try {
                AttendanceStatus.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
}

data class DayAttendanceSummary(
    val date: String, // "YYYY-MM-DD"
    val totalLectures: Int,
    val presentLectures: Int,
    val absentLectures: Int,
    val cancelledLectures: Int,
    val pendingLectures: Int
)

data class PeriodFilter(
    val type: FilterType,
    val title: String,
    val startDate: String,
    val endDate: String
) {
    enum class FilterType {
        TODAY,
        THIS_WEEK,
        THIS_MONTH,
        SELECTED_MONTH,
        SEMESTER,
        ACADEMIC_YEAR,
        ALL_TIME
    }
}
