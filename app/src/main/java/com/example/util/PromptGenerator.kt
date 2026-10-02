package com.example.util

object PromptGenerator {

    fun generateChatGptPrompt(
        institution: String = "",
        department: String = "",
        semester: String = "",
        batch: String = ""
    ): String {
        return """
Please act as an expert college timetable extraction engine. I am uploading an image, screenshot, or PDF of my college timetable schedule. 

Extract all scheduled classes, laboratories, practicals, tutorials, and seminars into a valid, standard downloadable CSV file according to the strict specification below.

### 1. CSV Schema & Column Order
The output CSV must start with the following header row in exact order:
schema_version,institution,department,academic_year,semester,branch,year_of_study,section,batch,day,period_number,start_time,end_time,subject_code,subject_name,class_type,room,teacher,session_id,notes

### 2. Schema Specification & Field Rules
- `schema_version`: Must always be 1
- `institution`: College or university name (if visible, e.g., "${institution.ifBlank { "University / College Name" }}")
- `department`: Department name (e.g., "${department.ifBlank { "Computer Science & Engineering" }}")
- `academicYear`: Academic year (e.g., "2026-2027")
- `semester`: Semester number or label (e.g., "${semester.ifBlank { "3" }}")
- `branch`: Course branch or specialization (e.g., "CSE", "ECE", "MECH")
- `year_of_study`: Year of study (1, 2, 3, or 4)
- `section`: Division or section (e.g., "A", "B")
- `batch`: Practical / lab batch group (e.g., "${batch.ifBlank { "A1" }}", "A2", "B1", "All"). If classes are shared across the whole section, set to "All" or leave consistent.
- `day`: Full English weekday name ("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
- `period_number`: Sequential period index for the day (1, 2, 3, 4, etc.)
- `start_time`: 24-hour format "HH:mm" (e.g., "09:00", "11:15", "14:00")
- `end_time`: 24-hour format "HH:mm" (e.g., "10:00", "12:15", "16:00")
- `subject_code`: Official course code if shown (e.g., "CS301", "MA201")
- `subject_name`: Full subject or course name (e.g., "Data Structures and Algorithms")
- `class_type`: Exactly one of: "Lecture", "Practical", "Lab", "Tutorial", "Seminar", "Other"
- `room`: Classroom, hall, or lab number (e.g., "LH-101", "Lab 3")
- `teacher`: Teacher / professor name (e.g., "Dr. Smith")
- `session_id`: Unique identifier grouping continuous multi-hour periods of the same class (e.g., "mon_p1", "mon_lab_a1")
- `notes`: Any special batch instructions or uncertainties

### 3. Extraction & Layout Instructions
1. Inspect the entire timetable carefully, including complex grid layouts, merged table cells, grouped practical batches, and row/column headers.
2. Distinguish actual classes from lunch breaks, tea breaks, recesses, library/sports periods, or empty free slots. Exclude non-academic breaks.
3. If an entry spans multiple consecutive hours (e.g., a 2-hour lab from 11:15 to 13:15), you may output it as either the full continuous slot or individual period rows with identical `session_id`.
4. If different batches have parallel sessions at the same time (e.g., Batch A1 does Lab in Room 1 while Batch A2 does Lab in Room 2), extract rows for EACH batch so the combined CSV contains all options.
5. Preserve accurate subject names and codes. Never invent fake names or timings. If a field is unknown or ambiguous, leave it blank (empty CSV field).
6. Ensure standard RFC 4180 CSV formatting: quote fields containing commas or quotes, and escape internal quotes by doubling them (`""`).
7. UTF-8 encoding with NO Markdown code fences or extra prose inside the downloadable CSV file.

### 4. Required Output Delivery
1. Provide a downloadable `.csv` file named `timetable_import.csv` using your file creation tool / Python environment.
2. In your chat message reply, provide a concise summary listing:
   - Total detected weekdays and lecture slots
   - List of all unique batch / division groups detected (e.g., "Branch: CSE, Year: 2, Division: A, Batches: A1, A2, A3")
   - A list of all unique subjects identified.
""".trimIndent()
    }
}
