package com.example.ui.screens.home

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AttendanceGauge
import com.example.ui.components.GlassCard
import com.example.ui.components.StatMetricCard
import com.example.ui.components.SubjectAttendanceCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AttendanceViewModel,
    onNavigateToToday: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    val overallStats by viewModel.overallStats.collectAsStateWithLifecycle()
    val subjectsWithStats by viewModel.subjectsWithStats.collectAsStateWithLifecycle()
    val academicSettings by viewModel.academicSettings.collectAsStateWithLifecycle()
    val timetableSlots by viewModel.timetableSlots.collectAsStateWithLifecycle()
    val todayLectures by viewModel.todayLectures.collectAsStateWithLifecycle()

    val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = todayStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToTimetable,
                        modifier = Modifier.testTag("nav_timetable_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "View Timetable",
                            tint = TextMuted
                        )
                    }
                    IconButton(
                        onClick = onNavigateToImport,
                        modifier = Modifier.testTag("nav_chatgpt_import_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Import Timetable with ChatGPT",
                            tint = MintSecondary
                        )
                    }
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // 1. Academic Header Badge
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = listOfNotNull(
                                academicSettings.semesterName.ifBlank { "Semester 1" },
                                academicSettings.academicYear.ifBlank { "2026-2027" },
                                academicSettings.batch.ifBlank { null }?.let { "Batch $it" }
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.labelMedium,
                            color = BrightMint,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "Goal: ${academicSettings.defaultThreshold.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
            }

            // 2. Main Overall Attendance Hero Card
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("overall_attendance_hero_card"),
                    backgroundColor = DarkSurfaceVariant.copy(alpha = 0.7f),
                    borderColor = if (overallStats.isAboveThreshold || overallStats.totalConducted == 0) BorderDark else AbsentRed.copy(alpha = 0.4f),
                    cornerRadius = 20.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Overall Attendance",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "${overallStats.presentCount} of ${overallStats.totalConducted} classes attended",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }

                            if (overallStats.totalConducted > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (overallStats.isAboveThreshold) MintSubtle else AbsentRed.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (overallStats.isAboveThreshold) "Target Met" else "Below Target",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (overallStats.isAboveThreshold) BrightMint else AbsentRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Gauge
                        AttendanceGauge(
                            percentage = overallStats.percentage,
                            targetThreshold = overallStats.targetThreshold,
                            size = 150.dp,
                            strokeWidth = 14.dp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Prediction / Bunk Message
                        if (overallStats.totalConducted > 0) {
                            if (overallStats.isAboveThreshold) {
                                Text(
                                    text = if (overallStats.safeBunks > 0) "You can safely miss ${overallStats.safeBunks} class${if (overallStats.safeBunks > 1) "es" else ""} without falling below ${overallStats.targetThreshold.toInt()}%" else "You are currently right at your target boundary.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrightMint,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "Attend next ${overallStats.requiredConsecutiveClasses} consecutive class${if (overallStats.requiredConsecutiveClasses > 1) "es" else ""} to reach ${overallStats.targetThreshold.toInt()}% target.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AbsentRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                text = "Mark your daily classes to see real-time statistics and bunk predictions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // 3. Quick Metrics Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Conducted",
                        value = "${overallStats.totalConducted}",
                        icon = Icons.Filled.School,
                        accentColor = MintSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Attended",
                        value = "${overallStats.presentCount}",
                        icon = Icons.Filled.CheckCircle,
                        accentColor = PresentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Missed",
                        value = "${overallStats.absentCount}",
                        icon = Icons.Filled.Cancel,
                        accentColor = AbsentRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Today's Quick Action Card
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToToday() }
                        .testTag("today_quick_action_card"),
                    backgroundColor = SubtleGreenSurface,
                    borderColor = EmeraldPrimary.copy(alpha = 0.3f),
                    cornerRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EventAvailable,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Today's Schedule",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite
                                )
                                Text(
                                    text = if (todayLectures.isEmpty()) "No classes scheduled today" else "${todayLectures.count { it.record != null }}/${todayLectures.size} marked (${todayLectures.count { it.record == null }} pending)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Button(
                            onClick = onNavigateToToday,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = CharcoalBlack
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_mark_today_cta")
                        ) {
                            Text("Mark", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 5. ChatGPT Timetable Promo Card (if timetable empty or as quick access)
            if (timetableSlots.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToImport() }
                            .testTag("promo_chatgpt_import_card"),
                        backgroundColor = DarkSurfaceVariant,
                        borderColor = MintSecondary.copy(alpha = 0.4f),
                        cornerRadius = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MintSecondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MintSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Import Timetable with ChatGPT",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrightMint
                                )
                                Text(
                                    text = "Copy prompt, upload your timetable photo to ChatGPT, and import the CSV instantly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = BrightMint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 6. Warnings Section (if any subject below threshold)
            if (overallStats.subjectsBelowThresholdCount > 0) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = AbsentRed.copy(alpha = 0.1f),
                        borderColor = AbsentRed.copy(alpha = 0.4f),
                        cornerRadius = 14.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Warning",
                                tint = AbsentRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${overallStats.subjectsBelowThresholdCount} subject${if (overallStats.subjectsBelowThresholdCount > 1) "s are" else " is"} below the ${academicSettings.defaultThreshold.toInt()}% attendance threshold.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AbsentRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 7. Subject List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subject Attendance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    TextButton(
                        onClick = onNavigateToSubjects,
                        modifier = Modifier.testTag("btn_manage_subjects")
                    ) {
                        Text("Manage", color = MintSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // 8. Subject Cards or Empty State
            if (subjectsWithStats.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = DarkSurfaceVariant.copy(alpha = 0.4f),
                        borderColor = BorderDark,
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MenuBook,
                                contentDescription = null,
                                tint = TextDim,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No subjects added yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite
                            )
                            Text(
                                text = "Add your college subjects manually or import your schedule using ChatGPT.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onNavigateToSubjects,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                                    border = BorderStroke(1.dp, BorderDark),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Add Subject")
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
                }
            } else {
                items(subjectsWithStats, key = { it.subject.id }) { item ->
                    SubjectAttendanceCard(
                        item = item,
                        onClick = onNavigateToAnalytics
                    )
                }
            }
        }
    }
}
