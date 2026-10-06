package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StethoscopeIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF6366F1),
    size: Dp = 28.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = w * 0.08f

        // Ear tips & Y-tubing
        val path = Path().apply {
            // Left ear tip
            moveTo(w * 0.28f, h * 0.12f)
            lineTo(w * 0.32f, h * 0.28f)
            cubicTo(w * 0.32f, h * 0.48f, w * 0.46f, h * 0.52f, w * 0.5f, h * 0.52f)

            // Right ear tip
            moveTo(w * 0.72f, h * 0.12f)
            lineTo(w * 0.68f, h * 0.28f)
            cubicTo(w * 0.68f, h * 0.48f, w * 0.54f, h * 0.52f, w * 0.5f, h * 0.52f)

            // Main flexible tube downwards and curving around to chestpiece
            lineTo(w * 0.5f, h * 0.70f)
            cubicTo(w * 0.5f, h * 0.90f, w * 0.78f, h * 0.90f, w * 0.78f, h * 0.72f)
        }

        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Left ear bulb
        drawCircle(
            color = tint,
            radius = strokeWidth * 0.9f,
            center = Offset(w * 0.28f, h * 0.12f)
        )

        // Right ear bulb
        drawCircle(
            color = tint,
            radius = strokeWidth * 0.9f,
            center = Offset(w * 0.72f, h * 0.12f)
        )

        // Chestpiece (Diaphragm)
        drawCircle(
            color = tint,
            radius = w * 0.12f,
            center = Offset(w * 0.78f, h * 0.72f),
            style = Stroke(width = strokeWidth * 0.8f)
        )

        drawCircle(
            color = tint,
            radius = w * 0.05f,
            center = Offset(w * 0.78f, h * 0.72f)
        )
    }
}
