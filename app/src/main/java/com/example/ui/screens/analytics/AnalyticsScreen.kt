package com.example.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.model.AttendanceStatus
import com.example.data.model.PeriodFilter
import com.example.ui.components.AttendanceGauge
import com.example.ui.components.GlassCard
import com.example.ui.components.ProgressBarWithTarget
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.util.AttendanceCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AttendanceViewModel
) {
    val allRecords by viewModel.attendanceRecords.collectAsStateWithLifecycle()
    val allSubjects by viewModel.activeSubjects.collectAsStateWithLifecycle()
    val academicSettings by viewModel.academicSettings.collectAsStateWithLifecycle()
    val subjectsWithStats by viewModel.subjectsWithStats.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf(PeriodFilter.FilterType.ALL_TIME) }
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) } // null = overall

    // Interactive Simulator State
    var simulateAttendCount by remember { mutableStateOf(0) }
    var simulateMissCount by remember { mutableStateOf(0) }

    // Date range for chosen filter
    val today = LocalDate.now()
    val filteredRecords = remember(allRecords, selectedPeriod, selectedSubjectId, academicSettings) {
        val dateFiltered = when (selectedPeriod) {
            PeriodFilter.FilterType.TODAY -> {
                val tStr = today.toString()
                allRecords.filter { it.date == tStr }
            }
            PeriodFilter.FilterType.THIS_WEEK -> {
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
                val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).toString()
                allRecords.filter { it.date in startOfWeek..endOfWeek }
            }
            PeriodFilter.FilterType.THIS_MONTH -> {
                val ym = YearMonth.now().toString()
                allRecords.filter { it.date.startsWith(ym) }
            }
            PeriodFilter.FilterType.SEMESTER -> {
                val sStart = academicSettings.semesterStartDate.ifBlank { "2000-01-01" }
                val sEnd = academicSettings.semesterEndDate.ifBlank { "2099-12-31" }
                allRecords.filter { it.date in sStart..sEnd }
            }
            else -> allRecords
        }

        if (selectedSubjectId != null) {
            dateFiltered.filter { it.subjectId == selectedSubjectId }
        } else {
            dateFiltered
        }
    }

    // Metrics for filtered dataset
    val presentCount = filteredRecords.count { it.status == AttendanceStatus.PRESENT.name }
    val absentCount = filteredRecords.count { it.status == AttendanceStatus.ABSENT.name }
    val cancelledCount = filteredRecords.count { it.status == AttendanceStatus.CANCELLED.name }
    val conductedCount = presentCount + absentCount
    val percentage = AttendanceCalculator.calculatePercentage(presentCount, absentCount)

    val currentSubject = allSubjects.find { it.id == selectedSubjectId }
    val targetThreshold = currentSubject?.targetThreshold?.takeIf { it > 0 } ?: academicSettings.defaultThreshold

    val requiredConsecutive = AttendanceCalculator.calculateRequiredConsecutive(presentCount, absentCount, targetThreshold)
    val safeBunks = AttendanceCalculator.calculateSafeBunks(presentCount, absentCount, targetThreshold)

    // Simulated calculation
    val simulatedPercentage = AttendanceCalculator.simulateAttendance(
        currentPresent = presentCount,
        currentAbsent = absentCount,
        additionalPresent = simulateAttendCount,
        additionalAbsent = simulateMissCount
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics & Predictions",
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
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
        ) {
            // 1. Period Filter Chips (Horizontal Scroll)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        PeriodFilter.FilterType.ALL_TIME to "All Time",
                        PeriodFilter.FilterType.TODAY to "Today",
                        PeriodFilter.FilterType.THIS_WEEK to "This Week",
                        PeriodFilter.FilterType.THIS_MONTH to "This Month",
                        PeriodFilter.FilterType.SEMESTER to "Semester"
                    ).forEach { (type, label) ->
                        val isSelected = selectedPeriod == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPeriod = type },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = BrightMint
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = BorderDark,
                                selectedBorderColor = EmeraldPrimary,
                                enabled = true,
                                selected = isSelected
                            ),
                            modifier = Modifier.testTag("filter_period_${type.name}")
                        )
                    }
                }
            }

            // 2. Subject Filter Horizontal Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSubjectId == null,
                        onClick = { selectedSubjectId = null },
                        label = { Text("Overall") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MintSubtle,
                            selectedLabelColor = BrightMint
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = BorderDark,
                            selectedBorderColor = MintSecondary,
                            enabled = true,
                            selected = selectedSubjectId == null
                        ),
                        modifier = Modifier.testTag("filter_subj_overall")
                    )

                    allSubjects.forEach { subj ->
                        val isSelected = selectedSubjectId == subj.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSubjectId = if (isSelected) null else subj.id },
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
                            modifier = Modifier.testTag("filter_subj_${subj.id}")
                        )
                    }
                }
            }

            // 3. Gauge & Prediction Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().testTag("analytics_gauge_card"),
                    backgroundColor = DarkSurfaceVariant.copy(alpha = 0.7f),
                    borderColor = BorderDark,
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
                                    text = currentSubject?.name ?: "Overall Attendance",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Target: ${targetThreshold.toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }

                            if (percentage != null) {
                                val isAbove = percentage >= targetThreshold
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isAbove) MintSubtle else AbsentRed.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isAbove) "Safe Margin" else "Critical Alert",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAbove) BrightMint else AbsentRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        AttendanceGauge(
                            percentage = percentage,
                            targetThreshold = targetThreshold,
                            size = 150.dp,
                            strokeWidth = 14.dp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Detailed Bunk / Catch-up Math Explanation
                        if (conductedCount > 0 && percentage != null) {
                            if (percentage >= targetThreshold) {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundColor = SubtleGreenSurface,
                                    borderColor = EmeraldPrimary.copy(alpha = 0.3f),
                                    cornerRadius = 12.dp
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = BrightMint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Safe Absences Allowed: $safeBunks",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = BrightMint
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (safeBunks > 0) "You can miss up to $safeBunks upcoming class${if (safeBunks > 1) "es" else ""} and your attendance will stay at or above ${targetThreshold.toInt()}%." else "You are exactly at the minimum required boundary. Don't miss the next class!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextWhite
                                        )
                                    }
                                }
                            } else {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundColor = AbsentRed.copy(alpha = 0.12f),
                                    borderColor = AbsentRed.copy(alpha = 0.4f),
                                    cornerRadius = 12.dp
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Warning,
                                                contentDescription = null,
                                                tint = AbsentRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Needs $requiredConsecutive Consecutive Classes",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AbsentRed
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "To bring attendance back up to the required ${targetThreshold.toInt()}% threshold, attend the next $requiredConsecutive lecture${if (requiredConsecutive > 1) "s" else ""} consecutively without missing.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Metric Breakdown Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Conducted",
                        value = "$conductedCount",
                        icon = Icons.Filled.School,
                        accentColor = MintSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Attended",
                        value = "$presentCount",
                        icon = Icons.Filled.CheckCircle,
                        accentColor = PresentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Missed",
                        value = "$absentCount",
                        icon = Icons.Filled.Cancel,
                        accentColor = AbsentRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Interactive Attendance Simulator ("What-If" Calculator)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().testTag("attendance_simulator_card"),
                    backgroundColor = DarkSurfaceVariant.copy(alpha = 0.6f),
                    borderColor = MintSecondary.copy(alpha = 0.3f),
                    cornerRadius = 18.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Calculate,
                                    contentDescription = null,
                                    tint = BrightMint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Attendance Simulator",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }

                            TextButton(
                                onClick = {
                                    simulateAttendCount = 0
                                    simulateMissCount = 0
                                },
                                modifier = Modifier.testTag("btn_reset_simulator")
                            ) {
                                Text("Reset", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Text(
                            text = "Forecast what happens if you attend or miss future classes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Controls: Attend more / Miss more
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Attend Stepper
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurface)
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Attend Next", style = MaterialTheme.typography.labelSmall, color = BrightMint)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                        onClick = { if (simulateAttendCount > 0) simulateAttendCount-- },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Remove, null, tint = TextWhite, modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "+$simulateAttendCount",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = { simulateAttendCount++ },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, null, tint = BrightMint, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Miss Stepper
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurface)
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Miss Next", style = MaterialTheme.typography.labelSmall, color = AbsentRed)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                        onClick = { if (simulateMissCount > 0) simulateMissCount-- },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Remove, null, tint = TextWhite, modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "+$simulateMissCount",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = { simulateMissCount++ },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, null, tint = AbsentRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Forecast Result
                        if (simulatedPercentage != null) {
                            val isSimAbove = simulatedPercentage >= targetThreshold
                            val simColor = if (isSimAbove) BrightMint else AbsentRed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSimAbove) MintSubtle else AbsentRed.copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Projected Percentage:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = "${presentCount + simulateAttendCount} / ${conductedCount + simulateAttendCount + simulateMissCount} classes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", simulatedPercentage),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = simColor
                                )
                            }
                        }
                    }
                }
            }

            // 6. Subject-Wise Comparison Bars (if overall view)
            if (selectedSubjectId == null && subjectsWithStats.isNotEmpty()) {
                item {
                    Text(
                        text = "Subject Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(subjectsWithStats, key = { it.subject.id }) { item ->
                    val subj = item.subject
                    val stats = item.stats
                    val p = stats.percentage

                    val subjColor = try {
                        Color(android.graphics.Color.parseColor(subj.colorHex))
                    } catch (e: Exception) {
                        EmeraldPrimary
                    }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSubjectId = subj.id }
                            .testTag("analytics_subj_bar_${subj.id}"),
                        backgroundColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                        borderColor = BorderDark,
                        cornerRadius = 14.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(subjColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = subj.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextWhite
                                    )
                                }

                                Text(
                                    text = if (p != null) String.format(Locale.US, "%.1f%%", p) else "No Data",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        p == null -> TextMuted
                                        p >= stats.targetThreshold -> BrightMint
                                        p >= stats.targetThreshold - 10 -> WarningAmber
                                        else -> AbsentRed
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            ProgressBarWithTarget(
                                percentage = p,
                                targetThreshold = stats.targetThreshold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${stats.presentCount}/${stats.totalConducted} classes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = if (stats.isAboveThreshold) "${item.safeBunks} bunks safe" else "Attend ${item.requiredConsecutiveClasses}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (stats.isAboveThreshold) BrightMint else AbsentRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
