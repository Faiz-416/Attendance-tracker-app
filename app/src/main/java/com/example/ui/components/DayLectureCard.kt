package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.TodayLectureItem
import com.example.ui.theme.*

@Composable
fun DayLectureCard(
    item: TodayLectureItem,
    onStatusToggle: (AttendanceStatus) -> Unit,
    onEditDetails: (notes: String, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val subject = item.subject
    val slot = item.slot
    val record = item.record
    val currentStatus = item.currentStatus

    var showEditDialog by remember { mutableStateOf(false) }
    var notesInput by remember(record) { mutableStateOf(record?.notes ?: "") }
    var reasonInput by remember(record) { mutableStateOf(record?.absenceReason ?: "") }

    val subjectColor = try {
        Color(android.graphics.Color.parseColor(subject.colorHex))
    } catch (e: Exception) {
        EmeraldPrimary
    }

    val periodNum = slot?.periodNumber ?: record?.periodNumber ?: 1
    val startTime = slot?.startTime ?: record?.startTime ?: "09:00"
    val endTime = slot?.endTime ?: record?.endTime ?: "10:00"
    val room = slot?.room ?: subject.room
    val teacher = slot?.teacher ?: subject.teacher
    val classType = slot?.classType ?: "Class"

    val cardBorderColor = when (currentStatus) {
        AttendanceStatus.PRESENT -> EmeraldPrimary.copy(alpha = 0.4f)
        AttendanceStatus.ABSENT -> AbsentRed.copy(alpha = 0.4f)
        AttendanceStatus.CANCELLED -> CancelledSlate.copy(alpha = 0.4f)
        null -> BorderDark
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("day_lecture_${subject.id}_p$periodNum"),
        backgroundColor = DarkSurfaceVariant.copy(alpha = 0.6f),
        borderColor = cardBorderColor,
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Time & Period Column
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(subjectColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$startTime – $endTime",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkSurface)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Period $periodNum",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            if (item.isExtra) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(WarningAmber.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Extra",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarningAmber
                                    )
                                }
                            }
                        }

                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextWhite,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        val detailsList = listOfNotNull(
                            subject.code.ifBlank { null },
                            classType.ifBlank { null },
                            if (room.isNotBlank()) "Room $room" else null,
                            teacher.ifBlank { null }
                        )

                        if (detailsList.isNotEmpty()) {
                            Text(
                                text = detailsList.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        if (!record?.notes.isNullOrBlank() || !record?.absenceReason.isNullOrBlank()) {
                            Text(
                                text = listOfNotNull(
                                    record?.absenceReason?.ifBlank { null }?.let { "Reason: $it" },
                                    record?.notes?.ifBlank { null }?.let { "Note: $it" }
                                ).joinToString(" | "),
                                style = MaterialTheme.typography.bodySmall,
                                color = WarningAmber,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Edit Icon
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(32.dp).testTag("edit_lecture_btn_p$periodNum")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit record notes",
                        tint = TextDim,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Present / Absent / Cancelled
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // PRESENT Button
                val isPresent = currentStatus == AttendanceStatus.PRESENT
                Button(
                    onClick = { onStatusToggle(AttendanceStatus.PRESENT) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_present_p$periodNum"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPresent) EmeraldPrimary else DarkSurface,
                        contentColor = if (isPresent) CharcoalBlack else TextWhite
                    ),
                    border = BorderStroke(1.dp, if (isPresent) EmeraldPrimary else BorderDark),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isPresent) CharcoalBlack else PresentGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Present",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // ABSENT Button
                val isAbsent = currentStatus == AttendanceStatus.ABSENT
                Button(
                    onClick = { onStatusToggle(AttendanceStatus.ABSENT) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_absent_p$periodNum"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAbsent) AbsentRed else DarkSurface,
                        contentColor = if (isAbsent) Color.White else TextWhite
                    ),
                    border = BorderStroke(1.dp, if (isAbsent) AbsentRed else BorderDark),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isAbsent) Color.White else AbsentRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Absent",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isAbsent) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // CANCELLED Button
                val isCancelled = currentStatus == AttendanceStatus.CANCELLED
                Button(
                    onClick = { onStatusToggle(AttendanceStatus.CANCELLED) },
                    modifier = Modifier
                        .weight(1.1f)
                        .height(42.dp)
                        .testTag("btn_cancelled_p$periodNum"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCancelled) CancelledSlate else DarkSurface,
                        contentColor = if (isCancelled) Color.White else TextMuted
                    ),
                    border = BorderStroke(1.dp, if (isCancelled) CancelledSlate else BorderDark),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isCancelled) Color.White else CancelledSlate
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cancelled",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isCancelled) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "${subject.name} - Period $periodNum",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text("Reason for Absence (if absent)") },
                        placeholder = { Text("e.g., Medical, Event, Transport") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_absence_reason")
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notes / Topic covered") },
                        placeholder = { Text("e.g., Chapter 4 Graph Algorithms") },
                        modifier = Modifier.fillMaxWidth().testTag("input_notes")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEditDetails(notesInput, reasonInput)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = CharcoalBlack),
                    modifier = Modifier.testTag("btn_save_lecture_notes")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceVariant,
            shape = RoundedCornerShape(18.dp)
        )
    }
}
