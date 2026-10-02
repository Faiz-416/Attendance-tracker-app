package com.example.ui.screens.settings

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.local.entity.AcademicSettingEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AttendanceViewModel,
    onNavigateToSubjects: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToImport: () -> Unit
) {
    val academicSettings by viewModel.academicSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showEditAcademicDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Export CSV Launcher
    var csvContentToExport by remember { mutableStateOf<String?>(null) }
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null && csvContentToExport != null) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)
                    writer.write(csvContentToExport)
                    writer.close()
                    outputStream.close()
                    Toast.makeText(context, "Attendance history exported to CSV!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Export JSON Launcher
    var jsonContentToExport by remember { mutableStateOf<String?>(null) }
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && jsonContentToExport != null) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)
                    writer.write(jsonContentToExport)
                    writer.close()
                    outputStream.close()
                    Toast.makeText(context, "Complete backup saved to JSON!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Restore JSON Launcher
    val restoreJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                        val jsonString = reader.readText()
                        reader.close()
                        inputStream.close()
                        val success = viewModel.restoreFromJson(jsonString)
                        if (success) {
                            Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invalid backup file format", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalBlack)
            )
        },
        containerColor = CharcoalBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
        ) {
            // 1. Goal Attendance Threshold Slider Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().testTag("settings_threshold_card"),
                    backgroundColor = DarkSurfaceVariant.copy(alpha = 0.6f),
                    borderColor = BorderDark,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Target Attendance Goal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Default minimum required for your college",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = "${academicSettings.defaultThreshold.toInt()}%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrightMint
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = academicSettings.defaultThreshold,
                            onValueChange = { newThreshold ->
                                viewModel.updateSettings(academicSettings.copy(defaultThreshold = newThreshold))
                            },
                            valueRange = 50f..100f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = EmeraldPrimary,
                                activeTrackColor = EmeraldPrimary,
                                inactiveTrackColor = BorderDark
                            ),
                            modifier = Modifier.testTag("slider_default_threshold")
                        )
                    }
                }
            }

            // 2. Academic Calendar Info Card
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditAcademicDialog = true }
                        .testTag("settings_academic_info_card"),
                    backgroundColor = DarkSurfaceVariant.copy(alpha = 0.6f),
                    borderColor = BorderDark,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Academic Calendar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MintSecondary, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val details = listOf(
                            "Institution" to academicSettings.institution.ifBlank { "Not set" },
                            "Branch / Dept" to listOf(academicSettings.branch, academicSettings.department).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Not set" },
                            "Academic Year" to academicSettings.academicYear.ifBlank { "2026-2027" },
                            "Current Semester" to academicSettings.semesterName.ifBlank { "Semester 1" },
                            "Semester Duration" to "${academicSettings.semesterStartDate} to ${academicSettings.semesterEndDate}"
                        )

                        details.forEach { (label, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = TextWhite)
                            }
                        }
                    }
                }
            }

            // 3. Quick Navigation Items
            item {
                Text(
                    text = "Management & Timetable",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                    borderColor = BorderDark,
                    cornerRadius = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingsRow(
                            icon = Icons.Filled.AutoAwesome,
                            title = "Import Timetable with ChatGPT",
                            subtitle = "Extract schedules from photos/PDFs with AI prompt",
                            accentColor = MintSecondary,
                            onClick = onNavigateToImport,
                            testTag = "settings_row_chatgpt_import"
                        )
                        Divider(color = BorderDark, modifier = Modifier.padding(vertical = 4.dp))
                        SettingsRow(
                            icon = Icons.Filled.CalendarMonth,
                            title = "Weekly Timetable",
                            subtitle = "View, add, and adjust recurring lecture periods",
                            accentColor = TextWhite,
                            onClick = onNavigateToTimetable,
                            testTag = "settings_row_timetable"
                        )
                        Divider(color = BorderDark, modifier = Modifier.padding(vertical = 4.dp))
                        SettingsRow(
                            icon = Icons.Filled.MenuBook,
                            title = "Subject Management",
                            subtitle = "Add, edit, archive, and customize course goals",
                            accentColor = TextWhite,
                            onClick = onNavigateToSubjects,
                            testTag = "settings_row_subjects"
                        )
                    }
                }
            }

            // 4. Backup & Export Section
            item {
                Text(
                    text = "Backup, Export & Restore",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                    borderColor = BorderDark,
                    cornerRadius = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingsRow(
                            icon = Icons.Filled.TableChart,
                            title = "Export Attendance History (CSV)",
                            subtitle = "Download all recorded attendance sessions into standard CSV",
                            accentColor = BrightMint,
                            onClick = {
                                coroutineScope.launch {
                                    val csv = viewModel.exportAttendanceCsv()
                                    csvContentToExport = csv
                                    exportCsvLauncher.launch("attendance_history_${LocalDate.now()}.csv")
                                }
                            },
                            testTag = "settings_row_export_csv"
                        )
                        Divider(color = BorderDark, modifier = Modifier.padding(vertical = 4.dp))
                        SettingsRow(
                            icon = Icons.Filled.CloudDownload,
                            title = "Full Data Backup (JSON)",
                            subtitle = "Export all settings, timetable, subjects, and records",
                            accentColor = MintSecondary,
                            onClick = {
                                coroutineScope.launch {
                                    val json = viewModel.getFullBackupJson()
                                    jsonContentToExport = json
                                    exportJsonLauncher.launch("attendify_backup_${LocalDate.now()}.json")
                                }
                            },
                            testTag = "settings_row_backup_json"
                        )
                        Divider(color = BorderDark, modifier = Modifier.padding(vertical = 4.dp))
                        SettingsRow(
                            icon = Icons.Filled.Restore,
                            title = "Restore from JSON Backup",
                            subtitle = "Restore database from a previously exported JSON backup",
                            accentColor = WarningAmber,
                            onClick = {
                                restoreJsonLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            testTag = "settings_row_restore_json"
                        )
                    }
                }
            }

            // 5. Danger Zone / Reset
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().testTag("settings_reset_card"),
                    backgroundColor = AbsentRed.copy(alpha = 0.08f),
                    borderColor = AbsentRed.copy(alpha = 0.3f),
                    cornerRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reset Application Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AbsentRed
                            )
                            Text(
                                text = "Permanently wipe all attendance records, timetable slots, and subjects.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }

                        Button(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AbsentRed, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_reset_all_data")
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }

            // 6. About Card
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Attendance Tracker v1.0",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = "100% Offline • Private & Secure Local Database",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim
                    )
                }
            }
        }
    }

    // Edit Academic Calendar Dialog
    if (showEditAcademicDialog) {
        EditAcademicDialog(
            settings = academicSettings,
            context = context,
            onDismiss = { showEditAcademicDialog = false },
            onSave = { updated ->
                viewModel.updateSettings(updated)
                showEditAcademicDialog = false
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset Everything?", color = AbsentRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to reset all data? This will permanently erase your subjects, timetable slots, and attendance history.",
                    color = TextWhite
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "All data has been reset", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentRed, contentColor = Color.White),
                    modifier = Modifier.testTag("btn_confirm_reset_all")
                ) {
                    Text("Erase All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextWhite)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextDim, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EditAcademicDialog(
    settings: AcademicSettingEntity,
    context: Context,
    onDismiss: () -> Unit,
    onSave: (AcademicSettingEntity) -> Unit
) {
    var institution by remember { mutableStateOf(settings.institution) }
    var department by remember { mutableStateOf(settings.department) }
    var branch by remember { mutableStateOf(settings.branch) }
    var academicYear by remember { mutableStateOf(settings.academicYear) }
    var semesterName by remember { mutableStateOf(settings.semesterName) }
    var semesterStartDate by remember { mutableStateOf(settings.semesterStartDate) }
    var semesterEndDate by remember { mutableStateOf(settings.semesterEndDate) }
    var batch by remember { mutableStateOf(settings.batch) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Academic Calendar Details", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = institution,
                        onValueChange = { institution = it },
                        label = { Text("College / University Name") },
                        modifier = Modifier.fillMaxWidth().testTag("input_settings_institution")
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text("Department") },
                            modifier = Modifier.weight(1f).testTag("input_settings_department")
                        )
                        OutlinedTextField(
                            value = branch,
                            onValueChange = { branch = it },
                            label = { Text("Branch (e.g. CSE)") },
                            modifier = Modifier.weight(1f).testTag("input_settings_branch")
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = academicYear,
                            onValueChange = { academicYear = it },
                            label = { Text("Academic Year") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = semesterName,
                            onValueChange = { semesterName = it },
                            label = { Text("Semester") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = semesterStartDate,
                            onValueChange = { semesterStartDate = it },
                            label = { Text("Start (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = semesterEndDate,
                            onValueChange = { semesterEndDate = it },
                            label = { Text("End (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = batch,
                        onValueChange = { batch = it },
                        label = { Text("Batch / Division (e.g., A1)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        settings.copy(
                            institution = institution,
                            department = department,
                            branch = branch,
                            academicYear = academicYear,
                            semesterName = semesterName,
                            semesterStartDate = semesterStartDate,
                            semesterEndDate = semesterEndDate,
                            batch = batch
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = CharcoalBlack),
                modifier = Modifier.testTag("btn_save_academic_settings")
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
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
