package com.example.ui.screens.timetable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.util.CsvParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToSubjects: () -> Unit
) {
    val allSlots by viewModel.timetableSlots.collectAsStateWithLifecycle()
    val allSubjects by viewModel.activeSubjects.collectAsStateWithLifecycle()

    val subjectMap = remember(allSubjects) { allSubjects.associateBy { it.id } }

    var selectedDayOfWeek by remember { mutableStateOf(1) } // 1 (Mon) to 7 (Sun)
    var showAddEditDialog by remember { mutableStateOf<TimetableSlotEntity?>(null) }
    var isAddingNewSlot by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val daySlots = remember(allSlots, selectedDayOfWeek) {
        allSlots.filter { it.dayOfWeek == selectedDayOfWeek }.sortedBy { it.startTime }
    }

    val daysOfWeekLabels = listOf(
        1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Weekly Timetable",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_timetable")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToImport,
                        modifier = Modifier.testTag("btn_import_shortcut_timetable")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Import with ChatGPT", tint = MintSecondary)
                    }
                    if (allSlots.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllConfirm = true },
                            modifier = Modifier.testTag("btn_clear_all_slots")
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear Timetable", tint = TextDim)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalBlack)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingNewSlot = true },
                containerColor = EmeraldPrimary,
                contentColor = CharcoalBlack,
                modifier = Modifier.testTag("fab_add_timetable_slot")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Lecture Slot")
            }
        },
        containerColor = CharcoalBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Day of Week Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeekLabels.forEach { (dayInt, label) ->
                    val isSelected = selectedDayOfWeek == dayInt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) EmeraldPrimary else DarkSurfaceVariant)
                            .clickable { selectedDayOfWeek = dayInt }
                            .padding(vertical = 10.dp)
                            .testTag("tab_day_$dayInt"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CharcoalBlack else TextWhite
                        )
                    }
                }
            }

            // 2. Day Slot Count Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = CsvParser.dayOfWeekToString(selectedDayOfWeek),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrightMint
                )
                Text(
                    text = "${daySlots.size} lecture${if (daySlots.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            // 3. Slots List or Empty State
            if (daySlots.isEmpty()) {
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
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Classes on ${CsvParser.dayOfWeekToString(selectedDayOfWeek)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite
                        )
                        Text(
                            text = "Add individual slots manually or import your entire college timetable using ChatGPT.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { isAddingNewSlot = true },
                                border = BorderStroke(1.dp, BorderDark),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Add Slot")
                            }
                            Button(
                                onClick = onNavigateToImport,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = CharcoalBlack),
                                shape = RoundedCornerShape(8.dp)
                            ) {
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(daySlots, key = { it.id }) { slot ->
                        val subj = subjectMap[slot.subjectId]

                        val subjColor = try {
                            Color(android.graphics.Color.parseColor(subj?.colorHex ?: "#10B981"))
                        } catch (e: Exception) {
                            EmeraldPrimary
                        }

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddEditDialog = slot }
                                .testTag("timetable_slot_${slot.id}"),
                            backgroundColor = DarkSurfaceVariant.copy(alpha = 0.6f),
                            borderColor = BorderDark,
                            cornerRadius = 14.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(subjColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${slot.startTime} – ${slot.endTime}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextWhite
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• Period ${slot.periodNumber}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted
                                            )
                                        }

                                        Text(
                                            text = subj?.name ?: "Subject #${slot.subjectId}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = TextWhite
                                        )

                                        val details = listOfNotNull(
                                            subj?.code?.ifBlank { null },
                                            slot.classType.ifBlank { null },
                                            slot.room.ifBlank { null }?.let { "Room $it" },
                                            slot.teacher.ifBlank { null },
                                            slot.batch.ifBlank { null }?.let { "Batch $it" }
                                        )

                                        if (details.isNotEmpty()) {
                                            Text(
                                                text = details.joinToString(" • "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { showAddEditDialog = slot },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit Slot", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteTimetableSlot(slot.id) },
                                        modifier = Modifier.size(36.dp).testTag("btn_delete_slot_${slot.id}")
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete Slot", tint = TextDim, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Slot Dialog
    val editingSlot = showAddEditDialog
    if (isAddingNewSlot || editingSlot != null) {
        SlotEditDialog(
            slot = editingSlot,
            currentDay = selectedDayOfWeek,
            subjects = allSubjects,
            onDismiss = {
                isAddingNewSlot = false
                showAddEditDialog = null
            },
            onNavigateToSubjects = {
                isAddingNewSlot = false
                showAddEditDialog = null
                onNavigateToSubjects()
            },
            onSave = { slotToSave ->
                viewModel.saveTimetableSlot(slotToSave)
                isAddingNewSlot = false
                showAddEditDialog = null
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Clear Weekly Timetable?", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This removes all recurring weekly schedule slots. Your recorded attendance history will remain completely untouched.",
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllTimetableSlots()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed, contentColor = Color.White)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotEditDialog(
    slot: TimetableSlotEntity?,
    currentDay: Int,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    onSave: (TimetableSlotEntity) -> Unit
) {
    var selectedSubjectId by remember(slot) { mutableStateOf(slot?.subjectId ?: (subjects.firstOrNull()?.id ?: 0L)) }
    var periodNumber by remember(slot) { mutableStateOf("${slot?.periodNumber ?: 1}") }
    var startTime by remember(slot) { mutableStateOf(slot?.startTime ?: "09:00") }
    var endTime by remember(slot) { mutableStateOf(slot?.endTime ?: "10:00") }
    var classType by remember(slot) { mutableStateOf(slot?.classType ?: "Lecture") }
    var room by remember(slot) { mutableStateOf(slot?.room ?: "") }
    var teacher by remember(slot) { mutableStateOf(slot?.teacher ?: "") }
    var batch by remember(slot) { mutableStateOf(slot?.batch ?: "") }
    var expandedSubjDropdown by remember { mutableStateOf(false) }

    val currentSubj = subjects.find { it.id == selectedSubjectId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (slot == null) "Add Lecture Slot" else "Edit Lecture Slot",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        },
        text = {
            if (subjects.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Please add at least one subject first.", color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onNavigateToSubjects,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = CharcoalBlack)
                    ) {
                        Text("Add Subject", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Subject Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedSubjDropdown,
                        onExpandedChange = { expandedSubjDropdown = !expandedSubjDropdown }
                    ) {
                        OutlinedTextField(
                            value = currentSubj?.name ?: "Select Subject",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subject") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubjDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = periodNumber,
                            onValueChange = { periodNumber = it },
                            label = { Text("Period #") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = classType,
                            onValueChange = { classType = it },
                            label = { Text("Class Type") },
                            modifier = Modifier.weight(1.5f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Start Time") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End Time") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = room,
                            onValueChange = { room = it },
                            label = { Text("Room") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = batch,
                            onValueChange = { batch = it },
                            label = { Text("Batch") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Teacher Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (subjects.isNotEmpty()) {
                Button(
                    onClick = {
                        val p = periodNumber.toIntOrNull() ?: 1
                        val slotToSave = TimetableSlotEntity(
                            id = slot?.id ?: 0,
                            subjectId = selectedSubjectId,
                            dayOfWeek = currentDay,
                            periodNumber = p,
                            startTime = startTime,
                            endTime = endTime,
                            classType = classType,
                            room = room,
                            teacher = teacher,
                            batch = batch
                        )
                        onSave(slotToSave)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = CharcoalBlack),
                    modifier = Modifier.testTag("btn_save_timetable_slot")
                ) {
                    Text("Save Slot", fontWeight = FontWeight.Bold)
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
