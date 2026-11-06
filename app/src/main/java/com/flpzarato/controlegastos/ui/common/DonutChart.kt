package com.flpzarato.controlegastos.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import java.math.BigDecimal

data class DonutSlice(val value: BigDecimal, val color: Color)

/** Simple donut chart drawn directly on a Canvas, so no charting library is needed. */
@Composable
fun DonutChart(slices: List<DonutSlice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }
    Canvas(modifier = modifier) {
        if (total <= BigDecimal.ZERO) return@Canvas

        val strokeWidth = size.minDimension * 0.28f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = (slice.value.toFloat() / total.toFloat()) * 360f
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            )
            startAngle += sweep
        }
    }
}
