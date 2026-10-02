package com.example.ui.screens.calendar

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.model.AttendanceStatus
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: AttendanceViewModel
) {
    val selectedDateStr by viewModel.calendarSelectedDate.collectAsStateWithLifecycle()
    val allRecords by viewModel.attendanceRecords.collectAsStateWithLifecycle()
    val allSubjects by viewModel.allSubjects.collectAsStateWithLifecycle()

    val subjectMap = remember(allSubjects) { allSubjects.associateBy { it.id } }

    var selectedSubjectIdFilter by remember { mutableStateOf<Long?>(null) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }

    val selectedLocalDate = try { LocalDate.parse(selectedDateStr) } catch (e: Exception) { LocalDate.now() }

    // Filter records
    val filteredRecords = remember(allRecords, selectedSubjectIdFilter) {
        if (selectedSubjectIdFilter != null) {
            allRecords.filter { it.subjectId == selectedSubjectIdFilter }
        } else {
            allRecords
        }
    }

    // Records for selected date
    val dayRecords = remember(filteredRecords, selectedDateStr) {
        filteredRecords.filter { it.date == selectedDateStr }.sortedBy { it.startTime }
    }

    // Records mapped by date for monthly calendar indicator
    val recordsByDate = remember(filteredRecords, currentYearMonth) {
        filteredRecords.filter { it.date.startsWith(currentYearMonth.toString()) }
            .groupBy { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Attendance History",
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
        ) {
            // 1. Subject Filter Horizontal Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSubjectIdFilter == null,
                        onClick = { selectedSubjectIdFilter = null },
                        label = { Text("All Subjects") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = BrightMint
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = BorderDark,
                            selectedBorderColor = EmeraldPrimary,
                            enabled = true,
                            selected = selectedSubjectIdFilter == null
                        ),
                        modifier = Modifier.testTag("filter_chip_all_subjects")
                    )

                    allSubjects.take(5).forEach { subj ->
                        val isSelected = selectedSubjectIdFilter == subj.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSubjectIdFilter = if (isSelected) null else subj.id },
                            label = { Text(subj.code.ifBlank { subj.name }, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MintSubtle,
                                selectedLabelColor = BrightMint
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = BorderDark,
                                selectedBorderColor = MintSecondary,
                                enabled = true,
                                selected = isSelected
                            ),
                            modifier = Modifier.testTag("filter_chip_subj_${subj.id}")
                        )
                    }
                }
            }

            // 2. Month Selector & Calendar Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().testTag("monthly_calendar_card"),
                    backgroundColor = DarkSurfaceVariant.copy(alpha = 0.6f),
                    borderColor = BorderDark,
                    cornerRadius = 18.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Month Header Navigation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                                modifier = Modifier.size(36.dp).testTag("btn_prev_month")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Month",
                                    tint = TextWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )

                            IconButton(
                                onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                                modifier = Modifier.size(36.dp).testTag("btn_next_month")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Month",
                                    tint = TextWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Weekday Labels
                        val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            weekdays.forEach { day ->
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Calendar Grid Days
                        val daysInMonth = currentYearMonth.lengthOfMonth()
                        val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value // 1 (Mon) to 7 (Sun)
                        val totalCells = ((firstDayOfWeek - 1) + daysInMonth + 6) / 7 * 7

                        for (week in 0 until (totalCells / 7)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (dayCol in 0 until 7) {
                                    val cellIndex = week * 7 + dayCol
                                    val dayNum = cellIndex - (firstDayOfWeek - 1) + 1

                                    if (dayNum in 1..daysInMonth) {
                                        val dayDate = currentYearMonth.atDay(dayNum)
                                        val dayDateStr = dayDate.toString()
                                        val isSelected = dayDateStr == selectedDateStr
                                        val isToday = dayDate == LocalDate.now()

                                        val dayRecs = recordsByDate[dayDateStr] ?: emptyList()
                                        val hasPresent = dayRecs.any { it.status == AttendanceStatus.PRESENT.name }
                                        val hasAbsent = dayRecs.any { it.status == AttendanceStatus.ABSENT.name }
                                        val hasCancelled = dayRecs.any { it.status == AttendanceStatus.CANCELLED.name }

                                        val dotColor = when {
                                            hasAbsent -> AbsentRed
                                            hasPresent -> PresentGreen
                                            hasCancelled -> CancelledSlate
                                            else -> null
                                        }

                                        Column(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isSelected -> EmeraldPrimary
                                                        isToday -> DarkSurface
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .border(
                                                    width = if (isToday && !isSelected) 1.dp else 0.dp,
                                                    color = if (isToday && !isSelected) EmeraldPrimary.copy(alpha = 0.5f) else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { viewModel.setCalendarSelectedDate(dayDateStr) }
                                                .testTag("cal_day_$dayDateStr"),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) CharcoalBlack else TextWhite
                                            )
                                            if (dotColor != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) CharcoalBlack else dotColor)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.height(4.dp))
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(38.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Selected Day Detail Section
            item {
                val formattedSelected = selectedLocalDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault()))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedSelected,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "${dayRecords.size} class${if (dayRecords.size != 1) "es" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // 4. Day Records List or Empty
            if (dayRecords.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = DarkSurfaceVariant.copy(alpha = 0.4f),
                        borderColor = BorderDark,
                        cornerRadius = 14.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = TextDim,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No attendance recorded on this date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                items(dayRecords, key = { it.id }) { record ->
                    val subj = subjectMap[record.subjectId]
                    val status = try { AttendanceStatus.valueOf(record.status) } catch (e: Exception) { AttendanceStatus.PRESENT }

                    val statusColor = when (status) {
                        AttendanceStatus.PRESENT -> PresentGreen
                        AttendanceStatus.ABSENT -> AbsentRed
                        AttendanceStatus.CANCELLED -> CancelledSlate
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth().testTag("history_record_${record.id}"),
                        backgroundColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                        borderColor = statusColor.copy(alpha = 0.25f),
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
                                        text = "${record.startTime} – ${record.endTime}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextWhite
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• Period ${record.periodNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                                Text(
                                    text = subj?.name ?: "Subject #${record.subjectId}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Medium
                                )
                                if (record.notes.isNotBlank() || record.absenceReason.isNotBlank()) {
                                    Text(
                                        text = listOfNotNull(
                                            record.absenceReason.ifBlank { null }?.let { "Reason: $it" },
                                            record.notes.ifBlank { null }?.let { "Note: $it" }
                                        ).joinToString(" | "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarningAmber
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusColor.copy(alpha = 0.15f))
                                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = status.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteAttendanceRecord(record.id) },
                                    modifier = Modifier.size(36.dp).testTag("btn_delete_record_${record.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete Record",
                                        tint = TextDim,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
