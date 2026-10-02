package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AttendanceGauge(
    percentage: Float?,
    targetThreshold: Float = 75f,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = 14.dp
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percentage ?: 0f,
        animationSpec = tween(durationMillis = 800),
        label = "gauge_anim"
    )

    val gaugeColor = when {
        percentage == null -> CancelledSlate
        percentage >= targetThreshold -> EmeraldPrimary
        percentage >= targetThreshold - 10 -> WarningAmber
        else -> AbsentRed
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            val canvasSize = this.size.minDimension
            val radius = canvasSize / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)

            // Start angle: 135 degrees (bottom left), sweep 270 degrees
            val startAngle = 135f
            val maxSweepAngle = 270f

            // Background Track
            drawArc(
                color = BorderDark,
                startAngle = startAngle,
                sweepAngle = maxSweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // Target threshold tick marker
            val thresholdAngle = startAngle + (targetThreshold / 100f) * maxSweepAngle
            val rad = Math.toRadians(thresholdAngle.toDouble())
            val innerMarkerRadius = radius - strokeWidth.toPx() * 0.8f
            val outerMarkerRadius = radius + strokeWidth.toPx() * 0.8f
            val p1 = Offset(
                center.x + (innerMarkerRadius * cos(rad)).toFloat(),
                center.y + (innerMarkerRadius * sin(rad)).toFloat()
            )
            val p2 = Offset(
                center.x + (outerMarkerRadius * cos(rad)).toFloat(),
                center.y + (outerMarkerRadius * sin(rad)).toFloat()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = p1,
                end = p2,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Active Progress
            if (percentage != null && percentage > 0) {
                val currentSweep = (animatedPercent / 100f).coerceIn(0f, 1f) * maxSweepAngle
                drawArc(
                    color = gaugeColor,
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Center Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (percentage == null) {
                Text(
                    text = "--",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Text(
                    text = "No Classes",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.1f", percentage),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = gaugeColor
                    )
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = gaugeColor,
                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                    )
                }
                Text(
                    text = if (percentage >= targetThreshold) "Above Goal" else "Below Goal",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (percentage >= targetThreshold) BrightMint else AbsentRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
