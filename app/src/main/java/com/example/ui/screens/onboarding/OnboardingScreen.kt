package com.example.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel

@Composable
fun OnboardingScreen(
    viewModel: AttendanceViewModel,
    onFinishOnboarding: () -> Unit,
    onImportWithChatGPT: () -> Unit
) {
    val academicSettings by viewModel.academicSettings.collectAsStateWithLifecycle()

    var threshold by remember(academicSettings) { mutableStateOf(academicSettings.defaultThreshold) }
    var semesterName by remember(academicSettings) { mutableStateOf(academicSettings.semesterName.ifBlank { "Semester 1" }) }
    var academicYear by remember(academicSettings) { mutableStateOf(academicSettings.academicYear.ifBlank { "2026-2027" }) }

    Scaffold(
        containerColor = CharcoalBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp)
        ) {
            // Icon & Welcome Header
            item {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = BrightMint,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Welcome to Attendance Tracker",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Track lectures, calculate safe bunks, and import college timetables using ChatGPT.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Quick Setup Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_setup_card"),
                    backgroundColor = DarkSurfaceVariant.copy(alpha = 0.7f),
                    borderColor = MintSecondary.copy(alpha = 0.3f),
                    cornerRadius = 18.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Initial Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrightMint
                        )

                        // Threshold Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Target Attendance Goal:", style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                            Text("${threshold.toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BrightMint)
                        }

                        Slider(
                            value = threshold,
                            onValueChange = { threshold = it },
                            valueRange = 50f..100f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = EmeraldPrimary,
                                activeTrackColor = EmeraldPrimary,
                                inactiveTrackColor = BorderDark
                            ),
                            modifier = Modifier.testTag("slider_onboarding_threshold")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = semesterName,
                                onValueChange = { semesterName = it },
                                label = { Text("Semester") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = academicYear,
                                onValueChange = { academicYear = it },
                                label = { Text("Academic Year") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Options: Import with ChatGPT OR Manual Setup
            item {
                Text(
                    text = "Choose how to start:",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp)
                )
            }

            // Option 1: ChatGPT Import (Recommended)
            item {
                Button(
                    onClick = {
                        viewModel.updateSettings(
                            academicSettings.copy(
                                defaultThreshold = threshold,
                                semesterName = semesterName,
                                academicYear = academicYear,
                                isSetupCompleted = true
                            )
                        )
                        onImportWithChatGPT()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_onboarding_import_chatgpt"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = CharcoalBlack
                    )
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(20.dp), tint = CharcoalBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Timetable with ChatGPT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            // Option 2: Manual Start
            item {
                OutlinedButton(
                    onClick = {
                        viewModel.updateSettings(
                            academicSettings.copy(
                                defaultThreshold = threshold,
                                semesterName = semesterName,
                                academicYear = academicYear,
                                isSetupCompleted = true
                            )
                        )
                        onFinishOnboarding()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_onboarding_manual_start"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite)
                ) {
                    Icon(Icons.Filled.MenuBook, null, modifier = Modifier.size(18.dp), tint = TextWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Subjects & Mark Manually", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
