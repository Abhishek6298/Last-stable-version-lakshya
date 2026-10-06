package com.example.ui.screens
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.ui.components.GlassCard
import com.example.ui.components.AppSegmentedControl
import com.example.ui.components.ForestFocusGardenCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import com.example.data.ExamSyllabusDatabase
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.AppViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.compose.collectAsStateWithLifecycle


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    androidx.activity.compose.BackHandler(enabled = true) {
        onNavigateBack()
    }
    val isRunning by viewModel.timerIsRunning.collectAsStateWithLifecycle()
    val secondsElapsed by viewModel.timerSecondsElapsed.collectAsStateWithLifecycle()
    
    val subject by viewModel.timerSubject.collectAsStateWithLifecycle()
    val chapter by viewModel.timerChapter.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }

    val isCountdownMode by viewModel.isCountdownMode.collectAsStateWithLifecycle()
    val isStrictFocusMode by viewModel.isStrictFocusMode.collectAsStateWithLifecycle()
    val isPomodoroMode by viewModel.isPomodoroMode.collectAsStateWithLifecycle()
    val isStudyTargetAlertEnabled by viewModel.isStudyTargetAlertEnabled.collectAsStateWithLifecycle()
    val dailyStudyTargetMinutes by viewModel.dailyStudyTargetMinutes.collectAsStateWithLifecycle()
    val showStudyTargetSubtleAlert by viewModel.showStudyTargetSubtleAlert.collectAsStateWithLifecycle()

    LaunchedEffect(isFullScreen) {
        viewModel.setIsFullScreenTimer(isFullScreen)
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var isFirstLaunch by remember { mutableStateOf(true) }
    LaunchedEffect(isRunning) {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper && currentContext !is android.app.Activity) {
            currentContext = currentContext.baseContext
        }
        val activity = currentContext as? android.app.Activity
        
        if (activity != null) {
            try {
                if (isRunning && isStrictFocusMode) {
                    activity.startLockTask()
                } else {
                    try {
                        activity.stopLockTask()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Please enable App Pinning in Settings > Security. Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
        
        if (isFirstLaunch) {
            isFirstLaunch = false
        } else {
            try {
                val notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                val r = android.media.RingtoneManager.getRingtone(context, notification)
                r?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setIsFullScreenTimer(false)
        }
    }

    val activeExamGoal by viewModel.activeExamGoal.collectAsStateWithLifecycle()
    val subjects = listOf("Physics", "Chemistry", "Biology", "Mock Test")

    val currentChapterItems = remember(subject, activeExamGoal) {
        val items = ExamSyllabusDatabase.getChaptersFor(
            ExamSyllabusDatabase.parseExamCategory(activeExamGoal.name),
            subject
        )
        if (items.isNotEmpty()) {
            items
        } else {
            when {
                subject.contains("Physics", ignoreCase = true) -> ExamSyllabusDatabase.neetPhysicsChapters
                subject.contains("Chemistry", ignoreCase = true) -> ExamSyllabusDatabase.neetChemistryChapters
                subject.contains("Biology", ignoreCase = true) -> ExamSyllabusDatabase.neetBiologyChapters
                subject.contains("Mock", ignoreCase = true) -> listOf(
                    ExamSyllabusDatabase.ChapterItem("Full Syllabus NEET Mock", "12th", "🔥 720 Marks Full Test"),
                    ExamSyllabusDatabase.ChapterItem("Part Syllabus Test", "11th/12th", "High Yield Sectional"),
                    ExamSyllabusDatabase.ChapterItem("Previous Year Question Paper (PYQ)", "12th", "Past 10 Years PYQs"),
                    ExamSyllabusDatabase.ChapterItem("Subject-Wise Mock Test", "11th/12th", "Target 180 Marks")
                )
                else -> listOf(
                    ExamSyllabusDatabase.ChapterItem("General Revision", "11th/12th", "Daily Practice"),
                    ExamSyllabusDatabase.ChapterItem("Formula Sheet Practice", "11th/12th", "High Yield"),
                    ExamSyllabusDatabase.ChapterItem("Error Log / Mistake Review", "11th/12th", "Active Recall"),
                    ExamSyllabusDatabase.ChapterItem("NCERT Line-by-Line Reading", "11th/12th", "NCERT Direct")
                )
            }
        }
    }

    val currentChapterNames = remember(currentChapterItems) {
        currentChapterItems.map { it.name }
    }

    var showChapterSelectDialog by remember { mutableStateOf(false) }
    var chapterSearchQuery by remember { mutableStateOf("") }
    var branchFilter by remember { mutableStateOf("All") }

    LaunchedEffect(subject, currentChapterNames) {
        if (chapter.isBlank() && currentChapterNames.isNotEmpty()) {
            viewModel.setTimerChapter(currentChapterNames.first())
        }
    }

    val logs by viewModel.studyLogs.collectAsStateWithLifecycle()
    var isLogsExpanded by remember { mutableStateOf(true) }
    val filteredLogs = remember(logs) {
        logs.filter {
            it.subject != "Daily MCQs" &&
            it.subject != "Weekly MCQs" &&
            !it.subject.contains("MCQs") &&
            !it.subject.contains("Practice")
        }
    }
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val isDark = isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0x0DFFFFFF) else Color.White
    val cardBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
    val pillBg = if (isDark) Color(0x0DFFFFFF) else Color(0xFFF1F5F9)
    val dropdownBg = if (isDark) Color(0xFF000000) else Color.White

    Box(modifier = modifier.fillMaxSize()) {
        // Full Screen Focus Mode Overlay
        if (isFullScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF000000))
                    .zIndex(100f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Subject & Chapter Badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val subjectIcon = when (subject) {
                                "Physics" -> "⚡"
                                "Chemistry" -> "🧪"
                                "Mock Test" -> "📝"
                                else -> "🧬"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x266366F1))
                                    .border(1.dp, Color(0xFF818CF8), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "$subjectIcon $subject • ${chapter.ifEmpty { "General Study" }}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Exit Fullscreen Button
                        IconButton(
                            onClick = { isFullScreen = false },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x26FFFFFF))
                        ) {
                            Icon(
                                Icons.Default.FullscreenExit,
                                contentDescription = "Exit Fullscreen",
                                tint = Color.White
                            )
                        }
                    }

                    // Center Flip Clock
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎯 DISTRACTION FREE FOCUS MODE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF818CF8),
                            letterSpacing = 3.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        FlipClockDisplay(
                            secondsElapsed = secondsElapsed, 
                            fontSize = 56.sp,
                            targetSeconds = if (isCountdownMode) dailyStudyTargetMinutes * 60 else null
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isRunning) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRunning) "FOCUS SESSION ACTIVE • LOGGING" else "SESSION PAUSED",
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 2.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mini Plant Status in Fullscreen
                        val fsPlantStage = when {
                            secondsElapsed < 180 -> "🌰 Planting Seed (0-3m)"
                            secondsElapsed < 600 -> "🌱 Tender Sprout Unfurling"
                            secondsElapsed < 1500 -> "🌿 Thriving Sapling Growing"
                            secondsElapsed < 2700 -> "🌳 Full Bloomed Tree"
                            else -> "🌲✨ Ancient Radiant Wonder"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1F10B981))
                                .border(1.dp, Color(0x4D34D399), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Forest Tree: $fsPlantStage",
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Bottom Controls
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 40.dp)
                    ) {
                        // Stop & Save
                        IconButton(
                            onClick = {
                                viewModel.stopAndSaveTimer()
                                isLogsExpanded = false
                            },
                            enabled = secondsElapsed > 0,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (secondsElapsed > 0) Color(0xFFE11D48) else Color(0x0DFFFFFF))
                                .border(1.dp, if (secondsElapsed > 0) Color.White else Color(0x1AFFFFFF), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop & Save",
                                tint = if (secondsElapsed > 0) Color.White else Color(0x80FFFFFF),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(28.dp))

                        // Play / Pause Button
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) Color(0xFFF43F5E) else Color(0xFF4F46E5))
                                .border(3.dp, Color.White, CircleShape)
                                .clickable { viewModel.toggleTimer() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Header
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
                        Text("Study Session", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = textColor)
                        Text("Time is logged automatically to your dashboard.", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                    }
                }
            }

            // SUBTLE UI ALERT FOR STUDY TARGET
            if (showStudyTargetSubtleAlert) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color(0x1FEEBF2B) else Color(0xFFFEF3C7))
                            .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎯", fontSize = 18.sp)
                                }
                                Column {
                                    Text(
                                        text = "Almost There! 🔥",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(
                                        text = "You are less than 30 minutes away from your daily study target of ${dailyStudyTargetMinutes / 60} hours. Keep up the high focus!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            
                            IconButton(onClick = { viewModel.dismissStudyTargetAlert() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss alert",
                                    tint = textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Timer Card Box
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(32.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Subject Selector
                        AppSegmentedControl(
                            items = subjects,
                            selectedItem = subject,
                            onItemSelected = { s ->
                                if (!isRunning) {
                                    viewModel.setTimerSubject(s)
                                    val chs = ExamSyllabusDatabase.getChaptersFor(
                                        ExamSyllabusDatabase.parseExamCategory(activeExamGoal.name),
                                        s
                                    )
                                    val defaultCh = chs.firstOrNull()?.name ?: when {
                                        s.contains("Physics", ignoreCase = true) -> "Units & Measurements"
                                        s.contains("Chemistry", ignoreCase = true) -> "Some Basic Concepts of Chemistry (Mole Concept)"
                                        s.contains("Biology", ignoreCase = true) -> "The Living World & Biological Classification"
                                        s.contains("Mock", ignoreCase = true) -> "Full Syllabus NEET Mock"
                                        else -> "General Revision"
                                    }
                                    viewModel.setTimerChapter(defaultCh)
                                }
                            },
                            itemLabel = { it },
                            itemEmoji = {
                                when (it) {
                                    "Physics" -> "⚡"
                                    "Chemistry" -> "🧪"
                                    "Mock Test" -> "📝"
                                    else -> "🧬"
                                }
                            },
                            selectedColor = when (subject) {
                                "Physics" -> Color(0xFF2563EB)
                                "Chemistry" -> Color(0xFFD97706)
                                "Mock Test" -> Color(0xFF8B5CF6)
                                else -> Color(0xFF059669)
                            },
                            selectedGradient = when (subject) {
                                "Physics" -> listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                                "Chemistry" -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                "Mock Test" -> listOf(Color(0xFFA855F7), Color(0xFF8B5CF6))
                                else -> listOf(Color(0xFF10B981), Color(0xFF059669))
                            },
                            isDark = isDark,
                            fontSize = 12.sp,
                            cornerRadius = 14.dp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Interactive Chapter / Topic Selector Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC))
                                .border(1.dp, if (isDark) Color(0x26818CF8) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .clickable(enabled = !isRunning) {
                                    chapterSearchQuery = ""
                                    branchFilter = "All"
                                    showChapterSelectDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 13.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "CHAPTER / TOPIC",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1),
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${currentChapterItems.size} Syllabus Chapters)",
                                            fontSize = 10.sp,
                                            color = subTextColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = chapter.ifBlank { "Tap to Select Chapter" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Color(0x266366F1) else Color(0xFFEEF2FF))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search Chapters",
                                        tint = Color(0xFF818CF8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Browse",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF818CF8)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Flip Clock Style Display
                        FlipClockDisplay(
                            secondsElapsed = secondsElapsed, 
                            fontSize = 42.sp,
                            targetSeconds = if (isCountdownMode) dailyStudyTargetMinutes * 60 else null
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = if (isRunning) "SESSION ACTIVE • LOGGING..." else "READY TO START",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) Color(0xFFF43F5E) else Color(0x66FFFFFF),
                            letterSpacing = 2.sp
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Controls & Fullscreen Button Row
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Stop & Save Button
                            IconButton(
                                onClick = {
                                    viewModel.stopAndSaveTimer()
                                    isLogsExpanded = false
                                },
                                enabled = secondsElapsed > 0,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (secondsElapsed > 0) Color(0xFFE11D48) else Color(0x0DFFFFFF))
                                    .border(1.dp, if (secondsElapsed > 0) Color.White else Color(0x1AFFFFFF), CircleShape)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop & Save", tint = if (secondsElapsed > 0) Color.White else Color(0x80FFFFFF), modifier = Modifier.size(26.dp))
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Play / Pause Button
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(if (isRunning) Color(0xFFF43F5E) else Color(0xFF4F46E5))
                                    .border(3.dp, Color.White, CircleShape)
                                    .clickable { viewModel.toggleTimer() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                    contentDescription = if (isRunning) "Pause" else "Play", 
                                    tint = Color.White, 
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Fullscreen Button
                            IconButton(
                                onClick = { isFullScreen = true },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x266366F1))
                                    .border(1.dp, Color(0xFF818CF8), CircleShape)
                            ) {
                                Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen Mode", tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                }
            }

            // FOREST FOCUS GAMIFIED GARDEN & AMBIENT SOUNDBOARD CARD
            item {
                ForestFocusGardenCard(
                    secondsElapsed = secondsElapsed,
                    isRunning = isRunning,
                    studyLogs = logs,
                    isDark = isDark
                )
            }
            
            // STUDY TARGET CONFIGURATION SETTINGS CARD
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Daily Study Goal 🎯",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    "Set your study goal & enable alerts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subTextColor
                                )
                            }
                            
                            // Switch toggle
                            Switch(
                                checked = isStudyTargetAlertEnabled,
                                onCheckedChange = { viewModel.setStudyTargetAlertEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF6366F1),
                                    uncheckedThumbColor = subTextColor,
                                    uncheckedTrackColor = cardBorder
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Active target display
                        Text(
                            text = "Current Daily Goal: ${dailyStudyTargetMinutes / 60} hours (${dailyStudyTargetMinutes} minutes)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6366F1)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Quick click hours badges
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(60, 120, 180, 200, 240, 300, 360).forEach { mins ->
                                val hrs = mins / 60
                                val isSelected = dailyStudyTargetMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFF6366F1) else pillBg)
                                        .border(1.dp, if (isSelected) Color(0xFF818CF8) else cardBorder, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.setDailyStudyTargetMinutes(mins) }
                                        .padding(vertical = 8.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (mins == 200) "NEET (3h20m)" else "${hrs}h",
                                        color = if (isSelected) Color.White else textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Timer Countdown Mode Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Countdown Mode ⏳",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    "Run timer backwards from daily target",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subTextColor
                                )
                            }
                            Switch(
                                checked = isCountdownMode,
                                onCheckedChange = { viewModel.setIsCountdownMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF6366F1),
                                    uncheckedThumbColor = subTextColor,
                                    uncheckedTrackColor = cardBorder
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Pomodoro Mode Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Pomodoro Mode (50/10) ☕",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    "Alerts every 50 mins to take a 10 min break",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subTextColor
                                )
                            }
                            Switch(
                                checked = isPomodoroMode,
                                onCheckedChange = { viewModel.setIsPomodoroMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF10B981),
                                    uncheckedThumbColor = subTextColor,
                                    uncheckedTrackColor = cardBorder
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Strict Focus Mode Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Strict Focus Mode 🔒",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    "Locks the app to screen so you cannot exit while studying",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subTextColor
                                )
                            }
                            Switch(
                                checked = isStrictFocusMode,
                                onCheckedChange = { viewModel.setIsStrictFocusMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFE11D48),
                                    uncheckedThumbColor = subTextColor,
                                    uncheckedTrackColor = cardBorder
                                )
                            )
                        }
                        
                        if (isStudyTargetAlertEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "🔔 You will get a subtle banner alert on this screen when you are exactly 30 minutes or less away from your target time of ${dailyStudyTargetMinutes / 60}h.",
                                style = MaterialTheme.typography.labelSmall,
                                color = subTextColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            
            // Logs Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { isLogsExpanded = !isLogsExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Study Sessions Log", 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Black, 
                            color = textColor
                        )
                        Text(
                            text = if (isLogsExpanded) "Tap arrow to collapse study sessions 🔼" else "Tap arrow to expand and view study sessions 🔽",
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (filteredLogs.isNotEmpty()) {
                            Text(
                                "${filteredLogs.size} sessions", 
                                style = MaterialTheme.typography.labelMedium, 
                                color = subTextColor,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        IconButton(onClick = { isLogsExpanded = !isLogsExpanded }) {
                            Icon(
                                imageVector = if (isLogsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle logs visibility",
                                tint = textColor
                            )
                        }
                    }
                }
            }
            
            // Active timer recording pill if running
            if (secondsElapsed > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x1F6366F1) else Color(0xFFEEF2FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isRunning) Color(0xFF10B981) else Color(0xFFF59E0B))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            if (isRunning) "• RECORDING" else "• PAUSED",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        subject,
                                        color = Color(0xFF6366F1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    chapter.ifEmpty { "General Study" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                            
                            Button(
                                onClick = {
                                    viewModel.stopAndSaveTimer()
                                    isLogsExpanded = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Log", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Saved logs list or empty state
            if (isLogsExpanded) {
                if (filteredLogs.isEmpty() && secondsElapsed == 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "📖 No study logs saved yet", 
                                    color = textColor, 
                                    fontWeight = FontWeight.Bold, 
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Start the timer above and tap Stop to save your session with chapter details.", 
                                    color = subTextColor, 
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredLogs, key = { it.id }) { log ->
                        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        val formattedDate = sdf.format(Date(log.timestamp))
                        
                        val h_log = log.durationSeconds / 3600
                        val m_log = (log.durationSeconds % 3600) / 60
                        val s_log = log.durationSeconds % 60
                        val durationText = when {
                            h_log > 0 -> "${h_log}h ${m_log}m ${s_log}s"
                            m_log > 0 -> "${m_log}m ${s_log}s"
                            else -> "${s_log}s"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val subjectColor = when (log.subject) {
                                            "Physics" -> Color(0xFF0284C7)
                                            "Chemistry" -> Color(0xFFE11D48)
                                            "Mock Test" -> Color(0xFF8B5CF6)
                                            else -> Color(0xFF059669)
                                        }
                                        val subjectIcon = when (log.subject) {
                                            "Physics" -> "⚡"
                                            "Chemistry" -> "🧪"
                                            "Mock Test" -> "📝"
                                            else -> "🧬"
                                        }
                                        Text(
                                            "$subjectIcon ${log.subject}",
                                            color = subjectColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "• $formattedDate",
                                            color = subTextColor,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        log.chapter,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor
                                    )
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0x1A10B981))
                                            .border(1.dp, Color(0x3310B981), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            durationText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = { viewModel.deleteStudyLog(log.id) },
                                        modifier = Modifier.size(36.dp).padding(start = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete log",
                                            tint = Color(0x66FFFFFF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                com.example.ui.components.AppBrandingFooter(isDark = isDark)
            }
        }

        // Chapter Selection Dialog
        if (showChapterSelectDialog) {
            val branches = remember(subject) {
                when {
                    subject.contains("Chemistry", ignoreCase = true) -> listOf("All", "Physical", "Inorganic", "Organic")
                    subject.contains("Biology", ignoreCase = true) -> listOf("All", "Botany", "Zoology")
                    subject.contains("Physics", ignoreCase = true) -> listOf("All", "11th", "12th")
                    else -> listOf("All")
                }
            }

            val filteredChapterItems = remember(currentChapterItems, chapterSearchQuery, branchFilter) {
                currentChapterItems.filter { item ->
                    val matchesQuery = chapterSearchQuery.isBlank() ||
                            item.name.contains(chapterSearchQuery, ignoreCase = true) ||
                            item.branch.contains(chapterSearchQuery, ignoreCase = true) ||
                            item.classLevel.contains(chapterSearchQuery, ignoreCase = true)
                    val matchesBranch = when (branchFilter) {
                        "All" -> true
                        "11th" -> item.classLevel.contains("11th", ignoreCase = true)
                        "12th" -> item.classLevel.contains("12th", ignoreCase = true)
                        else -> item.branch.equals(branchFilter, ignoreCase = true)
                    }
                    matchesQuery && matchesBranch
                }
            }

            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showChapterSelectDialog = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isDark) Color(0xFF131C31) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x33818CF8) else Color(0xFFE2E8F0)),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Select $subject Chapter",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = "${filteredChapterItems.size} chapters available",
                                    fontSize = 12.sp,
                                    color = subTextColor
                                )
                            }
                            IconButton(
                                onClick = { showChapterSelectDialog = false },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Search input
                        OutlinedTextField(
                            value = chapterSearchQuery,
                            onValueChange = { chapterSearchQuery = it },
                            placeholder = { Text("Search chapter name or topic...", fontSize = 14.sp, color = subTextColor) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF818CF8))
                            },
                            trailingIcon = {
                                if (chapterSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { chapterSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = subTextColor, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF818CF8),
                                unfocusedBorderColor = cardBorder,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                cursorColor = Color(0xFF818CF8)
                            )
                        )

                        // Branch/Class filter chips
                        if (branches.size > 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                branches.forEach { b ->
                                    val isSelected = branchFilter == b
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { branchFilter = b },
                                        label = { Text(b, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF818CF8),
                                            selectedLabelColor = Color.White,
                                            containerColor = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9),
                                            labelColor = textColor
                                        ),
                                        border = null,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // If search query is entered and not in list, provide custom option
                        if (chapterSearchQuery.isNotBlank() && filteredChapterItems.none { it.name.equals(chapterSearchQuery.trim(), ignoreCase = true) }) {
                            Card(
                                onClick = {
                                    viewModel.setTimerChapter(chapterSearchQuery.trim())
                                    showChapterSelectDialog = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x266366F1) else Color(0xFFEEF2FF)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "+ Use custom: \"${chapterSearchQuery.trim()}\"",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
                                        )
                                        Text("Set custom chapter or DPP name", fontSize = 11.sp, color = subTextColor)
                                    }
                                }
                            }
                        }

                        // Chapter List
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredChapterItems) { item ->
                                val isSelected = chapter == item.name
                                Card(
                                    onClick = {
                                        viewModel.setTimerChapter(item.name)
                                        showChapterSelectDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            if (isDark) Color(0x336366F1) else Color(0xFFEEF2FF)
                                        } else {
                                            if (isDark) Color(0x0DFFFFFF) else Color(0xFFF8FAFC)
                                        }
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) Color(0xFF818CF8) else cardBorder
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (item.branch.isNotBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (isDark) Color(0x33818CF8) else Color(0xFFE0E7FF))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = item.branch,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF6366F1)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                if (item.classLevel.isNotBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (isDark) Color(0x26FFFFFF) else Color(0xFFE2E8F0))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = item.classLevel,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = subTextColor
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                if (item.pyqWeightage.isNotBlank()) {
                                                    Text(
                                                        text = item.pyqWeightage,
                                                        fontSize = 10.sp,
                                                        color = if (item.pyqWeightage.contains("High", ignoreCase = true)) Color(0xFFEF4444) else subTextColor,
                                                        fontWeight = if (item.pyqWeightage.contains("High", ignoreCase = true)) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 14.sp,
                                                color = if (isSelected) Color(0xFF818CF8) else textColor
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color(0xFF818CF8),
                                                modifier = Modifier.size(20.dp)
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

@Composable
fun FlipDigitCard(
    digit: String,
    label: String,
    fontSize: androidx.compose.ui.unit.TextUnit = 42.sp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(if (fontSize.value > 50) 84.dp else 70.dp)
                .height(if (fontSize.value > 50) 96.dp else 80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF23263B), Color(0xFF11121C))
                    )
                )
                .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = digit,
                transitionSpec = {
                    (slideInVertically(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) { height -> -height } +
                     fadeIn(animationSpec = tween(durationMillis = 300)))
                    .togetherWith(
                        slideOutVertically(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) { height -> height } +
                        fadeOut(animationSpec = tween(durationMillis = 300))
                    )
                },
                label = "flipClockCardDigit"
            ) { targetDigit ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = targetDigit,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Center horizontal split line for Flip Clock style
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                thickness = 2.5.dp,
                color = Color(0xFF090A0F)
            )

            // Split line subtle highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 1.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.1f))
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0x80FFFFFF),
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun FlipClockDisplay(
    secondsElapsed: Int,
    fontSize: androidx.compose.ui.unit.TextUnit = 42.sp,
    targetSeconds: Int? = null
) {
    val displaySeconds = if (targetSeconds != null) {
        (targetSeconds - secondsElapsed).coerceAtLeast(0)
    } else {
        secondsElapsed
    }
    val h = displaySeconds / 3600
    val m = (displaySeconds % 3600) / 60
    val s = displaySeconds % 60

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        FlipDigitCard(digit = String.format(java.util.Locale.US, "%02d", h), label = "HOURS", fontSize = fontSize)
        
        Text(
            text = ":",
            fontSize = (fontSize.value * 0.8).sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF818CF8),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        
        FlipDigitCard(digit = String.format(java.util.Locale.US, "%02d", m), label = "MINS", fontSize = fontSize)
        
        Text(
            text = ":",
            fontSize = (fontSize.value * 0.8).sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF818CF8),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        
        FlipDigitCard(digit = String.format(java.util.Locale.US, "%02d", s), label = "SECS", fontSize = fontSize)
    }
}

