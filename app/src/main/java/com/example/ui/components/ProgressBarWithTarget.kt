package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun ProgressBarWithTarget(
    percentage: Float?,
    targetThreshold: Float = 75f,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    barColor: Color? = null
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percentage ?: 0f,
        label = "progress_bar_anim"
    )

    val actualColor = barColor ?: when {
        percentage == null -> CancelledSlate
        percentage >= targetThreshold -> EmeraldPrimary
        percentage >= targetThreshold - 10 -> WarningAmber
        else -> AbsentRed
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
    ) {
        val w = this.size.width
        val h = this.size.height

        // Background bar
        drawRoundRect(
            color = BorderDark,
            size = Size(w, h),
            cornerRadius = CornerRadius(h / 2, h / 2)
        )

        // Progress bar
        if (percentage != null && percentage > 0) {
            val progressWidth = (w * (animatedPercent / 100f)).coerceIn(0f, w)
            drawRoundRect(
                color = actualColor,
                size = Size(progressWidth, h),
                cornerRadius = CornerRadius(h / 2, h / 2)
            )
        }

        // Target Threshold vertical line
        val targetX = (w * (targetThreshold / 100f)).coerceIn(0f, w)
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(targetX, 0f),
            end = Offset(targetX, h),
            strokeWidth = 2.dp.toPx()
        )
    }
}
