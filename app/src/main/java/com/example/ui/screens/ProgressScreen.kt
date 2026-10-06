package com.example.ui.screens
import androidx.compose.material.icons.automirrored.filled.*

import androidx.compose.animation.*
import com.example.ui.components.AppSegmentedControl
import com.example.ui.components.InteractiveAtmosphericCapsuleChart
import com.example.ui.components.CapsuleChartDataPoint
import com.example.ui.components.FireworksProgressBar
import java.util.Locale
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ExamSyllabusDatabase
import com.example.ui.AppViewModel

val subjectChapters = mapOf(
    "physics" to listOf(
        "Units and Measurements", "Motion in a Straight Line", "Motion in a Plane", "Laws of Motion",
        "Work, Energy and Power", "System of Particles and Rotational Motion", "Gravitation",
        "Mechanical Properties of Solids", "Mechanical Properties of Fluids", "Thermal Properties of Matter",
        "Thermodynamics", "Kinetic Theory", "Oscillations", "Waves", "Electric Charges and Fields"
    ),
    "chemistry" to listOf(
        "Some Basic Concepts of Chemistry", "Structure of Atom", "Classification of Elements and Periodicity",
        "Chemical Bonding", "Thermodynamics", "Equilibrium", "Redox Reactions",
        "Organic Chemistry: Some Basic Principles", "Hydrocarbons", "Solutions", "Electrochemistry",
        "Chemical Kinetics", "d and f Block Elements", "Coordination Compounds", "Haloalkanes and Haloarenes"
    ),
    "biology" to listOf(
        "The Living World", "Biological Classification", "Plant Kingdom", "Animal Kingdom",
        "Morphology of Flowering Plants", "Anatomy of Flowering Plants", "Structural Organisation in Animals",
        "Cell: The Unit of Life", "Biomolecules", "Cell Cycle and Cell Division", "Photosynthesis in Higher Plants",
        "Respiration in Plants", "Plant Growth and Development", "Human Reproduction", "Genetics and Evolution"
    )
)

val baseTopics = listOf(
    "Lecture Notes / Video",
    "Formula Sheet / Short Notes",
    "Topic-wise Practice Questions",
    "NCERT Reading & Back Exercises",
    "PYQs (Past 10 Years)",
    "Revision Module / Flashcards"
)

val chapterSubTopics: Map<String, List<String>>
    get() = ExamSyllabusDatabase.neetChapterSubtopics

@Composable
fun ProgressScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    showFooter: Boolean = true
) {
    val completedTopics by viewModel.completedTopics.collectAsStateWithLifecycle()
    val completedIds = remember(completedTopics) { completedTopics.map { it.topicId }.toSet() }
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDark = isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0x0DFFFFFF) else Color.White
    val cardBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
    val pillBg = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)
    
    val activeExamGoal by viewModel.activeExamGoal.collectAsStateWithLifecycle()
    val availableSubjects = remember(activeExamGoal) {
        val list = ExamSyllabusDatabase.getSubjectsFor(activeExamGoal)
        if (list.isNotEmpty()) list else listOf("Physics", "Chemistry", "Biology")
    }

    var activeSubject by remember { mutableStateOf(availableSubjects.firstOrNull() ?: "Physics") }
    var expandedChapter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeExamGoal) {
        if (activeSubject !in availableSubjects) {
            activeSubject = availableSubjects.firstOrNull() ?: "Physics"
        }
    }
    
    val chapters: List<String> = remember(activeExamGoal, activeSubject) {
        val list = ExamSyllabusDatabase.getChaptersFor(activeExamGoal, activeSubject).map { it.name }
        if (list.isNotEmpty()) {
            list
        } else {
            subjectChapters[activeSubject.lowercase()] ?: emptyList()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            if (availableSubjects.size <= 3) {
                AppSegmentedControl(
                    items = availableSubjects,
                    selectedItem = activeSubject,
                    onItemSelected = { activeSubject = it },
                    itemLabel = { it },
                    itemEmoji = {
                        when {
                            it.contains("Physics", ignoreCase = true) -> "⚡"
                            it.contains("Chemistry", ignoreCase = true) -> "🧪"
                            it.contains("Biology", ignoreCase = true) -> "🧬"
                            it.contains("Math", ignoreCase = true) -> "📐"
                            else -> "📚"
                        }
                    },
                    selectedColor = Color(0xFF6D28D9),
                    selectedGradient = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
                    isDark = isDark
                )
            } else {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(availableSubjects) { subj ->
                        val isSel = activeSubject == subj
                        val emoji = when {
                            subj.contains("Fluid", ignoreCase = true) -> "🌊"
                            subj.contains("Heat", ignoreCase = true) -> "🔥"
                            subj.contains("Mass", ignoreCase = true) -> "⚗️"
                            subj.contains("Reaction", ignoreCase = true) || subj.contains("CRE", ignoreCase = true) -> "⚛️"
                            subj.contains("Thermo", ignoreCase = true) -> "🌡️"
                            subj.contains("Control", ignoreCase = true) || subj.contains("Dynamics", ignoreCase = true) -> "🎛️"
                            subj.contains("Plant", ignoreCase = true) || subj.contains("Economics", ignoreCase = true) -> "🏭"
                            subj.contains("Tech", ignoreCase = true) -> "🧪"
                            subj.contains("Math", ignoreCase = true) -> "📐"
                            subj.contains("Aptitude", ignoreCase = true) -> "💡"
                            else -> "📖"
                        }
                        Surface(
                            color = if (isSel) Color(0xFF0284C7) else if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF0284C7) else if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable { activeSubject = subj }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(emoji, fontSize = 12.sp)
                                Text(
                                    subj,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSel) Color.White else textColor
                                )
                            }
                        }
                    }
                }
            }
        }
        
        
        val studyLogs by viewModel.studyLogs.collectAsStateWithLifecycle()
        val chapterTimes = remember(studyLogs, chapters) {
            val map = mutableMapOf<String, Float>()
            chapters.forEach { map[it] = 0f }
            studyLogs.forEach { log ->
                if (map.containsKey(log.chapter)) {
                    map[log.chapter] = (map[log.chapter] ?: 0f) + (log.durationSeconds / 3600f)
                }
            }
            map
        }


        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (chapterTimes.values.any { it > 0 }) {
                val activeChapters = chapters.filter { (chapterTimes[it] ?: 0f) > 0 }.ifEmpty { chapters.take(6) }
                val maxTime = activeChapters.maxOfOrNull { chapterTimes[it] ?: 0f }?.coerceAtLeast(1f) ?: 1f
                val avgHours = activeChapters.map { chapterTimes[it] ?: 0f }.average().toFloat()

                var studyPeriod by remember { mutableStateOf("Subject") }
                val chartPoints = activeChapters.take(8).mapIndexed { idx, chap ->
                    val h = chapterTimes[chap] ?: 0f
                    val shortName = if (chap.length > 7) chap.take(6) + ".." else chap
                    CapsuleChartDataPoint(
                        id = "prog_chap_$idx",
                        dayLetter = chap.take(1).uppercase(),
                        fullDate = "$chap • ${String.format(Locale.getDefault(), "%.1fh", h)}",
                        value = h,
                        displayValue = String.format(Locale.getDefault(), "%.1fh", h),
                        isAboveAverage = h >= avgHours && h > 0f
                    )
                }

                val totalHours = activeChapters.sumOf { (chapterTimes[it] ?: 0f).toDouble() }

                InteractiveAtmosphericCapsuleChart(
                    title = "Chapter Study Distribution",
                    subtitle = "Touch any bar or line to inspect topic hours",
                    selectedPeriod = studyPeriod,
                    onPeriodSelected = { studyPeriod = it },
                    dataPoints = chartPoints,
                    averageDisplay = String.format(Locale.getDefault(), "%.1fh Avg", avgHours),
                    qualityScore = String.format(Locale.getDefault(), "%.1fh Total", totalHours),
                    qualityRating = if (totalHours >= 10.0) "On Track" else "Growing",
                    maxValue = maxTime,
                    topGuideLabel = String.format(Locale.getDefault(), "%.1fh", maxTime),
                    bottomGuideLabel = "0h",
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            chapters.forEach { chapter ->
                val subTopics = remember(chapter) { ExamSyllabusDatabase.getSubtopicsForChapter(chapter) }
                val completedCount = subTopics.count { completedIds.contains("$chapter-$it") }
                val progressPercent = if (subTopics.isEmpty()) 0 else (completedCount * 100) / subTopics.size
                val isExpanded = expandedChapter == chapter
                val isAllCompleted = completedCount == subTopics.size

                val animatedProgress by animateFloatAsState(
                    targetValue = if (subTopics.isEmpty()) 0f else completedCount.toFloat() / subTopics.size.toFloat(),
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                    label = "progressBarWidth"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isAllCompleted) (if (isDark) Color(0x1A10B981) else Color(0xFFECFDF5)) else cardBg)
                        .border(
                            1.dp,
                            when {
                                isExpanded -> Color(0xFF6366F1)
                                isAllCompleted -> Color(0xFF10B981)
                                else -> cardBorder
                            },
                            RoundedCornerShape(24.dp)
                        )
                        .clickable { expandedChapter = if (isExpanded) null else chapter }
                        .padding(20.dp)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(chapter, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = if (isAllCompleted) Color(0xFF10B981) else subTextColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$completedCount/${subTopics.size} tasks", style = MaterialTheme.typography.labelSmall, color = if (isAllCompleted) Color(0xFF10B981) else subTextColor)
                                }
                            }
                            Text("$progressPercent%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = if (isAllCompleted) Color(0xFF10B981) else textColor)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Progress Bar
                        FireworksProgressBar(
                            progress = animatedProgress,
                            height = 8.dp,
                            gradientColors = if (isAllCompleted) listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF00F5A0))
                                             else listOf(Color(0xFF3B82F6), Color(0xFF60A5FA)),
                            sparkColor = if (isAllCompleted) Color(0xFF10B981) else Color(0xFF60A5FA),
                            isDark = isDark
                        )
                        
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 24.dp).fillMaxWidth()) {
                                HorizontalDivider(color = cardBorder, modifier = Modifier.padding(bottom = 16.dp))
                                subTopics.forEach { topic ->
                                    val topicId = "$chapter-$topic"
                                    val isChecked = completedIds.contains(topicId)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .clickable { viewModel.toggleTopicCompletion(topicId, !isChecked) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isChecked) Color(0xFF10B981) else Color.Transparent)
                                                .border(1.dp, if (isChecked) Color(0xFF10B981) else cardBorder, RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isChecked) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(topic, style = MaterialTheme.typography.bodyMedium, color = if (isChecked) subTextColor else textColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showFooter) {
                Spacer(modifier = Modifier.height(8.dp))
                com.example.ui.components.AppBrandingFooter(isDark = isDark)
            }
        }
    }
}
