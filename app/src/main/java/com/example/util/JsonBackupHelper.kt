package com.example.util

import com.example.data.local.entity.AcademicSettingEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import org.json.JSONArray
import org.json.JSONObject

data class FullBackupData(
    val exportTimestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0",
    val settings: AcademicSettingEntity?,
    val subjects: List<SubjectEntity>,
    val timetableSlots: List<TimetableSlotEntity>,
    val attendanceRecords: List<AttendanceRecordEntity>
)

object JsonBackupHelper {

    fun exportToJson(data: FullBackupData): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", data.exportTimestamp)
        root.put("app", "AttendanceTracker")

        // Settings
        data.settings?.let { s ->
            val sObj = JSONObject()
            sObj.put("institution", s.institution)
            sObj.put("department", s.department)
            sObj.put("branch", s.branch)
            sObj.put("academicYear", s.academicYear)
            sObj.put("semesterName", s.semesterName)
            sObj.put("semesterNumber", s.semesterNumber)
            sObj.put("yearOfStudy", s.yearOfStudy)
            sObj.put("section", s.section)
            sObj.put("batch", s.batch)
            sObj.put("semesterStartDate", s.semesterStartDate)
            sObj.put("semesterEndDate", s.semesterEndDate)
            sObj.put("defaultThreshold", s.defaultThreshold.toDouble())
            sObj.put("isSetupCompleted", s.isSetupCompleted)
            root.put("settings", sObj)
        }

        // Subjects
        val subjArray = JSONArray()
        data.subjects.forEach { subj ->
            val obj = JSONObject()
            obj.put("id", subj.id)
            obj.put("name", subj.name)
            obj.put("code", subj.code)
            obj.put("teacher", subj.teacher)
            obj.put("room", subj.room)
            obj.put("colorHex", subj.colorHex)
            obj.put("targetThreshold", subj.targetThreshold.toDouble())
            obj.put("isArchived", subj.isArchived)
            subjArray.put(obj)
        }
        root.put("subjects", subjArray)

        // Timetable Slots
        val slotsArray = JSONArray()
        data.timetableSlots.forEach { slot ->
            val obj = JSONObject()
            obj.put("id", slot.id)
            obj.put("subjectId", slot.subjectId)
            obj.put("dayOfWeek", slot.dayOfWeek)
            obj.put("periodNumber", slot.periodNumber)
            obj.put("startTime", slot.startTime)
            obj.put("endTime", slot.endTime)
            obj.put("classType", slot.classType)
            obj.put("room", slot.room)
            obj.put("teacher", slot.teacher)
            obj.put("batch", slot.batch)
            obj.put("sessionId", slot.sessionId)
            obj.put("notes", slot.notes)
            slotsArray.put(obj)
        }
        root.put("timetableSlots", slotsArray)

        // Attendance Records
        val recordsArray = JSONArray()
        data.attendanceRecords.forEach { rec ->
            val obj = JSONObject()
            obj.put("id", rec.id)
            obj.put("date", rec.date)
            obj.put("subjectId", rec.subjectId)
            obj.put("periodNumber", rec.periodNumber)
            obj.put("startTime", rec.startTime)
            obj.put("endTime", rec.endTime)
            obj.put("status", rec.status)
            obj.put("notes", rec.notes)
            obj.put("absenceReason", rec.absenceReason)
            obj.put("source", rec.source)
            recordsArray.put(obj)
        }
        root.put("attendanceRecords", recordsArray)

        return root.toString(2)
    }

    fun importFromJson(jsonString: String): FullBackupData? {
        return try {
            val root = JSONObject(jsonString)
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())

            var settings: AcademicSettingEntity? = null
            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                settings = AcademicSettingEntity(
                    institution = sObj.optString("institution", ""),
                    department = sObj.optString("department", ""),
                    branch = sObj.optString("branch", ""),
                    academicYear = sObj.optString("academicYear", "2026-2027"),
                    semesterName = sObj.optString("semesterName", "Semester 1"),
                    semesterNumber = sObj.optInt("semesterNumber", 1),
                    yearOfStudy = sObj.optInt("yearOfStudy", 1),
                    section = sObj.optString("section", ""),
                    batch = sObj.optString("batch", ""),
                    semesterStartDate = sObj.optString("semesterStartDate", "2026-08-01"),
                    semesterEndDate = sObj.optString("semesterEndDate", "2026-12-31"),
                    defaultThreshold = sObj.optDouble("defaultThreshold", 75.0).toFloat(),
                    isSetupCompleted = sObj.optBoolean("isSetupCompleted", true)
                )
            }

            val subjects = mutableListOf<SubjectEntity>()
            if (root.has("subjects")) {
                val array = root.getJSONArray("subjects")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    subjects.add(
                        SubjectEntity(
                            id = obj.optLong("id", 0),
                            name = obj.optString("name", "Untitled Subject"),
                            code = obj.optString("code", ""),
                            teacher = obj.optString("teacher", ""),
                            room = obj.optString("room", ""),
                            colorHex = obj.optString("colorHex", "#10B981"),
                            targetThreshold = obj.optDouble("targetThreshold", 75.0).toFloat(),
                            isArchived = obj.optBoolean("isArchived", false)
                        )
                    )
                }
            }

            val timetableSlots = mutableListOf<TimetableSlotEntity>()
            if (root.has("timetableSlots")) {
                val array = root.getJSONArray("timetableSlots")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    timetableSlots.add(
                        TimetableSlotEntity(
                            id = obj.optLong("id", 0),
                            subjectId = obj.optLong("subjectId", 0),
                            dayOfWeek = obj.optInt("dayOfWeek", 1),
                            periodNumber = obj.optInt("periodNumber", 1),
                            startTime = obj.optString("startTime", "09:00"),
                            endTime = obj.optString("endTime", "10:00"),
                            classType = obj.optString("classType", "Lecture"),
                            room = obj.optString("room", ""),
                            teacher = obj.optString("teacher", ""),
                            batch = obj.optString("batch", ""),
                            sessionId = obj.optString("sessionId", ""),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
            }

            val attendanceRecords = mutableListOf<AttendanceRecordEntity>()
            if (root.has("attendanceRecords")) {
                val array = root.getJSONArray("attendanceRecords")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    attendanceRecords.add(
                        AttendanceRecordEntity(
                            id = obj.optLong("id", 0),
                            date = obj.optString("date", "2026-01-01"),
                            subjectId = obj.optLong("subjectId", 0),
                            periodNumber = obj.optInt("periodNumber", 1),
                            startTime = obj.optString("startTime", "09:00"),
                            endTime = obj.optString("endTime", "10:00"),
                            status = obj.optString("status", "PRESENT"),
                            notes = obj.optString("notes", ""),
                            absenceReason = obj.optString("absenceReason", ""),
                            source = obj.optString("source", "MANUAL")
                        )
                    )
                }
            }

            FullBackupData(
                exportTimestamp = timestamp,
                settings = settings,
                subjects = subjects,
                timetableSlots = timetableSlots,
                attendanceRecords = attendanceRecords
            )
        } catch (e: Exception) {
            null
        }
    }
}
