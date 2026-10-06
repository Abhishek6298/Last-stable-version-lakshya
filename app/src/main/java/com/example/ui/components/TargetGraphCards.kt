package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyPractice
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TodayTargetGraphCard(
    pTarget: Int,
    cTarget: Int,
    bTarget: Int,
    pSolved: Int,
    cSolved: Int,
    bSolved: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val totalTarget = (pTarget + cTarget + bTarget).coerceAtLeast(1)
    val totalSolved = pSolved + cSolved + bSolved
    val progress = (totalSolved.toFloat() / totalTarget.toFloat()).coerceIn(0f, 1f)
    val progressPercent = (progress * 100).toInt()
    val remaining = (totalTarget - totalSolved).coerceAtLeast(0)
    val isAllCompleted = totalSolved >= totalTarget && totalTarget > 0

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val cardBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
    val containerBg = if (isDark) Color(0x0AFFFFFF) else Color(0xFFF8FAFC)
    val pinkRedBlink = Color(0xFFEC4899)

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF818CF8), Color(0xFFC084FC), Color(0xFF38BDF8))
                    } else {
                        listOf(Color(0xFFA5B4FC), Color(0xFFDDD6FE), Color(0xFFBAE6FD))
                    }
                ),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with title and live blinking badge positioned underneath
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚡", fontSize = 20.sp)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Today's Target Analytics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Live Badge positioned cleanly below the title
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isAllCompleted) Color(0xFF10B981).copy(alpha = 0.2f) else if (isDark) Color(0x22EC4899) else Color(0xFFFCE7F3),
                            border = BorderStroke(
                                1.dp,
                                if (isAllCompleted) Color(0xFF10B981) else Color(0xFFEC4899)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isAllCompleted) Color(0xFF10B981) else pinkRedBlink)
                                )
                                Text(
                                    text = if (isAllCompleted) "TARGET MET 🎉" else "TODAY LIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isAllCompleted) Color(0xFF10B981) else Color(0xFFEC4899),
                                    letterSpacing = 0.4.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Text(
                            text = "Daily target performance breakdown",
                            style = MaterialTheme.typography.labelSmall,
                            color = subTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Hero Progress Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(containerBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TOTAL QUESTIONS SOLVED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$totalSolved",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isAllCompleted) Color(0xFF10B981) else Color(0xFF6366F1)
                            )
                            Text(
                                text = " / $totalTarget Qs",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = subTextColor,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Status pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isAllCompleted -> Color(0xFF10B981).copy(alpha = 0.15f)
                                progress >= 0.5f -> Color(0xFF6366F1).copy(alpha = 0.15f)
                                else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = when {
                                    isAllCompleted -> "🔥 Daily Target Accomplished!"
                                    totalSolved == 0 -> "⏳ Start solving to hit today's goal"
                                    else -> "⚡ $remaining Qs remaining to reach goal"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isAllCompleted -> Color(0xFF10B981)
                                    progress >= 0.5f -> Color(0xFF818CF8)
                                    else -> Color(0xFFF59E0B)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Circular Progress Dial
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    colors = if (isAllCompleted) {
                                        listOf(Color(0xFF34D399), Color(0xFF10B981), Color(0xFF059669), Color(0xFF34D399))
                                    } else {
                                        listOf(Color(0xFF6366F1), Color(0xFFEC4899), Color(0xFF38BDF8), Color(0xFF6366F1))
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF0F172A) else Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$progressPercent%",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor
                                )
                                Text(
                                    text = "DONE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subTextColor,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Subject Bar Chart Graph Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Subject Graph 📊",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = subTextColor,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Physics • Chem • Bio",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = subTextColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visual 3-Pillar Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(containerBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Physics Bar
                SubjectVerticalBar(
                    subject = "Physics",
                    emoji = "🪐",
                    solved = pSolved,
                    target = pTarget,
                    gradientColors = listOf(Color(0xFF38BDF8), Color(0xFF60A5FA), Color(0xFF2563EB)),
                    textColor = textColor,
                    subTextColor = subTextColor
                )

                // Chemistry Bar
                SubjectVerticalBar(
                    subject = "Chem",
                    emoji = "🧪",
                    solved = cSolved,
                    target = cTarget,
                    gradientColors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFD97706)),
                    textColor = textColor,
                    subTextColor = subTextColor
                )

                // Biology Bar
                SubjectVerticalBar(
                    subject = "Biology",
                    emoji = "🧬",
                    solved = bSolved,
                    target = bTarget,
                    gradientColors = listOf(Color(0xFF6EE7B7), Color(0xFF34D399), Color(0xFF059669)),
                    textColor = textColor,
                    subTextColor = subTextColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Status KPI Chips (Target, Solved, Remaining)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(
                    label = "Target",
                    value = "$totalTarget Qs",
                    bgColor = if (isDark) Color(0x186366F1) else Color(0xFFEEF2FF),
                    textColor = Color(0xFF6366F1),
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "Solved",
                    value = "$totalSolved Qs",
                    bgColor = if (isDark) Color(0x1810B981) else Color(0xFFECFDF5),
                    textColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "Remaining",
                    value = "$remaining Qs",
                    bgColor = if (isDark) Color(0x18EC4899) else Color(0xFFFCE7F3),
                    textColor = Color(0xFFEC4899),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun WeeklyTargetGraphCard(
    wpTarget: Int,
    wcTarget: Int,
    wbTarget: Int,
    wpSolved: Int,
    wcSolved: Int,
    wbSolved: Int,
    dailyPractices: List<DailyPractice>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    // Auto-calculate last 7 days solved per subject from dailyPractices
    val (autoWp, autoWc, autoWb) = remember(dailyPractices) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var pSum = 0
        var cSum = 0
        var bSum = 0
        for (i in 0..6) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(c.time)
            val practice = dailyPractices.find { it.date == dateStr }
            if (practice != null) {
                pSum += practice.physicsSolved
                cSum += practice.chemistrySolved
                bSum += practice.biologySolved
            }
        }
        Triple(pSum, cSum, bSum)
    }

    val finalWpSolved = if (wpSolved > 0) wpSolved else autoWp
    val finalWcSolved = if (wcSolved > 0) wcSolved else autoWc
    val finalWbSolved = if (wbSolved > 0) wbSolved else autoWb

    val weeklyTargetTotal = (wpTarget + wcTarget + wbTarget).coerceAtLeast(1)
    val weeklySolvedTotal = finalWpSolved + finalWcSolved + finalWbSolved
    val weeklyProgress = (weeklySolvedTotal.toFloat() / weeklyTargetTotal.toFloat()).coerceIn(0f, 1f)
    val weeklyProgressPct = (weeklyProgress * 100).toInt()
    val weeklyRemaining = (weeklyTargetTotal - weeklySolvedTotal).coerceAtLeast(0)

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val cardBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
    val containerBg = if (isDark) Color(0x0AFFFFFF) else Color(0xFFF8FAFC)

    // Calculate last 7 days solved counts
    val last7DaysData = remember(dailyPractices, finalWpSolved, finalWcSolved, finalWbSolved) {
        val list = mutableListOf<Pair<String, Int>>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(c.time)
            val dayLabel = dayNameFormat.format(c.time)
            val practice = dailyPractices.find { it.date == dateStr }
            var totalOnDay = if (practice != null) practice.physicsSolved + practice.chemistrySolved + practice.biologySolved else 0
            if (i == 0) {
                val manualWeeklyTotal = finalWpSolved + finalWcSolved + finalWbSolved
                if (manualWeeklyTotal > totalOnDay) {
                    totalOnDay = manualWeeklyTotal
                }
            }
            list.add(Pair(dayLabel, totalOnDay))
        }
        list
    }

    val maxDaySolved = (last7DaysData.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    val weeklyAvgPerDay = if (last7DaysData.isNotEmpty()) last7DaysData.map { it.second }.average().toInt() else 0

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF34D399), Color(0xFF38BDF8), Color(0xFFA855F7))
                    } else {
                        listOf(Color(0xFFA7F3D0), Color(0xFFBAE6FD), Color(0xFFDDD6FE))
                    }
                ),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF10B981), Color(0xFF06B6D4))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📅", fontSize = 20.sp)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Weekly Target & 7-Day Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Weekly Badge positioned below title
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDark) Color(0x2210B981) else Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "WEEKLY TRACKER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF10B981),
                                    letterSpacing = 0.4.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Text(
                            text = "Comprehensive 7-day progress overview",
                            style = MaterialTheme.typography.labelSmall,
                            color = subTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Stats Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(containerBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WEEKLY GOAL SOLVED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$weeklySolvedTotal",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = " / $weeklyTargetTotal Qs",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = subTextColor,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "⚡ 7-Day Daily Avg: $weeklyAvgPerDay Qs/day",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Circular Percentage Dial
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(Color(0xFF10B981), Color(0xFF8B5CF6), Color(0xFF06B6D4), Color(0xFF10B981))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF064E3B).copy(alpha = 0.8f) else Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$weeklyProgressPct%",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669)
                                )
                                Text(
                                    text = "WEEK",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subTextColor,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7-Day Trend Chart Graph
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "7-Day Question Solving Trend 📊",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = subTextColor,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Peak: $maxDaySolved Qs",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Upgraded 7-Day Trend Pillar Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(containerBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                last7DaysData.forEachIndexed { index, (dayLabel, solvedCount) ->
                    val isToday = index == 6
                    val fraction = (solvedCount.toFloat() / maxDaySolved.toFloat()).coerceIn(0.08f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Count Badge on top
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isToday) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFF818CF8).copy(alpha = 0.12f),
                            border = BorderStroke(
                                0.8.dp,
                                if (isToday) Color(0xFF10B981).copy(alpha = 0.7f) else Color(0xFF818CF8).copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                }
                                Text(
                                    text = "$solvedCount",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isToday) Color(0xFF10B981) else textColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Vertical Bar Pillar (Atmospheric Capsule Style)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .width(22.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (isToday) Color(0xFF10B981).copy(alpha = 0.12f)
                                    else Color(0x0FFFFFFF)
                                )
                                .border(
                                    0.8.dp,
                                    if (isToday) Color(0xFF10B981).copy(alpha = 0.4f)
                                    else Color(0xFF818CF8).copy(alpha = 0.2f),
                                    RoundedCornerShape(11.dp)
                                ),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Glowing Filled Capsule Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(fraction)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        if (isToday) {
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFF6EE7B7),
                                                    Color(0xFF10B981),
                                                    Color(0xFF047857)
                                                )
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFFA5B4FC),
                                                    Color(0xFF818CF8),
                                                    Color(0xFF4F46E5)
                                                )
                                            )
                                        }
                                    )
                            )

                            // Top Glow Cap Line
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(fraction)
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 3.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.85f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isToday) "Today" else dayLabel,
                            fontSize = 10.sp,
                            fontWeight = if (isToday) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isToday) Color(0xFF10B981) else subTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Weekly Subject Breakdown Bars
            Text(
                text = "Weekly Subject Targets 📊",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = subTextColor,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WeeklySubjectRow(
                    subject = "Physics (Weekly)",
                    emoji = "🪐",
                    solved = finalWpSolved,
                    target = wpTarget,
                    gradientColors = listOf(Color(0xFF60A5FA), Color(0xFF2563EB)),
                    isDark = isDark,
                    textColor = textColor,
                    subTextColor = subTextColor
                )
                WeeklySubjectRow(
                    subject = "Chemistry (Weekly)",
                    emoji = "🧪",
                    solved = finalWcSolved,
                    target = wcTarget,
                    gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                    isDark = isDark,
                    textColor = textColor,
                    subTextColor = subTextColor
                )
                WeeklySubjectRow(
                    subject = "Biology (Weekly)",
                    emoji = "🧬",
                    solved = finalWbSolved,
                    target = wbTarget,
                    gradientColors = listOf(Color(0xFF34D399), Color(0xFF059669)),
                    isDark = isDark,
                    textColor = textColor,
                    subTextColor = subTextColor
                )
            }
        }
    }
}

@Composable
private fun SubjectVerticalBar(
    subject: String,
    emoji: String,
    solved: Int,
    target: Int,
    gradientColors: List<Color>,
    textColor: Color,
    subTextColor: Color
) {
    val targetFraction = if (target > 0) (solved.toFloat() / target.toFloat()).coerceIn(0.08f, 1f) else 0.08f
    val animatedBarFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "subjectVerticalBarAnim"
    )
    val isDone = solved >= target && target > 0
    val primaryColor = gradientColors.getOrNull(1) ?: gradientColors.first()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight()
    ) {
        // Count Pill badge on top matching Atmospheric style
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isDone) Color(0xFF10B981).copy(alpha = 0.22f) else primaryColor.copy(alpha = 0.15f),
            border = BorderStroke(
                1.dp,
                if (isDone) Color(0xFF10B981).copy(alpha = 0.7f) else primaryColor.copy(alpha = 0.45f)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
            ) {
                if (isDone) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                }
                Text(
                    text = "$solved/$target",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDone) Color(0xFF10B981) else textColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Full Capsule Track with rounded capsule pill inside (Atmospheric Design System)
        Box(
            modifier = Modifier
                .weight(1f)
                .width(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x0FFFFFFF))
                .border(
                    0.8.dp,
                    primaryColor.copy(alpha = 0.25f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Filled Glowing Capsule Bar
            Box(
                modifier = Modifier
                    .fillMaxHeight(animatedBarFraction)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            if (isDone) listOf(Color(0xFF6EE7B7), Color(0xFF10B981), Color(0xFF059669))
                            else gradientColors
                        )
                    )
            )

            // Glowing Top Cap Reflection (like AtmosphericCapsuleChart)
            Box(
                modifier = Modifier
                    .fillMaxHeight(animatedBarFraction)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "$emoji $subject",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = subTextColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun WeeklySubjectRow(
    subject: String,
    emoji: String,
    solved: Int,
    target: Int,
    gradientColors: List<Color>,
    isDark: Boolean,
    textColor: Color,
    subTextColor: Color
) {
    val pct = if (target > 0) ((solved.toFloat() / target.toFloat()) * 100).toInt().coerceIn(0, 100) else 0
    val progressFraction = if (target > 0) (solved.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val isDone = solved >= target && target > 0
    val primaryColor = gradientColors.first()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0x0AFFFFFF) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDone) Color(0xFF10B981).copy(alpha = 0.4f) else (if (isDark) Color(0x15FFFFFF) else Color(0xFFE2E8F0))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(emoji, fontSize = 14.sp)
                    Text(subject, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDone) Color(0xFF10B981).copy(alpha = 0.2f) else primaryColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isDone) "✓ $solved/$target Qs ($pct%)" else "$solved / $target Qs ($pct%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDone) Color(0xFF10B981) else primaryColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Fireworks Progress Bar
            FireworksProgressBar(
                progress = progressFraction,
                height = 8.dp,
                gradientColors = if (isDone) listOf(Color(0xFF34D399), Color(0xFF10B981)) else gradientColors,
                sparkColor = if (isDone) Color(0xFF34D399) else primaryColor,
                isDark = isDark
            )
        }
    }
}

@Composable
fun DifficultyPracticeAnalyticsCard(
    currentPractice: DailyPractice,
    allPractices: List<DailyPractice>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf("selected") } // "selected" or "week"

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    val easyColor = Color(0xFF10B981)
    val mediumColor = Color(0xFFF59E0B)
    val hardColor = Color(0xFFEF4444)

    // Data calculation based on viewMode
    val (easyCount, mediumCount, hardCount, totalCount) = remember(viewMode, currentPractice, allPractices) {
        if (viewMode == "selected") {
            var e = 0
            var m = 0
            var h = 0
            when (currentPractice.physicsDifficulty.lowercase()) {
                "easy" -> e += currentPractice.physicsSolved
                "hard" -> h += currentPractice.physicsSolved
                else -> m += currentPractice.physicsSolved
            }
            when (currentPractice.chemistryDifficulty.lowercase()) {
                "easy" -> e += currentPractice.chemistrySolved
                "hard" -> h += currentPractice.chemistrySolved
                else -> m += currentPractice.chemistrySolved
            }
            when (currentPractice.biologyDifficulty.lowercase()) {
                "easy" -> e += currentPractice.biologySolved
                "hard" -> h += currentPractice.biologySolved
                else -> m += currentPractice.biologySolved
            }
            val tot = e + m + h
            listOf(e, m, h, tot)
        } else {
            val recent = allPractices.sortedByDescending { it.date }.take(7)
            var e = 0
            var m = 0
            var h = 0
            recent.forEach { p ->
                when (p.physicsDifficulty.lowercase()) {
                    "easy" -> e += p.physicsSolved
                    "hard" -> h += p.physicsSolved
                    else -> m += p.physicsSolved
                }
                when (p.chemistryDifficulty.lowercase()) {
                    "easy" -> e += p.chemistrySolved
                    "hard" -> h += p.chemistrySolved
                    else -> m += p.chemistrySolved
                }
                when (p.biologyDifficulty.lowercase()) {
                    "easy" -> e += p.biologySolved
                    "hard" -> h += p.biologySolved
                    else -> m += p.biologySolved
                }
            }
            val tot = e + m + h
            listOf(e, m, h, tot)
        }
    }

    val easyPct = if (totalCount > 0) ((easyCount.toFloat() / totalCount) * 100).toInt() else 0
    val mediumPct = if (totalCount > 0) ((mediumCount.toFloat() / totalCount) * 100).toInt() else 0
    val hardPct = if (totalCount > 0) (100 - easyPct - mediumPct).coerceAtLeast(0) else 0

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFEC4899))
                    } else {
                        listOf(Color(0xFFBAE6FD), Color(0xFFA5B4FC), Color(0xFFFBCFE8))
                    }
                ),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎯", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "Difficulty Balance Diagnostic",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                        Text(
                            text = if (viewMode == "selected") "Active Date (${currentPractice.date})" else "Last 7 Days Cumulative",
                            fontSize = 11.5.sp,
                            color = subTextColor
                        )
                    }
                }

                // View Toggle Pill (Today vs 7-Days)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0),
                    modifier = Modifier.clickable {
                        viewMode = if (viewMode == "selected") "week" else "selected"
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (viewMode == "selected") "📅 Day" else "📈 7D",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segmented Progress Bar
            if (totalCount > 0) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0))
                    ) {
                        if (easyPct > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(easyPct.toFloat().coerceAtLeast(1f))
                                    .fillMaxHeight()
                                    .background(easyColor)
                            )
                        }
                        if (mediumPct > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(mediumPct.toFloat().coerceAtLeast(1f))
                                    .fillMaxHeight()
                                    .background(mediumColor)
                            )
                        }
                        if (hardPct > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(hardPct.toFloat().coerceAtLeast(1f))
                                    .fillMaxHeight()
                                    .background(hardColor)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Stat Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(
                    label = "Easy",
                    value = "$easyCount ($easyPct%)",
                    bgColor = easyColor.copy(alpha = 0.15f),
                    textColor = easyColor,
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "Medium",
                    value = "$mediumCount ($mediumPct%)",
                    bgColor = mediumColor.copy(alpha = 0.15f),
                    textColor = mediumColor,
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "Hard",
                    value = "$hardCount ($hardPct%)",
                    bgColor = hardColor.copy(alpha = 0.15f),
                    textColor = hardColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Subject Pill Badges for Selected Date
            if (viewMode == "selected") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SubjectDifficultyPill(
                        subject = "Physics",
                        solved = currentPractice.physicsSolved,
                        difficulty = currentPractice.physicsDifficulty,
                        color = Color(0xFF60A5FA),
                        modifier = Modifier.weight(1f)
                    )
                    SubjectDifficultyPill(
                        subject = "Chemistry",
                        solved = currentPractice.chemistrySolved,
                        difficulty = currentPractice.chemistryDifficulty,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                    SubjectDifficultyPill(
                        subject = "Biology",
                        solved = currentPractice.biologySolved,
                        difficulty = currentPractice.biologyDifficulty,
                        color = Color(0xFF34D399),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // AI Guardian Mentor Diagnostic Tip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0x146366F1) else Color(0x0F6366F1),
                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🤖", fontSize = 16.sp)
                    Column {
                        Text(
                            text = "AI Guardian Difficulty Advice:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF818CF8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val advice = when {
                            totalCount == 0 -> "Log today's solved questions with their difficulty tier to unlock your rank diagnostic ratio!"
                            hardPct < 20 && totalCount >= 15 -> "⚠️ High-Yield Rank Alert: Tumhare Hard questions 20% se kam hain! NEET 680+ score ke liye daily kam se kam 25-30% multi-concept & tricky numericals zaroori hain."
                            easyPct > 60 && totalCount >= 15 -> "💡 Comfort Zone Alert: Sirf direct NCERT formula recall mat karo. Medium & Hard standard PYQs zyada solve karo."
                            hardPct >= 25 -> "🔥 Toppers Ratio! Tumhara Hard questions practice ratio strong hai. Is balanced momentum ko NEET tak continue rakho!"
                            else -> "✨ Balanced Practice: Easy se speed banti hai aur Medium se accuracy. Great consistency!"
                        }
                        Text(
                            text = advice,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = textColor.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectDifficultyPill(
    subject: String,
    solved: Int,
    difficulty: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val diffColor = when (difficulty.lowercase()) {
        "easy" -> Color(0xFF10B981)
        "hard" -> Color(0xFFEF4444)
        else -> Color(0xFFF59E0B)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = subject,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "$solved Qs",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(diffColor.copy(alpha = 0.2f))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = difficulty.take(3),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = diffColor
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                color = textColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                color = textColor,
                fontWeight = FontWeight.Black
            )
        }
    }
}
