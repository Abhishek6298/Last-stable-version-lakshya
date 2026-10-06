package com.example.ui.screens

import com.example.data.ExamSyllabusDatabase
import com.example.data.getMaxScore
import com.example.ui.components.DailyTargetFireworksCard
import com.example.ui.components.FireworksProgressBar
import androidx.compose.animation.AnimatedVisibility
import com.example.ui.components.GlassCard
import kotlin.math.roundToInt
import androidx.compose.animation.core.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.net.Uri
import com.example.utils.ExportImportHelper
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.utils.NeetProgressPdfGenerator
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyRow
import com.example.ui.components.MathJaxView
import com.example.ui.components.NativeMarkdownText
import com.example.ui.components.needsMathJax
import com.example.util.MathFormatter
import com.example.ui.components.StethoscopeIcon
import com.example.ui.components.DailyAndWeeklyTargetChecklistCard
import com.example.ui.components.PracticeOverviewCard
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi


@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onNavigate: ((String) -> Unit)? = null
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val logs by viewModel.studyLogs.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val tests by viewModel.mockTests.collectAsStateWithLifecycle()
    val dailyPractices by viewModel.dailyPractices.collectAsStateWithLifecycle()
    val scheduledMockTests by viewModel.scheduledMockTests.collectAsStateWithLifecycle()
    val dppItems by viewModel.dppItems.collectAsStateWithLifecycle()
    val completedTopics by viewModel.completedTopics.collectAsStateWithLifecycle()
    val bookProgressions by viewModel.bookProgressions.collectAsStateWithLifecycle()
    val edunitiTargets by viewModel.edunitiTargets.collectAsStateWithLifecycle()
    val neetTargetMillis by viewModel.neetTargetMillis.collectAsStateWithLifecycle()
    val activeExamGoal by viewModel.activeExamGoal.collectAsStateWithLifecycle()
    
    val appPassword by viewModel.appPassword.collectAsStateWithLifecycle()
    val habitPassword by viewModel.habitPassword.collectAsStateWithLifecycle()
    val securityQuestion by viewModel.securityQuestion.collectAsStateWithLifecycle()
    val securityAnswer by viewModel.securityAnswer.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userClass by viewModel.userClass.collectAsStateWithLifecycle()
    val userAvatarUri by viewModel.userAvatarUri.collectAsStateWithLifecycle()
    val geminiApiKey1 by viewModel.geminiApiKey1.collectAsStateWithLifecycle()
    val geminiApiKey2 by viewModel.geminiApiKey2.collectAsStateWithLifecycle()
    val geminiApiKey3 by viewModel.geminiApiKey3.collectAsStateWithLifecycle()
    val selectedGeminiModel by viewModel.selectedGeminiModel.collectAsStateWithLifecycle()
    val aiProvider by viewModel.aiProvider.collectAsStateWithLifecycle()
    val openRouterSelectedModel by viewModel.openRouterSelectedModel.collectAsStateWithLifecycle()
    val isAiAdvisorAnalyzing by viewModel.isAiAdvisorAnalyzing.collectAsStateWithLifecycle()
    val aiAdvisorReport by viewModel.aiAdvisorReport.collectAsStateWithLifecycle()
    val aiAdvisorMessages by viewModel.aiAdvisorMessages.collectAsStateWithLifecycle()
    val isAiAdvisorReplying by viewModel.isAiAdvisorReplying.collectAsStateWithLifecycle()
    val studyTubeTodaySeconds by viewModel.studyTubeTodaySeconds.collectAsStateWithLifecycle()
    val studyTubeTotalSeconds by viewModel.studyTubeTotalSeconds.collectAsStateWithLifecycle()
    val studyTubeLastWatchedInfo by viewModel.studyTubeLastWatchedInfo.collectAsStateWithLifecycle()
    val guardianPulse by viewModel.guardianPulse.collectAsStateWithLifecycle()
    val timerIsRunning by viewModel.timerIsRunning.collectAsStateWithLifecycle()
    val timerSubject by viewModel.timerSubject.collectAsStateWithLifecycle()

    val notificationTimes by viewModel.notificationTimes.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isDark = isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF475569)
    val subTextMuted = if (isDark) Color(0x80FFFFFF) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0x0AFFFFFF) else Color.White
    val cardBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
    val tabBg = if (isDark) Color(0x0DFFFFFF) else Color(0xFFF1F5F9)
    val dialogBg = if (isDark) Color(0xFF000000) else Color.White
    val nowMillis = remember { System.currentTimeMillis() }

    var showNeetDateDialog by remember { mutableStateOf(false) }
    var showPlaygroundDialog by remember { mutableStateOf(false) }
    var showAdvisorDialog by remember { mutableStateOf(false) }
    var showPasswordPrompt by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var neetInputDate by remember { mutableStateOf("") }

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayLogs = remember(logs, todayStart) { logs.filter { it.timestamp >= todayStart } }
    val todayStudySeconds = remember(todayLogs) { todayLogs.sumOf { it.durationSeconds } }
    val todayStudyHours = remember(todayStudySeconds) { todayStudySeconds / 3600f }
    val todayStudyMinutes = remember(todayStudySeconds) { (todayStudySeconds % 3600) / 60 }
    
    val totalStudySeconds = remember(logs) { logs.sumOf { it.durationSeconds } }
    val totalStudyHours = remember(totalStudySeconds) { totalStudySeconds / 3600f }
    
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayPractice = remember(dailyPractices, todayDate) { dailyPractices.find { it.date == todayDate } }
    val pTarget = remember(todayPractice) { (todayPractice?.physicsTarget ?: 45).coerceAtLeast(1) }
    val cTarget = remember(todayPractice) { (todayPractice?.chemistryTarget ?: 45).coerceAtLeast(1) }
    val bTarget = remember(todayPractice) { (todayPractice?.biologyTarget ?: 90).coerceAtLeast(1) }
    val pSolved = remember(todayPractice) { todayPractice?.physicsSolved ?: 0 }
    val cSolved = remember(todayPractice) { todayPractice?.chemistrySolved ?: 0 }
    val bSolved = remember(todayPractice) { todayPractice?.biologySolved ?: 0 }

    val todayTargetTotal = remember(pTarget, cTarget, bTarget) { pTarget + cTarget + bTarget }
    val todaySolvedTotal = remember(pSolved, cSolved, bSolved) { pSolved + cSolved + bSolved }
    val todayOverallPct = remember(todaySolvedTotal, todayTargetTotal) {
        ((todaySolvedTotal.toFloat() / todayTargetTotal.toFloat()) * 100).toInt().coerceIn(0, 100)
    }
    val pPct = remember(pSolved, pTarget) { ((pSolved.toFloat() / pTarget.toFloat()) * 100).toInt().coerceIn(0, 100) }
    val cPct = remember(cSolved, cTarget) { ((cSolved.toFloat() / cTarget.toFloat()) * 100).toInt().coerceIn(0, 100) }
    val bPct = remember(bSolved, bTarget) { ((bSolved.toFloat() / bTarget.toFloat()) * 100).toInt().coerceIn(0, 100) }
    val isTodayTargetComplete = remember(todaySolvedTotal, todayTargetTotal) { todaySolvedTotal >= todayTargetTotal }

    val todaySolvedQuestions = todaySolvedTotal
    val todayTargetQuestions = todayTargetTotal
    val totalPhysicsAllTime = remember(dailyPractices) { dailyPractices.sumOf { it.physicsSolved } }
    val totalChemistryAllTime = remember(dailyPractices) { dailyPractices.sumOf { it.chemistrySolved } }
    val totalBiologyAllTime = remember(dailyPractices) { dailyPractices.sumOf { it.biologySolved } }
    val totalAllTimeQuestions = remember(totalPhysicsAllTime, totalChemistryAllTime, totalBiologyAllTime) {
        totalPhysicsAllTime + totalChemistryAllTime + totalBiologyAllTime
    }
    
    val avgScore = remember(tests) { if (tests.isNotEmpty()) tests.map { it.score }.average().toInt() else 0 }
    val pendingGoals = remember(goals) { goals.filter { it.status != com.example.data.GoalStatus.COMPLETED.value } }
    
    var activeTab by remember { mutableStateOf("Overview") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showQuestionsBreakdownDialog by remember { mutableStateOf(false) }
    
    var mockTimeFilter by remember { mutableStateOf("Recent") } // Recent, Monthly, All
    var practiceTimeFilter by remember { mutableStateOf("Daily") } // Daily, Weekly, Monthly

    // Intercept back presses when dialogs are open on dashboard
    BackHandler(enabled = showSettingsDialog || showNeetDateDialog || showPlaygroundDialog || showAdvisorDialog || showPasswordPrompt || showQuestionsBreakdownDialog) {
        when {
            showSettingsDialog -> showSettingsDialog = false
            showNeetDateDialog -> showNeetDateDialog = false
            showPlaygroundDialog -> showPlaygroundDialog = false
            showAdvisorDialog -> showAdvisorDialog = false
            showPasswordPrompt -> showPasswordPrompt = false
            showQuestionsBreakdownDialog -> showQuestionsBreakdownDialog = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Scrollable Content (including Top Navigation Bar and Header)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Navigation Bar - Scrolls smoothly with dashboard content
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 44.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Circular Menu Button & Hello Future Doctor Card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        // Small circular menu button fixed right beside the card
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDarkMode) Color(0xFF1E1B4B).copy(alpha = 0.9f) else Color(0xFFEEF2FF)
                                )
                                .border(
                                    1.2.dp,
                                    if (isDarkMode) Color(0xFF818CF8).copy(alpha = 0.6f) else Color(0xFFC7D2FE),
                                    CircleShape
                                )
                                .clickable {
                                    viewModel.toggleSidebar()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = if (isDarkMode) Color(0xFFA5B4FC) else Color(0xFF6366F1),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Avatar Profile & Greeting Card ("Hello Future Doctor")
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDarkMode) Color(0x221E293B) else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isDarkMode) Color(0x4D818CF8) else Color(0xFFE2E8F0)
                            ),
                            shadowElevation = if (isDarkMode) 0.dp else 2.dp,
                            modifier = Modifier
                                .clickable {
                                    if (!appPassword.isNullOrBlank()) {
                                        pendingAction = { showSettingsDialog = true }
                                        showPasswordPrompt = true
                                    } else {
                                        showSettingsDialog = true
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899))
                                            )
                                        )
                                        .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!userAvatarUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = userAvatarUri,
                                            contentDescription = "User Avatar",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "User",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Hello, $userName 👋",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = if (isDarkMode) Color.White else textColor,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "$userClass Aspirant 🎯",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Header Action Icons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDarkMode) Color(0xFF1E1B4B).copy(alpha = 0.8f) else Color(0xFFEEF2FF)
                                )
                                .border(
                                    1.dp,
                                    if (isDarkMode) Color(0xFF6366F1).copy(alpha = 0.4f) else Color(0xFFC7D2FE),
                                    CircleShape
                                )
                                .clickable {
                                    if (!appPassword.isNullOrBlank()) {
                                        pendingAction = { showSettingsDialog = true }
                                        showPasswordPrompt = true
                                    } else {
                                        showSettingsDialog = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (isDarkMode) Color(0xFFA5B4FC) else Color(0xFF6366F1),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            if (showPlaygroundDialog) {
                com.example.ui.components.DashboardPlaygroundDialog(
                    isDark = isDarkMode,
                    selectedGeminiModel = selectedGeminiModel,
                    onSelectGeminiModel = { viewModel.setSelectedGeminiModel(it) },
                    onDismissRequest = { showPlaygroundDialog = false }
                )
            }

            if (showAdvisorDialog) {
                val advisorTimerSeconds by viewModel.timerSecondsElapsed.collectAsStateWithLifecycle()
                com.example.ui.components.DashboardAdvisorDialog(
                    isDark = isDarkMode,
                    aiAdvisorMessages = aiAdvisorMessages,
                    isAiAdvisorReplying = isAiAdvisorReplying,
                    isAiAdvisorAnalyzing = isAiAdvisorAnalyzing,
                    guardianPulse = guardianPulse,
                    timerIsRunning = timerIsRunning,
                    timerSubject = timerSubject,
                    timerSecondsElapsed = advisorTimerSeconds,
                    studyTubeTodaySeconds = studyTubeTodaySeconds,
                    studyTubeTotalSeconds = studyTubeTotalSeconds,
                    studyTubeLastWatched = studyTubeLastWatchedInfo,
                    onClearMessages = { viewModel.clearAiAdvisorMessages() },
                    onRunDiagnosticScan = { viewModel.runAiAdvisorAnalysis(context) },
                    onSubmitFeedback = { viewModel.submitFeedbackToAdvisor(context, it) },
                    onStartTimer = { min, sub, chap -> viewModel.guardianStartTimer(sub, chap, min) },
                    onStopTimer = { viewModel.guardianStopTimer() },
                    onExecuteAction = { tag ->
                        viewModel.executeGuardianActionTag(tag) { route ->
                            onNavigate?.invoke(route)
                        }
                    },
                    onDismissRequest = { showAdvisorDialog = false }
                )
            }

            if (showPasswordPrompt && !appPassword.isNullOrBlank()) {
                com.example.ui.components.PasswordPromptDialog(
                    correctPassword = appPassword!!,
                    securityQuestion = securityQuestion,
                    securityAnswer = securityAnswer,
                    title = "Profile Protection 🔐",
                    description = "Enter password to access Profile & App Settings.",
                    onSuccess = {
                        showPasswordPrompt = false
                        pendingAction?.invoke()
                        pendingAction = null
                    },
                    onResetPassword = {
                        viewModel.removeAppPassword()
                        showPasswordPrompt = false
                        pendingAction?.invoke()
                        pendingAction = null
                    },
                    onDismiss = {
                        showPasswordPrompt = false
                        pendingAction = null
                    },
                    isDark = isDarkMode
                )
            }
            
            if (showSettingsDialog) {
                com.example.ui.components.DashboardProfileDialog(
                    appPassword = appPassword,
                    habitPassword = habitPassword,
                    onSetHabitPassword = { viewModel.setHabitPassword(it) },
                    securityQuestion = securityQuestion,
                    securityAnswer = securityAnswer,
                    onSetSecurityLock = { pass, q, a -> viewModel.setSecurityLock(pass, q, a) },
                    onSetAppPassword = { viewModel.setAppPassword(it) },
                    isDark = isDarkMode,
                    userName = userName,
                    userClass = userClass,
                    userAvatarUri = userAvatarUri,
                    geminiApiKey1 = geminiApiKey1,
                    geminiApiKey2 = geminiApiKey2,
                    geminiApiKey3 = geminiApiKey3,
                    selectedGeminiModel = selectedGeminiModel,
                    neetTargetMillis = neetTargetMillis,
                    logs = logs,
                    tests = tests,
                    dailyPractices = dailyPractices,
                    completedTopics = completedTopics,
                    notificationTimes = notificationTimes,
                    currentAiProvider = aiProvider,
                    onUpdateNotificationTime = { key, hour -> viewModel.updateNotificationTime(key, hour, context) },
                    onSaveProfile = { name, uClass, avatarUri, key1, key2, key3, model ->
                        viewModel.setUserProfile(name, uClass, avatarUri)
                        viewModel.saveCustomApiKeys(key1, key2, key3)
                        viewModel.setSelectedGeminiModel(model, switchProvider = false)
                    },
                    onSetDarkMode = { viewModel.setDarkMode(it) },
                    onResetAllData = { viewModel.resetAllData() },
                    onSelectOpenRouterModel = { viewModel.setOpenRouterSelectedModel(it) },
                    onSelectGroqModel = { viewModel.setGroqSelectedModel(it) },
                    onSelectAiProvider = { viewModel.setAiProvider(it) },
                    onDismissRequest = { showSettingsDialog = false }
                )
            }

            if (showQuestionsBreakdownDialog) {
                com.example.ui.components.DashboardQuestionsBreakdownDialog(
                    isDark = isDarkMode,
                    totalAllTimeQuestions = totalAllTimeQuestions,
                    totalPhysicsAllTime = totalPhysicsAllTime,
                    totalChemistryAllTime = totalChemistryAllTime,
                    totalBiologyAllTime = totalBiologyAllTime,
                    todaySolvedQuestions = todaySolvedQuestions,
                    todayTargetQuestions = todayTargetQuestions,
                    pSolvedToday = pSolved,
                    cSolvedToday = cSolved,
                    bSolvedToday = bSolved,
                    pTargetToday = pTarget,
                    cTargetToday = cTarget,
                    bTargetToday = bTarget,
                    onNavigateToAiTest = { onNavigate?.invoke("ai_test") },
                    onNavigateToMistakes = { onNavigate?.invoke("mistakes") },
                    onNavigateToTargets = { onNavigate?.invoke("targets") },
                    onDismissRequest = { showQuestionsBreakdownDialog = false }
                )
            }
            
                        // 🧠 REVISION NOTIFICATION CARD (Spaced Repetition Alert)
            val nowMillisForRevision = System.currentTimeMillis()
            val eligibleRevisionTopics = remember(completedTopics) {
                completedTopics
            }
            val pendingRevisionsCount = remember(eligibleRevisionTopics, nowMillisForRevision) {
                eligibleRevisionTopics.count { (nowMillisForRevision - it.timestamp) >= 3 * 86400000L }
            }
            val urgentRevisionsCount = remember(eligibleRevisionTopics, nowMillisForRevision) {
                eligibleRevisionTopics.count { (nowMillisForRevision - it.timestamp) >= 14 * 86400000L }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (isDarkMode) Color(0xFF1E1B4B).copy(alpha = 0.95f) else Color(0xFFFAF5FF),
                                if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.98f) else Color(0xFFF3E8FF)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFA855F7).copy(alpha = if (isDarkMode) 0.8f else 0.45f),
                                Color(0xFF6366F1).copy(alpha = if (isDarkMode) 0.8f else 0.45f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .clickable {
                        viewModel.setActiveTargetTab("Revision")
                        onNavigate?.invoke("targets")
                    }
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFA855F7), Color(0xFF6366F1))
                                )
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                urgentRevisionsCount > 0 -> "🚨"
                                pendingRevisionsCount > 0 -> "🧠"
                                eligibleRevisionTopics.isNotEmpty() -> "✨"
                                else -> "🧠"
                            },
                            fontSize = 22.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Spaced Repetition Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isDarkMode) Color.White else textColor
                            )
                            if (urgentRevisionsCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                                ) {
                                    Text(
                                        "URGENT",
                                        color = Color(0xFFF87171),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                urgentRevisionsCount > 0 -> "$urgentRevisionsCount topics urgent (>14d decay)"
                                pendingRevisionsCount > 0 -> "$pendingRevisionsCount topics due for revision"
                                eligibleRevisionTopics.isNotEmpty() -> "${eligibleRevisionTopics.size} topics ready in revision"
                                else -> "Complete Dashboard & NCERT to track revisions"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkMode) Color(0xFFC4B5FD) else Color(0xFF7C3AED),
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkMode) Color(0xFF4C1D95).copy(alpha = 0.5f) else Color(0xFFEDE9FE),
                        border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.6f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Open Hub",
                                color = if (isDarkMode) Color(0xFFE9D5FF) else Color(0xFF6D28D9),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = if (isDarkMode) Color(0xFFE9D5FF) else Color(0xFF6D28D9),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))



            DailyQuoteCard(
                isDark = isDarkMode,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textColor = textColor,
                subTextColor = subTextColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            // LAKSHYA AI Guardian Advisor Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (isDarkMode) Color(0xFF31102A).copy(alpha = 0.95f) else Color(0xFFFFF1F2),
                                if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.98f) else Color(0xFFFFE4E6)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFF43F5E).copy(alpha = if (isDarkMode) 0.8f else 0.45f),
                                Color(0xFFA855F7).copy(alpha = if (isDarkMode) 0.8f else 0.45f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .clickable { 
                        showAdvisorDialog = true 
                        if (aiAdvisorReport.contains("actively monitoring") || aiAdvisorReport.contains("monitoring your performance")) {
                            viewModel.runAiAdvisorAnalysis(context)
                        }
                    }
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFF43F5E), Color(0xFFFB7185))
                                )
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛡️", fontSize = 22.sp)
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "LAKSHYA AI Guardian",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isDarkMode) Color.White else textColor
                            )
                            GuardianPulsingDot()
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val pulse = guardianPulse
                        val nextT = pulse.nextUpcomingTest
                        val pulseSummary = if (nextT != null) {
                            "📅 Next: ${nextT.title} (${nextT.daysRemaining}d left) • DPPs: ${pulse.dppCompletedCount}/${pulse.dppTotalCount}"
                        } else {
                            "Readiness: ${pulse.readinessScore}% (${pulse.readinessStatus}) • DPPs: ${pulse.dppCompletedCount}/${pulse.dppTotalCount}"
                        }
                        Text(
                            pulseSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkMode) Color(0xFFFECDD3) else Color(0xFFBE123C),
                            lineHeight = 15.sp,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkMode) Color(0xFF881337).copy(alpha = 0.5f) else Color(0xFFFFE4E6),
                                border = BorderStroke(1.dp, Color(0xFFFB7185).copy(alpha = 0.6f)),
                                modifier = Modifier.clickable {
                                    showAdvisorDialog = true
                                    viewModel.runAiAdvisorAnalysis(context)
                                }
                            ) {
                                Text(
                                    "📈 Full Radar 🔍",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color(0xFFFFE4E6) else Color(0xFF9F1239),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (timerIsRunning) {
                                DashboardTimerChip(
                                    viewModel = viewModel,
                                    timerSubject = timerSubject,
                                    onClick = { onNavigate?.invoke("timer") }
                                )
                            }
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDarkMode) Color(0xFF4C0519).copy(alpha = 0.6f) else Color(0xFFFFE4E6)
                            )
                            .border(
                                1.dp,
                                Color(0xFFF43F5E).copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Guardian Advisor",
                            tint = if (isDarkMode) Color(0xFFFDA4AF) else Color(0xFFE11D48),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))

            // GMC Medical College Cutoff & Safe Score Predictor Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.92f) else Color(0xFFECFDF5),
                                if (isDarkMode) Color(0xFF022C22).copy(alpha = 0.98f) else Color(0xFFD1FAE5)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF10B981).copy(alpha = if (isDarkMode) 0.85f else 0.5f),
                                Color(0xFF059669).copy(alpha = if (isDarkMode) 0.85f else 0.5f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .clickable { onNavigate?.invoke("gmc_cutoffs") }
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF10B981), Color(0xFF34D399))
                                )
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏛️", fontSize = 22.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "GMC College Cutoffs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isDarkMode) Color.White else Color(0xFF064E3B)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF059669).copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, Color(0xFF34D399))
                            ) {
                                Text(
                                    "AIQ & STATE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "AIIMS, MAMC, KGMU, Seth GS & Top State GMC NEET Marks & Ranks",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF047857),
                            lineHeight = 15.sp,
                            fontSize = 11.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val latestMockScore = remember(tests) { tests.maxByOrNull { it.timestamp }?.score ?: tests.lastOrNull()?.score }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF047857).copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.6f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                if (latestMockScore != null) {
                                    val latestMockMax = tests.lastOrNull()?.getMaxScore() ?: 720
                                    Text(
                                        "🎯 Current Mock: $latestMockScore/$latestMockMax • Safe GMCs",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6EE7B7)
                                    )
                                } else {
                                    Text(
                                        "Safe Score Predictor 🎯",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6EE7B7)
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFFA7F3D0)
                            )
                            .border(
                                1.dp,
                                Color(0xFF10B981).copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open GMC Cutoffs",
                            tint = if (isDarkMode) Color(0xFF6EE7B7) else Color(0xFF047857),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))

            // 🧠 LAKSHYA AI CBT TEST ENGINE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (isDarkMode) Color(0xFF1E1B4B).copy(alpha = 0.95f) else Color(0xFFFAF5FF),
                                if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.98f) else Color(0xFFF3E8FF)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF6366F1).copy(alpha = if (isDarkMode) 0.85f else 0.5f),
                                Color(0xFFA855F7).copy(alpha = if (isDarkMode) 0.85f else 0.5f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .clickable { onNavigate?.invoke("ai_test") }
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                                )
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🤖", fontSize = 22.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "AI CBT Test Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isDarkMode) Color.White else Color(0xFF2E1065)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFD97706).copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, Color(0xFFFBBF24))
                            ) {
                                Text(
                                    "1988–2026 PYQ ✨",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "NEET & JEE Full Mocks, Chapter Tests & PDF-to-MCQ CBT Live Simulator",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkMode) Color(0xFFC7D2FE) else Color(0xFF4338CA),
                            fontSize = 11.5.sp,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkMode) Color(0xFF312E81).copy(alpha = 0.6f) else Color(0xFFEEF2FF),
                                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.6f))
                            ) {
                                Text(
                                    "NEET • JEE Main • Adv 🎯",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF4F46E5),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkMode) Color(0xFF581C87).copy(alpha = 0.6f) else Color(0xFFFDF4FF),
                                border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.6f))
                            ) {
                                Text(
                                    "PDF MCQ Scanner 📄",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color(0xFFD8B4FE) else Color(0xFFDB2777),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDarkMode) Color(0xFF312E81).copy(alpha = 0.6f) else Color(0xFFE0E7FF)
                            )
                            .border(
                                1.dp,
                                Color(0xFF6366F1).copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open AI Test Engine",
                            tint = if (isDarkMode) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Modern Stats Row (Study Time & Solved Today)
            val isStudyTimeHappy = todayStudyHours >= 6f
            val studyEmoji = if (isStudyTimeHappy) "🤩" else "⚡"
            val isSolvedHappy = isTodayTargetComplete && todayTargetTotal > 0
            val solvedEmoji = if (isSolvedHappy) "🔥" else "🎯"
            
            val edunitiTargetsToday = edunitiTargets
            val edunitiSolvedToday = edunitiTargetsToday.sumOf { it.questionsSolved }
            val studyProgressPct = (todayStudyHours / 6f).coerceIn(0f, 1f)
            val solvedProgressPct = if (todayTargetQuestions > 0) (todaySolvedQuestions.toFloat() / todayTargetQuestions.toFloat()).coerceIn(0f, 1f) else 0f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Study Time Glass Tile
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (isDarkMode) Color(0xFF1E1B4B).copy(alpha = 0.85f) else Color(0xFFFFFFFF),
                                    if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.92f) else Color(0xFFF8FAFC)
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF6366F1).copy(alpha = if (isDarkMode) 0.75f else 0.45f),
                                    Color(0xFFA855F7).copy(alpha = if (isDarkMode) 0.75f else 0.45f)
                                )
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isDarkMode) Color(0xFFA5B4FC) else Color(0xFF6366F1),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "STUDY TIME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkMode) Color(0xFFA5B4FC) else Color(0xFF6366F1),
                                    letterSpacing = 1.sp,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(studyEmoji, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            if (todayStudyHours >= 1f) {
                                Text(
                                    "${todayStudyHours.toInt()}h ${todayStudyMinutes}m",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDarkMode) Color.White else textColor
                                )
                            } else {
                                Text(
                                    "${todayStudyMinutes}m",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDarkMode) Color.White else textColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Progress Bar for 6h Goal
                        FireworksProgressBar(
                            progress = studyProgressPct,
                            height = 6.dp,
                            gradientColors = listOf(Color(0xFF6366F1), Color(0xFFA855F7)),
                            sparkColor = Color(0xFFA855F7),
                            isDark = isDarkMode
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Target: 6h", style = MaterialTheme.typography.labelSmall, color = if (isDarkMode) Color(0xFF94A3B8) else subTextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text("${(studyProgressPct * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA855F7), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }

                // Cumulative Questions Solved (Physics, Chemistry, Biology All-Time Tracker)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.85f) else Color(0xFFFFFFFF),
                                    if (isDarkMode) Color(0xFF022C22).copy(alpha = 0.92f) else Color(0xFFF8FAFC)
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF10B981).copy(alpha = if (isDarkMode) 0.75f else 0.45f),
                                    Color(0xFF38BDF8).copy(alpha = if (isDarkMode) 0.75f else 0.45f)
                                )
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .clickable { showQuestionsBreakdownDialog = true }
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    "TOTAL SOLVED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkMode) Color(0xFF6EE7B7) else Color(0xFF059669),
                                    letterSpacing = 0.8.sp,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDarkMode) Color(0x3310B981) else Color(0xFFE6F4EA))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    if (todaySolvedQuestions > 0) "+$todaySolvedQuestions today" else "ALL-TIME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkMode) Color(0xFF6EE7B7) else Color(0xFF059669),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "$totalAllTimeQuestions",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                " Qs",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDarkMode) Color(0xFF94A3B8) else subTextColor,
                                modifier = Modifier.padding(bottom = 2.dp, start = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Multi-color segmented ratio bar: Physics (Cyan), Chemistry (Amber), Biology (Emerald)
                        val safeTotal = totalAllTimeQuestions.coerceAtLeast(1).toFloat()
                        val pWeight = (totalPhysicsAllTime.toFloat() / safeTotal).coerceAtLeast(0.001f)
                        val cWeight = (totalChemistryAllTime.toFloat() / safeTotal).coerceAtLeast(0.001f)
                        val bWeight = (totalBiologyAllTime.toFloat() / safeTotal).coerceAtLeast(0.001f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isDarkMode) Color(0x33334155) else Color(0xFFE2E8F0))
                        ) {
                            if (totalAllTimeQuestions == 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .background(Color(0xFF94A3B8).copy(alpha = 0.3f))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(pWeight)
                                        .fillMaxHeight()
                                        .background(Color(0xFF38BDF8))
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(cWeight)
                                        .fillMaxHeight()
                                        .background(Color(0xFFF59E0B))
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(bWeight)
                                        .fillMaxHeight()
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Physics, Chemistry, Biology Question Counters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                                Text("P:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color(0xFF94A3B8) else subTextMuted)
                                Text("$totalPhysicsAllTime", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF38BDF8))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                                Text("C:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color(0xFF94A3B8) else subTextMuted)
                                Text("$totalChemistryAllTime", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                Text("B:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color(0xFF94A3B8) else subTextMuted)
                                Text("$totalBiologyAllTime", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Auto-Adding Sources Subtitle
                        Text(
                            "AI CBT • OCR • Practice ⚡",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkMode) Color(0xFF6EE7B7).copy(alpha = 0.8f) else Color(0xFF059669),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))

            // Modern Floating Segmented Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(50),
                        spotColor = if (isDarkMode) Color(0x336366F1) else Color(0x18475569)
                    )
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isDarkMode) {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xCC1E293B),
                                    Color(0xD9131C2E),
                                    Color(0xEA0F172A)
                                )
                            )
                        } else {
                            Brush.linearGradient(listOf(Color(0xF8FFFFFF), Color(0xEEF1F5F9)))
                        }
                    )
                    .border(
                        1.2.dp,
                        if (isDarkMode) {
                            Brush.linearGradient(
                                listOf(
                                    Color(0x66FFFFFF),
                                    Color(0x4D818CF8),
                                    Color(0x2638BDF8)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xF5FFFFFF),
                                    Color(0x4D6366F1),
                                    Color(0x33CBD5E1)
                                )
                            )
                        },
                        RoundedCornerShape(50)
                    )
                    .padding(5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Overview", "Progress", "Timetable").forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                                    )
                                } else {
                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable { activeTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tab,
                            color = if (isSelected) Color.White else (if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)),
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeTab == "Overview") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // EXECUTIVE 2x2 HERO KPI GRID
                    val heroDiffMillis = (neetTargetMillis - nowMillis).coerceAtLeast(0L)
                    val heroTotalSecs = heroDiffMillis / 1000
                    val heroDays = heroTotalSecs / 86400
                    val heroTargetDateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(neetTargetMillis))

                    val totalMasterChapters = ExamSyllabusDatabase.getAllChaptersForExam().size.coerceAtLeast(1)
                    val defaultSubtopicList = listOf(
                        "Theory & NCERT Reading",
                        "Concept & Solved Examples",
                        "Exercise-1 (Topic MCQs)",
                        "Exercise-2 (Advanced Practice)",
                        "PYQs (Past 10 Years Questions)"
                    )
                    val completedChaptersCount = bookProgressions.count { prog ->
                        val map = com.example.ui.screens.parseSubtopicsProgress(prog.subtopicsProgress)
                        if (map.isNotEmpty()) {
                            defaultSubtopicList.all { sub -> map[sub] == "Completed" }
                        } else {
                            prog.progressPercent == 100 && prog.status == "Completed"
                        }
                    }
                    val inProgressChaptersCount = bookProgressions.count { prog ->
                        val map = com.example.ui.screens.parseSubtopicsProgress(prog.subtopicsProgress)
                        if (map.isNotEmpty()) {
                            !defaultSubtopicList.all { sub -> map[sub] == "Completed" } && map.values.any { it != "Not Started" }
                        } else {
                            prog.progressPercent in 1..99
                        }
                    }
                    val sumChapterProgress: Int = bookProgressions.sumOf { prog ->
                        val map = com.example.ui.screens.parseSubtopicsProgress(prog.subtopicsProgress)
                        if (map.isNotEmpty()) {
                            val earnedPoints = defaultSubtopicList.sumOf { sub ->
                                when (map[sub]) {
                                    "Completed" -> 2
                                    "Ongoing" -> 1
                                    else -> 0
                                }
                            }
                            (earnedPoints * 100) / (defaultSubtopicList.size * 2)
                        } else prog.progressPercent
                    }
                    val overallSyllabusPct = if (totalMasterChapters > 0) {
                        ((sumChapterProgress.toDouble() / (totalMasterChapters.toDouble() * 100.0)) * 100.0).roundToInt().coerceIn(0, 100)
                    } else 0
                    val completedSubtopicsCount = completedTopics.size

                    val heroLatestMock = remember(tests) { tests.maxByOrNull { it.timestamp } }
                    val heroLatestScore = heroLatestMock?.score ?: 0
                    val heroLatestMax = remember(heroLatestMock) { heroLatestMock?.getMaxScore() ?: 720 }

                    val studyStreakData = remember(dailyPractices, logs) {
                        val activeDates = mutableSetOf<String>()
                        dailyPractices.forEach { dp ->
                            if ((dp.physicsSolved + dp.chemistrySolved + dp.biologySolved) > 0) {
                                activeDates.add(dp.date)
                            }
                        }
                        logs.forEach { log ->
                            if (log.durationSeconds > 0) {
                                val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(log.timestamp))
                                activeDates.add(d)
                            }
                        }
                        val cal = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val todayStr = sdf.format(cal.time)

                        var currentStreak = 0
                        val isTodayActive = activeDates.contains(todayStr)
                        if (isTodayActive) {
                            currentStreak++
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                        } else {
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                            val yesterdayStr = sdf.format(cal.time)
                            if (activeDates.contains(yesterdayStr)) {
                                // Yesterday was active, streak alive
                            } else {
                                currentStreak = 0
                            }
                        }

                        if (currentStreak > 0 || isTodayActive) {
                            while (true) {
                                val dateStr = sdf.format(cal.time)
                                if (activeDates.contains(dateStr)) {
                                    currentStreak++
                                    cal.add(Calendar.DAY_OF_YEAR, -1)
                                } else {
                                    break
                                }
                            }
                        }

                        val past7List = mutableListOf<Pair<String, Boolean>>()
                        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                        for (i in 6 downTo 0) {
                            val tempCal = Calendar.getInstance()
                            tempCal.add(Calendar.DAY_OF_YEAR, -i)
                            val k = sdf.format(tempCal.time)
                            val label = dayFormat.format(tempCal.time).take(1)
                            past7List.add(label to activeDates.contains(k))
                        }

                        Triple(currentStreak, isTodayActive, past7List)
                    }

                    val currentStreak = studyStreakData.first
                    val isTodayStreakActive = studyStreakData.second
                    val weeklyStreakDays = studyStreakData.third

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // KPI Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardKpiTile(
                                title = "Study Streak",
                                valueStr = "$currentStreak ${if (currentStreak == 1) "Day" else "Days"}",
                                subtitle = if (isTodayStreakActive) "🔥 Active Today! Keep going!" else "⚡ Complete study or MCQs!",
                                badgeText = if (isTodayStreakActive) "🔥 Active" else "⚡ Inactive",
                                badgeColor = if (isTodayStreakActive) Color(0xFFF97316) else Color(0xFFEF4444),
                                iconEmoji = "🔥",
                                gradientColors = listOf(Color(0xFFF97316), Color(0xFFEF4444)),
                                progress = if (currentStreak > 0) (currentStreak / 30f).coerceIn(0.1f, 1f) else 0f,
                                isDark = isDarkMode,
                                modifier = Modifier.weight(1f)
                            )

                            DashboardKpiTile(
                                title = "NEET Exam",
                                valueStr = "$heroDays Days",
                                subtitle = "Target: $heroTargetDateFormatted",
                                badgeText = "🔴 LIVE",
                                badgeColor = Color(0xFFEF4444),
                                iconEmoji = "⏳",
                                gradientColors = listOf(Color(0xFF6366F1), Color(0xFF3B82F6)),
                                progress = 1f,
                                isDark = isDarkMode,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    neetInputDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(neetTargetMillis))
                                    showNeetDateDialog = true
                                }
                            )
                        }

                        // KPI Row 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardKpiTile(
                                title = "Syllabus Coverage",
                                valueStr = "$completedChaptersCount / $totalMasterChapters Ch.",
                                subtitle = "$overallSyllabusPct% Syllabus • $completedSubtopicsCount Subtopics",
                                badgeText = "$overallSyllabusPct%",
                                badgeColor = Color(0xFF10B981),
                                iconEmoji = "📚",
                                gradientColors = listOf(Color(0xFF10B981), Color(0xFF06B6D4)),
                                progress = (overallSyllabusPct / 100f).coerceIn(0f, 1f),
                                isDark = isDarkMode,
                                modifier = Modifier.weight(1f)
                            )

                            DashboardKpiTile(
                                title = "Mock Test Score",
                                valueStr = if (heroLatestMock != null) "$heroLatestScore / $heroLatestMax" else "No Test",
                                subtitle = if (heroLatestMock != null) "Latest Test Result" else "Schedule a Mock Test",
                                badgeText = if (heroLatestMock != null && heroLatestScore >= (heroLatestMax * 0.7f)) "🏆 Excellent" else if (heroLatestScore > 0) "📈 Practice" else "🆕 New",
                                badgeColor = if (heroLatestMock != null && heroLatestScore >= (heroLatestMax * 0.7f)) Color(0xFFF59E0B) else Color(0xFF818CF8),
                                iconEmoji = "🏆",
                                gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFEA580C)),
                                progress = if (heroLatestMock != null && heroLatestMax > 0) (heroLatestScore.toFloat() / heroLatestMax.toFloat()).coerceIn(0f, 1f) else 0f,
                                isDark = isDarkMode,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // REVISION NOTIFICATION BANNER
                    val pendingRevisions = eligibleRevisionTopics.count { (nowMillis - it.timestamp) > 3 * 86400000L }
                    
                    if (pendingRevisions > 0) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.5.dp,
                                    Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444))),
                                    RoundedCornerShape(24.dp)
                                )
                                .clickable {
                                    viewModel.setActiveTargetTab("Revision")
                                    onNavigate?.invoke("targets")
                                }
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x33EF4444)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🧠", fontSize = 22.sp)
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text("Spaced Revisions Due", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                                            Text("$pendingRevisions topics need active recall review", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFEF4444)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("Revise", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 0. TOP STUDY STREAK & MOMENTUM BANNER
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.5.dp,
                                Brush.horizontalGradient(
                                    if (isTodayStreakActive) {
                                        listOf(Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFEAB308))
                                    } else {
                                        listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                                    }
                                ),
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isTodayStreakActive) Color(0x33F97316) else Color(0x22F59E0B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🔥", fontSize = 22.sp)
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = if (currentStreak > 0) "🔥 $currentStreak Day Study Streak!" else "⚡ Start Your Study Streak!",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = textColor
                                            )
                                            if (!isTodayStreakActive) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFEF4444))
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isTodayStreakActive) {
                                                "Awesome job! Streak active for today. Keep building your momentum!"
                                            } else if (currentStreak > 0) {
                                                "Streak at risk! Solve 1 MCQ or log study time today to keep your $currentStreak-day streak alive!"
                                            } else {
                                                "Log study time or solve practice questions today to ignite your streak!"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = subTextColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 7-Day Consistency Tracker Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                weeklyStreakDays.forEach { (dayLabel, isActive) ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isActive) {
                                                        Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEF4444)))
                                                    } else {
                                                        Brush.linearGradient(
                                                            if (isDark) listOf(Color(0x15FFFFFF), Color(0x15FFFFFF))
                                                            else listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))
                                                        )
                                                    }
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isActive) Color(0xFFFDBA74) else Color.Transparent,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isActive) {
                                                Text("🔥", fontSize = 15.sp)
                                            } else {
                                                Text(
                                                    text = dayLabel,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0x66FFFFFF) else Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                        Text(
                                            text = dayLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isActive) (if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C))
                                                    else (if (isDark) Color(0x66FFFFFF) else Color(0xFF94A3B8))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 1. PRACTICE OVERVIEW (WEEKLY & MONTHLY PRACTICE STATUS)
                    PracticeOverviewCard(
                        dailyPractices = dailyPractices,
                        isDark = isDarkMode,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. UPCOMING MOCK TEST DATE & SYLLABUS CARD
                    val activeScheduled = scheduledMockTests.filter { !it.isCompleted }
                    val nextTest = activeScheduled.firstOrNull()

                    GlassCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    1.dp,
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFF818CF8), Color(0xFFC084FC))
                                                    ),
                                                    RoundedCornerShape(28.dp)
                                                )
                                        ) {
                                            Column(modifier = Modifier.padding(20.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("📝", fontSize = 20.sp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text("Upcoming Mock Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = textColor)
                                                            Text("Scheduled test & syllabus", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                                if (nextTest == null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(cardBg)
                                                            .padding(14.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("No upcoming mock test scheduled. Schedule tests in the Mock Test section.", fontSize = 12.sp, color = subTextColor)
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(18.dp))
                                                            .background(cardBg)
                                                            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                                                            .padding(14.dp)
                                                    ) {
                                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                Text(nextTest.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .background(Color(0x33818CF8))
                                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                                ) {
                                                                    Text("🗓️ ${nextTest.scheduledDate}", fontSize = 10.5.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(10.dp))
                                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                if (nextTest.physicsSyllabus.isNotBlank()) {
                                                                    Box(
                                                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x1460A5FA)).padding(8.dp)
                                                                    ) {
                                                                        Text("⚡ Physics: ${nextTest.physicsSyllabus}", fontSize = 12.sp, color = Color(0xFF93C5FD), fontWeight = FontWeight.Medium)
                                                                    }
                                                                }
                                                                if (nextTest.biologySyllabus.isNotBlank()) {
                                                                    Box(
                                                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x1434D399)).padding(8.dp)
                                                                    ) {
                                                                        Text("🧬 Biology: ${nextTest.biologySyllabus}", fontSize = 12.sp, color = Color(0xFF6EE7B7), fontWeight = FontWeight.Medium)
                                                                    }
                                                                }
                                                                if (nextTest.chemistrySyllabus.isNotBlank()) {
                                                                    Box(
                                                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x14F59E0B)).padding(8.dp)
                                                                    ) {
                                                                        Text("🧪 Chemistry: ${nextTest.chemistrySyllabus}", fontSize = 12.sp, color = Color(0xFFFCD34D), fontWeight = FontWeight.Medium)
                                                                    }
                                                                }
                                                                if (nextTest.physicsSyllabus.isBlank() && nextTest.chemistrySyllabus.isBlank() && nextTest.biologySyllabus.isBlank() && nextTest.syllabusNotes.isNotBlank()) {
                                                                    Box(
                                                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x22000000)).padding(8.dp)
                                                                    ) {
                                                                        Text("📖 Syllabus: ${nextTest.syllabusNotes}", fontSize = 12.sp, color = textColor)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                    // 3. TODAY'S DPP TARGET STATUS CARD
                    val dppPhysicsList = dppItems.filter { it.subject == "Physics" }
                        val dppChemList = dppItems.filter { it.subject == "Chemistry" }
                        val dppBioList = dppItems.filter { it.subject == "Biology" }

                        val phyDone = dppPhysicsList.count { it.isCompleted }
                        val chemDone = dppChemList.count { it.isCompleted }
                        val bioDone = dppBioList.count { it.isCompleted }

                        val totalDppCount = dppItems.size
                        val totalDppDone = dppItems.count { it.isCompleted }
                        val dppPct = if (totalDppCount > 0) ((totalDppDone.toFloat() / totalDppCount.toFloat()) * 100).toInt() else 0

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("📑", fontSize = 22.sp)
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("DPP Target Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = textColor)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (totalDppDone > 0 && totalDppDone == totalDppCount) Color(0x3310B981) else Color(0x33F59E0B))
                                                    .padding(horizontal = 8.dp, vertical = 2.5.dp)
                                            ) {
                                                Text(
                                                    text = if (totalDppCount > 0 && totalDppDone == totalDppCount) "🎉 Target Done!" else "$totalDppDone / $totalDppCount Done",
                                                    color = if (totalDppCount > 0 && totalDppDone == totalDppCount) Color(0xFF34D399) else Color(0xFFF59E0B),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.5.sp,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                        Text("Daily Practice Problems", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x1460A5FA))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text("⚡ Physics DPP", fontSize = 11.sp, color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("$phyDone Done", fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x14F59E0B))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text("🧪 Chemistry DPP", fontSize = 11.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("$chemDone Done", fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x1434D399))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text("🧬 Biology DPP", fontSize = 11.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("$bioDone Done", fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                    // 4. SYLLABUS PROGRESS TRACKER CARD
                    val completedCount = completedChaptersCount
                    val remainingCount = (totalMasterChapters - completedCount).coerceAtLeast(0)

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("📚", fontSize = 22.sp)
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Syllabus Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = textColor)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x3310B981))
                                                .padding(horizontal = 8.dp, vertical = 2.5.dp)
                                        ) {
                                            Text("$overallSyllabusPct% Done", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                        }
                                    }
                                    Text("$completedCount / $totalMasterChapters Chapters Completed • $completedSubtopicsCount Subtopics", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            FireworksProgressBar(
                                progress = overallSyllabusPct / 100f,
                                height = 8.dp,
                                gradientColors = listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF00F5A0)),
                                sparkColor = Color(0xFF34D399),
                                isDark = isDark
                            )
                        }
                    }

                    // DAILY TARGET MESSAGE BANNER
                    val targetPhysics = todayPractice?.physicsTarget ?: 45
                    val targetChem = todayPractice?.chemistryTarget ?: 45
                    val targetBio = todayPractice?.biologyTarget ?: 90
                    val totalTarget = targetPhysics + targetChem + targetBio
                    
                    val solvedPhysics = todayPractice?.physicsSolved ?: 0
                    val solvedChem = todayPractice?.chemistrySolved ?: 0
                    val solvedBio = todayPractice?.biologySolved ?: 0
                    val totalSolved = solvedPhysics + solvedChem + solvedBio
                    
                    val remaining = (totalTarget - totalSolved).coerceAtLeast(0)
                    val pct = if (totalTarget > 0) ((totalSolved.toFloat() / totalTarget.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

                    val todayGoals = goals.filter { (it.targetType == "today" || it.targetType.isBlank()) && (it.date.isBlank() || it.date == todayDate) }
                    val pendingTodayGoals = todayGoals.filter { it.status != com.example.data.GoalStatus.COMPLETED.value }

                    DailyTargetFireworksCard(
                        totalTarget = totalTarget,
                        totalSolved = totalSolved,
                        targetPhysics = targetPhysics,
                        solvedPhysics = solvedPhysics,
                        targetChem = targetChem,
                        solvedChem = solvedChem,
                        targetBio = targetBio,
                        solvedBio = solvedBio,
                        isDark = isDark,
                        onQuickAdd = { subject, count ->
                            viewModel.recordSolvedQuestions(subject, count)
                        }
                    )

                    // DEDICATED DAILY & WEEKLY TARGETS CHECKLIST (Best-in-Class Interactive UI)
                    DailyAndWeeklyTargetChecklistCard(
                        goals = goals,
                        isDark = isDarkMode,
                        onToggleGoalStatus = { goalId, newStatus ->
                            viewModel.updateGoalStatus(goalId, newStatus)
                        },
                        onDeleteGoal = { goalId ->
                            viewModel.deleteGoal(goalId)
                        },
                        onAddGoal = { subject, text, targetType, date ->
                            viewModel.addGoal(subject, text, targetType, date)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    // Dedicated AI Engine Status Card (Reflects OpenRouter vs Native Gemini)
                    val isOpenRouterActive = aiProvider == com.example.data.AiProvider.OPENROUTER
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Tapping opens profile / AI settings
                                onNavigate?.invoke("profile")
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                val engineTitle = if (isOpenRouterActive) "OpenRouter AI Hub Active" else "Lakshya AI 3.7 Flash Mode"
                                Text(engineTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = textColor)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isOpenRouterActive) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF6366F1).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            if (isOpenRouterActive) "OPENROUTER" else "3.7 FLASH",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOpenRouterActive) Color(0xFF10B981) else Color(0xFF6366F1),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    val engineSubtitle = if (isOpenRouterActive) {
                                        "Model: ${openRouterSelectedModel.substringAfterLast("/")} • Gemini is OFF"
                                    } else {
                                        "Dedicated Google Gemini direct API for all AI features"
                                    }
                                    Text(engineSubtitle, style = MaterialTheme.typography.bodySmall, color = subTextColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isOpenRouterActive) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF6366F1).copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    if (isOpenRouterActive) "🌐 Active" else "⚡ Active",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isOpenRouterActive) Color(0xFF10B981) else Color(0xFF6366F1)
                                )
                            }
                        }
                    }
                    }
                } else if (activeTab == "Progress") {
                ProgressScreen(viewModel = viewModel, modifier = Modifier.fillMaxWidth(), showFooter = false)
            } else {
                com.example.ui.components.AiTimetableSection(
                    viewModel = viewModel,
                    isDark = isDarkMode,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }

            // App Branding & Copyright Warning Footer on Main Dashboard
            com.example.ui.components.AppBrandingFooter(isDark = isDarkMode)
        }
    }

        if (showNeetDateDialog) {
            AlertDialog(
                onDismissRequest = { showNeetDateDialog = false },
                title = { Text("Set NEET Exam Target Date", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Enter target NEET exam date (YYYY-MM-DD):", fontSize = 12.sp, color = Color(0x99FFFFFF))
                        OutlinedTextField(
                            value = neetInputDate,
                            onValueChange = { neetInputDate = it },
                            label = { Text("YYYY-MM-DD", color = Color(0x99FFFFFF)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFEC4899), unfocusedBorderColor = Color(0x1AFFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("NEET 2026" to "2026-05-03", "NEET 2027" to "2027-05-03", "NEET 2028" to "2028-05-07").forEach { (label, target) ->
                                Button(
                                    onClick = { neetInputDate = target },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(label, fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val parsed = sdf.parse(neetInputDate)
                            if (parsed != null) {
                                viewModel.setNeetTargetMillis(parsed.time)
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                        showNeetDateDialog = false
                    }) {
                        Text("Save Target", color = Color(0xFFEC4899))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNeetDateDialog = false }) {
                        Text("Cancel", color = Color(0x99FFFFFF))
                    }
                },
                containerColor = Color(0xFF1E1E2E)
            )
        }
    }

@Composable
fun OldGlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    GlassCard(modifier = modifier, cornerRadius = 24.dp) {
        content()
    }
}

@Composable
fun DailyQuoteCard(
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    subTextColor: Color
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentQuote by remember { mutableStateOf("Fetching daily inspiration...") }
    var currentAuthor by remember { mutableStateOf("Lakshya AI") }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val savedDate = prefs.getString("gemini_quote_date", "")
        val savedQuote = prefs.getString("gemini_quote_text", "")
        val savedAuthor = prefs.getString("gemini_quote_author", "")
        val seenSet = prefs.getStringSet("seen_inspiration_history", emptySet())?.toMutableSet() ?: mutableSetOf()
        
        if (savedDate == todayStr && !savedQuote.isNullOrEmpty() && savedAuthor != null && savedAuthor != "Unknown") {
            currentQuote = savedQuote
            currentAuthor = if (savedAuthor.isBlank() || savedAuthor == "Gemini" || savedAuthor == "Unknown" || savedAuthor == "Inspiration") "Dr. A.P.J. Abdul Kalam" else savedAuthor
            isLoading = false
        } else {
            isLoading = true
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val result = com.example.data.GeminiChatAssistant.getDailyInspiration(context, seenQuotes = seenSet)
                result.onSuccess { text ->
                    if (text.contains("|")) {
                        val parts = text.split("|")
                        currentQuote = parts[0].trim().removeSurrounding("\"").removeSurrounding("\'")
                        val rawAuthor = parts[1].trim().removeSurrounding("\"").removeSurrounding("\'")
                        currentAuthor = if (rawAuthor.isBlank() || rawAuthor == "Gemini" || rawAuthor == "Unknown" || rawAuthor == "Inspiration") "Dr. A.P.J. Abdul Kalam" else rawAuthor
                    } else {
                        currentQuote = text.trim().removeSurrounding("\"").removeSurrounding("\'")
                        currentAuthor = "Dr. A.P.J. Abdul Kalam"
                    }
                    seenSet.add(currentQuote)
                    val finalSeenSet = if (seenSet.size > 500) seenSet.toList().takeLast(300).toSet() else seenSet
                    prefs.edit()
                        .putString("gemini_quote_date", todayStr)
                        .putString("gemini_quote_text", currentQuote)
                        .putString("gemini_quote_author", currentAuthor)
                        .putStringSet("seen_inspiration_history", finalSeenSet)
                        .apply()
                    isLoading = false
                }.onFailure {
                    val staticQuote = com.example.utils.MotivationalQuotes.getQuoteOfTheDay(seenSet)
                    currentQuote = staticQuote.text
                    currentAuthor = staticQuote.author
                    seenSet.add(currentQuote)
                    val finalSeenSet = if (seenSet.size > 500) seenSet.toList().takeLast(300).toSet() else seenSet
                    prefs.edit()
                        .putString("gemini_quote_date", todayStr)
                        .putString("gemini_quote_text", currentQuote)
                        .putString("gemini_quote_author", currentAuthor)
                        .putStringSet("seen_inspiration_history", finalSeenSet)
                        .apply()
                    isLoading = false
                }
            }
        }
    }
    
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        cornerRadius = 22.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "✦",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFE2C99D) else Color(0xFFB45309)
                    )
                    Text(
                        text = "LAKSHYA AI DAILY INSPIRATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFE2C99D) else Color(0xFFB45309),
                        letterSpacing = 1.2.sp,
                        fontSize = 10.5.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterHorizontally),
                    color = Color(0xFFA78BFA)
                )
            } else {
                Text(
                    text = "“$currentQuote”",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                    lineHeight = 22.sp,
                    fontSize = 15.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "—— $currentAuthor",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
@Composable
fun OmrTimeBubbleBox(
    bubbleLabel: String,
    unitLabel: String,
    valueStr: String,
    glowColor: Color,
    isDark: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(glowColor.copy(alpha = 0.2f))
                .border(1.dp, glowColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(bubbleLabel, fontSize = 10.sp, fontWeight = FontWeight.Black, color = glowColor)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(glowColor.copy(alpha = if (isDark) 0.22f else 0.12f))
                .border(1.5.dp, glowColor.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = valueStr,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = if (isDark) Color.White else glowColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = unitLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SubjectNotificationChip(
    emoji: String,
    subject: String,
    solved: Int,
    target: Int,
    pct: Int,
    color: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0x0AFFFFFF) else Color(0xFFF8FAFC))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 13.sp)
            Text(
                text = subject,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$pct%",
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "$solved / $target Qs",
            fontSize = 9.sp,
            color = if (isDark) Color(0x80FFFFFF) else Color(0xFF64748B)
        )
    }
}

@Composable
fun DashboardKpiTile(
    title: String,
    valueStr: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    iconEmoji: String,
    gradientColors: List<Color>,
    progress: Float? = null,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val kpiShape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 5.dp,
                shape = kpiShape,
                spotColor = gradientColors.firstOrNull()?.copy(alpha = if (isDark) 0.35f else 0.20f) ?: Color(0x336366F1),
                ambientColor = if (isDark) Color(0x220B101E) else Color(0x100F172A)
            )
            .clip(kpiShape)
            .background(
                if (isDark) {
                    Brush.linearGradient(
                        listOf(
                            Color(0xCC1E293B),
                            Color(0xD9131C2E),
                            Color(0xEA0F172A)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color(0xF8FFFFFF),
                            Color(0xEEF8FAFC),
                            Color(0xEEF1F5F9)
                        )
                    )
                }
            )
            .border(
                1.2.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (isDark) 0.65f else 0.85f),
                        gradientColors.firstOrNull()?.copy(alpha = if (isDark) 0.85f else 0.6f) ?: Color(0xFF6366F1),
                        gradientColors.lastOrNull()?.copy(alpha = if (isDark) 0.45f else 0.3f) ?: Color(0xFF38BDF8)
                    )
                ),
                kpiShape
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(gradientColors)
                        )
                        .border(1.2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(iconEmoji, fontSize = 20.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = if (isDark) 0.25f else 0.15f))
                        .border(1.dp, badgeColor.copy(alpha = if (isDark) 0.6f else 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        badgeText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) badgeColor else badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                valueStr,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                fontSize = 19.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (progress != null) {
                FireworksProgressBar(
                    progress = progress,
                    height = 6.dp,
                    gradientColors = gradientColors,
                    sparkColor = gradientColors.firstOrNull() ?: Color(0xFF6366F1),
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 10.5.sp,
                maxLines = 1,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DashboardTimerChip(
    viewModel: AppViewModel,
    timerSubject: String,
    onClick: () -> Unit
) {
    val seconds by viewModel.timerSecondsElapsed.collectAsStateWithLifecycle()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF10B981).copy(alpha = 0.2f),
        border = BorderStroke(1.dp, Color(0xFF10B981)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            "⏱️ $timerSubject (${seconds / 60}m)",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10B981),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun GuardianPulsingDot() {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "GuardianBlink")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 650, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "GuardianDotAlpha"
    )
    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 650, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "GuardianDotScale"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(CircleShape)
            .background(Color(0xFF10B981))
    )
}


