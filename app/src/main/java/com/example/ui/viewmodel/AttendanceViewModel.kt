package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AcademicSettingEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.data.model.*
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SubjectRepository
import com.example.data.repository.TimetableRepository
import com.example.util.AttendanceCalculator
import com.example.util.CsvParser
import com.example.util.FullBackupData
import com.example.util.JsonBackupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val subjectRepo = SubjectRepository(db.subjectDao())
    private val timetableRepo = TimetableRepository(db.timetableDao())
    private val attendanceRepo = AttendanceRepository(db.attendanceDao())
    private val settingsRepo = SettingsRepository(db.academicSettingDao())

    // Settings
    val academicSettings: StateFlow<AcademicSettingEntity> = settingsRepo.settings
        .map { it ?: AcademicSettingEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AcademicSettingEntity()
        )

    // Subjects
    val activeSubjects: StateFlow<List<SubjectEntity>> = subjectRepo.activeSubjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allSubjects: StateFlow<List<SubjectEntity>> = subjectRepo.allSubjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Timetable Slots
    val timetableSlots: StateFlow<List<TimetableSlotEntity>> = timetableRepo.allSlots
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Attendance Records
    val attendanceRecords: StateFlow<List<AttendanceRecordEntity>> = attendanceRepo.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selected Dates
    private val _selectedTodayDate = MutableStateFlow(LocalDate.now().toString())
    val selectedTodayDate: StateFlow<String> = _selectedTodayDate.asStateFlow()

    private val _calendarSelectedDate = MutableStateFlow(LocalDate.now().toString())
    val calendarSelectedDate: StateFlow<String> = _calendarSelectedDate.asStateFlow()

    private val _calendarSelectedMonth = MutableStateFlow(YearMonth.now().toString()) // "YYYY-MM"
    val calendarSelectedMonth: StateFlow<String> = _calendarSelectedMonth.asStateFlow()

    // Undo Stack for Quick Undo
    private var lastMarkedRecord: AttendanceRecordEntity? = null
    private var previousRecordState: AttendanceRecordEntity? = null
    private val _undoAvailable = MutableStateFlow(false)
    val undoAvailable: StateFlow<Boolean> = _undoAvailable.asStateFlow()

    // Analytics Filters
    private val _analyticsPeriod = MutableStateFlow(PeriodFilter.FilterType.ALL_TIME)
    val analyticsPeriod: StateFlow<PeriodFilter.FilterType> = _analyticsPeriod.asStateFlow()

    private val _analyticsSubjectId = MutableStateFlow<Long?>(null)
    val analyticsSubjectId: StateFlow<Long?> = _analyticsSubjectId.asStateFlow()

    // Computed: Subject with Stats
    val subjectsWithStats: StateFlow<List<SubjectWithStats>> = combine(
        activeSubjects,
        attendanceRecords,
        timetableSlots,
        academicSettings
    ) { subjects, records, slots, settings ->
        val defaultThreshold = settings.defaultThreshold

        subjects.map { subj ->
            val subjRecords = records.filter { it.subjectId == subj.id }
            val presentCount = subjRecords.count { it.status == AttendanceStatus.PRESENT.name }
            val absentCount = subjRecords.count { it.status == AttendanceStatus.ABSENT.name }
            val cancelledCount = subjRecords.count { it.status == AttendanceStatus.CANCELLED.name }
            val conducted = presentCount + absentCount
            val percentage = AttendanceCalculator.calculatePercentage(presentCount, absentCount)
            val threshold = if (subj.targetThreshold > 0) subj.targetThreshold else defaultThreshold

            val required = AttendanceCalculator.calculateRequiredConsecutive(presentCount, absentCount, threshold)
            val safe = AttendanceCalculator.calculateSafeBunks(presentCount, absentCount, threshold)
            val scheduledCount = slots.count { it.subjectId == subj.id }

            SubjectWithStats(
                subject = subj,
                stats = AttendanceStats(
                    totalConducted = conducted,
                    presentCount = presentCount,
                    absentCount = absentCount,
                    cancelledCount = cancelledCount,
                    percentage = percentage,
                    targetThreshold = threshold
                ),
                requiredConsecutiveClasses = required,
                safeBunks = safe,
                scheduledSlotsCount = scheduledCount
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Computed: Today's Lectures
    val todayLectures: StateFlow<List<TodayLectureItem>> = combine(
        selectedTodayDate,
        timetableSlots,
        attendanceRecords,
        allSubjects
    ) { dateStr, slots, records, subjects ->
        val localDate = try { LocalDate.parse(dateStr) } catch (e: Exception) { LocalDate.now() }
        val dayOfWeek = localDate.dayOfWeek.value
        val daySlots = slots.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startTime }
        val dateRecords = records.filter { it.date == dateStr }

        val items = mutableListOf<TodayLectureItem>()
        val matchedRecordIds = mutableSetOf<Long>()

        // 1. Matched scheduled slots
        daySlots.forEach { slot ->
            val subj = subjects.find { it.id == slot.subjectId } ?: SubjectEntity(
                id = slot.subjectId,
                name = "Subject #${slot.subjectId}"
            )
            val matchingRecord = dateRecords.find {
                it.subjectId == slot.subjectId && it.periodNumber == slot.periodNumber
            }
            if (matchingRecord != null) {
                matchedRecordIds.add(matchingRecord.id)
            }
            items.add(
                TodayLectureItem(
                    slot = slot,
                    subject = subj,
                    record = matchingRecord,
                    isExtra = false
                )
            )
        }

        // 2. Extra / Manual records for this day not tied to scheduled slots
        val extraRecords = dateRecords.filter { it.id !in matchedRecordIds }
        extraRecords.forEach { rec ->
            val subj = subjects.find { it.id == rec.subjectId } ?: SubjectEntity(
                id = rec.subjectId,
                name = "Subject #${rec.subjectId}"
            )
            items.add(
                TodayLectureItem(
                    slot = null,
                    subject = subj,
                    record = rec,
                    isExtra = true
                )
            )
        }

        items
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Computed: Overall Attendance Stats
    val overallStats: StateFlow<OverallAttendanceStats> = combine(
        attendanceRecords,
        subjectsWithStats,
        academicSettings,
        todayLectures
    ) { records, subjStatsList, settings, todayItems ->
        val presentCount = records.count { it.status == AttendanceStatus.PRESENT.name }
        val absentCount = records.count { it.status == AttendanceStatus.ABSENT.name }
        val cancelledCount = records.count { it.status == AttendanceStatus.CANCELLED.name }
        val conducted = presentCount + absentCount
        val percentage = AttendanceCalculator.calculatePercentage(presentCount, absentCount)
        val threshold = settings.defaultThreshold

        val required = AttendanceCalculator.calculateRequiredConsecutive(presentCount, absentCount, threshold)
        val safe = AttendanceCalculator.calculateSafeBunks(presentCount, absentCount, threshold)

        val belowCount = subjStatsList.count { !it.stats.isAboveThreshold && it.stats.totalConducted > 0 }

        val todayCompleted = todayItems.count { it.record != null }
        val todayPending = todayItems.count { it.record == null }
        val todayTotal = todayItems.size

        OverallAttendanceStats(
            totalConducted = conducted,
            presentCount = presentCount,
            absentCount = absentCount,
            cancelledCount = cancelledCount,
            percentage = percentage,
            targetThreshold = threshold,
            requiredConsecutiveClasses = required,
            safeBunks = safe,
            subjectsBelowThresholdCount = belowCount,
            totalSubjectsCount = subjStatsList.size,
            todayCompleted = todayCompleted,
            todayPending = todayPending,
            todayTotal = todayTotal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OverallAttendanceStats()
    )

    // Import Workflow State
    private val _importState = MutableStateFlow(ImportUiState())
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    init {
        // Initialize settings if empty
        viewModelScope.launch {
            val existing = settingsRepo.getSettingsSync()
            if (existing == null) {
                settingsRepo.saveSettings(AcademicSettingEntity())
            }
        }
    }

    // --- Date Navigation ---
    fun setSelectedTodayDate(date: String) {
        _selectedTodayDate.value = date
    }

    fun stepTodayDate(days: Long) {
        val current = try { LocalDate.parse(_selectedTodayDate.value) } catch (e: Exception) { LocalDate.now() }
        _selectedTodayDate.value = current.plusDays(days).toString()
    }

    fun setCalendarSelectedDate(date: String) {
        _calendarSelectedDate.value = date
        try {
            val ym = YearMonth.parse(date.substring(0, 7))
            _calendarSelectedMonth.value = ym.toString()
        } catch (_: Exception) {}
    }

    fun setCalendarSelectedMonth(yearMonth: String) {
        _calendarSelectedMonth.value = yearMonth
    }

    fun setAnalyticsPeriod(period: PeriodFilter.FilterType) {
        _analyticsPeriod.value = period
    }

    fun setAnalyticsSubjectId(subjectId: Long?) {
        _analyticsSubjectId.value = subjectId
    }

    // --- Attendance Operations ---

    fun markAttendance(
        subjectId: Long,
        date: String,
        periodNumber: Int,
        status: AttendanceStatus,
        startTime: String = "",
        endTime: String = "",
        timetableSlotId: Long? = null,
        notes: String = "",
        absenceReason: String = ""
    ) {
        viewModelScope.launch {
            val existing = attendanceRepo.getRecordForSlot(date, subjectId, periodNumber)
            previousRecordState = existing

            val newRecord = AttendanceRecordEntity(
                id = existing?.id ?: 0,
                date = date,
                subjectId = subjectId,
                periodNumber = periodNumber,
                startTime = startTime.ifBlank { existing?.startTime ?: "09:00" },
                endTime = endTime.ifBlank { existing?.endTime ?: "10:00" },
                status = status.name,
                notes = notes.ifBlank { existing?.notes ?: "" },
                absenceReason = absenceReason.ifBlank { existing?.absenceReason ?: "" },
                source = if (timetableSlotId != null) "TIMETABLE" else "MANUAL",
                timetableSlotId = timetableSlotId ?: existing?.timetableSlotId,
                updatedAt = System.currentTimeMillis()
            )

            val insertedId = attendanceRepo.insertRecord(newRecord)
            lastMarkedRecord = newRecord.copy(id = insertedId)
            _undoAvailable.value = true
        }
    }

    fun toggleAttendanceStatus(
        item: TodayLectureItem,
        newStatus: AttendanceStatus
    ) {
        val date = _selectedTodayDate.value
        val period = item.slot?.periodNumber ?: item.record?.periodNumber ?: 1
        val startTime = item.slot?.startTime ?: item.record?.startTime ?: "09:00"
        val endTime = item.slot?.endTime ?: item.record?.endTime ?: "10:00"

        // If clicked status is already the current status, delete the record (unmark)
        if (item.currentStatus == newStatus && item.record != null) {
            viewModelScope.launch {
                previousRecordState = item.record
                attendanceRepo.deleteRecordById(item.record.id)
                lastMarkedRecord = null
                _undoAvailable.value = true
            }
        } else {
            markAttendance(
                subjectId = item.subject.id,
                date = date,
                periodNumber = period,
                status = newStatus,
                startTime = startTime,
                endTime = endTime,
                timetableSlotId = item.slot?.id
            )
        }
    }

    fun markAllToday(status: AttendanceStatus) {
        val currentItems = todayLectures.value
        val date = _selectedTodayDate.value

        viewModelScope.launch {
            currentItems.forEach { item ->
                val period = item.slot?.periodNumber ?: item.record?.periodNumber ?: 1
                val startTime = item.slot?.startTime ?: item.record?.startTime ?: "09:00"
                val endTime = item.slot?.endTime ?: item.record?.endTime ?: "10:00"

                markAttendance(
                    subjectId = item.subject.id,
                    date = date,
                    periodNumber = period,
                    status = status,
                    startTime = startTime,
                    endTime = endTime,
                    timetableSlotId = item.slot?.id
                )
            }
        }
    }

    fun markRemainingToday(status: AttendanceStatus) {
        val currentItems = todayLectures.value
        val date = _selectedTodayDate.value

        viewModelScope.launch {
            currentItems.filter { it.record == null }.forEach { item ->
                val period = item.slot?.periodNumber ?: 1
                val startTime = item.slot?.startTime ?: "09:00"
                val endTime = item.slot?.endTime ?: "10:00"

                markAttendance(
                    subjectId = item.subject.id,
                    date = date,
                    periodNumber = period,
                    status = status,
                    startTime = startTime,
                    endTime = endTime,
                    timetableSlotId = item.slot?.id
                )
            }
        }
    }

    fun undoLastAction() {
        viewModelScope.launch {
            val marked = lastMarkedRecord
            val prev = previousRecordState

            if (marked != null) {
                if (prev != null) {
                    attendanceRepo.insertRecord(prev)
                } else {
                    attendanceRepo.deleteRecordById(marked.id)
                }
            } else if (prev != null) {
                attendanceRepo.insertRecord(prev)
            }

            lastMarkedRecord = null
            previousRecordState = null
            _undoAvailable.value = false
        }
    }

    fun deleteAttendanceRecord(recordId: Long) {
        viewModelScope.launch {
            attendanceRepo.deleteRecordById(recordId)
        }
    }

    fun clearAttendanceForDate(date: String) {
        viewModelScope.launch {
            attendanceRepo.deleteRecordsForDate(date)
        }
    }

    // --- Subject Operations ---

    fun saveSubject(subject: SubjectEntity, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = subjectRepo.insertSubject(subject)
            onComplete?.invoke(id)
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            subjectRepo.deleteSubject(subject)
        }
    }

    fun archiveSubject(subject: SubjectEntity, archive: Boolean) {
        viewModelScope.launch {
            subjectRepo.updateSubject(subject.copy(isArchived = archive))
        }
    }

    // --- Timetable Operations ---

    fun saveTimetableSlot(slot: TimetableSlotEntity) {
        viewModelScope.launch {
            timetableRepo.insertSlot(slot)
        }
    }

    fun deleteTimetableSlot(slotId: Long) {
        viewModelScope.launch {
            timetableRepo.deleteSlotById(slotId)
        }
    }

    fun clearAllTimetableSlots() {
        viewModelScope.launch {
            timetableRepo.deleteAllSlots()
        }
    }

    // --- Settings & Configuration ---

    fun updateSettings(settings: AcademicSettingEntity) {
        viewModelScope.launch {
            settingsRepo.saveSettings(settings)
        }
    }

    // --- Backup & Restore ---

    suspend fun getFullBackupJson(): String = withContext(Dispatchers.IO) {
        val settings = settingsRepo.getSettingsSync()
        val subjects = db.subjectDao().getAllSubjects().first()
        val slots = db.timetableDao().getAllSlots().first()
        val records = db.attendanceDao().getAllRecords().first()

        JsonBackupHelper.exportToJson(
            FullBackupData(
                settings = settings,
                subjects = subjects,
                timetableSlots = slots,
                attendanceRecords = records
            )
        )
    }

    suspend fun restoreFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        val backup = JsonBackupHelper.importFromJson(jsonString) ?: return@withContext false

        backup.settings?.let { settingsRepo.saveSettings(it) }
        if (backup.subjects.isNotEmpty()) {
            db.subjectDao().insertSubjects(backup.subjects)
        }
        if (backup.timetableSlots.isNotEmpty()) {
            db.timetableDao().insertSlots(backup.timetableSlots)
        }
        if (backup.attendanceRecords.isNotEmpty()) {
            db.attendanceDao().insertRecords(backup.attendanceRecords)
        }
        true
    }

    suspend fun exportAttendanceCsv(): String = withContext(Dispatchers.IO) {
        val records = db.attendanceDao().getAllRecords().first()
        val subjects = db.subjectDao().getAllSubjects().first().associateBy { it.id }
        CsvParser.exportAttendanceCsv(records, subjects)
    }

    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            db.attendanceDao().deleteAllRecords()
            db.timetableDao().deleteAllSlots()
            db.subjectDao().deleteAllSubjects()
            settingsRepo.saveSettings(AcademicSettingEntity())
        }
    }

    // ==========================================
    // CHATGPT TIMETABLE IMPORT WORKFLOW
    // ==========================================

    fun startImportWorkflow() {
        _importState.value = ImportUiState(step = ImportStep.INSTRUCTIONS)
    }

    fun resetImportWorkflow() {
        _importState.value = ImportUiState(step = ImportStep.INSTRUCTIONS)
    }

    fun setImportStep(step: ImportStep) {
        _importState.update { it.copy(step = step) }
    }

    fun processImportedCsvContent(csvContent: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _importState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val parsedRows = CsvParser.parseTimetableCsv(csvContent)
                if (parsedRows.isEmpty()) {
                    _importState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "The file is empty or could not be parsed. Please verify the CSV format and try again."
                        )
                    }
                    return@launch
                }

                // Discover unique groups
                val groupsMap = mutableMapOf<String, MutableList<ParsedTimetableRow>>()
                parsedRows.forEach { row ->
                    val key = listOf(
                        row.institution,
                        row.department,
                        row.branch,
                        row.yearOfStudy,
                        row.semester,
                        row.section,
                        row.batch
                    ).joinToString("||")

                    groupsMap.getOrPut(key) { mutableListOf() }.add(row)
                }

                val groups = groupsMap.map { (key, rowList) ->
                    val first = rowList.first()
                    val uniqueSubjects = rowList.map { it.subjectName.ifBlank { it.subjectCode } }.filter { it.isNotBlank() }.distinct()
                    val weekdaysCount = rowList.map { it.dayOfWeek }.distinct().size

                    TimetableGroup(
                        id = key,
                        institution = first.institution,
                        department = first.department,
                        branch = first.branch,
                        yearOfStudy = first.yearOfStudy,
                        semester = first.semester,
                        section = first.section,
                        batch = first.batch,
                        totalEntries = rowList.size,
                        uniqueSubjectsCount = uniqueSubjects.size,
                        weekdaysCount = weekdaysCount,
                        sampleSubjects = uniqueSubjects.take(4)
                    )
                }

                val selectedGroup = groups.firstOrNull()
                val activeRows = if (selectedGroup != null) {
                    filterRowsForGroup(parsedRows, selectedGroup)
                } else {
                    parsedRows
                }

                val summary = generateValidationSummary(activeRows, groups.size)

                _importState.update {
                    it.copy(
                        isLoading = false,
                        allParsedRows = parsedRows,
                        detectedGroups = groups,
                        selectedGroup = selectedGroup,
                        activeGroupRows = activeRows,
                        validationSummary = summary,
                        step = if (groups.size > 1) ImportStep.BATCH_SELECTION else ImportStep.PREVIEW_AND_REVIEW,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _importState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error parsing CSV: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun selectTimetableGroup(group: TimetableGroup) {
        val allRows = _importState.value.allParsedRows
        val activeRows = filterRowsForGroup(allRows, group)
        val summary = generateValidationSummary(activeRows, _importState.value.detectedGroups.size)

        _importState.update {
            it.copy(
                selectedGroup = group,
                activeGroupRows = activeRows,
                validationSummary = summary,
                step = ImportStep.PREVIEW_AND_REVIEW
            )
        }
    }

    private fun filterRowsForGroup(allRows: List<ParsedTimetableRow>, group: TimetableGroup): List<ParsedTimetableRow> {
        return allRows.filter { row ->
            (group.batch.isBlank() || row.batch.isBlank() || row.batch.equals(group.batch, ignoreCase = true) || row.batch.equals("All", ignoreCase = true)) &&
            (group.section.isBlank() || row.section.isBlank() || row.section.equals(group.section, ignoreCase = true)) &&
            (group.semester.isBlank() || row.semester.isBlank() || row.semester.equals(group.semester, ignoreCase = true)) &&
            (group.branch.isBlank() || row.branch.isBlank() || row.branch.equals(group.branch, ignoreCase = true))
        }
    }

    private fun generateValidationSummary(rows: List<ParsedTimetableRow>, groupsCount: Int): ImportValidationSummary {
        val total = rows.size
        val valid = rows.count { it.isValid }
        val warnings = rows.count { !it.isValid }
        val uniqueSubjects = rows.map { it.subjectName.ifBlank { it.subjectCode } }.filter { it.isNotBlank() }.distinct().size
        val weekdays = rows.map { it.dayOfWeek }.distinct().size
        val issues = rows.flatMap { it.issues }.distinct()

        return ImportValidationSummary(
            totalRows = total,
            validRows = valid,
            rowsWithWarnings = warnings,
            uniqueSubjectsCount = uniqueSubjects,
            weekdaysCount = weekdays,
            detectedGroupsCount = groupsCount,
            issuesList = issues
        )
    }

    fun updatePreviewRow(updatedRow: ParsedTimetableRow) {
        val currentRows = _importState.value.activeGroupRows.map {
            if (it.rowId == updatedRow.rowId) updatedRow else it
        }
        val summary = generateValidationSummary(currentRows, _importState.value.detectedGroups.size)
        _importState.update {
            it.copy(
                activeGroupRows = currentRows,
                validationSummary = summary
            )
        }
    }

    fun deletePreviewRow(rowId: Long) {
        val currentRows = _importState.value.activeGroupRows.filter { it.rowId != rowId }
        val summary = generateValidationSummary(currentRows, _importState.value.detectedGroups.size)
        _importState.update {
            it.copy(
                activeGroupRows = currentRows,
                validationSummary = summary
            )
        }
    }

    fun confirmAndCommitImport(
        conflictMode: ImportConflictMode,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _importState.update { it.copy(isLoading = true) }

            try {
                val state = _importState.value
                val rowsToImport = state.activeGroupRows
                val selectedGroup = state.selectedGroup

                // 1. If replacing timetable, delete current slots (preserving historical attendance)
                if (conflictMode == ImportConflictMode.REPLACE_EXISTING_TIMETABLE) {
                    timetableRepo.deleteAllSlots()
                }

                // 2. Resolve / create subjects
                val existingSubjects = db.subjectDao().getAllSubjects().first()
                val subjectCodeMap = existingSubjects.associateBy { it.code.uppercase().trim() }.toMutableMap()
                val subjectNameMap = existingSubjects.associateBy { it.name.uppercase().trim() }.toMutableMap()

                // Default subject color palette
                val subjectColors = listOf(
                    "#10B981", "#3B82F6", "#8B5CF6", "#EC4899",
                    "#F59E0B", "#06B6D4", "#14B8A6", "#6366F1",
                    "#84CC16", "#F97316"
                )
                var colorIdx = existingSubjects.size

                val resolvedSubjectIdMap = mutableMapOf<String, Long>() // key: name/code -> subjectId

                rowsToImport.forEach { row ->
                    val codeKey = row.subjectCode.uppercase().trim()
                    val nameKey = row.subjectName.uppercase().trim()
                    val subjectKey = if (codeKey.isNotBlank()) codeKey else nameKey

                    if (!resolvedSubjectIdMap.containsKey(subjectKey)) {
                        val matched = (if (codeKey.isNotBlank()) subjectCodeMap[codeKey] else null)
                            ?: (if (nameKey.isNotBlank()) subjectNameMap[nameKey] else null)

                        if (matched != null) {
                            resolvedSubjectIdMap[subjectKey] = matched.id
                        } else {
                            val color = subjectColors[colorIdx % subjectColors.size]
                            colorIdx++

                            val newSubj = SubjectEntity(
                                name = row.subjectName.ifBlank { row.subjectCode.ifBlank { "Untitled Subject" } },
                                code = row.subjectCode,
                                teacher = row.teacher,
                                room = row.room,
                                colorHex = color,
                                targetThreshold = academicSettings.value.defaultThreshold
                            )
                            val newId = subjectRepo.insertSubject(newSubj)
                            val insertedSubj = newSubj.copy(id = newId)

                            if (codeKey.isNotBlank()) subjectCodeMap[codeKey] = insertedSubj
                            if (nameKey.isNotBlank()) subjectNameMap[nameKey] = insertedSubj
                            resolvedSubjectIdMap[subjectKey] = newId
                        }
                    }
                }

                // 3. Create Timetable Slots
                val slotsToInsert = rowsToImport.map { row ->
                    val codeKey = row.subjectCode.uppercase().trim()
                    val nameKey = row.subjectName.uppercase().trim()
                    val subjectKey = if (codeKey.isNotBlank()) codeKey else nameKey
                    val subjectId = resolvedSubjectIdMap[subjectKey] ?: 1L

                    TimetableSlotEntity(
                        subjectId = subjectId,
                        dayOfWeek = row.dayOfWeek,
                        periodNumber = row.periodNumber,
                        startTime = row.startTime,
                        endTime = row.endTime,
                        classType = row.classType,
                        room = row.room,
                        teacher = row.teacher,
                        batch = row.batch,
                        sessionId = row.sessionId,
                        notes = row.notes
                    )
                }

                timetableRepo.insertSlots(slotsToInsert)

                // 4. Update Academic Settings metadata if extracted
                selectedGroup?.let { g ->
                    val currentSettings = settingsRepo.getSettingsSync() ?: AcademicSettingEntity()
                    val updated = currentSettings.copy(
                        institution = g.institution.ifBlank { currentSettings.institution },
                        department = g.department.ifBlank { currentSettings.department },
                        branch = g.branch.ifBlank { currentSettings.branch },
                        semesterName = if (g.semester.isNotBlank()) "Semester ${g.semester}" else currentSettings.semesterName,
                        semesterNumber = g.semester.toIntOrNull() ?: currentSettings.semesterNumber,
                        yearOfStudy = g.yearOfStudy.toIntOrNull() ?: currentSettings.yearOfStudy,
                        section = g.section.ifBlank { currentSettings.section },
                        batch = g.batch.ifBlank { currentSettings.batch },
                        isSetupCompleted = true
                    )
                    settingsRepo.saveSettings(updated)
                }

                _importState.update {
                    it.copy(
                        isLoading = false,
                        step = ImportStep.COMPLETED
                    )
                }

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                _importState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to import timetable: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }
}

// UI State & Step models for Import
enum class ImportStep {
    INSTRUCTIONS,
    BATCH_SELECTION,
    PREVIEW_AND_REVIEW,
    COMPLETED
}

data class ImportUiState(
    val step: ImportStep = ImportStep.INSTRUCTIONS,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val allParsedRows: List<ParsedTimetableRow> = emptyList(),
    val detectedGroups: List<TimetableGroup> = emptyList(),
    val selectedGroup: TimetableGroup? = null,
    val activeGroupRows: List<ParsedTimetableRow> = emptyList(),
    val validationSummary: ImportValidationSummary = ImportValidationSummary()
)
