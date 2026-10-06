package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AiProvider
import com.example.data.ExamCategory
import com.example.data.WeakTopic
import com.example.data.sourceWebsite
import com.example.data.formatGuidanceWithSource
import com.example.data.WebQuestionExtractor
import com.example.data.QuestionDeduplicationManager
import com.example.ui.AppViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.MathJaxView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeakTopicTestScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val allWeakTopics by viewModel.weakTopics.collectAsStateWithLifecycle()

    val aiProvider by viewModel.aiProvider.collectAsStateWithLifecycle()
    val geminiSelectedModel by viewModel.selectedGeminiModel.collectAsStateWithLifecycle()
    val openRouterSelectedModel by viewModel.openrouterSelectedModel.collectAsStateWithLifecycle()
    val groqSelectedModel by viewModel.groqSelectedModel.collectAsStateWithLifecycle()
    val cloudflareSelectedModel by viewModel.cloudflareSelectedModel.collectAsStateWithLifecycle()

    val activeModelDisplayName = when (aiProvider) {
        AiProvider.NATIVE_GEMINI -> "Gemini: $geminiSelectedModel"
        AiProvider.OPENROUTER -> "OpenRouter: ${openRouterSelectedModel.substringAfterLast("/")}"
        AiProvider.GROQ -> "Groq: $groqSelectedModel"
        AiProvider.CLOUDFLARE -> "Cloudflare: ${cloudflareSelectedModel.substringAfterLast("/")}"
    }

    var selectedSubjectTab by remember { mutableIntStateOf(0) }
    val subjects = listOf("Physics", "Chemistry", "Biology")
    val currentSubject = subjects[selectedSubjectTab]

    val subjectTopics = remember(allWeakTopics, currentSubject) {
        allWeakTopics.filter { it.subject.equals(currentSubject, ignoreCase = true) }
    }

    // Generator Dialog State
    var showGenerateDialog by remember { mutableStateOf(false) }
    var selectedTopicForTest by remember { mutableStateOf<WeakTopic?>(null) }
    var targetQuestionCount by remember { mutableIntStateOf(15) }
    var customCountInput by remember { mutableStateOf("15") }
    var selectedDifficulty by remember { mutableStateOf("NEET Standard") }
    var customUserPrompt by remember { mutableStateOf("") }
    var isGeneratingTest by remember { mutableStateOf(false) }
    var websiteSourceInput by remember { mutableStateOf("") }
    var avoidRepeatedQuestions by remember { mutableStateOf(true) }

    // Add Custom Topic Dialog State
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var customChapterInput by remember { mutableStateOf("") }
    var customTopicInput by remember { mutableStateOf("") }
    var customSeverityInput by remember { mutableStateOf("HIGH") }
    var customGuidanceInput by remember { mutableStateOf("") }
    var customWebsiteInput by remember { mutableStateOf("") }

    BackHandler {
        onNavigateBack()
    }

    val primaryColor = when (currentSubject) {
        "Physics" -> Color(0xFF38BDF8)
        "Chemistry" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    val bgGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF020617)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFEEF2F6)))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // TOP HEADER BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E293B) else Color.White)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Weak Topic Diagnostics 🎯",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "AI-Driven Custom Tests & Target Remediation",
                        fontSize = 11.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }

                // Add Custom Topic Button
                IconButton(
                    onClick = {
                        customChapterInput = ""
                        customTopicInput = ""
                        customGuidanceInput = ""
                        customWebsiteInput = ""
                        showAddCustomDialog = true
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Weak Topic",
                        tint = primaryColor
                    )
                }
            }

            // MODEL SYNC & STATS HERO BANNER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f)
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active Engine: $activeModelDisplayName",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "Auto-Synced",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981),
                            modifier = Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val criticalCount = allWeakTopics.count { it.severityLevel == "CRITICAL" }
                        val highCount = allWeakTopics.count { it.severityLevel == "HIGH" }
                        val totalMistakes = allWeakTopics.sumOf { it.mistakesCount }

                        StatBox("Total Areas", "${allWeakTopics.size}", primaryColor, Modifier.weight(1f), isDark)
                        StatBox("Critical", "$criticalCount", Color(0xFFEF4444), Modifier.weight(1f), isDark)
                        StatBox("High Yield", "$highCount", Color(0xFFF59E0B), Modifier.weight(1f), isDark)
                        StatBox("Logged Flaws", "$totalMistakes", Color(0xFF8B5CF6), Modifier.weight(1f), isDark)
                    }
                }
            }

            // 3-SUBJECT TAB SELECTOR
            TabRow(
                selectedTabIndex = selectedSubjectTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp)),
                containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                contentColor = primaryColor,
                indicator = {}
            ) {
                subjects.forEachIndexed { index, title ->
                    val isSelected = selectedSubjectTab == index
                    val count = allWeakTopics.count { it.subject.equals(title, ignoreCase = true) }
                    val tabColor = when (title) {
                        "Physics" -> Color(0xFF38BDF8)
                        "Chemistry" -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    }

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) tabColor else Color.Transparent)
                            .clickable { selectedSubjectTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (title) {
                                    "Physics" -> "⚛️ Physics"
                                    "Chemistry" -> "🧪 Chemistry"
                                    else -> "🧬 Biology"
                                },
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                            )
                            if (count > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White.copy(alpha = 0.25f) else tabColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else tabColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // LIST OF WEAK TOPICS IN SELECTED SUBJECT
            if (subjectTopics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Weak Topics in $currentSubject!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Either you've mastered all chapters or take an AI CBT test to diagnose flaws.",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                customChapterInput = ""
                                customTopicInput = ""
                                customGuidanceInput = ""
                                showAddCustomDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add $currentSubject Topic Manually", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    items(subjectTopics, key = { it.id }) { item ->
                        WeakTopicItemCard(
                            item = item,
                            primaryColor = primaryColor,
                            isDark = isDark,
                            onGenerateTest = {
                                selectedTopicForTest = item
                                targetQuestionCount = 15
                                customCountInput = "15"
                                customUserPrompt = ""
                                websiteSourceInput = item.sourceWebsite ?: ""
                                avoidRepeatedQuestions = true
                                showGenerateDialog = true
                            },
                            onDelete = {
                                viewModel.deleteWeakTopic(item.id)
                                Toast.makeText(context, "Removed from weak list", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // GENERATE CUSTOM TARGETED TEST DIALOG
        if (showGenerateDialog && selectedTopicForTest != null) {
            val topicItem = selectedTopicForTest!!

            Dialog(onDismissRequest = { if (!isGeneratingTest) showGenerateDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF0F172A) else Color.White
                    ),
                    border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Targeted CBT Generator 🚀",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "${topicItem.subject} • ${topicItem.chapter}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { if (!isGeneratingTest) showGenerateDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Target Subtopic Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = primaryColor.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = "🎯 Focus: ${topicItem.topicName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // QUESTION COUNT SELECTOR (Preset Chips + Custom Input)
                        Text(
                            text = "Number of Questions (Aap Apne Hisab Se Chunein):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Count Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(5, 10, 15, 20, 30).forEach { cnt ->
                                val isSel = targetQuestionCount == cnt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) primaryColor else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)))
                                        .clickable {
                                            targetQuestionCount = cnt
                                            customCountInput = cnt.toString()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$cnt Q",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSel) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF475569))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Numeric Input Field
                        OutlinedTextField(
                            value = customCountInput,
                            onValueChange = { input ->
                                customCountInput = input.filter { it.isDigit() }.take(2)
                                val parsed = customCountInput.toIntOrNull()
                                if (parsed != null) {
                                    targetQuestionCount = parsed.coerceIn(5, 50)
                                }
                            },
                            label = { Text("Or Type Exact Custom Count (5 - 50)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // DIFFICULTY SELECTOR
                        Text(
                            text = "Difficulty Level:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("NEET Standard", "Tricky Trap", "39-Yr PYQ").forEach { diff ->
                                val isSel = selectedDifficulty == diff
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) Color(0xFF6366F1) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)))
                                        .clickable { selectedDifficulty = diff }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = diff,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF475569))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // WEBSITE SOURCE / URL
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌐 Question Source / Website:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                            )
                            if (websiteSourceInput.isNotBlank()) {
                                Text(
                                    text = "Auto-Extract Active",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = websiteSourceInput,
                            onValueChange = { websiteSourceInput = it },
                            placeholder = { Text("e.g. neetprep.com or direct question bank URL", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Quick Website Preset Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("neetprep.com", "allen.in", "testbook.com").forEach { site ->
                                val isSel = websiteSourceInput.contains(site, ignoreCase = true)
                                Surface(
                                    onClick = { websiteSourceInput = if (isSel) "" else site },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) Color(0xFF6366F1) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        site,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ZERO-REPETITION TOGGLE
                        Surface(
                            onClick = { avoidRepeatedQuestions = !avoidRepeatedQuestions },
                            shape = RoundedCornerShape(10.dp),
                            color = if (avoidRepeatedQuestions) Color(0xFF10B981).copy(alpha = 0.12f) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                            border = BorderStroke(1.dp, if (avoidRepeatedQuestions) Color(0xFF10B981).copy(alpha = 0.5f) else Color.Transparent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🛡️ Zero Repeated Questions",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (avoidRepeatedQuestions) Color(0xFF10B981) else (if (isDark) Color.White else Color(0xFF0F172A))
                                    )
                                    Text(
                                        text = "Strictly eliminates previously solved questions from this topic",
                                        fontSize = 10.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                                Switch(
                                    checked = avoidRepeatedQuestions,
                                    onCheckedChange = { avoidRepeatedQuestions = it }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // CUSTOM AI PROMPT / FOCUS
                        Text(
                            text = "Custom Instruction (Optional):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = customUserPrompt,
                            onValueChange = { customUserPrompt = it },
                            placeholder = { Text("e.g. Include graph questions, tricky numericals, or NCERT statement lines", fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // ACTIVE ENGINE NOTICE
                        Text(
                            text = "⚡ Generating via Profile Model: $activeModelDisplayName",
                            fontSize = 10.5.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // GENERATE BUTTON
                        Button(
                            onClick = {
                                if (isGeneratingTest) return@Button
                                isGeneratingTest = true
                                val count = customCountInput.toIntOrNull()?.coerceIn(5, 50) ?: targetQuestionCount
                                viewModel.generateAndLaunchWeakTopicTest(
                                    context = context,
                                    exam = viewModel.activeExamGoal.value,
                                    subject = topicItem.subject,
                                    chapter = topicItem.chapter,
                                    topic = topicItem.topicName,
                                    questionCount = count,
                                    difficulty = selectedDifficulty,
                                    customPrompt = customUserPrompt.ifBlank { null },
                                    websiteSource = websiteSourceInput.ifBlank { null },
                                    avoidRepeats = avoidRepeatedQuestions,
                                    onSuccess = {
                                        isGeneratingTest = false
                                        showGenerateDialog = false
                                        onNavigate("ai_test")
                                    },
                                    onError = { err ->
                                        isGeneratingTest = false
                                        Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = !isGeneratingTest,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (isGeneratingTest) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (websiteSourceInput.isNotBlank()) "Extracting from ${websiteSourceInput.take(18)}..." else "Synthesizing $targetQuestionCount Questions...",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate & Start $targetQuestionCount Q Test Now", fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // ADD CUSTOM TOPIC DIALOG
        if (showAddCustomDialog) {
            Dialog(onDismissRequest = { showAddCustomDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF0F172A) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Add Weak Topic ($currentSubject)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = customChapterInput,
                            onValueChange = { customChapterInput = it },
                            label = { Text("Chapter Name", fontSize = 11.5.sp) },
                            placeholder = { Text("e.g. Ray Optics, Chemical Equilibrium", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customTopicInput,
                            onValueChange = { customTopicInput = it },
                            label = { Text("Specific Weak Topic / Subtopic", fontSize = 11.5.sp) },
                            placeholder = { Text("e.g. Lens Maker Formula, Critical Angle", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customGuidanceInput,
                            onValueChange = { customGuidanceInput = it },
                            label = { Text("AI Revision Notes / Traps (Optional)", fontSize = 11.5.sp) },
                            placeholder = { Text("e.g. Watch sign conventions for concave surfaces", fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customWebsiteInput,
                            onValueChange = { customWebsiteInput = it },
                            label = { Text("Target Website / URL (e.g. neetprep.com, Optional)", fontSize = 11.5.sp) },
                            placeholder = { Text("e.g. neetprep.com or question bank URL", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Quick Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("neetprep.com", "allen.in", "testbook.com").forEach { site ->
                                val isSel = customWebsiteInput.contains(site, ignoreCase = true)
                                Surface(
                                    onClick = { customWebsiteInput = if (isSel) "" else site },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) Color(0xFF6366F1) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        site,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Severity Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("CRITICAL" to Color(0xFFEF4444), "HIGH" to Color(0xFFF59E0B), "MODERATE" to Color(0xFF38BDF8)).forEach { (sev, col) ->
                                val isSel = customSeverityInput == sev
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) col else col.copy(alpha = 0.15f))
                                        .clickable { customSeverityInput = sev }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sev,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else col
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddCustomDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    if (customChapterInput.isBlank() || customTopicInput.isBlank()) {
                                        Toast.makeText(context, "Please enter chapter and topic name", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.addWeakTopic(
                                        subject = currentSubject,
                                        chapter = customChapterInput.trim(),
                                        topicName = customTopicInput.trim(),
                                        severityLevel = customSeverityInput,
                                        aiGuidanceNotes = formatGuidanceWithSource(customGuidanceInput.trim(), customWebsiteInput.trim())
                                    )
                                    showAddCustomDialog = false
                                    Toast.makeText(context, "Weak Topic Added Successfully!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                            ) {
                                Text("Add Topic", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeakTopicItemCard(
    item: WeakTopic,
    primaryColor: Color,
    isDark: Boolean,
    onGenerateTest: () -> Unit,
    onDelete: () -> Unit
) {
    val severityColor = when (item.severityLevel) {
        "CRITICAL" -> Color(0xFFEF4444)
        "HIGH" -> Color(0xFFF59E0B)
        else -> Color(0xFF38BDF8)
    }

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.8f) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Chapter and Severity Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.chapter,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(severityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.severityLevel,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = severityColor
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(start = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Topic Name (with LaTeX equation rendering support)
            if (item.topicName.contains("$") || item.topicName.contains("\\") || item.topicName.contains("^") || item.topicName.contains("_")) {
                MathJaxView(
                    text = item.topicName,
                    isDark = isDark,
                    fontSize = 13.5.sp,
                    textColor = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = item.topicName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            }

            if (item.sourceWebsite != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF6366F1).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🌐 Source: ${item.sourceWebsite}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats Pill Row (Mistakes Count & Avg Response Time)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⚠️ ${item.mistakesCount} Mistakes",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }

                Text(text = "•", color = Color.Gray, fontSize = 10.sp)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⏱️ ${item.avgTimeSpentSeconds}s/Q avg",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.avgTimeSpentSeconds > 110) Color(0xFFF59E0B) else Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Expand indicator
                Text(
                    text = if (isExpanded) "Hide Tips 🔼" else "NCERT Tips 🔽",
                    fontSize = 10.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }

            // Expandable AI Guidance & NCERT Tips Box (with full MathJax LaTeX Support)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💡 AI NCERT Strategy & Formula Key:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    MathJaxView(
                        text = if (item.aiGuidanceNotes.isNotBlank()) item.aiGuidanceNotes else "Revise NCERT theory lines and solve 20-30 targeted PYQs on this subtopic to eliminate negative marks.",
                        isDark = isDark,
                        fontSize = 11.5.sp,
                        textColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Button: Generate Targeted Test
            Button(
                onClick = onGenerateTest,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Generate Targeted Test on This Topic ⚡",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, accentColor: Color, modifier: Modifier = Modifier, isDark: Boolean) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                maxLines = 1
            )
        }
    }
}
