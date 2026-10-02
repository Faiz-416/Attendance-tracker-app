package com.example

import com.example.data.local.entity.AcademicSettingEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.util.AttendanceCalculator
import com.example.util.CsvParser
import com.example.util.FullBackupData
import com.example.util.JsonBackupHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AttendanceTrackerLogicTest {

    @Test
    fun testAttendancePercentage() {
        // Zero conducted
        assertNull(AttendanceCalculator.calculatePercentage(0, 0))

        // Normal percentages
        assertEquals(100f, AttendanceCalculator.calculatePercentage(10, 0)!!, 0.001f)
        assertEquals(75f, AttendanceCalculator.calculatePercentage(15, 5)!!, 0.001f)
        assertEquals(50f, AttendanceCalculator.calculatePercentage(10, 10)!!, 0.001f)
        assertEquals(0f, AttendanceCalculator.calculatePercentage(0, 5)!!, 0.001f)
    }

    @Test
    fun testRequiredConsecutiveClasses() {
        // Current: 10 present, 10 absent (50%). Target = 75%
        // ceil((0.75 * 20 - 10) / (1 - 0.75)) = ceil((15 - 10) / 0.25) = ceil(5 / 0.25) = 20
        val needed = AttendanceCalculator.calculateRequiredConsecutive(10, 10, 75f)
        assertEquals(20, needed)

        // Verifying: 30 present / (30 present + 10 absent) = 30 / 40 = 75%
        val verifiedPercent = AttendanceCalculator.calculatePercentage(10 + 20, 10)
        assertEquals(75f, verifiedPercent!!, 0.001f)

        // Already above target
        assertEquals(0, AttendanceCalculator.calculateRequiredConsecutive(20, 0, 75f))
    }

    @Test
    fun testSafeBunks() {
        // Current: 30 present, 5 absent (85.7%). Target = 75%
        // floor(30 / 0.75 - 35) = floor(40 - 35) = 5
        val safe = AttendanceCalculator.calculateSafeBunks(30, 5, 75f)
        assertEquals(5, safe)

        // Verifying with 5 additional bunks: 30 / (30 + 10) = 30 / 40 = 75% (Target maintained)
        val verifiedPercent = AttendanceCalculator.calculatePercentage(30, 5 + 5)
        assertEquals(75f, verifiedPercent!!, 0.001f)

        // Currently below target -> safe bunks must be 0
        assertEquals(0, AttendanceCalculator.calculateSafeBunks(10, 10, 75f))
    }

    @Test
    fun testCsvParserWithQuotesAndCommas() {
        val sampleCsv = """
schema_version,institution,department,academic_year,semester,branch,year_of_study,section,batch,day,period_number,start_time,end_time,subject_code,subject_name,class_type,room,teacher,session_id,notes
1,"MIT, Pune","Computer Science & Engg",2026-2027,3,CSE,2,A,A1,Monday,1,09:00,10:00,CS301,"Data Structures, Advanced",Lecture,LH-101,"Prof. Turing, Alan",sess_1,"Note with ""escaped quotes"" and commas"
1,"MIT, Pune","Computer Science & Engg",2026-2027,3,CSE,2,A,A2,Monday,1,09:00,10:00,CS302,Digital Systems,Lab,Lab-2,Prof. Shannon,sess_2,Batch A2 Lab
        """.trimIndent()

        val parsed = CsvParser.parseTimetableCsv(sampleCsv)
        assertEquals(2, parsed.size)

        val row1 = parsed[0]
        assertEquals("MIT, Pune", row1.institution)
        assertEquals("Data Structures, Advanced", row1.subjectName)
        assertEquals("Prof. Turing, Alan", row1.teacher)
        assertEquals("Note with \"escaped quotes\" and commas", row1.notes)
        assertEquals("A1", row1.batch)
        assertEquals(1, row1.dayOfWeek) // Monday
        assertEquals("09:00", row1.startTime)
        assertEquals("10:00", row1.endTime)
        assertTrue(row1.isValid)

        val row2 = parsed[1]
        assertEquals("A2", row2.batch)
        assertEquals("Digital Systems", row2.subjectName)
    }

    @Test
    fun testJsonBackupAndRestore() {
        val settings = AcademicSettingEntity(
            institution = "Stanford University",
            department = "CS",
            academicYear = "2026-2027",
            defaultThreshold = 80f
        )
        val subject = SubjectEntity(id = 1, name = "Algorithms", code = "CS101", targetThreshold = 80f)
        val slot = TimetableSlotEntity(id = 10, subjectId = 1, dayOfWeek = 1, startTime = "09:00", endTime = "10:00")
        val record = AttendanceRecordEntity(id = 100, date = "2026-10-02", subjectId = 1, status = "PRESENT")

        val backupData = FullBackupData(
            settings = settings,
            subjects = listOf(subject),
            timetableSlots = listOf(slot),
            attendanceRecords = listOf(record)
        )

        val jsonString = JsonBackupHelper.exportToJson(backupData)
        assertTrue(jsonString.contains("Stanford University"))
        assertTrue(jsonString.contains("Algorithms"))

        val restored = JsonBackupHelper.importFromJson(jsonString)
        assertNotNull(restored)
        assertEquals("Stanford University", restored!!.settings?.institution)
        assertEquals(80f, restored.settings?.defaultThreshold ?: 0f, 0.001f)
        assertEquals(1, restored.subjects.size)
        assertEquals("Algorithms", restored.subjects[0].name)
        assertEquals(1, restored.attendanceRecords.size)
        assertEquals("PRESENT", restored.attendanceRecords[0].status)
    }
}
