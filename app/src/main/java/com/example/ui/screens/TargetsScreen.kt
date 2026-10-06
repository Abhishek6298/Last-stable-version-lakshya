package com.example.ui.screens
import com.example.data.GoalStatus
import androidx.compose.material.icons.automirrored.filled.*

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.*
import com.example.ui.components.GlassCard
import com.example.ui.components.AppSegmentedControl
import com.example.ui.components.FireworksProgressBar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyPractice
import com.example.data.DppItem
import com.example.data.Goal
import com.example.ui.AppViewModel
import com.example.ui.components.PracticeOverviewCard
import com.example.ui.components.TodayTargetGraphCard
import com.example.ui.components.WeeklyTargetGraphCard
import com.example.ui.components.DifficultyPracticeAnalyticsCard

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun shiftDate(currentDateStr: String, days: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        sdf.parse(currentDateStr)?.let { cal.time = it }
        cal.add(Calendar.DAY_OF_YEAR, days)
        sdf.format(cal.time)
    } catch (e: Exception) {
        currentDateStr
    }
}

private fun formatDisplayDate(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outSdf = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
        val parsed = sdf.parse(dateStr)
        if (parsed != null) outSdf.format(parsed) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onNavigate: ((String) -> Unit)? = null,
    onNavigateBack: () -> Unit = {}
) {
    androidx.activity.compose.BackHandler(enabled = true) {
        onNavigateBack()
    }
    val activeTargetTabFromVm by viewModel.activeTargetTab.collectAsStateWithLifecycle()
    val mainTab = activeTargetTabFromVm
    var targetPeriod by remember(activeTargetTabFromVm) {
        mutableStateOf(if (activeTargetTabFromVm == "Weekly") "Weekly Target" else "Today Target")
    }
    
    val activeTargetSubjectFromVm by viewModel.activeTargetSubject.collectAsStateWithLifecycle()
    val activeExamGoal by viewModel.activeExamGoal.collectAsStateWithLifecycle()
    val availableSubjects = remember(activeExamGoal) {
        com.example.data.ExamSyllabusDatabase.getSubjectsFor(activeExamGoal).ifEmpty {
            listOf("Physics", "Chemistry", "Biology")
        }
    }
    var selectedSubject by remember(activeExamGoal, activeTargetSubjectFromVm) { 
        mutableStateOf(
            if (availableSubjects.contains(activeTargetSubjectFromVm)) activeTargetSubjectFromVm 
            else (availableSubjects.firstOrNull() ?: "Physics")
        ) 
    }
    
    // Revision tab states
    var revisionSubjectFilter by remember { mutableStateOf("All") }
    var revisionUrgencyFilter by remember { mutableStateOf("All") } // "All", "Due", "Urgent", "Fresh", "Scheduled"
    var revisionSearchQuery by remember { mutableStateOf("") }
    var collapsedChapters by remember { mutableStateOf(setOf<String>()) }
    var showAddChapterRevisionDialog by remember { mutableStateOf(false) }
    var showAiMindPlanDialog by remember { mutableStateOf(false) }
    var scheduleTopicTarget by remember { mutableStateOf<com.example.data.CompletedTopic?>(null) }
    var scheduleChapterTarget by remember { mutableStateOf<ChapterRevisionGroup?>(null) }
    var showAiTopicSummaryDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var revisionAddSubject by remember { mutableStateOf(availableSubjects.firstOrNull() ?: "Physics") }
    var revisionAddChapter by remember { mutableStateOf("") }
    var revisionSelectedSubtopics by remember { mutableStateOf(setOf<String>()) }
            
    val context = LocalContext.current
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val dailyPractices by viewModel.dailyPractices.collectAsStateWithLifecycle()
    val dppItems by viewModel.dppItems.collectAsStateWithLifecycle()
    val studyLogs by viewModel.studyLogs.collectAsStateWithLifecycle()
    val completedTopics by viewModel.completedTopics.collectAsStateWithLifecycle()
    val bookProgressions by viewModel.bookProgressions.collectAsStateWithLifecycle()
    
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var selectedDate by remember { mutableStateOf(todayDate) }

    val currentPractice = dailyPractices.find { it.date == selectedDate } ?: DailyPractice(date = selectedDate)
    
    var pTarget by remember(currentPractice, selectedDate) { mutableStateOf(currentPractice.physicsTarget.toString()) }
    var cTarget by remember(currentPractice, selectedDate) { mutableStateOf(currentPractice.chemistryTarget.toString()) }
    var bTarget by remember(currentPractice, selectedDate) { mutableStateOf(currentPractice.biologyTarget.toString()) }
    
    var pSolved by remember(currentPractice, selectedDate) { mutableStateOf(if (currentPractice.physicsSolved > 0) currentPractice.physicsSolved.toString() else "") }
    var cSolved by remember(currentPractice, selectedDate) { mutableStateOf(if (currentPractice.chemistrySolved > 0) currentPractice.chemistrySolved.toString() else "") }
    var bSolved by remember(currentPractice, selectedDate) { mutableStateOf(if (currentPractice.biologySolved > 0) currentPractice.biologySolved.toString() else "") }

    var pDifficulty by remember(currentPractice, selectedDate) { mutableStateOf(currentPractice.physicsDifficulty.ifBlank { "Medium" }) }
    var cDifficulty by remember(currentPractice, selectedDate) { mutableStateOf(currentPractice.chemistryDifficulty.ifBlank { "Medium" }) }
    var bDifficulty by remember(currentPractice, selectedDate) { mutableStateOf(currentPractice.biologyDifficulty.ifBlank { "Medium" }) }

    // Expand / Collapse card states
    var isTodayTargetsExpanded by remember { mutableStateOf(true) }
    var isWeeklyTargetsExpanded by remember { mutableStateOf(true) }

    // Weekly Targets State from ViewModel
    val wpTarget by viewModel.weeklyPhysicsTarget.collectAsStateWithLifecycle()
    val wcTarget by viewModel.weeklyChemTarget.collectAsStateWithLifecycle()
    val wbTarget by viewModel.weeklyBioTarget.collectAsStateWithLifecycle()

    val wpSolved by viewModel.weeklyPhysicsSolved.collectAsStateWithLifecycle()
    val wcSolved by viewModel.weeklyChemSolved.collectAsStateWithLifecycle()
    val wbSolved by viewModel.weeklyBioSolved.collectAsStateWithLifecycle()

    var wpTargetInput by remember(wpTarget) { mutableStateOf(wpTarget.toString()) }
    var wcTargetInput by remember(wcTarget) { mutableStateOf(wcTarget.toString()) }
    var wbTargetInput by remember(wbTarget) { mutableStateOf(wbTarget.toString()) }

    var wpSolvedInput by remember(wpSolved) { mutableStateOf(if (wpSolved > 0) wpSolved.toString() else "") }
    var wcSolvedInput by remember(wcSolved) { mutableStateOf(if (wcSolved > 0) wcSolved.toString() else "") }
    var wbSolvedInput by remember(wbSolved) { mutableStateOf(if (wbSolved > 0) wbSolved.toString() else "") }

    // Dialog States for Goal Addition
    var showGoalDialog by remember { mutableStateOf(false) }
    var physicsGoalText by remember { mutableStateOf("") }
    var chemGoalText by remember { mutableStateOf("") }
    var bioGoalText by remember { mutableStateOf("") }
    var newGoalTargetType by remember { mutableStateOf("today") } // "today" or "weekly"

    // Dialog state for adding/uploading DPPs
    var showAddDppDialog by remember { mutableStateOf(false) }
    var activeDppChapter by remember { mutableStateOf("") }
    var activeDppSubject by remember { mutableStateOf("") }
    var newDppTitleInput by remember { mutableStateOf("") }
    var newDppPdfUriInput by remember { mutableStateOf<String?>(null) }
    var newDppPdfNameInput by remember { mutableStateOf<String?>(null) }
    var editingDppItem by remember { mutableStateOf<DppItem?>(null) }

    // DPP Search state
    var dppSearchQuery by remember { mutableStateOf("") }
    var collapsedDppChapters by remember { mutableStateOf(setOf<String>()) }

    // Intercept back presses when subdialogs, targets modals or revision dialogs are open
    BackHandler(
        enabled = showGoalDialog || showAddDppDialog || showAddChapterRevisionDialog || 
                  showAiMindPlanDialog || scheduleTopicTarget != null || scheduleChapterTarget != null ||
                  editingDppItem != null
    ) {
        when {
            showGoalDialog -> showGoalDialog = false
            showAddDppDialog -> showAddDppDialog = false
            showAddChapterRevisionDialog -> showAddChapterRevisionDialog = false
            showAiMindPlanDialog -> showAiMindPlanDialog = false
            scheduleTopicTarget != null -> scheduleTopicTarget = null
            scheduleChapterTarget != null -> scheduleChapterTarget = null
            editingDppItem != null -> editingDppItem = null
        }
    }

    // File launcher for uploading PDF
    val dppPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileNameFromUri(context, it) ?: "DPP_Attachment.pdf"
            newDppPdfUriInput = it.toString()
            newDppPdfNameInput = fileName
            Toast.makeText(context, "PDF selected: $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDark = isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0x0DFFFFFF) else Color.White
    val cardBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
    val inputBg = if (isDark) Color(0x0DFFFFFF) else Color(0xFFF8FAFC)
    val dialogBg = if (isDark) Color(0xFF000000) else Color.White

    val allChapters = remember(selectedSubject) { getChaptersForSubject(selectedSubject) }
    val filteredChapters = remember(allChapters, dppSearchQuery) {
        if (dppSearchQuery.isBlank()) allChapters
        else allChapters.filter { it.contains(dppSearchQuery, ignoreCase = true) }
    }
    val dppMap = remember(dppItems, selectedSubject) {
        dppItems.filter { it.subject == selectedSubject }.groupBy { it.chapter }
    }

    val todayGoalsList = goals.filter { 
        (it.targetType == "today" || it.targetType.isBlank()) && (it.date.isBlank() || it.date == selectedDate)
    }
    val weeklyGoalsList = goals.filter { it.targetType == "weekly" }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Targets & DPP", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = textColor)
                        Text("Set daily/weekly question goals & manage chapter-wise DPPs.", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // Top Segmented Control (Targets vs DPP vs Revision)
                AppSegmentedControl(
                    items = listOf("Targets", "DPP", "Revision"),
                    selectedItem = mainTab,
                    onItemSelected = { viewModel.setActiveTargetTab(it) },
                    itemLabel = {
                        when (it) {
                            "Targets" -> "Targets"
                            "DPP" -> "DPPs"
                            else -> "Revision"
                        }
                    },
                    itemIcon = {
                        when (it) {
                            "Targets" -> Icons.Default.TrackChanges
                            "DPP" -> Icons.AutoMirrored.Filled.MenuBook
                            else -> Icons.Default.Autorenew
                        }
                    },
                    selectedColor = Color(0xFF6D28D9),
                    selectedGradient = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
                    isDark = isDark,
                    fontSize = 13.5.sp
                )
            }

            // MAIN TAB 1: TARGETS (Today Target vs Weekly Target)
            if (mainTab == "Targets") {
                item {
                    // Sub-tab: Today Target vs Weekly Target
                    AppSegmentedControl(
                        items = listOf("Today Target", "Weekly Target"),
                        selectedItem = targetPeriod,
                        onItemSelected = { targetPeriod = it },
                        itemLabel = {
                            when (it) {
                                "Today Target" -> "Daily Target"
                                else -> "Weekly Target"
                            }
                        },
                        itemEmoji = {
                            when (it) {
                                "Today Target" -> "⚡"
                                else -> "📅"
                            }
                        },
                        selectedColor = Color(0xFF4F46E5),
                        selectedGradient = listOf(Color(0xFF6366F1), Color(0xFF4F46E5)),
                        isDark = isDark,
                        fontSize = 12.5.sp,
                        cornerRadius = 14.dp
                    )
                }

                if (targetPeriod == "Today Target") {
                    // --- DATE SELECTOR & NAVIGATOR CARD ---
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (selectedDate == todayDate) Color(0xFF6366F1).copy(alpha = 0.4f) else Color(0x1AFFFFFF),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Previous day
                                    IconButton(
                                        onClick = { selectedDate = shiftDate(selectedDate, -1) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = "Previous Day",
                                            tint = Color(0xFF818CF8)
                                        )
                                    }

                                    // Center Date display
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = Color(0xFF818CF8),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = if (selectedDate == todayDate) "Today • ${formatDisplayDate(selectedDate)}" else formatDisplayDate(selectedDate),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }

                                        if (selectedDate != todayDate) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                color = Color(0x336366F1),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.clickable { selectedDate = todayDate }
                                            ) {
                                                Text(
                                                    text = "↩ Jump to Today ($todayDate)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF818CF8),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Next day
                                    IconButton(
                                        onClick = { selectedDate = shiftDate(selectedDate, 1) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Next Day",
                                            tint = Color(0xFF818CF8)
                                        )
                                    }
                                }

                                val totT = (pTarget.toIntOrNull() ?: 0) + (cTarget.toIntOrNull() ?: 0) + (bTarget.toIntOrNull() ?: 0)
                                val totS = (pSolved.toIntOrNull() ?: 0) + (cSolved.toIntOrNull() ?: 0) + (bSolved.toIntOrNull() ?: 0)

                                if (totT > 0 && totS >= totT) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = Color(0x2E10B981),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0x4D10B981)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.Celebration, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "🎉 Awesome! Daily Target Completed for this Date ($totS/$totT Qs)",
                                                color = Color(0xFF10B981),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        val pt = pTarget.toIntOrNull() ?: 0
                        val ct = cTarget.toIntOrNull() ?: 0
                        val bt = bTarget.toIntOrNull() ?: 0

                        val ps = pSolved.toIntOrNull() ?: 0
                        val cs = cSolved.toIntOrNull() ?: 0
                        val bs = bSolved.toIntOrNull() ?: 0

                        TodayTargetGraphCard(
                            pTarget = pt,
                            cTarget = ct,
                            bTarget = bt,
                            pSolved = ps,
                            cSolved = cs,
                            bSolved = bs,
                            isDark = isDark,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        DifficultyPracticeAnalyticsCard(
                            currentPractice = currentPractice,
                            allPractices = dailyPractices,
                            isDark = isDark,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    // Question targets per subject (Expandable with Arrow & Save button)
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isTodayTargetsExpanded = !isTodayTargetsExpanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Daily Question Targets (${if (selectedDate == todayDate) "Today" else selectedDate})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                                        Text(if (isTodayTargetsExpanded) "Tap arrow to collapse" else "Tap arrow to expand and edit targets", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                                    }
                                    IconButton(
                                        onClick = { isTodayTargetsExpanded = !isTodayTargetsExpanded }
                                    ) {
                                        Icon(
                                            imageVector = if (isTodayTargetsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Toggle Expand",
                                            tint = Color(0xFF818CF8),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isTodayTargetsExpanded) {
                                    Column {
                                        Spacer(modifier = Modifier.height(16.dp))

                                        TargetQuestionRow(
                                            subject = "Physics ⚡",
                                            targetVal = pTarget,
                                            solvedVal = pSolved,
                                            difficulty = pDifficulty,
                                            onDifficultyChange = { pDifficulty = it },
                                            color = Color(0xFF60A5FA),
                                            onTargetChange = { pTarget = it },
                                            onSolvedChange = { pSolved = it },
                                            onSave = {
                                                viewModel.saveDailyPractice(
                                                    selectedDate,
                                                    pTarget.toIntOrNull() ?: 0,
                                                    cTarget.toIntOrNull() ?: 0,
                                                    bTarget.toIntOrNull() ?: 0,
                                                    pSolved.toIntOrNull() ?: 0,
                                                    cSolved.toIntOrNull() ?: 0,
                                                    bSolved.toIntOrNull() ?: 0,
                                                    pDifficulty,
                                                    cDifficulty,
                                                    bDifficulty
                                                )
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        TargetQuestionRow(
                                            subject = "Chemistry 🧪",
                                            targetVal = cTarget,
                                            solvedVal = cSolved,
                                            difficulty = cDifficulty,
                                            onDifficultyChange = { cDifficulty = it },
                                            color = Color(0xFFF59E0B),
                                            onTargetChange = { cTarget = it },
                                            onSolvedChange = { cSolved = it },
                                            onSave = {
                                                viewModel.saveDailyPractice(
                                                    selectedDate,
                                                    pTarget.toIntOrNull() ?: 0,
                                                    cTarget.toIntOrNull() ?: 0,
                                                    bTarget.toIntOrNull() ?: 0,
                                                    pSolved.toIntOrNull() ?: 0,
                                                    cSolved.toIntOrNull() ?: 0,
                                                    bSolved.toIntOrNull() ?: 0,
                                                    pDifficulty,
                                                    cDifficulty,
                                                    bDifficulty
                                                )
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        TargetQuestionRow(
                                            subject = "Biology 🧬",
                                            targetVal = bTarget,
                                            solvedVal = bSolved,
                                            difficulty = bDifficulty,
                                            onDifficultyChange = { bDifficulty = it },
                                            color = Color(0xFF34D399),
                                            onTargetChange = { bTarget = it },
                                            onSolvedChange = { bSolved = it },
                                            onSave = {
                                                viewModel.saveDailyPractice(
                                                    selectedDate,
                                                    pTarget.toIntOrNull() ?: 0,
                                                    cTarget.toIntOrNull() ?: 0,
                                                    bTarget.toIntOrNull() ?: 0,
                                                    pSolved.toIntOrNull() ?: 0,
                                                    cSolved.toIntOrNull() ?: 0,
                                                    bSolved.toIntOrNull() ?: 0,
                                                    pDifficulty,
                                                    cDifficulty,
                                                    bDifficulty
                                                )
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(20.dp))

                                         // Prominent Save Button
                                        Button(
                                            onClick = {
                                                val ptVal = pTarget.toIntOrNull() ?: 0
                                                val ctVal = cTarget.toIntOrNull() ?: 0
                                                val btVal = bTarget.toIntOrNull() ?: 0
                                                val psVal = pSolved.toIntOrNull() ?: 0
                                                val csVal = cSolved.toIntOrNull() ?: 0
                                                val bsVal = bSolved.toIntOrNull() ?: 0

                                                viewModel.saveDailyPractice(
                                                    selectedDate,
                                                    ptVal,
                                                    ctVal,
                                                    btVal,
                                                    psVal,
                                                    csVal,
                                                    bsVal,
                                                    pDifficulty,
                                                    cDifficulty,
                                                    bDifficulty
                                                )

                                                val totalSolvedNow = psVal + csVal + bsVal
                                                if (totalSolvedNow > 0) {
                                                    viewModel.addStudyLog("Daily MCQs", "Saved Daily Target for $selectedDate: $totalSolvedNow solved ($pDifficulty/$cDifficulty/$bDifficulty)", 1)
                                                }

                                                Toast.makeText(context, "Target for $selectedDate Saved Successfully! ✅", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Save Target for $selectedDate", fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        // Saved Target Logs & History List
                                        val historyList = dailyPractices.sortedByDescending { it.date }
                                        Text(
                                            text = "📋 Saved Target Logs History",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (historyList.isEmpty()) {
                                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                                Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("No target logs saved yet. Fill numbers above & tap 'Save Target'.", color = subTextColor, fontSize = 12.sp)
                                                }
                                            }
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                historyList.take(5).forEach { item ->
                                                    val itemTotT = item.physicsTarget + item.chemistryTarget + item.biologyTarget
                                                    val itemTotS = item.physicsSolved + item.chemistrySolved + item.biologySolved
                                                    val pctVal = if (itemTotT > 0) ((itemTotS.toFloat() / itemTotT.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

                                                    GlassCard(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { selectedDate = item.date }
                                                    ) {
                                                        Column(modifier = Modifier.padding(14.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.weight(1f, fill = false)
                                                                ) {
                                                                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text(
                                                                        text = if (item.date == todayDate) "Today's Log (${item.date})" else "Saved: ${item.date}",
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = textColor,
                                                                        fontSize = 12.5.sp,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .background(if (pctVal >= 100) Color(0x3310B981) else Color(0x336366F1))
                                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "$itemTotS/$itemTotT Qs ($pctVal%)",
                                                                        color = if (pctVal >= 100) Color(0xFF34D399) else Color(0xFF818CF8),
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 10.5.sp,
                                                                        maxLines = 1,
                                                                        softWrap = false
                                                                    )
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text("⚡ Phy: ${item.physicsSolved}/${item.physicsTarget} (${item.physicsDifficulty})", fontSize = 10.5.sp, color = Color(0xFF60A5FA), fontWeight = FontWeight.SemiBold)
                                                                Text("🧪 Chem: ${item.chemistrySolved}/${item.chemistryTarget} (${item.chemistryDifficulty})", fontSize = 10.5.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold)
                                                                Text("🧬 Bio: ${item.biologySolved}/${item.biologyTarget} (${item.biologyDifficulty})", fontSize = 10.5.sp, color = Color(0xFF34D399), fontWeight = FontWeight.SemiBold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Today Goals Checklist
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Today's Checkable Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                            TextButton(onClick = {
                                newGoalTargetType = "today"
                                showGoalDialog = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Goal", color = Color(0xFF818CF8))
                            }
                        }
                    }

                    if (todayGoalsList.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No specific goals set for today. Tap '+ Add Goal' to create one!", color = subTextColor, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        items(todayGoalsList, key = { it.id }) { goal ->
                            GoalItemCard(goal = goal, viewModel = viewModel)
                        }
                    }
                } else {
                    // --- WEEKLY TARGET CONTENT ---
                    item {
                        val wpt = wpTargetInput.toIntOrNull() ?: 300
                        val wct = wcTargetInput.toIntOrNull() ?: 300
                        val wbt = wbTargetInput.toIntOrNull() ?: 600

                        val wps = wpSolvedInput.toIntOrNull() ?: 0
                        val wcs = wcSolvedInput.toIntOrNull() ?: 0
                        val wbs = wbSolvedInput.toIntOrNull() ?: 0

                        WeeklyTargetGraphCard(
                            wpTarget = wpt,
                            wcTarget = wct,
                            wbTarget = wbt,
                            wpSolved = wps,
                            wcSolved = wcs,
                            wbSolved = wbs,
                            dailyPractices = dailyPractices,
                            isDark = isDark,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    // Weekly Question Targets Card (Expandable with Arrow & Save button)
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isWeeklyTargetsExpanded = !isWeeklyTargetsExpanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Weekly Question Targets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                                        Text(if (isWeeklyTargetsExpanded) "Tap arrow to collapse" else "Tap arrow to expand and set weekly targets", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                                    }
                                    IconButton(
                                        onClick = { isWeeklyTargetsExpanded = !isWeeklyTargetsExpanded }
                                    ) {
                                        Icon(
                                            imageVector = if (isWeeklyTargetsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Toggle Expand",
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isWeeklyTargetsExpanded) {
                                    Column {
                                        Spacer(modifier = Modifier.height(16.dp))

                                        TargetQuestionRow(
                                            subject = "Physics (Weekly) ⚡",
                                            targetVal = wpTargetInput,
                                            solvedVal = wpSolvedInput,
                                            color = Color(0xFF60A5FA),
                                            onTargetChange = { wpTargetInput = it },
                                            onSolvedChange = { wpSolvedInput = it },
                                            onSave = {
                                                viewModel.saveWeeklyPractice(
                                                    wpTargetInput.toIntOrNull() ?: 300,
                                                    wcTargetInput.toIntOrNull() ?: 300,
                                                    wbTargetInput.toIntOrNull() ?: 600,
                                                    wpSolvedInput.toIntOrNull() ?: 0,
                                                    wcSolvedInput.toIntOrNull() ?: 0,
                                                    wbSolvedInput.toIntOrNull() ?: 0
                                                )
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        TargetQuestionRow(
                                            subject = "Chemistry (Weekly) 🧪",
                                            targetVal = wcTargetInput,
                                            solvedVal = wcSolvedInput,
                                            color = Color(0xFFF59E0B),
                                            onTargetChange = { wcTargetInput = it },
                                            onSolvedChange = { wcSolvedInput = it },
                                            onSave = {
                                                viewModel.saveWeeklyPractice(
                                                    wpTargetInput.toIntOrNull() ?: 300,
                                                    wcTargetInput.toIntOrNull() ?: 300,
                                                    wbTargetInput.toIntOrNull() ?: 600,
                                                    wpSolvedInput.toIntOrNull() ?: 0,
                                                    wcSolvedInput.toIntOrNull() ?: 0,
                                                    wbSolvedInput.toIntOrNull() ?: 0
                                                )
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        TargetQuestionRow(
                                            subject = "Biology (Weekly) 🧬",
                                            targetVal = wbTargetInput,
                                            solvedVal = wbSolvedInput,
                                            color = Color(0xFF34D399),
                                            onTargetChange = { wbTargetInput = it },
                                            onSolvedChange = { wbSolvedInput = it },
                                            onSave = {
                                                viewModel.saveWeeklyPractice(
                                                    wpTargetInput.toIntOrNull() ?: 300,
                                                    wcTargetInput.toIntOrNull() ?: 300,
                                                    wbTargetInput.toIntOrNull() ?: 600,
                                                    wpSolvedInput.toIntOrNull() ?: 0,
                                                    wcSolvedInput.toIntOrNull() ?: 0,
                                                    wbSolvedInput.toIntOrNull() ?: 0
                                                )
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(20.dp))

                                        // Prominent Save Button
                                        Button(
                                            onClick = {
                                                val wpt = wpTargetInput.toIntOrNull() ?: 300
                                                val wct = wcTargetInput.toIntOrNull() ?: 300
                                                val wbt = wbTargetInput.toIntOrNull() ?: 600
                                                val wps = wpSolvedInput.toIntOrNull() ?: 0
                                                val wcs = wcSolvedInput.toIntOrNull() ?: 0
                                                val wbs = wbSolvedInput.toIntOrNull() ?: 0

                                                viewModel.saveWeeklyPractice(wpt, wct, wbt, wps, wcs, wbs)

                                                val totWeeklySolved = wps + wcs + wbs
                                                val totWeeklyTarget = wpt + wct + wbt
                                                val pctVal = if (totWeeklyTarget > 0) ((totWeeklySolved.toFloat() / totWeeklyTarget.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

                                                if (totWeeklySolved > 0) {
                                                    viewModel.addStudyLog(
                                                        "Weekly MCQs",
                                                        "Weekly Practice Target: $totWeeklySolved/$totWeeklyTarget solved (P:$wps/$wpt, C:$wcs/$wct, B:$wbs/$wbt)",
                                                        1
                                                    )
                                                }

                                                Toast.makeText(context, "Weekly Target Saved & Logged Successfully! ✅", Toast.LENGTH_SHORT).show()
                                                isWeeklyTargetsExpanded = false
                                            },
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Save Weekly Target", fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        // Saved Weekly Target Logs & History (Integrated inside Card)
                                        val wpt = wpTargetInput.toIntOrNull() ?: 300
                                        val wct = wcTargetInput.toIntOrNull() ?: 300
                                        val wbt = wbTargetInput.toIntOrNull() ?: 600
                                        val wps = wpSolvedInput.toIntOrNull() ?: 0
                                        val wcs = wcSolvedInput.toIntOrNull() ?: 0
                                        val wbs = wbSolvedInput.toIntOrNull() ?: 0

                                        val totWeeklyTarget = wpt + wct + wbt
                                        val totWeeklySolved = wps + wcs + wbs
                                        val pctVal = if (totWeeklyTarget > 0) ((totWeeklySolved.toFloat() / totWeeklyTarget.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

                                        Text(
                                            text = "📋 Saved Weekly Target Log",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("📅 Active Weekly Target Summary", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                                                        Text("$totWeeklySolved / $totWeeklyTarget MCQs Solved", fontSize = 12.sp, color = subTextColor)
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(20.dp),
                                                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            "$pctVal% Completed",
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF10B981)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))
                                                FireworksProgressBar(
                                                    progress = (pctVal / 100f).coerceIn(0f, 1f),
                                                    height = 6.dp,
                                                    gradientColors = listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF00F5A0)),
                                                    sparkColor = Color(0xFF34D399),
                                                    isDark = isDark
                                                )

                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Physics: $wps/$wpt", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))
                                                    Text("Chem: $wcs/$wct", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                                    Text("Bio: $wbs/$wbt", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Text(
                                            text = "📜 Weekly Practice Activity Logs History",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        val weeklyLogs = studyLogs.filter { log ->
                                            log.subject.contains("Weekly", ignoreCase = true) || log.chapter.contains("Weekly", ignoreCase = true)
                                        }.sortedByDescending { it.timestamp }

                                        if (weeklyLogs.isEmpty()) {
                                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                                Box(modifier = Modifier.padding(14.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Text("No weekly target logs recorded yet. Fill numbers above & tap 'Save Weekly Target'.", color = subTextColor, fontSize = 12.sp)
                                                }
                                            }
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                weeklyLogs.take(5).forEach { log ->
                                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                                        Row(
                                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(log.subject, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                                                                Text(log.chapter, fontSize = 12.sp, color = subTextColor)
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(log.timestamp)),
                                                                fontSize = 11.sp,
                                                                color = subTextColor
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Weekly Strategy Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                            TextButton(onClick = {
                                newGoalTargetType = "weekly"
                                showGoalDialog = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Weekly Goal", color = Color(0xFF818CF8))
                            }
                        }
                    }

                    if (weeklyGoalsList.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No weekly targets added yet. E.g. 'Solve 300 Physics MCQs' or 'Complete 2 mock tests'.", color = subTextColor, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        items(weeklyGoalsList, key = { it.id }) { goal ->
                            GoalItemCard(goal = goal, viewModel = viewModel)
                        }
                    }
                }
            }

            // MAIN TAB 2: DPP SECTION (Physics, Chemistry, Biology / Maths)
            if (mainTab == "DPP") {
                item {
                    // Subject Selector Tabs
                    if (availableSubjects.size <= 4) {
                        AppSegmentedControl(
                            items = availableSubjects,
                            selectedItem = selectedSubject,
                            onItemSelected = { selectedSubject = it },
                            itemLabel = { it },
                            itemEmoji = {
                                when {
                                    it.contains("Physics", ignoreCase = true) -> "⚡"
                                    it.contains("Chemistry", ignoreCase = true) -> "🧪"
                                    else -> "🧬"
                                }
                            },
                            selectedColor = when {
                                selectedSubject.contains("Physics", ignoreCase = true) -> Color(0xFF2563EB)
                                selectedSubject.contains("Chemistry", ignoreCase = true) -> Color(0xFFD97706)
                                else -> Color(0xFF059669)
                            },
                            selectedGradient = when {
                                selectedSubject.contains("Physics", ignoreCase = true) -> listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                                selectedSubject.contains("Chemistry", ignoreCase = true) -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                else -> listOf(Color(0xFF10B981), Color(0xFF059669))
                            },
                            isDark = isDark
                        )
                    } else {
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableSubjects.size) { idx ->
                                val s = availableSubjects[idx]
                                val isSelected = selectedSubject == s
                                Surface(
                                    onClick = { selectedSubject = s },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF6366F1) else (if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF818CF8) else (if (isDark) Color(0x1FFFFFFF) else Color(0xFFE2E8F0))
                                    )
                                ) {
                                    Text(
                                        s,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else textColor
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Search Bar & Completion Status Bar
                    val chapters = getChaptersForSubject(selectedSubject, activeExamGoal.name)
                    val subjectDpps = dppItems.filter { it.subject == selectedSubject }
                    val totalDppsCreated = subjectDpps.size
                    val completedDppsCount = subjectDpps.count { it.isCompleted }
                    val pct = if (totalDppsCreated > 0) (completedDppsCount * 100) / totalDppsCreated else 0

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$selectedSubject Chapter DPPs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                                Text("$completedDppsCount / $totalDppsCreated DPPs Done ($pct%)", style = MaterialTheme.typography.labelMedium, color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val dppProgress = if (totalDppsCreated > 0) completedDppsCount.toFloat() / totalDppsCreated.toFloat() else 0f
                            FireworksProgressBar(
                                progress = dppProgress,
                                height = 8.dp,
                                gradientColors = listOf(Color(0xFF6366F1), Color(0xFF818CF8), Color(0xFFA78BFA)),
                                sparkColor = Color(0xFF818CF8),
                                isDark = isDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = dppSearchQuery,
                                onValueChange = { dppSearchQuery = it },
                                placeholder = { Text("Search chapter name...", color = subTextColor, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = subTextColor) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF818CF8),
                                    unfocusedBorderColor = if (isDark) Color(0x1AFFFFFF) else Color(0xFFCBD5E1),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor
                                ),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true
                            )
                        }
                    }
                }

                items(filteredChapters, key = { it }) { chapterName ->
                    val chapterDpps = dppMap[chapterName] ?: emptyList()
                    val chapterCompletedCount = chapterDpps.count { it.isCompleted }
                    val isExpanded = !collapsedDppChapters.contains(chapterName)

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (chapterDpps.isNotEmpty() && chapterCompletedCount == chapterDpps.size) Color(0x4D10B981) else Color(0x1AFFFFFF),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f).clickable {
                                        collapsedDppChapters = if (isExpanded) {
                                            collapsedDppChapters + chapterName
                                        } else {
                                            collapsedDppChapters - chapterName
                                        }
                                    }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = chapterName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                        Text(
                                            text = if (chapterDpps.isEmpty()) "No DPPs added yet" else "${chapterDpps.size} DPP(s) • $chapterCompletedCount Done",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (chapterDpps.isNotEmpty() && chapterCompletedCount == chapterDpps.size) Color(0xFF34D399) else subTextColor
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = subTextColor,
                                        modifier = Modifier.size(24.dp).padding(end = 8.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        activeDppSubject = selectedSubject
                                        activeDppChapter = chapterName
                                        newDppTitleInput = "DPP ${String.format(Locale.getDefault(), "%02d", chapterDpps.size + 1)}"
                                        newDppPdfUriInput = null
                                        newDppPdfNameInput = null
                                        editingDppItem = null
                                        showAddDppDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Upload DPP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = cardBorder)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (chapterDpps.isEmpty()) {
                                        Surface(
                                            color = inputBg,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                activeDppSubject = selectedSubject
                                                activeDppChapter = chapterName
                                                newDppTitleInput = "DPP 01"
                                                newDppPdfUriInput = null
                                                newDppPdfNameInput = null
                                                editingDppItem = null
                                                showAddDppDialog = true
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "Tap to add DPP 1, DPP 2... for $chapterName",
                                                    fontSize = 12.sp,
                                                    color = subTextColor
                                                )
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            chapterDpps.forEach { dpp ->
                                                Surface(
                                                    color = if (dpp.isCompleted) Color(0x1A10B981) else inputBg,
                                                    shape = RoundedCornerShape(14.dp),
                                                    border = BorderStroke(1.dp, if (dpp.isCompleted) Color(0x3310B981) else cardBorder),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Checkbox(
                                                                checked = dpp.isCompleted,
                                                                onCheckedChange = { viewModel.toggleDppItemCompletion(dpp) },
                                                                colors = CheckboxDefaults.colors(
                                                                    checkedColor = Color(0xFF10B981),
                                                                    uncheckedColor = subTextColor,
                                                                    checkmarkColor = Color.White
                                                                )
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = dpp.dppName,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 13.sp,
                                                                    color = if (dpp.isCompleted) Color(0xFF34D399) else textColor,
                                                                    textDecoration = if (dpp.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                                )
                                                                if (!dpp.pdfFileName.isNullOrEmpty()) {
                                                                    Text(
                                                                        text = "📄 ${dpp.pdfFileName}",
                                                                        fontSize = 11.sp,
                                                                        color = Color(0xFF818CF8),
                                                                        maxLines = 1
                                                                    )
                                                                }
                                                                if (dpp.isCompleted && !dpp.completedDate.isNullOrBlank()) {
                                                                    Text(
                                                                        text = "✓ Completed on ${dpp.completedDate}",
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = Color(0xFF10B981).copy(alpha = 0.9f)
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            if (!dpp.pdfUri.isNullOrEmpty()) {
                                                                IconButton(
                                                                    onClick = {
                                                                        try {
                                                                            val uri = Uri.parse(dpp.pdfUri)
                                                                            viewModel.openPdfInReader(uri, dpp.pdfFileName ?: "${dpp.dppName} DPP")
                                                                            if (onNavigate != null) {
                                                                                onNavigate("pdf_viewer")
                                                                            } else {
                                                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                                                    setDataAndType(uri, "application/pdf")
                                                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                                }
                                                                                context.startActivity(Intent.createChooser(intent, "Open DPP PDF"))
                                                                            }
                                                                        } catch (e: Exception) {
                                                                            try {
                                                                                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(dpp.pdfUri)).apply {
                                                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                                }
                                                                                context.startActivity(fallbackIntent)
                                                                            } catch (ex: Exception) {
                                                                                Toast.makeText(context, "No app found to open PDF: ${dpp.pdfFileName ?: "file"}", Toast.LENGTH_SHORT).show()
                                                                            }
                                                                        }
                                                                    },
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open PDF", tint = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                                                                }
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    activeDppSubject = dpp.subject
                                                                    activeDppChapter = dpp.chapter
                                                                    newDppTitleInput = dpp.dppName
                                                                    newDppPdfUriInput = dpp.pdfUri
                                                                    newDppPdfNameInput = dpp.pdfFileName
                                                                    editingDppItem = dpp
                                                                    showAddDppDialog = true
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(Icons.Default.Edit, contentDescription = "Edit DPP", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    viewModel.deleteDppItem(dpp.id)
                                                                    Toast.makeText(context, "DPP deleted", Toast.LENGTH_SHORT).show()
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(Icons.Default.Delete, contentDescription = "Delete DPP", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (mainTab == "Revision") {
                item {
                    val now = System.currentTimeMillis()
                    
                    // All completed topics and chapters added to revision queue
                    val eligibleRevisionTopics = remember(completedTopics) {
                        completedTopics
                    }

                    // Stats with Custom Date support
                    val urgentTopics = remember(eligibleRevisionTopics, now) {
                        eligibleRevisionTopics.filter { topic ->
                            if (topic.customRevisionDate != null) {
                                topic.customRevisionDate < now
                            } else {
                                (now - topic.timestamp) >= 14 * 86400000L
                            }
                        }
                    }
                    val dueTopics = remember(eligibleRevisionTopics, now) {
                        eligibleRevisionTopics.filter { topic ->
                            if (topic.customRevisionDate != null) {
                                val d = (topic.customRevisionDate - now) / 86400000L
                                d in -1..0
                            } else {
                                val d = (now - topic.timestamp) / 86400000L
                                d in 3..13
                            }
                        }
                    }
                    val allDueTopics = remember(eligibleRevisionTopics, now) {
                        eligibleRevisionTopics.filter { topic ->
                            if (topic.customRevisionDate != null) {
                                topic.customRevisionDate <= (now + 86400000L)
                            } else {
                                (now - topic.timestamp) >= 3 * 86400000L
                            }
                        }
                    }
                    val scheduledTopics = remember(eligibleRevisionTopics) {
                        eligibleRevisionTopics.filter { it.customRevisionDate != null }
                    }
                    val freshTopics = remember(eligibleRevisionTopics, now) {
                        eligibleRevisionTopics.filter { topic ->
                            if (topic.customRevisionDate != null) {
                                topic.customRevisionDate > (now + 86400000L)
                            } else {
                                (now - topic.timestamp) < 3 * 86400000L
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // 1. Hero Revision Card
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.2.dp,
                                    Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1), Color(0xFFEC4899))),
                                    RoundedCornerShape(24.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF6366F1)))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🧠", fontSize = 22.sp)
                                        }
                                        Column {
                                            Text(
                                                "Spaced Repetition Engine",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = textColor
                                            )
                                            Text(
                                                "Ebbinghaus Memory Cycle: 3d • 7d • 14d reviews",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = subTextColor
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 4-Tile Metric Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Urgent
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isDark) Color(0x33EF4444) else Color(0xFFFEF2F2))
                                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                            .padding(vertical = 10.dp, horizontal = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${urgentTopics.size}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFFEF4444))
                                            Text("🚨 Urgent", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626))
                                            Text(">14d ago", fontSize = 8.5.sp, color = subTextColor)
                                        }
                                    }

                                    // Due Now
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isDark) Color(0x33F59E0B) else Color(0xFFFFFBEB))
                                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                            .padding(vertical = 10.dp, horizontal = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${dueTopics.size}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFFF59E0B))
                                            Text("⚡ Due Now", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFFDE68A) else Color(0xFFD97706))
                                            Text("3-13d ago", fontSize = 8.5.sp, color = subTextColor)
                                        }
                                    }

                                    // Fresh / Retained
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isDark) Color(0x3310B981) else Color(0xFFECFDF5))
                                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                            .padding(vertical = 10.dp, horizontal = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${freshTopics.size}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF10B981))
                                            Text("🟢 Fresh", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669))
                                            Text("<3d ago", fontSize = 8.5.sp, color = subTextColor)
                                        }
                                    }

                                    // Total in Loop
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isDark) Color(0x336366F1) else Color(0xFFEEF2FF))
                                            .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                            .padding(vertical = 10.dp, horizontal = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${eligibleRevisionTopics.size}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF6366F1))
                                            Text("📚 In Loop", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5))
                                            Text("Tracked", fontSize = 8.5.sp, color = subTextColor)
                                        }
                                    }
                                }

                                if (allDueTopics.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            allDueTopics.forEach { topic ->
                                                viewModel.markTopicRevised(topic.topicId)
                                            }
                                            Toast.makeText(context, "All ${allDueTopics.size} Due Topics Marked Revised! 🎉", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 42.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                                                ),
                                                RoundedCornerShape(14.dp)
                                            )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("⚡ Mark All Due as Revised (${allDueTopics.size}) ✓", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Subject Filter Tabs
                        val revisionSubjects = listOf("All") + availableSubjects
                        AppSegmentedControl(
                            items = revisionSubjects,
                            selectedItem = revisionSubjectFilter,
                            onItemSelected = { revisionSubjectFilter = it },
                            itemLabel = { it },
                            itemIcon = {
                                when {
                                    it == "All" -> Icons.Default.Public
                                    it.contains("Physics", ignoreCase = true) -> Icons.Default.Bolt
                                    it.contains("Chemistry", ignoreCase = true) -> Icons.Default.Science
                                    else -> Icons.Default.Biotech
                                }
                            },
                            selectedColor = Color(0xFF7C3AED),
                            selectedGradient = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
                            isDark = isDark,
                            fontSize = 12.sp,
                            cornerRadius = 14.dp
                        )

                        // 3. Urgency Filter Chips & Search Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                "All" to "All (${eligibleRevisionTopics.size})",
                                "Due" to "🚨 Due (${allDueTopics.size})",
                                "Urgent" to "⚠️ >14d (${urgentTopics.size})",
                                "Fresh" to "🟢 Fresh (${freshTopics.size})"
                            ).forEach { (key, label) ->
                                val isSelected = revisionUrgencyFilter == key
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Color(0xFF6366F1) else (if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF6366F1) else cardBorder),
                                    modifier = Modifier
                                        .clickable { revisionUrgencyFilter = key }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = if (isSelected) Color.White else textColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Search Input & Add Chapter Button Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = revisionSearchQuery,
                                onValueChange = { revisionSearchQuery = it },
                                placeholder = { Text("Search chapter or subtopic in revision...", color = subTextColor, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = subTextColor, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (revisionSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { revisionSearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = subTextColor, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = cardBorder,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = { showAddChapterRevisionDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.White)
                                }
                            }
                        }

                        // 4. Topic List Filtered and Grouped By Chapter
                        val filteredList = remember(eligibleRevisionTopics, revisionSubjectFilter, revisionUrgencyFilter, revisionSearchQuery, now) {
                            eligibleRevisionTopics.filter { topic ->
                                val daysAgo = (now - topic.timestamp) / 86400000L
                                val parts = topic.topicId.split("-", limit = 2)
                                val chapterName = parts.getOrNull(0) ?: ""
                                val topicName = parts.getOrNull(1) ?: topic.topicId
                                val subject = getSubjectForChapter(chapterName, activeExamGoal.name)

                                val matchesSubject = revisionSubjectFilter == "All" || subject.equals(revisionSubjectFilter, ignoreCase = true)
                                val matchesUrgency = when (revisionUrgencyFilter) {
                                    "Due" -> {
                                        if (topic.customRevisionDate != null) topic.customRevisionDate <= (now + 86400000L)
                                        else daysAgo >= 3
                                    }
                                    "Scheduled" -> topic.customRevisionDate != null
                                    "Urgent" -> {
                                        if (topic.customRevisionDate != null) topic.customRevisionDate < now
                                        else daysAgo >= 14
                                    }
                                    "Fresh" -> {
                                        if (topic.customRevisionDate != null) topic.customRevisionDate > (now + 86400000L)
                                        else daysAgo < 3
                                    }
                                    else -> true
                                }
                                val matchesSearch = revisionSearchQuery.isBlank() ||
                                        chapterName.contains(revisionSearchQuery, ignoreCase = true) ||
                                        topicName.contains(revisionSearchQuery, ignoreCase = true)

                                matchesSubject && matchesUrgency && matchesSearch
                            }.sortedWith(compareBy<com.example.data.CompletedTopic> {
                                it.customRevisionDate ?: (it.timestamp + 3 * 86400000L)
                            })
                        }

                        val groupedChapters = remember(filteredList, activeExamGoal) {
                            filteredList.groupBy { topic ->
                                val parts = topic.topicId.split("-", limit = 2)
                                parts.getOrNull(0) ?: "General"
                            }.map { (chapterName, topicList) ->
                                val subject = getSubjectForChapter(chapterName, activeExamGoal.name)
                                ChapterRevisionGroup(
                                    chapterName = chapterName,
                                    subject = subject,
                                    topics = topicList
                                )
                            }
                        }

                        if (eligibleRevisionTopics.isEmpty()) {
                            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("📚", fontSize = 36.sp)
                                    Text(
                                        "No Topics in Revision Queue",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(
                                        "No topics in your Revision queue yet. Tap '➕ Add Chapter to Revision' to add any chapter and subtopics into your Spaced Repetition queue, or mark topics completed in your targets!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = subTextColor,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                    Button(
                                        onClick = { showAddChapterRevisionDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("➕ Add Chapter to Revision", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        } else if (groupedChapters.isEmpty()) {
                            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🎉", fontSize = 36.sp)
                                    Text(
                                        "All Caught Up!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(
                                        "No topics match the selected filters or all matching topics are fresh in memory.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = subTextColor,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Chapters in Revision (${groupedChapters.size} chapters • ${filteredList.size} subtopics)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = textColor,
                                    fontSize = 13.sp
                                )

                                TextButton(
                                    onClick = {
                                        collapsedChapters = if (collapsedChapters.isEmpty()) {
                                            groupedChapters.map { it.chapterName }.toSet()
                                        } else {
                                            emptySet()
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        if (collapsedChapters.isEmpty()) "Collapse All" else "Expand All",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6366F1)
                                    )
                                }
                            }

                            val aiMindSuggestions = remember(groupedChapters, now) {
                                computeAiMindSuggestions(groupedChapters, now)
                            }

                            // ─── AI MIND ZERO-PRESSURE AUTO-SCHEDULER BANNER ───
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.2.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF8B5CF6).copy(alpha = 0.18f),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("🧠", fontSize = 18.sp)
                                                }
                                            }
                                            Column {
                                                Text(
                                                    "AI Mind Spaced Scheduler",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp,
                                                    color = textColor
                                                )
                                                Text(
                                                    "Zero-Pressure • Balances Revision with Syllabus",
                                                    fontSize = 10.5.sp,
                                                    color = subTextColor
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "🌿 Light Load",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        "AI automatically spreads chapter revisions across upcoming days (max 1–2 chapters/day) so your brain stays fresh while completing new chapters.",
                                        fontSize = 11.5.sp,
                                        color = subTextColor,
                                        lineHeight = 16.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val batch = aiMindSuggestions.mapValues { it.value.suggestedDateMillis }
                                                viewModel.applyAiMindBatchSchedule(batch)
                                                Toast.makeText(context, "✨ AI Mind scheduled ${batch.size} chapters across upcoming days! 🧠🎯", Toast.LENGTH_LONG).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1.3f)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Auto-Schedule All", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        OutlinedButton(
                                            onClick = { showAiMindPlanDialog = true },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("View AI Plan", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9))
                                        }
                                    }
                                }
                            }

                            groupedChapters.forEach { group ->
                                val isExpanded = !collapsedChapters.contains(group.chapterName)
                                val chapterUrgentCount = group.topics.count {
                                    if (it.customRevisionDate != null) it.customRevisionDate < now else (now - it.timestamp) >= 14 * 86400000L
                                }
                                val chapterDueCount = group.topics.count {
                                    if (it.customRevisionDate != null) {
                                        val d = (it.customRevisionDate - now) / 86400000L
                                        d in -1..0
                                    } else {
                                        val d = (now - it.timestamp) / 86400000L
                                        d in 3..13
                                    }
                                }
                                val chapterFreshTopics = remember(group.topics, now) {
                                    group.topics.filter {
                                        if (it.customRevisionDate != null) it.customRevisionDate > (now + 86400000L) else (now - it.timestamp) < 3 * 86400000L
                                    }
                                }
                                val chapterTotalDue = chapterUrgentCount + chapterDueCount
                                val isChapterAllDone = chapterTotalDue == 0 && group.topics.isNotEmpty()

                                val subjectColor = when {
                                    group.subject.contains("Physics", ignoreCase = true) -> Color(0xFF2563EB)
                                    group.subject.contains("Chemistry", ignoreCase = true) -> Color(0xFFD97706)
                                    group.subject.contains("Math", ignoreCase = true) -> Color(0xFF7C3AED)
                                    else -> Color(0xFF059669)
                                }

                                val chapterBorderColor = when {
                                    isChapterAllDone -> Color(0xFF10B981).copy(alpha = 0.7f)
                                    chapterUrgentCount > 0 -> Color(0xFFEF4444).copy(alpha = 0.6f)
                                    chapterDueCount > 0 -> Color(0xFFF59E0B).copy(alpha = 0.5f)
                                    else -> cardBorder
                                }

                                val retentionRate = if (group.topics.isNotEmpty()) chapterFreshTopics.size.toFloat() / group.topics.size.toFloat() else 0f

                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.2.dp, chapterBorderColor, RoundedCornerShape(20.dp))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        // ─── CHAPTER HEADER (Chapter Name prominently displayed) ───
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        collapsedChapters = if (isExpanded) {
                                                            collapsedChapters + group.chapterName
                                                        } else {
                                                            collapsedChapters - group.chapterName
                                                        }
                                                    }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Subject Badge
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = subjectColor.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = group.subject,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = subjectColor,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    
                                                    if (isChapterAllDone) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFF10B981).copy(alpha = 0.18f),
                                                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                                                        ) {
                                                            Text(
                                                                text = "Done ✔️",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = Color(0xFF10B981),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    } else if (chapterUrgentCount > 0) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = "🚨 $chapterUrgentCount urgent",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = Color(0xFFEF4444),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    } else if (chapterDueCount > 0) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = "⚡ $chapterDueCount due",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = Color(0xFFF59E0B),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    } else {
                                                         Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = "🟢 Fresh",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = Color(0xFF10B981),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                
                                                // CHAPTER NAME
                                                Text(
                                                    text = "📖 ${group.chapterName}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isChapterAllDone) (if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)) else textColor,
                                                    fontSize = 15.sp,
                                                    lineHeight = 20.sp
                                                )
                                            }
                                            
                                            // Action Buttons (Schedule & Done Toggle Button)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isDark) Color(0x256366F1) else Color(0xFFEEF2FF),
                                                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
                                                    modifier = Modifier.clickable {
                                                        scheduleChapterTarget = group
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(Icons.Default.DateRange, contentDescription = "Schedule", tint = Color(0xFF6366F1), modifier = Modifier.size(13.dp))
                                                        Text("Schedule", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                                                    }
                                                }

                                                if (isChapterAllDone) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = Color(0xFF10B981),
                                                        modifier = Modifier.clickable {
                                                            viewModel.toggleChapterRevisionDone(group.chapterName, true)
                                                            Toast.makeText(context, "Marked '${group.chapterName}' as Pending for review ⏳", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(13.dp))
                                                            Text("Done ✔️", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        }
                                                    }
                                                } else {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = Color(0xFF6366F1),
                                                        modifier = Modifier.clickable {
                                                            viewModel.toggleChapterRevisionDone(group.chapterName, false)
                                                            Toast.makeText(context, "Marked '${group.chapterName}' as Done ✔️! 🎉", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(Icons.Default.CheckCircle, contentDescription = "Mark Done", tint = Color.White, modifier = Modifier.size(13.dp))
                                                            Text("Done ✔️", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // ─── PROGRESS & RETENTION BAR ───
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isChapterAllDone) "🟢 Done ✔️ (All ${group.topics.size} subtopics fresh)" else "${chapterFreshTopics.size}/${group.topics.size} subtopics fresh in memory",
                                                fontSize = 11.sp,
                                                color = if (isChapterAllDone) Color(0xFF10B981) else subTextColor,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${(retentionRate * 100).toInt()}%",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (retentionRate >= 1.0f) Color(0xFF10B981) else Color(0xFF6366F1)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        FireworksProgressBar(
                                            progress = retentionRate,
                                            height = 6.dp,
                                            gradientColors = if (isChapterAllDone) listOf(Color(0xFF10B981), Color(0xFF34D399))
                                                             else listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
                                            sparkColor = if (isChapterAllDone) Color(0xFF10B981) else Color(0xFF6366F1),
                                            isDark = isDark
                                        )

                                        // ─── AI MIND SUGGESTION PILL ON CHAPTER CARD ───
                                        val aiSuggestion = aiMindSuggestions[group.chapterName]
                                        if (aiSuggestion != null) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isDark) Color(0x228B5CF6) else Color(0xFFF5F3FF),
                                                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.Top,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Text("🧠", fontSize = 15.sp, modifier = Modifier.padding(top = 1.dp))
                                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                            Text(
                                                                "AI Suggests: ${aiSuggestion.formattedDate}",
                                                                fontSize = 11.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
                                                            )
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                                                ) {
                                                                    Text(
                                                                        aiSuggestion.loadLabel,
                                                                        fontSize = 9.5.sp,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = Color(0xFF10B981),
                                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                                Text(
                                                                    aiSuggestion.stageLabel,
                                                                    fontSize = 9.5.sp,
                                                                    color = subTextColor
                                                                )
                                                            }
                                                        }
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFF7C3AED),
                                                        modifier = Modifier.clickable {
                                                            viewModel.setChapterCustomRevisionDate(group.chapterName, aiSuggestion.suggestedDateMillis)
                                                            Toast.makeText(context, "Scheduled '${group.chapterName}' for ${aiSuggestion.formattedDate} with AI Mind! 🧠⚡", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Text(
                                                            "Accept ⚡",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // ─── COMPLETED SUBTOPICS DISPLAYED UNDER CHAPTER NAME ───
                                        if (chapterFreshTopics.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "✅ Completed Subtopics (${chapterFreshTopics.size}):",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF10B981)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                LazyRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    items(chapterFreshTopics, key = { it.topicId }) { topic ->
                                                        val subName = topic.topicId.substringAfter("-").ifBlank { "Full Chapter" }
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                                                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF10B981),
                                                                    modifier = Modifier.size(11.dp)
                                                                )
                                                                Text(
                                                                    text = subName,
                                                                    fontSize = 10.5.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46),
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // ─── COLLAPSE / EXPAND TOGGLE BAR (Saves screen space) ───
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    collapsedChapters = if (isExpanded) {
                                                        collapsedChapters + group.chapterName
                                                    } else {
                                                        collapsedChapters - group.chapterName
                                                    }
                                                }
                                                .background(if (isDark) Color(0x15FFFFFF) else Color(0xFFF1F5F9))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                                    tint = Color(0xFF6366F1),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    if (isExpanded) "Collapse Subtopics" else "View & Revise Subtopics (${group.topics.size})",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF6366F1)
                                                )
                                            }
                                            Text(
                                                if (isExpanded) "Hide ▴" else "Show Details ▾",
                                                fontSize = 10.5.sp,
                                                color = subTextColor,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        // ─── COLLAPSIBLE SUBTOPICS LIST ───
                                        AnimatedVisibility(visible = isExpanded) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                HorizontalDivider(
                                                    color = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0),
                                                    modifier = Modifier.padding(bottom = 2.dp)
                                                )

                                                group.topics.forEach { topic ->
                                                    val subtopicTitle = topic.topicId.substringAfter("-").ifBlank { "Full Chapter Revision" }
                                                    val daysAgo = ((now - topic.timestamp) / 86400000L).coerceAtLeast(0)
                                                    val isUrgent = if (topic.customRevisionDate != null) topic.customRevisionDate < now else daysAgo >= 14
                                                    val isDue = if (topic.customRevisionDate != null) {
                                                        val d = (topic.customRevisionDate - now) / 86400000L
                                                        d in -1..0
                                                    } else daysAgo in 3..13
                                                    val isFresh = if (topic.customRevisionDate != null) topic.customRevisionDate > (now + 86400000L) else daysAgo < 3

                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = when {
                                                            isUrgent -> if (isDark) Color(0x22EF4444) else Color(0xFFFEF2F2)
                                                            isDue -> if (isDark) Color(0x22F59E0B) else Color(0xFFFFFBEB)
                                                            else -> if (isDark) Color(0x1810B981) else Color(0xFFF0FDF4)
                                                        },
                                                        border = BorderStroke(
                                                            1.dp,
                                                            when {
                                                                isUrgent -> Color(0xFFEF4444).copy(alpha = 0.4f)
                                                                isDue -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                                                                else -> Color(0xFF10B981).copy(alpha = 0.35f)
                                                            }
                                                        ),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                                // Left: Checkbox & Name & Status
                                                                Row(
                                                                    modifier = Modifier.weight(1f),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                ) {
                                                                    IconButton(
                                                                        onClick = {
                                                                            if (isFresh) {
                                                                                viewModel.markTopicPending(topic.topicId)
                                                                                Toast.makeText(context, "Marked '$subtopicTitle' as Pending for review ⏳", Toast.LENGTH_SHORT).show()
                                                                            } else {
                                                                                viewModel.markTopicRevised(topic.topicId)
                                                                                Toast.makeText(context, "Revised '$subtopicTitle' today! 🎯", Toast.LENGTH_SHORT).show()
                                                                            }
                                                                        },
                                                                        modifier = Modifier.size(28.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = if (isFresh) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                                            contentDescription = if (isFresh) "Done - Click to Mark Pending" else "Pending - Click to Mark Done",
                                                                            tint = if (isFresh) Color(0xFF10B981) else if (isUrgent) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }

                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Text(
                                                                            text = subtopicTitle,
                                                                            fontSize = 12.5.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = if (isFresh) (if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)) else textColor,
                                                                            maxLines = 2,
                                                                            overflow = TextOverflow.Ellipsis
                                                                        )
                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                        ) {
                                                                            val statusText = when {
                                                                                topic.customRevisionDate != null -> {
                                                                                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                                                                                    "🗓️ Scheduled: ${sdf.format(Date(topic.customRevisionDate))}"
                                                                                }
                                                                                daysAgo == 0L -> "🟢 Revised Today"
                                                                                daysAgo == 1L -> "🟢 Revised Yesterday"
                                                                                isFresh -> "🟢 Fresh ($daysAgo days ago)"
                                                                                isDue -> "⚡ Due ($daysAgo days ago)"
                                                                                else -> "🚨 Critical ($daysAgo days ago)"
                                                                            }
                                                                            Text(
                                                                                text = statusText,
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.SemiBold,
                                                                                color = when {
                                                                                    isUrgent -> Color(0xFFEF4444)
                                                                                    isDue -> Color(0xFFD97706)
                                                                                    else -> Color(0xFF10B981)
                                                                                }
                                                                            )
                                                                        }
                                                                    }
                                                                }

                                                                // Right Actions: Schedule, Delete
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                                ) {
                                                                    IconButton(
                                                                        onClick = {
                                                                            scheduleTopicTarget = topic
                                                                        },
                                                                        modifier = Modifier.size(30.dp)
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Default.DateRange,
                                                                            contentDescription = "Schedule",
                                                                            tint = Color(0xFF6366F1),
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }

                                                                    IconButton(
                                                                        onClick = {
                                                                            viewModel.toggleTopicCompletion(topic.topicId, false)
                                                                            Toast.makeText(context, "Removed '$subtopicTitle' from Revision queue", Toast.LENGTH_SHORT).show()
                                                                        },
                                                                        modifier = Modifier.size(30.dp)
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Default.DeleteOutline,
                                                                            contentDescription = "Remove",
                                                                            tint = Color(0xFF94A3B8),
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
// App Branding & Copyright Warning Footer at bottom of Targets & Checklist
            item {
                com.example.ui.components.AppBrandingFooter(isDark = isDark)
            }
        }

        }
        // Add Goal Dialog
        if (showGoalDialog) {
            AlertDialog(
                onDismissRequest = { showGoalDialog = false },
                title = { Text(if (newGoalTargetType == "today") "Add Today's Checkable Goals" else "Add Weekly Strategy Goals", color = textColor, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Goal Duration:", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                        AppSegmentedControl(
                            items = listOf("today", "weekly"),
                            selectedItem = newGoalTargetType,
                            onItemSelected = { newGoalTargetType = it },
                            itemLabel = {
                                when (it) {
                                    "today" -> "Today"
                                    else -> "Weekly"
                                }
                            },
                            itemEmoji = {
                                when (it) {
                                    "today" -> "⚡"
                                    else -> "📅"
                                }
                            },
                            selectedColor = Color(0xFF6D28D9),
                            selectedGradient = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
                            isDark = isDark,
                            fontSize = 12.sp,
                            cornerRadius = 12.dp
                        )

                        Text("Enter Goal / Keyword per Subject:", style = MaterialTheme.typography.labelSmall, color = subTextColor)

                        // Physics Goal Keyword Input
                        OutlinedTextField(
                            value = physicsGoalText,
                            onValueChange = { physicsGoalText = it },
                            label = { Text("⚡ Physics Goal Keyword", color = Color(0xFF2563EB), fontSize = 11.sp) },
                            placeholder = { Text("e.g. Kinematics 50 MCQs", color = subTextColor.copy(alpha = 0.6f), fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = cardBorder,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Chemistry Goal Keyword Input
                        OutlinedTextField(
                            value = chemGoalText,
                            onValueChange = { chemGoalText = it },
                            label = { Text("🧪 Chemistry Goal Keyword", color = Color(0xFFD97706), fontSize = 11.sp) },
                            placeholder = { Text("e.g. Thermodynamics NCERT", color = subTextColor.copy(alpha = 0.6f), fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD97706),
                                unfocusedBorderColor = cardBorder,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Biology Goal Keyword Input
                        OutlinedTextField(
                            value = bioGoalText,
                            onValueChange = { bioGoalText = it },
                            label = { Text("🧬 Biology Goal Keyword", color = Color(0xFF059669), fontSize = 11.sp) },
                            placeholder = { Text("e.g. Genetics 100 MCQs", color = subTextColor.copy(alpha = 0.6f), fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF059669),
                                unfocusedBorderColor = cardBorder,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            var added = 0
                            val goalDate = if (newGoalTargetType == "today") selectedDate else todayDate
                            if (physicsGoalText.isNotBlank()) {
                                viewModel.addGoal("Physics", physicsGoalText.trim(), newGoalTargetType, goalDate)
                                added++
                            }
                            if (chemGoalText.isNotBlank()) {
                                viewModel.addGoal("Chemistry", chemGoalText.trim(), newGoalTargetType, goalDate)
                                added++
                            }
                            if (bioGoalText.isNotBlank()) {
                                viewModel.addGoal("Biology", bioGoalText.trim(), newGoalTargetType, goalDate)
                                added++
                            }

                            if (added > 0) {
                                physicsGoalText = ""
                                chemGoalText = ""
                                bioGoalText = ""
                                showGoalDialog = false
                                Toast.makeText(context, "$added Goal(s) saved successfully! ✅", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter at least one goal keyword!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Text("Save Goals", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoalDialog = false }) {
                        Text("Cancel", color = subTextColor)
                    }
                },
                containerColor = dialogBg
            )
        }

        // Upload / Add DPP Dialog
        if (showAddDppDialog) {
            AlertDialog(
                onDismissRequest = { showAddDppDialog = false },
                title = { Text(if (editingDppItem != null) "Edit DPP" else "Upload / Add Chapter DPP", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("For: $activeDppChapter ($activeDppSubject)", style = MaterialTheme.typography.bodySmall, color = subTextColor)

                        OutlinedTextField(
                            value = newDppTitleInput,
                            onValueChange = { newDppTitleInput = it },
                            label = { Text("DPP Title / Number", color = subTextColor) },
                            placeholder = { Text("e.g. DPP 01 or DPP 02: Kinematics", color = subTextColor.copy(alpha = 0.6f), fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = cardBorder,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // File Upload / PDF Status
                        Surface(
                            color = if (isDark) Color(0x1A6366F1) else Color(0xFFEEF2FF),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0x336366F1)),
                            modifier = Modifier.fillMaxWidth().clickable {
                                dppPdfPickerLauncher.launch("application/pdf")
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = newDppPdfNameInput ?: "Attach PDF File from Storage",
                                            color = textColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (newDppPdfNameInput != null) "Tap to change PDF" else "Tap to pick PDF document",
                                            color = subTextColor,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp))
                            }
                        }

                        // Web / Drive Link alternative
                        OutlinedTextField(
                            value = newDppPdfUriInput ?: "",
                            onValueChange = {
                                newDppPdfUriInput = it
                                if (newDppPdfNameInput == null && it.startsWith("http")) {
                                    newDppPdfNameInput = "Web DPP Link"
                                }
                            },
                            label = { Text("Drive / Web Link (Optional)", color = subTextColor) },
                            placeholder = { Text("https://drive.google.com/...", color = subTextColor.copy(alpha = 0.6f), fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = cardBorder,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newDppTitleInput.isNotBlank()) {
                                if (editingDppItem != null) {
                                    viewModel.updateDppItem(
                                        editingDppItem!!.copy(
                                            dppName = newDppTitleInput,
                                            pdfUri = newDppPdfUriInput,
                                            pdfFileName = newDppPdfNameInput ?: if (!newDppPdfUriInput.isNullOrEmpty()) "DPP Link" else null
                                        )
                                    )
                                    Toast.makeText(context, "DPP updated ✅", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addDppItem(
                                        subject = activeDppSubject,
                                        chapter = activeDppChapter,
                                        dppName = newDppTitleInput,
                                        pdfUri = newDppPdfUriInput,
                                        pdfFileName = newDppPdfNameInput ?: if (!newDppPdfUriInput.isNullOrEmpty()) "DPP Link" else null
                                    )
                                    Toast.makeText(context, "DPP added ✅", Toast.LENGTH_SHORT).show()
                                }
                                showAddDppDialog = false
                            } else {
                                Toast.makeText(context, "Please enter a title for the DPP", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Text("Save DPP", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDppDialog = false }) {
                        Text("Cancel", color = subTextColor)
                    }
                },
                containerColor = dialogBg
            )
        }

        // Add Chapter to Revision Dialog
        if (showAddChapterRevisionDialog) {
            val examGoal = activeExamGoal
            val chaptersForSubj = remember(revisionAddSubject, examGoal) {
                com.example.data.ExamSyllabusDatabase.getChaptersFor(examGoal, revisionAddSubject).map { it.name }
            }
            LaunchedEffect(showAddChapterRevisionDialog, revisionAddSubject, examGoal) {
                val currentChapters = com.example.data.ExamSyllabusDatabase.getChaptersFor(examGoal, revisionAddSubject).map { it.name }
                if (revisionAddChapter.isBlank() || !currentChapters.contains(revisionAddChapter)) {
                    revisionAddChapter = currentChapters.firstOrNull() ?: ""
                }
                val subs = if (revisionAddChapter.isNotBlank()) {
                    val dbSubs = com.example.data.ExamSyllabusDatabase.getSubtopicsForChapter(revisionAddChapter)
                    if (dbSubs.isNotEmpty()) dbSubs else listOf("Core Theory & Concepts", "Formula Revision", "PYQ Practice", "Chapter Test & Error Log")
                } else emptyList()
                revisionSelectedSubtopics = subs.toSet()
            }
            val allSubtopicsInChapter = remember(revisionAddChapter) {
                if (revisionAddChapter.isNotBlank()) {
                    val dbSubs = com.example.data.ExamSyllabusDatabase.getSubtopicsForChapter(revisionAddChapter)
                    if (dbSubs.isNotEmpty()) dbSubs else listOf("Core Theory & Concepts", "Formula Revision", "PYQ Practice", "Chapter Test & Error Log")
                } else emptyList()
            }

            AlertDialog(
                onDismissRequest = { showAddChapterRevisionDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("➕", fontSize = 20.sp)
                        Text("Add Chapter to Revision", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = textColor)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Select subject and chapter to add its subtopics into Spaced Repetition queue:",
                            fontSize = 12.sp,
                            color = subTextColor
                        )

                        // Subject Selector
                        Text("Subject:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableSubjects.forEach { subj ->
                                val isSel = revisionAddSubject == subj
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) Color(0xFF6366F1) else (if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)),
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF6366F1) else cardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            revisionAddSubject = subj
                                            val currentChapters = com.example.data.ExamSyllabusDatabase.getChaptersFor(examGoal, subj).map { it.name }
                                            val firstCh = currentChapters.firstOrNull() ?: ""
                                            revisionAddChapter = firstCh
                                            val subs = if (firstCh.isNotBlank()) {
                                                val dbSubs = com.example.data.ExamSyllabusDatabase.getSubtopicsForChapter(firstCh)
                                                if (dbSubs.isNotEmpty()) dbSubs else listOf("Core Theory & Concepts", "Formula Revision", "PYQ Practice", "Chapter Test & Error Log")
                                            } else emptyList()
                                            revisionSelectedSubtopics = subs.toSet()
                                        }
                                ) {
                                    Text(
                                        text = subj,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Color.White else textColor,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        // Chapter Selector Dropdown / Selection
                        Text("Chapter:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                        var isChapterDropdownExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { isChapterDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = revisionAddChapter.ifBlank { "Select Chapter" },
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = isChapterDropdownExpanded,
                                onDismissRequest = { isChapterDropdownExpanded = false },
                                modifier = Modifier.heightIn(max = 280.dp)
                            ) {
                                chaptersForSubj.forEach { ch ->
                                    DropdownMenuItem(
                                        text = { Text(ch, fontSize = 12.sp) },
                                        onClick = {
                                            revisionAddChapter = ch
                                            val subs = com.example.data.ExamSyllabusDatabase.getSubtopicsForChapter(ch)
                                            val finalSubs = if (subs.isNotEmpty()) subs else listOf("Core Theory & Concepts", "Formula Revision", "PYQ Practice", "Chapter Test & Error Log")
                                            revisionSelectedSubtopics = finalSubs.toSet()
                                            isChapterDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Subtopics selection
                        if (allSubtopicsInChapter.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Subtopics (${revisionSelectedSubtopics.size}/${allSubtopicsInChapter.size}):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = textColor
                                )
                                TextButton(
                                    onClick = {
                                        revisionSelectedSubtopics = if (revisionSelectedSubtopics.size == allSubtopicsInChapter.size) emptySet() else allSubtopicsInChapter.toSet()
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        if (revisionSelectedSubtopics.size == allSubtopicsInChapter.size) "Deselect All" else "Select All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6366F1)
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                allSubtopicsInChapter.forEach { subtopic ->
                                    val isChecked = revisionSelectedSubtopics.contains(subtopic)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                revisionSelectedSubtopics = if (isChecked) revisionSelectedSubtopics - subtopic else revisionSelectedSubtopics + subtopic
                                            }
                                            .padding(vertical = 4.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                revisionSelectedSubtopics = if (checked) revisionSelectedSubtopics + subtopic else revisionSelectedSubtopics - subtopic
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(subtopic, fontSize = 11.5.sp, color = textColor, lineHeight = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (revisionAddChapter.isNotBlank()) {
                                val toAdd = if (revisionSelectedSubtopics.isNotEmpty()) {
                                    revisionSelectedSubtopics.toList()
                                } else {
                                    listOf("Chapter Revision")
                                }
                                viewModel.addChapterToRevision(revisionAddChapter, toAdd)
                                Toast.makeText(context, "Added ${toAdd.size} subtopic(s) from '$revisionAddChapter' to Revision Queue! 🎉", Toast.LENGTH_SHORT).show()
                                showAddChapterRevisionDialog = false
                            } else {
                                Toast.makeText(context, "Please select a chapter", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val count = if (revisionSelectedSubtopics.isNotEmpty()) revisionSelectedSubtopics.size else 1
                        Text("Add to Revision ($count)", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddChapterRevisionDialog = false }) {
                        Text("Cancel", color = subTextColor)
                    }
                },
                containerColor = dialogBg
            )
        }

        // Schedule Subtopic Revision Date Dialog
        scheduleTopicTarget?.let { topic ->
            val subtopicName = topic.topicId.substringAfter("-").ifBlank { "Full Chapter" }
            AlertDialog(
                onDismissRequest = { scheduleTopicTarget = null },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🗓️", fontSize = 20.sp)
                        Text("Schedule Revision", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = textColor)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Set next review date for:",
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                        Text(
                            text = subtopicName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        if (topic.customRevisionDate != null) {
                            val sdfFull = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📌 Currently set for: ${sdfFull.format(Date(topic.customRevisionDate))}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF6366F1),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // ─── CUSTOM DATE PICKER FROM CALENDAR ───
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0x3010B981) else Color(0xFFECFDF5),
                            border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val cal = Calendar.getInstance()
                                    if (topic.customRevisionDate != null) {
                                        cal.timeInMillis = topic.customRevisionDate
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedCal = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                set(Calendar.HOUR_OF_DAY, 9)
                                                set(Calendar.MINUTE, 0)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            val targetTime = selectedCal.timeInMillis
                                            viewModel.setTopicCustomRevisionDate(topic.topicId, targetTime)
                                            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                                            Toast.makeText(context, "Revision scheduled for ${sdf.format(Date(targetTime))} 📅", Toast.LENGTH_SHORT).show()
                                            scheduleTopicTarget = null
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).apply {
                                        datePicker.minDate = System.currentTimeMillis() - 1000L
                                    }.show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.DateRange,
                                                contentDescription = "Pick Custom Date",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            "📅 Choose from Calendar",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                                        )
                                        Text(
                                            "Pick any custom date yourself",
                                            fontSize = 10.5.sp,
                                            color = subTextColor
                                        )
                                    }
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Or Quick Spaced Presets:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )

                        val nowMs = System.currentTimeMillis()
                        val presets = listOf(
                            Triple("⚡ +3 Days", "Short-term reinforcement", nowMs + 3 * 86400000L),
                            Triple("📅 +7 Days", "Weekly consolidation", nowMs + 7 * 86400000L),
                            Triple("🧠 +14 Days", "Long-term memory lock", nowMs + 14 * 86400000L),
                            Triple("🏆 +30 Days", "Monthly master check", nowMs + 30 * 86400000L)
                        )

                        presets.forEach { (label, desc, targetTime) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0x226366F1) else Color(0xFFEEF2FF),
                                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setTopicCustomRevisionDate(topic.topicId, targetTime)
                                        Toast.makeText(context, "Scheduled '$subtopicName' for $label", Toast.LENGTH_SHORT).show()
                                        scheduleTopicTarget = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF6366F1))
                                        Text(desc, fontSize = 10.sp, color = subTextColor)
                                    }
                                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                                    Text(sdf.format(Date(targetTime)), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                                }
                            }
                        }

                        if (topic.customRevisionDate != null) {
                            TextButton(
                                onClick = {
                                    viewModel.setTopicCustomRevisionDate(topic.topicId, null)
                                    Toast.makeText(context, "Reset '$subtopicName' to automatic cycle", Toast.LENGTH_SHORT).show()
                                    scheduleTopicTarget = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🔄 Reset to Automatic Ebbinghaus Cycle", color = Color(0xFFEF4444), fontSize = 11.5.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { scheduleTopicTarget = null }) {
                        Text("Close", color = subTextColor)
                    }
                },
                containerColor = dialogBg
            )
        }

        // Schedule Entire Chapter Revision Date Dialog
        scheduleChapterTarget?.let { chapterGroup ->
            AlertDialog(
                onDismissRequest = { scheduleChapterTarget = null },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🗓️", fontSize = 20.sp)
                        Text("Schedule Chapter Revision", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = textColor)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Set next review date for all subtopics of:",
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                        Text(
                            text = "📖 ${chapterGroup.chapterName} (${chapterGroup.topics.size} subtopics)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        // ─── CUSTOM DATE PICKER FROM CALENDAR ───
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0x3010B981) else Color(0xFFECFDF5),
                            border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val cal = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedCal = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                set(Calendar.HOUR_OF_DAY, 9)
                                                set(Calendar.MINUTE, 0)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            val targetTime = selectedCal.timeInMillis
                                            viewModel.setChapterCustomRevisionDate(chapterGroup.chapterName, targetTime)
                                            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                                            Toast.makeText(context, "Scheduled '${chapterGroup.chapterName}' for ${sdf.format(Date(targetTime))} 📅", Toast.LENGTH_SHORT).show()
                                            scheduleChapterTarget = null
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).apply {
                                        datePicker.minDate = System.currentTimeMillis() - 1000L
                                    }.show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.DateRange,
                                                contentDescription = "Pick Custom Date",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            "📅 Choose from Calendar",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                                        )
                                        Text(
                                            "Pick custom date for all subtopics",
                                            fontSize = 10.5.sp,
                                            color = subTextColor
                                        )
                                    }
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Or Quick Spaced Presets:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )

                        val nowMs = System.currentTimeMillis()
                        val presets = listOf(
                            Triple("⚡ +3 Days", "Short-term reinforcement", nowMs + 3 * 86400000L),
                            Triple("📅 +7 Days", "Weekly consolidation", nowMs + 7 * 86400000L),
                            Triple("🧠 +14 Days", "Long-term memory lock", nowMs + 14 * 86400000L),
                            Triple("🏆 +30 Days", "Monthly master check", nowMs + 30 * 86400000L)
                        )

                        presets.forEach { (label, desc, targetTime) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0x226366F1) else Color(0xFFEEF2FF),
                                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setChapterCustomRevisionDate(chapterGroup.chapterName, targetTime)
                                        Toast.makeText(context, "Scheduled '${chapterGroup.chapterName}' for $label", Toast.LENGTH_SHORT).show()
                                        scheduleChapterTarget = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF6366F1))
                                        Text(desc, fontSize = 10.sp, color = subTextColor)
                                    }
                                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                                    Text(sdf.format(Date(targetTime)), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                                }
                            }
                        }

                        TextButton(
                            onClick = {
                                viewModel.setChapterCustomRevisionDate(chapterGroup.chapterName, null)
                                Toast.makeText(context, "Reset '${chapterGroup.chapterName}' to automatic cycle", Toast.LENGTH_SHORT).show()
                                scheduleChapterTarget = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔄 Reset Chapter to Automatic Cycle", color = Color(0xFFEF4444), fontSize = 11.5.sp)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { scheduleChapterTarget = null }) {
                        Text("Close", color = subTextColor)
                    }
                },
                containerColor = dialogBg
            )
        }

        // ─── AI MIND COMPREHENSIVE REVISION PLAN DIALOG ───
        if (showAiMindPlanDialog) {
            val nowMs = System.currentTimeMillis()
            val eligibleRevisionTopics = remember(completedTopics) {
                completedTopics
            }
            val allRevisionGroups = remember(eligibleRevisionTopics, activeExamGoal) {
                eligibleRevisionTopics.groupBy { topic ->
                    val parts = topic.topicId.split("-", limit = 2)
                    parts.getOrNull(0) ?: "General"
                }.map { (chapterName, topicList) ->
                    val subject = getSubjectForChapter(chapterName, activeExamGoal.name)
                    ChapterRevisionGroup(
                        chapterName = chapterName,
                        subject = subject,
                        topics = topicList
                    )
                }
            }
            val suggestions = remember(allRevisionGroups, nowMs) {
                computeAiMindSuggestions(allRevisionGroups, nowMs)
            }
            AlertDialog(
                onDismissRequest = { showAiMindPlanDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🧠", fontSize = 18.sp)
                            }
                        }
                        Column {
                            Text(
                                "AI Mind Spaced Plan",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = textColor
                            )
                            Text(
                                "Zero-Pressure • 1 Chapter / Day Schedule",
                                fontSize = 10.5.sp,
                                color = subTextColor
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0x228B5CF6) else Color(0xFFF5F3FF),
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎯", fontSize = 18.sp)
                                Text(
                                    "AI distributes ${suggestions.size} chapters across upcoming days so you never feel overloaded while continuing your syllabus.",
                                    fontSize = 11.5.sp,
                                    color = if (isDark) Color(0xFFDDD6FE) else Color(0xFF5B21B6),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Text(
                            "Suggested Schedule Timeline:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = textColor
                        )

                        suggestions.values.sortedBy { it.dayOffset }.forEach { plan ->
                            val subColor = when {
                                plan.subject.contains("Physics", ignoreCase = true) -> Color(0xFF2563EB)
                                plan.subject.contains("Chemistry", ignoreCase = true) -> Color(0xFFD97706)
                                plan.subject.contains("Math", ignoreCase = true) -> Color(0xFF7C3AED)
                                else -> Color(0xFF059669)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0x15FFFFFF) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = subColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                plan.subject,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = subColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "🗓️ ${plan.formattedDate}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF7C3AED),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "📖 ${plan.chapterName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = textColor
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            plan.stageLabel,
                                            fontSize = 10.sp,
                                            color = subTextColor
                                        )
                                        Text(
                                            plan.loadLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val batch = suggestions.mapValues { it.value.suggestedDateMillis }
                            viewModel.applyAiMindBatchSchedule(batch)
                            Toast.makeText(context, "✨ AI Mind schedule applied successfully! 🧠🎯", Toast.LENGTH_SHORT).show()
                            showAiMindPlanDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply All Dates ✨", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAiMindPlanDialog = false }) {
                        Text("Close", color = subTextColor)
                    }
                },
                containerColor = dialogBg
            )
        }

          }
}

data class AiMindSuggestion(
    val chapterName: String,
    val subject: String,
    val suggestedDateMillis: Long,
    val formattedDate: String,
    val dayOffset: Int,
    val stageLabel: String,
    val loadLabel: String,
    val reason: String
)

fun computeAiMindSuggestions(
    groups: List<ChapterRevisionGroup>,
    now: Long
): Map<String, AiMindSuggestion> {
    if (groups.isEmpty()) return emptyMap()

    // Sort chapters so chapters needing review sooner get prioritized first
    val sortedGroups = groups.sortedBy { group ->
        val minTs = group.topics.minOfOrNull { it.customRevisionDate ?: it.timestamp } ?: now
        minTs
    }

    val result = mutableMapOf<String, AiMindSuggestion>()
    val sdf = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())

    var currentDayOffset = 1

    sortedGroups.forEach { group ->
        val oldestTopic = group.topics.minByOrNull { it.customRevisionDate ?: it.timestamp }
        val daysSince = if (oldestTopic != null) ((now - oldestTopic.timestamp) / 86400000L).coerceAtLeast(0) else 0L

        val dayOffset = currentDayOffset
        currentDayOffset += 1 // Spreads 1 chapter per day so student has zero stress

        val targetCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetMs = targetCal.timeInMillis
        val formatted = sdf.format(Date(targetMs))

        val stage = when {
            daysSince >= 14 -> "Critical Review (Memory Lock)"
            daysSince in 7..13 -> "Active Recall (Retention Boost)"
            daysSince in 3..6 -> "Consolidation Check"
            else -> "Spaced Reinforcement"
        }

        val load = when (dayOffset) {
            1 -> "Tomorrow • 15-20 min"
            2 -> "In 2 Days • 15-20 min"
            else -> "Day +$dayOffset • 15 min"
        }

        val reason = "Scheduled separately to keep your daily study load low and let you learn new syllabus effortlessly."

        result[group.chapterName] = AiMindSuggestion(
            chapterName = group.chapterName,
            subject = group.subject,
            suggestedDateMillis = targetMs,
            formattedDate = formatted,
            dayOffset = dayOffset,
            stageLabel = stage,
            loadLabel = load,
            reason = reason
        )
    }

    return result
}

fun isNcertCompletedForTopic(
    chapterName: String,
    subtopicName: String,
    bookProgressions: List<com.example.data.BookProgression>
): Boolean {
    val prog = bookProgressions.find { it.chapter.equals(chapterName, ignoreCase = true) } ?: return false
    if (prog.progressPercent >= 100 || prog.status.equals("Completed", ignoreCase = true)) return true

    val map = parseSubtopicsProgress(prog.subtopicsProgress)
    if (map.isNotEmpty()) {
        if (DEFAULT_SUBTOPICS.all { map[it] == "Completed" }) return true
        if (subtopicName.isNotBlank() && map[subtopicName] == "Completed") return true
    }
    return false
}

data class ChapterRevisionGroup(
    val chapterName: String,
    val subject: String,
    val topics: List<com.example.data.CompletedTopic>
)

@Composable
fun TargetQuestionRow(
    subject: String,
    targetVal: String,
    solvedVal: String,
    color: Color,
    difficulty: String? = null,
    onDifficultyChange: ((String) -> Unit)? = null,
    onTargetChange: (String) -> Unit,
    onSolvedChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val tInt = (targetVal.toIntOrNull() ?: 0).coerceAtLeast(0)
    val sInt = (solvedVal.toIntOrNull() ?: 0).coerceAtLeast(0)
    val isDark = MaterialTheme.colorScheme.background.run { (red + green + blue) < 1.0f }
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0x0EFFFFFF) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)

    val isMet = tInt > 0 && sInt >= tInt
    val progressFraction = if (tInt > 0) (sInt.toFloat() / tInt.toFloat()).coerceIn(0f, 1f) else 0f
    val pct = if (tInt > 0) ((sInt.toFloat() / tInt.toFloat()) * 100).toInt() else 0

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(
            1.2.dp,
            if (isMet) Color(0xFF10B981).copy(alpha = 0.5f) else color.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Subject Badge + Progress Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isMet) Color(0xFF10B981) else color)
                    )
                    Text(
                        text = subject,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isMet) Color(0xFF10B981) else textColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isMet) Color(0xFF10B981).copy(alpha = 0.18f) else color.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isMet) "🎉 $sInt/$tInt Qs ($pct%)" else "$sInt / $tInt Qs ($pct%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMet) Color(0xFF10B981) else color
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            FireworksProgressBar(
                progress = progressFraction,
                height = 6.dp,
                gradientColors = if (isMet) listOf(Color(0xFF34D399), Color(0xFF10B981))
                                 else listOf(color.copy(alpha = 0.7f), color),
                sparkColor = if (isMet) Color(0xFF10B981) else color,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Two Clean Inputs: Solved vs Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Solved Input Box
                OutlinedTextField(
                    value = solvedVal,
                    onValueChange = {
                        onSolvedChange(it.filter { ch -> ch.isDigit() })
                        onSave()
                    },
                    label = { Text("Solved (Qs)", fontSize = 11.sp, color = subTextColor) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = color,
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedContainerColor = if (isDark) Color(0x14000000) else Color.White,
                        unfocusedContainerColor = if (isDark) Color(0x14000000) else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Target Input Box
                OutlinedTextField(
                    value = targetVal,
                    onValueChange = {
                        onTargetChange(it.filter { ch -> ch.isDigit() })
                        onSave()
                    },
                    label = { Text("Target (Qs)", fontSize = 11.sp, color = subTextColor) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedContainerColor = if (isDark) Color(0x14000000) else Color.White,
                        unfocusedContainerColor = if (isDark) Color(0x14000000) else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Difficulty Level (Easy, Medium, Hard) Selection for Questions Solved
            if (difficulty != null && onDifficultyChange != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Difficulty Level (Solved Qs):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor
                        )
                        val diffTagColor = when (difficulty.lowercase()) {
                            "easy" -> Color(0xFF10B981)
                            "hard" -> Color(0xFFEF4444)
                            else -> Color(0xFFF59E0B)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = diffTagColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = when (difficulty.lowercase()) {
                                    "easy" -> "🟢 Easy (NCERT Direct)"
                                    "hard" -> "🔴 Hard (Multi-Concept)"
                                    else -> "🟡 Medium (Standard PYQ)"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = diffTagColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val diffOptions = listOf(
                        Triple("Easy", "🟢 Easy", Color(0xFF10B981)),
                        Triple("Medium", "🟡 Medium", Color(0xFFF59E0B)),
                        Triple("Hard", "🔴 Hard", Color(0xFFEF4444))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        diffOptions.forEach { (levelKey, label, levelColor) ->
                            val isSelected = difficulty.equals(levelKey, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) levelColor else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) levelColor else if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onDifficultyChange(levelKey)
                                        onSave()
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSelected) Color.White else textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Stepper / Increment Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -10 button (if > 0)
                if (sInt >= 10) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0),
                        modifier = Modifier.clickable {
                            val newS = (sInt - 10).coerceAtLeast(0)
                            onSolvedChange(newS.toString())
                            onSave()
                        }
                    ) {
                        Text(
                            text = "-10",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        )
                    }
                }

                // Quick Increment Pills
                listOf(5, 10, 25).forEach { inc ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = color.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, color.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val newS = sInt + inc
                                onSolvedChange(newS.toString())
                                onSave()
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = "+$inc",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = color
                            )
                        }
                    }
                }

                // Match Target Shortcut
                if (tInt > 0 && sInt < tInt) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.18f),
                        border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier.clickable {
                            onSolvedChange(tInt.toString())
                            onSave()
                        }
                    ) {
                        Text(
                            text = "✓ Target",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalItemCard(goal: Goal, viewModel: AppViewModel) {
    val isDone = goal.status == GoalStatus.COMPLETED.value
    val isDark = MaterialTheme.colorScheme.background.run { (red + green + blue) < 1.0f }
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x80FFFFFF) else Color(0xFF94A3B8)
    val inputBg = if (isDark) Color(0xFF1E2235) else Color(0xFFF1F5F9)
    val dialogBg = if (isDark) Color(0xFF171A29) else Color.White

    var showEditDialog by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf(goal.text) }
    var editedSubject by remember { mutableStateOf(goal.subject) }
    var editedTargetType by remember { mutableStateOf(goal.targetType) }

    val subjectColor = when (goal.subject) {
        "Physics" -> Color(0xFF60A5FA)
        "Chemistry" -> Color(0xFFF59E0B)
        else -> Color(0xFF34D399)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.2.dp,
                color = if (isDone) Color(0xFF10B981).copy(alpha = 0.5f) else if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .background(if (isDone) Color(0x1210B981) else Color.Transparent)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(
                        checked = isDone,
                        onCheckedChange = { checked ->
                            viewModel.updateGoalStatus(goal.id, if (checked) GoalStatus.COMPLETED else GoalStatus.TODO)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF10B981),
                            uncheckedColor = subTextColor,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = if (isDone) Color(0x2210B981) else subjectColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = goal.subject,
                                    color = if (isDone) Color(0xFF10B981) else subjectColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (goal.date.isNotBlank()) {
                                Text(
                                    text = "📅 ${goal.date}",
                                    fontSize = 10.sp,
                                    color = subTextColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = goal.text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isDone) FontWeight.Normal else FontWeight.SemiBold,
                            color = if (isDone) subTextColor.copy(alpha = 0.7f) else textColor,
                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDone) {
                        Surface(
                            color = Color(0x2E10B981),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (!goal.completedDate.isNullOrBlank()) "Done" else "Completed",
                                    color = Color(0xFF10B981),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            editedText = goal.text
                            editedSubject = goal.subject
                            editedTargetType = goal.targetType
                            showEditDialog = true
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Goal", tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
                    }

                    IconButton(
                        onClick = { viewModel.deleteGoal(goal.id) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Goal", tint = Color(0xFFF43F5E), modifier = Modifier.size(15.dp))
                    }
                }
            }

            if (isDone && !goal.completedDate.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.padding(start = 34.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "✓ Completed on ${goal.completedDate}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981).copy(alpha = 0.9f)
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Goal", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Subject:", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Physics", "Chemistry", "Biology").forEach { subj ->
                            FilterChip(
                                selected = editedSubject == subj,
                                onClick = { editedSubject = subj },
                                label = { Text(subj, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0x33818CF8),
                                    selectedLabelColor = Color(0xFF6366F1),
                                    containerColor = inputBg,
                                    labelColor = subTextColor
                                )
                            )
                        }
                    }

                    Text("Goal Duration:", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = editedTargetType == "today",
                            onClick = { editedTargetType = "today" },
                            label = { Text("⚡ Today", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0x33818CF8),
                                selectedLabelColor = Color(0xFF6366F1),
                                containerColor = inputBg,
                                labelColor = subTextColor
                            )
                        )
                        FilterChip(
                            selected = editedTargetType == "weekly",
                            onClick = { editedTargetType = "weekly" },
                            label = { Text("📅 Weekly", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0x3334D399),
                                selectedLabelColor = Color(0xFF059669),
                                containerColor = inputBg,
                                labelColor = subTextColor
                            )
                        )
                    }

                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        label = { Text("Goal Description", color = subTextColor, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedText.isNotBlank()) {
                            viewModel.updateGoal(
                                goal.copy(
                                    subject = editedSubject,
                                    text = editedText.trim(),
                                    targetType = editedTargetType
                                )
                            )
                            showEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = subTextColor)
                }
            },
            containerColor = dialogBg
        )
    }
}


fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = it.getString(index)
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

fun getChaptersForSubject(subject: String, examGoal: String = "NEET"): List<String> {
    val databaseChapters = com.example.data.ExamSyllabusDatabase.getChaptersFor(examGoal, subject)
    if (databaseChapters.isNotEmpty()) {
        return databaseChapters
    }
    // Fallback if needed
    return emptyList()
}

fun getSubjectForChapter(chapter: String, examGoal: String): String {
    val category = com.example.data.ExamSyllabusDatabase.parseExamCategory(examGoal)
    val subjects = com.example.data.ExamSyllabusDatabase.getSubjectsFor(category)
    for (subj in subjects) {
        val chList = com.example.data.ExamSyllabusDatabase.getChaptersFor(category, subj)
        if (chList.any { it.name.equals(chapter, ignoreCase = true) || chapter.contains(it.name, ignoreCase = true) || it.name.contains(chapter, ignoreCase = true) }) {
            return subj
        }
    }
    return when {
        chapter.contains("phy", ignoreCase = true) || chapter.contains("kinematics", ignoreCase = true) || chapter.contains("optics", ignoreCase = true) || chapter.contains("current", ignoreCase = true) || chapter.contains("motion", ignoreCase = true) || chapter.contains("gravitation", ignoreCase = true) -> "Physics"
        chapter.contains("chem", ignoreCase = true) || chapter.contains("organic", ignoreCase = true) || chapter.contains("bonding", ignoreCase = true) || chapter.contains("equilibrium", ignoreCase = true) || chapter.contains("atomic", ignoreCase = true) || chapter.contains("reaction", ignoreCase = true) -> "Chemistry"
        chapter.contains("math", ignoreCase = true) || chapter.contains("calculus", ignoreCase = true) || chapter.contains("algebra", ignoreCase = true) || chapter.contains("matrix", ignoreCase = true) || chapter.contains("coordinate", ignoreCase = true) -> "Mathematics"
        else -> if (category == com.example.data.ExamCategory.JEE_MAIN || category == com.example.data.ExamCategory.JEE_ADVANCED) "Physics" else "Biology"
    }
}
