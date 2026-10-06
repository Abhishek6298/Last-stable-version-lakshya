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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyPractice
import com.example.utils.PracticeStatsCalculator
import com.example.utils.PracticePeriodStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PracticeCapsulePoint(
    val id: String,
    val dayLetter: String,
    val fullDate: String,
    val solvedCount: Int,
    val targetCount: Int,
    val physicsCount: Int = 0,
    val chemistryCount: Int = 0,
    val biologyCount: Int = 0,
    val isHighlighted: Boolean = false,
    val isAboveAverage: Boolean = false
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PracticeOverviewCard(
    dailyPractices: List<DailyPractice>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf("Weekly") } // "Daily", "Weekly" or "Monthly"
    
    val dailyStats = remember(dailyPractices) {
        PracticeStatsCalculator.calculateStatsForDays(dailyPractices, 1)
    }
    
    val weeklyStats = remember(dailyPractices) {
        PracticeStatsCalculator.calculateStatsForDays(dailyPractices, 7)
    }
    
    val monthlyStats = remember(dailyPractices) {
        PracticeStatsCalculator.calculateStatsForDays(dailyPractices, 30)
    }
    
    val currentStats = when (selectedPeriod) {
        "Daily" -> dailyStats
        "Weekly" -> weeklyStats
        else -> monthlyStats
    }

    val (graphSubtitle, trendCapsulePoints) = remember(selectedPeriod, dailyPractices, currentStats) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        when (selectedPeriod) {
            "Daily" -> {
                val pTarget = currentStats.physicsTarget.coerceAtLeast(1)
                val cTarget = currentStats.chemistryTarget.coerceAtLeast(1)
                val bTarget = currentStats.biologyTarget.coerceAtLeast(1)
                val avgSolved = (currentStats.physicsSolved + currentStats.chemistrySolved + currentStats.biologySolved) / 3f

                val list = listOf(
                    PracticeCapsulePoint(
                        id = "d_phy",
                        dayLetter = "Phy",
                        fullDate = "Physics Practice",
                        solvedCount = currentStats.physicsSolved,
                        targetCount = pTarget,
                        physicsCount = currentStats.physicsSolved,
                        chemistryCount = 0,
                        biologyCount = 0,
                        isHighlighted = false,
                        isAboveAverage = currentStats.physicsSolved >= avgSolved && currentStats.physicsSolved > 0
                    ),
                    PracticeCapsulePoint(
                        id = "d_chem",
                        dayLetter = "Chem",
                        fullDate = "Chemistry Practice",
                        solvedCount = currentStats.chemistrySolved,
                        targetCount = cTarget,
                        physicsCount = 0,
                        chemistryCount = currentStats.chemistrySolved,
                        biologyCount = 0,
                        isHighlighted = false,
                        isAboveAverage = currentStats.chemistrySolved >= avgSolved && currentStats.chemistrySolved > 0
                    ),
                    PracticeCapsulePoint(
                        id = "d_bio",
                        dayLetter = "Bio",
                        fullDate = "Biology Practice",
                        solvedCount = currentStats.biologySolved,
                        targetCount = bTarget,
                        physicsCount = 0,
                        chemistryCount = 0,
                        biologyCount = currentStats.biologySolved,
                        isHighlighted = true,
                        isAboveAverage = currentStats.biologySolved >= avgSolved && currentStats.biologySolved > 0
                    )
                )
                "Today's Subjects" to list
            }
            "Weekly" -> {
                val list = mutableListOf<PracticeCapsulePoint>()
                val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
                val fullDateFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
                val dayLetterFormat = SimpleDateFormat("EEEEE", Locale.getDefault())

                val dailySolvedList = mutableListOf<Int>()
                for (i in 6 downTo 0) {
                    val c = Calendar.getInstance()
                    c.add(Calendar.DAY_OF_YEAR, -i)
                    val dateStr = sdf.format(c.time)
                    val dp = dailyPractices.find { it.date == dateStr }
                    val total = if (dp != null) dp.physicsSolved + dp.chemistrySolved + dp.biologySolved else 0
                    dailySolvedList.add(total)
                }
                val avgSolved = if (dailySolvedList.any { it > 0 }) dailySolvedList.filter { it > 0 }.average().toFloat() else 10f

                for (i in 6 downTo 0) {
                    val c = Calendar.getInstance()
                    c.add(Calendar.DAY_OF_YEAR, -i)
                    val dateStr = sdf.format(c.time)
                    val dayLabel = dayNameFormat.format(c.time)
                    val fullDate = fullDateFormat.format(c.time)
                    val dayLetter = dayLetterFormat.format(c.time).take(1).uppercase()
                    val practice = dailyPractices.find { it.date == dateStr }
                    val p = practice?.physicsSolved ?: 0
                    val ch = practice?.chemistrySolved ?: 0
                    val b = practice?.biologySolved ?: 0
                    val totalOnDay = p + ch + b
                    val isToday = (i == 0)

                    list.add(
                        PracticeCapsulePoint(
                            id = "w_$dateStr",
                            dayLetter = if (dayLabel.isNotBlank()) dayLabel.take(3) else dayLetter,
                            fullDate = if (isToday) "Today ($fullDate)" else fullDate,
                            solvedCount = totalOnDay,
                            targetCount = 180,
                            physicsCount = p,
                            chemistryCount = ch,
                            biologyCount = b,
                            isHighlighted = isToday,
                            isAboveAverage = totalOnDay >= avgSolved && totalOnDay > 0
                        )
                    )
                }
                "7-Day Activity" to list
            }
            else -> { // Monthly
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val todayMs = cal.timeInMillis

                fun getSolvedDataForRange(startDaysAgo: Int, endDaysAgo: Int): Triple<Int, Int, Int> {
                    val startTime = todayMs - (startDaysAgo * 24 * 3600 * 1000L)
                    val endTime = todayMs - (endDaysAgo * 24 * 3600 * 1000L) + (24 * 3600 * 1000L)
                    var pSum = 0
                    var cSum = 0
                    var bSum = 0
                    dailyPractices.forEach { dp ->
                        val t = try { sdf.parse(dp.date)?.time ?: 0L } catch (e: Exception) { 0L }
                        if (t in startTime..endTime) {
                            pSum += dp.physicsSolved
                            cSum += dp.chemistrySolved
                            bSum += dp.biologySolved
                        }
                    }
                    return Triple(pSum, cSum, bSum)
                }

                val (w1p, w1c, w1b) = getSolvedDataForRange(27, 21)
                val (w2p, w2c, w2b) = getSolvedDataForRange(20, 14)
                val (w3p, w3c, w3b) = getSolvedDataForRange(13, 7)
                val (w4p, w4c, w4b) = getSolvedDataForRange(6, 0)

                val totals = listOf(w1p + w1c + w1b, w2p + w2c + w2b, w3p + w3c + w3b, w4p + w4c + w4b)
                val avgW = if (totals.any { it > 0 }) totals.filter { it > 0 }.average().toFloat() else 100f

                val list = listOf(
                    PracticeCapsulePoint("m_w1", "W1", "Week 1 (Day 1-7)", w1p + w1c + w1b, 1260, w1p, w1c, w1b, false, (w1p + w1c + w1b) >= avgW && (w1p + w1c + w1b) > 0),
                    PracticeCapsulePoint("m_w2", "W2", "Week 2 (Day 8-14)", w2p + w2c + w2b, 1260, w2p, w2c, w2b, false, (w2p + w2c + w2b) >= avgW && (w2p + w2c + w2b) > 0),
                    PracticeCapsulePoint("m_w3", "W3", "Week 3 (Day 15-21)", w3p + w3c + w3b, 1260, w3p, w3c, w3b, false, (w3p + w3c + w3b) >= avgW && (w3p + w3c + w3b) > 0),
                    PracticeCapsulePoint("m_w4", "This Wk", "Current Week (Day 22-30)", w4p + w4c + w4b, 1260, w4p, w4c, w4b, true, (w4p + w4c + w4b) >= avgW && (w4p + w4c + w4b) > 0)
                )

                "30-Day Monthly Trend" to list
            }
        }
    }
    
    val maxTrendSolved = (trendCapsulePoints.maxOfOrNull { it.solvedCount }?.toFloat() ?: 10f).coerceAtLeast(10f)
    
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0x0AFFFFFF) else Color.White
    val cardBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
    
    val totalPct = if (currentStats.totalTarget > 0) {
        ((currentStats.totalSolved.toFloat() / currentStats.totalTarget.toFloat()) * 100).toInt().coerceIn(0, 100)
    } else 0

    // Interactive chart state
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(selectedPeriod, trendCapsulePoints) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
        if (trendCapsulePoints.isNotEmpty()) {
            selectedIndex = trendCapsulePoints.indices.last
        }
    }

    val activePoint = selectedIndex?.let { trendCapsulePoints.getOrNull(it) }

    // Atmospheric Gradients
    val neonEmeraldGradient = listOf(
        Color(0xFF6EE7B7), // Mint
        Color(0xFF34D399), // Emerald bright
        Color(0xFF10B981), // Neon Green
        Color(0xFF047857)  // Deep Emerald
    )

    val neonPurpleGradient = listOf(
        Color(0xFFE879F9), // Light neon pink
        Color(0xFFC084FC), // Lavender
        Color(0xFF818CF8), // Violet
        Color(0xFF6366F1)  // Deep Indigo
    )

    val amberCoralGradient = listOf(
        Color(0xFFFDE68A), // Light amber
        Color(0xFFFBBF24), // Amber
        Color(0xFFFB923C), // Coral
        Color(0xFFF43F5E)  // Rose
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF10B981), Color(0xFF38BDF8), Color(0xFF818CF8))
                    } else {
                        listOf(Color(0xFF6EE7B7), Color(0xFFA5B4FC))
                    }
                ),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Text("📊", fontSize = 20.sp)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Practice Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor
                            )
                            // Live pulse indicator
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                        }
                        Text(
                            text = "Track your question solving streaks & performance",
                            style = MaterialTheme.typography.labelSmall,
                            color = subTextColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Period Segmented Control
            AppSegmentedControl(
                items = listOf("Daily", "Weekly", "Monthly"),
                selectedItem = selectedPeriod,
                onItemSelected = { selectedPeriod = it },
                itemLabel = { it },
                itemEmoji = {
                    when (it) {
                        "Daily" -> "⚡"
                        "Weekly" -> "📅"
                        else -> "🗓️"
                    }
                },
                selectedColor = Color(0xFF10B981),
                selectedGradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
                isDark = isDark,
                fontSize = 12.sp,
                cornerRadius = 14.dp
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Total solved large view
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0x05FFFFFF) else Color(0xFFF8FAFC))
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = when (selectedPeriod) {
                            "Daily" -> "SOLVED TODAY"
                            "Weekly" -> "SOLVED LAST 7 DAYS"
                            else -> "SOLVED LAST 30 DAYS"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = subTextColor,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${currentStats.totalSolved}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = " / ${currentStats.totalTarget} Qs",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = subTextColor,
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                        )
                    }
                }
                
                // Circular completion indicator representation
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(Color(0xFF34D399), Color(0xFF10B981), Color(0xFF34D399))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(cardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalPct%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor
                            )
                            Text(
                                text = "done",
                                fontSize = 9.sp,
                                color = subTextColor
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            // Graph section header with blinking indicators & interactive note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Solving Trend Graph 📊",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = graphSubtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================================
            // INTERACTIVE TOUCH INSPECTION POPOVER CARD
            // =========================================================================
            AnimatedVisibility(
                visible = activePoint != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                activePoint?.let { pt ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.9f) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (pt.isAboveAverage || pt.solvedCount > 0) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFFFB923C).copy(alpha = 0.6f)
                        ),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (pt.isAboveAverage || pt.solvedCount > 0) Color(0xFF10B981) else Color(0xFFFB923C)
                                            )
                                    )
                                    Column {
                                        Text(
                                            text = pt.fullDate,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        Text(
                                            text = if (pt.isAboveAverage) "High Velocity Day 🚀" else if (pt.solvedCount > 0) "Activity Logged 🎯" else "No Activity Yet",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (pt.isAboveAverage || pt.solvedCount > 0) Color(0xFF10B981) else subTextColor
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (pt.isAboveAverage || pt.solvedCount > 0) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFFB923C).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${pt.solvedCount} Qs",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (pt.isAboveAverage || pt.solvedCount > 0) (if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)) else (if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C)),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (pt.physicsCount > 0 || pt.chemistryCount > 0 || pt.biologyCount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (pt.physicsCount > 0) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF60A5FA).copy(alpha = 0.15f)) {
                                            Text("🪐 P: ${pt.physicsCount}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    if (pt.chemistryCount > 0) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF59E0B).copy(alpha = 0.15f)) {
                                            Text("🧪 C: ${pt.chemistryCount}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    if (pt.biologyCount > 0) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF34D399).copy(alpha = 0.15f)) {
                                            Text("🧬 B: ${pt.biologyCount}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // ATMOSPHERIC GLOWING CAPSULE BAR CHART WITH X & Y AXES
            // =========================================================================
            val maxLabel = "${maxTrendSolved.toInt()} Q"
            val midVal = maxTrendSolved / 2f
            val midLabel = "${midVal.toInt()} Q"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.45f) else Color(0xFFF1F5F9).copy(alpha = 0.7f))
                    .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Y-Axis Scale
                    Column(
                        modifier = Modifier
                            .width(36.dp)
                            .fillMaxHeight()
                            .padding(top = 10.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = maxLabel,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor,
                            maxLines = 1
                        )
                        Text(
                            text = midLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = subTextColor.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                        Text(
                            text = "0 Q",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Canvas and X-Axis Area
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // Interactive Canvas with touch & drag detection
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(trendCapsulePoints) {
                                    detectTapGestures { offset ->
                                        val count = trendCapsulePoints.size
                                        if (count > 0) {
                                            val barSlotWidth = size.width / count
                                            val index = (offset.x / barSlotWidth).toInt().coerceIn(0, count - 1)
                                            selectedIndex = index
                                        }
                                    }
                                }
                                .pointerInput(trendCapsulePoints) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        val count = trendCapsulePoints.size
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
                            val bottomPadding = 28.dp.toPx()
                            val topPadding = 14.dp.toPx()
                            val chartHeight = height - bottomPadding - topPadding

                            // Guidelines
                            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            val guideColor = if (isDark) Color(0x22FFFFFF) else Color(0x22000000)

                            // Top guide
                            drawLine(
                                color = guideColor,
                                start = Offset(0f, topPadding),
                                end = Offset(width, topPadding),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dashedEffect
                            )

                            // Mid guide
                            drawLine(
                                color = guideColor.copy(alpha = 0.5f),
                                start = Offset(0f, topPadding + chartHeight / 2f),
                                end = Offset(width, topPadding + chartHeight / 2f),
                                strokeWidth = 0.8.dp.toPx(),
                                pathEffect = dashedEffect
                            )

                            // Bottom baseline
                            drawLine(
                                color = guideColor,
                                start = Offset(0f, height - bottomPadding),
                                end = Offset(width, height - bottomPadding),
                                strokeWidth = 1.dp.toPx()
                            )

                            if (trendCapsulePoints.isEmpty()) return@Canvas

                            val count = trendCapsulePoints.size
                            val slotWidth = width / count
                            val barWidth = (slotWidth * 0.35f).coerceIn(10.dp.toPx(), 22.dp.toPx())
                            val cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)

                            trendCapsulePoints.forEachIndexed { i, pt ->
                                val centerX = (i + 0.5f) * slotWidth
                                val fraction = if (maxTrendSolved > 0) (pt.solvedCount / maxTrendSolved).coerceIn(0.08f, 1f) else 0.08f
                                val barHeight = chartHeight * fraction * animatedProgress.value
                                val barTop = (height - bottomPadding) - barHeight
                                val barLeft = centerX - (barWidth / 2)

                                val isSelected = selectedIndex == i

                                // Background guide track
                                drawRoundRect(
                                    color = if (isDark) Color(0x0DFFFFFF) else Color(0x08000000),
                                    topLeft = Offset(barLeft, topPadding),
                                    size = Size(barWidth, chartHeight),
                                    cornerRadius = cornerRadius
                                )

                                // Highlight aura if touched/selected
                                if (isSelected) {
                                    drawRoundRect(
                                        color = if (pt.isAboveAverage || pt.solvedCount > 0) Color(0x3310B981) else Color(0x33FB923C),
                                        topLeft = Offset(barLeft - 3.dp.toPx(), barTop - 3.dp.toPx()),
                                        size = Size(barWidth + 6.dp.toPx(), barHeight + 6.dp.toPx()),
                                        cornerRadius = CornerRadius((barWidth + 6.dp.toPx()) / 2, (barWidth + 6.dp.toPx()) / 2)
                                    )
                                }

                                // Gradient Brush for the capsule bar
                                val gradientColors = if (pt.isAboveAverage || pt.solvedCount > 0) {
                                    if (pt.isHighlighted) neonEmeraldGradient else neonPurpleGradient
                                } else {
                                    amberCoralGradient
                                }

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
                                    radius = barWidth * 0.22f,
                                    center = Offset(centerX, barTop + (barWidth / 2))
                                )
                            }
                        }

                        // X-Axis Day Labels below the canvas
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            trendCapsulePoints.forEachIndexed { i, pt ->
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
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) {
                                            if (pt.isAboveAverage || pt.solvedCount > 0) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFFB923C).copy(alpha = 0.2f)
                                        } else Color.Transparent
                                    ) {
                                        Text(
                                            text = pt.dayLetter,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                            color = if (isSelected) {
                                                if (pt.isAboveAverage || pt.solvedCount > 0) Color(0xFF10B981) else Color(0xFFFB923C)
                                            } else subTextColor,
                                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            // Subject-wise progress details
            Text(
                text = "Subject Breakdown",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = subTextColor,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Physics
                val pFraction = if (currentStats.physicsTarget > 0) (currentStats.physicsSolved.toFloat() / currentStats.physicsTarget.toFloat()).coerceIn(0f, 1f) else 0f
                SubjectProgressRow(
                    emoji = "🪐",
                    subjectName = "Physics",
                    solved = currentStats.physicsSolved,
                    target = currentStats.physicsTarget,
                    fraction = pFraction,
                    color = Color(0xFF60A5FA),
                    isDark = isDark,
                    textColor = textColor,
                    subTextColor = subTextColor
                )
                
                // Chemistry
                val cFraction = if (currentStats.chemistryTarget > 0) (currentStats.chemistrySolved.toFloat() / currentStats.chemistryTarget.toFloat()).coerceIn(0f, 1f) else 0f
                SubjectProgressRow(
                    emoji = "🧪",
                    subjectName = "Chemistry",
                    solved = currentStats.chemistrySolved,
                    target = currentStats.chemistryTarget,
                    fraction = cFraction,
                    color = Color(0xFFF59E0B),
                    isDark = isDark,
                    textColor = textColor,
                    subTextColor = subTextColor
                )
                
                // Biology
                val bFraction = if (currentStats.biologyTarget > 0) (currentStats.biologySolved.toFloat() / currentStats.biologyTarget.toFloat()).coerceIn(0f, 1f) else 0f
                SubjectProgressRow(
                    emoji = "🧬",
                    subjectName = "Biology",
                    solved = currentStats.biologySolved,
                    target = currentStats.biologyTarget,
                    fraction = bFraction,
                    color = Color(0xFF34D399),
                    isDark = isDark,
                    textColor = textColor,
                    subTextColor = subTextColor
                )
            }
        }
    }
}

@Composable
fun SubjectProgressRow(
    emoji: String,
    subjectName: String,
    solved: Int,
    target: Int,
    fraction: Float,
    color: Color,
    isDark: Boolean,
    textColor: Color,
    subTextColor: Color
) {
    val pct = (fraction * 100).toInt()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(emoji, fontSize = 14.sp)
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Text(
                text = "$solved / $target Qs ($pct%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        val subjectGradients = when (subjectName.lowercase()) {
            "physics" -> listOf(Color(0xFF00F2FE), Color(0xFF4FACFE), Color(0xFF60A5FA))
            "chemistry" -> listOf(Color(0xFFFF0844), Color(0xFFFF4E50), Color(0xFFFFD600))
            "biology" -> listOf(Color(0xFF00F5A0), Color(0xFF10B981), Color(0xFF34D399))
            else -> listOf(color, color.copy(alpha = 0.7f))
        }
        FireworksProgressBar(
            progress = fraction,
            height = 8.dp,
            gradientColors = subjectGradients,
            sparkColor = color,
            isDark = isDark
        )
    }
}

