package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Universal Fireworks & Glowing Motion Spark Progress Bar.
 * Features:
 * - Smooth spring/tween progress motion animation.
 * - Dynamic travelling molten shimmer light wave along the active track.
 * - Leading glowing head with pulsing halo and rocket core.
 * - Real radiating fireworks spark particles emitted from the progress tip with gravity, trails, and multi-color bursts.
 * - Special 360-degree celebratory explosion sparks when progress reaches 100% (1.0f).
 */
@Composable
fun FireworksProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(
        Color(0xFFFF0844),
        Color(0xFFFF4E50),
        Color(0xFFFFD600)
    ),
    sparkColor: Color = Color(0xFFFF4E50),
    height: Dp = 8.dp,
    isDark: Boolean = true,
    trackColor: Color? = null,
    animateProgress: Boolean = true,
    animationDurationMillis: Int = 800,
    showSparks: Boolean = true,
    showShimmer: Boolean = true
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val isCompleted = clampedProgress >= 0.999f

    val animatedProg by if (animateProgress) {
        animateFloatAsState(
            targetValue = clampedProgress,
            animationSpec = tween(durationMillis = animationDurationMillis, easing = FastOutSlowInEasing),
            label = "fireworksBarProgress"
        )
    } else {
        rememberUpdatedState(clampedProgress)
    }

    // Continuous Animation Driver for Fireworks Sparks & Shimmer Sweep
    val infiniteTransition = rememberInfiniteTransition(label = "fireworksBarMotion")

    val sparkTimeState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkTime"
    )

    val shimmerOffsetState = infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val haloPulseState = infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloPulse"
    )

    val effectiveTrackColor = trackColor ?: if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height + 12.dp)
    ) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val sparkTime = sparkTimeState.value
        val shimmerOffset = shimmerOffsetState.value
        val haloPulse = haloPulseState.value

        val barHeight = height.toPx()
        val barTop = (size.height - barHeight) / 2f
        val cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)

        // 1. Draw Background Track with Soft Recessed Gradient Inset
        drawRoundRect(
            color = effectiveTrackColor,
            topLeft = Offset(0f, barTop),
            size = Size(size.width, barHeight),
            cornerRadius = cornerRadius
        )

        // 2. Draw Active Progress Fill
        if (animatedProg > 0.005f) {
            val fillWidth = (size.width * animatedProg).coerceIn(barHeight, size.width)

            // Dynamic Gradient Colors
            val effectiveGradients = if (isCompleted) {
                listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF00F5A0), Color(0xFFFFD600))
            } else {
                gradientColors
            }

            val fillBrush = Brush.horizontalGradient(
                colors = effectiveGradients,
                startX = 0f,
                endX = fillWidth
            )

            drawRoundRect(
                brush = fillBrush,
                topLeft = Offset(0f, barTop),
                size = Size(fillWidth, barHeight),
                cornerRadius = cornerRadius
            )

            // 3. Shimmer Molten Wave Sweep
            if (showShimmer) {
                val waveCenter = (fillWidth * shimmerOffset)
                val waveWidth = (30.dp.toPx()).coerceAtLeast(barHeight * 2f)
                if (waveCenter > -waveWidth && waveCenter < fillWidth + waveWidth) {
                    val shimmerBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.65f),
                            Color.Transparent
                        ),
                        startX = (waveCenter - waveWidth).coerceAtLeast(0f),
                        endX = (waveCenter + waveWidth).coerceAtMost(fillWidth)
                    )
                    drawRoundRect(
                        brush = shimmerBrush,
                        topLeft = Offset(0f, barTop),
                        size = Size(fillWidth, barHeight),
                        cornerRadius = cornerRadius
                    )
                }
            }

            // 4. Leading Head - Firework Spark Core & Pulsing Halo
            val headX = fillWidth
            val headY = size.height / 2f

            val haloRadius = (height.toPx() * 1.5f * haloPulse).coerceAtLeast(8.dp.toPx())
            val effectiveSparkColor = if (isCompleted) Color(0xFF10B981) else sparkColor

            // Multi-tier radial glow aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        effectiveSparkColor.copy(alpha = 0.85f),
                        effectiveSparkColor.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(headX, headY),
                    radius = haloRadius
                ),
                radius = haloRadius,
                center = Offset(headX, headY)
            )

            // Solid Core Star
            val coreRadius = (height.toPx() * 0.45f).coerceIn(2.5.dp.toPx(), 6.dp.toPx())
            drawCircle(
                color = Color.White,
                radius = coreRadius,
                center = Offset(headX, headY)
            )

            // 5. RADIATING FIREWORKS SPARK PARTICLES & TRAILS
            if (showSparks) {
                val particleCount = if (isCompleted) 16 else 10
                for (i in 0 until particleCount) {
                    val particlePhase = (sparkTime + (i.toFloat() / particleCount)) % 1f

                    // Trajectory angle: 360-degree burst on complete, radiating fanned arc forward otherwise
                    val baseAngle = if (isCompleted) {
                        (i * (360f / particleCount)) + (sparkTime * 45f)
                    } else {
                        // Fanning out (-140 to +40 degrees)
                        -80f + (i * 22f) - 60f
                    }
                    val rad = baseAngle * (PI / 180.0)

                    // Distance traveled with easing
                    val maxDist = (10 + (i % 5) * 6).dp.toPx()
                    val currentDist = maxDist * particlePhase

                    val pX = headX + (cos(rad) * currentDist).toFloat()
                    val gravityOffset = if (isCompleted) 0f else (particlePhase * particlePhase * 4.dp.toPx())
                    val pY = headY + (sin(rad) * currentDist).toFloat() + gravityOffset

                    val particleAlpha = (1f - particlePhase).coerceIn(0f, 1f)
                    val particleRadius = (2.2.dp.toPx() * (1f - particlePhase * 0.5f)).coerceAtLeast(0.7f)

                    val particleColor = when (i % 4) {
                        0 -> Color.White
                        1 -> effectiveSparkColor
                        2 -> Color(0xFFFFD600) // Deep Sun Gold
                        else -> Color(0xFF00F5A0) // Cyber Mint
                    }.copy(alpha = particleAlpha)

                    // Draw the primary spark
                    drawCircle(
                        color = particleColor,
                        radius = particleRadius,
                        center = Offset(pX, pY)
                    )

                    // Draw glowing trail streak
                    if (particlePhase < 0.7f) {
                        val trailLen = 3.5.dp.toPx() * (1f - particlePhase)
                        drawLine(
                            color = particleColor.copy(alpha = particleAlpha * 0.6f),
                            start = Offset(pX, pY),
                            end = Offset(
                                pX - (cos(rad) * trailLen).toFloat(),
                                pY - (sin(rad) * trailLen).toFloat()
                            ),
                            strokeWidth = 1.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fireworks Circular Progress Bar with rotating glowing sparkhead and dancing firework particles.
 */
@Composable
fun FireworksCircularProgressBar(
    progress: Float,
    modifier: Modifier = Modifier.size(48.dp),
    strokeWidth: Dp = 5.dp,
    gradientColors: List<Color> = listOf(
        Color(0xFFFF0844),
        Color(0xFFFF4E50),
        Color(0xFFFFD600)
    ),
    sparkColor: Color = Color(0xFFFF4E50),
    isDark: Boolean = true,
    trackColor: Color? = null,
    animateProgress: Boolean = true,
    showSparks: Boolean = true
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val isCompleted = clampedProgress >= 0.999f

    val animatedProg by if (animateProgress) {
        animateFloatAsState(
            targetValue = clampedProgress,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            label = "fireworksCircularProg"
        )
    } else {
        rememberUpdatedState(clampedProgress)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fireworksCircularMotion")
    val sparkTimeState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "circSparkTime"
    )

    val effectiveTrackColor = trackColor ?: if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)

    Canvas(modifier = modifier) {
        val sparkTime = sparkTimeState.value
        val sw = strokeWidth.toPx()
        val diameter = size.minDimension - sw - 12.dp.toPx()
        if (diameter <= 0f) return@Canvas

        val centerOffset = Offset(size.width / 2f, size.height / 2f)
        val radius = diameter / 2f
        val topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)
        val arcSize = Size(diameter, diameter)

        // 1. Draw Background Ring
        drawArc(
            color = effectiveTrackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )

        // 2. Draw Active Arc
        if (animatedProg > 0.005f) {
            val sweepAngle = 360f * animatedProg
            val effectiveColors = if (isCompleted) {
                listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFFFFD600))
            } else {
                gradientColors
            }

            drawArc(
                brush = Brush.sweepGradient(
                    colors = effectiveColors,
                    center = centerOffset
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )

            // Leading head position on circle circumference
            val headAngleRad = (-90f + sweepAngle) * (PI / 180.0)
            val headX = centerOffset.x + (radius * cos(headAngleRad)).toFloat()
            val headY = centerOffset.y + (radius * sin(headAngleRad)).toFloat()

            // Glowing Head
            drawCircle(
                color = Color.White,
                radius = sw * 0.6f,
                center = Offset(headX, headY)
            )

            // Outer Spark Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, sparkColor.copy(alpha = 0.7f), Color.Transparent),
                    center = Offset(headX, headY),
                    radius = sw * 2.2f
                ),
                radius = sw * 2.2f,
                center = Offset(headX, headY)
            )

            // Radiating Fireworks Sparks
            if (showSparks) {
                val particleCount = 8
                for (i in 0 until particleCount) {
                    val phase = (sparkTime + (i.toFloat() / particleCount)) % 1f
                    val angleOffset = -40f + (i * 20f)
                    val particleRad = headAngleRad + (angleOffset * (PI / 180.0))
                    val dist = (6 + (i % 3) * 5).dp.toPx() * phase

                    val pX = headX + (cos(particleRad) * dist).toFloat()
                    val pY = headY + (sin(particleRad) * dist).toFloat()

                    val alpha = (1f - phase).coerceIn(0f, 1f)
                    val pColor = when (i % 3) {
                        0 -> Color.White
                        1 -> sparkColor
                        else -> Color(0xFFFFD600)
                    }.copy(alpha = alpha)

                    drawCircle(
                        color = pColor,
                        radius = (1.8.dp.toPx() * (1f - phase * 0.4f)).coerceAtLeast(0.6f),
                        center = Offset(pX, pY)
                    )
                }
            }
        }
    }
}

/**
 * Drop-in lambda-compatible Fireworks Progress Indicator replacing standard LinearProgressIndicator.
 */
@Composable
fun FireworksLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6366F1),
    trackColor: Color? = null,
    isDark: Boolean = true,
    height: Dp = 8.dp
) {
    FireworksProgressBar(
        progress = progress(),
        modifier = modifier,
        height = height,
        gradientColors = listOf(color, color.copy(alpha = 0.8f), Color(0xFFFFD600)),
        sparkColor = color,
        isDark = isDark,
        trackColor = trackColor
    )
}
