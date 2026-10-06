package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.MainActivity
import com.example.data.CompletedTopic
import com.example.data.DailyPractice
import com.example.data.GeminiChatAssistant
import com.example.data.MockTest
import com.example.data.StudyLog
import com.example.ui.GuardianMessage
import com.example.ui.GuardianPulseState
import com.example.ui.GuardianUpcomingTestInfo
import com.example.utils.ExportImportHelper
import com.example.utils.NeetProgressPdfGenerator
import com.example.workers.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardPlaygroundDialog(
    isDark: Boolean,
    selectedGeminiModel: String,
    onSelectGeminiModel: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF475569)
    val cardBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
    val liquidGlassBg = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                listOf(
                    Color(0xF01E293B),
                    Color(0xF5111827),
                    Color(0xFA0F172A)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xFAFFFFFF),
                    Color(0xF5F8FAFC),
                    Color(0xF8F1F5F9)
                )
            )
        }
    }
    val liquidGlassBorder = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                listOf(
                    Color(0x80FFFFFF),
                    Color(0x33818CF8),
                    Color(0x14FFFFFF),
                    Color(0x2E38BDF8)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xF2FFFFFF),
                    Color(0x4D6366F1),
                    Color(0x33CBD5E1),
                    Color(0x80FFFFFF)
                )
            )
        }
    }

    var testPrompt by remember { mutableStateOf("") }
    var testResponse by remember { mutableStateOf("") }
    var isTestingInProgress by remember { mutableStateOf(false) }
    var attachedImageUri by remember { mutableStateOf<String?>(null) }

    val playgroundImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        attachedImageUri = uri?.toString()
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = if (isDark) Color(0x4D6366F1) else Color(0x26475569)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(liquidGlassBg)
                .border(1.2.dp, liquidGlassBorder, RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🔮 LAKSHYA AI Playground", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = textColor)
                        Text("Test Lakshya intelligence models live", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close Dialog", tint = textColor)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                // Model Selection Row (Exclusively Gemini 3.7 Flash)
                Text("Dedicated AI Model", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, Color(0xFF6366F1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectGeminiModel("gemini-3.8-flash")
                            Toast.makeText(context, "Active AI Model: 🧠 Gemini 3.8 Flash", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🧠", fontSize = 18.sp)
                            Column {
                                Text(
                                    "Google Gemini",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
                                )
                                Text(
                                    "Ultra-Fast Reasoning & Vision Engine",
                                    fontSize = 10.5.sp,
                                    color = subTextColor
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                        ) {
                            Text(
                                "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(18.dp))
                
                OutlinedTextField(
                    value = testPrompt,
                    onValueChange = { testPrompt = it },
                    placeholder = { Text("Ask something to test AI intelligence...", color = subTextColor, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0x0AFFFFFF) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0x05FFFFFF) else Color(0xFFF1F5F9),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text("💡 Quick Symbols & LaTeX:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    val mathSymbols = listOf(
                        "π" to "π",
                        "θ" to "θ",
                        "α" to "α",
                        "β" to "β",
                        "λ" to "λ",
                        "μ" to "μ",
                        "Δ" to "Δ",
                        "Ω" to "Ω",
                        "√x" to "\\sqrt{x}",
                        "a/b" to "\\frac{a}{b}",
                        "∫" to "\\int",
                        "∞" to "\\infty",
                        "°" to "^\\circ",
                        "x²" to "^2",
                        "x₀" to "_0",
                        "±" to "\\pm"
                    )
                    items(mathSymbols, key = { it.first }) { (label, symbol) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0x15FFFFFF) else Color(0xFFE2E8F0))
                                .clickable {
                                    testPrompt = testPrompt + symbol
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = textColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Attachment Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x10FFFFFF) else Color(0xFFF1F5F9))
                            .clickable { playgroundImagePicker.launch("image/*") }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Upload Image",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Upload Doubt Image 📸",
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (attachedImageUri != null) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.2.dp, Color(0xFF6366F1), RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = attachedImageUri,
                                contentDescription = "Selected Image Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(16.dp)
                                    .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                                    .clickable { attachedImageUri = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Image",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (testPrompt.isNotBlank()) {
                            isTestingInProgress = true
                            coroutineScope.launch {
                                val res = GeminiChatAssistant.executeTestingPrompt(
                                    context = context,
                                    prompt = testPrompt,
                                    mediaUriStr = attachedImageUri,
                                    mediaType = if (attachedImageUri != null) "image" else null,
                                    targetModel = selectedGeminiModel
                                )
                                res.onSuccess { text ->
                                    testResponse = text
                                    isTestingInProgress = false
                                }.onFailure { err ->
                                    testResponse = "Error testing prompt: ${err.message}"
                                    isTestingInProgress = false
                                }
                            }
                        }
                    },
                    enabled = !isTestingInProgress && testPrompt.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (testPrompt.isBlank() || isTestingInProgress) {
                                    Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.2f), Color.Gray.copy(alpha = 0.2f)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899)))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTestingInProgress) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("✨", fontSize = 14.sp)
                                Text("Run AI Intelligence Test", color = if (testPrompt.isBlank()) subTextColor else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
                
                if (testResponse.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🤖 Response (${selectedGeminiModel.substringAfterLast("/")}):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8))
                        Text(
                            "Copy",
                            color = Color(0xFF6366F1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                val clipboardManagerLocal = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Lakshya Response", testResponse)
                                clipboardManagerLocal.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied response to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x08FFFFFF) else Color(0xFFF8FAFC))
                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        MathJaxView(
                            text = testResponse,
                            isDark = isDark,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardAdvisorDialog(
    isDark: Boolean,
    aiAdvisorMessages: List<GuardianMessage>,
    isAiAdvisorReplying: Boolean,
    isAiAdvisorAnalyzing: Boolean,
    guardianPulse: GuardianPulseState? = null,
    timerIsRunning: Boolean = false,
    timerSubject: String = "Physics",
    timerSecondsElapsed: Int = 0,
    studyTubeTodaySeconds: Long = 0L,
    studyTubeTotalSeconds: Long = 0L,
    studyTubeLastWatched: String = "",
    onClearMessages: () -> Unit,
    onRunDiagnosticScan: () -> Unit,
    onSubmitFeedback: (String) -> Unit,
    onStartTimer: ((minutes: Int, subject: String, chapter: String) -> Unit)? = null,
    onStopTimer: (() -> Unit)? = null,
    onExecuteAction: ((String) -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF475569)
    val cardBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
    val liquidGlassBg = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                listOf(
                    Color(0xF01E293B),
                    Color(0xF5111827),
                    Color(0xFA0F172A)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xFAFFFFFF),
                    Color(0xF5F8FAFC),
                    Color(0xF8F1F5F9)
                )
            )
        }
    }
    val liquidGlassBorder = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                listOf(
                    Color(0x80FFFFFF),
                    Color(0x33818CF8),
                    Color(0x14FFFFFF),
                    Color(0x2E38BDF8)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xF2FFFFFF),
                    Color(0x4D6366F1),
                    Color(0x33CBD5E1),
                    Color(0x80FFFFFF)
                )
            )
        }
    }

    var advisorFeedbackText by remember { mutableStateOf("") }
    var showSyllabusExpanded by remember { mutableStateOf(false) }

    val advisorSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                advisorFeedbackText = if (advisorFeedbackText.isBlank()) spokenText else "$advisorFeedbackText $spokenText"
            }
        }
    }

    fun startAdvisorVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your doubt or reply to LAKSHYA AI Guardian...")
        }
        try {
            advisorSpeechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice input not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.90f)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = if (isDark) Color(0x4D6366F1) else Color(0x26475569)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(liquidGlassBg)
                .border(1.2.dp, liquidGlassBorder, RoundedCornerShape(28.dp))
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛡️", fontSize = 18.sp)
                        }
                        Column {
                            val dialogGuardianTransition = rememberInfiniteTransition(label = "DialogGuardianBlink")
                            val dialogGuardianDotAlpha by dialogGuardianTransition.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "DialogGuardianDotAlpha"
                            )
                            val dialogGuardianDotScale by dialogGuardianTransition.animateFloat(
                                initialValue = 0.8f,
                                targetValue = 1.25f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "DialogGuardianDotScale"
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "LAKSHYA AI Guardian",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Box(
                                    modifier = Modifier
                                        .scale(dialogGuardianDotScale)
                                        .alpha(dialogGuardianDotAlpha)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                            Text(
                                "Big Brother & NEET/JEE Mentor • Active Recall Coach",
                                style = MaterialTheme.typography.labelSmall,
                                color = subTextColor
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (aiAdvisorMessages.isNotEmpty()) {
                            IconButton(onClick = onClearMessages) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset Chat", tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                            }
                        }
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Guardian Academic Command & Pulse Banner
                val pulse = guardianPulse ?: GuardianPulseState()
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.6f) else Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🎯", fontSize = 13.sp)
                                Text(
                                    pulse.readinessStatus,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(pulse.readinessColorHex)
                                )
                            }
                            Text(
                                "Readiness: ${pulse.readinessScore}%",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(pulse.readinessColorHex)
                            )
                        }

                        // Upcoming Mock Test & Syllabus Info
                        val nextTest = pulse.nextUpcomingTest
                        if (nextTest != null) {
                            val daysStr = when (nextTest.daysRemaining) {
                                0 -> "TODAY!"
                                1 -> "Tomorrow!"
                                else -> "in ${nextTest.daysRemaining} days"
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0x3038BDF8) else Color(0xFFE0F2FE),
                                border = BorderStroke(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSyllabusExpanded = !showSyllabusExpanded }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("📅", fontSize = 12.sp)
                                            Text(
                                                "Upcoming: ${nextTest.title}",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else Color(0xFF0369A1)
                                            )
                                        }
                                        Text(
                                            daysStr,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (nextTest.daysRemaining <= 2) Color(0xFFEF4444) else Color(0xFF0284C7)
                                        )
                                    }
                                    if (showSyllabusExpanded) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "⚛️ Physics: ${nextTest.physicsSyllabus.ifBlank { "Full Syllabus" }}\n" +
                                            "🧬 Bio: ${nextTest.biologySyllabus.ifBlank { "Full Syllabus" }}\n" +
                                            "🧪 Chem: ${nextTest.chemistrySyllabus.ifBlank { "Full Syllabus" }}",
                                            fontSize = 10.sp,
                                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                                            lineHeight = 14.sp
                                        )
                                    } else {
                                        Text(
                                            "Tap to view Physics, Bio & Chem syllabus breakdown ▾",
                                            fontSize = 9.5.sp,
                                            color = if (isDark) Color(0xAAFFFFFF) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }

                        // DPP Backlog & Active Timer Control Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📝 DPPs: ${pulse.dppCompletedCount}/${pulse.dppTotalCount} (${pulse.dppPendingCount} pending)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = subTextColor
                            )

                            if (timerIsRunning) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                                    modifier = Modifier.clickable { onStopTimer?.invoke() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981))
                                        )
                                        Text(
                                            "⏱️ $timerSubject (${timerSecondsElapsed / 60}m) • Stop",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Messages LazyColumn
                val listState = rememberLazyListState()
                
                LaunchedEffect(aiAdvisorMessages.size, isAiAdvisorReplying, isAiAdvisorAnalyzing) {
                    if (aiAdvisorMessages.isNotEmpty() && !isAiAdvisorAnalyzing) {
                        val targetIndex = if (isAiAdvisorReplying) aiAdvisorMessages.size else (aiAdvisorMessages.size - 1).coerceAtLeast(0)
                        try {
                            listState.animateScrollToItem(targetIndex)
                        } catch (_: Throwable) {
                            try { listState.scrollToItem(targetIndex) } catch (_: Throwable) {}
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) Color(0x05FFFFFF) else Color(0xFFF8FAFC))
                        .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                        .padding(10.dp)
                ) {
                    if (isAiAdvisorAnalyzing) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF6366F1),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Running active multi-graph trend scan...",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Lakshya AI is reviewing scores & study logs for downfalls",
                                style = MaterialTheme.typography.labelSmall,
                                color = subTextColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else if (aiAdvisorMessages.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🛡️", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Active Diagnostic Scan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Run a real-time scan to detect score downfalls and receive actionable advice.", style = MaterialTheme.typography.bodySmall, color = subTextColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRunDiagnosticScan,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Run Diagnostic Scan 🕵️‍♂️", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(aiAdvisorMessages, key = { it.id }) { msg ->
                                if (msg.sender == "user") {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp))
                                                .background(Brush.horizontalGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))))
                                                .padding(12.dp)
                                        ) {
                                            Column {
                                                Text("💬 Your Question", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(msg.text, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFFFFFFF))
                                                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                                .padding(12.dp)
                                        ) {
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text("🛡️", fontSize = 12.sp)
                                                        Text("LAKSHYA AI Guardian", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (isDark) Color(0x18FFFFFF) else Color(0xFFF1F5F9),
                                                        modifier = Modifier.clickable {
                                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                            val clip = ClipData.newPlainText("Guardian Advice", msg.text)
                                                            clipboard?.setPrimaryClip(clip)
                                                            Toast.makeText(context, "Copied advice! 📋", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = subTextColor, modifier = Modifier.size(10.dp))
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text("Copy", fontSize = 9.5.sp, color = subTextColor, fontWeight = FontWeight.Medium)
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                val cleanText = msg.text.replace(Regex("\\[ACTION:[^\\]]+\\]"), "").trim()
                                                NativeMarkdownText(
                                                    text = cleanText,
                                                    isDark = isDark,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                // Extract and render Action Buttons
                                                val actionRegex = Regex("\\[ACTION:[^\\]]+\\]")
                                                val actionTags = actionRegex.findAll(msg.text).map { it.value }.toList()
                                                if (actionTags.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        actionTags.forEach { actionTag ->
                                                            val clean = actionTag.trim().removeSurrounding("[", "]").removePrefix("ACTION:")
                                                            val parts = clean.split(":")
                                                            val actionType = parts.getOrNull(0)?.uppercase(Locale.ROOT) ?: ""
                                                            when (actionType) {
                                                                "TIMER", "START_TIMER" -> {
                                                                    val mins = parts.getOrNull(1)?.toIntOrNull() ?: 45
                                                                    val sub = parts.getOrNull(2) ?: "Physics"
                                                                    val chap = parts.getOrNull(3) ?: "Sprint"
                                                                    Button(
                                                                        onClick = {
                                                                            onStartTimer?.invoke(mins, sub, chap)
                                                                            Toast.makeText(context, "Guardian: Started $mins min $sub timer! ⏱️", Toast.LENGTH_SHORT).show()
                                                                        },
                                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                                        shape = RoundedCornerShape(10.dp),
                                                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                                                    ) {
                                                                        Text(
                                                                            "⏱️ Auto-Launch $mins Min $sub Timer ($chap)",
                                                                            fontSize = 11.5.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = Color.White
                                                                        )
                                                                    }
                                                                }
                                                                "NAV" -> {
                                                                    val dest = parts.getOrNull(1) ?: "dashboard"
                                                                    val (label, icon) = when (dest.lowercase(Locale.ROOT)) {
                                                                        "mocks" -> "📅 View Upcoming Tests & Syllabus" to Icons.Default.DateRange
                                                                        "targets" -> "📝 Open DPPs & Daily Targets" to Icons.Default.CheckCircle
                                                                        "timer" -> "⏱️ Open Study Timer Screen" to Icons.Default.Timer
                                                                        "studytube" -> "📺 Open StudyTube Classes" to Icons.Default.PlayArrow
                                                                        "mistakes" -> "📓 Open Error Notebook" to Icons.Default.Book
                                                                        else -> "🚀 Navigate to $dest" to Icons.Default.ArrowForward
                                                                    }
                                                                    OutlinedButton(
                                                                        onClick = {
                                                                            onExecuteAction?.invoke(actionTag)
                                                                        },
                                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6366F1)),
                                                                        border = BorderStroke(1.dp, Color(0xFF6366F1)),
                                                                        shape = RoundedCornerShape(10.dp),
                                                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                                                    ) {
                                                                        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                                                        Spacer(modifier = Modifier.width(6.dp))
                                                                        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
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

                            if (isAiAdvisorReplying) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDark) Color(0x10FFFFFF) else Color(0xFFF1F5F9))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            CircularProgressIndicator(color = Color(0xFF6366F1), strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                            Text("Guardian is analyzing your radar & preparing guidance...", fontSize = 11.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))

                // Quick Suggestion Chips Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0x206366F1) else Color(0xFFEEF2FF),
                            border = BorderStroke(0.8.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                advisorFeedbackText = "Bhai 45 min ka Physics timer laga do aur padhai shuru karwa do!"
                            }
                        ) {
                            Text("⏱️ 45m Physics Timer Laga Do", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6366F1), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0x2010B981) else Color(0xFFECFDF5),
                            border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                advisorFeedbackText = "Kaisa chal raha hai padhai aur upcoming test ka taiyari? Pura review do!"
                            }
                        ) {
                            Text("🎯 Padhai Aur Test Review", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF10B981), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0x20F59E0B) else Color(0xFFFFFBEB),
                            border = BorderStroke(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                advisorFeedbackText = "Upcoming mock test syllabus kaisa cover karein? DPPs backlog kaise clear karein?"
                            }
                        ) {
                            Text("📅 Syllabus & DPPs Strategy", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD97706), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                }
                
                val sendBtnEnabled = advisorFeedbackText.isNotBlank() && !isAiAdvisorReplying && !isAiAdvisorAnalyzing
                val disabledCol = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
                val sendBtnBgBrush = if (sendBtnEnabled) {
                    Brush.horizontalGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)))
                } else {
                    Brush.horizontalGradient(listOf(disabledCol, disabledCol))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = advisorFeedbackText,
                        onValueChange = { advisorFeedbackText = it },
                        placeholder = { Text("Ask Guardian or tell 'timer laga do'...", fontSize = 12.5.sp, color = subTextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                            unfocusedContainerColor = if (isDark) Color(0x0AFFFFFF) else Color(0xFFF8FAFC),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = if (isDark) Color(0x20FFFFFF) else Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                        maxLines = 4,
                        enabled = !isAiAdvisorReplying && !isAiAdvisorAnalyzing,
                        trailingIcon = {
                            IconButton(
                                onClick = { startAdvisorVoiceInput() },
                                enabled = !isAiAdvisorReplying && !isAiAdvisorAnalyzing
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(sendBtnBgBrush)
                            .clickable(enabled = sendBtnEnabled) {
                                val textToSend = advisorFeedbackText
                                advisorFeedbackText = ""
                                onSubmitFeedback(textToSend)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAiAdvisorReplying) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (sendBtnEnabled) Color.White else subTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardProfileDialog(
    appPassword: String?,
    habitPassword: String?,
    securityQuestion: String = "What is your favourite food?",
    securityAnswer: String? = null,
    onSetSecurityLock: (password: String?, question: String?, answer: String?) -> Unit = { _, _, _ -> },
    onSetAppPassword: (String?) -> Unit = {},
    onSetHabitPassword: (String?) -> Unit = {},
    isDark: Boolean,
    userName: String,
    userClass: String,
    userAvatarUri: String?,
    geminiApiKey1: String,
    geminiApiKey2: String,
    geminiApiKey3: String,
    selectedGeminiModel: String,
    neetTargetMillis: Long,
    logs: List<StudyLog>,
    tests: List<MockTest>,
    dailyPractices: List<DailyPractice>,
    completedTopics: List<CompletedTopic>,
    notificationTimes: Map<String, Int> = emptyMap(),
    onUpdateNotificationTime: (String, Int) -> Unit = { _, _ -> },
    onSaveProfile: (name: String, userClass: String, avatarUri: String?, key1: String, key2: String, key3: String, model: String) -> Unit,
    onSetDarkMode: (Boolean) -> Unit,
    onResetAllData: () -> Unit,
    onSelectOpenRouterModel: ((String) -> Unit)? = null,
    onSelectGroqModel: ((String) -> Unit)? = null,
    onSelectCloudflareModel: ((String) -> Unit)? = null,
    currentAiProvider: com.example.data.AiProvider = com.example.data.AiProvider.NATIVE_GEMINI,
    onSelectAiProvider: ((com.example.data.AiProvider) -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cardBorder = if (isDark) Color(0x33818CF8) else Color(0x406366F1)
    var tempName by remember { mutableStateOf(userName) }
    var tempClass by remember { mutableStateOf(userClass) }
    var tempAvatarUri by remember { mutableStateOf(userAvatarUri) }
    var tempKey1 by remember { mutableStateOf(geminiApiKey1) }
    var tempKey2 by remember { mutableStateOf(geminiApiKey2) }
    var tempKey3 by remember { mutableStateOf(geminiApiKey3) }
    var tempModel by remember(selectedGeminiModel) { mutableStateOf(selectedGeminiModel) }
    var isDarkTheme by remember { mutableStateOf(isDark) }

    // Gemini Model State
    var geminiSelectedModel by remember(selectedGeminiModel) {
        mutableStateOf(com.example.data.GeminiModelManager.getSelectedModel(context))
    }
    var geminiModels by remember {
        mutableStateOf(com.example.data.GeminiModelManager.getCachedModels(context))
    }
    var isGeminiLoadingModels by remember { mutableStateOf(false) }
    var showGeminiModelPicker by remember { mutableStateOf(false) }
    var showGeminiQuotaBadge by remember {
        mutableStateOf(
            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getBoolean("show_gemini_quota_badge", true)
        )
    }

    // Dual AI Engine State
    var aiProvider by remember(currentAiProvider) {
        mutableStateOf(currentAiProvider)
    }
    var openRouterApiKey by remember {
        mutableStateOf(
            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("openrouter_api_key", "") ?: ""
        )
    }
    var openRouterSelectedModel by remember {
        mutableStateOf(
            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("openrouter_selected_model", "google/gemini-2.5-flash") ?: "google/gemini-2.5-flash"
        )
    }
    var openRouterMaxTokens by remember {
        mutableStateOf(
            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getInt("openrouter_max_tokens", 8192)
        )
    }
    var openRouterKeyStatus by remember { mutableStateOf("") }
    var isVerifyingOpenRouterKey by remember { mutableStateOf(false) }
    var showOpenRouterModelPicker by remember { mutableStateOf(false) }
    var openRouterModels by remember {
        mutableStateOf(com.example.data.OpenRouterManager.getCachedModels(context))
    }
    var isOpenRouterLoadingModels by remember { mutableStateOf(false) }

    // Groq LPU State
    var groqApiKey by remember {
        mutableStateOf(com.example.data.GroqManager.getGroqApiKey(context))
    }
    var groqSelectedModel by remember {
        mutableStateOf(com.example.data.GroqManager.getSelectedModel(context))
    }
    var groqKeyStatus by remember { mutableStateOf("") }
    var isVerifyingGroqKey by remember { mutableStateOf(false) }
    var groqModels by remember {
        mutableStateOf(com.example.data.GroqManager.getCachedModels(context))
    }
    var isGroqLoadingModels by remember { mutableStateOf(false) }
    var showGroqModelPicker by remember { mutableStateOf(false) }

    // Cloudflare Workers AI State
    var cloudflareAccountId by remember {
        mutableStateOf(context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getString(com.example.data.CloudflareManager.KEY_CF_ACCOUNT_ID, "") ?: "")
    }
    var cloudflareApiToken by remember {
        mutableStateOf(context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getString(com.example.data.CloudflareManager.KEY_CF_API_TOKEN, "") ?: "")
    }
    var cloudflareStatus by remember { mutableStateOf("") }
    var isVerifyingCloudflare by remember { mutableStateOf(false) }
    var cloudflareSelectedModel by remember {
        mutableStateOf(com.example.data.CloudflareManager.getSelectedModel(context))
    }
    var cloudflareModels by remember {
        mutableStateOf(com.example.data.CloudflareManager.getCachedModels(context))
    }
    var isCloudflareLoadingModels by remember { mutableStateOf(false) }
    var showCloudflareModelPicker by remember { mutableStateOf(false) }

    fun verifyCloudflare() {
        coroutineScope.launch {
            isVerifyingCloudflare = true
            cloudflareStatus = "Testing Cloudflare..."
            val res = com.example.data.CloudflareManager.verifyCloudflareCredentials(context, cloudflareAccountId, cloudflareApiToken)
            cloudflareStatus = res
            isVerifyingCloudflare = false
        }
    }

    var key1Status by remember { mutableStateOf("") }
    var key2Status by remember { mutableStateOf("") }
    var key3Status by remember { mutableStateOf("") }
    
    var isVerifyingKeys by remember { mutableStateOf(false) }
    var currentlyWorkingApiKey by remember {
        mutableStateOf(
            com.example.data.GeminiChatAssistant.getCurrentlyWorkingApiKey(context)
        )
    }
    var showExportBackupDialog by remember { mutableStateOf(false) }
    var exportIncludeApiKeys by remember { mutableStateOf(true) }

    var showResetConfirmation by remember { mutableStateOf(false) }

    fun verifyApiKeys() {
        coroutineScope.launch {
            isVerifyingKeys = true
            key1Status = if (tempKey1.isNotBlank()) "Testing Gemini..." else "Not Configured"

            if (tempKey1.isNotBlank()) {
                key1Status = com.example.data.GeminiChatAssistant.checkKeyStatus(tempKey1)
            }

            if (key1Status.contains("Active") || key1Status.contains("Ready") || key1Status.contains("✅")) {
                context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .edit().putString("currently_working_api_key", tempKey1.trim()).apply()
                currentlyWorkingApiKey = tempKey1.trim()
            }
            isVerifyingKeys = false
        }
    }

    fun verifyGroqKey() {
        coroutineScope.launch {
            isVerifyingGroqKey = true
            groqKeyStatus = "Testing Groq LPU..."
            val res = com.example.data.GroqManager.verifyGroqKey(context, groqApiKey)
            groqKeyStatus = res
            isVerifyingGroqKey = false
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val persistentFile = java.io.File(context.filesDir, "user_avatar_persistent.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    persistentFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempAvatarUri = Uri.fromFile(persistentFile).toString()
                Toast.makeText(context, "Profile picture selected! 📸", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                tempAvatarUri = uri.toString()
                Toast.makeText(context, "Profile picture selected! 📸", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val (success, msg) = com.example.utils.ExportImportHelper.exportData(context, uri, includeApiKeys = exportIncludeApiKeys)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val (success, msg) = com.example.utils.ExportImportHelper.importData(context, uri)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                if (success) {
                    onDismissRequest()
                }
            }
        }
    }

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0x2E1E293B) else Color(0xB8FFFFFF)

    val dialogBgBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                listOf(
                    Color(0xF01E293B),
                    Color(0xF5111827),
                    Color(0xFA0F172A)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xEBFFFFFF),
                    Color(0xD9F8FAFC),
                    Color(0xCCF1F5F9),
                    Color(0xDEFFFFFF)
                )
            )
        }
    }
    val dialogBorderBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                listOf(
                    Color(0x80FFFFFF),
                    Color(0x33818CF8),
                    Color(0x14FFFFFF),
                    Color(0x2E38BDF8)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xF5FFFFFF),
                    Color(0x666366F1),
                    Color(0x4038BDF8),
                    Color(0xB3FFFFFF)
                )
            )
        }
    }

    val handleDismiss = {
        onSelectAiProvider?.invoke(aiProvider)
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit().putString("ai_provider", aiProvider.name).apply()
        onDismissRequest()
    }

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = if (isDark) Color(0x4D6366F1) else Color(0x26475569)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(dialogBgBrush)
                .border(1.2.dp, dialogBorderBrush, RoundedCornerShape(28.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Liquid Glass Accents
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Aspirant Profile",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                fontSize = 21.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                        )
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "NEET 2026/27",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(
                            text = "Aspirant Identity, Cloud Synchronization & Liquid Glass",
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor,
                            fontSize = 11.5.sp
                        )
                    }
                    IconButton(
                        onClick = handleDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x20FFFFFF) else Color(0x306366F1))
                            .border(1.dp, if (isDark) Color(0x20FFFFFF) else Color(0x40CBD5E1), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Scrollable Body Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 1. Profile Avatar Card with Liquid Ring
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                Color(0xFF6366F1),
                                                Color(0xFFEC4899),
                                                Color(0xFF06B6D4),
                                                Color(0xFF10B981),
                                                Color(0xFF6366F1)
                                            )
                                        )
                                    )
                                    .padding(2.5.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF0F172A) else Color.White)
                                    .clickable { avatarPicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!tempAvatarUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = tempAvatarUri,
                                        contentDescription = "Profile Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tempName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 24.sp
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Profile Avatar 🩺",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        fontSize = 15.sp
                                    )
                                }
                                Text(
                                    text = "Tap circle or button to choose your photo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subTextColor,
                                    fontSize = 11.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) Color(0x306366F1) else Color(0x206366F1),
                                    border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable { avatarPicker.launch("image/*") }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color(0xFF818CF8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Change Photo 📷",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4F46E5)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Display Name
                    Column {
                        Text(
                            text = "Display Name",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            trailingIcon = { Text("👨‍⚕️", fontSize = 18.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (isDark) Color(0x10FFFFFF) else Color(0x88FFFFFF),
                                unfocusedContainerColor = if (isDark) Color(0x0AFFFFFF) else Color(0x40FFFFFF),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = if (isDark) Color(0x20FFFFFF) else Color(0xFFCBD5E1)
                            )
                        )
                    }

                    // 3. Class Selection
                    Column {
                        Text(
                            text = "Class & Aspirant Stage",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("11th", "12th", "Dropper").forEach { classOption ->
                                val isSelected = tempClass.equals(classOption, ignoreCase = true)
                                val classPillBrush = if (isSelected) {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                    )
                                } else {
                                    Brush.linearGradient(
                                        if (isDark) listOf(Color(0x18FFFFFF), Color(0x18FFFFFF))
                                        else listOf(Color(0x60FFFFFF), Color(0x60FFFFFF))
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(classPillBrush)
                                        .border(
                                            1.2.dp,
                                            if (isSelected) Color(0xFFC7D2FE) else (if (isDark) Color(0x20FFFFFF) else Color(0x40CBD5E1)),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { tempClass = classOption }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = classOption,
                                        color = if (isSelected) Color.White else textColor,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // 4. Academic Progress Report Card
                    ProfileAcademicReportCard(
                        isDark = isDark,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        tempName = tempName,
                        tempClass = tempClass,
                        tempAvatarUri = tempAvatarUri,
                        neetTargetMillis = neetTargetMillis,
                        logs = logs,
                        tests = tests,
                        dailyPractices = dailyPractices,
                        completedTopics = completedTopics
                    )


                    // 5. Cloud Backup Card
                    ProfileCloudBackupCard(
                        isDark = isDark,
                        onExportBackup = {
                            showExportBackupDialog = true
                        },
                        onImportBackup = { importLauncher.launch("application/json") },
                        onShareBackup = {
                            val backupFile = com.example.utils.ExportImportHelper.createBackupZipFile(context)
                            if (backupFile != null) {
                                com.example.utils.ExportImportHelper.shareBackupToGoogleDriveOrEmail(context, backupFile)
                            } else {
                                Toast.makeText(context, "Failed to create backup file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // 6. App Appearance & Theme
                    ProfileAppearanceCard(
                        isDark = isDark,
                        isDarkTheme = isDarkTheme,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        onSetTheme = { newDark ->
                            isDarkTheme = newDark
                            onSetDarkMode(newDark)
                        }
                    )

                    // 7. AI Engines & OCR Models (Dual Architecture: Native Gemini vs OpenRouter)
                    AiEngineSettingsSection(
                        isDark = isDark,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        activeProvider = aiProvider,
                        onProviderChange = { provider ->
                            aiProvider = provider
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit().putString("ai_provider", provider.name).apply()
                            onSelectAiProvider?.invoke(provider)
                            Toast.makeText(context, "Active AI Engine: ${provider.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        isVerifyingGeminiKeys = isVerifyingKeys,
                        onVerifyGeminiKeys = { verifyApiKeys() },
                        currentlyWorkingGeminiKey = currentlyWorkingApiKey,
                        selectedGeminiModelId = geminiSelectedModel,
                        onSelectGeminiModel = { modelId ->
                            geminiSelectedModel = modelId
                            tempModel = modelId
                            aiProvider = com.example.data.AiProvider.NATIVE_GEMINI
                            com.example.data.GeminiModelManager.setSelectedModel(context, modelId)
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString(com.example.data.GeminiModelManager.KEY_SELECTED_GEMINI_MODEL, modelId)
                                .putString("selected_gemini_model", modelId)
                                .putString("ai_provider", com.example.data.AiProvider.NATIVE_GEMINI.name)
                                .apply()
                            onSaveProfile(tempName, tempClass, tempAvatarUri, tempKey1, tempKey2, tempKey3, modelId)
                            onSelectAiProvider?.invoke(com.example.data.AiProvider.NATIVE_GEMINI)
                            Toast.makeText(context, "Gemini Model: $modelId (Active ✦)", Toast.LENGTH_SHORT).show()
                        },
                        geminiModels = geminiModels,
                        isGeminiLoadingModels = isGeminiLoadingModels,
                        onRefreshGeminiModels = {
                            if (!isGeminiLoadingModels) {
                                isGeminiLoadingModels = true
                                coroutineScope.launch {
                                    val res = com.example.data.GeminiModelManager.fetchLiveModels(context, tempKey1)
                                    isGeminiLoadingModels = false
                                    res.fold(
                                        onSuccess = {
                                            geminiModels = it
                                            Toast.makeText(context, "✅ Synced ${it.size} live Gemini models!", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { err ->
                                            val msg = if (tempKey1.isBlank()) {
                                                "⚠️ Please enter your Gemini API Key in Settings first"
                                            } else {
                                                "Sync failed: ${err.message}"
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        },
                        onOpenGeminiModelPicker = {
                            showGeminiModelPicker = true
                        },
                        showGeminiQuotaBadge = showGeminiQuotaBadge,
                        onToggleShowGeminiQuotaBadge = { enabled ->
                            showGeminiQuotaBadge = enabled
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("show_gemini_quota_badge", enabled)
                                .apply()
                            val msg = if (enabled) "Gemini Quota Badge: ON 🟢" else "Gemini Quota Badge: OFF ⚪"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        geminiKey1 = tempKey1,
                        geminiKey1Status = key1Status,
                        onGeminiKey1Change = {
                            tempKey1 = it
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit().putString("gemini_api_key_1", it.trim()).apply()
                        },
                        onClearGeminiKey1 = {
                            tempKey1 = ""
                            key1Status = ""
                            currentlyWorkingApiKey = ""
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit().putString("gemini_api_key_1", "").apply()
                            onSaveProfile(tempName, tempClass, tempAvatarUri, "", "", "", com.example.data.GeminiModelManager.DEFAULT_MODEL)
                            Toast.makeText(context, "Gemini Key removed 🗑️", Toast.LENGTH_SHORT).show()
                        },
                        openRouterKey = openRouterApiKey,
                        openRouterKeyStatus = openRouterKeyStatus,
                        isVerifyingOpenRouterKey = isVerifyingOpenRouterKey,
                        onOpenRouterKeyChange = {
                            openRouterApiKey = it
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit().putString("openrouter_api_key", it.trim()).apply()
                        },
                        onClearOpenRouterKey = {
                            openRouterApiKey = ""
                            openRouterKeyStatus = ""
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit().putString("openrouter_api_key", "").apply()
                            Toast.makeText(context, "OpenRouter key removed 🗑️", Toast.LENGTH_SHORT).show()
                        },
                        onVerifyOpenRouterKey = {
                            if (openRouterApiKey.isNotBlank()) {
                                isVerifyingOpenRouterKey = true
                                openRouterKeyStatus = "Verifying OpenRouter Key..."
                                coroutineScope.launch {
                                    val res = com.example.data.OpenRouterManager.verifyApiKey(openRouterApiKey)
                                    isVerifyingOpenRouterKey = false
                                    res.fold(
                                        onSuccess = { msg ->
                                            openRouterKeyStatus = msg
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            // Auto-refresh models with the newly validated key
                                            isOpenRouterLoadingModels = true
                                            val mRes = com.example.data.OpenRouterManager.fetchLiveModels(context, openRouterApiKey)
                                            isOpenRouterLoadingModels = false
                                            mRes.onSuccess { openRouterModels = it }
                                        },
                                        onFailure = { err ->
                                            openRouterKeyStatus = "❌ ${err.message}"
                                            Toast.makeText(context, "Failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        },
                        selectedOpenRouterModelId = openRouterSelectedModel,
                        onSelectOpenRouterModel = { modelId ->
                            openRouterSelectedModel = modelId
                            aiProvider = com.example.data.AiProvider.OPENROUTER
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("openrouter_selected_model", modelId)
                                .putString("ai_provider", com.example.data.AiProvider.OPENROUTER.name)
                                .apply()
                            onSelectOpenRouterModel?.invoke(modelId)
                            onSelectAiProvider?.invoke(com.example.data.AiProvider.OPENROUTER)
                            Toast.makeText(context, "Selected Model: ${modelId.substringAfterLast("/")} (OpenRouter Active 🌐)", Toast.LENGTH_SHORT).show()
                        },
                        openRouterModels = openRouterModels,
                        isOpenRouterLoadingModels = isOpenRouterLoadingModels,
                        onRefreshOpenRouterModels = {
                            if (!isOpenRouterLoadingModels) {
                                isOpenRouterLoadingModels = true
                                coroutineScope.launch {
                                    val res = com.example.data.OpenRouterManager.fetchLiveModels(context, openRouterApiKey)
                                    isOpenRouterLoadingModels = false
                                    res.onSuccess {
                                        openRouterModels = it
                                        Toast.makeText(context, "✅ Synced ${it.size} live models from OpenRouter!", Toast.LENGTH_SHORT).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Sync failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onOpenModelPicker = {
                            showOpenRouterModelPicker = true
                        },
                        openRouterMaxTokens = openRouterMaxTokens,
                        onOpenRouterMaxTokensChange = { newTokens ->
                            openRouterMaxTokens = newTokens
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit().putInt("openrouter_max_tokens", newTokens).apply()
                            com.example.data.OpenRouterManager.setMaxTokens(context, newTokens)
                            Toast.makeText(context, "🎯 OpenRouter Token Limit: $newTokens", Toast.LENGTH_SHORT).show()
                        },
                        groqKey = groqApiKey,
                        groqKeyStatus = groqKeyStatus,
                        isVerifyingGroqKey = isVerifyingGroqKey,
                        onGroqKeyChange = {
                            groqApiKey = it
                            com.example.data.GroqManager.setGroqApiKey(context, it)
                        },
                        onClearGroqKey = {
                            groqApiKey = ""
                            groqKeyStatus = ""
                            com.example.data.GroqManager.setGroqApiKey(context, "")
                            Toast.makeText(context, "Groq Key removed 🗑️", Toast.LENGTH_SHORT).show()
                        },
                        onVerifyGroqKey = { verifyGroqKey() },
                        selectedGroqModelId = groqSelectedModel,
                        onSelectGroqModel = { modelId ->
                            groqSelectedModel = modelId
                            aiProvider = com.example.data.AiProvider.GROQ
                            com.example.data.GroqManager.setSelectedModel(context, modelId)
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString(com.example.data.GroqManager.KEY_GROQ_MODEL, modelId)
                                .putString("ai_provider", com.example.data.AiProvider.GROQ.name)
                                .apply()
                            onSelectGroqModel?.invoke(modelId)
                            onSelectAiProvider?.invoke(com.example.data.AiProvider.GROQ)
                            Toast.makeText(context, "Groq Model: ${modelId.substringAfterLast("/")} (Active ⚡)", Toast.LENGTH_SHORT).show()
                        },
                        groqModels = groqModels,
                        isGroqLoadingModels = isGroqLoadingModels,
                        onRefreshGroqModels = {
                            if (!isGroqLoadingModels) {
                                isGroqLoadingModels = true
                                coroutineScope.launch {
                                    val res = com.example.data.GroqManager.fetchLiveModels(context, groqApiKey)
                                    isGroqLoadingModels = false
                                    res.onSuccess {
                                        groqModels = it
                                        Toast.makeText(context, "✅ Synced ${it.size} live models from Groq server!", Toast.LENGTH_SHORT).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Sync failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onOpenGroqModelPicker = {
                            showGroqModelPicker = true
                        },
                        cloudflareAccountId = cloudflareAccountId,
                        onCloudflareAccountIdChange = {
                            cloudflareAccountId = it
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit().putString(com.example.data.CloudflareManager.KEY_CF_ACCOUNT_ID, it.trim()).apply()
                        },
                        cloudflareApiToken = cloudflareApiToken,
                        cloudflareStatus = cloudflareStatus,
                        isVerifyingCloudflare = isVerifyingCloudflare,
                        onCloudflareApiTokenChange = {
                            cloudflareApiToken = it
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit().putString(com.example.data.CloudflareManager.KEY_CF_API_TOKEN, it.trim()).apply()
                        },
                        onClearCloudflareToken = {
                            cloudflareApiToken = ""
                            cloudflareStatus = ""
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit().remove(com.example.data.CloudflareManager.KEY_CF_API_TOKEN).apply()
                            Toast.makeText(context, "Cloudflare Token cleared 🗑️", Toast.LENGTH_SHORT).show()
                        },
                        onVerifyCloudflare = { verifyCloudflare() },
                        selectedCloudflareModelId = cloudflareSelectedModel,
                        onSelectCloudflareModel = { modelId ->
                            cloudflareSelectedModel = modelId
                            aiProvider = com.example.data.AiProvider.CLOUDFLARE
                            com.example.data.CloudflareManager.setSelectedModel(context, modelId)
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString(com.example.data.CloudflareManager.KEY_CF_MODEL, modelId)
                                .putString("ai_provider", com.example.data.AiProvider.CLOUDFLARE.name)
                                .apply()
                            onSelectCloudflareModel?.invoke(modelId)
                            onSelectAiProvider?.invoke(com.example.data.AiProvider.CLOUDFLARE)
                            Toast.makeText(context, "Cloudflare Model: ${modelId.substringAfterLast("/")} (Active ☁️)", Toast.LENGTH_SHORT).show()
                        },
                        cloudflareModels = cloudflareModels,
                        isCloudflareLoadingModels = isCloudflareLoadingModels,
                        onRefreshCloudflareModels = {
                            if (!isCloudflareLoadingModels) {
                                isCloudflareLoadingModels = true
                                coroutineScope.launch {
                                    val res = com.example.data.CloudflareManager.fetchLiveModels(context, cloudflareAccountId, cloudflareApiToken)
                                    isCloudflareLoadingModels = false
                                    res.onSuccess {
                                        cloudflareModels = it
                                        Toast.makeText(context, "✅ Synced ${it.size} live models from Cloudflare!", Toast.LENGTH_SHORT).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Sync failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onOpenCloudflareModelPicker = {
                            showCloudflareModelPicker = true
                        }
                    )

                    // 8. Password & Privacy Lock
                    ProfileSecurityLockSection(
                        isDark = isDark,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        cardBorder = cardBorder,
                        appPassword = appPassword,
                        habitPassword = habitPassword,
                        onSetSecurityLock = onSetSecurityLock,
                        onSetHabitPassword = onSetHabitPassword
                    )

                    // 9. Danger Zone
                    ProfileDangerZoneSection(
                        subTextColor = subTextColor,
                        onTriggerReset = { showResetConfirmation = true }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Permanent Reset Confirmation Dialog
                if (showResetConfirmation) {
                    ProfileDangerResetDialog(
                        isDark = isDark,
                        subTextColor = subTextColor,
                        onConfirm = {
                            showResetConfirmation = false
                            onResetAllData()
                            Toast.makeText(context, "All data permanently deleted 🗑️", Toast.LENGTH_SHORT).show()
                            onDismissRequest()
                        },
                        onDismiss = { showResetConfirmation = false }
                    )
                }

                // Download & Export Backup with API Key Visibility & Control Dialog
                if (showExportBackupDialog) {
                    val hasAnyKeys = tempKey1.isNotBlank() || tempKey2.isNotBlank() || tempKey3.isNotBlank()
                    AlertDialog(
                        onDismissRequest = { showExportBackupDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📦", fontSize = 20.sp)
                                Text("Download & Export Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "Review your API Keys and study data before downloading your backup file:",
                                    fontSize = 12.sp,
                                    color = subTextColor
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (hasAnyKeys) Color(0xFF6366F1).copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🔑 Configured API Keys in this Backup:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = textColor)

                                        if (tempKey1.isNotBlank()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Key #1 (Primary):", fontSize = 10.5.sp, color = subTextColor)
                                                    val preview1 = if (tempKey1.length > 10) "${tempKey1.take(7)}••••${tempKey1.takeLast(4)}" else tempKey1
                                                    Text(preview1, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textColor)
                                                }
                                                if (currentlyWorkingApiKey == tempKey1 || key1Status.contains("Active") || key1Status.contains("✅")) {
                                                    Text("⚡ Working", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                                                }
                                            }
                                        }

                                        if (tempKey2.isNotBlank()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Key #2 (Backup 1):", fontSize = 10.5.sp, color = subTextColor)
                                                    val preview2 = if (tempKey2.length > 10) "${tempKey2.take(7)}••••${tempKey2.takeLast(4)}" else tempKey2
                                                    Text(preview2, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textColor)
                                                }
                                                if (currentlyWorkingApiKey == tempKey2 || key2Status.contains("Active") || key2Status.contains("✅")) {
                                                    Text("⚡ Working", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                                                }
                                            }
                                        }

                                        if (tempKey3.isNotBlank()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Key #3 (Backup 2):", fontSize = 10.5.sp, color = subTextColor)
                                                    val preview3 = if (tempKey3.length > 10) "${tempKey3.take(7)}••••${tempKey3.takeLast(4)}" else tempKey3
                                                    Text(preview3, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, color = textColor)
                                                }
                                                if (currentlyWorkingApiKey == tempKey3 || key3Status.contains("Active") || key3Status.contains("✅")) {
                                                    Text("⚡ Working", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                                                }
                                            }
                                        }

                                        if (!hasAnyKeys) {
                                            Text("No custom API keys set. (Clean / Safe)", fontSize = 11.sp, color = subTextColor)
                                        }
                                    }
                                }

                                if (hasAnyKeys) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (exportIncludeApiKeys) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.1f))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                if (exportIncludeApiKeys) "Include API Keys in Download" else "Exclude API Keys from Download",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (exportIncludeApiKeys) Color(0xFF6366F1) else Color(0xFFEF4444)
                                            )
                                            Text(
                                                if (exportIncludeApiKeys) "Keys will be saved in the downloaded file" else "Keys will be stripped before downloading",
                                                fontSize = 10.sp,
                                                color = subTextColor
                                            )
                                        }
                                        Switch(
                                            checked = exportIncludeApiKeys,
                                            onCheckedChange = { exportIncludeApiKeys = it }
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            tempKey1 = ""
                                            tempKey2 = ""
                                            tempKey3 = ""
                                            key1Status = ""
                                            key2Status = ""
                                            key3Status = ""
                                            currentlyWorkingApiKey = ""
                                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit()
                                                .putString("gemini_api_key_1", "")
                                                .putString("gemini_api_key_2", "")
                                                .putString("gemini_api_key_3", "")
                                                .putString("currently_working_api_key", "")
                                                .apply()
                                            onSaveProfile(tempName, tempClass, tempAvatarUri, "", "", "", com.example.data.GeminiModelManager.DEFAULT_MODEL)
                                            Toast.makeText(context, "All keys removed from App! 🗑️", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Remove Keys from App", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showExportBackupDialog = false
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    exportLauncher.launch("Lakshya_NEET_Backup_$timestamp.json")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("📥 Download Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExportBackupDialog = false }) {
                                Text("Cancel", color = subTextColor)
                            }
                        }
                    )
                }

                // Footer Save Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            onSaveProfile(tempName, tempClass, tempAvatarUri, tempKey1, tempKey2, tempKey3, tempModel)
                            onSelectAiProvider?.invoke(aiProvider)
                            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit().putString("ai_provider", aiProvider.name).apply()
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF6366F1))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFF38BDF8))
                                ),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Save Profile Changes 💾",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    if (showOpenRouterModelPicker) {
        OpenRouterModelPickerDialog(
            isDark = isDark,
            currentSelectedModelId = openRouterSelectedModel,
            models = openRouterModels,
            isLoading = isOpenRouterLoadingModels,
            onSelectModel = { modelId ->
                openRouterSelectedModel = modelId
                aiProvider = com.example.data.AiProvider.OPENROUTER
                context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("openrouter_selected_model", modelId)
                    .putString("ai_provider", com.example.data.AiProvider.OPENROUTER.name)
                    .apply()
                onSelectOpenRouterModel?.invoke(modelId)
                Toast.makeText(context, "Selected Model: $modelId (Global Default)", Toast.LENGTH_SHORT).show()
            },
            onRefreshLiveModels = {
                if (!isOpenRouterLoadingModels) {
                    isOpenRouterLoadingModels = true
                    coroutineScope.launch {
                        val res = com.example.data.OpenRouterManager.fetchLiveModels(context, openRouterApiKey)
                        isOpenRouterLoadingModels = false
                        res.onSuccess {
                            openRouterModels = it
                            Toast.makeText(context, "✅ Synced ${it.size} live models from OpenRouter!", Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                            Toast.makeText(context, "Failed to refresh: ${err.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onDismiss = { showOpenRouterModelPicker = false }
        )
    }

    if (showGroqModelPicker) {
        GroqModelPickerDialog(
            isDark = isDark,
            currentSelectedModelId = groqSelectedModel,
            models = groqModels,
            isLoading = isGroqLoadingModels,
            onSelectModel = { modelId ->
                groqSelectedModel = modelId
                aiProvider = com.example.data.AiProvider.GROQ
                com.example.data.GroqManager.setSelectedModel(context, modelId)
                context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString(com.example.data.GroqManager.KEY_GROQ_MODEL, modelId)
                    .putString("ai_provider", com.example.data.AiProvider.GROQ.name)
                    .apply()
                onSelectGroqModel?.invoke(modelId)
                onSelectAiProvider?.invoke(com.example.data.AiProvider.GROQ)
                showGroqModelPicker = false
                Toast.makeText(context, "Selected Model: ${modelId.substringAfterLast("/")} (Groq LPU Active ⚡)", Toast.LENGTH_SHORT).show()
            },
            onRefreshLiveModels = {
                if (!isGroqLoadingModels) {
                    isGroqLoadingModels = true
                    coroutineScope.launch {
                        val res = com.example.data.GroqManager.fetchLiveModels(context, groqApiKey)
                        isGroqLoadingModels = false
                        res.fold(
                            onSuccess = {
                                groqModels = it
                                Toast.makeText(context, "✅ Synced ${it.size} live models from Groq!", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { err ->
                                val msg = if (groqApiKey.isBlank()) {
                                    "⚠️ Please enter your Groq API Key in Settings first (starts with 'gsk_')"
                                } else {
                                    "Sync failed: ${err.message}"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            },
            onDismiss = { showGroqModelPicker = false }
        )
    }

    if (showGeminiModelPicker) {
        GeminiModelPickerDialog(
            isDark = isDark,
            currentSelectedModelId = geminiSelectedModel,
            models = geminiModels,
            isLoading = isGeminiLoadingModels,
            onSelectModel = { modelId ->
                geminiSelectedModel = modelId
                tempModel = modelId
                aiProvider = com.example.data.AiProvider.NATIVE_GEMINI
                com.example.data.GeminiModelManager.setSelectedModel(context, modelId)
                context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString(com.example.data.GeminiModelManager.KEY_SELECTED_GEMINI_MODEL, modelId)
                    .putString("selected_gemini_model", modelId)
                    .putString("ai_provider", com.example.data.AiProvider.NATIVE_GEMINI.name)
                    .apply()
                onSaveProfile(tempName, tempClass, tempAvatarUri, tempKey1, tempKey2, tempKey3, modelId)
                onSelectAiProvider?.invoke(com.example.data.AiProvider.NATIVE_GEMINI)
                showGeminiModelPicker = false
                Toast.makeText(context, "Selected Model: $modelId (Gemini Active ✦)", Toast.LENGTH_SHORT).show()
            },
            onRefreshLiveModels = {
                if (!isGeminiLoadingModels) {
                    isGeminiLoadingModels = true
                    coroutineScope.launch {
                        val res = com.example.data.GeminiModelManager.fetchLiveModels(context, tempKey1)
                        isGeminiLoadingModels = false
                        res.fold(
                            onSuccess = {
                                geminiModels = it
                                Toast.makeText(context, "✅ Synced ${it.size} live Gemini models!", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { err ->
                                val msg = if (tempKey1.isBlank()) {
                                    "⚠️ Please enter your Gemini API Key in Settings first"
                                } else {
                                    "Sync failed: ${err.message}"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            },
            onDismiss = { showGeminiModelPicker = false }
        )
    }

    if (showCloudflareModelPicker) {
        CloudflareModelPickerDialog(
            isDark = isDark,
            currentSelectedModelId = cloudflareSelectedModel,
            models = cloudflareModels,
            isLoading = isCloudflareLoadingModels,
            onSelectModel = { modelId ->
                cloudflareSelectedModel = modelId
                aiProvider = com.example.data.AiProvider.CLOUDFLARE
                com.example.data.CloudflareManager.setSelectedModel(context, modelId)
                context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString(com.example.data.CloudflareManager.KEY_CF_MODEL, modelId)
                    .putString("ai_provider", com.example.data.AiProvider.CLOUDFLARE.name)
                    .apply()
                onSelectCloudflareModel?.invoke(modelId)
                onSelectAiProvider?.invoke(com.example.data.AiProvider.CLOUDFLARE)
                showCloudflareModelPicker = false
                Toast.makeText(context, "Selected Model: ${modelId.substringAfterLast("/")} (Cloudflare Active ☁️)", Toast.LENGTH_SHORT).show()
            },
            onRefreshLiveModels = {
                if (!isCloudflareLoadingModels) {
                    isCloudflareLoadingModels = true
                    coroutineScope.launch {
                        val res = com.example.data.CloudflareManager.fetchLiveModels(context, cloudflareAccountId, cloudflareApiToken)
                        isCloudflareLoadingModels = false
                        res.fold(
                            onSuccess = {
                                cloudflareModels = it
                                Toast.makeText(context, "✅ Synced ${it.size} live models from Cloudflare!", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { err ->
                                val msg = if (cloudflareAccountId.isBlank() || cloudflareApiToken.isBlank()) {
                                    "⚠️ Please enter Cloudflare Account ID & API Token in Settings"
                                } else {
                                    "Sync failed: ${err.message}"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            },
            onDismiss = { showCloudflareModelPicker = false }
        )
    }
}

// Alias for backwards compatibility
@Composable
fun SettingsDialog(
    isDark: Boolean,
    geminiApiKey1: String,
    geminiApiKey2: String,
    geminiApiKey3: String,
    selectedGeminiModel: String,
    neetTargetMillis: Long,
    logs: List<StudyLog>,
    tests: List<MockTest>,
    dailyPractices: List<DailyPractice>,
    completedTopics: List<CompletedTopic>,
    notificationTimes: Map<String, Int> = emptyMap(),
    onUpdateNotificationTime: (String, Int) -> Unit = { _, _ -> },
    onSaveSettings: (key1: String, key2: String, key3: String, model: String) -> Unit,
    onSetDarkMode: (Boolean) -> Unit,
    onResetAllData: () -> Unit,
    onSelectOpenRouterModel: ((String) -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    DashboardProfileDialog(
        appPassword = null,
        habitPassword = null,
        onSetHabitPassword = {},
        onSetAppPassword = {},
        isDark = isDark,
        userName = "Future Doctor 👨‍⚕️",
        userClass = "Dropper",
        userAvatarUri = null,
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
        onUpdateNotificationTime = onUpdateNotificationTime,
        onSaveProfile = { name, uClass, avatarUri, k1, k2, k3, model ->
            onSaveSettings(k1, k2, k3, model)
        },
        onSetDarkMode = onSetDarkMode,
        onResetAllData = onResetAllData,
        onSelectOpenRouterModel = onSelectOpenRouterModel,
        onDismissRequest = onDismissRequest
    )
}

@Composable
fun PasswordPromptDialog(
    correctPassword: String,
    securityQuestion: String = "What is your favourite food?",
    securityAnswer: String? = null,
    title: String = "Enter Password",
    description: String = "This section is protected.",
    onSuccess: () -> Unit,
    onResetPassword: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    isDark: Boolean
) {
    val context = LocalContext.current
    val dialogBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    var input by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotAnswerInput by remember { mutableStateOf("") }
    var forgotAnswerError by remember { mutableStateOf(false) }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔑 Password Recovery", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Answer your security question to unlock or reset your password.",
                        color = subTextColor,
                        fontSize = 12.5.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0x18FFFFFF) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isDark) Color(0x336366F1) else Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Security Question:",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6366F1)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                securityQuestion,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                        }
                    }

                    OutlinedTextField(
                        value = forgotAnswerInput,
                        onValueChange = {
                            forgotAnswerInput = it
                            forgotAnswerError = false
                        },
                        label = { Text("Your Answer", color = subTextColor, fontSize = 12.sp) },
                        placeholder = { Text("Enter your answer", fontSize = 12.sp) },
                        isError = forgotAnswerError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )

                    if (forgotAnswerError) {
                        Text(
                            "Incorrect answer. Please check your answer and try again.",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val expectedAnswer = securityAnswer?.trim()
                        val isCorrect = if (expectedAnswer.isNullOrBlank()) {
                            true // No answer set, allow recovery
                        } else {
                            forgotAnswerInput.trim().equals(expectedAnswer, ignoreCase = true)
                        }

                        if (isCorrect) {
                            Toast.makeText(context, "✅ Security Question verified! Unlocked.", Toast.LENGTH_SHORT).show()
                            showForgotDialog = false
                            onResetPassword?.invoke()
                            onSuccess()
                        } else {
                            forgotAnswerError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Verify & Unlock 🔓", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Cancel", color = subTextColor)
                }
            },
            containerColor = dialogBg
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(22.dp))
                    Text(title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(description, color = subTextColor, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it; showError = false },
                        label = { Text("Password", color = subTextColor, fontSize = 12.sp) },
                        placeholder = { Text("Enter password / PIN", fontSize = 12.sp) },
                        visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    tint = subTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        isError = showError,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )
                    if (showError) {
                        Text("Incorrect password", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                forgotAnswerInput = ""
                                forgotAnswerError = false
                                showForgotDialog = true
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Forgot Password? (Security Question) 🔑",
                                color = Color(0xFF6366F1),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (input.trim() == correctPassword.trim()) {
                            onSuccess()
                        } else {
                            showError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Unlock 🔓", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel", color = subTextColor) }
            },
            containerColor = dialogBg
        )
    }
}
