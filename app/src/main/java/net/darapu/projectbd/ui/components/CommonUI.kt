package net.darapu.projectbd.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class MetricType {
    NONE, MOVE, EXERCISE, STAND, STEPS
}

data class RingData(val progress: Float, val color: Color, val type: MetricType)

@Composable
fun InteractiveActivityRings(
    rings: List<RingData>,
    selectedMetric: MetricType,
    size: androidx.compose.ui.unit.Dp
) {
    val strokeWidth = 14.dp
    val spacing = 3.dp

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        rings.forEachIndexed { index, ring ->
            val ringSize = size - (index * (strokeWidth + spacing).value * 2.2f).dp
            val alpha by animateFloatAsState(
                targetValue = if (selectedMetric == MetricType.NONE || selectedMetric == ring.type) 1f else 0.15f,
                label = "alpha"
            )
            
            ActivityRingInternal(
                progress = ring.progress,
                color = ring.color,
                backgroundColor = ring.color.copy(alpha = 0.2f),
                strokeWidth = strokeWidth,
                alpha = alpha,
                modifier = Modifier.size(ringSize)
            )
        }
    }
}

@Composable
fun ActivityRingInternal(
    progress: Float,
    color: Color,
    backgroundColor: Color,
    strokeWidth: androidx.compose.ui.unit.Dp,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawArc(
            color = backgroundColor.copy(alpha = alpha * 0.2f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = color.copy(alpha = alpha),
            startAngle = -90f,
            sweepAngle = (progress * 360f).coerceIn(0.1f, 360f),
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun MetricDetailRow(
    label: String, 
    value: String, 
    color: Color, 
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(text = value, fontWeight = FontWeight.Bold)
        }
    }
}
