package com.example.ui.screens.importflow

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.ImportStep
import com.example.util.CsvParser
import com.example.util.PromptGenerator
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatGPTImportScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onImportSuccess: () -> Unit
) {
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val academicSettings by viewModel.academicSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showSampleCsvDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showConfirmCommitDialog by remember { mutableStateOf(false) }
    var selectedConflictMode by remember { mutableStateOf(ImportConflictMode.REPLACE_EXISTING_TIMETABLE) }

    // File picker SAF launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                    val content = reader.readText()
                    reader.close()
                    inputStream.close()
                    viewModel.processImportedCsvContent(content)
                } else {
                    Toast.makeText(context, "Could not open selected file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (importState.step) {
                            ImportStep.INSTRUCTIONS -> "Import Timetable (ChatGPT)"
                            ImportStep.BATCH_SELECTION -> "Select Your Batch"
                            ImportStep.PREVIEW_AND_REVIEW -> "Review Timetable"
                            ImportStep.COMPLETED -> "Import Completed"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (importState.step == ImportStep.INSTRUCTIONS) {
                                onNavigateBack()
                            } else {
                                viewModel.setImportStep(ImportStep.INSTRUCTIONS)
                            }
                        },
                        modifier = Modifier.testTag("btn_back_import")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = "Help & FAQ",
                            tint = TextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalBlack)
            )
        },
        containerColor = CharcoalBlack
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (importState.step) {
                ImportStep.INSTRUCTIONS -> {
                    InstructionsView(
                        academicSettings = academicSettings,
                        context = context,
                        onOpenSampleCsv = { showSampleCsvDialog = true },
                        onPickFile = {
                            filePickerLauncher.launch(arrayOf("text/*", "application/csv", "*/*"))
                        }
                    )
                }
                ImportStep.BATCH_SELECTION -> {
                    BatchSelectionView(
                        groups = importState.detectedGroups,
                        onSelectGroup = { group ->
                            viewModel.selectTimetableGroup(group)
                        }
                    )
                }
                ImportStep.PREVIEW_AND_REVIEW -> {
                    PreviewAndReviewView(
                        rows = importState.activeGroupRows,
                        validationSummary = importState.validationSummary,
                        selectedGroup = importState.selectedGroup,
                        onUpdateRow = { updated -> viewModel.updatePreviewRow(updated) },
                        onDeleteRow = { rowId -> viewModel.deletePreviewRow(rowId) },
                        onProceedToCommit = { showConfirmCommitDialog = true }
                    )
                }
                ImportStep.COMPLETED -> {
                    ImportCompletedView(
                        onFinish = onImportSuccess
                    )
                }
            }

            // Error Overlay / Loading Indicator
            if (importState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                }
            }

            if (importState.errorMessage != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.setImportStep(ImportStep.INSTRUCTIONS) },
                    title = { Text("Import Issue", color = AbsentRed, fontWeight = FontWeight.Bold) },
                    text = { Text(importState.errorMessage ?: "", color = TextWhite) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.setImportStep(ImportStep.INSTRUCTIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = CharcoalBlack)
                        ) {
                            Text("Try Again")
                        }
                    },
                    containerColor = DarkSurfaceVariant
                )
            }
        }
    }

    // Sample CSV Dialog
    if (showSampleCsvDialog) {
        AlertDialog(
            onDismissRequest = { showSampleCsvDialog = false },
            title = { Text("Standard CSV Format", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "The expected CSV schema and columns:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = CsvParser.getSampleCsv(),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                            color = BrightMint
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSampleCsvDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = CharcoalBlack)
                ) {
                    Text("Got It")
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Help & FAQ Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("ChatGPT Timetable Import Help", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("• No API key needed: You use your existing free or plus ChatGPT account.", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    Text("• How it works: Copy our specialized prompt, upload a clear photo or screenshot of your college timetable schedule into ChatGPT, and ChatGPT creates a .csv file.", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    Text("• Batches: If your timetable has multiple batches (e.g., A1, A2, B1), ChatGPT extracts all of them into the CSV, and this app lets you select yours.", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    Text("• Safe & Offline: All parsing and schedule generation happens locally inside the app.", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = CharcoalBlack)
                ) {
                    Text("Close")
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    // Confirmation Commit Dialog
    if (showConfirmCommitDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCommitDialog = false },
            title = { Text("Confirm Timetable Import", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "You are about to import ${importState.activeGroupRows.size} timetable slots.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextWhite
                    )

                    Text(
                        text = "How would you like to handle your existing weekly timetable?",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    // Option 1: Replace
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedConflictMode = ImportConflictMode.REPLACE_EXISTING_TIMETABLE }
                            .background(if (selectedConflictMode == ImportConflictMode.REPLACE_EXISTING_TIMETABLE) MintSubtle else DarkSurface)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedConflictMode == ImportConflictMode.REPLACE_EXISTING_TIMETABLE,
                            onClick = { selectedConflictMode = ImportConflictMode.REPLACE_EXISTING_TIMETABLE },
                            colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Replace Current Timetable", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextWhite)
                            Text("Overwrites current weekly slots (past attendance records remain safe)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }

                    // Option 2: Merge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedConflictMode = ImportConflictMode.MERGE_WITH_EXISTING }
                            .background(if (selectedConflictMode == ImportConflictMode.MERGE_WITH_EXISTING) MintSubtle else DarkSurface)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedConflictMode == ImportConflictMode.MERGE_WITH_EXISTING,
                            onClick = { selectedConflictMode = ImportConflictMode.MERGE_WITH_EXISTING },
                            colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Merge with Current Timetable", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextWhite)
                            Text("Adds new slots alongside existing ones", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmCommitDialog = false
                        viewModel.confirmAndCommitImport(selectedConflictMode) {
                            Toast.makeText(context, "Timetable successfully imported!", Toast.LENGTH_SHORT).show()
                            onImportSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = CharcoalBlack),
                    modifier = Modifier.testTag("btn_confirm_commit_import")
                ) {
                    Text("Confirm Import", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCommitDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }
}

// -------------------------------------------------------------
// STEP 1: INSTRUCTIONS VIEW
// -------------------------------------------------------------

@Composable
private fun InstructionsView(
    academicSettings: com.example.data.local.entity.AcademicSettingEntity,
    context: Context,
    onOpenSampleCsv: () -> Unit,
    onPickFile: () -> Unit
) {
    val prompt = remember(academicSettings) {
        PromptGenerator.generateChatGptPrompt(
            institution = academicSettings.institution,
            department = academicSettings.department,
            semester = if (academicSettings.semesterNumber > 0) "${academicSettings.semesterNumber}" else "1",
            batch = academicSettings.batch
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // Hero explanation card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurfaceVariant.copy(alpha = 0.7f),
                borderColor = MintSecondary.copy(alpha = 0.3f),
                cornerRadius = 18.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MintSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = BrightMint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AI Timetable Extraction",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Extract your college schedule from a photo or PDF using ChatGPT in 3 simple steps without sharing API keys.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        }

        // Steps List
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                borderColor = BorderDark,
                cornerRadius = 18.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Workflow Steps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    StepRow(number = 1, title = "Copy the generated extraction prompt below.")
                    StepRow(number = 2, title = "Open ChatGPT (App or Web) and paste the prompt.")
                    StepRow(number = 3, title = "Upload your timetable photo, screenshot, or PDF.")
                    StepRow(number = 4, title = "ChatGPT extracts all batches and provides a downloadable CSV.")
                    StepRow(number = 5, title = "Download the CSV file and tap 'Import CSV File' below.")
                    StepRow(number = 6, title = "Select your batch, review the slots, and confirm!")
                }
            }
        }

        // Action Buttons: Copy Prompt & Open ChatGPT
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ChatGPT Timetable Prompt", prompt)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Prompt copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_copy_prompt"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = BrightMint
                    ),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp), tint = BrightMint)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Prompt", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com"))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_open_chatgpt"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = TextWhite
                    ),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(18.dp), tint = TextWhite)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open ChatGPT", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Big Primary Import Button
        item {
            Button(
                onClick = onPickFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_import_csv_file"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = CharcoalBlack
                )
            ) {
                Icon(Icons.Filled.UploadFile, null, modifier = Modifier.size(22.dp), tint = CharcoalBlack)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select & Import CSV File", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        // Prompt Preview Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurface,
                borderColor = BorderDark,
                cornerRadius = 14.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Prompt Preview",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        TextButton(
                            onClick = onOpenSampleCsv,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("Sample CSV", color = MintSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                        color = TextMuted,
                        maxLines = 8
                    )
                }
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = BrightMint
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextWhite
        )
    }
}

// -------------------------------------------------------------
// STEP 2: BATCH SELECTION VIEW
// -------------------------------------------------------------

@Composable
private fun BatchSelectionView(
    groups: List<TimetableGroup>,
    onSelectGroup: (TimetableGroup) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Detected ${groups.size} Group${if (groups.size > 1) "s" else ""}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = "ChatGPT identified multiple division/batch schedules. Choose which batch you belong to:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(groups, key = { it.id }) { group ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectGroup(group) }
                    .testTag("batch_group_card_${group.batch.ifBlank { "default" }}"),
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
                            text = group.displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrightMint
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = BrightMint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (group.subtitle.isNotBlank()) {
                        Text(
                            text = group.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurface)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${group.totalEntries} slots",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextWhite
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurface)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${group.uniqueSubjectsCount} subjects",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextWhite
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurface)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${group.weekdaysCount} days",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextWhite
                            )
                        }
                    }

                    if (group.sampleSubjects.isNotEmpty()) {
                        Text(
                            text = "Subjects: " + group.sampleSubjects.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 3: PREVIEW AND REVIEW VIEW
// -------------------------------------------------------------

@Composable
private fun PreviewAndReviewView(
    rows: List<ParsedTimetableRow>,
    validationSummary: ImportValidationSummary,
    selectedGroup: TimetableGroup?,
    onUpdateRow: (ParsedTimetableRow) -> Unit,
    onDeleteRow: (Long) -> Unit,
    onProceedToCommit: () -> Unit
) {
    var editingRow by remember { mutableStateOf<ParsedTimetableRow?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // Summary Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth().testTag("import_validation_summary_card"),
                backgroundColor = DarkSurfaceVariant.copy(alpha = 0.7f),
                borderColor = if (validationSummary.rowsWithWarnings > 0) WarningAmber.copy(alpha = 0.4f) else EmeraldPrimary.copy(alpha = 0.4f),
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
                                text = selectedGroup?.displayTitle ?: "Timetable Preview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "${validationSummary.weekdaysCount} weekdays • ${validationSummary.totalRows} slots • ${validationSummary.uniqueSubjectsCount} subjects",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrightMint
                            )
                        }

                        if (validationSummary.rowsWithWarnings > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(WarningAmber.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${validationSummary.rowsWithWarnings} to review",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarningAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (validationSummary.issuesList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Warnings: " + validationSummary.issuesList.joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningAmber
                        )
                    }
                }
            }
        }

        // Primary Proceed Button
        item {
            Button(
                onClick = onProceedToCommit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_proceed_to_confirm_import"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = CharcoalBlack
                )
            ) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(20.dp), tint = CharcoalBlack)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm & Create Timetable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        // Rows Grouped by Weekday
        val rowsByDay = rows.groupBy { it.dayOfWeek }
        (1..7).forEach { dayOfWeek ->
            val dayRows = rowsByDay[dayOfWeek] ?: emptyList()
            if (dayRows.isNotEmpty()) {
                item {
                    Text(
                        text = CsvParser.dayOfWeekToString(dayOfWeek),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrightMint,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(dayRows, key = { it.rowId }) { row ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().testTag("preview_row_${row.rowId}"),
                        backgroundColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                        borderColor = if (row.isValid) BorderDark else WarningAmber.copy(alpha = 0.5f),
                        cornerRadius = 14.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${row.startTime} – ${row.endTime}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextWhite
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• Period ${row.periodNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }

                                Text(
                                    text = row.subjectName.ifBlank { row.subjectCode.ifBlank { "Untitled Subject" } },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextWhite
                                )

                                val details = listOfNotNull(
                                    row.subjectCode.ifBlank { null },
                                    row.classType.ifBlank { null },
                                    row.room.ifBlank { null }?.let { "Room $it" },
                                    row.teacher.ifBlank { null },
                                    row.batch.ifBlank { null }?.let { "Batch $it" }
                                )

                                if (details.isNotEmpty()) {
                                    Text(
                                        text = details.joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }

                                if (row.issues.isNotEmpty()) {
                                    Text(
                                        text = "Issue: " + row.issues.joinToString(", "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarningAmber,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { editingRow = row },
                                    modifier = Modifier.size(36.dp).testTag("btn_edit_preview_row_${row.rowId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit Row",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteRow(row.rowId) },
                                    modifier = Modifier.size(36.dp).testTag("btn_delete_preview_row_${row.rowId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete Row",
                                        tint = TextDim,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Row Dialog
    editingRow?.let { row ->
        EditRowDialog(
            row = row,
            onDismiss = { editingRow = null },
            onSave = { updated ->
                onUpdateRow(updated)
                editingRow = null
            }
        )
    }
}

@Composable
private fun EditRowDialog(
    row: ParsedTimetableRow,
    onDismiss: () -> Unit,
    onSave: (ParsedTimetableRow) -> Unit
) {
    var subjectName by remember { mutableStateOf(row.subjectName) }
    var subjectCode by remember { mutableStateOf(row.subjectCode) }
    var startTime by remember { mutableStateOf(row.startTime) }
    var endTime by remember { mutableStateOf(row.endTime) }
    var room by remember { mutableStateOf(row.room) }
    var teacher by remember { mutableStateOf(row.teacher) }
    var classType by remember { mutableStateOf(row.classType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Timetable Slot", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_subject_name")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subjectCode,
                        onValueChange = { subjectCode = it },
                        label = { Text("Code") },
                        modifier = Modifier.weight(1f).testTag("input_edit_subject_code")
                    )
                    OutlinedTextField(
                        value = classType,
                        onValueChange = { classType = it },
                        label = { Text("Type") },
                        modifier = Modifier.weight(1f).testTag("input_edit_class_type")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start") },
                        modifier = Modifier.weight(1f).testTag("input_edit_start_time")
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End") },
                        modifier = Modifier.weight(1f).testTag("input_edit_end_time")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Room") },
                        modifier = Modifier.weight(1f).testTag("input_edit_room")
                    )
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Teacher") },
                        modifier = Modifier.weight(1f).testTag("input_edit_teacher")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        row.copy(
                            subjectName = subjectName,
                            subjectCode = subjectCode,
                            startTime = startTime,
                            endTime = endTime,
                            room = room,
                            teacher = teacher,
                            classType = classType,
                            issues = emptyList() // User manually validated
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = CharcoalBlack),
                modifier = Modifier.testTag("btn_save_row_edit")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = DarkSurfaceVariant
    )
}

// -------------------------------------------------------------
// STEP 4: COMPLETED VIEW
// -------------------------------------------------------------

@Composable
private fun ImportCompletedView(
    onFinish: () -> Unit
) {
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
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = BrightMint,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Timetable Imported Successfully!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = "Your weekly schedule has been saved. Daily classes will now automatically appear in the Today tab.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = CharcoalBlack
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp).testTag("btn_finish_import")
            ) {
                Text("Go to Timetable", fontWeight = FontWeight.Bold)
            }
        }
    }
}
