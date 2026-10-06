@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GeminiChatAssistant
import com.example.data.GmcAiAnalysisResult
import com.example.data.GmcAiCollegeRecommendation
import com.example.data.GmcRepository
import com.example.data.MedicalCollege
import com.example.data.YearlyCutoffRecord
import com.example.ui.AppViewModel
import com.example.ui.components.AppBrandingFooter
import com.example.ui.components.MathJaxView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GmcCutoffScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val mockTests by viewModel.mockTests.collectAsStateWithLifecycle()
    
    val latestMock = remember(mockTests) {
        mockTests.maxByOrNull { it.timestamp } ?: mockTests.lastOrNull()
    }
    val latestMockScore = latestMock?.score
    val avgMockScore = remember(mockTests) {
        if (mockTests.isNotEmpty()) {
            (mockTests.map { it.score }.average()).toInt()
        } else {
            650
        }
    }
    val bestMockScore = remember(mockTests) {
        mockTests.maxOfOrNull { it.score }
    }

    var hasUserManuallyEditedScore by remember { mutableStateOf(false) }
    var userScoreInput by remember { mutableStateOf((latestMockScore ?: avgMockScore).toString()) }

    LaunchedEffect(mockTests) {
        if (!hasUserManuallyEditedScore && mockTests.isNotEmpty()) {
            val scoreToUse = latestMockScore ?: avgMockScore
            userScoreInput = scoreToUse.toString()
        }
    }
    var selectedCategory by remember { mutableStateOf("General (UR)") }
    var selectedQuota by remember { mutableStateOf("State Quota 85%") } // Default to State Quota for UP priority
    var selectedState by remember { mutableStateOf("Uttar Pradesh") } // Default to Uttar Pradesh priority
    var selectedYear by remember { mutableStateOf(2025) } // 2026 (Pending), 2025, 2024, 2023, 2022, 2021
    var searchQuery by remember { mutableStateOf("") }
    var selectedUpTier by remember { mutableStateOf("All UP") } // "All UP", "Top Apex", "Classic", "ASMC", "AIIMS"
    var sortBy by remember { mutableStateOf("Cutoff Marks") } // "Cutoff Marks", "NIRF Rank", "Seats"
    var selectedCollegeForDetails by remember { mutableStateOf<MedicalCollege?>(null) }
    var showUpCutoffInfoDialog by remember { mutableStateOf(false) }

    // LAKSHYA AI Live Sync State
    var isSyncingWithGemini by remember { mutableStateOf(false) }
    var geminiAnalysisResult by remember { mutableStateOf<GmcAiAnalysisResult?>(null) }
    var showGeminiAnalysisDialog by remember { mutableStateOf(false) }
    var syncErrorMessage by remember { mutableStateOf<String?>(null) }

    // Interactive LAKSHYA AI Cutoff Consultant Chat State
    var gmcChatMessages by remember {
        mutableStateOf(
            listOf(
                GmcChatMessage(
                    isUser = false,
                    text = "👋 Namaste! I am your **LAKSHYA AI Medical College & Cutoff Consultant**.\n\nAsk me anything about:\n- 🎯 What medical college you can get for your score & category\n- 🏛️ State Quota (85%) vs AIQ (15%) admission probability\n- 📊 5-Year Cutoff trends (2021–2025) & 2026 Expected cutoff inflation\n- ⚖️ Rural service bond duration, penalties, fee structures & internal PG quotas\n\nTap any quick prompt chip below or ask your own question!"
                )
            )
        )
    }
    var gmcChatInput by remember { mutableStateOf("") }
    var isGmcChatTyping by remember { mutableStateOf(false) }
    var isGmcChatExpanded by remember { mutableStateOf(true) }
    var showGmcFullChatDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val currentScoreInt = userScoreInput.toIntOrNull() ?: 0

    fun sendGmcQuestion(questionText: String) {
        val trimmed = questionText.trim()
        if (trimmed.isBlank() || isGmcChatTyping) return

        val userMsg = GmcChatMessage(isUser = true, text = trimmed)
        gmcChatMessages = gmcChatMessages + userMsg
        gmcChatInput = ""
        isGmcChatTyping = true

        coroutineScope.launch {
            val result = GeminiChatAssistant.askGmcCutoffQuestion(
                context = context,
                userQuestion = trimmed,
                userScore = currentScoreInt,
                category = selectedCategory,
                quota = selectedQuota,
                preferredState = selectedState,
                collegeContext = selectedCollegeForDetails?.name
            )
            isGmcChatTyping = false
            result.onSuccess { aiResponse ->
                gmcChatMessages = gmcChatMessages + GmcChatMessage(isUser = false, text = aiResponse)
            }.onFailure { error ->
                gmcChatMessages = gmcChatMessages + GmcChatMessage(
                    isUser = false,
                    text = "⚠️ **Unable to consult LAKSHYA AI**: ${error.localizedMessage ?: "Please check your internet connection or custom API key."}"
                )
            }
        }
    }

    fun performGeminiLiveSync() {
        if (isSyncingWithGemini) return
        isSyncingWithGemini = true
        syncErrorMessage = null
        coroutineScope.launch {
            val mockScores = mockTests.map { it.score }
            val result = GeminiChatAssistant.syncGmcCutoffsWithGeminiAi(
                context = context,
                userScore = currentScoreInt,
                category = selectedCategory,
                quota = selectedQuota,
                preferredState = selectedState,
                mockScoresHistory = mockScores
            )
            isSyncingWithGemini = false
            result.onSuccess { analysis ->
                geminiAnalysisResult = analysis
                showGeminiAnalysisDialog = true
                Toast.makeText(context, "⚡ LAKSHYA AI Counseling Sync Complete!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                val error = err.localizedMessage ?: "Failed to sync with LAKSHYA AI"
                syncErrorMessage = error
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    val statesList = remember {
        listOf("Uttar Pradesh", "All India") + GmcRepository.getAllStates().filter { it != "Uttar Pradesh" }
    }

    val categories = listOf("General (UR)", "OBC", "EWS", "SC", "ST")

    // Filter & Sort Logic with UP Tier Support
    val filteredColleges = remember(searchQuery, selectedState, selectedCategory, selectedQuota, sortBy, selectedUpTier) {
        GmcRepository.colleges.filter { college ->
            val matchesSearch = searchQuery.isBlank() || 
                college.name.contains(searchQuery, ignoreCase = true) || 
                college.shortName.contains(searchQuery, ignoreCase = true) || 
                college.city.contains(searchQuery, ignoreCase = true) ||
                college.state.contains(searchQuery, ignoreCase = true)

            val matchesState = selectedState == "All India" || college.state.equals(selectedState, ignoreCase = true)

            val matchesUpTier = if (selectedState == "Uttar Pradesh" && selectedUpTier != "All UP") {
                when (selectedUpTier) {
                    "Top Apex" -> college.id in listOf("ims_bhu_varanasi", "kgmu_lucknow", "dr_rml_lucknow", "jnmc_amu_aligarh", "gsvm_kanpur")
                    "Classic" -> college.id in listOf("mln_prayagraj", "llrm_meerut", "snmc_agra", "brd_gorakhpur", "mlbmc_jhansi", "upums_saifai", "gmc_saharanpur", "gmc_kannauj", "gmc_azamgarh", "gmc_jalaun", "gmc_banda", "gmc_badaun")
                    "ASMC" -> college.id.startsWith("gmc_") && college.shortName.startsWith("ASMC")
                    "AIIMS" -> college.id in listOf("aiims_gorakhpur", "aiims_raebareli")
                    else -> true
                }
            } else true

            matchesSearch && matchesState && matchesUpTier
        }.sortedWith { a, b ->
            when (sortBy) {
                "NIRF Rank" -> a.nirfRank.compareTo(b.nirfRank)
                "Seats" -> b.totalSeats.compareTo(a.totalSeats)
                else -> {
                    val scoreA = getCutoffMarks(a, selectedCategory, selectedQuota)
                    val scoreB = getCutoffMarks(b, selectedCategory, selectedQuota)
                    scoreB.compareTo(scoreA)
                }
            }
        }
    }

    // Predictor Breakdown Counts
    val safeCount = filteredColleges.count { college ->
        val cutoff = getCutoffMarks(college, selectedCategory, selectedQuota)
        currentScoreInt >= cutoff + 5
    }
    val targetCount = filteredColleges.count { college ->
        val cutoff = getCutoffMarks(college, selectedCategory, selectedQuota)
        currentScoreInt in (cutoff - 15)..(cutoff + 4)
    }
    val reachCount = filteredColleges.count { college ->
        val cutoff = getCutoffMarks(college, selectedCategory, selectedQuota)
        currentScoreInt < cutoff - 15
    }

    val baseBgColor = Color.Transparent
    val cardBgColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.55f) else Color(0xFFFFFFFF).copy(alpha = 0.75f)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    BackHandler {
        if (showGeminiAnalysisDialog) {
            showGeminiAnalysisDialog = false
        } else if (showGmcFullChatDialog) {
            showGmcFullChatDialog = false
        } else if (selectedCollegeForDetails != null) {
            selectedCollegeForDetails = null
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.padding(end = 4.dp)) {
                        Text(
                            "🏛️ GMC Cutoffs & Predictor",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = textColor
                        )
                        Text(
                            "NEET AIQ & State Quota Benchmarks",
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = subTextColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showGeminiAnalysisDialog) {
                            showGeminiAnalysisDialog = false
                        } else if (showGmcFullChatDialog) {
                            showGmcFullChatDialog = false
                        } else if (selectedCollegeForDetails != null) {
                            selectedCollegeForDetails = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
                    // Ask LAKSHYA AI Cutoff Consultant Icon Button
                    IconButton(
                        onClick = { showGmcFullChatDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("💬", fontSize = 14.sp)
                            }
                        }
                    }

                    // Live LAKSHYA AI Sync Action Button
                    IconButton(
                        onClick = { performGeminiLiveSync() },
                        enabled = !isSyncingWithGemini,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSyncingWithGemini) Color(0xFF6366F1).copy(alpha = 0.25f) else if (geminiAnalysisResult != null) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF6366F1).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (geminiAnalysisResult != null) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF6366F1).copy(alpha = 0.5f)),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isSyncingWithGemini) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF818CF8)
                                    )
                                } else {
                                    Text(if (geminiAnalysisResult != null) "✨" else "⚡", fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { 
                            val score = latestMockScore ?: avgMockScore
                            userScoreInput = score.toString()
                            hasUserManuallyEditedScore = false
                            val msg = if (latestMockScore != null) "🎯 Fetched current mock test score ($latestMockScore) from ${latestMock?.testName ?: "latest test"}" else "No mock tests recorded yet (default 650)"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Fetch Current Mock Score", tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Hero Scorecard Predictor Input Box
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                if (isDark) listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
                                else listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF818CF8))),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4F46E5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎯", fontSize = 18.sp)
                                }
                                Column {
                                    Text(
                                        "NEET Score Predictor",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = if (isDark) Color.White else Color(0xFF1E1B4B)
                                    )
                                    Text(
                                        if (latestMockScore != null) "Auto-synced with your mock tests" else "Live admission probability & rank estimate",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA)
                                    )
                                }
                            }

                            if (latestMockScore != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "LIVE FETCH",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Mock Test Score Selector Chips
                        if (mockTests.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (latestMockScore != null) {
                                    val isSelected = userScoreInput == latestMockScore.toString()
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF4F46E5).copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF818CF8) else Color(0xFF4F46E5).copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .clickable {
                                                userScoreInput = latestMockScore.toString()
                                                hasUserManuallyEditedScore = false
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                "🎯 Current: ${latestMockScore}",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else (if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA)),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                val isAvgSelected = userScoreInput == avgMockScore.toString()
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isAvgSelected) Color(0xFF4F46E5) else Color(0xFF4F46E5).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (isAvgSelected) Color(0xFF818CF8) else Color(0xFF4F46E5).copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .clickable {
                                            userScoreInput = avgMockScore.toString()
                                            hasUserManuallyEditedScore = false
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "📊 Avg: $avgMockScore",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAvgSelected) Color.White else (if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA)),
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (bestMockScore != null) {
                                    val isBestSelected = userScoreInput == bestMockScore.toString()
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isBestSelected) Color(0xFF4F46E5) else Color(0xFF4F46E5).copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, if (isBestSelected) Color(0xFF818CF8) else Color(0xFF4F46E5).copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .clickable {
                                                userScoreInput = bestMockScore.toString()
                                                hasUserManuallyEditedScore = false
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                "🏆 Best: $bestMockScore",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isBestSelected) Color.White else (if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA)),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = userScoreInput,
                                onValueChange = { 
                                    if (it.length <= 3) {
                                        userScoreInput = it
                                        hasUserManuallyEditedScore = true
                                    }
                                },
                                label = { Text("Your NEET / Mock Score (out of 720)") },
                                placeholder = { Text("e.g. 680") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                leadingIcon = {
                                    Text("📊", fontSize = 16.sp, modifier = Modifier.padding(start = 6.dp))
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1),
                                    focusedContainerColor = if (isDark) Color(0x22000000) else Color.White,
                                    unfocusedContainerColor = if (isDark) Color(0x22000000) else Color.White
                                )
                            )

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    val score = userScoreInput.toIntOrNull()
                                    if (score == null) {
                                        Toast.makeText(context, "Please enter a valid NEET score (0-720)", Toast.LENGTH_SHORT).show()
                                    } else if (score > 720) {
                                        Toast.makeText(context, "NEET max score is 720. Please check your score.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "🎯 Score $score Checked! Safe: $safeCount | Target: $targetCount | Reach: $reachCount", Toast.LENGTH_SHORT).show()
                                        performGeminiLiveSync()
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                modifier = Modifier.heightIn(min = 52.dp)
                            ) {
                                Text("Check", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats pills: Safe, Target, Reach
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScoreStatusSummaryPill(
                                title = "Safe Zone",
                                count = safeCount,
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            ScoreStatusSummaryPill(
                                title = "Target Zone",
                                count = targetCount,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                            ScoreStatusSummaryPill(
                                title = "Dream / Reach",
                                count = reachCount,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dedicated LAKSHYA AI Live Counseling & Cutoff Sync Action Strip
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.8f) else Color.White,
                            border = BorderStroke(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899))
                                )
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("✨", fontSize = 14.sp)
                                        }
                                        Column {
                                            Text(
                                                "LAKSHYA AI Live Sync",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = textColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                if (geminiAnalysisResult != null) "MCC Live Trends Synced" else "Live counseling & rank insights",
                                                fontSize = 10.sp,
                                                color = subTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Button(
                                        onClick = { performGeminiLiveSync() },
                                        enabled = !isSyncingWithGemini,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                    ) {
                                        if (isSyncingWithGemini) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Syncing...", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text(
                                                if (geminiAnalysisResult != null) "Re-Sync 🔄" else "Sync ✨",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (geminiAnalysisResult != null) {
                                    val result = geminiAnalysisResult!!
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "Predicted Rank Range",
                                                fontSize = 10.5.sp,
                                                color = subTextColor
                                            )
                                            Text(
                                                result.expectedAirRankRange,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF818CF8)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                            modifier = Modifier.clickable { showGeminiAnalysisDialog = true }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    "View AI Report",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF818CF8)
                                                )
                                                Icon(
                                                    Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = Color(0xFF818CF8),
                                                    modifier = Modifier.size(14.dp)
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

            // Interactive LAKSHYA AI Cutoff Consultant Card & Q&A
            item {
                GmcGeminiChatCard(
                    messages = gmcChatMessages,
                    inputText = gmcChatInput,
                    onInputChange = { gmcChatInput = it },
                    isTyping = isGmcChatTyping,
                    isExpanded = isGmcChatExpanded,
                    onToggleExpand = { isGmcChatExpanded = !isGmcChatExpanded },
                    onSendMessage = { sendGmcQuestion(it) },
                    onOpenFullScreen = { showGmcFullChatDialog = true },
                    onClearChat = {
                        gmcChatMessages = listOf(
                            GmcChatMessage(
                                isUser = false,
                                text = "👋 Namaste! I am your **LAKSHYA AI Medical College & Cutoff Consultant**.\n\nAsk me anything about:\n- 🎯 What medical college you can get for your score & category\n- 🏛️ State Quota (85%) vs AIQ (15%) admission probability\n- 📊 5-Year Cutoff trends (2021–2025) & 2026 Expected cutoff inflation\n- ⚖️ Rural service bond duration, penalties, fee structures & internal PG quotas\n\nTap any quick prompt chip below or ask your own question!"
                            )
                        )
                    },
                    userScore = currentScoreInt,
                    category = selectedCategory,
                    state = selectedState,
                    quota = selectedQuota,
                    isDark = isDark
                )
            }

            // UP Special Domicile Priority Hub Banner (When UP is active or selected)
            if (selectedState == "Uttar Pradesh") {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.7f) else Color(0xFFEFF6FF),
                        border = BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFF8B5CF6))
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF6366F1)))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("⭐", fontSize = 16.sp)
                                    }
                                    Column {
                                        Text(
                                            "UP Domicile (85% DGME) Priority Hub",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.5.sp,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "44 GMCs • 4,500+ Govt MBBS Seats • 2-Yr Rural Bond",
                                            fontSize = 10.5.sp,
                                            color = subTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                                    modifier = Modifier.clickable { showUpCutoffInfoDialog = true }
                                ) {
                                    Text(
                                        "Cutoff Dossier 📊",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3B82F6),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // UP State Quota Last Round Closing Benchmarks (Stray Vacancy)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0x22000000) else Color.White,
                                border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFDBEAFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        "📌 UP State Quota 85% Last Round Closing Cutoff:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("UR / Gen", fontSize = 10.sp, color = subTextColor)
                                            Text("605 M", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                                            Text("AIR ~24.2k", fontSize = 9.sp, color = subTextColor)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("OBC", fontSize = 10.sp, color = subTextColor)
                                            Text("603 M", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF3B82F6))
                                            Text("AIR ~25.6k", fontSize = 9.sp, color = subTextColor)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("EWS", fontSize = 10.sp, color = subTextColor)
                                            Text("602 M", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B5CF6))
                                            Text("AIR ~26.1k", fontSize = 9.sp, color = subTextColor)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("SC", fontSize = 10.sp, color = subTextColor)
                                            Text("476 M", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                                            Text("AIR ~1.38L", fontSize = 9.sp, color = subTextColor)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("ST", fontSize = 10.sp, color = subTextColor)
                                            Text("328 M", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                                            Text("AIR ~3.40L", fontSize = 9.sp, color = subTextColor)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // UP Tier Filter Chips
                            Text(
                                "Filter UP Colleges by Tier:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = subTextColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val upTiers = listOf(
                                    "All UP" to "All (44)",
                                    "Top Apex" to "👑 Apex (5)",
                                    "Classic" to "🏛️ Classic (12)",
                                    "ASMC" to "🏥 ASMCs (25+)",
                                    "AIIMS" to "⚡ AIIMS (2)"
                                )
                                items(upTiers, key = { it.first }) { (tierKey, tierLabel) ->
                                    val isTierSelected = selectedUpTier == tierKey
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isTierSelected) Color(0xFF3B82F6) else if (isDark) Color(0x22FFFFFF) else Color.White,
                                        border = BorderStroke(1.dp, if (isTierSelected) Color(0xFF2563EB) else if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)),
                                        modifier = Modifier.clickable { selectedUpTier = tierKey }
                                    ) {
                                        Text(
                                            text = tierLabel,
                                            fontSize = 11.sp,
                                            fontWeight = if (isTierSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isTierSelected) Color.White else textColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar & Filter Options
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search GMC name, city or district...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = subTextColor) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = subTextColor)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1),
                            focusedContainerColor = cardBgColor,
                            unfocusedContainerColor = cardBgColor
                        )
                    )

                    // Quota Selector (State Quota 85% vs AIQ 15%)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("State Quota 85%", "AIQ 15%").forEach { quota ->
                            val isSelected = selectedQuota == quota
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF4F46E5) else cardBgColor,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF6366F1) else if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedQuota = quota }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (quota == "State Quota 85%") "⭐ UP State Quota (85%)" else "AIQ (15%) All India",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = if (isSelected) Color.White else textColor
                                    )
                                }
                            }
                        }
                    }

                    // 5-Year Cutoff Selection Bar (2021-2025 + 2026 Pending)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📅 Select Counselling Year:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
                            )
                            if (selectedYear == 2026) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        "⏳ 2026 Pending Official MCC",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            val maxExamYear = maxOf(2026, com.example.data.NeetTenYearTrendDatabase.currentExamYear)
                            val yearsList = (maxExamYear downTo 2021).map { yr ->
                                val label = when {
                                    yr >= 2026 -> "⏳ $yr (Live/Pending)"
                                    yr == 2025 -> "2025 (Latest)"
                                    else -> "$yr"
                                }
                                yr to label
                            }
                            items(yearsList, key = { it.first }) { (yr, label) ->
                                val isSelected = selectedYear == yr
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) {
                                        if (yr == 2026) Color(0xFFEF4444) else Color(0xFF6366F1)
                                    } else {
                                        cardBgColor
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color.Transparent else if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.clickable { selectedYear = yr }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = if (isSelected) Color.White else textColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Category Filter Tabs
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(categories, key = { it }) { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF6366F1),
                                    selectedLabelColor = Color.White,
                                    containerColor = cardBgColor,
                                    labelColor = textColor
                                )
                            )
                        }
                    }

                    // State Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(statesList, key = { it }) { state ->
                            val isSelected = selectedState == state
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) (if (state == "Uttar Pradesh") Color(0xFF3B82F6) else Color(0xFF10B981)).copy(alpha = 0.2f) else cardBgColor,
                                border = BorderStroke(1.dp, if (isSelected) (if (state == "Uttar Pradesh") Color(0xFF3B82F6) else Color(0xFF10B981)) else if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)),
                                modifier = Modifier.clickable { 
                                    selectedState = state 
                                    if (state == "Uttar Pradesh") {
                                        selectedQuota = "State Quota 85%"
                                    }
                                }
                            ) {
                                Text(
                                    text = if (state == "Uttar Pradesh") "⭐ Uttar Pradesh" else state,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) (if (state == "Uttar Pradesh") Color(0xFF3B82F6) else Color(0xFF10B981)) else subTextColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Results count & Sort Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredColleges.size} Colleges ($selectedYear)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Sort:", fontSize = 11.sp, color = subTextColor, maxLines = 1, softWrap = false)
                            listOf("Cutoff" to "Cutoff Marks", "NIRF" to "NIRF Rank", "Seats" to "Seats").forEach { (shortLabel, fullOption) ->
                                val isSelected = sortBy == fullOption
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.15f) else Color.Transparent,
                                    border = if (isSelected) BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f)) else null,
                                    modifier = Modifier.clickable { sortBy = fullOption }
                                ) {
                                    Text(
                                        text = shortLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF6366F1) else subTextColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // List of Medical Colleges
            if (filteredColleges.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No GMCs found matching your search", fontWeight = FontWeight.Bold, color = textColor)
                            Text("Try clearing filters or search query", fontSize = 12.sp, color = subTextColor)
                        }
                    }
                }
            } else {
                items(filteredColleges, key = { it.id }) { college ->
                    MedicalCollegeCard(
                        college = college,
                        category = selectedCategory,
                        quota = selectedQuota,
                        userScore = currentScoreInt,
                        year = selectedYear,
                        isDark = isDark,
                        onClick = { selectedCollegeForDetails = college }
                    )
                }
            }

            // App Branding & Copyright Warning Footer at bottom of list
            item {
                AppBrandingFooter(isDark = isDark)
            }
        }
    }

    // Detail Dialog Sheet
    if (selectedCollegeForDetails != null) {
        MedicalCollegeDetailDialog(
            college = selectedCollegeForDetails!!,
            category = selectedCategory,
            quota = selectedQuota,
            userScore = currentScoreInt,
            selectedYear = selectedYear,
            isDark = isDark,
            onDismiss = { selectedCollegeForDetails = null },
            onLiveSyncGemini = { performGeminiLiveSync() },
            onAskGemini = { question ->
                sendGmcQuestion(question)
                showGmcFullChatDialog = true
            }
        )
    }

    // Interactive Full-Screen LAKSHYA AI Cutoff Consultant Chat Dialog
    if (showGmcFullChatDialog) {
        GmcCutoffChatDialog(
            messages = gmcChatMessages,
            inputText = gmcChatInput,
            onInputChange = { gmcChatInput = it },
            isTyping = isGmcChatTyping,
            onSendMessage = { sendGmcQuestion(it) },
            onClearChat = {
                gmcChatMessages = listOf(
                    GmcChatMessage(
                        isUser = false,
                        text = "👋 Namaste! I am your **LAKSHYA AI Medical College & Cutoff Consultant**.\n\nAsk me anything about:\n- 🎯 What medical college you can get for your score & category\n- 🏛️ State Quota (85%) vs AIQ (15%) admission probability\n- 📊 5-Year Cutoff trends (2021–2025) & 2026 Expected cutoff inflation\n- ⚖️ Rural service bond duration, penalties, fee structures & internal PG quotas\n\nTap any quick prompt chip below or ask your own question!"
                    )
                )
            },
            userScore = currentScoreInt,
            category = selectedCategory,
            quota = selectedQuota,
            state = selectedState,
            isDark = isDark,
            onDismiss = { showGmcFullChatDialog = false }
        )
    }

    // UP Domicile & Last Cutoff Comprehensive Dossier Dialog
    if (showUpCutoffInfoDialog) {
        UpCutoffDossierDialog(
            userScore = currentScoreInt,
            category = selectedCategory,
            isDark = isDark,
            onDismiss = { showUpCutoffInfoDialog = false },
            onSelectTier = { tier ->
                selectedUpTier = tier
                showUpCutoffInfoDialog = false
            }
        )
    }

    // LAKSHYA AI Live Counseling & Cutoff Intelligence Dialog
    if (showGeminiAnalysisDialog && geminiAnalysisResult != null) {
        GeminiAiCounselingDialog(
            result = geminiAnalysisResult!!,
            userScore = currentScoreInt,
            category = selectedCategory,
            quota = selectedQuota,
            state = selectedState,
            isDark = isDark,
            onDismiss = { showGeminiAnalysisDialog = false },
            onSelectCollege = { collegeName ->
                searchQuery = collegeName
                showGeminiAnalysisDialog = false
            }
        )
    }
}

@Composable
fun ScoreStatusSummaryPill(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = color,
                maxLines = 1
            )
            Text(
                text = title,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MedicalCollegeCard(
    college: MedicalCollege,
    category: String,
    quota: String,
    userScore: Int,
    year: Int = 2025,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val cutoffMarks = getCutoffMarks(college, category, quota, year)
    val closingRank = getClosingRank(college, category, year)
    val isUpCollege = college.state.equals("Uttar Pradesh", ignoreCase = true)

    val (statusLabel, statusColor, statusEmoji) = when {
        userScore >= cutoffMarks + 5 -> Triple("Safe Zone", Color(0xFF10B981), "🟢")
        userScore in (cutoffMarks - 15)..(cutoffMarks + 4) -> Triple("Target Zone", Color(0xFFF59E0B), "🟡")
        else -> {
            val diff = if (userScore > 0) maxOf(1, cutoffMarks - userScore) else cutoffMarks
            Triple("Reach (+$diff M)", Color(0xFFEF4444), "🔴")
        }
    }

    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val cardBorder = if (isUpCollege) {
        if (isDark) Color(0xFF3B82F6).copy(alpha = 0.4f) else Color(0xFFBFDBFE)
    } else {
        if (isDark) Color(0x26FFFFFF) else Color(0xFFE2E8F0)
    }
    val titleColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(if (isUpCollege) 1.5.dp else 1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = college.shortName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isUpCollege) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF3B82F6).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "UP 85%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF3B82F6),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (year == 2026) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF6366F1).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (year == 2026) "2026 Pend." else "$year",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (year == 2026) Color(0xFFEF4444) else Color(0xFF6366F1),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Text(
                        text = "📍 ${college.city}, ${college.state} • NIRF #${college.nirfRank}",
                        fontSize = 11.5.sp,
                        color = subColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Probability Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "$statusEmoji $statusLabel",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cutoff stats strip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.1f)) {
                        Text(
                            text = if (quota == "State Quota 85%") "State 85% ($year)" else "AIQ 15% ($year)",
                            fontSize = 10.sp,
                            color = subColor,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$cutoffMarks",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isUpCollege) Color(0xFF3B82F6) else Color(0xFF6366F1)
                            )
                            Text(" / 720", fontSize = 11.sp, color = subColor, modifier = Modifier.padding(bottom = 1.dp))
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Text("Est. Closing Rank", fontSize = 10.sp, color = subColor, maxLines = 1)
                        Text(
                            text = "AIR #$closingRank",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            maxLines = 1
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annual Fees", fontSize = 10.sp, color = subColor, maxLines = 1)
                        Text(
                            text = college.annualFee,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🛏️ ${college.hospitalBeds} • 👨‍⚕️ ${college.totalSeats} Seats • ${college.bondYears}",
                    fontSize = 11.sp,
                    color = subColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Text("5-Yr History", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1), maxLines = 1, softWrap = false)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun MedicalCollegeDetailDialog(
    college: MedicalCollege,
    category: String,
    quota: String,
    userScore: Int,
    selectedYear: Int = 2025,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onLiveSyncGemini: (() -> Unit)? = null,
    onAskGemini: ((String) -> Unit)? = null
) {
    val cutoffMarks = getCutoffMarks(college, category, quota, selectedYear)
    val closingRank = getClosingRank(college, category, selectedYear)
    val isUp = college.state.equals("Uttar Pradesh", ignoreCase = true)

    val userScoreDisplay = if (userScore > 0) "$userScore M" else "Baseline (0 M)"
    val (statusLabel, statusColor) = when {
        userScore >= cutoffMarks + 5 -> "High Probability (Safe Zone)" to Color(0xFF10B981)
        userScore in (cutoffMarks - 15)..(cutoffMarks + 4) -> "Moderate / Target (Contestable)" to Color(0xFFF59E0B)
        else -> {
            val needed = if (userScore > 0) maxOf(1, cutoffMarks - userScore) else cutoffMarks
            "Need +$needed Marks to Clear Cutoff" to Color(0xFFEF4444)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(26.dp))
                .background(if (isDark) Color(0xFF0F172A) else Color.White)
                .border(1.5.dp, if (isDark) Color(0x336366F1) else Color(0xFFCBD5E1), RoundedCornerShape(26.dp))
                .padding(18.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = college.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "📍 ${college.city}, ${college.state} • Estd. ${college.established}",
                                fontSize = 11.5.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                }

                // 2026 Pending Entry Banner & Live Sync
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF6366F1).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("⏳", fontSize = 14.sp)
                                    Text(
                                        "NEET 2026 Official Cutoff: Pending MCC",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    "Predicted range: ${college.aiqGeneralMarks - 3} - ${college.aiqGeneralMarks + 4} Marks.",
                                    fontSize = 10.5.sp,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
                                )
                            }

                            if (onLiveSyncGemini != null) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onLiveSyncGemini()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("AI Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // AI Probability Alert
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🎯", fontSize = 20.sp)
                            Column {
                                Text("Chance for your score ($userScoreDisplay in $selectedYear)", fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.Bold)
                                Text(statusLabel, fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = statusColor)
                            }
                        }
                    }
                }

                // 5-Year Cutoff History Table (2021 to 2025 + 2026 Pending)
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0x15FFFFFF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "📈 5-Year NEET Cutoff History ($category)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
                                )
                                Text("2021 - 2025", fontSize = 10.sp, color = if (isDark) Color.LightGray else Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDark) Color(0x226366F1) else Color(0x186366F1), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Year", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A), modifier = Modifier.weight(1f))
                                Text("Cutoff Marks", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A), modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                                Text("Closing AIR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A), modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // 2026 Pending Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("2026 (Pending)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444), modifier = Modifier.weight(1.3f))
                                Text("Est. ~${cutoffMarks}M", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("Live AI Sync", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                            }

                            HorizontalDivider(color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0))

                            // Yearly Records (2025 down to 2021)
                            val historyYears = listOf(2025, 2024, 2023, 2022, 2021)
                            historyYears.forEach { yr ->
                                val yrMarks = college.getYearlyMarks(yr, category, quota)
                                val yrRank = college.getYearlyRank(yr, category)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$yr",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (yr == selectedYear) FontWeight.Black else FontWeight.Medium,
                                        color = if (yr == selectedYear) Color(0xFF6366F1) else if (isDark) Color.White else Color(0xFF0F172A),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "$yrMarks / 720",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF10B981) else Color(0xFF059669),
                                        modifier = Modifier.weight(1.2f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "AIR #$yrRank",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.weight(1.2f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                if (yr != 2021) {
                                    HorizontalDivider(color = if (isDark) Color(0x11FFFFFF) else Color(0xFFF1F5F9))
                                }
                            }
                        }
                    }
                }

                // Round-wise Cutoff Trajectory
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "📊 Counselling Round-Wise Benchmarks ($category - $selectedYear)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6366F1)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Round 1", fontSize = 10.5.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                    Text("${cutoffMarks + 12} M", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Round 2", fontSize = 10.5.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                    Text("${cutoffMarks + 4} M", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Mop-Up", fontSize = 10.5.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                    Text("$cutoffMarks M", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Last Stray", fontSize = 10.5.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                    Text("${cutoffMarks - 3} M", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                            }
                        }
                    }
                }

                // Breakdown Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow(label = "Category & Quota", value = "$category ($quota)", isDark = isDark)
                        DetailRow(label = "NEET $selectedYear Cutoff", value = "$cutoffMarks / 720 (AIR ~$closingRank)", isDark = isDark, valueColor = Color(0xFF6366F1))
                        DetailRow(label = "MBBS Intake Capacity", value = "${college.totalSeats} Seats", isDark = isDark)
                        DetailRow(label = "Annual Tuition Fee", value = college.annualFee, isDark = isDark, valueColor = Color(0xFF10B981))
                        DetailRow(label = "Hospital & Bed Count", value = college.hospitalBeds, isDark = isDark)
                        DetailRow(label = "Internship Monthly Stipend", value = college.internStipend, isDark = isDark)
                        DetailRow(label = "Compulsory Rural Service Bond", value = "${college.bondYears} (Penalty: ${college.bondPenalty})", isDark = isDark)
                        DetailRow(label = "NIRF Medical Ranking", value = "#${college.nirfRank} in India", isDark = isDark)
                    }
                }

                // UP Special Domicile Guidelines (if in UP)
                if (isUp) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF3B82F6).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("⭐ UP DGME Counselling Notes", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF3B82F6))
                                Text("• Domicile rule: 10th & 12th from UP = No domicile certificate needed. If done outside UP, UP resident father/mother certificate required.", fontSize = 11.sp, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))
                                Text("• Security Deposit: ₹30,000 for UP Govt colleges (Refundable).", fontSize = 11.sp, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))
                            }
                        }
                    }
                }

                // Campus Highlight
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 ${college.campusHighlight}",
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onAskGemini != null) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onAskGemini("Can I get admission in ${college.name} with my score of $userScore marks ($category)? What was the closing cutoff and bond policy?")
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.2.dp, Color(0xFF6366F1)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("💬 Ask LAKSHYA AI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

data class GmcChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String
)

@Composable
fun GmcGeminiChatCard(
    messages: List<GmcChatMessage>,
    inputText: String,
    onInputChange: (String) -> Unit,
    isTyping: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSendMessage: (String) -> Unit,
    onOpenFullScreen: () -> Unit,
    onClearChat: () -> Unit,
    userScore: Int,
    category: String,
    state: String,
    quota: String,
    isDark: Boolean
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF131B2E) else Color(0xFFF8FAFC),
        border = BorderStroke(1.2.dp, if (isDark) Color(0x336366F1) else Color(0xFFCBD5E1)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gmc_gemini_chat_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { onToggleExpand() }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6366F1).copy(alpha = 0.2f)
                    ) {
                        Text("🤖", fontSize = 16.sp, modifier = Modifier.padding(6.dp))
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "LAKSHYA AI Cutoff Consultant",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "LIVE Q&A",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Score: ${if (userScore > 0) "$userScore M" else "Not set"} • $category • $state",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onOpenFullScreen, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Expand Chat",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Expand",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                // Quick Prompt Chips
                val quickPrompts = listOf(
                    "🎯 Can I get a GMC with my score?",
                    "🏛️ State Quota (85%) vs AIQ (15%)?",
                    "📜 UP, MP & Raj service bond duration",
                    "📈 Expected 2026 cutoff inflation?"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickPrompts.forEach { prompt ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF),
                            border = BorderStroke(1.dp, if (isDark) Color(0x33818CF8) else Color(0x446366F1)),
                            modifier = Modifier.clickable { onSendMessage(prompt) }
                        ) {
                            Text(
                                text = prompt,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Chat Messages Scroll Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF),
                    border = BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            GmcChatMessageBubble(message = msg, isDark = isDark)
                        }

                        if (isTyping) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF6366F1)
                                    )
                                    Text(
                                        "Lakshya AI is analyzing MCC database & cutoffs...",
                                        fontSize = 11.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = Color(0xFF6366F1)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Message Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        placeholder = { Text("Ask about cutoffs, colleges, bonds...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        enabled = !isTyping,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSendMessage(inputText) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
                        )
                    )

                    IconButton(
                        onClick = { onSendMessage(inputText) },
                        enabled = inputText.isNotBlank() && !isTyping,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (inputText.isNotBlank() && !isTyping) Color(0xFF4F46E5) else Color.Gray.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GmcChatMessageBubble(
    message: GmcChatMessage,
    isDark: Boolean
) {
    val context = LocalContext.current
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (message.isUser) 14.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 14.dp
            ),
            color = if (message.isUser) {
                Color(0xFF4F46E5)
            } else {
                if (isDark) Color(0xFF131B2E) else Color(0xFFF8FAFC)
            },
            border = if (!message.isUser) {
                BorderStroke(1.dp, if (isDark) Color(0x33818CF8) else Color(0xFFE2E8F0))
            } else null,
            modifier = if (message.isUser) {
                Modifier.widthIn(max = 300.dp)
            } else {
                Modifier.fillMaxWidth(0.98f)
            }
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                if (!message.isUser) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("🤖", fontSize = 12.sp)
                            Text(
                                "LAKSHYA AI Consultant",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                            )
                        }
                        IconButton(
                            onClick = {
                                val clip = ClipData.newPlainText("AI Counseling Advice", message.text)
                                clipboardManager?.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied advice! 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy advice",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                MathJaxView(
                    text = message.text,
                    isDark = isDark,
                    fontSize = 12.5.sp,
                    textColor = if (message.isUser) Color.White else null
                )
            }
        }
    }
}

@Composable
fun GmcCutoffChatDialog(
    messages: List<GmcChatMessage>,
    inputText: String,
    onInputChange: (String) -> Unit,
    isTyping: Boolean,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    userScore: Int,
    category: String,
    quota: String,
    state: String,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) Color(0xFF0F172A) else Color.White)
                .border(1.5.dp, Color(0xFF6366F1), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🤖", fontSize = 22.sp)
                        Column {
                            Text(
                                "Lakshya Cutoff AI Consultant",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                "Target: ${if (userScore > 0) "$userScore Marks" else "Score unset"} • $category • $state",
                                fontSize = 11.5.sp,
                                color = Color(0xFF6366F1)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClearChat) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = Color.Gray)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(8.dp))

                // Messages List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        GmcChatMessageBubble(message = msg, isDark = isDark)
                    }

                    if (isTyping) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF6366F1)
                                )
                                Text(
                                    "Consulting LAKSHYA AI & NTA/MCC Cutoff Matrix...",
                                    fontSize = 11.5.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = Color(0xFF6366F1)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Prompt Chips
                val dialogPrompts = listOf(
                    "What choice filling order is best for $userScore marks?",
                    "What is UP/MP service bond penalty rule?",
                    "AIQ vs State Quota probability for $category",
                    "List top 5 GMCs I can clear cutoff for"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    dialogPrompts.forEach { prompt ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF),
                            modifier = Modifier.clickable { onSendMessage(prompt) }
                        ) {
                            Text(
                                text = prompt,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        placeholder = { Text("Ask any question about cutoffs & admissions...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        enabled = !isTyping,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSendMessage(inputText) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
                        )
                    )

                    IconButton(
                        onClick = { onSendMessage(inputText) },
                        enabled = inputText.isNotBlank() && !isTyping,
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = if (inputText.isNotBlank() && !isTyping) Color(0xFF4F46E5) else Color.Gray.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun UpCutoffDossierDialog(
    userScore: Int,
    category: String,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSelectTier: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(26.dp))
                .background(if (isDark) Color(0xFF0F172A) else Color.White)
                .border(1.5.dp, Color(0xFF3B82F6), RoundedCornerShape(26.dp))
                .padding(18.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⭐", fontSize = 20.sp)
                            Column {
                                Text(
                                    "UP NEET Cutoff Dossier",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text("DGME 85% State Quota Analysis", fontSize = 11.5.sp, color = Color(0xFF3B82F6))
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                }

                // Overview Stats
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total UP GMCs", fontSize = 10.5.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                Text("44+", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF3B82F6))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Govt MBBS Seats", fontSize = 10.5.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                Text("4,500+", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Rural Bond", fontSize = 10.5.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                Text("2 Yrs", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                            }
                        }
                    }
                }

                // Last Round Cutoff Benchmarks Table
                item {
                    Text("📌 UP State Quota Last Closing Cutoffs (Round 1 vs Last Stray):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A))
                }

                val upCutoffData = listOf(
                    Triple("UR / Unreserved", "625 M (AIR ~16,400)", "605 M (AIR ~24,200)"),
                    Triple("OBC (BCNO)", "622 M (AIR ~17,800)", "603 M (AIR ~25,600)"),
                    Triple("EWS (EWNO)", "621 M (AIR ~18,400)", "602 M (AIR ~26,100)"),
                    Triple("SC (SCNO)", "495 M (AIR ~1,15,000)", "476 M (AIR ~1,38,000)"),
                    Triple("ST (STNO)", "360 M (AIR ~2,80,000)", "328 M (AIR ~3,40,000)")
                )

                items(upCutoffData, key = { it.first }) { (catTitle, r1, lastRound) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(catTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isDark) Color.White else Color(0xFF0F172A))
                                Text("Round 1: $r1", fontSize = 10.5.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Last Stray Vacancy", fontSize = 10.sp, color = Color(0xFF10B981))
                                Text(lastRound, fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF10B981))
                            }
                        }
                    }
                }

                // Important DGME UP Counselling Rules
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("📋 Essential DGME UP Counselling Rules:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A))
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MathJaxView(
                                text = """
1. **Domicile Criteria**: Candidates who passed 10th & 12th from Uttar Pradesh do NOT require a domicile certificate. If 10th or 12th was outside UP, UP domicile certificate of parents is mandatory.
2. **Security Money**: ₹30,000 refundable security deposit is required for Govt colleges (paid online during registration).
3. **Service Bond**: 2 years rural service in UP Govt PHC/CHC or ₹10 Lakhs penalty. (Exempted in BHU, AMU & AIIMS).
4. **Horizontal Reservation**: 30% Women (GL), 5% PwD (PH), 2% Freedom Fighters (FF), 1% Ex-Servicemen (NCC/EA).
                                """.trimIndent(),
                                isDark = isDark,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                // Quick Filter Buttons
                item {
                    Text("Explore by Tier:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF0F172A))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSelectTier("Top Apex") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("👑 Apex (5)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onSelectTier("Classic") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("🏛️ Classic (12)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onSelectTier("ASMC") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("🏥 ASMC (25+)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isDark: Boolean, valueColor: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            modifier = Modifier.weight(1.1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: if (isDark) Color.White else Color(0xFF0F172A),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.3f, fill = false)
        )
    }
}

private fun getCutoffMarks(college: MedicalCollege, category: String, quota: String, year: Int = 2025): Int {
    return college.getYearlyMarks(year, category, quota)
}

private fun getClosingRank(college: MedicalCollege, category: String, year: Int = 2025): Int {
    return college.getYearlyRank(year, category)
}

@Composable
fun GeminiAiCounselingDialog(
    result: GmcAiAnalysisResult,
    userScore: Int,
    category: String,
    quota: String,
    state: String,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSelectCollege: (String) -> Unit
) {
    val context = LocalContext.current
    var showFullMarkdown by remember { mutableStateOf(false) }

    val formattedTime = remember(result.syncTimestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(result.syncTimestamp))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(26.dp))
                .background(if (isDark) Color(0xFF0F172A) else Color.White)
                .border(
                    1.5.dp,
                    Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899))),
                    RoundedCornerShape(26.dp)
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 18.sp)
                        }
                        Column {
                            Text(
                                "Lakshya Live Counseling AI",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                "Synced: $formattedTime",
                                fontSize = 10.5.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile Parameters Tag Strip
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Score: $userScore/720",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF6366F1)
                                )
                                Text(
                                    "$category • $quota",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                                Text(
                                    "State: $state",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
                                )
                            }
                        }
                    }

                    // Predicted AIR Rank Range Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🎯", fontSize = 16.sp)
                                    Text(
                                        "Projected All India Rank (AIR)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF818CF8)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    result.expectedAirRankRange,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color.White else Color(0xFF1E1B4B)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    result.admissionChancesSummary,
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Round-Wise Trend Forecast
                    if (result.roundShiftForecast.isNotBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("📈", fontSize = 14.sp)
                                        Text(
                                            "Round Shift & Cutoff Dynamics",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Color(0xFF0F172A)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        result.roundShiftForecast,
                                        fontSize = 11.5.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Recommended Best-Fit Medical Colleges
                    if (result.topRecommendedColleges.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🏛️", fontSize = 14.sp)
                                    Text(
                                        "AI Best-Fit College Recommendations",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                }

                                result.topRecommendedColleges.forEach { rec ->
                                    val (badgeColor, badgeText) = when (rec.safetyLevel.lowercase()) {
                                        "safe" -> Color(0xFF10B981) to "Safe Zone"
                                        "reach", "borderline" -> Color(0xFFEF4444) to "Dream / Reach"
                                        else -> Color(0xFFF59E0B) to "Target Zone"
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectCollege(rec.collegeName) }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = rec.collegeName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = badgeColor.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        badgeText,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = badgeColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "📍 ${rec.state} • ${rec.quota}",
                                                    fontSize = 11.sp,
                                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                                )
                                                if (rec.estimatedClosingScore.isNotBlank()) {
                                                    Text(
                                                        "Est. Cutoff: ${rec.estimatedClosingScore}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF6366F1)
                                                    )
                                                }
                                            }

                                            if (rec.reason.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "💡 ${rec.reason}",
                                                    fontSize = 11.sp,
                                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Strategic Counseling Advice
                    if (result.strategicCounselingAdvice.isNotBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("💡", fontSize = 14.sp)
                                        Text(
                                            "Strategic Choice-Filling & Bond Advice",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        result.strategicCounselingAdvice,
                                        fontSize = 11.5.sp,
                                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFF78350F),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Expandable Raw Report
                    if (result.rawAnalysisMarkdown.isNotBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0x11FFFFFF) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFullMarkdown = !showFullMarkdown }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "📄 Full AI Counseling Dossier",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isDark) Color.White else Color(0xFF0F172A)
                                        )
                                        Icon(
                                            if (showFullMarkdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }

                                    if (showFullMarkdown) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        MathJaxView(
                                            text = result.rawAnalysisMarkdown,
                                            isDark = isDark,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(
                                "Lakshya GMC Analysis",
                                "Score: $userScore/720 ($category, $quota, $state)\nPredicted AIR: ${result.expectedAirRankRange}\n\n${result.admissionChancesSummary}\n\nCounseling Advice:\n${result.strategicCounselingAdvice}\n\nFull Report:\n${result.rawAnalysisMarkdown}"
                            )
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Counseling report copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

