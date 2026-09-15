package com.hornoreflow.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun TempCurveChart(points: List<Pair<Int, Float>>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val minTemp = 0f
        val maxTemp = 260f
        val maxTime = points.last().first.coerceAtLeast(1)

        for (i in 0..4) {
            val y = size.height * i / 4f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }

        val path = Path()
        points.forEachIndexed { index, (t, temp) ->
            val x = size.width * t / maxTime
            val y = size.height * (1f - ((temp - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f))
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
    }
}
