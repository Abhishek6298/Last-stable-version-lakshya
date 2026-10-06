package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DailyPractice
import com.example.data.MockTest
import com.example.data.StudyLog
import com.example.data.TestSubjectCategory
import com.example.data.getSubjectCategory
import com.example.data.getSubjectScore
import com.example.data.isSingleSubjectTest
import com.example.data.getMaxScore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

data class MonthWeekPartition(
    val header: String,
    val fullLabel: String,
    val startDay: Int,
    val endDay: Int,
    val startMs: Long,
    val endMs: Long
)

data class AvailableMonthOption(
    val year: Int,
    val month: Int,
    val fullLabel: String,
    val shortLabel: String,
    val isCurrent: Boolean,
    val hasData: Boolean
)

@Composable
fun DashboardChartsSection(
    tests: List<MockTest>,
    dailyPractices: List<DailyPractice>,
    todayPractice: DailyPractice?,
    logs: List<StudyLog>,
    isDark: Boolean,
    onNavigate: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Current live calendar values
    val currentCal = remember { Calendar.getInstance() }
    val currentYear = remember { currentCal.get(Calendar.YEAR) }
    val currentMonth = remember { currentCal.get(Calendar.MONTH) } // 0-indexed

    // Selected Month & Year states for historical review
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var showMonthPickerDialog by remember { mutableStateOf(false) }

    // Chart period filter states ("Day", "Week", "Month")
    var mockTimeFilter by remember { mutableStateOf("Week") }
    var practiceTimeFilter by remember { mutableStateOf("Week") }
    var studyTimeFilter by remember { mutableStateOf("Week") }

    val isCurrentMonth = (selectedYear == currentYear && selectedMonth == currentMonth)
    val isPastMonth = (selectedYear < currentYear || (selectedYear == currentYear && selectedMonth < currentMonth))
    val canGoNextMonth = !isCurrentMonth

    val onPrevMonth = {
        if (selectedMonth == 0) {
            selectedYear -= 1
            selectedMonth = 11
        } else {
            selectedMonth -= 1
        }
    }

    val onNextMonth = {
        if (canGoNextMonth) {
            if (selectedMonth == 11) {
                selectedYear += 1
                selectedMonth = 0
            } else {
                selectedMonth += 1
            }
        }
    }

    val onResetToCurrentMonth = {
        selectedYear = currentYear
        selectedMonth = currentMonth
    }

    // Colors
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0x12FFFFFF) else Color.White
    val cardBorder = if (isDark) Color(0x1FFFFFFF) else Color(0xFFE2E8F0)

    // Compute active month metadata and week partitions
    val (monthStartMs, monthEndMs, monthFullLabel, monthPartitions) = remember(selectedYear, selectedMonth) {
        val sCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val maxDays = sCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val eCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, maxDays)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val fullLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(sCal.time)
        val mName = SimpleDateFormat("MMM", Locale.getDefault()).format(sCal.time)

        fun makePartition(header: String, label: String, sDay: Int, eDay: Int): MonthWeekPartition {
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
            return MonthWeekPartition(header, label, sDay, eDay, partStart, partEnd)
        }

        val parts = mutableListOf<MonthWeekPartition>()
        parts.add(makePartition("W1", "Week 1 (1-7 $mName)", 1, 7))
        parts.add(makePartition("W2", "Week 2 (8-14 $mName)", 8, 14))
        parts.add(makePartition("W3", "Week 3 (15-21 $mName)", 15, 21))
        parts.add(makePartition("W4", "Week 4 (22-28 $mName)", 22, 28))
        if (maxDays > 28) {
            parts.add(makePartition("W5", "Week 5 (29-$maxDays $mName)", 29, maxDays))
        }
        Quadruple(sCal.timeInMillis, eCal.timeInMillis, fullLabel, parts)
    }

    // Collect available months list (past 8 months + any month with data)
    val availableMonthOptions = remember(tests, logs, dailyPractices, currentYear, currentMonth) {
        val set = mutableSetOf<Pair<Int, Int>>()
        // Add past 8 consecutive months
        for (i in 0..7) {
            val c = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth)
                add(Calendar.MONTH, -i)
            }
            set.add(c.get(Calendar.YEAR) to c.get(Calendar.MONTH))
        }
        // Add any month with mock tests
        tests.forEach {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            set.add(c.get(Calendar.YEAR) to c.get(Calendar.MONTH))
        }
        // Add any month with study logs
        logs.forEach {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            set.add(c.get(Calendar.YEAR) to c.get(Calendar.MONTH))
        }
        // Add any month with daily practice
        dailyPractices.forEach { dp ->
            val parts = dp.date.split("-")
            if (parts.size >= 2) {
                val y = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()?.minus(1)
                if (y != null && m != null) {
                    set.add(y to m)
                }
            }
        }

        val sdfFull = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val sdfShort = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        set.toList()
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
            .map { (y, m) ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val isCurr = (y == currentYear && m == currentMonth)
                // Check if this month has recorded activity
                val mStart = cal.timeInMillis
                val maxD = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val mEnd = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, maxD)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis
                val hasT = tests.any { it.timestamp in mStart..mEnd }
                val hasL = logs.any { it.timestamp in mStart..mEnd }
                val mPrefix = String.format(Locale.getDefault(), "%04d-%02d", y, m + 1)
                val hasP = dailyPractices.any { it.date.startsWith(mPrefix) }
                AvailableMonthOption(
                    year = y,
                    month = m,
                    fullLabel = sdfFull.format(cal.time),
                    shortLabel = sdfShort.format(cal.time),
                    isCurrent = isCurr,
                    hasData = hasT || hasL || hasP
                )
            }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val sdfDate = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
        val sdfKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
        val sdfDay = remember { SimpleDateFormat("EEE", Locale.getDefault()) }

        val isJeeExam = remember(tests) {
            tests.any { it.testName.contains("JEE", true) || it.testName.contains("IIT", true) || (it.geminiAnalysis ?: "").contains("JEE", true) }
        }
        val maxTotal = if (isJeeExam) 300f else 720f
        val maxSub = if (isJeeExam) 100f else 180f
        val maxBioMath = if (isJeeExam) 100f else 360f
        val thirdSubjectName = if (isJeeExam) "Maths" else "Biology"

        // =========================================================================
        // HISTORICAL MONTH EXPLORER HEADER CARD
        // =========================================================================
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
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
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Historical Months Explorer",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor
                            )
                            Text(
                                text = "Select any past month to inspect charts & stats",
                                fontSize = 11.sp,
                                color = subTextColor
                            )
                        }
                    }

                    if (onNavigate != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF6366F1).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .clickable { onNavigate("monthly") }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Full Report",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8)
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal scrollable Month Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    availableMonthOptions.forEach { opt ->
                        val isSelected = (opt.year == selectedYear && opt.month == selectedMonth)
                        val chipBg = if (isSelected) {
                            Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899))))
                        } else {
                            val c = if (opt.isCurrent) (if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0))
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
                                .clickable {
                                    selectedYear = opt.year
                                    selectedMonth = opt.month
                                    // Also set chart filters to Month view so user sees this month's graphs right away
                                    mockTimeFilter = "Month"
                                    practiceTimeFilter = "Month"
                                    studyTimeFilter = "Month"
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = if (opt.isCurrent) "${opt.shortLabel} (Now)" else opt.shortLabel,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else textColor
                                )
                                if (opt.hasData && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                }
                            }
                        }
                    }

                    // "More Months..." Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color(0x12FFFFFF) else Color(0xFFF1F5F9))
                            .border(1.dp, if (isDark) Color(0x25FFFFFF) else Color(0xFFCBD5E1), RoundedCornerShape(20.dp))
                            .clickable { showMonthPickerDialog = true }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Pick Month",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8)
                            )
                        }
                    }
                }

                // Active Month Status Indicator & Return button
                if (isPastMonth) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Viewing Past Month: $monthFullLabel",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }

                        TextButton(
                            onClick = onResetToCurrentMonth,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Current Month ➔",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // GRAPH 1: Full Mock Test Performance (720M / 300M)
        // Strictly isolated: single-subject tests do NOT appear in this graph!
        // =========================================================================
        val fullMockTests = remember(tests) {
            tests.filter { !it.isSingleSubjectTest() }
        }

        val activePeriodTests = remember(fullMockTests, mockTimeFilter, selectedYear, selectedMonth, monthStartMs, monthEndMs) {
            val nowMs = System.currentTimeMillis()
            when (mockTimeFilter) {
                "Day" -> fullMockTests.sortedBy { it.timestamp }.takeLast(7)
                "Week" -> {
                    val inWeek = fullMockTests.filter { (nowMs - it.timestamp) <= 7L * 24 * 3600 * 1000L }
                    if (inWeek.isNotEmpty()) inWeek else fullMockTests.sortedBy { it.timestamp }.takeLast(7)
                }
                else -> { // "Month" - Filter tests strictly within the selected month!
                    fullMockTests.filter { it.timestamp in monthStartMs..monthEndMs }.sortedBy { it.timestamp }
                }
            }
        }

        val mockChartPoints = remember(fullMockTests, mockTimeFilter, isJeeExam, selectedYear, selectedMonth, monthPartitions) {
            when (mockTimeFilter) {
                "Day" -> {
                    val recent = fullMockTests.sortedBy { it.timestamp }.takeLast(7)
                    if (recent.isEmpty()) {
                        listOf("T1", "T2", "T3", "T4", "T5").mapIndexed { idx, name ->
                            CapsuleChartDataPoint(
                                id = "mock_day_empty_$idx",
                                dayLetter = name,
                                fullDate = "Full Mock ${idx + 1}",
                                value = 0f,
                                displayValue = "No Test",
                                isAboveAverage = false,
                                detailNotes = "No full-length mock test logged yet"
                            )
                        }
                    } else {
                        val avg = recent.map { it.score }.average().toFloat().coerceAtLeast(1f)
                        recent.mapIndexed { idx, t ->
                            val tIsJee = t.testName.contains("JEE", true) || t.testName.contains("IIT", true) || (t.geminiAnalysis ?: "").contains("JEE", true)
                            val tMax = t.getMaxScore(tIsJee)
                            val tSubMax = if (tIsJee) 100f else 180f
                            val tBioMax = if (tIsJee) 100f else 360f
                            CapsuleChartDataPoint(
                                id = "mock_t_${t.id}_$idx",
                                dayLetter = "T${idx + 1}",
                                fullDate = "${t.testName} (${sdfDate.format(Date(t.timestamp))})",
                                value = t.score.toFloat(),
                                displayValue = "${t.score}/$tMax",
                                isAboveAverage = t.score >= avg,
                                detailNotes = "Phy: ${t.physics}, Chem: ${t.chemistry}, ${if (tIsJee) "Math" else "Bio"}: ${t.biology}${if (t.negative > 0) " • -${t.negative} Neg" else ""}",
                                subMetric1 = if (t.physics > 0) (t.physics.toFloat() / tSubMax * 100f).coerceIn(0f, 100f) else 0f,
                                subMetric2 = if (t.chemistry > 0) (t.chemistry.toFloat() / tSubMax * 100f).coerceIn(0f, 100f) else 0f,
                                subMetric3 = if (t.biology > 0) (t.biology.toFloat() / tBioMax * 100f).coerceIn(0f, 100f) else 0f
                            )
                        }
                    }
                }
                "Week" -> {
                    val points = mutableListOf<CapsuleChartDataPoint>()
                    val nowMs = System.currentTimeMillis()
                    val weekTests = fullMockTests.filter { (nowMs - it.timestamp) <= 7L * 24 * 3600 * 1000L }
                    val avg = if (weekTests.isNotEmpty()) weekTests.map { it.score }.average().toFloat() else (if (isJeeExam) 180f else 450f)
                    for (i in 6 downTo 0) {
                        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                        val dayKey = sdfKey.format(c.time)
                        val dayLabel = sdfDay.format(c.time).take(3)
                        val dateLabel = sdfDate.format(c.time)
                        val dayTest = fullMockTests.filter { sdfKey.format(Date(it.timestamp)) == dayKey }.maxByOrNull { it.score }
                        val score = dayTest?.score ?: 0
                        val tIsJee = dayTest?.let { it.testName.contains("JEE", true) || it.testName.contains("IIT", true) || (it.geminiAnalysis ?: "").contains("JEE", true) } ?: isJeeExam
                        val tMax = dayTest?.getMaxScore(tIsJee) ?: (if (tIsJee) 300 else 720)
                        val tSubMax = if (tIsJee) 100f else 180f
                        val tBioMax = if (tIsJee) 100f else 360f
                        points.add(
                            CapsuleChartDataPoint(
                                id = "mock_week_$dayKey",
                                dayLetter = dayLabel,
                                fullDate = if (dayTest != null) "${dayTest.testName} • ${sdfDate.format(Date(dayTest.timestamp))}" else "$dayLabel, $dateLabel • No Test",
                                value = score.toFloat(),
                                displayValue = if (dayTest != null) "$score/$tMax" else "0/$tMax",
                                isAboveAverage = score >= avg && score > 0,
                                detailNotes = if (dayTest != null) "Phy: ${dayTest.physics}, Chem: ${dayTest.chemistry}, ${if (tIsJee) "Math" else "Bio"}: ${dayTest.biology}" else "No full mock on $dateLabel",
                                subMetric1 = if (dayTest != null && dayTest.physics > 0) (dayTest.physics.toFloat() / tSubMax * 100f).coerceIn(0f, 100f) else 0f,
                                subMetric2 = if (dayTest != null && dayTest.chemistry > 0) (dayTest.chemistry.toFloat() / tSubMax * 100f).coerceIn(0f, 100f) else 0f,
                                subMetric3 = if (dayTest != null && dayTest.biology > 0) (dayTest.biology.toFloat() / tBioMax * 100f).coerceIn(0f, 100f) else 0f
                            )
                        )
                    }
                    points
                }
                else -> { // "Month" - Accurately partition the selected month into weeks
                    val testsInMonth = fullMockTests.filter { it.timestamp in monthStartMs..monthEndMs }
                    val avg = if (testsInMonth.isNotEmpty()) testsInMonth.map { it.score }.average().toFloat() else (if (isJeeExam) 180f else 450f)
                    monthPartitions.mapIndexed { idx, part ->
                        val inWeek = testsInMonth.filter { it.timestamp in part.startMs..part.endMs }
                        val score = if (inWeek.isNotEmpty()) inWeek.map { it.score }.average().roundToInt() else 0
                        val avgPhy = if (inWeek.isNotEmpty()) (inWeek.map { it.physics }.average() / maxSub * 100).toFloat() else 0f
                        val avgChem = if (inWeek.isNotEmpty()) (inWeek.map { it.chemistry }.average() / maxSub * 100).toFloat() else 0f
                        val avgBio = if (inWeek.isNotEmpty()) (inWeek.map { it.biology }.average() / maxBioMath * 100).toFloat() else 0f
                        CapsuleChartDataPoint(
                            id = "mock_month_${selectedYear}_${selectedMonth}_$idx",
                            dayLetter = part.header,
                            fullDate = "${part.fullLabel} • ${if (inWeek.isNotEmpty()) "${inWeek.size} Full Tests" else "No Tests"}",
                            value = score.toFloat(),
                            displayValue = if (score > 0) "$score/${maxTotal.toInt()}" else "0/${maxTotal.toInt()}",
                            isAboveAverage = score >= avg && score > 0,
                            detailNotes = if (inWeek.isNotEmpty()) "${inWeek.size} tests • Avg: $score/${maxTotal.toInt()}" else "No full mock tests recorded in this week of $monthFullLabel",
                            subMetric1 = avgPhy,
                            subMetric2 = avgChem,
                            subMetric3 = avgBio
                        )
                    }
                }
            }
        }

        val avgMockScore = remember(activePeriodTests, isJeeExam, maxTotal) {
            if (activePeriodTests.isNotEmpty()) {
                "${(activePeriodTests.map { it.score }.average()).roundToInt()} / ${maxTotal.toInt()}"
            } else "0 / ${maxTotal.toInt()}"
        }
        val avgPhysicsPct = remember(activePeriodTests, maxSub) {
            if (activePeriodTests.isNotEmpty()) (activePeriodTests.map { it.physics }.average() / maxSub * 100).toInt().coerceIn(0, 100) else 0
        }
        val avgChemPct = remember(activePeriodTests, maxSub) {
            if (activePeriodTests.isNotEmpty()) (activePeriodTests.map { it.chemistry }.average() / maxSub * 100).toInt().coerceIn(0, 100) else 0
        }
        val avgBioPct = remember(activePeriodTests, maxBioMath) {
            if (activePeriodTests.isNotEmpty()) (activePeriodTests.map { it.biology }.average() / maxBioMath * 100).toInt().coerceIn(0, 100) else 0
        }

        val qualityRating = remember(activePeriodTests, maxTotal) {
            if (activePeriodTests.isEmpty()) "Awaiting Tests 🎯"
            else {
                val avgPct = (activePeriodTests.map { it.score }.average() / maxTotal) * 100
                when {
                    avgPct >= 80 -> "AIIMS / Top Tier 🏆"
                    avgPct >= 65 -> "Target Zone 🎯"
                    avgPct >= 50 -> "Good Progress 📈"
                    else -> "Foundation ⚡"
                }
            }
        }

        InteractiveAtmosphericCapsuleChart(
            title = if (mockTimeFilter == "Month") "Full Mock Performance (${maxTotal.toInt()}M) • $monthFullLabel" else "Full Mock Performance (${maxTotal.toInt()}M)",
            subtitle = if (mockTimeFilter == "Month") {
                if (isPastMonth) "Showing ${maxTotal.toInt()}M full mock tests for $monthFullLabel • Touch to inspect"
                else "Showing ${maxTotal.toInt()}M full mock tests ($monthFullLabel) • Touch to inspect"
            } else "NEET Full Length Mocks (${maxTotal.toInt()} Marks) • Touch any bar to inspect",
            selectedPeriod = if (mockTimeFilter in listOf("Day", "Week", "Month")) mockTimeFilter else "Week",
            onPeriodSelected = { mockTimeFilter = it },
            dataPoints = mockChartPoints,
            averageDisplay = avgMockScore,
            qualityScore = if (activePeriodTests.isNotEmpty()) "${((activePeriodTests.map { it.score }.average() / maxTotal) * 100).roundToInt()}%" else "0%",
            qualityRating = qualityRating,
            gauge1Label = "Physics",
            gauge1Percent = avgPhysicsPct,
            gauge2Label = "Chemistry",
            gauge2Percent = avgChemPct,
            gauge3Label = thirdSubjectName,
            gauge3Percent = avgBioPct,
            maxValue = maxTotal,
            topGuideLabel = "${maxTotal.toInt()} M",
            bottomGuideLabel = "0 M",
            isDark = isDark,
            monthLabel = monthFullLabel,
            isPastMonth = isPastMonth,
            canGoNextMonth = canGoNextMonth,
            canGoPrevMonth = true,
            onPrevMonth = onPrevMonth,
            onNextMonth = onNextMonth,
            onMonthClick = { showMonthPickerDialog = true },
            onResetToCurrentMonth = onResetToCurrentMonth,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )

        // =========================================================================
        // SUBJECT PERFORMANCE GRAPHS: 1. Physics, 2. Chemistry, 3. Biology
        // Added as requested: Dedicated graphs for single & sectional subject marks
        // =========================================================================
        var selectedSubjectGraphTab by remember { mutableStateOf(0) }
        var phyTimeFilter by remember { mutableStateOf("Week") }
        var chemTimeFilter by remember { mutableStateOf("Week") }
        var bioTimeFilter by remember { mutableStateOf("Week") }

        val phyTests = remember(tests) {
            tests.filter { it.getSubjectCategory() == TestSubjectCategory.PHYSICS || it.physics > 0 }
        }
        val chemTests = remember(tests) {
            tests.filter { it.getSubjectCategory() == TestSubjectCategory.CHEMISTRY || it.chemistry > 0 }
        }
        val bioTests = remember(tests) {
            tests.filter { it.getSubjectCategory() == TestSubjectCategory.BIOLOGY || it.biology > 0 }
        }

        // Section Header for Subject Graphs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subject Performance Analytics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Physics (180M), Chemistry (180M) & Biology (${maxBioMath.toInt()}M) graphs",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs for Subject Graphs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val subjectTabs = listOf(
                    "⚡ Physics (${maxSub.toInt()}M)",
                    "🧪 Chemistry (${maxSub.toInt()}M)",
                    "🧬 $thirdSubjectName (${maxBioMath.toInt()}M)",
                    "📊 View All 3"
                )
                subjectTabs.forEachIndexed { idx, tabTitle ->
                    val isSelected = selectedSubjectGraphTab == idx
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF6366F1) else (if (isDark) Color(0x1EFFFFFF) else Color(0xFFF1F5F9)),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF818CF8) else (if (isDark) Color(0x28FFFFFF) else Color(0xFFE2E8F0))
                        ),
                        modifier = Modifier.clickable { selectedSubjectGraphTab = idx }
                    ) {
                        Text(
                            text = tabTitle,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // 1. PHYSICS GRAPH
        if (selectedSubjectGraphTab == 0 || selectedSubjectGraphTab == 3) {
            val phyPoints = remember(phyTests, phyTimeFilter, maxSub, selectedYear, selectedMonth, monthPartitions) {
                buildSubjectChartPoints(
                    subjectTests = phyTests,
                    category = TestSubjectCategory.PHYSICS,
                    timeFilter = phyTimeFilter,
                    maxScore = maxSub,
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    monthPartitions = monthPartitions,
                    monthStartMs = monthStartMs,
                    monthEndMs = monthEndMs,
                    monthFullLabel = monthFullLabel
                )
            }
            val phyScores = phyTests.map { it.getSubjectScore(TestSubjectCategory.PHYSICS) }
            val avgPhyVal = if (phyScores.isNotEmpty()) phyScores.average().roundToInt() else 0
            val phyQualityRating = when {
                avgPhyVal >= maxSub * 0.8f -> "Physics Master ⚡"
                avgPhyVal >= maxSub * 0.65f -> "Strong Grip 🎯"
                avgPhyVal >= maxSub * 0.5f -> "Progressing 📈"
                else -> "Needs Revision 💡"
            }
            val phyHigh = if (phyScores.isNotEmpty()) phyScores.maxOrNull() ?: 0 else 0

            InteractiveAtmosphericCapsuleChart(
                title = if (phyTimeFilter == "Month") "Physics Performance (${maxSub.toInt()}M) • $monthFullLabel" else "Physics Performance (${maxSub.toInt()}M)",
                subtitle = "Physics single subject & sectional tests • Touch bar to inspect",
                selectedPeriod = if (phyTimeFilter in listOf("Day", "Week", "Month")) phyTimeFilter else "Week",
                onPeriodSelected = { phyTimeFilter = it },
                dataPoints = phyPoints,
                averageDisplay = "$avgPhyVal / ${maxSub.toInt()}",
                qualityScore = if (maxSub > 0) "${((avgPhyVal / maxSub) * 100).roundToInt()}%" else "0%",
                qualityRating = phyQualityRating,
                gauge1Label = "Avg Score %",
                gauge1Percent = if (maxSub > 0) ((avgPhyVal / maxSub) * 100).toInt().coerceIn(0, 100) else 0,
                gauge2Label = "Tests Taken",
                gauge2Percent = (phyTests.size * 10).coerceIn(0, 100),
                gauge3Label = "High Score %",
                gauge3Percent = if (maxSub > 0) ((phyHigh / maxSub) * 100).toInt().coerceIn(0, 100) else 0,
                maxValue = maxSub,
                topGuideLabel = "${maxSub.toInt()} M",
                bottomGuideLabel = "0 M",
                isDark = isDark,
                monthLabel = monthFullLabel,
                isPastMonth = isPastMonth,
                canGoNextMonth = canGoNextMonth,
                canGoPrevMonth = true,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onMonthClick = { showMonthPickerDialog = true },
                onResetToCurrentMonth = onResetToCurrentMonth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        // 2. CHEMISTRY GRAPH
        if (selectedSubjectGraphTab == 1 || selectedSubjectGraphTab == 3) {
            val chemPoints = remember(chemTests, chemTimeFilter, maxSub, selectedYear, selectedMonth, monthPartitions) {
                buildSubjectChartPoints(
                    subjectTests = chemTests,
                    category = TestSubjectCategory.CHEMISTRY,
                    timeFilter = chemTimeFilter,
                    maxScore = maxSub,
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    monthPartitions = monthPartitions,
                    monthStartMs = monthStartMs,
                    monthEndMs = monthEndMs,
                    monthFullLabel = monthFullLabel
                )
            }
            val chemScores = chemTests.map { it.getSubjectScore(TestSubjectCategory.CHEMISTRY) }
            val avgChemVal = if (chemScores.isNotEmpty()) chemScores.average().roundToInt() else 0
            val chemQualityRating = when {
                avgChemVal >= maxSub * 0.8f -> "Chemistry Pro 🧪"
                avgChemVal >= maxSub * 0.65f -> "High Accuracy 🎯"
                avgChemVal >= maxSub * 0.5f -> "Steadily Improving 📈"
                else -> "Focus on NCERT 💡"
            }
            val chemHigh = if (chemScores.isNotEmpty()) chemScores.maxOrNull() ?: 0 else 0

            InteractiveAtmosphericCapsuleChart(
                title = if (chemTimeFilter == "Month") "Chemistry Performance (${maxSub.toInt()}M) • $monthFullLabel" else "Chemistry Performance (${maxSub.toInt()}M)",
                subtitle = "Chemistry single subject & sectional tests • Touch bar to inspect",
                selectedPeriod = if (chemTimeFilter in listOf("Day", "Week", "Month")) chemTimeFilter else "Week",
                onPeriodSelected = { chemTimeFilter = it },
                dataPoints = chemPoints,
                averageDisplay = "$avgChemVal / ${maxSub.toInt()}",
                qualityScore = if (maxSub > 0) "${((avgChemVal / maxSub) * 100).roundToInt()}%" else "0%",
                qualityRating = chemQualityRating,
                gauge1Label = "Avg Score %",
                gauge1Percent = if (maxSub > 0) ((avgChemVal / maxSub) * 100).toInt().coerceIn(0, 100) else 0,
                gauge2Label = "Tests Taken",
                gauge2Percent = (chemTests.size * 10).coerceIn(0, 100),
                gauge3Label = "High Score %",
                gauge3Percent = if (maxSub > 0) ((chemHigh / maxSub) * 100).toInt().coerceIn(0, 100) else 0,
                maxValue = maxSub,
                topGuideLabel = "${maxSub.toInt()} M",
                bottomGuideLabel = "0 M",
                isDark = isDark,
                monthLabel = monthFullLabel,
                isPastMonth = isPastMonth,
                canGoNextMonth = canGoNextMonth,
                canGoPrevMonth = true,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onMonthClick = { showMonthPickerDialog = true },
                onResetToCurrentMonth = onResetToCurrentMonth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        // 3. BIOLOGY / MATHS GRAPH
        if (selectedSubjectGraphTab == 2 || selectedSubjectGraphTab == 3) {
            val bioPoints = remember(bioTests, bioTimeFilter, maxBioMath, selectedYear, selectedMonth, monthPartitions) {
                buildSubjectChartPoints(
                    subjectTests = bioTests,
                    category = TestSubjectCategory.BIOLOGY,
                    timeFilter = bioTimeFilter,
                    maxScore = maxBioMath,
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    monthPartitions = monthPartitions,
                    monthStartMs = monthStartMs,
                    monthEndMs = monthEndMs,
                    monthFullLabel = monthFullLabel
                )
            }
            val bioScores = bioTests.map { it.getSubjectScore(TestSubjectCategory.BIOLOGY) }
            val avgBioVal = if (bioScores.isNotEmpty()) bioScores.average().roundToInt() else 0
            val bioQualityRating = when {
                avgBioVal >= maxBioMath * 0.85f -> "$thirdSubjectName Ace 🧬"
                avgBioVal >= maxBioMath * 0.7f -> "Top Scorer 🎯"
                avgBioVal >= maxBioMath * 0.5f -> "Consistent 📈"
                else -> "Read Line-by-Line 💡"
            }
            val bioHigh = if (bioScores.isNotEmpty()) bioScores.maxOrNull() ?: 0 else 0

            InteractiveAtmosphericCapsuleChart(
                title = if (bioTimeFilter == "Month") "$thirdSubjectName Performance (${maxBioMath.toInt()}M) • $monthFullLabel" else "$thirdSubjectName Performance (${maxBioMath.toInt()}M)",
                subtitle = "$thirdSubjectName single subject & sectional tests • Touch bar to inspect",
                selectedPeriod = if (bioTimeFilter in listOf("Day", "Week", "Month")) bioTimeFilter else "Week",
                onPeriodSelected = { bioTimeFilter = it },
                dataPoints = bioPoints,
                averageDisplay = "$avgBioVal / ${maxBioMath.toInt()}",
                qualityScore = if (maxBioMath > 0) "${((avgBioVal / maxBioMath) * 100).roundToInt()}%" else "0%",
                qualityRating = bioQualityRating,
                gauge1Label = "Avg Score %",
                gauge1Percent = if (maxBioMath > 0) ((avgBioVal / maxBioMath) * 100).toInt().coerceIn(0, 100) else 0,
                gauge2Label = "Tests Taken",
                gauge2Percent = (bioTests.size * 10).coerceIn(0, 100),
                gauge3Label = "High Score %",
                gauge3Percent = if (maxBioMath > 0) ((bioHigh / maxBioMath) * 100).toInt().coerceIn(0, 100) else 0,
                maxValue = maxBioMath,
                topGuideLabel = "${maxBioMath.toInt()} M",
                bottomGuideLabel = "0 M",
                isDark = isDark,
                monthLabel = monthFullLabel,
                isPastMonth = isPastMonth,
                canGoNextMonth = canGoNextMonth,
                canGoPrevMonth = true,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onMonthClick = { showMonthPickerDialog = true },
                onResetToCurrentMonth = onResetToCurrentMonth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        // =========================================================================
        // GRAPH 2: Practice History
        // =========================================================================
        val (periodPracticeP, periodPracticeC, periodPracticeB, periodPracticeTotal) = remember(
            dailyPractices, practiceTimeFilter, todayPractice, selectedYear, selectedMonth
        ) {
            val sdfKeyLocal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            when (practiceTimeFilter) {
                "Day" -> {
                    val p = todayPractice?.physicsSolved ?: 0
                    val c = todayPractice?.chemistrySolved ?: 0
                    val b = todayPractice?.biologySolved ?: 0
                    Quadruple(p, c, b, p + c + b)
                }
                "Week" -> {
                    var p = 0
                    var c = 0
                    var b = 0
                    for (i in 6 downTo 0) {
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                        val key = sdfKeyLocal.format(cal.time)
                        val dp = dailyPractices.find { it.date == key }
                        p += dp?.physicsSolved ?: 0
                        c += dp?.chemistrySolved ?: 0
                        b += dp?.biologySolved ?: 0
                    }
                    Quadruple(p, c, b, p + c + b)
                }
                else -> { // "Month" - strictly sum all practices in the selected month
                    val monthPrefix = String.format(Locale.getDefault(), "%04d-%02d", selectedYear, selectedMonth + 1)
                    var p = 0
                    var c = 0
                    var b = 0
                    dailyPractices.filter { it.date.startsWith(monthPrefix) }.forEach { dp ->
                        p += dp.physicsSolved
                        c += dp.chemistrySolved
                        b += dp.biologySolved
                    }
                    Quadruple(p, c, b, p + c + b)
                }
            }
        }

        val practiceChartPoints = remember(dailyPractices, practiceTimeFilter, todayPractice, selectedYear, selectedMonth, monthPartitions) {
            when (practiceTimeFilter) {
                "Day" -> {
                    val p = todayPractice?.physicsSolved ?: 0
                    val c = todayPractice?.chemistrySolved ?: 0
                    val b = todayPractice?.biologySolved ?: 0
                    val tot = p + c + b
                    val avg = if (tot > 0) (tot / 3f) else 10f
                    listOf(
                        CapsuleChartDataPoint("prac_d_phy", "Phy", "Physics Practice • Today", p.toFloat(), "$p Qs", p >= avg && p > 0, subMetric1 = 100f),
                        CapsuleChartDataPoint("prac_d_chem", "Chem", "Chemistry Practice • Today", c.toFloat(), "$c Qs", c >= avg && c > 0, subMetric2 = 100f),
                        CapsuleChartDataPoint("prac_d_bio", "Bio", "Biology Practice • Today", b.toFloat(), "$b Qs", b >= avg && b > 0, subMetric3 = 100f),
                        CapsuleChartDataPoint("prac_d_tot", "Total", "Total Practice • Today", tot.toFloat(), "$tot Qs", tot > 0, subMetric1 = if (tot > 0) (p.toFloat() / tot * 100f) else 0f, subMetric2 = if (tot > 0) (c.toFloat() / tot * 100f) else 0f, subMetric3 = if (tot > 0) (b.toFloat() / tot * 100f) else 0f)
                    )
                }
                "Week" -> {
                    val points = mutableListOf<CapsuleChartDataPoint>()
                    val weekTotals = mutableListOf<Int>()
                    for (i in 6 downTo 0) {
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                        val key = sdfKey.format(cal.time)
                        val dp = dailyPractices.find { it.date == key }
                        val tot = (dp?.physicsSolved ?: 0) + (dp?.chemistrySolved ?: 0) + (dp?.biologySolved ?: 0)
                        weekTotals.add(tot)
                    }
                    val avg = if (weekTotals.any { it > 0 }) weekTotals.filter { it > 0 }.average().toFloat() else 10f
                    for (i in 6 downTo 0) {
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                        val key = sdfKey.format(cal.time)
                        val dayName = sdfDay.format(cal.time).take(3)
                        val dateLabel = sdfDate.format(cal.time)
                        val dp = dailyPractices.find { it.date == key }
                        val p = dp?.physicsSolved ?: 0
                        val c = dp?.chemistrySolved ?: 0
                        val b = dp?.biologySolved ?: 0
                        val total = p + c + b
                        val isToday = (i == 0)
                        points.add(
                            CapsuleChartDataPoint(
                                id = "prac_w_$key",
                                dayLetter = dayName,
                                fullDate = if (isToday) "Today ($dayName, $dateLabel) • $total Qs" else "$dayName, $dateLabel • $total Qs",
                                value = total.toFloat(),
                                displayValue = "$total Qs",
                                isAboveAverage = total >= avg && total > 0,
                                subMetric1 = if (total > 0) (p.toFloat() / total * 100f) else 0f,
                                subMetric2 = if (total > 0) (c.toFloat() / total * 100f) else 0f,
                                subMetric3 = if (total > 0) (b.toFloat() / total * 100f) else 0f
                            )
                        )
                    }
                    points
                }
                else -> { // "Month" - calculate each week's questions in selected month
                    val weekTotals = mutableListOf<Int>()
                    val weekPoints = monthPartitions.map { part ->
                        var pSum = 0
                        var cSum = 0
                        var bSum = 0
                        for (d in part.startDay..part.endDay) {
                            val key = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, d)
                            val dp = dailyPractices.find { it.date == key }
                            pSum += dp?.physicsSolved ?: 0
                            cSum += dp?.chemistrySolved ?: 0
                            bSum += dp?.biologySolved ?: 0
                        }
                        val total = pSum + cSum + bSum
                        weekTotals.add(total)
                        Triple(part, total, Triple(pSum, cSum, bSum))
                    }
                    val avg = if (weekTotals.any { it > 0 }) weekTotals.filter { it > 0 }.average().toFloat() else 25f
                    weekPoints.mapIndexed { idx, (part, total, splits) ->
                        val (p, c, b) = splits
                        CapsuleChartDataPoint(
                            id = "prac_m_${selectedYear}_${selectedMonth}_$idx",
                            dayLetter = part.header,
                            fullDate = "${part.fullLabel} • $total Qs Solved",
                            value = total.toFloat(),
                            displayValue = "$total Qs",
                            isAboveAverage = total >= avg && total > 0,
                            subMetric1 = if (total > 0) (p.toFloat() / total * 100f) else 0f,
                            subMetric2 = if (total > 0) (c.toFloat() / total * 100f) else 0f,
                            subMetric3 = if (total > 0) (b.toFloat() / total * 100f) else 0f
                        )
                    }
                }
            }
        }

        val practiceMaxQuestions = remember(practiceChartPoints) {
            practiceChartPoints.maxOfOrNull { it.value }?.coerceAtLeast(50f) ?: 100f
        }

        val avgPracticeQ = remember(practiceChartPoints, practiceTimeFilter, periodPracticeTotal) {
            if (practiceTimeFilter == "Day") {
                "$periodPracticeTotal Qs Today"
            } else {
                val valid = practiceChartPoints.map { it.value }
                if (valid.isNotEmpty()) (valid.average()).roundToInt().toString() + " Qs / d" else "0 Qs"
            }
        }
        val practiceTotalAll = periodPracticeTotal.coerceAtLeast(1)

        InteractiveAtmosphericCapsuleChart(
            title = if (practiceTimeFilter == "Month") "Practice History • $monthFullLabel" else "Practice History",
            subtitle = if (practiceTimeFilter == "Month") {
                if (isPastMonth) "Showing historical questions for $monthFullLabel • Touch to inspect"
                else "Showing current month ($monthFullLabel) practice • Touch to inspect"
            } else "Touch any bar or line to inspect question volume & subject splits",
            selectedPeriod = if (practiceTimeFilter in listOf("Day", "Week", "Month")) practiceTimeFilter else "Week",
            onPeriodSelected = { practiceTimeFilter = it },
            dataPoints = practiceChartPoints,
            averageDisplay = avgPracticeQ,
            qualityScore = "$periodPracticeTotal Total",
            qualityRating = when {
                periodPracticeTotal >= 150 -> "Top Pace 🚀"
                periodPracticeTotal >= 60 -> "Consistent ⚡"
                periodPracticeTotal > 0 -> "Building 🎯"
                else -> "Awaiting Practice"
            },
            gauge1Label = "Physics",
            gauge1Percent = (periodPracticeP * 100 / practiceTotalAll).coerceIn(0, 100),
            gauge2Label = "Chemistry",
            gauge2Percent = (periodPracticeC * 100 / practiceTotalAll).coerceIn(0, 100),
            gauge3Label = thirdSubjectName,
            gauge3Percent = (periodPracticeB * 100 / practiceTotalAll).coerceIn(0, 100),
            maxValue = practiceMaxQuestions,
            topGuideLabel = "${practiceMaxQuestions.toInt()} Q",
            bottomGuideLabel = "0 Q",
            isDark = isDark,
            monthLabel = monthFullLabel,
            isPastMonth = isPastMonth,
            canGoNextMonth = canGoNextMonth,
            canGoPrevMonth = true,
            onPrevMonth = onPrevMonth,
            onNextMonth = onNextMonth,
            onMonthClick = { showMonthPickerDialog = true },
            onResetToCurrentMonth = onResetToCurrentMonth,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        // =========================================================================
        // GRAPH 3: Study Trends
        // =========================================================================
        val (periodStudyPhy, periodStudyChem, periodStudyBio, periodStudyTotalSec) = remember(
            logs, studyTimeFilter, selectedYear, selectedMonth, monthStartMs, monthEndMs
        ) {
            val sdfKeyLocal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val nowMs = System.currentTimeMillis()
            val filteredLogs = when (studyTimeFilter) {
                "Day" -> {
                    val todayKey = sdfKeyLocal.format(Date())
                    logs.filter { sdfKeyLocal.format(Date(it.timestamp)) == todayKey }
                }
                "Week" -> {
                    logs.filter { (nowMs - it.timestamp) <= 7L * 24 * 3600 * 1000L }
                }
                else -> { // "Month" - strictly within selected month
                    logs.filter { it.timestamp in monthStartMs..monthEndMs }
                }
            }
            val p = filteredLogs.filter { it.subject.equals("Physics", ignoreCase = true) }.sumOf { it.durationSeconds }
            val c = filteredLogs.filter { it.subject.equals("Chemistry", ignoreCase = true) }.sumOf { it.durationSeconds }
            val b = filteredLogs.filter { it.subject.equals("Biology", ignoreCase = true) || it.subject.equals("Botany", ignoreCase = true) || it.subject.equals("Zoology", ignoreCase = true) }.sumOf { it.durationSeconds }
            Quadruple(p, c, b, p + c + b)
        }

        val studyChartPoints = remember(logs, studyTimeFilter, selectedYear, selectedMonth, monthPartitions) {
            when (studyTimeFilter) {
                "Day" -> {
                    val todayKey = sdfKey.format(Date())
                    val todayLogs = logs.filter { sdfKey.format(Date(it.timestamp)) == todayKey }
                    val phyH = todayLogs.filter { it.subject.equals("Physics", ignoreCase = true) }.sumOf { it.durationSeconds } / 3600f
                    val chemH = todayLogs.filter { it.subject.equals("Chemistry", ignoreCase = true) }.sumOf { it.durationSeconds } / 3600f
                    val bioH = todayLogs.filter { it.subject.equals("Biology", ignoreCase = true) || it.subject.equals("Botany", ignoreCase = true) || it.subject.equals("Zoology", ignoreCase = true) }.sumOf { it.durationSeconds } / 3600f
                    val totH = phyH + chemH + bioH
                    val avg = if (totH > 0) (totH / 3f) else 1f
                    listOf(
                        CapsuleChartDataPoint("study_d_phy", "Phy", "Physics Study • Today", phyH, String.format(Locale.getDefault(), "%.1fh", phyH), phyH >= avg && phyH > 0, subMetric1 = 100f),
                        CapsuleChartDataPoint("study_d_chem", "Chem", "Chemistry Study • Today", chemH, String.format(Locale.getDefault(), "%.1fh", chemH), chemH >= avg && chemH > 0, subMetric2 = 100f),
                        CapsuleChartDataPoint("study_d_bio", "Bio", "Biology Study • Today", bioH, String.format(Locale.getDefault(), "%.1fh", bioH), bioH >= avg && bioH > 0, subMetric3 = 100f),
                        CapsuleChartDataPoint("study_d_tot", "Total", "Total Study • Today", totH, String.format(Locale.getDefault(), "%.1fh", totH), totH > 0, subMetric1 = if (totH > 0) (phyH / totH * 100f) else 0f, subMetric2 = if (totH > 0) (chemH / totH * 100f) else 0f, subMetric3 = if (totH > 0) (bioH / totH * 100f) else 0f)
                    )
                }
                "Week" -> {
                    val points = mutableListOf<CapsuleChartDataPoint>()
                    val dailyHours = mutableListOf<Float>()
                    for (i in 6 downTo 0) {
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                        val key = sdfKey.format(cal.time)
                        val dayLogs = logs.filter { sdfKey.format(Date(it.timestamp)) == key }
                        val h = dayLogs.sumOf { it.durationSeconds } / 3600f
                        dailyHours.add(h)
                    }
                    val avg = if (dailyHours.any { it > 0 }) dailyHours.filter { it > 0 }.average().toFloat() else 2f
                    for (i in 6 downTo 0) {
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                        val key = sdfKey.format(cal.time)
                        val dayName = sdfDay.format(cal.time).take(3)
                        val dateLabel = sdfDate.format(cal.time)
                        val dayLogs = logs.filter { sdfKey.format(Date(it.timestamp)) == key }
                        val phyH = dayLogs.filter { it.subject.equals("Physics", ignoreCase = true) }.sumOf { it.durationSeconds } / 3600f
                        val chemH = dayLogs.filter { it.subject.equals("Chemistry", ignoreCase = true) }.sumOf { it.durationSeconds } / 3600f
                        val bioH = dayLogs.filter { it.subject.equals("Biology", ignoreCase = true) || it.subject.equals("Botany", ignoreCase = true) || it.subject.equals("Zoology", ignoreCase = true) }.sumOf { it.durationSeconds } / 3600f
                        val totalH = phyH + chemH + bioH
                        val isToday = (i == 0)
                        points.add(
                            CapsuleChartDataPoint(
                                id = "study_w_$key",
                                dayLetter = dayName,
                                fullDate = if (isToday) "Today ($dayName, $dateLabel) • ${String.format(Locale.getDefault(), "%.1fh", totalH)}" else "$dayName, $dateLabel • ${String.format(Locale.getDefault(), "%.1fh", totalH)}",
                                value = totalH,
                                displayValue = String.format(Locale.getDefault(), "%.1fh", totalH),
                                isAboveAverage = totalH >= avg && totalH > 0,
                                subMetric1 = if (totalH > 0) (phyH / totalH * 100f) else 0f,
                                subMetric2 = if (totalH > 0) (chemH / totalH * 100f) else 0f,
                                subMetric3 = if (totalH > 0) (bioH / totalH * 100f) else 0f
                            )
                        )
                    }
                    points
                }
                else -> { // "Month" - partition selected month into weeks
                    val monthLogs = logs.filter { it.timestamp in monthStartMs..monthEndMs }
                    val weekHoursList = mutableListOf<Float>()
                    val weekPoints = monthPartitions.map { part ->
                        val weekLogs = monthLogs.filter { it.timestamp in part.startMs..part.endMs }
                        val phyH = weekLogs.filter { it.subject.equals("Physics", ignoreCase = true) }.sumOf { it.durationSeconds } / 3600f
                        val chemH = weekLogs.filter { it.subject.equals("Chemistry", ignoreCase = true) }.sumOf { it.durationSeconds } / 3600f
                        val bioH = weekLogs.filter { it.subject.equals("Biology", ignoreCase = true) || it.subject.equals("Botany", ignoreCase = true) || it.subject.equals("Zoology", ignoreCase = true) }.sumOf { it.durationSeconds } / 3600f
                        val totalH = phyH + chemH + bioH
                        weekHoursList.add(totalH)
                        Triple(part, totalH, Triple(phyH, chemH, bioH))
                    }
                    val avg = if (weekHoursList.any { it > 0 }) weekHoursList.filter { it > 0 }.average().toFloat() else 5f
                    weekPoints.mapIndexed { idx, (part, totalH, splits) ->
                        val (p, c, b) = splits
                        CapsuleChartDataPoint(
                            id = "study_m_${selectedYear}_${selectedMonth}_$idx",
                            dayLetter = part.header,
                            fullDate = "${part.fullLabel} • ${String.format(Locale.getDefault(), "%.1fh", totalH)}",
                            value = totalH,
                            displayValue = String.format(Locale.getDefault(), "%.1fh", totalH),
                            isAboveAverage = totalH >= avg && totalH > 0,
                            subMetric1 = if (totalH > 0) (p / totalH * 100f) else 0f,
                            subMetric2 = if (totalH > 0) (c / totalH * 100f) else 0f,
                            subMetric3 = if (totalH > 0) (b / totalH * 100f) else 0f
                        )
                    }
                }
            }
        }

        val periodStudyHours = periodStudyTotalSec / 3600.0
        val avgDailyStudyHours = remember(studyChartPoints, studyTimeFilter, periodStudyHours) {
            if (studyTimeFilter == "Day") {
                String.format(Locale.getDefault(), "%.1fh Today", periodStudyHours)
            } else {
                val valid = studyChartPoints.map { it.value }
                if (valid.isNotEmpty()) String.format(Locale.getDefault(), "%.1fh / day", valid.average()) else "0.0h"
            }
        }
        val sumStudyTotal = periodStudyTotalSec.coerceAtLeast(1)
        val maxStudyVal = remember(studyChartPoints) {
            studyChartPoints.maxOfOrNull { it.value }?.coerceAtLeast(4f) ?: 8f
        }

        InteractiveAtmosphericCapsuleChart(
            title = if (studyTimeFilter == "Month") "Study Trends • $monthFullLabel" else "Study Trends",
            subtitle = if (studyTimeFilter == "Month") {
                if (isPastMonth) "Showing historical study hours for $monthFullLabel • Touch to inspect"
                else "Showing current month ($monthFullLabel) study hours • Touch to inspect"
            } else "Touch any bar or line to inspect study duration & subject splits",
            selectedPeriod = if (studyTimeFilter in listOf("Day", "Week", "Month")) studyTimeFilter else "Week",
            onPeriodSelected = { studyTimeFilter = it },
            dataPoints = studyChartPoints,
            averageDisplay = avgDailyStudyHours,
            qualityScore = String.format(Locale.getDefault(), "%.1fh Total", periodStudyHours),
            qualityRating = when {
                periodStudyHours >= 25.0 -> "Elite Focus ⚡"
                periodStudyHours >= 10.0 -> "Strong Pace 📈"
                periodStudyHours > 0 -> "In Progress 🎯"
                else -> "Awaiting Logs"
            },
            gauge1Label = "Physics",
            gauge1Percent = ((periodStudyPhy.toDouble() / sumStudyTotal) * 100).toInt().coerceIn(0, 100),
            gauge2Label = "Chemistry",
            gauge2Percent = ((periodStudyChem.toDouble() / sumStudyTotal) * 100).toInt().coerceIn(0, 100),
            gauge3Label = thirdSubjectName,
            gauge3Percent = ((periodStudyBio.toDouble() / sumStudyTotal) * 100).toInt().coerceIn(0, 100),
            maxValue = maxStudyVal,
            topGuideLabel = String.format(Locale.getDefault(), "%.0fh", maxStudyVal),
            bottomGuideLabel = "0h",
            isDark = isDark,
            monthLabel = monthFullLabel,
            isPastMonth = isPastMonth,
            canGoNextMonth = canGoNextMonth,
            canGoPrevMonth = true,
            onPrevMonth = onPrevMonth,
            onNextMonth = onNextMonth,
            onMonthClick = { showMonthPickerDialog = true },
            onResetToCurrentMonth = onResetToCurrentMonth,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }

    // =========================================================================
    // MONTH PICKER DIALOG
    // =========================================================================
    if (showMonthPickerDialog) {
        var pickerYear by remember { mutableStateOf(selectedYear) }
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        Dialog(onDismissRequest = { showMonthPickerDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Month & Year",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                        IconButton(
                            onClick = { showMonthPickerDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = subTextColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Year Selector Row
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x15FFFFFF) else Color(0xFFF1F5F9))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = { pickerYear -= 1 },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Year", tint = textColor, modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "$pickerYear",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )

                        IconButton(
                            onClick = { if (pickerYear < currentYear) pickerYear += 1 },
                            enabled = pickerYear < currentYear,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Year",
                                tint = if (pickerYear < currentYear) textColor else subTextColor.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 12 Months Grid (3 columns x 4 rows)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in 0..3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0..2) {
                                    val mIdx = row * 3 + col
                                    val isSelected = (pickerYear == selectedYear && mIdx == selectedMonth)
                                    val isFuture = (pickerYear > currentYear || (pickerYear == currentYear && mIdx > currentMonth))

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    isSelected -> Color(0xFF6366F1)
                                                    isFuture -> Color.Transparent
                                                    else -> (if (isDark) Color(0x10FFFFFF) else Color(0xFFF8FAFC))
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                when {
                                                    isSelected -> Color.Transparent
                                                    isFuture -> Color.Transparent
                                                    else -> (if (isDark) Color(0x15FFFFFF) else Color(0xFFE2E8F0))
                                                },
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable(enabled = !isFuture) {
                                                selectedYear = pickerYear
                                                selectedMonth = mIdx
                                                mockTimeFilter = "Month"
                                                practiceTimeFilter = "Month"
                                                studyTimeFilter = "Month"
                                                showMonthPickerDialog = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = monthNames[mIdx],
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                            color = when {
                                                isSelected -> Color.White
                                                isFuture -> subTextColor.copy(alpha = 0.3f)
                                                else -> textColor
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick current month shortcut
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                selectedYear = currentYear
                                selectedMonth = currentMonth
                                mockTimeFilter = "Month"
                                practiceTimeFilter = "Month"
                                studyTimeFilter = "Month"
                                showMonthPickerDialog = false
                            }
                        ) {
                            Text("Jump to Current Month", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showMonthPickerDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun buildSubjectChartPoints(
    subjectTests: List<MockTest>,
    category: TestSubjectCategory,
    timeFilter: String,
    maxScore: Float,
    selectedYear: Int,
    selectedMonth: Int,
    monthPartitions: List<MonthWeekPartition>,
    monthStartMs: Long,
    monthEndMs: Long,
    monthFullLabel: String
): List<CapsuleChartDataPoint> {
    val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDay = SimpleDateFormat("EEE", Locale.getDefault())
    val sdfDate = SimpleDateFormat("dd MMM", Locale.getDefault())
    val subName = category.displayName

    return when (timeFilter) {
        "Day" -> {
            val recent = subjectTests.sortedBy { it.timestamp }.takeLast(7)
            if (recent.isEmpty()) {
                listOf("T1", "T2", "T3", "T4", "T5").mapIndexed { idx, name ->
                    CapsuleChartDataPoint(
                        id = "${subName.lowercase()}_day_empty_$idx",
                        dayLetter = name,
                        fullDate = "$subName Test ${idx + 1}",
                        value = 0f,
                        displayValue = "No Test",
                        isAboveAverage = false,
                        detailNotes = "No $subName test data logged yet"
                    )
                }
            } else {
                val scores = recent.map { it.getSubjectScore(category) }
                val avg = scores.average().toFloat().coerceAtLeast(1f)
                recent.mapIndexed { idx, t ->
                    val sc = t.getSubjectScore(category)
                    val isSingle = t.isSingleSubjectTest()
                    CapsuleChartDataPoint(
                        id = "${subName.lowercase()}_t_${t.id}_$idx",
                        dayLetter = "T${idx + 1}",
                        fullDate = "${t.testName} (${sdfDate.format(Date(t.timestamp))})",
                        value = sc.toFloat(),
                        displayValue = "$sc/${maxScore.toInt()}",
                        isAboveAverage = sc >= avg,
                        detailNotes = if (isSingle) "$subName Test: $sc/${maxScore.toInt()}" else "Sectional $subName: $sc/${maxScore.toInt()} (Total: ${t.score})",
                        subMetric1 = (sc.toFloat() / maxScore * 100f).coerceIn(0f, 100f),
                        subMetric2 = if (t.negative > 0) ((maxScore - t.negative).coerceAtLeast(0f) / maxScore * 100f) else 100f,
                        subMetric3 = if (sc >= maxScore * 0.75f) 100f else if (sc >= maxScore * 0.5f) 60f else 30f
                    )
                }
            }
        }
        "Week" -> {
            val points = mutableListOf<CapsuleChartDataPoint>()
            val nowMs = System.currentTimeMillis()
            val weekTests = subjectTests.filter { (nowMs - it.timestamp) <= 7L * 24 * 3600 * 1000L }
            val avg = if (weekTests.isNotEmpty()) weekTests.map { it.getSubjectScore(category) }.average().toFloat() else (maxScore * 0.65f)
            for (i in 6 downTo 0) {
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                val dayKey = sdfKey.format(c.time)
                val dayLabel = sdfDay.format(c.time).take(3)
                val dateLabel = sdfDate.format(c.time)
                val dayTest = subjectTests.filter { sdfKey.format(Date(it.timestamp)) == dayKey }.maxByOrNull { it.getSubjectScore(category) }
                val sc = dayTest?.getSubjectScore(category) ?: 0
                val isSingle = dayTest?.isSingleSubjectTest() == true
                points.add(
                    CapsuleChartDataPoint(
                        id = "${subName.lowercase()}_week_$dayKey",
                        dayLetter = dayLabel,
                        fullDate = if (dayTest != null) "${dayTest.testName} • ${sdfDate.format(Date(dayTest.timestamp))}" else "$dayLabel, $dateLabel • No Test",
                        value = sc.toFloat(),
                        displayValue = if (dayTest != null) "$sc/${maxScore.toInt()}" else "0/${maxScore.toInt()}",
                        isAboveAverage = sc >= avg && sc > 0,
                        detailNotes = if (dayTest != null) (if (isSingle) "$subName Test: $sc/${maxScore.toInt()}" else "Sectional $subName: $sc/${maxScore.toInt()} (Total: ${dayTest.score})") else "No $subName test on $dateLabel",
                        subMetric1 = if (sc > 0) (sc.toFloat() / maxScore * 100f).coerceIn(0f, 100f) else 0f,
                        subMetric2 = if (dayTest != null && dayTest.negative > 0) 70f else 100f,
                        subMetric3 = if (sc >= maxScore * 0.75f) 100f else 50f
                    )
                )
            }
            points
        }
        else -> { // "Month"
            val testsInMonth = subjectTests.filter { it.timestamp in monthStartMs..monthEndMs }
            val avg = if (testsInMonth.isNotEmpty()) testsInMonth.map { it.getSubjectScore(category) }.average().toFloat() else (maxScore * 0.65f)
            monthPartitions.mapIndexed { idx, part ->
                val inWeek = testsInMonth.filter { it.timestamp in part.startMs..part.endMs }
                val sc = if (inWeek.isNotEmpty()) inWeek.map { it.getSubjectScore(category) }.average().roundToInt() else 0
                CapsuleChartDataPoint(
                    id = "${subName.lowercase()}_month_${selectedYear}_${selectedMonth}_$idx",
                    dayLetter = part.header,
                    fullDate = "${part.fullLabel} • ${if (inWeek.isNotEmpty()) "${inWeek.size} $subName Tests" else "No Tests"}",
                    value = sc.toFloat(),
                    displayValue = if (sc > 0) "$sc/${maxScore.toInt()}" else "0/${maxScore.toInt()}",
                    isAboveAverage = sc >= avg && sc > 0,
                    detailNotes = if (inWeek.isNotEmpty()) "${inWeek.size} tests • Avg $subName: $sc/${maxScore.toInt()}" else "No $subName tests recorded in this week of $monthFullLabel",
                    subMetric1 = if (sc > 0) (sc.toFloat() / maxScore * 100f).coerceIn(0f, 100f) else 0f,
                    subMetric2 = if (inWeek.any { it.isSingleSubjectTest() }) 100f else 60f,
                    subMetric3 = if (sc >= maxScore * 0.75f) 100f else 50f
                )
            }
        }
    }
}
