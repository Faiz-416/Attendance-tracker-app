package com.example.ui.screens.today

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.SubjectEntity
import com.example.data.model.AttendanceStatus
import com.example.ui.components.DayLectureCard
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: AttendanceViewModel,
    onNavigateToImport: () -> Unit,
    onNavigateToSubjects: () -> Unit
) {
    val selectedDateStr by viewModel.selectedTodayDate.collectAsStateWithLifecycle()
    val todayLectures by viewModel.todayLectures.collectAsStateWithLifecycle()
    val allSubjects by viewModel.activeSubjects.collectAsStateWithLifecycle()
    val undoAvailable by viewModel.undoAvailable.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val currentDate = try { LocalDate.parse(selectedDateStr) } catch (e: Exception) { LocalDate.now() }
    val isToday = currentDate == LocalDate.now()

    var showAddExtraDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val formattedDate = currentDate.format(DateTimeFormatter.ofPattern("EEE, MMMM d, yyyy", Locale.getDefault()))

    // DatePicker
    val datePickerDialog = remember(currentDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newDate = LocalDate.of(year, month + 1, dayOfMonth)
                viewModel.setSelectedTodayDate(newDate.toString())
            },
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isToday) "Today's Attendance" else "Attendance Tracker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isToday) BrightMint else TextMuted
                        )
                    }
                },
                actions = {
                    if (undoAvailable) {
                        IconButton(
                            onClick = { viewModel.undoLastAction() },
                            modifier = Modifier.testTag("btn_undo_attendance")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Undo,
                                contentDescription = "Undo Last Marking",
                                tint = BrightMint
                            )
                        }
                    }

                    IconButton(
                        onClick = { showAddExtraDialog = true },
                        modifier = Modifier.testTag("btn_add_extra_lecture")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCircleOutline,
                            contentDescription = "Add Extra Class",
                            tint = MintSecondary
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("today_overflow_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More Options",
                                tint = TextMuted
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(DarkSurfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mark All Present", color = TextWhite) },
                                leadingIcon = { Icon(Icons.Filled.DoneAll, null, tint = PresentGreen) },
                                onClick = {
                                    viewModel.markAllToday(AttendanceStatus.PRESENT)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mark Remaining Present", color = TextWhite) },
                                leadingIcon = { Icon(Icons.Filled.Check, null, tint = MintSecondary) },
                                onClick = {
                                    viewModel.markRemainingToday(AttendanceStatus.PRESENT)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mark All Absent", color = TextWhite) },
                                leadingIcon = { Icon(Icons.Filled.Close, null, tint = AbsentRed) },
                                onClick = {
                                    viewModel.markAllToday(AttendanceStatus.ABSENT)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Day's Attendance", color = TextWhite) },
                                leadingIcon = { Icon(Icons.Filled.DeleteSweep, null, tint = TextMuted) },
                                onClick = {
                                    viewModel.clearAttendanceForDate(selectedDateStr)
                                    showMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalBlack)
            )
        },
        containerColor = CharcoalBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Date Navigator Strip
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                backgroundColor = DarkSurfaceVariant.copy(alpha = 0.6f),
                borderColor = BorderDark,
                cornerRadius = 14.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.stepTodayDate(-1) },
                        modifier = Modifier.size(36.dp).testTag("btn_prev_day")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("btn_date_picker_dialog")
                    ) {
                        IconButton(
                            onClick = { datePickerDialog.show() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = "Pick Date",
                                tint = BrightMint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isToday) {
                            TextButton(
                                onClick = { viewModel.setSelectedTodayDate(LocalDate.now().toString()) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp).testTag("btn_jump_to_today")
                            ) {
                                Text("Today", color = BrightMint, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        IconButton(
                            onClick = { viewModel.stepTodayDate(1) },
                            modifier = Modifier.size(36.dp).testTag("btn_next_day")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Day",
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2. Day Summary Stats Bar
            val totalToday = todayLectures.size
            val presentCount = todayLectures.count { it.currentStatus == AttendanceStatus.PRESENT }
            val absentCount = todayLectures.count { it.currentStatus == AttendanceStatus.ABSENT }
            val cancelledCount = todayLectures.count { it.currentStatus == AttendanceStatus.CANCELLED }
            val pendingCount = todayLectures.count { it.currentStatus == null }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Summary Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Total: $totalToday",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PresentGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Present: $presentCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrightMint,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AbsentRed.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Absent: $absentCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = AbsentRed,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (pendingCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarningAmber.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pending: $pendingCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Fast Batch Action Bar
            if (pendingCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.markAllToday(AttendanceStatus.PRESENT) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_mark_all_present"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceVariant,
                            contentColor = BrightMint
                        ),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Filled.DoneAll, null, modifier = Modifier.size(16.dp), tint = BrightMint)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark All Present", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.markRemainingToday(AttendanceStatus.PRESENT) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_mark_remaining_present"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderDark),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Mark Remaining", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // 4. Lectures List or Empty State
            if (todayLectures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EventNote,
                            contentDescription = null,
                            tint = TextDim,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Classes Scheduled for this Day",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        Text(
                            text = "Enjoy your free day, add a manual class, or import your weekly timetable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { showAddExtraDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkSurfaceVariant,
                                    contentColor = TextWhite
                                ),
                                border = BorderStroke(1.dp, BorderDark),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_add_manual_lecture_empty")
                            ) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Class")
                            }

                            Button(
                                onClick = onNavigateToImport,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = CharcoalBlack
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_import_timetable_empty")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import Timetable", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    items(todayLectures, key = { item ->
                        val slotId = item.slot?.id ?: 0
                        val recId = item.record?.id ?: 0
                        "lecture_${item.subject.id}_${slotId}_${recId}_${item.slot?.periodNumber ?: item.record?.periodNumber}"
                    }) { item ->
                        DayLectureCard(
                            item = item,
                            onStatusToggle = { newStatus ->
                                viewModel.toggleAttendanceStatus(item, newStatus)
                            },
                            onEditDetails = { notes, reason ->
                                val period = item.slot?.periodNumber ?: item.record?.periodNumber ?: 1
                                val startTime = item.slot?.startTime ?: item.record?.startTime ?: "09:00"
                                val endTime = item.slot?.endTime ?: item.record?.endTime ?: "10:00"
                                val currentStatus = item.currentStatus ?: AttendanceStatus.PRESENT

                                viewModel.markAttendance(
                                    subjectId = item.subject.id,
                                    date = selectedDateStr,
                                    periodNumber = period,
                                    status = currentStatus,
                                    startTime = startTime,
                                    endTime = endTime,
                                    timetableSlotId = item.slot?.id,
                                    notes = notes,
                                    absenceReason = reason
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Extra / Manual Class Dialog
    if (showAddExtraDialog) {
        AddManualClassDialog(
            subjects = allSubjects,
            onDismiss = { showAddExtraDialog = false },
            onNavigateToSubjects = {
                showAddExtraDialog = false
                onNavigateToSubjects()
            },
            onAdd = { subjectId, periodNumber, startTime, endTime, status, notes ->
                viewModel.markAttendance(
                    subjectId = subjectId,
                    date = selectedDateStr,
                    periodNumber = periodNumber,
                    status = status,
                    startTime = startTime,
                    endTime = endTime,
                    notes = notes
                )
                showAddExtraDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualClassDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    onAdd: (subjectId: Long, periodNumber: Int, startTime: String, endTime: String, status: AttendanceStatus, notes: String) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var periodNumber by remember { mutableStateOf("1") }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var selectedStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var notes by remember { mutableStateOf("") }
    var expandedSubjDropdown by remember { mutableStateOf(false) }

    val currentSubject = subjects.find { it.id == selectedSubjectId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Extra / Manual Class",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        },
        text = {
            if (subjects.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "You don't have any subjects yet. Please add a subject first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNavigateToSubjects,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = CharcoalBlack)
                    ) {
                        Text("Add Subject", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Subject Selector
                    ExposedDropdownMenuBox(
                        expanded = expandedSubjDropdown,
                        onExpandedChange = { expandedSubjDropdown = !expandedSubjDropdown }
                    ) {
                        OutlinedTextField(
                            value = currentSubject?.name ?: "Select Subject",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subject") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubjDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("dropdown_manual_subject")
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSubjDropdown,
                            onDismissRequest = { expandedSubjDropdown = false },
                            modifier = Modifier.background(DarkSurfaceVariant)
                        ) {
                            subjects.forEach { subj ->
                                DropdownMenuItem(
                                    text = { Text(subj.name, color = TextWhite) },
                                    onClick = {
                                        selectedSubjectId = subj.id
                                        expandedSubjDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Period Number & Timings
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = periodNumber,
                            onValueChange = { periodNumber = it },
                            label = { Text("Period #") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_manual_period")
                        )
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Start (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f).testTag("input_manual_start_time")
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f).testTag("input_manual_end_time")
                        )
                    }

                    // Initial Status
                    Text(
                        text = "Mark As:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AttendanceStatus.values().forEach { status ->
                            val isSelected = selectedStatus == status
                            val color = when (status) {
                                AttendanceStatus.PRESENT -> PresentGreen
                                AttendanceStatus.ABSENT -> AbsentRed
                                AttendanceStatus.CANCELLED -> CancelledSlate
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStatus = status },
                                label = { Text(status.name.lowercase().capitalize(Locale.ROOT)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.2f),
                                    selectedLabelColor = color
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = BorderDark,
                                    selectedBorderColor = color,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        placeholder = { Text("e.g., Substitution / Extra lecture") },
                        modifier = Modifier.fillMaxWidth().testTag("input_manual_notes")
                    )
                }
            }
        },
        confirmButton = {
            if (subjects.isNotEmpty()) {
                Button(
                    onClick = {
                        val p = periodNumber.toIntOrNull() ?: 1
                        onAdd(selectedSubjectId, p, startTime, endTime, selectedStatus, notes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = CharcoalBlack),
                    modifier = Modifier.testTag("btn_confirm_add_manual_class")
                ) {
                    Text("Add Class", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = DarkSurfaceVariant,
        shape = RoundedCornerShape(18.dp)
    )
}
