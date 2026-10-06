package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Data Model for individual items in the Atmospheric Capsule Chart
 */
data class CapsuleChartDataPoint(
    val id: String,
    val dayLetter: String, // e.g. "S", "M", "T", "W", "T", "F", "S"
    val fullDate: String,  // e.g. "Wed, 14 Aug 2026"
    val value: Float,      // numeric metric
    val displayValue: String, // e.g. "7 h 30m" or "680 / 720"
    val isAboveAverage: Boolean,
    val detailNotes: String = "",
    val subMetric1: Float = 0f, // e.g. Physics 20%
    val subMetric2: Float = 0f, // e.g. Chemistry 50%
    val subMetric3: Float = 0f  // e.g. Biology 30%
)

/**
 * Sleek Interactive Capsule Chart matching the reference visual design:
 * - Day / Week / Month tab switcher with indicator
 * - Slender glowing capsule bars with neon magenta/purple (above avg) & amber/coral (below avg) gradients
 * - Real-time Touch & Drag inspection to reveal exact data tooltips
 * - Bottom metrics with Circular Arc Gauges
 */
@Composable
fun InteractiveAtmosphericCapsuleChart(
    title: String,
    subtitle: String = "",
    selectedPeriod: String, // "Day", "Week", "Month"
    onPeriodSelected: (String) -> Unit,
    dataPoints: List<CapsuleChartDataPoint>,
    averageDisplay: String,
    qualityScore: String = "85/100",
    qualityRating: String = "Good",
    gauge1Label: String = "Physics",
    gauge1Percent: Int = 20,
    gauge2Label: String = "Chemistry",
    gauge2Percent: Int = 50,
    gauge3Label: String = "Biology",
    gauge3Percent: Int = 30,
    maxValue: Float? = null,
    topGuideLabel: String = "Max",
    bottomGuideLabel: String = "Min",
    isDark: Boolean = true,
    monthLabel: String? = null,
    isPastMonth: Boolean = false,
    canGoNextMonth: Boolean = true,
    canGoPrevMonth: Boolean = true,
    onPrevMonth: (() -> Unit)? = null,
    onNextMonth: (() -> Unit)? = null,
    onMonthClick: (() -> Unit)? = null,
    onResetToCurrentMonth: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(dataPoints, selectedPeriod) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
        // Select last active data point by default for immediate preview
        if (dataPoints.isNotEmpty()) {
            selectedIndex = dataPoints.indices.last
        }
    }

    val activeDataPoint = selectedIndex?.let { dataPoints.getOrNull(it) }

    // Color definitions
    val aboveAvgGradient = listOf(
        Color(0xFFE879F9), // Light neon pink
        Color(0xFFC084FC), // Lavender
        Color(0xFF818CF8), // Violet
        Color(0xFF6366F1)  // Deep Indigo
    )

    val belowAvgGradient = listOf(
        Color(0xFFFBBF24), // Amber
        Color(0xFFFB923C), // Coral
        Color(0xFFF43F5E), // Rose Pink
        Color(0xFFE11D48)  // Crimson
    )

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.65f) else Color(0xFFFFFFFF).copy(alpha = 0.85f)
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0xFF818CF8).copy(alpha = if (isDark) 0.35f else 0.2f),
                        Color(0xFFEC4899).copy(alpha = if (isDark) 0.25f else 0.1f)
                    )
                ),
                RoundedCornerShape(28.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Title & Wave Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        letterSpacing = (-0.5).sp
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = subTextColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Sleek wave graph icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time Selector Tabs: Day | Week | Month
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFF1F5F9))
                    .border(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Day", "Week", "Month").forEach { tab ->
                    val isSelected = selectedPeriod.equals(tab, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) {
                                    if (isDark) Color(0xFF334155).copy(alpha = 0.8f) else Color.White
                                } else Color.Transparent
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onPeriodSelected(tab) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = tab,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) textColor else subTextColor
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height(2.5.dp)
                                        .clip(CircleShape)
                                        .background(Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))))
                                )
                            }
                        }
                    }
                }
            }

            // Month Navigator (Revealed when "Month" period is active and monthLabel is provided)
            if (selectedPeriod.equals("Month", ignoreCase = true) && monthLabel != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color(0xFFF1F5F9))
                        .border(
                            1.dp,
                            if (isPastMonth) Color(0xFF8B5CF6).copy(alpha = 0.5f) else (if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onPrevMonth?.invoke() },
                        enabled = canGoPrevMonth,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = if (canGoPrevMonth) (if (isDark) Color.White else Color(0xFF1E293B)) else subTextColor.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = onMonthClick != null) { onMonthClick?.invoke() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (isPastMonth) Color(0xFFEC4899) else Color(0xFF818CF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = monthLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        if (isPastMonth) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Past",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA855F7)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Current",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                        if (onMonthClick != null) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = subTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isPastMonth && onResetToCurrentMonth != null) {
                            IconButton(
                                onClick = { onResetToCurrentMonth() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Today,
                                    contentDescription = "Jump to Current Month",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { onNextMonth?.invoke() },
                            enabled = canGoNextMonth,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Month",
                                tint = if (canGoNextMonth) (if (isDark) Color.White else Color(0xFF1E293B)) else subTextColor.copy(alpha = 0.35f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Interactive Touch Data Popover Bar (Revealed when touched!)
            AnimatedVisibility(
                visible = activeDataPoint != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                activeDataPoint?.let { dp ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.85f) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (dp.isAboveAverage) Color(0xFFC084FC).copy(alpha = 0.6f) else Color(0xFFFB923C).copy(alpha = 0.6f)
                        ),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (dp.isAboveAverage) Color(0xFFC084FC) else Color(0xFFFB923C)
                                        )
                                )
                                Column {
                                    Text(
                                        text = dp.fullDate,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = subTextColor
                                    )
                                    val isNoTest = dp.value <= 0f || dp.displayValue.contains("No Test", ignoreCase = true)
                                    Text(
                                        text = if (isNoTest) "No Test Record ⏸️" else if (dp.isAboveAverage) "Above Average Record 🚀" else "Below Average Benchmark 🎯",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNoTest) subTextColor else if (dp.isAboveAverage) Color(0xFFC084FC) else Color(0xFFFB923C)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (dp.isAboveAverage) Color(0xFFC084FC).copy(alpha = 0.2f) else Color(0xFFFB923C).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = dp.displayValue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (dp.isAboveAverage) (if (isDark) Color(0xFFF0ABFC) else Color(0xFF9333EA)) else (if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C)),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // MAIN CAPSULE BAR CHART CANVAS (Slender glowing pills)
            // ----------------------------------------------------
            val effectiveMax = maxValue ?: (dataPoints.maxOfOrNull { it.value }?.coerceAtLeast(10f) ?: 10f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Interactive Canvas with touch & drag detection
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dataPoints) {
                            detectTapGestures { offset ->
                                val count = dataPoints.size
                                if (count > 0) {
                                    val barSlotWidth = size.width / count
                                    val index = (offset.x / barSlotWidth).toInt().coerceIn(0, count - 1)
                                    selectedIndex = index
                                }
                            }
                        }
                        .pointerInput(dataPoints) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val count = dataPoints.size
                                if (count > 0) {
                                    val barSlotWidth = size.width / count
                                    val index = (change.position.x / barSlotWidth).toInt().coerceIn(0, count - 1)
                                    selectedIndex = index
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 32.dp.toPx()
                    val topPadding = 20.dp.toPx()
                    val chartHeight = height - bottomPadding - topPadding

                    // 1. Subtle horizontal guide lines (Min & Max)
                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    val guideColor = if (isDark) Color(0x26FFFFFF) else Color(0x33000000)

                    // Top guide line
                    drawLine(
                        color = guideColor,
                        start = Offset(0f, topPadding),
                        end = Offset(width, topPadding),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashedEffect
                    )

                    // Bottom baseline
                    drawLine(
                        color = guideColor,
                        start = Offset(0f, height - bottomPadding),
                        end = Offset(width, height - bottomPadding),
                        strokeWidth = 1.dp.toPx()
                    )

                    if (dataPoints.isEmpty()) return@Canvas

                    val count = dataPoints.size
                    val slotWidth = width / count
                    val barWidth = (slotWidth * 0.28f).coerceIn(8.dp.toPx(), 18.dp.toPx())
                    val cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)

                    dataPoints.forEachIndexed { i, dp ->
                        val centerX = (i + 0.5f) * slotWidth
                        val fraction = if (effectiveMax > 0) (dp.value / effectiveMax).coerceIn(0.06f, 1f) else 0.1f
                        val barHeight = chartHeight * fraction * animatedProgress.value
                        val barTop = (height - bottomPadding) - barHeight
                        val barLeft = centerX - (barWidth / 2)

                        val isSelected = selectedIndex == i

                        // Background guide track (subtle light column)
                        drawRoundRect(
                            color = if (isDark) Color(0x0FFFFFFF) else Color(0x0A000000),
                            topLeft = Offset(barLeft, topPadding),
                            size = Size(barWidth, chartHeight),
                            cornerRadius = cornerRadius
                        )

                        // Highlight aura if touched
                        if (isSelected) {
                            drawRoundRect(
                                color = if (dp.isAboveAverage) Color(0x33C084FC) else Color(0x33FB923C),
                                topLeft = Offset(barLeft - 4.dp.toPx(), barTop - 4.dp.toPx()),
                                size = Size(barWidth + 8.dp.toPx(), barHeight + 8.dp.toPx()),
                                cornerRadius = CornerRadius((barWidth + 8.dp.toPx()) / 2, (barWidth + 8.dp.toPx()) / 2)
                            )
                        }

                        // Gradient Brush for the capsule bar
                        val gradientColors = if (dp.isAboveAverage) aboveAvgGradient else belowAvgGradient
                        val barBrush = Brush.verticalGradient(
                            colors = gradientColors,
                            startY = barTop,
                            endY = height - bottomPadding
                        )

                        // Draw Slender Capsule Bar
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = cornerRadius
                        )

                        // Glowing Top Cap Reflection
                        drawCircle(
                            color = Color.White.copy(alpha = if (isSelected) 0.95f else 0.75f),
                            radius = barWidth * 0.25f,
                            center = Offset(centerX, barTop + (barWidth / 2))
                        )
                    }
                }

                // X-Axis Day Labels below the canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    dataPoints.forEachIndexed { i, dp ->
                        val isSelected = selectedIndex == i
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedIndex = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dp.dayLetter,
                                fontSize = when {
                                    dp.dayLetter.length > 3 -> 9.sp
                                    dp.dayLetter.length > 2 -> 10.sp
                                    else -> 11.5.sp
                                },
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                color = if (isSelected) {
                                    if (dp.isAboveAverage) Color(0xFFC084FC) else Color(0xFFFB923C)
                                } else subTextColor,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart Legend (Above Average / Below Average dots)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(aboveAvgGradient))
                    )
                    Text("Above average", fontSize = 11.5.sp, color = subTextColor, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.width(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(belowAvgGradient))
                    )
                    Text("Below average", fontSize = 11.5.sp, color = subTextColor, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ----------------------------------------------------
            // METRICS ROW: Average Time/Score & Quality Rating Cards
            // ----------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Average Time / Score
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Average Metric",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subTextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = averageDisplay,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEC4899).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Details",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Card 2: Quality & Efficiency Index
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Accuracy / Quality",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subTextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = qualityScore,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = qualityRating,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ----------------------------------------------------
            // CIRCULAR GAUGE ARCS (3 Radial Breakdown Rings)
            // ----------------------------------------------------
            Text(
                text = "Performance Breakdown",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gauge 1: (e.g. Physics / Awake)
                CircularGaugeItem(
                    label = gauge1Label,
                    percent = gauge1Percent,
                    gradientColors = listOf(Color(0xFFFB923C), Color(0xFFF43F5E)),
                    isDark = isDark
                )

                // Gauge 2: (e.g. Chemistry / Light)
                CircularGaugeItem(
                    label = gauge2Label,
                    percent = gauge2Percent,
                    gradientColors = listOf(Color(0xFFC084FC), Color(0xFF9333EA)),
                    isDark = isDark
                )

                // Gauge 3: (e.g. Biology / Deep)
                CircularGaugeItem(
                    label = gauge3Label,
                    percent = gauge3Percent,
                    gradientColors = listOf(Color(0xFF38BDF8), Color(0xFF2563EB)),
                    isDark = isDark
                )
            }
        }
    }
}

/**
 * Individual Circular Arc Gauge matching the bottom 3 circular indicators of the reference image
 */
@Composable
fun CircularGaugeItem(
    label: String,
    percent: Int,
    gradientColors: List<Color>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedPercent = remember { Animatable(0f) }
    LaunchedEffect(percent) {
        animatedPercent.animateTo(
            targetValue = percent.toFloat(),
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                val strokeWidth = 7.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                // Background Track Arc (Faint circle)
                drawCircle(
                    color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                // Foreground Glowing Arc
                val sweepAngle = (animatedPercent.value / 100f) * 360f
                drawArc(
                    brush = Brush.sweepGradient(gradientColors),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Center Percentage
            Text(
                text = "${percent}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
    }
}
