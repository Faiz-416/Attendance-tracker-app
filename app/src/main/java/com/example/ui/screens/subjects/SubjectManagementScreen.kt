package com.example.ui.screens.subjects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectManagementScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit
) {
    val allSubjects by viewModel.allSubjects.collectAsStateWithLifecycle()
    val academicSettings by viewModel.academicSettings.collectAsStateWithLifecycle()

    var showAddEditDialog by remember { mutableStateOf<SubjectEntity?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Manage Subjects",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_subjects")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalBlack)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingNew = true },
                containerColor = EmeraldPrimary,
                contentColor = CharcoalBlack,
                modifier = Modifier.testTag("fab_add_subject")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Subject")
            }
        },
        containerColor = CharcoalBlack
    ) { padding ->
        if (allSubjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = TextDim,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Subjects Added",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    Text(
                        text = "Create your courses to start tracking attendance and predicting safe bunks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { isAddingNew = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = CharcoalBlack),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_add_first_subject")
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Subject", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(allSubjects, key = { it.id }) { subject ->
                    val subjColor = try {
                        Color(android.graphics.Color.parseColor(subject.colorHex))
                    } catch (e: Exception) {
                        EmeraldPrimary
                    }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddEditDialog = subject }
                            .testTag("subject_row_${subject.id}"),
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
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(subjColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = subject.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextWhite
                                        )
                                        if (subject.isArchived) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(DarkSurface)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Archived", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            }
                                        }
                                    }

                                    val details = listOfNotNull(
                                        subject.code.ifBlank { null },
                                        subject.teacher.ifBlank { null },
                                        subject.room.ifBlank { null }?.let { "Room $it" },
                                        "Goal: ${subject.targetThreshold.toInt()}%"
                                    )

                                    Text(
                                        text = details.joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showAddEditDialog = subject },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit Subject", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { subjectToDelete = subject },
                                    modifier = Modifier.size(36.dp).testTag("btn_delete_subject_${subject.id}")
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete Subject", tint = TextDim, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Subject Dialog
    val editing = showAddEditDialog
    if (isAddingNew || editing != null) {
        SubjectDialog(
            subject = editing,
            defaultThreshold = academicSettings.defaultThreshold,
            onDismiss = {
                isAddingNew = false
                showAddEditDialog = null
            },
            onSave = { toSave ->
                viewModel.saveSubject(toSave)
                isAddingNew = false
                showAddEditDialog = null
            }
        )
    }

    // Delete Confirmation Dialog
    subjectToDelete?.let { subj ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Delete Subject?", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete '${subj.name}'? All related timetable slots and historical attendance records for this subject will also be permanently deleted.",
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubject(subj)
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed, contentColor = Color.White),
                    modifier = Modifier.testTag("btn_confirm_delete_subject")
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }
}

@Composable
private fun SubjectDialog(
    subject: SubjectEntity?,
    defaultThreshold: Float,
    onDismiss: () -> Unit,
    onSave: (SubjectEntity) -> Unit
) {
    var name by remember(subject) { mutableStateOf(subject?.name ?: "") }
    var code by remember(subject) { mutableStateOf(subject?.code ?: "") }
    var teacher by remember(subject) { mutableStateOf(subject?.teacher ?: "") }
    var room by remember(subject) { mutableStateOf(subject?.room ?: "") }
    var colorHex by remember(subject) { mutableStateOf(subject?.colorHex ?: "#10B981") }
    var thresholdSlider by remember(subject) { mutableStateOf(subject?.targetThreshold ?: defaultThreshold) }

    val colorPalette = listOf(
        "#10B981", "#3B82F6", "#8B5CF6", "#EC4899",
        "#F59E0B", "#06B6D4", "#14B8A6", "#6366F1",
        "#84CC16", "#F97316"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (subject == null) "Add Subject" else "Edit Subject",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name *") },
                    placeholder = { Text("e.g., Data Structures") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_subject_name")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code (optional)") },
                        placeholder = { Text("e.g., CS301") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_subject_code")
                    )
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Room") },
                        placeholder = { Text("LH-101") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Teacher Name") },
                    placeholder = { Text("e.g., Prof. Smith") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Color Selector
                Text("Color Badge:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorPalette.forEach { hex ->
                        val isSelected = colorHex.equals(hex, ignoreCase = true)
                        val c = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }

                // Threshold Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Attendance Goal:", style = MaterialTheme.typography.labelMedium, color = TextWhite)
                    Text("${thresholdSlider.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrightMint)
                }

                Slider(
                    value = thresholdSlider,
                    onValueChange = { thresholdSlider = it },
                    valueRange = 50f..100f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary,
                        inactiveTrackColor = BorderDark
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val toSave = SubjectEntity(
                            id = subject?.id ?: 0,
                            name = name.trim(),
                            code = code.trim(),
                            teacher = teacher.trim(),
                            room = room.trim(),
                            colorHex = colorHex,
                            targetThreshold = thresholdSlider,
                            isArchived = subject?.isArchived ?: false
                        )
                        onSave(toSave)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = CharcoalBlack),
                modifier = Modifier.testTag("btn_save_subject")
            ) {
                Text("Save Subject", fontWeight = FontWeight.Bold)
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
