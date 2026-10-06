package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.example.data.*
import com.example.ui.AppViewModel
import com.example.ui.components.AiQuestionSolutionDeepExplainerDialog
import com.example.ui.components.CbtDisplayTheme
import com.example.ui.components.CbtEyeCareSettingsDialog
import com.example.ui.components.CbtThemeManager
import com.example.ui.components.CbtThemePalette
import com.example.ui.components.ChatMessageRenderer
import com.example.ui.components.FireworksProgressBar
import com.example.ui.components.GlassCard
import com.example.ui.components.MathJaxView
import com.example.ui.components.QuickAiEngineSwitchDialog
import com.example.ui.components.OpenRouterModelPickerDialog
import com.example.ui.components.GroqModelPickerDialog
import com.example.ui.components.GeminiModelPickerDialog
import com.example.ui.components.CloudflareModelPickerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTestScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigate: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDark = isDarkMode

    val activeExam by viewModel.activeTestExam.collectAsStateWithLifecycle()
    val activeType by viewModel.activeTestType.collectAsStateWithLifecycle()
    val activeTitle by viewModel.activeTestTitle.collectAsStateWithLifecycle()
    val activeQuestions by viewModel.activeTestQuestions.collectAsStateWithLifecycle()
    val currentQIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val isTestActive by viewModel.isTestActive.collectAsStateWithLifecycle()
    val isTestSubmitted by viewModel.isTestSubmitted.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isAiTestGenerating.collectAsStateWithLifecycle()
    val genError by viewModel.aiTestGenerationError.collectAsStateWithLifecycle()
    val timeRemaining by viewModel.testTimeRemainingSeconds.collectAsStateWithLifecycle()
    val scoreSummary by viewModel.testScoreSummary.collectAsStateWithLifecycle()
    val savedTests by viewModel.aiSavedTests.collectAsStateWithLifecycle()
    val smartOcrProgress by viewModel.smartOcrProgress.collectAsStateWithLifecycle()
    val currentQuestionTime by viewModel.currentQuestionTimeSpentSeconds.collectAsStateWithLifecycle()

    val currentAiProvider by viewModel.aiProvider.collectAsStateWithLifecycle()
    val selectedGeminiModel by viewModel.selectedGeminiModel.collectAsStateWithLifecycle()
    val geminiModels by viewModel.geminiModels.collectAsStateWithLifecycle()
    val isGeminiLoadingModels by viewModel.isGeminiLoadingModels.collectAsStateWithLifecycle()
    val openRouterSelectedModel by viewModel.openrouterSelectedModel.collectAsStateWithLifecycle()
    val openRouterModels by viewModel.openrouterModels.collectAsStateWithLifecycle()
    val isOpenRouterLoadingModels by viewModel.isOpenRouterLoadingModels.collectAsStateWithLifecycle()
    val groqSelectedModel by viewModel.groqSelectedModel.collectAsStateWithLifecycle()
    val groqModels by viewModel.groqModels.collectAsStateWithLifecycle()
    val isGroqLoadingModels by viewModel.isGroqLoadingModels.collectAsStateWithLifecycle()
    val cloudflareSelectedModel by viewModel.cloudflareSelectedModel.collectAsStateWithLifecycle()
    val cloudflareModels by viewModel.cloudflareModels.collectAsStateWithLifecycle()
    val isCloudflareLoadingModels by viewModel.isCloudflareLoadingModels.collectAsStateWithLifecycle()

    var showQuickAiEngineDialog by remember { mutableStateOf(false) }
    var showGeminiModelPickerInTestScreen by remember { mutableStateOf(false) }
    var showOpenRouterModelPickerInTestScreen by remember { mutableStateOf(false) }
    var showGroqModelPickerInTestScreen by remember { mutableStateOf(false) }
    var showCloudflareModelPickerInTestScreen by remember { mutableStateOf(false) }

    // Hub State
    var fullTestCount by remember { mutableIntStateOf(30) }

    // Mistakes Revision State
    var revisionSubject by remember { mutableStateOf("All") }
    var revisionFilterScope by remember { mutableStateOf("ALL") }
    var revisionWebsiteSource by remember { mutableStateOf("") }

    // Full Mock Adaptive CBT State
    var mockIncludeMistakesAndSkipped by remember { mutableStateOf(false) }
    var mockWebsiteSource by remember { mutableStateOf("") }

    // AI CBT Engine Hub Mode Selector State
    var hubSelectedMode by rememberSaveable { mutableStateOf("Chapter-Wise") } // "Chapter-Wise", "Full Mock", "Mistakes", "PDF OCR", "Saved", "All"

    // Chapter-Wise AI CBT State
    var chapterSubject by remember { mutableStateOf("Physics") }
    var chapterSelectedName by remember { mutableStateOf("Units & Measurements") }
    var chapterQuestionCount by remember { mutableIntStateOf(25) }
    var chapterIsCustomCount by remember { mutableStateOf(false) }
    var chapterCustomCountText by remember { mutableStateOf("25") }
    var chapterDifficulty by remember { mutableStateOf("NEET Standard") }
    var chapterWebsiteSource by remember { mutableStateOf("") }
    var chapterCustomCommand by remember { mutableStateOf("") }

    val cbtUnmasteredMistakes by viewModel.cbtUnmasteredMistakesCount.collectAsStateWithLifecycle()
    val cbtUnmasteredSkipped by viewModel.cbtUnmasteredSkippedCount.collectAsStateWithLifecycle()
    val cbtMasteredCount by viewModel.cbtMasteredCount.collectAsStateWithLifecycle()

    LaunchedEffect(activeExam) {
        viewModel.refreshCbtMistakeStats(context, activeExam)
    }

    // Document / PDF / Image OCR state
    var selectedDocUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDocName by remember { mutableStateOf<String?>(null) }
    var selectedInstituteFilter by remember { mutableStateOf("All") }

    val availableInstitutes = remember(savedTests) {
        val list = mutableListOf("All")
        val insts = savedTests.map { it.institute.ifBlank { "Self/General" } }.distinct().sorted()
        list.addAll(insts)
        list
    }

    val filteredSavedTests = remember(savedTests, selectedInstituteFilter) {
        if (selectedInstituteFilter == "All") savedTests
        else savedTests.filter { it.institute.equals(selectedInstituteFilter, ignoreCase = true) }
    }

    // CBT palette, Eye-Care mode & submit dialog state
    var showPaletteSheet by remember { mutableStateOf(false) }
    var showSubmitConfirmDialog by remember { mutableStateOf(false) }
    var solutionFilter by remember { mutableStateOf("All") } // "All", "Incorrect", "Correct", "Unattempted"

    // Eye-Care & Night Mode CBT Display State (Normal Screen, Night Mode, Deep Dark, Sepia)
    var cbtDisplayTheme by rememberSaveable {
        mutableStateOf(CbtDisplayTheme.NORMAL)
    }
    var cbtFontScale by rememberSaveable { mutableFloatStateOf(1.0f) }
    val cbtPrefs = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }
    var cbtAutoInvertImages by rememberSaveable {
        mutableStateOf(cbtPrefs.getBoolean("cbt_auto_invert_images", false))
    }
    var showEyeCareDialog by remember { mutableStateOf(false) }

    val activeCbtPalette = remember(cbtDisplayTheme) {
        CbtThemeManager.getPalette(cbtDisplayTheme)
    }

    // Handle back presses inside AI CBT Test, scorecard, palettes, and sub-dialogs
    BackHandler {
        when {
            showEyeCareDialog -> showEyeCareDialog = false
            showSubmitConfirmDialog -> showSubmitConfirmDialog = false
            showPaletteSheet -> showPaletteSheet = false
            isGenerating -> viewModel.resetAiTestState()
            isTestActive && !isTestSubmitted -> {
                // In active test, prompt submit/exit confirmation so student doesn't accidentally lose test progress
                showSubmitConfirmDialog = true
            }
            isTestSubmitted -> {
                // Return from scorecard / solutions view back to AI Test Hub
                viewModel.resetAiTestState()
            }
            else -> onNavigateBack()
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedDocUri = uri
            selectedDocName = getAiTestFileNameFromUri(context, uri) ?: "Test_Paper.pdf"
        }
    }

    var selectedAnswerKeyUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAnswerKeyName by remember { mutableStateOf<String?>(null) }

    val answerKeyPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedAnswerKeyUri = uri
            selectedAnswerKeyName = getAiTestFileNameFromUri(context, uri) ?: "Answer_Key.pdf"
        }
    }

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)

    // Primary Colors per exam
    val examThemeGradient = when (activeExam) {
        ExamCategory.NEET -> listOf(Color(0xFF10B981), Color(0xFF059669))
        ExamCategory.JEE_MAIN -> listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
        ExamCategory.JEE_ADVANCED -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            if (isTestActive && !isTestSubmitted) {
                // CBT Active Test Top Bar
                CbtTopBar(
                    title = activeTitle,
                    exam = activeExam,
                    timeRemainingSeconds = timeRemaining,
                    currentQuestion = currentQIndex + 1,
                    totalQuestions = activeQuestions.size,
                    currentTheme = cbtDisplayTheme,
                    onOpenEyeCare = { showEyeCareDialog = true },
                    onToggleNormalScreen = {
                        cbtDisplayTheme = if (cbtDisplayTheme == CbtDisplayTheme.NORMAL) {
                            CbtDisplayTheme.NIGHT_SLATE
                        } else {
                            CbtDisplayTheme.NORMAL
                        }
                    },
                    palette = activeCbtPalette,
                    onOpenPalette = { showPaletteSheet = true },
                    onSubmitClick = { showSubmitConfirmDialog = true },
                    onBackClick = { showSubmitConfirmDialog = true },
                    isDark = activeCbtPalette.isDarkEquivalent
                )
            } else {
                // Hub Top Bar
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(examThemeGradient)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "LAKSHYA AI Test Engine",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textColor
                                )
                                Text(
                                    "AI CBT Practice & OCR Custom Tests",
                                    fontSize = 11.sp,
                                    color = subTextColor
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (showPaletteSheet) {
                                showPaletteSheet = false
                            } else if (showSubmitConfirmDialog) {
                                showSubmitConfirmDialog = false
                            } else if (isGenerating) {
                                viewModel.resetAiTestState()
                            } else if (isTestSubmitted) {
                                viewModel.resetAiTestState()
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                        }
                    },
                    actions = {
                        // AI Engine Switcher Chip (Gemini vs OpenRouter vs Groq vs Cloudflare)
                        val engineColor = when (currentAiProvider) {
                            AiProvider.NATIVE_GEMINI -> Color(0xFF6366F1)
                            AiProvider.OPENROUTER -> Color(0xFF10B981)
                            AiProvider.GROQ -> Color(0xFFF97316)
                            AiProvider.CLOUDFLARE -> Color(0xFFF6821F)
                        }
                        val engineLabel = when (currentAiProvider) {
                            AiProvider.NATIVE_GEMINI -> "✦ ${selectedGeminiModel.substringAfterLast("/").take(13)}"
                            AiProvider.OPENROUTER -> "🌐 ${openRouterSelectedModel.substringAfterLast("/").take(12)}"
                            AiProvider.GROQ -> "⚡ ${groqSelectedModel.substringAfterLast("/").take(12)}"
                            AiProvider.CLOUDFLARE -> "☁️ ${cloudflareSelectedModel.substringAfterLast("/").take(12)}"
                        }

                        Surface(
                            color = engineColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, engineColor.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clickable { showQuickAiEngineDialog = true }
                                .padding(end = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    engineLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = engineColor
                                )
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Switch Engine",
                                    tint = engineColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Surface(
                            color = examThemeGradient[0].copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, examThemeGradient[0].copy(alpha = 0.4f)),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                activeExam.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = examThemeGradient[0],
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isGenerating -> {
                    // AI Generating Radar
                    AiGeneratingLoadingView(
                        exam = activeExam,
                        isDark = isDark,
                        errorText = genError,
                        smartOcrProgress = smartOcrProgress,
                        onStopAndStartEarly = { viewModel.stopExtractionAndLaunchActiveTest() },
                        onRetry = { viewModel.resetAiTestState() }
                    )
                }

                isTestActive && !isTestSubmitted -> {
                    // LIVE CBT TEST SCREEN with Eye-Care & Deep Dark Support
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(activeCbtPalette.screenBackground)
                    ) {
                        val currentQuestion = activeQuestions.getOrNull(currentQIndex)
                        if (currentQuestion != null) {
                            CbtQuestionView(
                                question = currentQuestion,
                                questionIndex = currentQIndex,
                                questionLiveSeconds = currentQuestionTime,
                                totalQuestions = activeQuestions.size,
                                questions = activeQuestions,
                                exam = activeExam,
                                palette = activeCbtPalette,
                                fontScale = cbtFontScale,
                                onOptionSelected = { opt -> viewModel.selectQuestionOption(currentQIndex, opt) },
                                onNumericalAnswerChanged = { ans -> viewModel.setNumericalResponse(currentQIndex, ans) },
                                onClear = { viewModel.clearQuestionResponse(currentQIndex) },
                                onToggleReview = { viewModel.toggleQuestionReview(currentQIndex) },
                                onPrev = { viewModel.setCurrentQuestionIndex(currentQIndex - 1) },
                                onJumpToQuestion = { viewModel.setCurrentQuestionIndex(it) },
                                onNext = {
                                    if (currentQIndex < activeQuestions.size - 1) {
                                        viewModel.setCurrentQuestionIndex(currentQIndex + 1)
                                    } else {
                                        showSubmitConfirmDialog = true
                                    }
                                },
                                isDark = activeCbtPalette.isDarkEquivalent
                            )
                        }
                    }
                }

                isTestSubmitted && scoreSummary != null -> {
                    // SCORECARD & SOLUTIONS REVIEW VIEW
                    ScorecardAndSolutionsView(
                        summary = scoreSummary!!,
                        questions = activeQuestions,
                        exam = activeExam,
                        title = activeTitle,
                        selectedFilter = solutionFilter,
                        viewModel = viewModel,
                        onNavigate = onNavigate,
                        onFilterChanged = { solutionFilter = it },
                        onReAttemptMistakes = {
                            viewModel.startImmediateWrongQuestionsRetest()
                        },
                        onAddToMistakesNotebook = {
                            viewModel.addAllIncorrectToMistakesNotebook()
                            Toast.makeText(context, "Added wrong questions to Mistakes Notebook! 📕", Toast.LENGTH_SHORT).show()
                        },
                        onBookmarkSingle = { q ->
                            viewModel.addSingleQuestionToMistakes(q)
                            Toast.makeText(context, "Question added to Mistakes Notebook! 📕", Toast.LENGTH_SHORT).show()
                        },
                        onRetakeFullTest = {
                            viewModel.reattemptCurrentTest()
                            Toast.makeText(context, "Re-attempting test with clean timer! 🚀", Toast.LENGTH_SHORT).show()
                        },
                        onBackToHub = {
                            viewModel.resetAiTestState()
                        },
                        isDark = isDark
                    )
                }

                else -> {
                    // HUB VIEW (Selection & Launch Mode)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Exam Switcher: NEET / JEE MAIN / JEE ADVANCED
                        item {
                            ExamCategorySelector(
                                selected = activeExam,
                                onSelect = { viewModel.setExamCategory(it) },
                                isDark = isDark
                            )
                        }

                        // AI CBT Engine Mode Selector Chips
                        item {
                            val modes = listOf(
                                "Chapter-Wise" to "📖 Chapter-Wise CBT",
                                "Full Mock" to "🎯 Full Mock",
                                "Mistakes" to "📕 Mistakes Drill",
                                "PDF OCR" to "📄 PDF & OCR",
                                "Saved" to "💾 Saved Tests",
                                "All" to "🌟 All"
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(modes) { (key, label) ->
                                    val isSel = hubSelectedMode == key
                                    Surface(
                                        onClick = { hubSelectedMode = key },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSel) Color(0xFF6366F1) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSel) Color(0xFF6366F1) else if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
                                        )
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSel) FontWeight.Black else FontWeight.SemiBold,
                                            color = if (isSel) Color.White else textColor,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Error Banner if previous attempt failed
                        if (genError != null) {
                            item {
                                Surface(
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(genError ?: "", color = Color(0xFFEF4444), fontSize = 12.sp, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { viewModel.resetAiTestState() }) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // CARD 1: 📖 Chapter-Wise AI CBT Engine (Physics, Chemistry, Biology + 25, 45, 90, 180, Custom Qs)
                        if (hubSelectedMode == "Chapter-Wise" || hubSelectedMode == "All") {
                            item {
                                ChapterWiseCbtCard(
                                    exam = activeExam,
                                    selectedSubject = chapterSubject,
                                    onSubjectChange = { subj ->
                                        chapterSubject = subj
                                        val chapters = ExamSyllabusDatabase.getChaptersFor(activeExam, subj)
                                        chapterSelectedName = chapters.firstOrNull()?.name ?: "All Topics"
                                    },
                                    selectedChapter = chapterSelectedName,
                                    onChapterChange = { chapterSelectedName = it },
                                    selectedCount = chapterQuestionCount,
                                    onCountChange = { count ->
                                        chapterQuestionCount = count
                                        chapterIsCustomCount = false
                                    },
                                    isCustomCount = chapterIsCustomCount,
                                    onToggleCustomCount = { chapterIsCustomCount = it },
                                    customCountText = chapterCustomCountText,
                                    onCustomCountTextChange = { chapterCustomCountText = it },
                                    difficulty = chapterDifficulty,
                                    onDifficultyChange = { chapterDifficulty = it },
                                    websiteSource = chapterWebsiteSource,
                                    onWebsiteSourceChange = { chapterWebsiteSource = it },
                                    customCommand = chapterCustomCommand,
                                    onCustomCommandChange = { chapterCustomCommand = it },
                                    isGenerating = isGenerating,
                                    onLaunch = {
                                        val effCount = if (chapterIsCustomCount) {
                                            chapterCustomCountText.toIntOrNull()?.coerceIn(5, 200) ?: 25
                                        } else {
                                            chapterQuestionCount
                                        }
                                        viewModel.startChapterWiseAiTest(
                                            context = context,
                                            exam = activeExam,
                                            subject = chapterSubject,
                                            chapter = chapterSelectedName,
                                            questionCount = effCount,
                                            difficulty = chapterDifficulty,
                                            customCommand = chapterCustomCommand,
                                            websiteSource = chapterWebsiteSource
                                        )
                                    },
                                    isDark = isDark
                                )
                            }
                        }

                        // CARD 2: 🎯 39-Years PYQ Full Length Mock Test
                        if (hubSelectedMode == "Full Mock" || hubSelectedMode == "All") {
                            item {
                                FullLengthMockCard(
                                    exam = activeExam,
                                    selectedCount = fullTestCount,
                                    onCountSelected = { fullTestCount = it },
                                    includeMistakesAndSkipped = mockIncludeMistakesAndSkipped,
                                    onToggleIncludeMistakes = { mockIncludeMistakesAndSkipped = it },
                                    websiteSource = mockWebsiteSource,
                                    onWebsiteSourceChange = { mockWebsiteSource = it },
                                    unmasteredCount = cbtUnmasteredMistakes + cbtUnmasteredSkipped,
                                    onLaunch = {
                                        viewModel.startFullLengthMockTest(
                                            context = context,
                                            exam = activeExam,
                                            questionCount = fullTestCount,
                                            includeMistakesAndSkipped = mockIncludeMistakesAndSkipped,
                                            websiteSource = mockWebsiteSource.ifBlank { null }
                                        )
                                    },
                                    isDark = isDark
                                )
                            }
                        }

                        // CARD 3: 📕 Mistakes & Weak Area CBT Revision Retest
                        if (hubSelectedMode == "Mistakes" || hubSelectedMode == "All") {
                            item {
                                MistakesRevisionTestCard(
                                    exam = activeExam,
                                    selectedSubject = revisionSubject,
                                    onSubjectChange = { revisionSubject = it },
                                    unmasteredWrongCount = cbtUnmasteredMistakes,
                                    unmasteredSkippedCount = cbtUnmasteredSkipped,
                                    masteredCount = cbtMasteredCount,
                                    filterScope = revisionFilterScope,
                                    onFilterScopeChange = { revisionFilterScope = it },
                                    websiteSource = revisionWebsiteSource,
                                    onWebsiteSourceChange = { revisionWebsiteSource = it },
                                    onLaunchRevision = {
                                        viewModel.startMistakesRevisionTestFromSaved(
                                            context = context,
                                            exam = activeExam,
                                            subjectFilter = revisionSubject,
                                            includeSkipped = (revisionFilterScope != "WRONG_ONLY"),
                                            onlySkipped = (revisionFilterScope == "SKIPPED_ONLY"),
                                            websiteSource = revisionWebsiteSource.ifBlank { null }
                                        )
                                    },
                                    isDark = isDark
                                )
                            }
                        }

                        // CARD 4: 🪄 Abhi Magic: PDF & Photo Question Extractor
                        if (hubSelectedMode == "PDF OCR" || hubSelectedMode == "All") {
                            item {
                                SmartOcrTestExtractorCard(
                                exam = activeExam,
                                selectedFileName = selectedDocName,
                                selectedFileUri = selectedDocUri,
                                selectedAnswerKeyName = selectedAnswerKeyName,
                                selectedAnswerKeyUri = selectedAnswerKeyUri,
                                onPickDoc = { docPickerLauncher.launch("*/*") },
                                onClearDoc = {
                                    selectedDocUri = null
                                    selectedDocName = null
                                },
                                onPickAnswerKeyDoc = { answerKeyPickerLauncher.launch("*/*") },
                                onClearAnswerKeyDoc = {
                                    selectedAnswerKeyUri = null
                                    selectedAnswerKeyName = null
                                },
                                onLaunchExtraction = { inst, startPg, endPg, scanLang, autoCrop, customDuration, ansUri, ansText ->
                                    if (selectedDocUri != null) {
                                        viewModel.extractTestFromDocumentOrImage(
                                            context = context,
                                            exam = activeExam,
                                            uri = selectedDocUri!!,
                                            fileName = selectedDocName ?: "Uploaded_Test.pdf",
                                            institute = inst,
                                            startPage = startPg,
                                            endPage = endPg,
                                            scanLanguage = scanLang,
                                            autoCropDiagrams = autoCrop,
                                            customDurationMinutes = customDuration,
                                            answerKeyUri = ansUri,
                                            answerKeyText = ansText
                                        )
                                    } else {
                                        Toast.makeText(context, "Please select a PDF or Image question paper first", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                isDark = isDark
                            )
                        }
                    }

                        // RECENT AI TESTS & SAVED PAPERS ARCHIVE
                        if ((hubSelectedMode == "Saved" || hubSelectedMode == "All") && savedTests.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Recent AI Tests Archive",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textColor
                                    )
                                    Surface(
                                        color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "${savedTests.size} TESTS",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF6366F1),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            if (availableInstitutes.size > 2) {
                                item {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        items(availableInstitutes) { inst ->
                                            val isSel = selectedInstituteFilter == inst
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { selectedInstituteFilter = inst },
                                                label = {
                                                    Text(
                                                        if (inst == "All") "All Categories (${savedTests.size})" else "$inst (${savedTests.count { it.institute == inst }})",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF6366F1),
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            items(filteredSavedTests, key = { it.id }) { test ->
                                SavedAiTestItemCard(
                                    test = test,
                                    onReview = { viewModel.loadSavedAiTest(test) },
                                    onRetake = {
                                        viewModel.reattemptSavedAiTest(test)
                                        Toast.makeText(context, "Re-attempting ${test.title}! 🚀", Toast.LENGTH_SHORT).show()
                                    },
                                    onDelete = { viewModel.deleteSavedAiTest(test.id) },
                                    isDark = isDark
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                com.example.ui.components.AppBrandingFooter(isDark = isDark)
                            }
                        }
                    }
                }
            }

            // CBT Eye-Care & Night Mode Dialog
            if (showEyeCareDialog) {
                CbtEyeCareSettingsDialog(
                    currentTheme = cbtDisplayTheme,
                    onSelectTheme = { cbtDisplayTheme = it },
                    fontScale = cbtFontScale,
                    onFontScaleChange = { cbtFontScale = it },
                    autoInvertImages = cbtAutoInvertImages,
                    onAutoInvertImagesChange = {
                        cbtAutoInvertImages = it
                        cbtPrefs.edit().putBoolean("cbt_auto_invert_images", it).apply()
                    },
                    onDismiss = { showEyeCareDialog = false }
                )
            }

            // CBT Question Palette Bottom Sheet / Dialog
            if (showPaletteSheet && isTestActive && !isTestSubmitted) {
                QuestionPaletteDialog(
                    questions = activeQuestions,
                    currentIndex = currentQIndex,
                    onSelectQuestion = { idx ->
                        viewModel.setCurrentQuestionIndex(idx)
                        showPaletteSheet = false
                    },
                    onDismiss = { showPaletteSheet = false },
                    isDark = activeCbtPalette.isDarkEquivalent
                )
            }

            // Submit Confirmation Dialog
            if (showSubmitConfirmDialog) {
                SubmitConfirmationDialog(
                    questions = activeQuestions,
                    onConfirm = {
                        showSubmitConfirmDialog = false
                        viewModel.submitAiTest()
                    },
                    onDismiss = { showSubmitConfirmDialog = false },
                    onExitWithoutSubmit = {
                        showSubmitConfirmDialog = false
                        viewModel.resetAiTestState()
                    },
                    isDark = activeCbtPalette.isDarkEquivalent
                )
            }

            // Quick Engine Switcher & Live Model Picker
            if (showQuickAiEngineDialog) {
                QuickAiEngineSwitchDialog(
                    isDark = isDark,
                    activeProvider = currentAiProvider,
                    onProviderChange = { newProvider ->
                        viewModel.setAiProvider(newProvider)
                        Toast.makeText(context, "Switched to ${newProvider.displayName}", Toast.LENGTH_SHORT).show()
                    },
                    selectedGeminiModel = selectedGeminiModel,
                    onOpenGeminiModelPicker = {
                        showQuickAiEngineDialog = false
                        showGeminiModelPickerInTestScreen = true
                    },
                    selectedOpenRouterModel = openRouterSelectedModel,
                    onOpenModelPicker = {
                        showQuickAiEngineDialog = false
                        showOpenRouterModelPickerInTestScreen = true
                    },
                    selectedGroqModel = groqSelectedModel,
                    onOpenGroqModelPicker = {
                        showQuickAiEngineDialog = false
                        showGroqModelPickerInTestScreen = true
                    },
                    selectedCloudflareModel = cloudflareSelectedModel,
                    onOpenCloudflareModelPicker = {
                        showQuickAiEngineDialog = false
                        showCloudflareModelPickerInTestScreen = true
                    },
                    onDismiss = { showQuickAiEngineDialog = false }
                )
            }

            if (showGeminiModelPickerInTestScreen) {
                GeminiModelPickerDialog(
                    isDark = isDark,
                    currentSelectedModelId = selectedGeminiModel,
                    models = geminiModels,
                    isLoading = isGeminiLoadingModels,
                    onSelectModel = { modelId ->
                        viewModel.setSelectedGeminiModel(modelId)
                        showGeminiModelPickerInTestScreen = false
                        Toast.makeText(context, "Selected Model: ${modelId.substringAfterLast("/")} (Gemini Active ✦)", Toast.LENGTH_SHORT).show()
                    },
                    onRefreshLiveModels = {
                        viewModel.fetchGeminiLiveModels { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = { showGeminiModelPickerInTestScreen = false }
                )
            }

            if (showOpenRouterModelPickerInTestScreen) {
                OpenRouterModelPickerDialog(
                    isDark = isDark,
                    currentSelectedModelId = openRouterSelectedModel,
                    models = openRouterModels,
                    isLoading = isOpenRouterLoadingModels,
                    onSelectModel = { modelId ->
                        viewModel.setOpenRouterSelectedModel(modelId)
                        showOpenRouterModelPickerInTestScreen = false
                        Toast.makeText(context, "Selected Model: ${modelId.substringAfterLast("/")} (OpenRouter Active)", Toast.LENGTH_SHORT).show()
                    },
                    onRefreshLiveModels = {
                        viewModel.fetchOpenRouterLiveModels()
                    },
                    onDismiss = { showOpenRouterModelPickerInTestScreen = false }
                )
            }

            if (showGroqModelPickerInTestScreen) {
                GroqModelPickerDialog(
                    isDark = isDark,
                    currentSelectedModelId = groqSelectedModel,
                    models = groqModels,
                    isLoading = isGroqLoadingModels,
                    onSelectModel = { modelId ->
                        viewModel.setGroqSelectedModel(modelId)
                        showGroqModelPickerInTestScreen = false
                        Toast.makeText(context, "Selected Model: ${modelId.substringAfterLast("/")} (Groq LPU Active ⚡)", Toast.LENGTH_SHORT).show()
                    },
                    onRefreshLiveModels = {
                        viewModel.fetchGroqLiveModels { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = { showGroqModelPickerInTestScreen = false }
                )
            }

            if (showCloudflareModelPickerInTestScreen) {
                CloudflareModelPickerDialog(
                    isDark = isDark,
                    currentSelectedModelId = cloudflareSelectedModel,
                    models = cloudflareModels,
                    isLoading = isCloudflareLoadingModels,
                    onSelectModel = { modelId ->
                        viewModel.setCloudflareSelectedModel(modelId)
                        showCloudflareModelPickerInTestScreen = false
                        Toast.makeText(context, "Selected Model: ${modelId.substringAfterLast("/")} (Cloudflare Active ☁️)", Toast.LENGTH_SHORT).show()
                    },
                    onRefreshLiveModels = {
                        viewModel.fetchCloudflareLiveModels { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = { showCloudflareModelPickerInTestScreen = false }
                )
            }
        }
    }
}

// ----------------------------------------------------
// CBT TOP BAR & QUESTION VIEWER
// ----------------------------------------------------

@Composable
fun CbtTopBar(
    title: String,
    exam: ExamCategory,
    timeRemainingSeconds: Int,
    currentQuestion: Int,
    totalQuestions: Int,
    currentTheme: CbtDisplayTheme = CbtDisplayTheme.NORMAL,
    onOpenEyeCare: () -> Unit = {},
    onToggleNormalScreen: () -> Unit = {},
    palette: CbtThemePalette? = null,
    onOpenPalette: () -> Unit,
    onSubmitClick: () -> Unit,
    onBackClick: () -> Unit,
    isDark: Boolean
) {
    val activePalette = palette ?: CbtThemeManager.getPalette(if (isDark) CbtDisplayTheme.NIGHT_SLATE else CbtDisplayTheme.NORMAL)
    val mins = timeRemainingSeconds / 60
    val secs = timeRemainingSeconds % 60
    val isTimeLow = timeRemainingSeconds <= 300 // under 5 mins

    val timerColor = if (isTimeLow) Color(0xFFEF4444) else if (isDark) Color(0xFF34D399) else Color(0xFF059669)

    Surface(
        color = activePalette.topBarBackground,
        border = BorderStroke(1.dp, activePalette.cardBorder),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(activePalette.optionUnselectedBg)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit / Pause Test",
                        tint = activePalette.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = activePalette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Q $currentQuestion of $totalQuestions • +4 / ${if (exam == ExamCategory.JEE_ADVANCED) "-2" else "-1"} Marks",
                        fontSize = 10.5.sp,
                        color = activePalette.textSecondary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Timer Pill (Display only - custom time configured before starting test)
                Surface(
                    color = timerColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, timerColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = timerColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            String.format(Locale.getDefault(), "%02d:%02d", mins, secs),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.5.sp,
                            color = timerColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(5.dp))

                // Direct 1-Tap "Normal Screen" / "Night Mode" Quick Switch Pill
                Surface(
                    onClick = onToggleNormalScreen,
                    shape = RoundedCornerShape(8.dp),
                    color = if (currentTheme != CbtDisplayTheme.NORMAL) activePalette.textAccent.copy(alpha = 0.22f) else activePalette.optionUnselectedBg,
                    border = BorderStroke(1.dp, if (currentTheme != CbtDisplayTheme.NORMAL) activePalette.textAccent else activePalette.cardBorder),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentTheme != CbtDisplayTheme.NORMAL) {
                            Icon(
                                Icons.Default.WbSunny,
                                contentDescription = "Switch to Normal Screen",
                                tint = activePalette.textPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Normal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activePalette.textPrimary
                            )
                        } else {
                            Icon(
                                Icons.Default.NightlightRound,
                                contentDescription = "Switch to Night Mode",
                                tint = activePalette.textAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Night",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activePalette.textAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Eye-Care Theme & Display Options Button
                IconButton(
                    onClick = onOpenEyeCare,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(activePalette.textAccent.copy(alpha = 0.18f))
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "Eye Protection & Night Mode Settings",
                        tint = activePalette.textAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Palette Grid Button
                IconButton(
                    onClick = onOpenPalette,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(activePalette.optionUnselectedBg)
                ) {
                    Icon(
                        Icons.Default.GridView,
                        contentDescription = "Question Palette",
                        tint = activePalette.textPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Submit Button
                Button(
                    onClick = onSubmitClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 34.dp)
                ) {
                    Text("Submit", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CbtQuestionView(
    question: AiTestQuestion,
    questionIndex: Int,
    questionLiveSeconds: Int = 0,
    totalQuestions: Int,
    questions: List<AiTestQuestion> = emptyList(),
    exam: ExamCategory,
    palette: CbtThemePalette? = null,
    fontScale: Float = 1.0f,
    onOptionSelected: (String) -> Unit,
    onNumericalAnswerChanged: (String) -> Unit = {},
    onClear: () -> Unit,
    onToggleReview: () -> Unit,
    onPrev: () -> Unit,
    onJumpToQuestion: (Int) -> Unit = {},
    onNext: () -> Unit,
    isDark: Boolean
) {
    val activePalette = palette ?: CbtThemeManager.getPalette(if (isDark) CbtDisplayTheme.NIGHT_SLATE else CbtDisplayTheme.LIGHT_DAY)
    val textColor = activePalette.textPrimary
    val subTextColor = activePalette.textSecondary
    val cbtContext = LocalContext.current
    val cbtAutoInvertImages = remember(question.id) {
        cbtContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getBoolean("cbt_auto_invert_images", false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // NTA CBT Section Switcher Tabs
        val sections = remember(questions) {
            questions.map { it.subject }.distinct().filter { it.isNotBlank() }
        }

        if (sections.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sections.forEach { sec ->
                    val isCurrentSec = question.subject.equals(sec, ignoreCase = true)
                    val firstSecIdx = questions.indexOfFirst { it.subject.equals(sec, ignoreCase = true) }
                    val secQuestions = questions.filter { it.subject.equals(sec, ignoreCase = true) }
                    val answeredInSec = secQuestions.count { it.selectedOption != null }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrentSec) Color(0xFF6366F1) else activePalette.cardBackground,
                        border = BorderStroke(
                            1.dp,
                            if (isCurrentSec) Color(0xFF6366F1) else activePalette.cardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (firstSecIdx >= 0) onJumpToQuestion(firstSecIdx)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                sec,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isCurrentSec) Color.White else if (isDark) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "$answeredInSec/${secQuestions.size} done",
                                fontSize = 8.5.sp,
                                color = if (isCurrentSec) Color.White.copy(alpha = 0.85f) else subTextColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Question Scrollable Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = activePalette.cardBackground,
            border = BorderStroke(1.dp, activePalette.cardBorder),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Header Tags: Subject, Chapter, PYQ Year, Per-Question Live Timer, Review Star
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                question.subject.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF6366F1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                            )
                        }

                        if (question.pyqYear.isNotBlank()) {
                            val isJee = question.pyqYear.contains("JEE", ignoreCase = true) || question.pyqYear.contains("AIEEE", ignoreCase = true)
                            val isHcv = question.pyqYear.contains("HCV", ignoreCase = true) || question.pyqYear.contains("Pandey", ignoreCase = true) || question.pyqYear.contains("Benchmark", ignoreCase = true)
                            val badgeColor = when {
                                isJee -> Color(0xFFF59E0B)
                                isHcv -> Color(0xFF8B5CF6)
                                else -> Color(0xFF10B981)
                            }
                            val badgePrefix = when {
                                isJee -> "⚡ "
                                isHcv -> "🏆 "
                                else -> "🩺 "
                            }

                            Surface(
                                color = badgeColor.copy(alpha = 0.15f),
                                border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    badgePrefix + question.pyqYear,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                )
                            }
                        }

                        // Live Per-Question Timer Pill
                        val qSecs = questionLiveSeconds
                        val qTimeFormatted = if (qSecs < 60) "${qSecs}s" else "${qSecs / 60}m ${qSecs % 60}s"
                        Surface(
                            color = Color(0xFF6366F1).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.8.dp, Color(0xFF6366F1).copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "⏱️ $qTimeFormatted",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1)
                                )
                            }
                        }
                    }

                    // Difficulty Badge
                    val diffColor = when (question.difficulty.lowercase()) {
                        "easy" -> Color(0xFF10B981)
                        "hard" -> Color(0xFFEF4444)
                        else -> Color(0xFFF59E0B)
                    }
                    Surface(
                        color = diffColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, diffColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            question.difficulty.uppercase(),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = diffColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    // Institute Badge if available
                    if (question.institute.isNotBlank() && question.institute != "Self/General") {
                        Surface(
                            color = Color(0xFF6366F1).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "🏛️ " + question.institute,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1)
                                )
                            }
                        }
                    }

                    if (question.isMarkedForReview) {
                        Surface(
                            color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("REVIEW", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B5CF6))
                            }
                        }
                    }
                }

                if (question.chapter.isNotBlank()) {
                    Text(
                        question.chapter,
                        fontSize = 11.sp,
                        color = subTextColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = activePalette.cardBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // JEE Advanced Style Paragraph / Comprehension Card
                if (!question.passage.isNullOrBlank()) {
                    PassageCard(
                        passageText = question.passage,
                        isDark = activePalette.isDarkEquivalent,
                        textColor = activePalette.textPrimary,
                        fontScale = fontScale
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Question Statement with LaTeX / Math support
                val qNumDisplay = if (!question.paperQNo.isNullOrBlank() && question.paperQNo != "${questionIndex + 1}") {
                    "Q${questionIndex + 1}. [Paper Q.${question.paperQNo}]"
                } else {
                    "Q${questionIndex + 1}."
                }
                Text(
                    qNumDisplay,
                    fontWeight = FontWeight.Black,
                    fontSize = (15f * fontScale).sp,
                    color = activePalette.textAccent
                )
                Spacer(modifier = Modifier.height(6.dp))
                CbtQuestionStatementText(
                    text = question.questionText,
                    isDark = activePalette.isDarkEquivalent,
                    textColor = activePalette.textPrimary,
                    fontSize = (15.5f * fontScale).sp
                )

                // Visual / Diagram / NCERT Figure Card if present (strictly authentic source images only)
                if (!question.imageUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    QuestionDiagramCard(
                        question = question,
                        isDark = activePalette.isDarkEquivalent,
                        isSolutionMode = false,
                        autoInvertImages = cbtAutoInvertImages
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4 Options (A, B, C, D) - Guaranteed standard MCQ display for ALL questions (Zero typing needed)
                val options = question.getResolvedOptions()

                options.forEach { (letter, optText) ->
                    val isSelected = question.selectedOption?.trim()?.equals(letter, ignoreCase = true) == true
                    val optBorder = if (isSelected) activePalette.optionSelectedBorder else activePalette.optionUnselectedBorder
                    val optBg = if (isSelected) activePalette.optionSelectedBg else activePalette.optionUnselectedBg

                    Surface(
                        onClick = {
                            onOptionSelected(letter)
                            onNumericalAnswerChanged(letter)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = optBg,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, optBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .testTag("option_${letter.lowercase()}_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // PW Style: Bold Option Number/Letter
                            Text(
                                text = letter,
                                fontWeight = FontWeight.Bold,
                                fontSize = (15f * fontScale).sp,
                                color = if (isSelected) Color(0xFF6366F1) else textColor,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            // PW Style: Subtle Vertical Divider Line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(if (activePalette.isDarkEquivalent) Color(0x2EFFFFFF) else Color(0x22000000))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            // Formula / Statement Content
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                CbtOptionStatementText(
                                    text = optText,
                                    isDark = activePalette.isDarkEquivalent,
                                    textColor = activePalette.textPrimary,
                                    fontSize = (14f * fontScale).sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            // PW Style: Sleek Radio Button Circle
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = if (isSelected) Color(0xFF6366F1) else if (activePalette.isDarkEquivalent) Color(0xFF475569) else Color(0xFFCBD5E1),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF6366F1))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // CBT Bottom Navigation Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(14.dp))
                .background(activePalette.bottomBarBg)
                .border(1.dp, activePalette.cardBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous
            OutlinedButton(
                onClick = onPrev,
                enabled = questionIndex > 0,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, activePalette.cardBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = activePalette.textPrimary),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.weight(0.9f).heightIn(min = 42.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Prev", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }

            // Clear
            OutlinedButton(
                onClick = onClear,
                enabled = question.selectedOption != null,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, activePalette.cardBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = activePalette.textPrimary),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.weight(0.8f).heightIn(min = 42.dp)
            ) {
                Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }

            // Mark for Review
            Button(
                onClick = onToggleReview,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (question.isMarkedForReview) Color(0xFF8B5CF6) else activePalette.optionUnselectedBg
                ),
                border = BorderStroke(1.dp, if (question.isMarkedForReview) Color(0xFF8B5CF6) else activePalette.cardBorder),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.weight(1f).heightIn(min = 42.dp)
            ) {
                Icon(
                    imageVector = if (question.isMarkedForReview) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = if (question.isMarkedForReview) Color.White else textColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    if (question.isMarkedForReview) "Unmark" else "Review",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (question.isMarkedForReview) Color.White else textColor,
                    maxLines = 1
                )
            }

            // Next / Save & Next
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.weight(1.2f).heightIn(min = 42.dp)
            ) {
                Text(
                    if (questionIndex == totalQuestions - 1) "Finish ▶" else "Next ▶",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

// ----------------------------------------------------
// QUESTION PALETTE DIALOG (CBT Grid)
// ----------------------------------------------------

@Composable
fun QuestionPaletteDialog(
    questions: List<AiTestQuestion>,
    currentIndex: Int,
    onSelectQuestion: (Int) -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    var selectedSection by remember { mutableStateOf("All") }
    val sections = remember(questions) {
        listOf("All") + questions.map { it.subject }.distinct().filter { it.isNotBlank() }
    }

    val filteredQuestionsWithOriginalIndices = remember(questions, selectedSection) {
        questions.mapIndexed { index, question -> Pair(index, question) }
            .filter { selectedSection == "All" || it.second.subject.equals(selectedSection, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF0F172A) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "NTA Question Palette",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = textColor
                        )
                        Text(
                            "Real exam 5-state navigation",
                            fontSize = 10.sp,
                            color = subTextColor
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section Filter Tabs (Physics / Chemistry / Biology)
                if (sections.size > 2) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        items(sections) { sec ->
                            val isSecSelected = selectedSection == sec
                            val count = if (sec == "All") questions.size else questions.count { it.subject.equals(sec, ignoreCase = true) }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSecSelected) Color(0xFF6366F1) else if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9),
                                border = BorderStroke(0.8.dp, if (isSecSelected) Color(0xFF6366F1) else if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                                modifier = Modifier.clickable { selectedSection = sec }
                            ) {
                                Text(
                                    "$sec ($count)",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSecSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSecSelected) Color.White else textColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Official NTA 5-State Legend
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0x11FFFFFF) else Color(0xFFF8FAFC))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendItem(color = Color(0xFF10B981), label = "Answered", isDark = isDark)
                        LegendItem(color = Color(0xFFEF4444), label = "Not Answered", isDark = isDark)
                        LegendItem(color = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1), label = "Not Visited", isDark = isDark)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        LegendItem(color = Color(0xFF8B5CF6), label = "Marked for Review", isDark = isDark)
                        LegendItem(color = Color(0xFF059669), label = "Ans & Review 🟣🟢", isDark = isDark)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grid of question buttons
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(filteredQuestionsWithOriginalIndices, key = { (_, q) -> q.id }) { (originalIdx, q) ->
                        val isAnswered = q.selectedOption != null
                        val isReview = q.isMarkedForReview
                        val isCurrent = originalIdx == currentIndex
                        val isVisited = originalIdx <= currentIndex || isAnswered || isReview

                        val (btnBg, btnBorder, btnText) = when {
                            isAnswered && isReview -> Triple(Color(0xFF8B5CF6), Color(0xFF10B981), Color.White)
                            isReview -> Triple(Color(0xFF8B5CF6), Color.Transparent, Color.White)
                            isAnswered -> Triple(Color(0xFF10B981), Color.Transparent, Color.White)
                            isVisited -> Triple(Color(0xFFEF4444), Color.Transparent, Color.White)
                            else -> Triple(
                                if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9),
                                if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1),
                                textColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(btnBg)
                                .border(
                                    width = if (isCurrent) 2.5.dp else if (isAnswered && isReview) 2.dp else 1.dp,
                                    color = if (isCurrent) Color(0xFFF59E0B) else btnBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onSelectQuestion(originalIdx)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${originalIdx + 1}",
                                fontSize = 13.sp,
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                color = btnText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, isDark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            label,
            fontSize = 9.5.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
        )
    }
}

// ----------------------------------------------------
// SUBMIT CONFIRMATION DIALOG
// ----------------------------------------------------

@Composable
fun SubmitConfirmationDialog(
    questions: List<AiTestQuestion>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onExitWithoutSubmit: (() -> Unit)? = null,
    isDark: Boolean
) {
    val total = questions.size
    val answered = questions.count { it.selectedOption != null }
    val unanswered = total - answered
    val review = questions.count { it.isMarkedForReview }

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF0F172A) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Submit Test Paper?", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = textColor)
                Text("Are you sure you want to end this test? You cannot modify your answers afterwards.", textAlign = TextAlign.Center, fontSize = 12.sp, color = subTextColor)

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Stats Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatPill("Total", "$total", Color(0xFF6366F1), Modifier.weight(1f), isDark)
                    StatPill("Answered", "$answered", Color(0xFF10B981), Modifier.weight(1f), isDark)
                    StatPill("Left", "$unanswered", Color(0xFFEF4444), Modifier.weight(1f), isDark)
                    StatPill("Review", "$review", Color(0xFF8B5CF6), Modifier.weight(1f), isDark)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) {
                        Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) {
                        Text("Yes, Submit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                if (onExitWithoutSubmit != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onExitWithoutSubmit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Exit Test (Discard & Return to Hub)",
                            color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B),
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatPill(title: String, value: String, color: Color, modifier: Modifier = Modifier, isDark: Boolean) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B), maxLines = 1, softWrap = false)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = color, maxLines = 1, softWrap = false)
        }
    }
}

fun getSubjectIconAndColor(subject: String): Pair<String, Color> {
    val s = subject.lowercase()
    return when {
        s.contains("chem") -> "🧪" to Color(0xFF10B981)
        s.contains("phys") -> "⚛️" to Color(0xFF3B82F6)
        s.contains("bio") || s.contains("bot") || s.contains("zoo") -> "🧬" to Color(0xFF8B5CF6)
        s.contains("math") -> "📐" to Color(0xFFF59E0B)
        else -> "📚" to Color(0xFF6366F1)
    }
}

@Composable
private fun DifficultyDistributionPill(
    label: String,
    questions: List<AiTestQuestion>,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    val total = questions.size
    val correct = questions.count { it.selectedOption != null && it.selectedOption.equals(it.correctOption, ignoreCase = true) }
    val wrong = questions.count { it.selectedOption != null && !it.selectedOption.equals(it.correctOption, ignoreCase = true) }
    val attempted = correct + wrong
    val acc = if (attempted > 0) (correct.toFloat() / attempted) * 100f else 0f

    Surface(
        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A))
            Spacer(modifier = Modifier.height(3.dp))
            Text("$correct / $total", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF6366F1))
            Text("Acc: ${String.format(Locale.getDefault(), "%.0f%%", acc)}", fontSize = 9.sp, color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B))
        }
    }
}

@Composable
fun SubjectWiseDistributionAnalysisCard(
    summary: TestScoreSummary,
    questions: List<AiTestQuestion>,
    selectedSubjectFilter: String,
    onSelectSubjectFilter: (String) -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)

    val breakdown = summary.subjectBreakdown
    val isSingleSubject = breakdown.size <= 1

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isSingleSubject) {
                // ==========================================
                // SINGLE SUBJECT FOCUSED DISTRIBUTION ANALYSIS
                // ==========================================
                val singleEntry = breakdown.entries.firstOrNull()
                val subjectName = singleEntry?.key?.ifBlank { "Subject" } ?: (questions.firstOrNull()?.subject?.ifBlank { "Subject" } ?: "Subject")
                val sc = singleEntry?.value ?: SubjectScore(
                    correct = summary.correctCount,
                    incorrect = summary.incorrectCount,
                    unattempted = summary.unattemptedCount,
                    marks = summary.totalScore,
                    maxMarks = summary.maxScore,
                    positiveMarks = summary.correctCount * 4,
                    negativeMarks = summary.incorrectCount * 1,
                    accuracy = summary.accuracyPercentage
                )
                val (subjIcon, subjColor) = getSubjectIconAndColor(subjectName)
                val totalQ = sc.totalQuestions.coerceAtLeast(1)

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = subjColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(subjIcon, fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "$subjectName Distribution Analysis",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                color = textColor
                            )
                            Text(
                                "Single Subject In-Depth CBT Performance",
                                fontSize = 10.5.sp,
                                color = subTextColor
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = subjColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, subjColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            "${sc.totalQuestions} Questions",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = subjColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Score & Accuracy Pills Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatPill("Subject Score", "${sc.marks}/${sc.maxMarks}", subjColor, Modifier.weight(1f), isDark)
                    val accColor = if (sc.accuracy >= 75f) Color(0xFF10B981) else if (sc.accuracy >= 50f) Color(0xFFF59E0B) else Color(0xFFEF4444)
                    StatPill("Accuracy", String.format(Locale.getDefault(), "%.1f%%", sc.accuracy), accColor, Modifier.weight(1f), isDark)
                    StatPill("Attempt Rate", "${sc.attemptedCount}/${sc.totalQuestions}", Color(0xFF6366F1), Modifier.weight(1f), isDark)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Question Distribution Segmented Visual Bar
                Text("Question Status Distribution", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    val correctWeight = (sc.correct.toFloat() / totalQ).coerceIn(0f, 1f)
                    val incorrectWeight = (sc.incorrect.toFloat() / totalQ).coerceIn(0f, 1f)
                    val unattemptedWeight = (sc.unattempted.toFloat() / totalQ).coerceIn(0f, 1f)

                    if (correctWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(correctWeight)
                                .fillMaxHeight()
                                .background(Color(0xFF10B981))
                        )
                    }
                    if (incorrectWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(incorrectWeight)
                                .fillMaxHeight()
                                .background(Color(0xFFEF4444))
                        )
                    }
                    if (unattemptedWeight > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(unattemptedWeight)
                                .fillMaxHeight()
                                .background(if (isDark) Color(0xFF64748B) else Color(0xFFCBD5E1))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Distribution Legend & Marks Economy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Correct: ${sc.correct} (+${sc.positiveMarks}m)", fontSize = 10.5.sp, color = textColor, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Wrong: ${sc.incorrect} (-${sc.negativeMarks}m)", fontSize = 10.5.sp, color = textColor, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isDark) Color(0xFF64748B) else Color(0xFFCBD5E1)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Skipped: ${sc.unattempted}", fontSize = 10.5.sp, color = subTextColor, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Difficulty Distribution Breakdown
                val easyQs = questions.filter { it.difficulty.equals("Easy", ignoreCase = true) }
                val medQs = questions.filter { it.difficulty.equals("Medium", ignoreCase = true) }
                val hardQs = questions.filter { it.difficulty.equals("Hard", ignoreCase = true) }

                Text("Difficulty-Wise Distribution", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DifficultyDistributionPill("🟢 Easy", easyQs, Modifier.weight(1f), isDark)
                    DifficultyDistributionPill("🟡 Medium", medQs, Modifier.weight(1f), isDark)
                    DifficultyDistributionPill("🔴 Hard", hardQs, Modifier.weight(1f), isDark)
                }

                // Negative Marking Impact Alert if user had penalties
                if (sc.incorrect > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.10f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Negative marking cost you ${sc.negativeMarks} marks in this $subjectName test. Review step-by-step solutions below to fix conceptual slips.",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            } else {
                // ==========================================
                // MULTI-SUBJECT DISTRIBUTION ANALYSIS
                // ==========================================
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Subject-Wise Distribution Analysis",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = textColor
                        )
                        Text(
                            "Comparative distribution & negative marking across subjects",
                            fontSize = 11.sp,
                            color = subTextColor
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF6366F1).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f))
                    ) {
                        Text(
                            "${breakdown.size} Subjects",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Comparative Multi-Subject Segmented Distribution Bar
                val totalAllQuestions = questions.size.coerceAtLeast(1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    breakdown.forEach { (subj, sc) ->
                        val (_, col) = getSubjectIconAndColor(subj)
                        val weight = (sc.totalQuestions.toFloat() / totalAllQuestions).coerceIn(0.01f, 1f)
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .fillMaxHeight()
                                .background(col)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subject bar legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    breakdown.forEach { (subj, sc) ->
                        val (icon, col) = getSubjectIconAndColor(subj)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(col))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$icon $subj (${sc.totalQuestions})", fontSize = 10.sp, color = subTextColor, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Subject Cards Column
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    breakdown.forEach { (subj, sc) ->
                        val (subjIcon, subjColor) = getSubjectIconAndColor(subj)
                        val isFiltered = selectedSubjectFilter.equals(subj, ignoreCase = true)

                        Surface(
                            color = if (isFiltered) subjColor.copy(alpha = 0.12f) else if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(if (isFiltered) 1.5.dp else 1.dp, if (isFiltered) subjColor else cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Subject Header Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(subjIcon, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(subj, fontWeight = FontWeight.Black, fontSize = 13.5.sp, color = subjColor)
                                    }

                                    // Score / Max
                                    Text(
                                        "${sc.marks}/${sc.maxMarks} Marks",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = textColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Progress Bar
                                val progress = (sc.scorePercentage / 100f).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = subjColor,
                                    trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Metrics Row: Accuracy, Attempted, Breakdown
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            color = (if (sc.accuracy >= 75f) Color(0xFF10B981) else if (sc.accuracy >= 50f) Color(0xFFF59E0B) else Color(0xFFEF4444)).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                "Acc: ${String.format(Locale.getDefault(), "%.0f%%", sc.accuracy)}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (sc.accuracy >= 75f) Color(0xFF10B981) else if (sc.accuracy >= 50f) Color(0xFFF59E0B) else Color(0xFFEF4444),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            "✅${sc.correct}  ❌${sc.incorrect}  ⚪${sc.unattempted}",
                                            fontSize = 10.5.sp,
                                            color = textColor,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        )
                                    }

                                    // Filter Solutions for this Subject
                                    Surface(
                                        onClick = { onSelectSubjectFilter(subj) },
                                        color = if (isFiltered) subjColor else subjColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, subjColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            if (isFiltered) "Showing Solutions ✓" else "Review $subj →",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFiltered) Color.White else subjColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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

// ----------------------------------------------------
// (Note: Keyboard system completely removed per requirement - all questions converted to 4 verified options)
// ----------------------------------------------------

// ----------------------------------------------------
// SCORECARD & SOLUTIONS REVIEW VIEW
// ----------------------------------------------------

@Composable
fun ScorecardAndSolutionsView(
    summary: TestScoreSummary,
    questions: List<AiTestQuestion>,
    exam: ExamCategory,
    title: String,
    selectedFilter: String,
    viewModel: AppViewModel,
    onNavigate: ((String) -> Unit)? = null,
    onFilterChanged: (String) -> Unit,
    onReAttemptMistakes: () -> Unit,
    onAddToMistakesNotebook: () -> Unit,
    onBookmarkSingle: (AiTestQuestion) -> Unit,
    onRetakeFullTest: () -> Unit,
    onBackToHub: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val reviewContext = LocalContext.current
    val cbtAutoInvertImages = remember(questions) {
        reviewContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getBoolean("cbt_auto_invert_images", false)
    }

    var activeExplainerQuestion by remember { mutableStateOf<AiTestQuestion?>(null) }
    var activeStudyTubeLensQuestion by remember { mutableStateOf<AiTestQuestion?>(null) }
    var selectedSubjectFilter by remember { mutableStateOf("All") }

    if (activeExplainerQuestion != null) {
        AiQuestionSolutionDeepExplainerDialog(
            question = activeExplainerQuestion!!,
            examTitle = title,
            isDark = isDark,
            onDismiss = { activeExplainerQuestion = null },
            onSaveToMistakes = {
                onBookmarkSingle(activeExplainerQuestion!!)
            },
            onOpenStudyTubeLens = {
                val target = activeExplainerQuestion
                activeExplainerQuestion = null
                activeStudyTubeLensQuestion = target
            }
        )
    }

    if (activeStudyTubeLensQuestion != null) {
        val lensQ = activeStudyTubeLensQuestion!!
        val formattedText = remember(lensQ.id) {
            buildString {
                append("Q${lensQ.id}. ")
                append(lensQ.questionText.trim())
                append("\n\nOptions:")
                val opts = lensQ.getResolvedOptions()
                if (opts.isNotEmpty()) {
                    opts.forEach { (label, text) ->
                        if (text.isNotBlank()) append("\n($label) $text")
                    }
                } else {
                    if (lensQ.optionA.isNotBlank()) append("\n(A) ${lensQ.optionA}")
                    if (lensQ.optionB.isNotBlank()) append("\n(B) ${lensQ.optionB}")
                    if (lensQ.optionC.isNotBlank()) append("\n(C) ${lensQ.optionC}")
                    if (lensQ.optionD.isNotBlank()) append("\n(D) ${lensQ.optionD}")
                }
                if (!lensQ.passage.isNullOrBlank()) {
                    append("\n\nPassage / Context:\n${lensQ.passage}")
                }
                if (lensQ.subject.isNotBlank()) append("\n\n[Subject: ${lensQ.subject}]")
                if (lensQ.chapter.isNotBlank()) append("\n[Chapter: ${lensQ.chapter}]")
            }
        }

        val resolvedImageUri: Uri? = remember(lensQ.id, lensQ.imageUrl) {
            if (!lensQ.imageUrl.isNullOrBlank()) {
                val raw = lensQ.imageUrl!!.trim()
                try {
                    when {
                        raw.startsWith("file://") || raw.startsWith("content://") || raw.startsWith("http://") || raw.startsWith("https://") -> Uri.parse(raw)
                        raw.startsWith("/") -> Uri.fromFile(java.io.File(raw))
                        else -> Uri.parse(raw)
                    }
                } catch (_: Exception) {
                    null
                }
            } else null
        }

        val cbtPrefs = remember { reviewContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }
        val youtubeApiKey = remember { cbtPrefs.getString("youtube_api_key", "") ?: "" }
        val geminiKey = remember {
            val k1 = cbtPrefs.getString("gemini_api_key_1", "") ?: ""
            val k2 = cbtPrefs.getString("gemini_api_key_2", "") ?: ""
            val k3 = cbtPrefs.getString("gemini_api_key_3", "") ?: ""
            k1.ifBlank { k2.ifBlank { k3 } }
        }
        val selectedModel = remember {
            com.example.data.GeminiModelManager.getSelectedModel(reviewContext)
        }

        StudyTubeLensScannerDialog(
            isOpen = true,
            onDismiss = { activeStudyTubeLensQuestion = null },
            initialImageUri = resolvedImageUri,
            initialQueryText = formattedText,
            youtubeApiKey = youtubeApiKey,
            geminiApiKey = geminiKey,
            selectedModel = selectedModel,
            onSelectVideo = { video ->
                activeStudyTubeLensQuestion = null
                viewModel.openInStudyTube(video.id, title = video.title, isPlaylist = false)
                if (onNavigate != null) {
                    onNavigate("studytube")
                }
            }
        )
    }

    val filteredQuestions = remember(questions, selectedFilter, selectedSubjectFilter) {
        questions.filter { q ->
            val matchesStatus = when (selectedFilter) {
                "Incorrect" -> q.selectedOption != null && !q.selectedOption.equals(q.correctOption, ignoreCase = true)
                "Correct" -> q.selectedOption != null && q.selectedOption.equals(q.correctOption, ignoreCase = true)
                "Unattempted" -> q.selectedOption == null
                else -> true
            }
            val matchesSubject = if (selectedSubjectFilter == "All") true else q.subject.equals(selectedSubjectFilter, ignoreCase = true)
            matchesStatus && matchesSubject
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // DETAILED STEP-BY-STEP SOLUTIONS & SCORECARD VIEW
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // HERO SCORECARD CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF131A2E) else Color(0xFFEEF2FF)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF10B981))), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "TEST COMPLETED • SYNCED TO APP",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = textColor, textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Score Big Display
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${summary.totalScore}",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = if (summary.totalScore >= 0) Color(0xFF6366F1) else Color(0xFFEF4444)
                        )
                        Text(
                            " / ${summary.maxScore}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // Rank Tier Badge
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            summary.rankTier,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4 Stat Badges + Average Time per Question
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val avgTimePerQ = if (questions.isNotEmpty()) summary.timeTakenSeconds / questions.size else 0
                        val avgTimeStr = if (avgTimePerQ < 60) "${avgTimePerQ}s" else "${avgTimePerQ / 60}m ${avgTimePerQ % 60}s"

                        StatPill("Accuracy", String.format(Locale.getDefault(), "%.1f%%", summary.accuracyPercentage), Color(0xFF10B981), Modifier.weight(1f), isDark)
                        StatPill("Correct", "${summary.correctCount} Q", Color(0xFF10B981), Modifier.weight(1f), isDark)
                        StatPill("Wrong", "${summary.incorrectCount} Q", Color(0xFFEF4444), Modifier.weight(1f), isDark)
                        StatPill("Total Time", "${summary.timeTakenSeconds / 60}m", Color(0xFF6366F1), Modifier.weight(1f), isDark)
                        StatPill("Avg/Q", avgTimeStr, Color(0xFF8B5CF6), Modifier.weight(1f), isDark)
                    }
                }
            }
        }

        // SUBJECT-WISE DISTRIBUTION ANALYSIS (Multi-Subject or Single-Subject Focused)
        if (summary.subjectBreakdown.isNotEmpty() || questions.isNotEmpty()) {
            item {
                SubjectWiseDistributionAnalysisCard(
                    summary = summary,
                    questions = questions,
                    selectedSubjectFilter = selectedSubjectFilter,
                    onSelectSubjectFilter = { newSubj ->
                        selectedSubjectFilter = if (selectedSubjectFilter.equals(newSubj, ignoreCase = true)) "All" else newSubj
                    },
                    isDark = isDark
                )
            }
        }

        // AI SURGICAL DIAGNOSTIC & WEAK TOPIC EXTRACTOR
        item {
            var customCommandText by remember { mutableStateOf("") }
            var isRunningAiDiagnosis by remember { mutableStateOf(false) }
            var aiDiagnosisReport by remember { mutableStateOf<String?>(null) }
            var candidateWeakTopics by remember { mutableStateOf<List<CandidateWeakTopic>>(emptyList()) }
            val approvedCandidateIds = remember { mutableStateListOf<String>() }

            val activeProvider by viewModel.aiProvider.collectAsStateWithLifecycle()
            val activeGeminiModel by viewModel.selectedGeminiModel.collectAsStateWithLifecycle()
            val activeOpenRouterModel by viewModel.openrouterSelectedModel.collectAsStateWithLifecycle()
            val activeGroqModel by viewModel.groqSelectedModel.collectAsStateWithLifecycle()
            val activeCloudflareModel by viewModel.cloudflareSelectedModel.collectAsStateWithLifecycle()

            val modelLabel = when (activeProvider) {
                AiProvider.NATIVE_GEMINI -> "Gemini: $activeGeminiModel"
                AiProvider.OPENROUTER -> "OpenRouter: ${activeOpenRouterModel.substringAfterLast("/")}"
                AiProvider.GROQ -> "Groq: $activeGroqModel"
                AiProvider.CLOUDFLARE -> "Cloudflare: ${activeCloudflareModel.substringAfterLast("/")}"
            }

            // Time-per-question analytics
            val slowQuestionsCount = remember(questions) { questions.count { it.timeSpentSeconds > 110 } }
            val rushedMistakesCount = remember(questions) {
                questions.count { it.timeSpentSeconds < 25 && !it.isUserAnswerCorrect() && !it.selectedOption.isNullOrBlank() }
            }
            val avgSeconds = remember(questions) {
                if (questions.isNotEmpty()) questions.map { it.timeSpentSeconds }.average().toInt() else 0
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF131A2E) else Color(0xFFF0FDF4)
                ),
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF6366F1))))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "AI Test Doctor & Weak Topic Scanner",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    "Powered by $modelLabel",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Time & Speed Snapshot Pill Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⏱️ Avg Speed", fontSize = 9.5.sp, color = subTextColor)
                                Text("${avgSeconds}s / Q", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF38BDF8))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🐢 >110s Slow", fontSize = 9.5.sp, color = subTextColor)
                                Text("$slowQuestionsCount Qs", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (slowQuestionsCount > 0) Color(0xFFF59E0B) else Color(0xFF10B981))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚡ <25s Silly", fontSize = 9.5.sp, color = subTextColor)
                                Text("$rushedMistakesCount Flaws", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (rushedMistakesCount > 0) Color(0xFFEF4444) else Color(0xFF10B981))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom Command / Prompt Field
                    Text(
                        "Give Custom AI Diagnostic Command:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customCommandText,
                        onValueChange = { customCommandText = it },
                        placeholder = { Text("e.g. Audit my time per question, find where I panicked, and extract weak chapters", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset Quick Command Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "💊 Quick Rx" to "Give me an ultra-compact 3-bullet clinical diagnosis of weak areas and negative mark leakage.",
                            "⏱️ Time Audit" to "Analyze question-by-question time and find where I took excessive time.",
                            "🎯 Weak Chapters" to "Identify top weak topics and calculate negative marks leakage.",
                            "🧠 NCERT Traps" to "Explain which NCERT statement traps caught me in this test."
                        ).forEach { (chipLabel, prompt) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF6366F1).copy(alpha = 0.15f))
                                    .clickable { customCommandText = prompt }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(chipLabel, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // RUN AI BUTTON
                    Button(
                        onClick = {
                            if (isRunningAiDiagnosis) return@Button
                            isRunningAiDiagnosis = true
                            viewModel.analyzeTestAndExtractWeakTopics(
                                context = reviewContext,
                                testTitle = title,
                                exam = exam,
                                questions = questions,
                                customUserPrompt = customCommandText.ifBlank { null }
                            ) { report, candidates ->
                                isRunningAiDiagnosis = false
                                aiDiagnosisReport = report
                                candidateWeakTopics = candidates
                                approvedCandidateIds.clear()
                            }
                        },
                        enabled = !isRunningAiDiagnosis,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        if (isRunningAiDiagnosis) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Diagnosing Speed & Weak Chapters...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consult AI Doctor 🩺", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }

                    // Display Result Report if available
                    if (aiDiagnosisReport != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF06141D) else Color(0xFFF0FDF4)
                            ),
                            border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                val wrongCount = questions.count { !it.isUserAnswerCorrect() && !it.selectedOption.isNullOrBlank() }
                                val healthBadge = when {
                                    wrongCount == 0 -> "🟢 ZERO LEAKAGE" to Color(0xFF10B981)
                                    wrongCount <= 3 -> "🟡 LOW LEAKAGE (-${wrongCount}m)" to Color(0xFFF59E0B)
                                    else -> "🔴 HIGH LEAKAGE (-${wrongCount}m)" to Color(0xFFEF4444)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("🩺 AI DOCTOR CLINICAL Rx", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981), letterSpacing = 0.4.sp)
                                            Text("Post-CBT Clinical Triage", fontSize = 9.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(healthBadge.second.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(healthBadge.first, fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = healthBadge.second)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val topChapter = candidateWeakTopics.maxByOrNull { it.mistakesCount }?.chapter ?: "Clean"
                                    val avgSec = if (questions.isNotEmpty()) questions.map { it.timeSpentSeconds }.average().toInt() else 0

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDark) Color(0xFF1E293B) else Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Marks Leakage", fontSize = 8.5.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                            Text(if (wrongCount == 0) "0 Marks" else "-$wrongCount Marks", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDark) Color(0xFF1E293B) else Color.White,
                                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f)),
                                        modifier = Modifier.weight(1.4f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Primary Weak Area", fontSize = 8.5.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
                                            Text(topChapter, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF6366F1), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDark) Color(0xFF1E293B) else Color.White,
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Avg Speed", fontSize = 8.5.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                            Text("${avgSec}s / Q", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0xFF0F172A) else Color.White,
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        MathJaxView(
                                            text = aiDiagnosisReport!!,
                                            isDark = isDark,
                                            fontSize = 12.sp,
                                            textColor = textColor,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // MANUAL REVIEW & APPROVAL CARD FOR WEAK TOPICS
                        if (candidateWeakTopics.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "🎯 Detected Weak Chapters (${candidateWeakTopics.size})",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF6366F1)
                                            )
                                            Text(
                                                "Review & approve before sending to Weak Topics section:",
                                                fontSize = 10.5.sp,
                                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                            )
                                        }
                                        val pendingCandidates = candidateWeakTopics.filter { it.id !in approvedCandidateIds }
                                        if (pendingCandidates.isNotEmpty()) {
                                            Button(
                                                onClick = {
                                                    pendingCandidates.forEach { cand ->
                                                        viewModel.addWeakTopic(
                                                            subject = cand.subject,
                                                            chapter = cand.chapter,
                                                            topicName = cand.topicName,
                                                            mistakesCount = cand.mistakesCount,
                                                            avgTimeSpentSeconds = cand.avgTimeSpentSeconds,
                                                            severityLevel = cand.severityLevel,
                                                            aiGuidanceNotes = cand.aiGuidanceNotes
                                                        )
                                                        approvedCandidateIds.add(cand.id)
                                                    }
                                                    Toast.makeText(reviewContext, "Approved & added ${pendingCandidates.size} topics to Weak Topics! 🎯", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Approve All (${pendingCandidates.size})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    candidateWeakTopics.forEach { cand ->
                                        val isApproved = cand.id in approvedCandidateIds
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isDark) Color(0xFF0F172A) else Color.White
                                            ),
                                            border = BorderStroke(1.dp, if (isApproved) Color(0xFF10B981) else Color.LightGray.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        val subjColor = when (cand.subject.lowercase()) {
                                                            "physics" -> Color(0xFF3B82F6)
                                                            "chemistry" -> Color(0xFFF59E0B)
                                                            else -> Color(0xFF10B981)
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(subjColor.copy(alpha = 0.15f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(cand.subject, fontSize = 9.sp, fontWeight = FontWeight.Black, color = subjColor)
                                                        }
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            "❌ ${cand.mistakesCount} Mistakes • ⏱️ ${cand.avgTimeSpentSeconds}s/Q",
                                                            fontSize = 9.5.sp,
                                                            color = if (cand.severityLevel == "CRITICAL") Color(0xFFEF4444) else Color(0xFFF59E0B),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        cand.chapter,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textColor
                                                    )
                                                    Text(
                                                        cand.topicName,
                                                        fontSize = 10.sp,
                                                        color = subTextColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                if (isApproved) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Approved ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                                        }
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            viewModel.addWeakTopic(
                                                                subject = cand.subject,
                                                                chapter = cand.chapter,
                                                                topicName = cand.topicName,
                                                                mistakesCount = cand.mistakesCount,
                                                                avgTimeSpentSeconds = cand.avgTimeSpentSeconds,
                                                                severityLevel = cand.severityLevel,
                                                                aiGuidanceNotes = cand.aiGuidanceNotes
                                                            )
                                                            approvedCandidateIds.add(cand.id)
                                                            Toast.makeText(reviewContext, "Approved & added '${cand.chapter}' 🎯", Toast.LENGTH_SHORT).show()
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Approve", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (onNavigate != null) {
                            Button(
                                onClick = { onNavigate("weak_topic_test") },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                modifier = Modifier.fillMaxWidth().height(38.dp)
                            ) {
                                Icon(Icons.Default.Troubleshoot, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Go to Weak Topic Tests in Sidebar 🎯", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 1-TAP REVISION & ACTION BUTTONS
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // If user made mistakes, provide high-yield 1-tap Retest button
                if (summary.incorrectCount > 0) {
                    Button(
                        onClick = onReAttemptMistakes,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "⚡ Re-Test ${summary.incorrectCount} Mistakes Now (Live Revision CBT)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRetakeFullTest,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1.2f).heightIn(min = 44.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Re-Attempt Test 🔁", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }

                    if (summary.incorrectCount > 0) {
                        OutlinedButton(
                            onClick = onAddToMistakesNotebook,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                            modifier = Modifier.weight(1.1f).heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Notebook 📕", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                    }

                    OutlinedButton(
                        onClick = onBackToHub,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Test", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // SOLUTIONS HEADER & FILTER CHIPS
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Step-by-Step AI Solutions",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = textColor
                )
                Text(
                    "${filteredQuestions.size} Questions",
                    fontSize = 11.5.sp,
                    color = subTextColor
                )
            }
        }

        // Subject Filter Chips (Multi-Subject Only)
        if (summary.subjectBreakdown.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedSubjectFilter == "All",
                            onClick = { selectedSubjectFilter = "All" },
                            label = { Text("All Subjects", fontSize = 11.sp, fontWeight = if (selectedSubjectFilter == "All") FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF6366F1),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(summary.subjectBreakdown.keys.toList(), key = { it }) { subj ->
                        val isSel = selectedSubjectFilter.equals(subj, ignoreCase = true)
                        val (icon, col) = getSubjectIconAndColor(subj)
                        val subjCount = summary.subjectBreakdown[subj]?.totalQuestions ?: questions.count { it.subject.equals(subj, ignoreCase = true) }
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedSubjectFilter = if (isSel) "All" else subj },
                            label = { Text("$icon $subj ($subjCount)", fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = col,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Status Filter Chips (All, Incorrect, Correct, Unattempted)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf(
                    "All" to "All (${questions.size})",
                    "Incorrect" to "❌ Incorrect (${summary.incorrectCount})",
                    "Correct" to "✅ Correct (${summary.correctCount})",
                    "Unattempted" to "⚪ Skipped (${summary.unattemptedCount})"
                )
                items(filters, key = { it.first }) { (key, label) ->
                    val isSel = selectedFilter == key
                    FilterChip(
                        selected = isSel,
                        onClick = { onFilterChanged(key) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (filteredQuestions.isEmpty()) {
            item {
                Surface(
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎯", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No Questions Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textColor
                        )
                        Text(
                            "No questions match filter: $selectedFilter${if (selectedSubjectFilter != "All") " in $selectedSubjectFilter" else ""}.",
                            fontSize = 11.5.sp,
                            color = subTextColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // DETAILED QUESTION SOLUTIONS LIST
        itemsIndexed(filteredQuestions) { idx, q ->
            val isCorrect = q.isUserAnswerCorrect()
            val isIncorrect = q.selectedOption != null && !isCorrect
            val isSkipped = q.selectedOption == null

            val statusColor = when {
                isCorrect -> Color(0xFF10B981)
                isIncorrect -> Color(0xFFEF4444)
                else -> if (isDark) Color(0x66FFFFFF) else Color(0xFF94A3B8)
            }

            val qSecs = q.timeSpentSeconds
            val timeDisplay = if (qSecs < 60) "${qSecs}s" else "${qSecs / 60}m ${qSecs % 60}s"
            val paceColor = when {
                q.selectedOption == null -> subTextColor
                qSecs < 45 -> Color(0xFF10B981)
                qSecs <= 120 -> Color(0xFF6366F1)
                else -> Color(0xFFEF4444)
            }
            val paceLabel = when {
                q.selectedOption == null -> "⚪ Skipped ($timeDisplay)"
                qSecs < 45 -> "⚡ Fast ($timeDisplay)"
                qSecs <= 120 -> "⚖️ Balanced ($timeDisplay)"
                else -> "⏳ Slow ($timeDisplay)"
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) Color(0xFF131A2E) else Color.White,
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    when {
                                        isCorrect -> "✅ CORRECT (+4)"
                                        isIncorrect -> "❌ WRONG (${if (exam == ExamCategory.JEE_ADVANCED) "-2" else "-1"})"
                                        else -> "⚪ UNATTEMPTED (0)"
                                    },
                                    color = statusColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                )
                            }

                            // Time spent pill on this question
                            Surface(
                                color = paceColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.8.dp, paceColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    paceLabel,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = paceColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }

                            Text(q.subject, fontSize = 10.sp, color = subTextColor, fontWeight = FontWeight.Bold)

                            val diffColor = when (q.difficulty.lowercase()) {
                                "easy" -> Color(0xFF10B981)
                                "hard" -> Color(0xFFEF4444)
                                else -> Color(0xFFF59E0B)
                            }
                            Surface(
                                color = diffColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    q.difficulty.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = diffColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }

                            if (q.institute.isNotBlank() && q.institute != "Self/General") {
                                Surface(
                                    color = Color(0xFF6366F1).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "🏛️ " + q.institute,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6366F1),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { activeStudyTubeLensQuestion = q },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = "Watch Video Solution in StudyTube Lens", tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                            }
                            IconButton(
                                onClick = { onBookmarkSingle(q) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.BookmarkBorder, contentDescription = "Add to Mistakes", tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // JEE Advanced Style Paragraph / Comprehension Card in Solution Mode
                    if (!q.passage.isNullOrBlank()) {
                        PassageCard(
                            passageText = q.passage,
                            isDark = isDark,
                            textColor = textColor,
                            fontScale = 0.95f
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Question text with Hybrid MathJax 3 Engine (Complex math/science uses MathJax 3, plain English uses native Text)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Q${q.id}. ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF6366F1),
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        CbtQuestionStatementText(
                            text = q.questionText,
                            isDark = isDark,
                            textColor = textColor,
                            fontSize = 13.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Visual / Diagram Card if present (strictly authentic source images only)
                    if (!q.imageUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        QuestionDiagramCard(
                            question = q,
                            isDark = isDark,
                            isSolutionMode = true,
                            autoInvertImages = cbtAutoInvertImages
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // MCQ / VERIFIED ANSWER KEY PILL
                    val resolvedKey = q.resolvedCorrectOption
                    val userAns = q.selectedOption
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = if (isCorrect) Color(0xFF10B981).copy(alpha = 0.15f) else if (isIncorrect) Color(0xFFEF4444).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Your Answer: Option ${userAns ?: "Unattempted"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) Color(0xFF10B981) else if (isIncorrect) Color(0xFFEF4444) else subTextColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Verified Key: Option $resolvedKey",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Render all Options with MathJax formatting
                    if (q.options.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            q.options.forEachIndexed { optIdx, optText ->
                                val optLetter = ('A' + optIdx).toString()
                                val isThisKey = optLetter.equals(q.correctOption, ignoreCase = true)
                                val isThisUserSelected = optLetter.equals(q.selectedOption, ignoreCase = true)

                                val optBorderColor = when {
                                    isThisKey -> Color(0xFF10B981)
                                    isThisUserSelected -> Color(0xFFEF4444)
                                    else -> if (isDark) Color(0x18FFFFFF) else Color(0xFFE2E8F0)
                                }
                                val optBgColor = when {
                                    isThisKey -> Color(0xFF10B981).copy(alpha = if (isDark) 0.18f else 0.1f)
                                    isThisUserSelected -> Color(0xFFEF4444).copy(alpha = if (isDark) 0.18f else 0.1f)
                                    else -> if (isDark) Color(0x08FFFFFF) else Color(0xFFF8FAFC)
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = optBgColor,
                                    border = BorderStroke(1.dp, optBorderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isThisKey) Color(0xFF10B981) else if (isThisUserSelected) Color(0xFFEF4444) else Color(0xFF6366F1).copy(alpha = 0.15f),
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    optLetter,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isThisKey || isThisUserSelected) Color.White else Color(0xFF6366F1)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.weight(1f)) {
                                            CbtOptionStatementText(
                                                text = optText,
                                                isDark = isDark,
                                                textColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                                                fontSize = 12.sp
                                            )
                                        }
                                        if (isThisKey) {
                                            Text("✓ KEY", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                                        } else if (isThisUserSelected) {
                                            Text("YOU", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ✨ PROMINENT LAKSHYA AI DEEP BREAKDOWN & DOUBT SOLVER BUTTON
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { activeExplainerQuestion = q },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "✨ Ask Lakshya AI Deep Breakdown & Doubts",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    // 📺 STUDYTUBE LENS VIDEO SOLUTION BUTTON
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { activeStudyTubeLensQuestion = q },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, Color(0xFFEF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDark) Color(0xFF1E1015) else Color(0xFFFEF2F2),
                            contentColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp)
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (q.hasImage || !q.imageUrl.isNullOrBlank()) "📺 StudyTube Lens: Watch Video Solution (With Diagram)"
                                   else "📺 StudyTube Lens: Watch Video Solution (PW/Doubtnut)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            com.example.ui.components.AppBrandingFooter(isDark = isDark)
        }
    }
}
}

// ----------------------------------------------------
// HUB CARDS & SELECTORS
// ----------------------------------------------------

@Composable
fun ExamCategorySelector(
    selected: ExamCategory,
    onSelect: (ExamCategory) -> Unit,
    isDark: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF131A2E) else Color.White,
        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(6.dp)) {
            ExamCategory.values().forEach { cat ->
                val isSel = selected == cat
                val bgGradient = when (cat) {
                    ExamCategory.NEET -> listOf(Color(0xFF10B981), Color(0xFF059669))
                    ExamCategory.JEE_MAIN -> listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                    ExamCategory.JEE_ADVANCED -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .then(if (isSel) Modifier.background(Brush.linearGradient(bgGradient)) else Modifier.background(Color.Transparent))
                        .clickable { onSelect(cat) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            cat.displayName,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                            color = if (isSel) Color.White else if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            when (cat) {
                                ExamCategory.NEET -> "PCB (720M)"
                                ExamCategory.JEE_MAIN -> "PCM (300M)"
                                ExamCategory.JEE_ADVANCED -> "PCM (IIT)"
                            },
                            fontSize = 8.5.sp,
                            color = if (isSel) Color.White.copy(alpha = 0.85f) else if (isDark) Color(0x66FFFFFF) else Color(0xFF94A3B8),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterPickerDialog(
    subject: String,
    currentChapter: String,
    chapters: List<ExamSyllabusDatabase.ChapterItem>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, chapters) {
        if (searchQuery.isBlank()) chapters
        else chapters.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.branch.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF131A2E) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "$subject Chapters",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            "${chapters.size} Syllabus Chapters",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color.White else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search $subject chapter...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0EA5E9),
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered, key = { it.name }) { item ->
                        val isSel = item.name == currentChapter
                        Surface(
                            onClick = {
                                onSelect(item.name)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) Color(0xFF0EA5E9).copy(alpha = 0.18f)
                                    else if (isDark) Color(0x0EFFFFFF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (isSel) Color(0xFF0EA5E9) else if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSel) Color(0xFF0EA5E9) else if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                item.classLevel,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xCCFFFFFF) else Color(0xFF475569),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        if (item.branch.isNotBlank()) {
                                            Surface(
                                                color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    item.branch,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF6366F1),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            item.pyqWeightage,
                                            fontSize = 9.5.sp,
                                            color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
                                        )
                                    }
                                }
                                if (isSel) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF0EA5E9),
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

@Composable
fun ChapterWiseCbtCard(
    exam: ExamCategory,
    selectedSubject: String,
    onSubjectChange: (String) -> Unit,
    selectedChapter: String,
    onChapterChange: (String) -> Unit,
    selectedCount: Int,
    onCountChange: (Int) -> Unit,
    isCustomCount: Boolean,
    onToggleCustomCount: (Boolean) -> Unit,
    customCountText: String,
    onCustomCountTextChange: (String) -> Unit,
    difficulty: String,
    onDifficultyChange: (String) -> Unit,
    websiteSource: String,
    onWebsiteSourceChange: (String) -> Unit,
    customCommand: String,
    onCustomCommandChange: (String) -> Unit,
    isGenerating: Boolean,
    onLaunch: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    var showChapterPicker by remember { mutableStateOf(false) }

    val subjects = remember(exam) {
        if (exam == ExamCategory.NEET) listOf("Physics", "Chemistry", "Biology")
        else listOf("Physics", "Chemistry", "Biology", "Mathematics")
    }

    val availableChapters = remember(exam, selectedSubject) {
        ExamSyllabusDatabase.getChaptersFor(exam, selectedSubject)
    }

    val currentChapterItem = remember(availableChapters, selectedChapter) {
        availableChapters.find { it.name.equals(selectedChapter, ignoreCase = true) }
    }

    val subjectThemeColor = when (selectedSubject.lowercase()) {
        "physics" -> Color(0xFF38BDF8)
        "chemistry" -> Color(0xFFF59E0B)
        "biology" -> Color(0xFF10B981)
        else -> Color(0xFFEC4899)
    }

    if (showChapterPicker) {
        ChapterPickerDialog(
            subject = selectedSubject,
            currentChapter = selectedChapter,
            chapters = availableChapters,
            onSelect = onChapterChange,
            onDismiss = { showChapterPicker = false },
            isDark = isDark
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF131A2E) else Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.3.dp,
                Brush.linearGradient(listOf(subjectThemeColor, Color(0xFF6366F1))),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(subjectThemeColor, Color(0xFF6366F1)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📖", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Chapter-Wise AI CBT Engine",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = subjectThemeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "CHAPTER DRILL",
                                color = subjectThemeColor,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        "Targeted 39-Year PYQ mastery, formula numericals & NCERT concepts",
                        fontSize = 11.5.sp,
                        color = subTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Subject Selector: Physics, Chemistry, Biology (or Mathematics)
            Text(
                "Select Subject:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjects.forEach { subj ->
                    val isSel = selectedSubject.equals(subj, ignoreCase = true)
                    val sColor = when (subj.lowercase()) {
                        "physics" -> Color(0xFF38BDF8)
                        "chemistry" -> Color(0xFFF59E0B)
                        "biology" -> Color(0xFF10B981)
                        else -> Color(0xFFEC4899)
                    }
                    Surface(
                        onClick = { onSubjectChange(subj) },
                        color = if (isSel) sColor else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSel) sColor else if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
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

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Chapter Selector Box (Tap to search/pick any chapter)
            Text(
                "Target Chapter (${availableChapters.size} in $selectedSubject):",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                onClick = { showChapterPicker = true },
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, subjectThemeColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(subjectThemeColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = subjectThemeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selectedChapter,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentChapterItem?.classLevel?.let { cls ->
                                Text(
                                    cls,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subjectThemeColor
                                )
                            }
                            currentChapterItem?.pyqWeightage?.let { w ->
                                Text(
                                    "• $w",
                                    fontSize = 10.sp,
                                    color = subTextColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = subjectThemeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Change",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = subjectThemeColor
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = subjectThemeColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Question Count Options: 25, 45, 90, 180, and Custom
            Text(
                "Select Question Count:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(6.dp))

            val countPresets = listOf(25 to "25 Q", 45 to "45 Q", 90 to "90 Q", 180 to "180 Q")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                countPresets.forEach { (count, label) ->
                    val isSel = !isCustomCount && selectedCount == count
                    Surface(
                        onClick = {
                            onCountChange(count)
                            onToggleCustomCount(false)
                        },
                        color = if (isSel) subjectThemeColor else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSel) subjectThemeColor else if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                                color = if (isSel) Color.White else textColor,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Custom Count Pill
                Surface(
                    onClick = { onToggleCustomCount(true) },
                    color = if (isCustomCount) subjectThemeColor else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isCustomCount) subjectThemeColor else if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Custom ✏️",
                            fontSize = 9.5.sp,
                            fontWeight = if (isCustomCount) FontWeight.Black else FontWeight.Bold,
                            color = if (isCustomCount) Color.White else textColor,
                            maxLines = 1
                        )
                    }
                }
            }

            // Animated Custom Question Count Input
            AnimatedVisibility(visible = isCustomCount) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = customCountText,
                        onValueChange = { newTxt ->
                            val digits = newTxt.filter { it.isDigit() }.take(3)
                            onCustomCountTextChange(digits)
                        },
                        label = { Text("Custom Question Count (5 - 200)", fontSize = 11.5.sp) },
                        placeholder = { Text("e.g. 15, 30, 50, 60, 100...", fontSize = 11.5.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = subjectThemeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (customCountText.isNotBlank()) {
                                IconButton(onClick = { onCustomCountTextChange("") }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = subTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = subjectThemeColor,
                            unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1),
                            focusedLabelColor = subjectThemeColor,
                            unfocusedLabelColor = subTextColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Dynamic Pattern & Marks Indicator Badge
            Spacer(modifier = Modifier.height(8.dp))
            val effectiveDisplayCount = if (isCustomCount) {
                customCountText.toIntOrNull()?.coerceIn(5, 200) ?: 25
            } else {
                selectedCount
            }
            val patternBadgeText = when {
                isCustomCount -> "✏️ Custom Drill: $effectiveDisplayCount Questions • ${effectiveDisplayCount * 4} Marks • $effectiveDisplayCount Mins Duration"
                selectedCount == 25 -> "⚡ Speed Test: 25 Questions • 100 Marks • 25 Mins (Fast Numerical & Concept Drill)"
                selectedCount == 45 -> "🎯 NTA Standard: 45 Questions • 180 Marks • 45 Mins (Exact Single Subject Section)"
                selectedCount == 90 -> "🔥 Deep Mastery: 90 Questions • 360 Marks • 90 Mins (Intensive Chapter Sprint)"
                selectedCount == 180 -> "🏆 Full Marathon: 180 Questions • 720 Marks • 180 Mins (Complete 39Y PYQ Sweep)"
                else -> "⚡ $selectedCount Questions • ${selectedCount * 4} Marks • $selectedCount Mins"
            }
            Surface(
                color = subjectThemeColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, subjectThemeColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        patternBadgeText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) subjectThemeColor.copy(alpha = 0.95f) else subjectThemeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Website Source / URL Input (with preset chips)
            OutlinedTextField(
                value = websiteSource,
                onValueChange = onWebsiteSourceChange,
                label = { Text("Website Source / URL (Optional)", fontSize = 11.5.sp) },
                placeholder = { Text("e.g. neetprep.com or examgoal.com link", fontSize = 11.5.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = subjectThemeColor,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (websiteSource.isNotBlank()) {
                        IconButton(onClick = { onWebsiteSourceChange("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = subTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = subjectThemeColor,
                    unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1),
                    focusedLabelColor = subjectThemeColor,
                    unfocusedLabelColor = subTextColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Preset website chips
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val sitePresets = listOf("neetprep.com", "examgoal.com", "testbook.com", "allen.in", "pw.live")
                sitePresets.forEach { site ->
                    val isSel = websiteSource.contains(site, ignoreCase = true)
                    Surface(
                        onClick = { onWebsiteSourceChange(if (isSel) "" else site) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) subjectThemeColor.copy(alpha = 0.18f) else if (isDark) Color(0x0EFFFFFF) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            1.dp,
                            if (isSel) subjectThemeColor else if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Text(
                            site,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) subjectThemeColor else subTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Custom Command / Focus Prompt Input (with shortcut directive chips)
            OutlinedTextField(
                value = customCommand,
                onValueChange = onCustomCommandChange,
                label = { Text("Custom Command / Focus Directive (Optional)", fontSize = 11.5.sp) },
                placeholder = { Text("e.g. Focus on assertion-reason, tricky numericals, formulas...", fontSize = 11.5.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.TipsAndUpdates,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (customCommand.isNotBlank()) {
                        IconButton(onClick = { onCustomCommandChange("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = subTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = subjectThemeColor,
                    unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1),
                    focusedLabelColor = subjectThemeColor,
                    unfocusedLabelColor = subTextColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Shortcut directive chips
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 2.dp)
            ) {
                val directivePresets = listOf(
                    "Tricky Numericals ⚡",
                    "NCERT Line-by-Line 🌿",
                    "Assertion-Reason 🎯",
                    "PYQ 2020-2026 Drill 🏆",
                    "Formula Application 📐",
                    "Match-the-Column 🧩",
                    "Diagram & Graph Focus 📊"
                )
                items(directivePresets) { directive ->
                    val isSel = customCommand.contains(directive.substringBefore(" "), ignoreCase = true)
                    Surface(
                        onClick = { onCustomCommandChange(if (isSel) "" else directive) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) Color(0xFFF59E0B).copy(alpha = 0.18f) else if (isDark) Color(0x0EFFFFFF) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            1.dp,
                            if (isSel) Color(0xFFF59E0B) else if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Text(
                            directive,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color(0xFFF59E0B) else subTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Launch Chapter CBT Button
            val effectiveCount = if (isCustomCount) {
                customCountText.toIntOrNull()?.coerceIn(5, 200) ?: 25
            } else {
                selectedCount
            }

            Button(
                onClick = onLaunch,
                enabled = !isGenerating,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = subjectThemeColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Generating Chapter CBT...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Launch $selectedChapter CBT (${effectiveCount} Q)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun FullLengthMockCard(
    exam: ExamCategory,
    selectedCount: Int,
    onCountSelected: (Int) -> Unit,
    includeMistakesAndSkipped: Boolean = false,
    onToggleIncludeMistakes: (Boolean) -> Unit = {},
    websiteSource: String = "",
    onWebsiteSourceChange: (String) -> Unit = {},
    unmasteredCount: Int = 0,
    onLaunch: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF131A2E) else Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎯", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Full Length Mock CBT", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = textColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = Color(0xFF6366F1).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                            Text("NTA PATTERN", color = Color(0xFF6366F1), fontSize = 8.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                    Text(
                        if (exam == ExamCategory.NEET) "Standard NEET Mock: Physics + Chemistry + Biology (Botany & Zoology)"
                        else "Standard JEE Mock: Physics + Chemistry + Mathematics",
                        fontSize = 11.5.sp,
                        color = subTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Select Question Set:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(6.dp))

            val options = if (exam == ExamCategory.NEET) {
                listOf(30 to "30 Q (Std)", 45 to "45 Q (1 Hr)", 90 to "90 Q (Half)", 180 to "180 Q (Full NEET)")
            } else {
                listOf(15 to "15 Q", 30 to "30 Q", 45 to "45 Q", 75 to "75 Q (Full JEE)", 90 to "90 Q")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { (count, label) ->
                    val isSel = selectedCount == count
                    Surface(
                        color = if (isSel) Color(0xFF6366F1) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFF6366F1) else if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)),
                        modifier = Modifier.weight(1f).clickable { onCountSelected(count) }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp), contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 9.5.sp, fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold, color = if (isSel) Color.White else textColor, maxLines = 1)
                        }
                    }
                }
            }

            if (exam == ExamCategory.NEET && selectedCount == 180) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF6366F1).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Exact NTA NEET Pattern: 90 Biology (Botany + Zoology) • 45 Physics • 45 Chemistry | 720 Marks • 200 Mins",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🧠 SMART ADAPTIVE REVISION TOGGLE (Mix past wrong & skipped questions)
            Surface(
                color = if (includeMistakesAndSkipped) Color(0xFF8B5CF6).copy(alpha = 0.15f) else if (isDark) Color(0x0EFFFFFF) else Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (includeMistakesAndSkipped) Color(0xFF8B5CF6).copy(alpha = 0.5f) else if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onToggleIncludeMistakes(!includeMistakesAndSkipped) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Mix Past Wrong & Skipped Questions", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                            if (unmasteredCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFE11D48).copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "$unmasteredCount pending",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFE11D48),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            "Pull unresolved errors from previous tests. Answer correctly once, and they never repeat!",
                            fontSize = 10.5.sp,
                            color = subTextColor,
                            lineHeight = 14.sp
                        )
                    }
                    Switch(
                        checked = includeMistakesAndSkipped,
                        onCheckedChange = { onToggleIncludeMistakes(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF8B5CF6)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 🌐 OPTIONAL WEBSITE SOURCE / URL FIELD
            OutlinedTextField(
                value = websiteSource,
                onValueChange = onWebsiteSourceChange,
                label = { Text("Website Source / URL (Optional)", fontSize = 11.5.sp) },
                placeholder = { Text("e.g. neetprep.com or practice link", fontSize = 11.5.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (websiteSource.isNotBlank()) {
                        IconButton(onClick = { onWebsiteSourceChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = subTextColor, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1),
                    focusedLabelColor = Color(0xFF6366F1),
                    unfocusedLabelColor = subTextColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onLaunch,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
            ) {
                Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                val extraLabel = if (includeMistakesAndSkipped) " (Adaptive Revision)" else ""
                Text("Launch ${exam.displayName} Full Mock Test ($selectedCount Q)$extraLabel", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun SmartOcrTestExtractorCard(
    exam: ExamCategory,
    selectedFileName: String?,
    selectedFileUri: Uri?,
    selectedAnswerKeyName: String? = null,
    selectedAnswerKeyUri: Uri? = null,
    onPickDoc: () -> Unit,
    onClearDoc: () -> Unit,
    onPickAnswerKeyDoc: () -> Unit = {},
    onClearAnswerKeyDoc: () -> Unit = {},
    onLaunchExtraction: (
        institute: String,
        startPage: Int,
        endPage: Int?,
        scanLanguage: String,
        autoCropDiagrams: Boolean,
        customDurationMinutes: Int?,
        answerKeyUri: Uri?,
        answerKeyText: String?
    ) -> Unit,
    isDark: Boolean
) {
    val context = LocalContext.current
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    // Popular Coaching Institutes
    val institutePresets = listOf(
        "Allen",
        "Aakash",
        "PhysicsWallah (PW)",
        "Motion",
        "Sri Chaitanya",
        "Narayana",
        "Resonance",
        "NTA Abhyas",
        "Self/General",
        "Custom"
    )

    var selectedInstitute by remember { mutableStateOf("Allen") }
    var customInstituteText by remember { mutableStateOf("") }
    var scanLanguage by remember { mutableStateOf("English") } // "English", "Hindi", "Both"
    var pageScope by remember { mutableStateOf("ALL") } // "ALL" or "CUSTOM"
    var startPageText by remember { mutableStateOf("1") }
    var endPageText by remember { mutableStateOf("") }
    var autoCropDiagrams by remember { mutableStateOf(true) }
    var durationMode by remember { mutableStateOf("AUTO") } // "AUTO", "30", "60", "180", "200", "CUSTOM"
    var customMinutesText by remember { mutableStateOf("60") }

    var showAnswerKeySection by remember { mutableStateOf(false) }
    var answerKeyInputMode by remember { mutableStateOf("FILE") } // "FILE" or "TEXT"
    var answerKeyText by remember { mutableStateOf("") }

    val isPdf = selectedFileName?.endsWith(".pdf", ignoreCase = true) == true
    val totalPdfPages = remember(selectedFileUri) {
        if (selectedFileUri != null && isPdf) {
            SmartOcrMultiPageEngine.getPdfTotalPages(context, selectedFileUri)
        } else 1
    }

    LaunchedEffect(totalPdfPages) {
        if (endPageText.isBlank() && totalPdfPages > 1) {
            endPageText = totalPdfPages.toString()
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF131A2E) else Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444))), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🪄", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Abhi Magic 🪄",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = textColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                        ) {
                            Text(
                                "30-40 PAGES SUPPORT",
                                color = Color(0xFFF59E0B),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Extracts MCQs + crops exact diagram screenshots from 30-40 page Allen / Aakash / PW / NTA Test Papers with Abhi Magic 🪄",
                        fontSize = 11.5.sp,
                        color = subTextColor,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. FILE PICKER BUTTON / PREVIEW
            if (selectedFileName == null) {
                Surface(
                    color = if (isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth().clickable { onPickDoc() }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tap to Select Test Paper PDF or Image (Abhi Magic 🪄)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                        Text("Supports 30-40 page major test papers, DPPs & multi-column PDF scans", fontSize = 11.sp, color = subTextColor)
                    }
                }
            } else {
                Surface(
                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(selectedFileName, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (isPdf && totalPdfPages > 1) {
                                    Text("Detected: $totalPdfPages Pages • Full Multi-Page Batch Engine Ready", fontSize = 10.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold)
                                } else {
                                    Text("Ready for AI Diagram Screenshot & MCQ Extraction", fontSize = 10.sp, color = Color(0xFFF59E0B))
                                }
                            }
                        }
                        IconButton(onClick = onClearDoc, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = textColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. INSTITUTE CATEGORY SELECTOR
                Text("Coaching Institute / Paper Source", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = textColor)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(institutePresets) { inst ->
                        val isSel = selectedInstitute == inst
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFFF59E0B) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFFF59E0B) else if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable { selectedInstitute = inst }
                        ) {
                            Text(
                                text = inst,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSel) Color.White else textColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (selectedInstitute == "Custom") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customInstituteText,
                        onValueChange = { customInstituteText = it },
                        placeholder = { Text("Enter institute name (e.g. Vibrant, Fiitjee)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Strict Zero-Skip Full Scan Mode (English Only) Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Strict Zero-Skip Full Scan Mode (English Only)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                            )
                            Text(
                                "Scans 100% of questions top-to-bottom in English. Preserves formulas, diagrams & all options without omissions.",
                                fontSize = 10.5.sp,
                                color = subTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. MULTI-PAGE RANGE SELECTION (If PDF has > 1 page)
                if (isPdf && totalPdfPages > 1) {
                    Text("Pages to Extract", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = textColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (pageScope == "ALL") Color(0xFF10B981).copy(alpha = 0.2f) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (pageScope == "ALL") Color(0xFF10B981) else if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)),
                            modifier = Modifier.weight(1f).clickable { pageScope = "ALL" }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Full Paper (All $totalPdfPages Pages)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (pageScope == "ALL") Color(0xFF10B981) else textColor)
                                Text("Complete test scan", fontSize = 9.5.sp, color = subTextColor)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (pageScope == "CUSTOM") Color(0xFF6366F1).copy(alpha = 0.2f) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (pageScope == "CUSTOM") Color(0xFF6366F1) else if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)),
                            modifier = Modifier.weight(1f).clickable { pageScope = "CUSTOM" }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Custom Page Range", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (pageScope == "CUSTOM") Color(0xFF6366F1) else textColor)
                                Text("e.g. Pages 1 to 15", fontSize = 9.5.sp, color = subTextColor)
                            }
                        }
                    }

                    if (pageScope == "CUSTOM") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = startPageText,
                                onValueChange = { startPageText = it },
                                label = { Text("From Page", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6366F1))
                            )
                            Text("to", fontSize = 12.sp, color = subTextColor, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = endPageText,
                                onValueChange = { endPageText = it },
                                label = { Text("To Page", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6366F1))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 4. AUTO-CROP DIAGRAMS SCREENSHOT SWITCH
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0x0DFFFFFF) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CropFree, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Auto-Crop Diagram Screenshots", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Text("Crops circuits, graphs & anatomy directly from pages", fontSize = 9.5.sp, color = subTextColor)
                            }
                        }
                        Switch(
                            checked = autoCropDiagrams,
                            onCheckedChange = { autoCropDiagrams = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFF59E0B)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4b. OFFICIAL ANSWER KEY UPLOADER & INPUT SECTION (ANTI-HALLUCINATION)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedAnswerKeyUri != null || answerKeyText.isNotBlank()) Color(0xFF10B981).copy(alpha = if (isDark) 0.15f else 0.08f) else if (isDark) Color(0x0DFFFFFF) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.2.dp, if (selectedAnswerKeyUri != null || answerKeyText.isNotBlank()) Color(0xFF10B981) else if (isDark) Color(0x1AFFFFFF) else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🔑", fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Official Answer Key", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                                        Surface(
                                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "100% ACCURACY",
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text("Upload PDF/Photo or paste key text so Gemini never gives wrong answers", fontSize = 9.5.sp, color = subTextColor)
                                }
                            }
                            Switch(
                                checked = showAnswerKeySection || selectedAnswerKeyUri != null || answerKeyText.isNotBlank(),
                                onCheckedChange = { showAnswerKeySection = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF10B981)
                                )
                            )
                        }

                        if (showAnswerKeySection || selectedAnswerKeyUri != null || answerKeyText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Toggle Tabs: Upload File vs Type / Paste Text
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (answerKeyInputMode == "FILE") Color(0xFF10B981) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                                    modifier = Modifier.weight(1f).clickable { answerKeyInputMode = "FILE" }
                                ) {
                                    Text(
                                        "📄 Upload Key File",
                                        fontSize = 11.sp,
                                        fontWeight = if (answerKeyInputMode == "FILE") FontWeight.Bold else FontWeight.Medium,
                                        color = if (answerKeyInputMode == "FILE") Color.White else textColor,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (answerKeyInputMode == "TEXT") Color(0xFF10B981) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                                    modifier = Modifier.weight(1f).clickable { answerKeyInputMode = "TEXT" }
                                ) {
                                    Text(
                                        "✍️ Paste Key Text",
                                        fontSize = 11.sp,
                                        fontWeight = if (answerKeyInputMode == "TEXT") FontWeight.Bold else FontWeight.Medium,
                                        color = if (answerKeyInputMode == "TEXT") Color.White else textColor,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (answerKeyInputMode == "FILE") {
                                if (selectedAnswerKeyUri == null) {
                                    OutlinedButton(
                                        onClick = onPickAnswerKeyDoc,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Select Answer Key PDF / Photo", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }
                                } else {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    selectedAnswerKeyName ?: "Answer_Key_Loaded.pdf",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(onClick = onClearAnswerKeyDoc, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = textColor, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = answerKeyText,
                                    onValueChange = { answerKeyText = it },
                                    label = { Text("Answer Key Text", fontSize = 11.sp) },
                                    placeholder = { Text("e.g. 1-A, 2-C, 3-D, 4-(2), 21-25.4, 22-120", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF10B981))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4c. CUSTOM TEST TIMER / DURATION SELECTOR
                Text("⏱️ Test Duration & Timer", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = textColor)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val durationOptions = listOf(
                        "AUTO" to "Auto (2m/Q)",
                        "30" to "30 Mins",
                        "60" to "1 Hour",
                        "180" to "3 Hours",
                        "200" to "200 Mins (NEET)",
                        "CUSTOM" to "Custom ⏱️"
                    )
                    items(durationOptions) { (mode, label) ->
                        val isSel = durationMode == mode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFFF59E0B) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFFF59E0B) else if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable { durationMode = mode }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSel) Color.White else textColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (durationMode == "CUSTOM") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = customMinutesText,
                            onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 4) customMinutesText = it },
                            label = { Text("Enter Custom Minutes", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFF59E0B))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Minutes", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. LAUNCH BUTTON
                val targetInstitute = if (selectedInstitute == "Custom") customInstituteText.ifBlank { "Custom" } else selectedInstitute
                val startPg = if (pageScope == "CUSTOM") (startPageText.toIntOrNull() ?: 1).coerceAtLeast(1) else 1
                val endPg = if (pageScope == "CUSTOM") (endPageText.toIntOrNull() ?: totalPdfPages).coerceAtMost(totalPdfPages) else totalPdfPages
                val resolvedCustomMinutes = when (durationMode) {
                    "30" -> 30
                    "60" -> 60
                    "180" -> 180
                    "200" -> 200
                    "CUSTOM" -> customMinutesText.toIntOrNull()?.coerceIn(5, 360) ?: 60
                    else -> null
                }

                Button(
                    onClick = {
                        onLaunchExtraction(
                            targetInstitute,
                            startPg,
                            endPg,
                            scanLanguage,
                            autoCropDiagrams,
                            resolvedCustomMinutes,
                            selectedAnswerKeyUri,
                            answerKeyText.ifBlank { null }
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    val langSuffix = if (scanLanguage == "Hindi") " (हिंदी Only)" else if (scanLanguage == "English") " (Eng Only)" else ""
                    val btnLabel = if (isPdf && totalPdfPages > 1) {
                        if (pageScope == "ALL") "Extract Full Paper ($totalPdfPages Pages)$langSuffix with Abhi Magic 🪄"
                        else "Extract Pages $startPg-$endPg ($targetInstitute)$langSuffix with Abhi Magic 🪄"
                    } else {
                        "Extract $targetInstitute Test$langSuffix with Abhi Magic 🪄"
                    }
                    Text(btnLabel, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun MistakesRevisionTestCard(
    exam: ExamCategory,
    selectedSubject: String,
    onSubjectChange: (String) -> Unit,
    unmasteredWrongCount: Int = 0,
    unmasteredSkippedCount: Int = 0,
    masteredCount: Int = 0,
    filterScope: String = "ALL",
    onFilterScopeChange: (String) -> Unit = {},
    websiteSource: String = "",
    onWebsiteSourceChange: (String) -> Unit = {},
    onLaunchRevision: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E1428) else Color(0xFFFFF1F2)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFF43F5E), Color(0xFF8B5CF6))), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFBE123C)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📕", fontSize = 22.sp)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Mistakes & Weak Areas Revision",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = textColor
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFFE11D48).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "1-TAP RETEST",
                                color = Color(0xFFE11D48),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = "Targeted CBT from past wrong & skipped questions",
                            fontSize = 11.5.sp,
                            color = subTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🏆 LIVE STATS BAR: Wrong, Skipped, and Mastered counts
            Surface(
                color = if (isDark) Color(0x1AFFFFFF) else Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFFECDD3)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🔴", fontSize = 11.sp)
                        Text("$unmasteredWrongCount Wrong", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                    }
                    Text("•", color = subTextColor, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("⚪", fontSize = 11.sp)
                        Text("$unmasteredSkippedCount Skipped", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                    Text("•", color = subTextColor, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🏆", fontSize = 11.sp)
                        Text("$masteredCount Mastered", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SCOPE SELECTION (ALL, WRONG ONLY, SKIPPED ONLY)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val scopes = listOf(
                    "ALL" to "All (${unmasteredWrongCount + unmasteredSkippedCount})",
                    "WRONG_ONLY" to "Wrong ($unmasteredWrongCount)",
                    "SKIPPED_ONLY" to "Skipped ($unmasteredSkippedCount)"
                )
                scopes.forEach { (sc, label) ->
                    val isSel = filterScope == sc
                    Surface(
                        color = if (isSel) Color(0xFFE11D48) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFFE11D48) else if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)),
                        modifier = Modifier.weight(1f).clickable { onFilterScopeChange(sc) }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 9.5.sp, fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold, color = if (isSel) Color.White else textColor, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Select Subject for Revision:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(6.dp))
            val subjects = listOf("All") + exam.subjects
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subjects, key = { it }) { subj ->
                    val isSel = selectedSubject == subj
                    FilterChip(
                        selected = isSel,
                        onClick = { onSubjectChange(subj) },
                        label = { Text(subj, fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE11D48),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 🌐 OPTIONAL WEBSITE SOURCE / URL
            OutlinedTextField(
                value = websiteSource,
                onValueChange = onWebsiteSourceChange,
                label = { Text("Web Source / URL (Optional, e.g. neetprep.com)", fontSize = 11.sp) },
                placeholder = { Text("URL to extract backup questions & diagrams", fontSize = 11.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (websiteSource.isNotBlank()) {
                        IconButton(onClick = { onWebsiteSourceChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = subTextColor, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE11D48),
                    unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1),
                    focusedLabelColor = Color(0xFFE11D48),
                    unfocusedLabelColor = subTextColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onLaunchRevision,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Mistakes Revision Test 🚀", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun CbtHybridQuestionBlock(
    text: String,
    isDark: Boolean,
    textColor: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val blockRegex = remember { Regex("""(?s)(\$\$.*?\$\$|\\\[.*?\\\])""") }
    val parts = remember(text) {
        val list = mutableListOf<Pair<String, Boolean>>()
        var lastIdx = 0
        blockRegex.findAll(text).forEach { match ->
            if (match.range.first > lastIdx) {
                val pre = text.substring(lastIdx, match.range.first).trim()
                if (pre.isNotEmpty()) list.add(pre to false)
            }
            val formula = match.value
                .removePrefix("$$").removeSuffix("$$")
                .removePrefix("\\[").removeSuffix("\\]")
                .trim()
            if (formula.isNotEmpty()) list.add(formula to true)
            lastIdx = match.range.last + 1
        }
        if (lastIdx < text.length) {
            val post = text.substring(lastIdx).trim()
            if (post.isNotEmpty()) list.add(post to false)
        }
        list
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        parts.forEach { (content, isFormula) ->
            if (isFormula) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    com.example.ui.components.MathJax3MathBlock(
                        latex = content,
                        isDark = isDark,
                        modifier = Modifier
                    )
                }
            } else {
                val formatted = remember(content) { com.example.util.MathFormatter.formatMathAndLatex(content) }
                Text(
                    text = formatted,
                    fontSize = fontSize,
                    color = textColor,
                    lineHeight = fontSize * 1.4f,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CbtQuestionStatementText(
    text: String,
    isDark: Boolean,
    textColor: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val clean = text.trim()
    if (clean.isBlank()) return

    // 1. Explicit display LaTeX block ($$...$$ or \[...\]) with complex formula -> Hybrid Block
    val hasDisplayBlock = remember(clean) {
        (clean.contains("$$") || clean.contains("\\[")) && com.example.util.MathFormatter.containsComplexFormula(clean)
    }

    if (hasDisplayBlock) {
        CbtHybridQuestionBlock(
            text = clean,
            isDark = isDark,
            textColor = textColor,
            fontSize = fontSize,
            modifier = modifier
        )
        return
    }

    // 2. English narrative / question sentence (Zero Lag Native Compose Text, crisp native typography)
    val isEnglishSentence = remember(clean) {
        com.example.util.MathFormatter.hasEnglishNarrative(clean)
    }

    if (isEnglishSentence || !com.example.util.MathFormatter.isComplexMathOrChemistry(clean)) {
        val formatted = remember(clean) { com.example.util.MathFormatter.formatMathAndLatex(clean) }
        Text(
            text = formatted,
            fontSize = fontSize,
            color = textColor,
            lineHeight = fontSize * 1.4f,
            fontWeight = FontWeight.Medium,
            modifier = modifier
        )
        return
    }

    // 3. Standalone pure complex formula without English narrative (e.g. \sqrt{...}, \vec{...})
    com.example.ui.components.MathJax3QuestionText(
        text = clean,
        isDark = isDark,
        textColor = textColor,
        fontSize = fontSize,
        modifier = modifier
    )
}

@Composable
private fun CbtOptionStatementText(
    text: String,
    isDark: Boolean,
    textColor: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val clean = text.trim()
    if (clean.isBlank()) return

    // Fast path: English text or non-complex expressions use Native Compose Text
    val isEnglish = remember(clean) { com.example.util.MathFormatter.hasEnglishNarrative(clean) }
    val isComplex = remember(clean) { com.example.util.MathFormatter.isComplexMathOrChemistry(clean) }

    if (!isEnglish && isComplex) {
        // High-precision 2D MathJax renderer ONLY for standalone complex square roots (\sqrt), vertical fractions (\frac), physics vectors (\vec) & chemistry (\ce)
        com.example.ui.components.MathJax3OptionText(
            text = clean,
            isDark = isDark,
            fontSize = fontSize,
            textColor = textColor,
            modifier = modifier
        )
    } else {
        // Ultra-fast Native Compose Text for normal values (e.g. "10 m", "5 m/s"), English statements, and conceptual choices
        val formatted = remember(clean) { com.example.util.MathFormatter.formatMathAndLatex(clean) }
        Text(
            text = formatted,
            fontSize = fontSize,
            color = textColor,
            lineHeight = fontSize * 1.35f,
            fontWeight = FontWeight.Normal,
            modifier = modifier
        )
    }
}

@Composable
fun QuestionDiagramCard(
    question: AiTestQuestion,
    isDark: Boolean,
    isSolutionMode: Boolean = false,
    autoInvertImages: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawUrl = question.imageUrl?.trim()
    if (rawUrl.isNullOrBlank()) return

    var loadAttempt by remember(rawUrl) { mutableIntStateOf(0) }
    var imageLoadSuccess by remember(rawUrl) { mutableStateOf(false) }

    val imgFile = remember(rawUrl) {
        if (rawUrl.startsWith("/") || rawUrl.startsWith("file:")) {
            val path = rawUrl.removePrefix("file://")
            java.io.File(path)
        } else null
    }

    val activeImageModel: Any = remember(rawUrl, imgFile, loadAttempt) {
        if (imgFile != null) {
            imgFile
        } else if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            val targetUrl = when (loadAttempt) {
                0 -> rawUrl
                1 -> if (rawUrl.contains("?")) rawUrl.substringBefore("?") else rawUrl
                else -> rawUrl
            }
            val domain = try { java.net.URI(targetUrl).host ?: "" } catch (_: Exception) { "" }
            ImageRequest.Builder(context)
                .data(targetUrl)
                .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .apply {
                    if (domain.isNotBlank()) {
                        setHeader("Referer", "https://$domain/")
                    }
                }
                .crossfade(true)
                .build()
        } else {
            rawUrl
        }
    }

    var manualInvertOverride by remember(question.id, autoInvertImages) {
        mutableStateOf<Boolean?>(null)
    }
    var showZoomDialog by remember { mutableStateOf(false) }
    val shouldInvert = manualInvertOverride ?: autoInvertImages

    // Full screen zoom dialog
    if (showZoomDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showZoomDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = activeImageModel,
                        contentDescription = "Zoomed Diagram",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        colorFilter = if (shouldInvert) ColorFilter.colorMatrix(CbtThemeManager.InvertColorMatrix) else null
                    )
                    IconButton(
                        onClick = { showZoomDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(16.dp)
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showZoomDialog = true },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = activeImageModel,
                    contentDescription = question.diagramLabel ?: "Question Diagram",
                    onSuccess = {
                        imageLoadSuccess = true
                    },
                    onError = {
                        if (loadAttempt == 0 && rawUrl.contains("?")) {
                            loadAttempt = 1
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 360.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    colorFilter = if (shouldInvert) ColorFilter.colorMatrix(CbtThemeManager.InvertColorMatrix) else null
                )

                // Quick Toggle for Anti-Glare Invert directly on diagram
                Surface(
                    onClick = { manualInvertOverride = !shouldInvert },
                    shape = RoundedCornerShape(8.dp),
                    color = if (shouldInvert) Color(0xFF6366F1).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            if (shouldInvert) "🌓 Invert (ON)" else "☀️ Normal",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Tap to zoom hint
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                ) {
                    Text(
                        "🔍 Tap to Zoom",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            val labelText = question.diagramLabel?.trim()
            if (!labelText.isNullOrBlank() && !labelText.startsWith("<svg", ignoreCase = true) && !labelText.startsWith("[", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = labelText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xCCFFFFFF) else Color(0xFF475569),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SavedAiTestItemCard(
    test: AiSavedTest,
    onReview: () -> Unit,
    onRetake: () -> Unit,
    onDelete: () -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val dateStr = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(test.timestamp))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF131A2E) else Color.White,
        border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth().clickable { onReview() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = Color(0xFF6366F1).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            test.examCategory,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF6366F1),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                        )
                    }
                    if (test.institute.isNotBlank() && test.institute != "Self/General") {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "🏛️ " + test.institute,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                    Text(dateStr, fontSize = 10.sp, color = subTextColor)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(test.title, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Score: ${test.score}/${test.maxScore} • Accuracy: ${String.format(Locale.getDefault(), "%.0f%%", test.accuracyPct)}", fontSize = 11.sp, color = subTextColor)
                if (test.difficultyDistribution.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("⚡ " + test.difficultyDistribution, fontSize = 10.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onRetake() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retake Test", tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Retake", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                    }
                }
                IconButton(onClick = onReview, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Review", tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444).copy(alpha = 0.7f), modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

// ----------------------------------------------------
// AI GENERATION RADAR LOADING VIEW
// ----------------------------------------------------

@Composable
fun AiGeneratingLoadingView(
    exam: ExamCategory,
    isDark: Boolean,
    errorText: String?,
    smartOcrProgress: SmartOcrMultiPageEngine.ExtractionProgress? = null,
    onStopAndStartEarly: (() -> Unit)? = null,
    onRetry: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aiGeneratingAnim")

    // Rotation for orbit halo
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloRotation"
    )

    // Reverse rotation for secondary outer ring
    val reverseHaloRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverseHaloRotation"
    )

    // Core badge breathing scale
    val badgeScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeScale"
    )

    // Glowing atmospheric pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Radar Ripple 1
    val ripple1Scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1Scale"
    )
    val ripple1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1Alpha"
    )

    // Radar Ripple 2
    val ripple2Scale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, delayMillis = 400, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2Scale"
    )
    val ripple2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, delayMillis = 400, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2Alpha"
    )

    // Sparkle Icon sway rotation & scale
    val iconRotate by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconRotate"
    )
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    // Simulated progress ticker
    val progressTick by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progressTick"
    )

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    // Dynamic phase text based on progress
    val statusPhase = when {
        progressTick < 0.35f -> "🔍 Sourcing authentic NTA syllabus patterns & weightage..."
        progressTick < 0.70f -> "🧠 Structuring step-by-step AI solutions & formulas..."
        else -> "🎯 Calibrating CBT countdown timer & question palette..."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0B1120) else Color(0xFFF8FAFC))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            // ================= ANIMATED LOGO CONTAINER =================
            Box(
                modifier = Modifier
                    .size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // 1. Radar Expanding Ripple 1
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(ripple1Scale)
                        .alpha(ripple1Alpha)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFF8B5CF6), CircleShape)
                )

                // 2. Radar Expanding Ripple 2
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(ripple2Scale)
                        .alpha(ripple2Alpha)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFFEC4899), CircleShape)
                )

                // 3. Ambient Glow Backlight
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .alpha(glowAlpha)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF8B5CF6).copy(alpha = 0.55f),
                                    Color(0xFFEC4899).copy(alpha = 0.30f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 4. Outer Rotating Orbit Halo (Canvas)
                Canvas(
                    modifier = Modifier
                        .size(105.dp)
                        .rotate(haloRotation)
                ) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFF6366F1),
                                Color(0xFFEC4899),
                                Color(0xFF06B6D4),
                                Color.Transparent,
                                Color(0xFF6366F1)
                            )
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 5. Reverse Counter-Rotating Dashed Orbit Ring
                Canvas(
                    modifier = Modifier
                        .size(122.dp)
                        .rotate(reverseHaloRotation)
                ) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.6f),
                                Color.Transparent,
                                Color(0xFFA855F7).copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        ),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(12f, 16f), 0f
                            )
                        )
                    )
                }

                // 6. Central Glowing Interactive Logo Badge
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(badgeScale)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            spotColor = Color(0xFF8B5CF6),
                            ambientColor = Color(0xFFEC4899)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF4F46E5),
                                    Color(0xFF8B5CF6),
                                    Color(0xFFEC4899)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.8f),
                                    Color(0xFF38BDF8),
                                    Color(0xFFF472B6),
                                    Color.White.copy(alpha = 0.8f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Inside Sparkle Icon with animated float, scale and rotation
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI Test Engine Generating",
                        tint = Color.White,
                        modifier = Modifier
                            .size(38.dp)
                            .rotate(iconRotate)
                            .scale(iconScale)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ================= HEADER TITLE =================
            val title = if (smartOcrProgress != null) {
                "Abhi Magic 🪄 Scanning Test Paper (${smartOcrProgress.currentPageIndex}/${smartOcrProgress.totalPages} Pages)..."
            } else {
                "LAKSHYA AI Generating Full Mock CBT..."
            }

            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Step Text
            val stepText = smartOcrProgress?.statusMessage ?: statusPhase
            Text(
                text = stepText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFA78BFA),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            if (smartOcrProgress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Text(
                            "📝 ${smartOcrProgress.questionsExtracted} Questions Found",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                    ) {
                        Text(
                            "📸 ${smartOcrProgress.diagramsExtracted} Diagrams Snapped",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sourcing authentic ${exam.displayName} 1988-2026 PYQ patterns with complete formulas & step-by-step solutions.",
                    fontSize = 12.sp,
                    color = subTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ================= PROGRESS BAR =================
            val pVal = if (smartOcrProgress != null && smartOcrProgress.totalBatches > 0) {
                smartOcrProgress.currentBatch.toFloat() / smartOcrProgress.totalBatches.toFloat()
            } else {
                progressTick
            }

            FireworksProgressBar(
                progress = pVal,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(7.dp),
                gradientColors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899)),
                sparkColor = Color(0xFFEC4899),
                trackColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
            )

            // Early Start Button if user has enough questions already
            if (smartOcrProgress != null && smartOcrProgress.questionsExtracted > 0 && onStopAndStartEarly != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onStopAndStartEarly,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Start CBT Now (${smartOcrProgress.questionsExtracted} Questions Ready)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }

            if (errorText != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorText,
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Go Back / Retry", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Utility to get file name from Uri
private fun getAiTestFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = it.getString(index)
                }
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

@Composable
fun PassageCard(
    passageText: String,
    isDark: Boolean,
    textColor: Color,
    fontScale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF0F7FF)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6).copy(alpha = 0.45f) else Color(0xFF93C5FD)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF3B82F6).copy(alpha = 0.15f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "READING PARAGRAPH / COMPREHENSION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        color = Color(0xFF3B82F6)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            val formattedPassage = remember(passageText) { com.example.util.MathFormatter.formatMathAndLatex(passageText) }
            Text(
                text = formattedPassage,
                fontSize = (13.5f * fontScale).sp,
                color = textColor,
                lineHeight = (13.5f * fontScale * 1.45f).sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
