package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Goal
import com.example.data.GoalStatus
import com.example.ui.theme.CookieRunFontFamily
import com.example.ui.theme.CookieRunTypography
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Deep Hot Color Palette
val DeepHotCrimson = Color(0xFFFF0844)
val DeepHotFlamingOrange = Color(0xFFFF4E50)
val DeepHotSunGold = Color(0xFFFFD600)
val DeepHotNeonPink = Color(0xFFFF007F)
val DeepHotMagmaPurple = Color(0xFF9D00FF)
val DeepHotElectricBlue = Color(0xFF00F2FE)
val DeepHotPlasmaCyan = Color(0xFF4FACFE)
val DeepHotCyberMint = Color(0xFF00F5A0)
val DeepHotEmerald = Color(0xFF10B981)

@Composable
fun DailyTargetFireworksCard(
    totalTarget: Int,
    totalSolved: Int,
    targetPhysics: Int,
    solvedPhysics: Int,
    targetChem: Int,
    solvedChem: Int,
    targetBio: Int,
    solvedBio: Int,
    isDark: Boolean,
    onQuickAdd: (subject: String, count: Int) -> Unit,
    todayGoals: List<Goal> = emptyList(),
    pendingTodayGoals: List<Goal> = emptyList(),
    onGoalStatusChange: ((goalId: Int, newStatus: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val totalTargetCoerced = totalTarget.coerceAtLeast(1)
    val remaining = (totalTarget - totalSolved).coerceAtLeast(0)
    val pct = ((totalSolved.toFloat() / totalTargetCoerced.toFloat()) * 100).toInt().coerceIn(0, 100)
    val isCompleted = pct >= 100 && totalSolved > 0

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xCCFFFFFF) else Color(0xFF475569)

    // Master Infinite Animation for Fireworks, Pulses and Spark Emitters
    val infiniteTransition = rememberInfiniteTransition(label = "fireworksCardAnim")

    val sparkTimeState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkTime"
    )

    val flamePulseState = infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flamePulse"
    )

    val shimmerOffsetState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    // Local Burst Animation Triggers for Tapping +5, +10, +25
    var lastTappedSubject by remember { mutableStateOf<String?>(null) }
    var burstCounter by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row with Fire Icon & Glowing Percentage Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Glowing Flame Icon Badge
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val s = flamePulseState.value
                                scaleX = s
                                scaleY = s
                            }
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isDark) Color(0x25FF4E50) else Color(0x1AFF4E50)
                            )
                            .border(1.dp, Color(0x40FF4E50), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎯", fontSize = 20.sp)
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Daily Target Message",
                                fontFamily = CookieRunFontFamily,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                fontSize = 17.sp
                            )
                        }
                        Text(
                            text = if (isCompleted) "🎉 All Daily Goals Smashed!" else "Today's live study mission status",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCompleted) DeepHotEmerald else subTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Deep Hot Percentage Pill with Glowing Spark Border
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isCompleted) {
                        DeepHotEmerald.copy(alpha = 0.22f)
                    } else {
                        DeepHotCrimson.copy(alpha = 0.18f)
                    },
                    border = BorderStroke(
                        1.2.dp,
                        if (isCompleted) SolidColor(DeepHotEmerald.copy(alpha = 0.8f))
                        else Brush.horizontalGradient(listOf(DeepHotNeonPink, DeepHotSunGold))
                    ),
                    modifier = Modifier.graphicsLayer {
                        val s = if (isCompleted) flamePulseState.value else 1f
                        scaleX = s
                        scaleY = s
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isCompleted) DeepHotEmerald else DeepHotFlamingOrange)
                        )
                        Text(
                            text = if (isCompleted) "100% MET 🎉" else "$pct% DONE",
                            fontFamily = CookieRunFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isCompleted) DeepHotEmerald else if (isDark) Color(0xFFFF6B81) else DeepHotCrimson,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Message Banner Box matching glass styling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9))
                    .border(
                        1.dp,
                        if (isDark) Color(0x18FFFFFF) else Color(0xFFE2E8F0),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(14.dp)
            ) {
                Column {
                    // Notification Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(DeepHotSunGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = DeepHotSunGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = if (isCompleted) {
                                "Awesome job! You completed 100% of today's target ($totalSolved/$totalTarget MCQs)! 🔥"
                            } else if (totalSolved > 0) {
                                "Target: $totalTarget MCQs | Solved: $totalSolved MCQs ($remaining MCQs left today). Keep pushing! ⚡"
                            } else {
                                "Target: $totalTarget MCQs (Phy: $targetPhysics, Chem: $targetChem, Bio: $targetBio). Start solving today's MCQs!"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. PHYSICS PROGRESS BAR WITH DEEP HOT ELECTRIC PLASMA FIREWORKS
                    val pProg = if (targetPhysics > 0) (solvedPhysics.toFloat() / targetPhysics.toFloat()).coerceIn(0f, 1f) else 0f
                    val animatedPhysicsProg by animateFloatAsState(
                        targetValue = pProg,
                        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                        label = "physicsProgress"
                    )

                    SubjectFireworksProgressRow(
                        subject = "Physics",
                        emoji = "⚡",
                        solved = solvedPhysics,
                        target = targetPhysics,
                        progress = animatedPhysicsProg,
                        gradientColors = listOf(
                            Color(0xFF00F2FE),
                            Color(0xFF4FACFE),
                            Color(0xFF2563EB),
                            Color(0xFF7C3AED)
                        ),
                        sparkColor = DeepHotElectricBlue,
                        sparkTime = sparkTimeState,
                        shimmerOffset = shimmerOffsetState,
                        isDark = isDark,
                        textColor = textColor,
                        subTextColor = subTextColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. CHEMISTRY PROGRESS BAR WITH BLAZING MAGMA LAVA FIREWORKS
                    val cProg = if (targetChem > 0) (solvedChem.toFloat() / targetChem.toFloat()).coerceIn(0f, 1f) else 0f
                    val animatedChemistryProg by animateFloatAsState(
                        targetValue = cProg,
                        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                        label = "chemProgress"
                    )

                    SubjectFireworksProgressRow(
                        subject = "Chemistry",
                        emoji = "🧪",
                        solved = solvedChem,
                        target = targetChem,
                        progress = animatedChemistryProg,
                        gradientColors = listOf(
                            Color(0xFFFF0844),
                            Color(0xFFFF4E50),
                            Color(0xFFFFA000),
                            Color(0xFFFFD600)
                        ),
                        sparkColor = DeepHotFlamingOrange,
                        sparkTime = sparkTimeState,
                        shimmerOffset = shimmerOffsetState,
                        isDark = isDark,
                        textColor = textColor,
                        subTextColor = subTextColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. BIOLOGY PROGRESS BAR WITH TOXIC NEON EMERALD FIREWORKS
                    val bProg = if (targetBio > 0) (solvedBio.toFloat() / targetBio.toFloat()).coerceIn(0f, 1f) else 0f
                    val animatedBiologyProg by animateFloatAsState(
                        targetValue = bProg,
                        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                        label = "bioProgress"
                    )

                    SubjectFireworksProgressRow(
                        subject = "Biology",
                        emoji = "🧬",
                        solved = solvedBio,
                        target = targetBio,
                        progress = animatedBiologyProg,
                        gradientColors = listOf(
                            Color(0xFF00F5A0),
                            Color(0xFF00E676),
                            Color(0xFF10B981),
                            Color(0xFF059669)
                        ),
                        sparkColor = DeepHotCyberMint,
                        sparkTime = sparkTimeState,
                        shimmerOffset = shimmerOffsetState,
                        isDark = isDark,
                        textColor = textColor,
                        subTextColor = subTextColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Add Solved MCQs with Fireworks Feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("⚡", fontSize = 13.sp)
                            Text(
                                text = "Quick Add Solved MCQs:",
                                fontSize = 11.5.sp,
                                color = subTextColor,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                        Text(
                            text = "Tap to burst ✨",
                            fontSize = 10.5.sp,
                            color = DeepHotSunGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Physics Quick Buttons
                        QuickAddSubjectColumn(
                            subject = "Physics",
                            labelColor = DeepHotElectricBlue,
                            pillColor = DeepHotElectricBlue,
                            isDark = isDark,
                            modifier = Modifier.weight(1f),
                            onAdd = { count ->
                                lastTappedSubject = "Physics"
                                burstCounter++
                                onQuickAdd("Physics", count)
                            }
                        )

                        // Chemistry Quick Buttons
                        QuickAddSubjectColumn(
                            subject = "Chemistry",
                            labelColor = DeepHotFlamingOrange,
                            pillColor = DeepHotFlamingOrange,
                            isDark = isDark,
                            modifier = Modifier.weight(1f),
                            onAdd = { count ->
                                lastTappedSubject = "Chemistry"
                                burstCounter++
                                onQuickAdd("Chemistry", count)
                            }
                        )

                        // Biology Quick Buttons
                        QuickAddSubjectColumn(
                            subject = "Biology",
                            labelColor = DeepHotCyberMint,
                            pillColor = DeepHotCyberMint,
                            isDark = isDark,
                            modifier = Modifier.weight(1f),
                            onAdd = { count ->
                                lastTappedSubject = "Biology"
                                burstCounter++
                                onQuickAdd("Biology", count)
                            }
                        )
                    }
                }
            }
        }

        // Ambient Firework Burst Canvas Overlay across the card on burst triggers
        if (burstCounter > 0) {
            FireworksBurstCanvas(
                burstId = burstCounter,
                subject = lastTappedSubject ?: "Physics",
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
            )
        }
    }
}

/**
 * Subject Progress Row with a Canvas-based Animated Fireworks & Spark Emitting Progress Bar
 */
@Composable
private fun SubjectFireworksProgressRow(
    subject: String,
    emoji: String,
    solved: Int,
    target: Int,
    progress: Float,
    gradientColors: List<Color>,
    sparkColor: Color,
    sparkTime: State<Float>,
    shimmerOffset: State<Float>,
    isDark: Boolean,
    textColor: Color,
    subTextColor: Color
) {
    val isMet = solved >= target && target > 0
    val pct = (progress * 100).toInt().coerceIn(0, 100)
    val primaryColor = gradientColors.first()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(emoji, fontSize = 12.sp)
                Text(
                    text = subject,
                    fontSize = 12.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = CookieRunFontFamily
                )
                if (isMet) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DeepHotEmerald.copy(alpha = 0.2f),
                        border = BorderStroke(0.8.dp, DeepHotEmerald.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "✓ MET",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepHotEmerald,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Text(
                text = "$solved / $target Q ($pct%)",
                fontSize = 11.5.sp,
                color = if (isMet) DeepHotEmerald else textColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Custom Canvas Fireworks Progress Bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val barHeight = 8.dp.toPx()
            val barTop = (size.height - barHeight) / 2f
            val cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)

            // 1. Draw Background Track with Deep Glowing Inset
            drawRoundRect(
                color = if (isDark) Color(0xFF10081C) else Color(0xFFE2E8F0),
                topLeft = Offset(0f, barTop),
                size = Size(size.width, barHeight),
                cornerRadius = cornerRadius
            )

            // 2. Draw Active Progress Fill if progress > 0
            if (progress > 0.01f) {
                val fillWidth = (size.width * progress).coerceIn(barHeight, size.width)

                // Hot Glowing Gradient Fill
                val fillBrush = Brush.horizontalGradient(
                    colors = if (isMet) listOf(DeepHotCyberMint, DeepHotEmerald, Color(0xFF047857)) else gradientColors,
                    startX = 0f,
                    endX = fillWidth
                )

                drawRoundRect(
                    brush = fillBrush,
                    topLeft = Offset(0f, barTop),
                    size = Size(fillWidth, barHeight),
                    cornerRadius = cornerRadius
                )

                // Shimmering Molten Wave overlay
                val waveCenter = (fillWidth * shimmerOffset.value)
                val waveWidth = 35.dp.toPx()
                if (waveCenter > 0f && waveCenter < fillWidth + waveWidth) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.55f),
                                Color.Transparent
                            ),
                            startX = (waveCenter - waveWidth).coerceAtLeast(0f),
                            endX = (waveCenter + waveWidth).coerceAtMost(fillWidth)
                        ),
                        topLeft = Offset(0f, barTop),
                        size = Size(fillWidth, barHeight),
                        cornerRadius = cornerRadius
                    )
                }

                // 3. Leading Head - Firework Spark Star & Rocket Core
                val headX = fillWidth
                val headY = size.height / 2f

                // Outer Halo Glow Circle
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            sparkColor.copy(alpha = 0.85f),
                            sparkColor.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(headX, headY),
                        radius = 12.dp.toPx()
                    ),
                    radius = 12.dp.toPx(),
                    center = Offset(headX, headY)
                )

                // Solid Core Sparkle
                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = Offset(headX, headY)
                )

                // 4. FIREWORKS SPARK PARTICLES EMITTED FROM THE TIP
                val currentSparkTime = sparkTime.value
                val particleCount = 10
                for (i in 0 until particleCount) {
                    // Seeded pseudo-random physics for consistent continuous firework sparks
                    val seed = i * 47 + (currentSparkTime * 100).toInt()
                    val particlePhase = (currentSparkTime + (i.toFloat() / particleCount)) % 1f

                    // Angle fanning out (-130 to +50 degrees, radiating upwards and outwards)
                    val baseAngle = -70f + (i * 24f) - 100f
                    val rad = baseAngle * (PI / 180.0)

                    // Distance traveled by spark
                    val maxDist = (14 + (i % 4) * 8).dp.toPx()
                    val currentDist = maxDist * particlePhase

                    val pX = headX + (cos(rad) * currentDist).toFloat()
                    // Gravity pulling sparks slightly downward
                    val pY = headY + (sin(rad) * currentDist).toFloat() + (particlePhase * particlePhase * 6.dp.toPx())

                    val particleAlpha = (1f - particlePhase).coerceIn(0f, 1f)
                    val particleRadius = (2.2.dp.toPx() * (1f - particlePhase * 0.6f)).coerceAtLeast(0.8f)

                    val particleColor = when (i % 3) {
                        0 -> Color.White
                        1 -> sparkColor
                        else -> DeepHotSunGold
                    }.copy(alpha = particleAlpha)

                    // Draw Spark Particle
                    drawCircle(
                        color = particleColor,
                        radius = particleRadius,
                        center = Offset(pX, pY)
                    )

                    // Draw subtle sparkle streak line
                    if (particlePhase < 0.6f) {
                        val trailLength = 3.dp.toPx() * (1f - particlePhase)
                        drawLine(
                            color = particleColor.copy(alpha = particleAlpha * 0.7f),
                            start = Offset(pX, pY),
                            end = Offset(
                                pX - (cos(rad) * trailLength).toFloat(),
                                pY - (sin(rad) * trailLength).toFloat()
                            ),
                            strokeWidth = 1.2.dp.toPx()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quick Add Buttons Column for each subject with deep hot glowing pill styling
 */
@Composable
private fun QuickAddSubjectColumn(
    subject: String,
    labelColor: Color,
    pillColor: Color,
    isDark: Boolean,
    onAdd: (count: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(labelColor)
            )
            Text(
                text = subject,
                fontSize = 11.sp,
                color = labelColor,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = CookieRunFontFamily
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(5, 10, 25).forEach { addQ ->
                Surface(
                    onClick = { onAdd(addQ) },
                    shape = RoundedCornerShape(9.dp),
                    color = if (isDark) pillColor.copy(alpha = 0.18f) else pillColor.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.2.dp,
                        pillColor.copy(alpha = if (isDark) 0.5f else 0.4f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = "+$addQ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = CookieRunFontFamily,
                            color = pillColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-Card Explosive Fireworks Burst Canvas triggered when tapping quick add buttons
 */
@Composable
private fun FireworksBurstCanvas(
    burstId: Int,
    subject: String,
    modifier: Modifier = Modifier
) {
    val burstProgress = remember(burstId) { Animatable(0.01f) }

    LaunchedEffect(burstId) {
        burstProgress.snapTo(0.01f)
        burstProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
        )
    }

    val primaryBurstColor = when (subject) {
        "Physics" -> DeepHotElectricBlue
        "Chemistry" -> DeepHotFlamingOrange
        "Biology" -> DeepHotCyberMint
        else -> DeepHotSunGold
    }

    Canvas(modifier = modifier) {
        val p = burstProgress.value.coerceIn(0.01f, 1f)
        if (p < 0.98f && size.width > 0f && size.height > 0f) {
            val centerX = size.width * 0.5f
            val centerY = size.height * 0.45f
            val maxRadius = (size.width * 0.45f).coerceAtLeast(10f)

            // 1. Expanding Shockwave Ring
            val ringRadius = (maxRadius * p).coerceAtLeast(4f)
            val ringAlpha = (1f - p).coerceIn(0f, 0.8f)
            val strokeW = ((3.5f * (1f - p)).coerceIn(0.5f, 4f)).dp.toPx()

            if (ringRadius > 2f && ringAlpha > 0.05f) {
                drawCircle(
                    color = primaryBurstColor.copy(alpha = ringAlpha * 0.7f),
                    radius = ringRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = strokeW)
                )
            }

            // 2. Exploding Firework Star Sparks
            val sparkCount = 24
            for (i in 0 until sparkCount) {
                val angle = (i.toFloat() / sparkCount) * 360f
                val rad = angle * (PI / 180.0)

                // Staggered distances
                val distMultiplier = 0.6f + ((i % 4) * 0.12f)
                val dist = (maxRadius * p * distMultiplier).coerceAtLeast(0f)

                val sparkX = centerX + (cos(rad) * dist).toFloat()
                val sparkY = centerY + (sin(rad) * dist).toFloat() + (p * p * 18.dp.toPx()) // Gravity fall

                val sparkAlpha = ((1f - p) * 1.1f).coerceIn(0f, 1f)
                val sparkSize = (3.dp.toPx() * (1f - p * 0.5f)).coerceIn(0.8f, 6.dp.toPx())

                val sparkColor = when (i % 4) {
                    0 -> Color.White
                    1 -> primaryBurstColor
                    2 -> DeepHotSunGold
                    else -> DeepHotNeonPink
                }.copy(alpha = sparkAlpha)

                // Spark dot
                if (sparkAlpha > 0.02f) {
                    drawCircle(
                        color = sparkColor,
                        radius = sparkSize,
                        center = Offset(sparkX, sparkY)
                    )

                    // Spark trail ray
                    val trailDist = (5.dp.toPx() * (1f - p)).coerceAtLeast(0.5f)
                    drawLine(
                        color = sparkColor.copy(alpha = sparkAlpha * 0.5f),
                        start = Offset(sparkX, sparkY),
                        end = Offset(
                            sparkX - (cos(rad) * trailDist).toFloat(),
                            sparkY - (sin(rad) * trailDist).toFloat()
                        ),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }
        }
    }
}
