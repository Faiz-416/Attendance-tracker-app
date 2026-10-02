package com.example.data.model

data class ParsedTimetableRow(
    val rowId: Long = 0,
    val schemaVersion: Int = 1,
    val institution: String = "",
    val department: String = "",
    val academicYear: String = "",
    val semester: String = "",
    val branch: String = "",
    val yearOfStudy: String = "",
    val section: String = "",
    val batch: String = "",
    val day: String = "",
    val dayOfWeek: Int = 1, // 1..7
    val periodNumber: Int = 1,
    val startTime: String = "09:00",
    val endTime: String = "10:00",
    val subjectCode: String = "",
    val subjectName: String = "",
    val classType: String = "Lecture",
    val room: String = "",
    val teacher: String = "",
    val sessionId: String = "",
    val notes: String = "",
    val issues: List<String> = emptyList()
) {
    val isValid: Boolean
        get() = issues.isEmpty()
}

data class TimetableGroup(
    val id: String, // composite unique key
    val institution: String,
    val department: String,
    val branch: String,
    val yearOfStudy: String,
    val semester: String,
    val section: String,
    val batch: String,
    val totalEntries: Int,
    val uniqueSubjectsCount: Int,
    val weekdaysCount: Int,
    val sampleSubjects: List<String>
) {
    val displayTitle: String
        get() {
            val parts = mutableListOf<String>()
            if (branch.isNotBlank()) parts.add(branch)
            else if (department.isNotBlank()) parts.add(department)

            if (yearOfStudy.isNotBlank()) parts.add("Year $yearOfStudy")
            if (semester.isNotBlank()) parts.add("Sem $semester")
            if (section.isNotBlank()) parts.add("Sec $section")
            if (batch.isNotBlank()) parts.add("Batch $batch")

            return if (parts.isNotEmpty()) parts.joinToString(" • ") else "Default Timetable Group"
        }

    val subtitle: String
        get() {
            val parts = mutableListOf<String>()
            if (institution.isNotBlank()) parts.add(institution)
            if (department.isNotBlank() && department != branch) parts.add(department)
            return parts.joinToString(" • ")
        }
}

data class ImportValidationSummary(
    val totalRows: Int = 0,
    val validRows: Int = 0,
    val rowsWithWarnings: Int = 0,
    val uniqueSubjectsCount: Int = 0,
    val weekdaysCount: Int = 0,
    val detectedGroupsCount: Int = 0,
    val issuesList: List<String> = emptyList()
)

enum class ImportConflictMode {
    REPLACE_EXISTING_TIMETABLE,
    MERGE_WITH_EXISTING
}
