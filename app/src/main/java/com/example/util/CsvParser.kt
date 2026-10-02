package com.example.util

import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.data.model.ParsedTimetableRow
import java.io.StringReader

object CsvParser {

    const val TIMETABLE_HEADER = "schema_version,institution,department,academic_year,semester,branch,year_of_study,section,batch,day,period_number,start_time,end_time,subject_code,subject_name,class_type,room,teacher,session_id,notes"

    /**
     * Parses RFC 4180 CSV text with UTF-8 BOM support, multiline cells, quotes and escaped quotes.
     */
    fun parseCsvText(rawText: String): List<List<String>> {
        // Strip BOM if present
        val text = rawText.removePrefix("\uFEFF").trim()
        if (text.isEmpty()) return emptyList()

        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()

        var inQuotes = false
        var i = 0
        val length = text.length

        while (i < length) {
            val c = text[i]

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < length && text[i + 1] == '"') {
                        // Escaped quote: "" -> "
                        currentField.append('"')
                        i += 2
                        continue
                    } else {
                        // End of quoted cell
                        inQuotes = false
                        i++
                        continue
                    }
                } else {
                    currentField.append(c)
                    i++
                    continue
                }
            } else {
                when (c) {
                    '"' -> {
                        inQuotes = true
                        i++
                    }
                    ',' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        i++
                    }
                    '\r' -> {
                        if (i + 1 < length && text[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.any { it.isNotEmpty() }) {
                            rows.add(ArrayList(currentRow))
                        }
                        currentRow.clear()
                        i++
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.any { it.isNotEmpty() }) {
                            rows.add(ArrayList(currentRow))
                        }
                        currentRow.clear()
                        i++
                    }
                    else -> {
                        currentField.append(c)
                        i++
                    }
                }
            }
        }

        // Add last field and row if remaining
        currentRow.add(currentField.toString().trim())
        if (currentRow.any { it.isNotEmpty() }) {
            rows.add(ArrayList(currentRow))
        }

        return rows
    }

    /**
     * Parses and validates raw CSV records into ParsedTimetableRow list.
     */
    fun parseTimetableCsv(csvContent: String): List<ParsedTimetableRow> {
        val rows = parseCsvText(csvContent)
        if (rows.isEmpty()) return emptyList()

        // Detect if first row is header
        val headerRow = rows.first()
        val isHeader = headerRow.any { col ->
            col.equals("schema_version", ignoreCase = true) ||
            col.equals("subject_name", ignoreCase = true) ||
            col.equals("start_time", ignoreCase = true) ||
            col.equals("day", ignoreCase = true)
        }

        val dataRows = if (isHeader) rows.drop(1) else rows
        val parsedList = mutableListOf<ParsedTimetableRow>()

        dataRows.forEachIndexed { index, rawCols ->
            // Skip empty rows
            if (rawCols.all { it.isBlank() }) return@forEachIndexed

            val issues = mutableListOf<String>()

            fun getCol(idx: Int): String {
                return if (idx < rawCols.size) rawCols[idx].trim() else ""
            }

            val schemaVersionStr = getCol(0)
            val schemaVer = schemaVersionStr.toIntOrNull() ?: 1

            val institution = getCol(1)
            val department = getCol(2)
            val academicYear = getCol(3)
            val semester = getCol(4)
            val branch = getCol(5)
            val yearOfStudy = getCol(6)
            val section = getCol(7)
            val batch = getCol(8)
            val day = getCol(9)
            val periodNumStr = getCol(10)
            val startTimeRaw = getCol(11)
            val endTimeRaw = getCol(12)
            val subjectCode = getCol(13)
            val subjectName = getCol(14)
            val classType = getCol(15).ifBlank { "Lecture" }
            val room = getCol(16)
            val teacher = getCol(17)
            val sessionId = getCol(18)
            val notes = getCol(19)

            // Validate Day
            val dayOfWeek = parseDayOfWeek(day)
            if (dayOfWeek == null) {
                issues.add("Invalid day of week: '$day'")
            }

            // Validate Subject
            if (subjectName.isBlank() && subjectCode.isBlank()) {
                issues.add("Missing subject name or code")
            }

            // Validate Times
            val normStart = normalizeTime(startTimeRaw)
            val normEnd = normalizeTime(endTimeRaw)
            if (normStart == null) {
                issues.add("Invalid start time: '$startTimeRaw' (expected HH:mm)")
            }
            if (normEnd == null) {
                issues.add("Invalid end time: '$endTimeRaw' (expected HH:mm)")
            }
            if (normStart != null && normEnd != null && normStart >= normEnd) {
                issues.add("Start time ($normStart) must be earlier than end time ($normEnd)")
            }

            val periodNumber = periodNumStr.toIntOrNull() ?: (index + 1)

            parsedList.add(
                ParsedTimetableRow(
                    rowId = (index + 1).toLong(),
                    schemaVersion = schemaVer,
                    institution = institution,
                    department = department,
                    academicYear = academicYear,
                    semester = semester,
                    branch = branch,
                    yearOfStudy = yearOfStudy,
                    section = section,
                    batch = batch,
                    day = day,
                    dayOfWeek = dayOfWeek ?: 1,
                    periodNumber = periodNumber,
                    startTime = normStart ?: startTimeRaw.ifBlank { "09:00" },
                    endTime = normEnd ?: endTimeRaw.ifBlank { "10:00" },
                    subjectCode = subjectCode,
                    subjectName = if (subjectName.isNotBlank()) subjectName else subjectCode,
                    classType = classType,
                    room = room,
                    teacher = teacher,
                    sessionId = sessionId,
                    notes = notes,
                    issues = issues
                )
            )
        }

        return parsedList
    }

    fun parseDayOfWeek(dayStr: String): Int? {
        val clean = dayStr.trim().lowercase()
        return when {
            clean.startsWith("mon") || clean == "1" -> 1
            clean.startsWith("tue") || clean == "2" -> 2
            clean.startsWith("wed") || clean == "3" -> 3
            clean.startsWith("thu") || clean == "4" -> 4
            clean.startsWith("fri") || clean == "5" -> 5
            clean.startsWith("sat") || clean == "6" -> 6
            clean.startsWith("sun") || clean == "7" -> 7
            else -> null
        }
    }

    fun dayOfWeekToString(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Monday"
        }
    }

    fun normalizeTime(timeStr: String): String? {
        val trimmed = timeStr.trim().replace(" ", "").uppercase()
        if (trimmed.isEmpty()) return null

        // Support formats like "9:00", "09:00", "9:00AM", "09:00 AM", "1:30PM", "13:30"
        var isPm = trimmed.endsWith("PM")
        var isAm = trimmed.endsWith("AM")
        val cleanTime = trimmed.removeSuffix("AM").removeSuffix("PM")

        val parts = cleanTime.split(":")
        if (parts.size != 2) return null

        var hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null

        if (minute !in 0..59) return null

        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0

        if (hour !in 0..23) return null

        return String.format("%02d:%02d", hour, minute)
    }

    /**
     * Escapes a single CSV field according to RFC 4180.
     */
    fun escapeCsvField(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    /**
     * Exports attendance records with subject names to CSV string.
     */
    fun exportAttendanceCsv(
        records: List<AttendanceRecordEntity>,
        subjectMap: Map<Long, SubjectEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("date,period_number,start_time,end_time,subject_code,subject_name,status,absence_reason,notes,source\n")
        records.forEach { r ->
            val subj = subjectMap[r.subjectId]
            val row = listOf(
                r.date,
                r.periodNumber.toString(),
                r.startTime,
                r.endTime,
                subj?.code ?: "",
                subj?.name ?: "Unknown Subject",
                r.status,
                r.absenceReason,
                r.notes,
                r.source
            ).map { escapeCsvField(it) }
            sb.append(row.joinToString(",")).append("\n")
        }
        return sb.toString()
    }

    /**
     * Generates a sample CSV for preview.
     */
    fun getSampleCsv(): String {
        return """schema_version,institution,department,academic_year,semester,branch,year_of_study,section,batch,day,period_number,start_time,end_time,subject_code,subject_name,class_type,room,teacher,session_id,notes
1,Engineering College,Computer Science,2026-2027,3,CSE,2,A,A1,Monday,1,09:00,10:00,CS301,Data Structures,Lecture,LH-101,Prof. Alan Turing,sess_1,
1,Engineering College,Computer Science,2026-2027,3,CSE,2,A,A1,Monday,2,10:00,11:00,CS302,Digital Electronics,Lecture,LH-101,Prof. Claude Shannon,sess_2,
1,Engineering College,Computer Science,2026-2027,3,CSE,2,A,A1,Monday,3,11:15,13:15,CS303,Data Structures Lab,Lab,Lab-2,Prof. Alan Turing,sess_3,Practical batch A1
1,Engineering College,Computer Science,2026-2027,3,CSE,2,A,A2,Monday,3,11:15,13:15,CS304,Digital Lab,Lab,Lab-4,Prof. Shannon,sess_4,Practical batch A2
1,Engineering College,Computer Science,2026-2027,3,CSE,2,A,A1,Tuesday,1,09:00,10:00,CS305,Discrete Mathematics,Lecture,LH-101,Prof. Euler,sess_5,"""
    }
}
