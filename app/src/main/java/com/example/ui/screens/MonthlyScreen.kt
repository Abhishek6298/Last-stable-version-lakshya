package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyPractice
import com.example.data.MockTest
import com.example.data.StudyLog
import com.example.data.getMaxScore
import com.example.ui.AppViewModel
import com.example.ui.components.AppBrandingFooter
import com.example.ui.components.CapsuleChartDataPoint
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveAtmosphericCapsuleChart
import com.example.ui.components.MonthWeekPartition
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class HistoricalMonthEntry(
    val year: Int,
    val month: Int, // 0-based
    val fullLabel: String,
    val shortLabel: String,
    val startMs: Long,
    val endMs: Long,
    val isCurrent: Boolean,
    val testCount: Int,
    val questionsCount: Int,
    val studyHours: Float
)

@Composable
fun MonthlyScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val logs by viewModel.studyLogs.collectAsStateWithLifecycle()
    val tests by viewModel.mockTests.collectAsStateWithLifecycle()
    val dailyPractices by viewModel.dailyPractices.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDark = isDarkMode

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0x12FFFFFF) else Color.White
    val cardBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)

    val currentCal = remember { Calendar.getInstance() }
    val currentYear = remember { currentCal.get(Calendar.YEAR) }
    val currentMonth = remember { currentCal.get(Calendar.MONTH) }

    // Build exhaustive list of all historical months from data + past 12 months
    val allMonths = remember(logs, tests, dailyPractices, currentYear, currentMonth) {
        val set = mutableSetOf<Pair<Int, Int>>()
        for (i in 0..11) {
            val c = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth)
                add(Calendar.MONTH, -i)
            }
            set.add(c.get(Calendar.YEAR) to c.get(Calendar.MONTH))
        }
        tests.forEach {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            set.add(c.get(Calendar.YEAR) to c.get(Calendar.MONTH))
        }
        logs.forEach {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            set.add(c.get(Calendar.YEAR) to c.get(Calendar.MONTH))
        }
        dailyPractices.forEach { dp ->
            val p = dp.date.split("-")
            if (p.size >= 2) {
                val y = p[0].toIntOrNull()
                val m = p[1].toIntOrNull()?.minus(1)
                if (y != null && m != null) set.add(y to m)
            }
        }

        val fullSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val shortSdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        set.toList()
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
            .map { (y, m) ->
                val sCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val maxD = sCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val eCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, maxD)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val sMs = sCal.timeInMillis
                val eMs = eCal.timeInMillis

                val tCount = tests.count { it.timestamp in sMs..eMs }
                val lHours = logs.filter { it.timestamp in sMs..eMs }.sumOf { it.durationSeconds } / 3600f
                val mPrefix = String.format(Locale.getDefault(), "%04d-%02d", y, m + 1)
                val qCount = dailyPractices.filter { it.date.startsWith(mPrefix) }.sumOf { it.physicsSolved + it.chemistrySolved + it.biologySolved }

                HistoricalMonthEntry(
                    year = y,
                    month = m,
                    fullLabel = fullSdf.format(sCal.time),
                    shortLabel = shortSdf.format(sCal.time),
                    startMs = sMs,
                    endMs = eMs,
                    isCurrent = (y == currentYear && m == currentMonth),
                    testCount = tCount,
                    questionsCount = qCount,
                    studyHours = lHours
                )
            }
    }

    // Selected Month state
    var selectedMonthEntry by remember(allMonths) {
        mutableStateOf(allMonths.firstOrNull { it.isCurrent } ?: allMonths.first())
    }

    var testToDelete by remember { mutableStateOf<MockTest?>(null) }
    var practiceToDelete by remember { mutableStateOf<DailyPractice?>(null) }
    var logToDelete by remember { mutableStateOf<StudyLog?>(null) }
    var showClearMonthDialog by remember { mutableStateOf(false) }

    val selectedYear = selectedMonthEntry.year
    val selectedMonth = selectedMonthEntry.month
    val monthStartMs = selectedMonthEntry.startMs
    val monthEndMs = selectedMonthEntry.endMs
    val isPastMonth = !selectedMonthEntry.isCurrent

    // Month Navigation callbacks
    val currentIndex = allMonths.indexOfFirst { it.year == selectedYear && it.month == selectedMonth }
    val canGoPrev = currentIndex < allMonths.lastIndex
    val canGoNext = currentIndex > 0

    val onPrevMonth = {
        if (canGoPrev) {
            selectedMonthEntry = allMonths[currentIndex + 1]
        }
    }
    val onNextMonth = {
        if (canGoNext) {
            selectedMonthEntry = allMonths[currentIndex - 1]
        }
    }

    // Month Week Partitions
    val monthPartitions = remember(selectedYear, selectedMonth) {
        val sCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = sCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val mName = SimpleDateFormat("MMM", Locale.getDefault()).format(sCal.time)

        fun makePart(h: String, l: String, sDay: Int, eDay: Int): MonthWeekPartition {
            val partStart = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, sDay)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val partEnd = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, eDay)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            return MonthWeekPartition(h, l, sDay, eDay, partStart, partEnd)
        }

        val list = mutableListOf<MonthWeekPartition>()
        list.add(makePart("W1", "Week 1 (1-7 $mName)", 1, 7))
        list.add(makePart("W2", "Week 2 (8-14 $mName)", 8, 14))
        list.add(makePart("W3", "Week 3 (15-21 $mName)", 15, 21))
        list.add(makePart("W4", "Week 4 (22-28 $mName)", 22, 28))
        if (maxDays > 28) {
            list.add(makePart("W5", "Week 5 (29-$maxDays $mName)", 29, maxDays))
        }
        list
    }

    // Filtered data strictly for the selected month
    val monthTests = remember(tests, monthStartMs, monthEndMs) {
        tests.filter { it.timestamp in monthStartMs..monthEndMs }.sortedBy { it.timestamp }
    }
    val monthLogs = remember(logs, monthStartMs, monthEndMs) {
        logs.filter { it.timestamp in monthStartMs..monthEndMs }.sortedBy { it.timestamp }
    }
    val monthPrefix = String.format(Locale.getDefault(), "%04d-%02d", selectedYear, selectedMonth + 1)
    val monthPractices = remember(dailyPractices, monthPrefix) {
        dailyPractices.filter { it.date.startsWith(monthPrefix) }.sortedBy { it.date }
    }

    // Monthly Metrics
    val isJee = remember(monthTests) {
        monthTests.any { it.testName.contains("JEE", true) || it.testName.contains("IIT", true) || (it.geminiAnalysis ?: "").contains("JEE", true) }
    }
    val maxScore = if (isJee) 300f else 720f
    val maxSub = if (isJee) 100f else 180f
    val maxBio = if (isJee) 100f else 360f

    val totalQuestions = monthPractices.sumOf { it.physicsSolved + it.chemistrySolved + it.biologySolved }
    val totalStudyHours = monthLogs.sumOf { it.durationSeconds } / 3600f
    val avgMockScore = if (monthTests.isNotEmpty()) monthTests.map { it.score }.average().roundToInt() else 0
    val bestMockScore = if (monthTests.isNotEmpty()) monthTests.maxOf { it.score } else 0

    // Chart toggle: 0 = Mock Tests, 1 = Practice, 2 = Study Duration
    var activeChartTab by remember { mutableIntStateOf(0) }

    // Tab for records: 0 = Mock Tests, 1 = Daily Practice, 2 = Study Sessions
    var activeRecordTab by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP APP BAR WITH BACK NAVIGATION
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Monthly Analytics",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                        Text(
                            text = "Multi-month history, graphs & logs",
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                    }
                }

                if (isPastMonth) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable {
                                allMonths.firstOrNull { it.isCurrent }?.let { selectedMonthEntry = it }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Today, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(14.dp))
                            Text("Current", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                        }
                    }
                }
            }
        }

        // MONTH SELECTOR STRIP
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Month Stepper Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevMonth,
                            enabled = canGoPrev,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Month",
                                tint = if (canGoPrev) textColor else subTextColor.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = selectedMonthEntry.fullLabel,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor
                                )
                                if (selectedMonthEntry.isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Current", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Past Month", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                                    }
                                }
                            }
                            Text(
                                text = "${monthTests.size} Tests • $totalQuestions Qs • ${String.format(Locale.getDefault(), "%.1fh", totalStudyHours)} studied",
                                fontSize = 11.sp,
                                color = subTextColor
                            )
                        }

                        IconButton(
                            onClick = onNextMonth,
                            enabled = canGoNext,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Month",
                                tint = if (canGoNext) textColor else subTextColor.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Month Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        allMonths.forEach { entry ->
                            val isSelected = (entry.year == selectedYear && entry.month == selectedMonth)
                            val chipBg = if (isSelected) {
                                Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899))))
                            } else {
                                val c = if (entry.isCurrent) (if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0))
                                        else (if (isDark) Color(0x12FFFFFF) else Color(0xFFF1F5F9))
                                Modifier.background(c)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .then(chipBg)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.Transparent else (if (isDark) Color(0x1FFFFFFF) else Color(0xFFCBD5E1)),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedMonthEntry = entry }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = entry.shortLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = if (isSelected) Color.White else textColor
                                    )
                                    if ((entry.testCount > 0 || entry.questionsCount > 0 || entry.studyHours > 0) && !isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // MONTH SNAPSHOT KPI CARDS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Mock Tests Summary
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                                Text("Mock Tests", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subTextColor)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${monthTests.size} Tests",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor
                            )
                            Text(
                                text = if (monthTests.isNotEmpty()) "Avg: $avgMockScore • Best: $bestMockScore" else "No tests this month",
                                fontSize = 10.sp,
                                color = subTextColor
                            )
                        }
                    }

                    // Questions Solved Summary
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Text("Questions", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subTextColor)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$totalQuestions Qs",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor
                            )
                            val phyQ = monthPractices.sumOf { it.physicsSolved }
                            val chemQ = monthPractices.sumOf { it.chemistrySolved }
                            val bioQ = monthPractices.sumOf { it.biologySolved }
                            Text(
                                text = "P: $phyQ • C: $chemQ • B: $bioQ",
                                fontSize = 10.sp,
                                color = subTextColor
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Study Hours Summary
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(16.dp))
                                Text("Study Time", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subTextColor)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f hrs", totalStudyHours),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor
                            )
                            val daysCount = monthPractices.size.coerceAtLeast(1)
                            Text(
                                text = String.format(Locale.getDefault(), "%.1fh / day active", totalStudyHours / daysCount),
                                fontSize = 10.sp,
                                color = subTextColor
                            )
                        }
                    }

                    // Quality / Preparation Rating
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                Text("Preparation Pace", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subTextColor)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val paceRating = when {
                                totalStudyHours >= 40f || totalQuestions >= 400 -> "Top Gear 🚀"
                                totalStudyHours >= 20f || totalQuestions >= 150 -> "On Track ⚡"
                                totalStudyHours > 0f || totalQuestions > 0 -> "Building 📈"
                                else -> "No Activity 🎯"
                            }
                            Text(
                                text = paceRating,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor
                            )
                            Text(
                                text = if (isPastMonth) "Historical Record" else "Active Progress",
                                fontSize = 10.sp,
                                color = subTextColor
                            )
                        }
                    }
                }
            }
        }

        // CHART SELECTOR TABS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Mock Tests", "Practice Qs", "Study Hours").forEachIndexed { index, tabTitle ->
                    val isSelected = (activeChartTab == index)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) (if (isDark) Color(0xFF334155) else Color.White) else Color.Transparent)
                            .clickable { activeChartTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabTitle,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isSelected) textColor else subTextColor
                        )
                    }
                }
            }
        }

        // INTERACTIVE ATMOSPHERIC GRAPH FOR SELECTED MONTH
        item {
            when (activeChartTab) {
                0 -> {
                    // Mock Tests Progression Chart for this month
                    val avg = if (monthTests.isNotEmpty()) monthTests.map { it.score }.average().toFloat() else (if (isJee) 180f else 450f)
                    val points = monthPartitions.mapIndexed { idx, part ->
                        val inWeek = monthTests.filter { it.timestamp in part.startMs..part.endMs }
                        val score = if (inWeek.isNotEmpty()) inWeek.map { it.score }.average().roundToInt() else 0
                        val avgP = if (inWeek.isNotEmpty()) (inWeek.map { it.physics }.average() / maxSub * 100).toFloat() else 0f
                        val avgC = if (inWeek.isNotEmpty()) (inWeek.map { it.chemistry }.average() / maxSub * 100).toFloat() else 0f
                        val avgB = if (inWeek.isNotEmpty()) (inWeek.map { it.biology }.average() / maxBio * 100).toFloat() else 0f
                        CapsuleChartDataPoint(
                            id = "monthly_test_$idx",
                            dayLetter = part.header,
                            fullDate = "${part.fullLabel} • ${if (inWeek.isNotEmpty()) "${inWeek.size} Tests" else "No Tests"}",
                            value = score.toFloat(),
                            displayValue = if (score > 0) "$score/${maxScore.toInt()}" else "0/${maxScore.toInt()}",
                            isAboveAverage = score >= avg && score > 0,
                            detailNotes = if (inWeek.isNotEmpty()) "${inWeek.size} tests taken • Avg: $score/${maxScore.toInt()}" else "No tests recorded in this week",
                            subMetric1 = avgP,
                            subMetric2 = avgC,
                            subMetric3 = avgB
                        )
                    }
                    val avgP = if (monthTests.isNotEmpty()) (monthTests.map { it.physics }.average() / maxSub * 100).toInt().coerceIn(0, 100) else 0
                    val avgC = if (monthTests.isNotEmpty()) (monthTests.map { it.chemistry }.average() / maxSub * 100).toInt().coerceIn(0, 100) else 0
                    val avgB = if (monthTests.isNotEmpty()) (monthTests.map { it.biology }.average() / maxBio * 100).toInt().coerceIn(0, 100) else 0

                    InteractiveAtmosphericCapsuleChart(
                        title = "Mock Test Progression",
                        subtitle = "Weekly scores in ${selectedMonthEntry.fullLabel}",
                        selectedPeriod = "Month",
                        onPeriodSelected = {},
                        dataPoints = points,
                        averageDisplay = if (monthTests.isNotEmpty()) "$avgMockScore / ${maxScore.toInt()}" else "0 / ${maxScore.toInt()}",
                        qualityScore = if (monthTests.isNotEmpty()) "${(avgMockScore / maxScore * 100).roundToInt()}%" else "0%",
                        qualityRating = if (monthTests.isNotEmpty() && avgMockScore >= (if (isJee) 200 else 550)) "Target Zone 🏆" else "In Progress ⚡",
                        gauge1Label = "Physics",
                        gauge1Percent = avgP,
                        gauge2Label = "Chemistry",
                        gauge2Percent = avgC,
                        gauge3Label = if (isJee) "Maths" else "Biology",
                        gauge3Percent = avgB,
                        maxValue = maxScore,
                        topGuideLabel = "${maxScore.toInt()} M",
                        bottomGuideLabel = "0 M",
                        isDark = isDark,
                        monthLabel = selectedMonthEntry.fullLabel,
                        isPastMonth = isPastMonth,
                        canGoPrevMonth = canGoPrev,
                        canGoNextMonth = canGoNext,
                        onPrevMonth = onPrevMonth,
                        onNextMonth = onNextMonth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                1 -> {
                    // Practice Questions Chart for this month
                    val weekTotals = mutableListOf<Int>()
                    val points = monthPartitions.mapIndexed { idx, part ->
                        var p = 0
                        var c = 0
                        var b = 0
                        for (d in part.startDay..part.endDay) {
                            val key = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, d)
                            val dp = dailyPractices.find { it.date == key }
                            p += dp?.physicsSolved ?: 0
                            c += dp?.chemistrySolved ?: 0
                            b += dp?.biologySolved ?: 0
                        }
                        val tot = p + c + b
                        weekTotals.add(tot)
                        CapsuleChartDataPoint(
                            id = "monthly_prac_$idx",
                            dayLetter = part.header,
                            fullDate = "${part.fullLabel} • $tot Qs Solved",
                            value = tot.toFloat(),
                            displayValue = "$tot Qs",
                            isAboveAverage = tot > 0,
                            subMetric1 = if (tot > 0) (p.toFloat() / tot * 100f) else 0f,
                            subMetric2 = if (tot > 0) (c.toFloat() / tot * 100f) else 0f,
                            subMetric3 = if (tot > 0) (b.toFloat() / tot * 100f) else 0f
                        )
                    }
                    val maxPractice = points.maxOfOrNull { it.value }?.coerceAtLeast(30f) ?: 100f
                    val totP = monthPractices.sumOf { it.physicsSolved }
                    val totC = monthPractices.sumOf { it.chemistrySolved }
                    val totB = monthPractices.sumOf { it.biologySolved }
                    val totAll = totalQuestions.coerceAtLeast(1)

                    InteractiveAtmosphericCapsuleChart(
                        title = "Practice History",
                        subtitle = "Weekly question solving in ${selectedMonthEntry.fullLabel}",
                        selectedPeriod = "Month",
                        onPeriodSelected = {},
                        dataPoints = points,
                        averageDisplay = "$totalQuestions Qs",
                        qualityScore = "$totalQuestions Total",
                        qualityRating = if (totalQuestions >= 200) "Top Volume 🚀" else "Building 🎯",
                        gauge1Label = "Physics",
                        gauge1Percent = (totP * 100 / totAll).coerceIn(0, 100),
                        gauge2Label = "Chemistry",
                        gauge2Percent = (totC * 100 / totAll).coerceIn(0, 100),
                        gauge3Label = if (isJee) "Maths" else "Biology",
                        gauge3Percent = (totB * 100 / totAll).coerceIn(0, 100),
                        maxValue = maxPractice,
                        topGuideLabel = "${maxPractice.toInt()} Q",
                        bottomGuideLabel = "0 Q",
                        isDark = isDark,
                        monthLabel = selectedMonthEntry.fullLabel,
                        isPastMonth = isPastMonth,
                        canGoPrevMonth = canGoPrev,
                        canGoNextMonth = canGoNext,
                        onPrevMonth = onPrevMonth,
                        onNextMonth = onNextMonth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                else -> {
                    // Study Hours Chart for this month
                    val points = monthPartitions.mapIndexed { idx, part ->
                        val weekLogs = monthLogs.filter { it.timestamp in part.startMs..part.endMs }
                        val phyH = weekLogs.filter { it.subject.equals("Physics", ignoreCase = true) }.sumOf { it.durationSeconds } / 3600f
                        val chemH = weekLogs.filter { it.subject.equals("Chemistry", ignoreCase = true) }.sumOf { it.durationSeconds } / 3600f
                        val bioH = weekLogs.filter { it.subject.equals("Biology", ignoreCase = true) || it.subject.equals("Botany", ignoreCase = true) || it.subject.equals("Zoology", ignoreCase = true) }.sumOf { it.durationSeconds } / 3600f
                        val totalH = phyH + chemH + bioH
                        CapsuleChartDataPoint(
                            id = "monthly_study_$idx",
                            dayLetter = part.header,
                            fullDate = "${part.fullLabel} • ${String.format(Locale.getDefault(), "%.1fh", totalH)}",
                            value = totalH,
                            displayValue = String.format(Locale.getDefault(), "%.1fh", totalH),
                            isAboveAverage = totalH > 0,
                            subMetric1 = if (totalH > 0) (phyH / totalH * 100f) else 0f,
                            subMetric2 = if (totalH > 0) (chemH / totalH * 100f) else 0f,
                            subMetric3 = if (totalH > 0) (bioH / totalH * 100f) else 0f
                        )
                    }
                    val maxH = points.maxOfOrNull { it.value }?.coerceAtLeast(4f) ?: 10f
                    val totSec = monthLogs.sumOf { it.durationSeconds }.coerceAtLeast(1)
                    val phySec = monthLogs.filter { it.subject.equals("Physics", ignoreCase = true) }.sumOf { it.durationSeconds }
                    val chemSec = monthLogs.filter { it.subject.equals("Chemistry", ignoreCase = true) }.sumOf { it.durationSeconds }
                    val bioSec = monthLogs.filter { it.subject.equals("Biology", ignoreCase = true) || it.subject.equals("Botany", ignoreCase = true) || it.subject.equals("Zoology", ignoreCase = true) }.sumOf { it.durationSeconds }

                    InteractiveAtmosphericCapsuleChart(
                        title = "Study Duration",
                        subtitle = "Weekly study hours in ${selectedMonthEntry.fullLabel}",
                        selectedPeriod = "Month",
                        onPeriodSelected = {},
                        dataPoints = points,
                        averageDisplay = String.format(Locale.getDefault(), "%.1fh Total", totalStudyHours),
                        qualityScore = String.format(Locale.getDefault(), "%.1fh", totalStudyHours),
                        qualityRating = if (totalStudyHours >= 30f) "Deep Focus ⚡" else "In Progress 🎯",
                        gauge1Label = "Physics",
                        gauge1Percent = ((phySec.toDouble() / totSec) * 100).toInt().coerceIn(0, 100),
                        gauge2Label = "Chemistry",
                        gauge2Percent = ((chemSec.toDouble() / totSec) * 100).toInt().coerceIn(0, 100),
                        gauge3Label = if (isJee) "Maths" else "Biology",
                        gauge3Percent = ((bioSec.toDouble() / totSec) * 100).toInt().coerceIn(0, 100),
                        maxValue = maxH,
                        topGuideLabel = String.format(Locale.getDefault(), "%.0fh", maxH),
                        bottomGuideLabel = "0h",
                        isDark = isDark,
                        monthLabel = selectedMonthEntry.fullLabel,
                        isPastMonth = isPastMonth,
                        canGoPrevMonth = canGoPrev,
                        canGoNextMonth = canGoNext,
                        onPrevMonth = onPrevMonth,
                        onNextMonth = onNextMonth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // DETAILED RECORDS ACCORDION / TABS
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity Log (${selectedMonthEntry.shortLabel})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor
                    )

                    val hasAnyMonthItems = monthTests.isNotEmpty() || monthPractices.isNotEmpty() || monthLogs.isNotEmpty()
                    if (hasAnyMonthItems) {
                        TextButton(
                            onClick = { showClearMonthDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Month", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Month", color = Color(0xFFEF4444), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        "Mock Tests (${monthTests.size})",
                        "Daily Practice (${monthPractices.size})",
                        "Study Logs (${monthLogs.size})"
                    ).forEachIndexed { idx, tabTitle ->
                        val isSelected = (activeRecordTab == idx)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) (if (isDark) Color(0xFF334155) else Color.White) else Color.Transparent)
                                .clickable { activeRecordTab = idx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabTitle,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) textColor else subTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // RECORD ITEMS LIST
        when (activeRecordTab) {
            0 -> {
                // Mock Tests List for this month
                if (monthTests.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = subTextColor, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No mock tests logged in ${selectedMonthEntry.fullLabel}", color = subTextColor, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(monthTests.reversed(), key = { it.id }) { test ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            val itemIsJee = test.testName.contains("JEE", true) || test.testName.contains("IIT", true) || (test.geminiAnalysis ?: "").contains("JEE", true)
                            val itemMax = test.getMaxScore(itemIsJee)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = test.testName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(
                                        text = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date(test.timestamp)),
                                        fontSize = 11.sp,
                                        color = subTextColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Phy: ${test.physics} • Chem: ${test.chemistry} • ${if (itemIsJee) "Math" else "Bio"}: ${test.biology}${if (test.negative > 0) " • -${test.negative} Neg" else ""}",
                                        fontSize = 11.sp,
                                        color = subTextColor
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF6366F1).copy(alpha = 0.15f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${test.score}/$itemMax",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF818CF8)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { testToDelete = test },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Test",
                                            tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Daily Practice List for this month
                if (monthPractices.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = subTextColor, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No practice questions recorded in ${selectedMonthEntry.fullLabel}", color = subTextColor, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(monthPractices.reversed(), key = { it.date }) { dp ->
                        val tot = dp.physicsSolved + dp.chemistrySolved + dp.biologySolved
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dp.date,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(
                                        text = "Physics: ${dp.physicsSolved} • Chemistry: ${dp.chemistrySolved} • Biology: ${dp.biologySolved}",
                                        fontSize = 11.sp,
                                        color = subTextColor
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "$tot Qs",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { practiceToDelete = dp },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Practice",
                                            tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Study Logs List for this month
                if (monthLogs.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = subTextColor, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No study logs recorded in ${selectedMonthEntry.fullLabel}", color = subTextColor, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(monthLogs.reversed(), key = { it.id }) { log ->
                        val hours = log.durationSeconds / 3600f
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.subject,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    if (log.chapter.isNotBlank()) {
                                        Text(
                                            text = log.chapter,
                                            fontSize = 12.sp,
                                            color = textColor.copy(alpha = 0.85f)
                                        )
                                    }
                                    Text(
                                        text = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault()).format(Date(log.timestamp)),
                                        fontSize = 10.sp,
                                        color = subTextColor
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFEC4899).copy(alpha = 0.15f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.1fh", hours),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFEC4899)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { logToDelete = log },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Log",
                                            tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            AppBrandingFooter(isDark = isDark)
        }
    }

    // Delete Mock Test Confirmation Dialog
    testToDelete?.let { test ->
        AlertDialog(
            onDismissRequest = { testToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("Delete Mock Test?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${test.testName}' (${test.score} marks)? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMockTest(test.id)
                        testToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { testToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Daily Practice Confirmation Dialog
    practiceToDelete?.let { dp ->
        val tot = dp.physicsSolved + dp.chemistrySolved + dp.biologySolved
        AlertDialog(
            onDismissRequest = { practiceToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("Delete Practice Log?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete daily practice record for ${dp.date} ($tot questions)?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDailyPractice(dp.date)
                        practiceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { practiceToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Study Log Confirmation Dialog
    logToDelete?.let { log ->
        val hours = log.durationSeconds / 3600f
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("Delete Study Log?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this ${log.subject} log (${String.format(Locale.getDefault(), "%.1fh", hours)})?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudyLog(log.id)
                        logToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Month Activity Logs Confirmation Dialog
    if (showClearMonthDialog) {
        AlertDialog(
            onDismissRequest = { showClearMonthDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("Clear ${selectedMonthEntry.fullLabel} Logs?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all ${monthTests.size} mock tests, ${monthPractices.size} practice entries, and ${monthLogs.size} study logs for ${selectedMonthEntry.fullLabel}.") },
            confirmButton = {
                Button(
                    onClick = {
                        monthTests.forEach { viewModel.deleteMockTest(it.id) }
                        monthPractices.forEach { viewModel.deleteDailyPractice(it.date) }
                        monthLogs.forEach { viewModel.deleteStudyLog(it.id) }
                        showClearMonthDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearMonthDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
