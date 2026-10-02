package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SubjectWithStats
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun SubjectAttendanceCard(
    item: SubjectWithStats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subject = item.subject
    val stats = item.stats
    val percentage = stats.percentage

    val subjectColor = try {
        Color(android.graphics.Color.parseColor(subject.colorHex))
    } catch (e: Exception) {
        EmeraldPrimary
    }

    val statusColor = when {
        percentage == null -> CancelledSlate
        percentage >= stats.targetThreshold -> EmeraldPrimary
        percentage >= stats.targetThreshold - 10 -> WarningAmber
        else -> AbsentRed
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("subject_card_${subject.id}")
            .clickable { onClick() },
        backgroundColor = DarkSurfaceVariant.copy(alpha = 0.5f),
        borderColor = if (percentage != null && percentage < stats.targetThreshold) {
            AbsentRed.copy(alpha = 0.35f)
        } else {
            BorderDark
        },
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Color bar / icon + Name + Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(subjectColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite,
                            maxLines = 1
                        )
                        if (subject.code.isNotBlank() || subject.teacher.isNotBlank()) {
                            Text(
                                text = listOf(subject.code, subject.teacher).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Percentage
                if (percentage == null) {
                    Text(
                        text = "No Classes",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextDim
                    )
                } else {
                    Text(
                        text = String.format(Locale.US, "%.1f%%", percentage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            ProgressBarWithTarget(
                percentage = percentage,
                targetThreshold = stats.targetThreshold,
                barColor = statusColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer info: Conducted count + Safe bunks / required classes badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stats.presentCount}/${stats.totalConducted} attended" + if (stats.cancelledCount > 0) " (${stats.cancelledCount} canc.)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                // Prediction / Bunk Pill
                if (percentage != null && stats.totalConducted > 0) {
                    if (stats.isAboveThreshold) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MintSubtle)
                                .border(1.dp, EmeraldPrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (item.safeBunks > 0) "Can bunk ${item.safeBunks} class${if (item.safeBunks > 1) "es" else ""}" else "On Track",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrightMint,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AbsentRed.copy(alpha = 0.15f))
                                .border(1.dp, AbsentRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Attend ${item.requiredConsecutiveClasses} to reach ${(stats.targetThreshold).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = AbsentRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
