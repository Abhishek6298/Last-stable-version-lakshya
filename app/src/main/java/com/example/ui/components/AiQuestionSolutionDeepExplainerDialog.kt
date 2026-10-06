package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import com.example.data.AiTestQuestion
import com.example.data.GeminiChatAssistant
import com.example.data.QuestionImageFilter
import com.example.ui.components.CbtThemeManager
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch

/**
 * Interactive Full-Screen Dialog for Deep AI Solution Derivation,
 * NCERT Concept Linking, Speed Tricks, and Live Follow-up Doubt Solving.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiQuestionSolutionDeepExplainerDialog(
    question: AiTestQuestion,
    examTitle: String = "NEET/JEE Test",
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSaveToMistakes: (() -> Unit)? = null,
    onOpenStudyTubeLens: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var aiDeepExplanation by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var userCustomDoubt by remember { mutableStateOf("") }
    var isSendingDoubt by remember { mutableStateOf(false) }
    val doubtThreads = remember { mutableStateListOf<Pair<String, String>>() }

    val dialogBg = if (isDark) Color(0xFF0B0F19) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF131A2E) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val primaryAccent = Color(0xFF6366F1)
    val emeraldColor = Color(0xFF10B981)
    val roseColor = Color(0xFFEF4444)

    // Intelligently parse question text, embedded options, user selection, correct key and solution
    val parsedDetails = remember(question) {
        var cleanQText = question.questionText
        var extractedUserAns = question.selectedOption
        var extractedKey = question.correctOption
        var extractedSolution = question.explanation

        // 1. Extract [Your Answer: Option X | Correct: Option Y] or "❌ My Answer: (B)"
        val ansMatch = Regex("""\[Your Answer:\s*(?:Option\s*)?([A-Za-z0-9]+)\s*\|\s*Correct:\s*(?:Option\s*)?([A-Za-z0-9]+)\]""").find(question.questionText)
        if (ansMatch != null) {
            val userVal = ansMatch.groupValues[1].trim()
            if (extractedUserAns == null && !userVal.equals("Unattempted", ignoreCase = true)) {
                extractedUserAns = userVal
            }
            if (extractedKey.isBlank() || extractedKey.equals("Key", ignoreCase = true)) {
                extractedKey = ansMatch.groupValues[2].trim()
            }
            cleanQText = cleanQText.replace(ansMatch.value, "").trim()
        } else {
            val myAnsMatch = Regex("""(?:My Answer|Your Answer):\s*\(?([A-D])\)?""", RegexOption.IGNORE_CASE).find(question.questionText)
            if (myAnsMatch != null && extractedUserAns == null) extractedUserAns = myAnsMatch.groupValues[1]

            val correctAnsMatch = Regex("""(?:Correct Answer|Key):\s*\(?([A-D])\)?""", RegexOption.IGNORE_CASE).find(question.questionText)
            if (correctAnsMatch != null && (extractedKey.isBlank() || extractedKey.equals("Key", ignoreCase = true))) {
                extractedKey = correctAnsMatch.groupValues[1]
            }
        }

        // 2. Extract Solution
        val solMatch = Regex("""(?:💡\s*Solution:|Master Solution:|Explanation:)\s*([\s\S]+)$""").find(question.questionText)
        if (solMatch != null) {
            if (extractedSolution.isBlank()) {
                extractedSolution = solMatch.groupValues[1].trim()
            }
            cleanQText = cleanQText.replace(solMatch.value, "").trim()
        }

        // 3. Extract Options if question.options is blank
        val opts = if (question.options.any { it.isNotBlank() }) {
            question.options
        } else {
            val optA = Regex("""(?m)^\s*[\(\[]?A[\)\]\.\:]\s*(.*?)$""").find(cleanQText)?.groupValues?.get(1)?.trim()
            val optB = Regex("""(?m)^\s*[\(\[]?B[\)\]\.\:]\s*(.*?)$""").find(cleanQText)?.groupValues?.get(1)?.trim()
            val optC = Regex("""(?m)^\s*[\(\[]?C[\)\]\.\:]\s*(.*?)$""").find(cleanQText)?.groupValues?.get(1)?.trim()
            val optD = Regex("""(?m)^\s*[\(\[]?D[\)\]\.\:]\s*(.*?)$""").find(cleanQText)?.groupValues?.get(1)?.trim()
            if (optA != null && optB != null) {
                cleanQText = cleanQText
                    .replace(Regex("""(?m)^\s*[\(\[]?A[\)\]\.\:]\s*.*?$"""), "")
                    .replace(Regex("""(?m)^\s*[\(\[]?B[\)\]\.\:]\s*.*?$"""), "")
                    .replace(Regex("""(?m)^\s*[\(\[]?C[\)\]\.\:]\s*.*?$"""), "")
                    .replace(Regex("""(?m)^\s*[\(\[]?D[\)\]\.\:]\s*.*?$"""), "")
                    .trim()
                listOf(optA, optB, optC ?: "", optD ?: "")
            } else {
                emptyList()
            }
        }

        Triple(cleanQText, opts, Triple(extractedUserAns, extractedKey, extractedSolution))
    }

    val (displayQText, displayOpts, answerInfo) = parsedDetails
    val (displayUserAns, displayKey, displaySolution) = answerInfo

    // Function to load the initial deep explanation from Gemini
    fun fetchAiDeepSolution(customQuery: String? = null) {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = GeminiChatAssistant.explainTestQuestionSolution(
                context = context,
                questionText = displayQText,
                options = displayOpts,
                userSelectedOption = displayUserAns,
                correctOption = if (displayKey.isNotBlank() && !displayKey.equals("Key", ignoreCase = true)) displayKey else question.correctOption,
                subject = question.subject,
                chapter = question.chapter,
                baseExplanation = displaySolution.ifBlank { question.explanation },
                customStudentDoubt = customQuery
            )
            isLoading = false
            result.onSuccess { text ->
                aiDeepExplanation = text
            }.onFailure { err ->
                val base = displaySolution.ifBlank { question.explanation }
                if (base.isNotBlank()) {
                    // Fallback to rich base explanation so student is never blocked
                    aiDeepExplanation = base
                } else {
                    errorMessage = err.message ?: "Failed to generate AI deep explanation."
                }
            }
        }
    }

    // Function to handle follow-up doubt prompt
    fun sendFollowUpDoubt(promptText: String) {
        if (promptText.isBlank() || isSendingDoubt) return
        val currentDoubt = promptText.trim()
        userCustomDoubt = ""
        coroutineScope.launch {
            isSendingDoubt = true
            val result = GeminiChatAssistant.explainTestQuestionSolution(
                context = context,
                questionText = displayQText,
                options = displayOpts,
                userSelectedOption = displayUserAns,
                correctOption = if (displayKey.isNotBlank() && !displayKey.equals("Key", ignoreCase = true)) displayKey else question.correctOption,
                subject = question.subject,
                chapter = question.chapter,
                baseExplanation = aiDeepExplanation ?: displaySolution.ifBlank { question.explanation },
                customStudentDoubt = currentDoubt
            )
            isSendingDoubt = false
            result.onSuccess { reply ->
                doubtThreads.add(currentDoubt to reply)
            }.onFailure { err ->
                val base = aiDeepExplanation ?: displaySolution.ifBlank { question.explanation }
                val fallbackAnswer = if (base.isNotBlank()) {
                    "💡 **Concept Breakdown**:\n\n$base\n\n*(Note: Custom Lakshya AI Key is required in Settings for interactive real-time live follow-up generation)*"
                } else {
                    "⚠️ ${err.message ?: "Please configure your Lakshya AI Key in Settings to chat with the AI Master Tutor."}"
                }
                doubtThreads.add(currentDoubt to fallbackAnswer)
            }
        }
    }

    // Auto-fetch if not yet loaded
    LaunchedEffect(question.id) {
        if (aiDeepExplanation == null && !isLoading) {
            fetchAiDeepSolution()
        }
    }

    val quickActionPrompts = listOf(
        "⚡ 10-Sec Speed Shortcut" to "What is the fastest 10-second mental shortcut or Vedic calculation trick to solve this without writing full equations?",
        "🗣️ Explain in Hindi / Hinglish" to "Please explain this question, its core formula, and step-by-step solution in simple conversational Hinglish (Hindi + English).",
        "📖 NCERT Page & Line Link" to "Which exact Class 11/12 NCERT Biology/Physics/Chemistry chapter and page/topic is this question based on? Quote the exact concept.",
        "❌ Why are Options A, B, C, D Wrong?" to "Analyze each of the 4 options one by one. Explain why the correct option works and what specific misconception makes each wrong option incorrect.",
        "🎯 Similar Practice Question" to "Create 1 new high-probability NEET/JEE level numerical/conceptual question based on this exact same formula with step-by-step solution."
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        val density = LocalDensity.current
        var measuredKeyboardHeightDp by remember { mutableStateOf(0.dp) }

        DisposableEffect(view, density) {
            var parent = view.parent
            while (parent != null && parent !is DialogWindowProvider) {
                parent = parent.parent
            }
            val dialogWindow = (parent as? DialogWindowProvider)?.window
            dialogWindow?.let { win ->
                win.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                win.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                WindowCompat.setDecorFitsSystemWindows(win, false)
                ViewCompat.setOnApplyWindowInsetsListener(win.decorView) { _, insets ->
                    ViewCompat.dispatchApplyWindowInsets(view, insets)
                    insets
                }
                view.requestApplyInsets()
            }

            val targetDecorView = dialogWindow?.decorView ?: view.rootView
            val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                val rect = Rect()
                targetDecorView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = targetDecorView.rootView.height
                val keypadHeight = screenHeight - rect.bottom
                val isKeyboard = keypadHeight > screenHeight * 0.15
                val newHeightDp = if (isKeyboard) {
                    with(density) { keypadHeight.toDp() }
                } else {
                    0.dp
                }
                if (measuredKeyboardHeightDp != newHeightDp) {
                    measuredKeyboardHeightDp = newHeightDp
                }
            }
            targetDecorView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
            onDispose {
                if (targetDecorView.viewTreeObserver.isAlive) {
                    targetDecorView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
                }
            }
        }

        val scrollState = rememberScrollState()

        val imeInsets = WindowInsets.ime
        val navBarInsets = WindowInsets.navigationBars
        val imeBottomDp = imeInsets.asPaddingValues().calculateBottomPadding()
        val navBottomDp = navBarInsets.asPaddingValues().calculateBottomPadding()
        
        // Effective keyboard height from either Compose insets OR direct display frame measurement
        val effectiveKeyboardHeight = maxOf(imeBottomDp, measuredKeyboardHeightDp)
        val isKeyboardOpen = WindowInsets.isImeVisible || effectiveKeyboardHeight > 20.dp

        // Perfectly lifted above the navigation bar when closed, and immediately above the keyboard when opened
        val dynamicBottomPadding = if (isKeyboardOpen) {
            effectiveKeyboardHeight + 8.dp
        } else {
            maxOf(navBottomDp, 32.dp) + 8.dp
        }

        val animatedBottomPadding by animateDpAsState(
            targetValue = dynamicBottomPadding,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "bottomBarKeyboardPadding"
        )

        // Auto-scroll down when new doubt message is added or keyboard opens
        LaunchedEffect(doubtThreads.size, isSendingDoubt) {
            if (doubtThreads.isNotEmpty() || isSendingDoubt) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
        LaunchedEffect(isKeyboardOpen) {
            if (isKeyboardOpen) {
                kotlinx.coroutines.delay(120)
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Surface(
                    color = if (isDark) Color(0xFF17212B) else Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF2AABEE), Color(0xFF6366F1))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    "✨ AI Master Solution & Doubts",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                val subtitleText = buildString {
                                    if (question.subject.isNotBlank()) append(question.subject)
                                    if (question.id > 0) {
                                        if (isNotEmpty()) append(" • ")
                                        append("Q${question.id}")
                                    }
                                    if (displayKey.isNotBlank() && !displayKey.equals("Key", ignoreCase = true)) {
                                        if (isNotEmpty()) append(" • ")
                                        append("Key: $displayKey")
                                    }
                                }
                                if (subtitleText.isNotBlank()) {
                                    Text(
                                        subtitleText,
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFF64B5F6) else Color(0xFF0284C7),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            bottomBar = {
                // Sleek Telegram-Style Chat Input Bar with Chips
                Surface(
                    color = if (isDark) Color(0xFF17212B) else Color.White,
                    shadowElevation = 16.dp,
                    border = BorderStroke(1.dp, if (isDark) Color(0x2AFFFFFF) else Color(0x1A000000)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 8.dp)
                            .padding(bottom = animatedBottomPadding)
                    ) {
                        // Quick prompt suggestion chips (Telegram pill carousel style)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            items(quickActionPrompts, key = { it.first }) { (label, prompt) ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) Color(0xFF242F3D) else Color(0xFFEEF6FC),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDark) Color(0x402AABEE) else Color(0x662AABEE)
                                    ),
                                    modifier = Modifier.clickable {
                                        sendFollowUpDoubt(prompt)
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFF64B5F6) else Color(0xFF0284C7),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // Telegram Input row: Pill input capsule + Circular Send Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pill text box
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = if (isDark) Color(0xFF242F3D) else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0x26FFFFFF) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.QuestionAnswer,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF64B5F6) else Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedTextField(
                                        value = userCustomDoubt,
                                        onValueChange = { userCustomDoubt = it },
                                        placeholder = {
                                            Text(
                                                "Ask doubt, formula, or trick...",
                                                fontSize = 13.sp,
                                                color = subTextColor
                                            )
                                        },
                                        singleLine = false,
                                        maxLines = 4,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                        keyboardActions = KeyboardActions(onSend = {
                                            sendFollowUpDoubt(userCustomDoubt)
                                        }),
                                        modifier = Modifier.weight(1f),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = textColor),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            cursorColor = Color(0xFF2AABEE)
                                        )
                                    )

                                    if (userCustomDoubt.isNotBlank()) {
                                        IconButton(
                                            onClick = { userCustomDoubt = "" },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = subTextColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Telegram Circular Send Button
                            val canSend = userCustomDoubt.isNotBlank() && !isSendingDoubt
                            IconButton(
                                onClick = { sendFollowUpDoubt(userCustomDoubt) },
                                enabled = canSend,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (canSend) Brush.linearGradient(
                                            listOf(Color(0xFF2AABEE), Color(0xFF229ED9))
                                        ) else Brush.linearGradient(
                                            listOf(Color(0x33888888), Color(0x33888888))
                                        )
                                    )
                            ) {
                                if (isSendingDoubt) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send Doubt",
                                        tint = if (canSend) Color.White else subTextColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = dialogBg
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. QUESTION SUMMARY CARD WITH MATHJAX
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = primaryAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "${question.subject.uppercase()} • Q${question.id}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = primaryAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                )
                            }

                            val isUserCorrect = displayUserAns != null && displayUserAns.equals(displayKey, ignoreCase = true)
                            val isUserWrong = displayUserAns != null && !displayUserAns.equals(displayKey, ignoreCase = true)

                            Surface(
                                color = when {
                                    isUserCorrect -> emeraldColor.copy(alpha = 0.15f)
                                    isUserWrong -> roseColor.copy(alpha = 0.15f)
                                    else -> Color.Gray.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    when {
                                        isUserCorrect -> "✅ CORRECT"
                                        isUserWrong -> "❌ WRONG (Ans: $displayUserAns)"
                                        else -> "⚪ UNATTEMPTED"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when {
                                        isUserCorrect -> emeraldColor
                                        isUserWrong -> roseColor
                                        else -> subTextColor
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Full MathJax Question Text
                        MathJaxView(
                            text = displayQText,
                            isDark = isDark,
                            fontSize = 14.sp
                        )

                        // Real diagram / cropped image from OCR test paper if present
                        val isAdOrInvalidDiag = QuestionImageFilter.isAdOrPromotionalImage(question.imageUrl)
                        if (!question.imageUrl.isNullOrBlank() && !isAdOrInvalidDiag) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val dialogContext = LocalContext.current
                            var invertDiag by remember(question.imageUrl) {
                                val prefs = dialogContext.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                                mutableStateOf(prefs.getBoolean("cbt_auto_invert_images", true))
                            }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = question.imageUrl,
                                    contentDescription = question.diagramLabel ?: "Question Diagram",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit,
                                    colorFilter = if (invertDiag) ColorFilter.colorMatrix(CbtThemeManager.InvertColorMatrix) else null
                                )
                                Surface(
                                    onClick = { invertDiag = !invertDiag },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (invertDiag) Color(0xFF6366F1).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            if (invertDiag) "🌓 Anti-Glare ON" else "☀️ Normal",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Render options with MathJax only if options exist
                        if (displayOpts.any { it.isNotBlank() }) {
                            Spacer(modifier = Modifier.height(12.dp))

                            displayOpts.forEachIndexed { idx, opt ->
                                if (opt.isNotBlank()) {
                                    val optLetter = ('A' + idx).toString()
                                    val isThisCorrect = optLetter.equals(displayKey, ignoreCase = true)
                                    val isThisUserChoice = optLetter.equals(displayUserAns, ignoreCase = true)

                                    val optBg = when {
                                        isThisCorrect -> emeraldColor.copy(alpha = if (isDark) 0.2f else 0.12f)
                                        isThisUserChoice -> roseColor.copy(alpha = if (isDark) 0.2f else 0.12f)
                                        else -> if (isDark) Color(0x0AFFFFFF) else Color(0xFFF1F5F9)
                                    }
                                    val optBorder = when {
                                        isThisCorrect -> emeraldColor
                                        isThisUserChoice -> roseColor
                                        else -> if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = optBg,
                                        border = BorderStroke(1.dp, optBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isThisCorrect) emeraldColor else if (isThisUserChoice) roseColor else primaryAccent.copy(alpha = 0.15f),
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        optLetter,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isThisCorrect || isThisUserChoice) Color.White else primaryAccent
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(modifier = Modifier.weight(1f)) {
                                                MathJaxView(text = opt, isDark = isDark, fontSize = 12.sp)
                                            }
                                            if (isThisCorrect) {
                                                Text("✓ KEY", fontSize = 9.sp, fontWeight = FontWeight.Black, color = emeraldColor)
                                            } else if (isThisUserChoice) {
                                                Text("YOU", fontSize = 9.sp, fontWeight = FontWeight.Black, color = roseColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. AI DEEP EXPLANATION CARD WITH FULL MATHJAX & MARKDOWN
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.5.dp, primaryAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(18.dp))
                                Text(
                                    "Lakshya AI Deep Concept & Derivation",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = primaryAccent
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val currentExplanationText = aiDeepExplanation ?: displaySolution.ifBlank { question.explanation }
                                if (currentExplanationText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("AI Solution", currentExplanationText)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Solution copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = subTextColor, modifier = Modifier.size(16.dp))
                                    }
                                }

                                if (onSaveToMistakes != null) {
                                    IconButton(
                                        onClick = onSaveToMistakes,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Save to Notebook", tint = primaryAccent, modifier = Modifier.size(16.dp))
                                    }
                                }

                                if (onOpenStudyTubeLens != null) {
                                    IconButton(
                                        onClick = onOpenStudyTubeLens,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.PlayCircle, contentDescription = "Watch Video Solution in StudyTube Lens", tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                                    }
                                }

                                IconButton(
                                    onClick = { fetchAiDeepSolution() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = subTextColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
                        )

                        val solutionToShow = aiDeepExplanation ?: displaySolution.ifBlank { question.explanation }

                        when {
                            isLoading && solutionToShow.isBlank() -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = primaryAccent,
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        "Deriving Step-by-Step AI Solution with MathJax & LaTeX...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = subTextColor
                                    )
                                }
                            }

                            errorMessage != null && solutionToShow.isBlank() -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        "⚠️ $errorMessage",
                                        fontSize = 12.sp,
                                        color = roseColor,
                                        lineHeight = 16.sp
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { fetchAiDeepSolution() },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text("Retry with Lakshya AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        // Provide offline core concept derivation if available
                                        val basicConcept = when {
                                            question.subject.contains("Physic", ignoreCase = true) -> "💡 **Fundamental Physics Principle**:\n\n$$\\vec{F}_{net} = m \\vec{a}$$\n\nAlways draw the Free Body Diagram (FBD), resolve forces along perpendicular coordinate axes, and apply Newton's Laws."
                                            question.subject.contains("Chemi", ignoreCase = true) -> "💡 **Fundamental Chemistry Principle**:\n\nApply stoichiometry, mole concept \$n = \\frac{w}{M}\$, or electronic configuration rules for precise answers."
                                            else -> "💡 **NCERT Concept**:\n\nRefer to NCERT textbook concepts, definitions, and diagrams for this topic."
                                        }
                                        OutlinedButton(
                                            onClick = { aiDeepExplanation = basicConcept },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text("View Core Concept", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            solutionToShow.isNotBlank() -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (isLoading) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(primaryAccent.copy(alpha = 0.1f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = primaryAccent, strokeWidth = 1.5.dp)
                                            Text("Enhancing with Lakshya AI deep derivation & tricks...", fontSize = 10.sp, color = primaryAccent, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    MathJaxView(
                                        text = solutionToShow,
                                        isDark = isDark,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            else -> {
                                Text(
                                    "No solution data available. Tap refresh above to generate AI derivation.",
                                    fontSize = 12.sp,
                                    color = subTextColor
                                )
                            }
                        }
                    }
                }

                if (onOpenStudyTubeLens != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenStudyTubeLens,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, Color(0xFFEF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDark) Color(0xFF1E1015) else Color(0xFFFEF2F2),
                            contentColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (question.hasImage || !question.imageUrl.isNullOrBlank()) "📺 StudyTube Lens: Watch Video Solution (With Diagram)"
                            else "📺 StudyTube Lens: Watch Video Solution (PW/Doubtnut)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                // 3. INTERACTIVE FOLLOW-UP DOUBTS THREAD (Telegram Style Chat Bubbles)
                if (doubtThreads.isNotEmpty() || isSendingDoubt) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                Icons.Default.Forum,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF64B5F6) else Color(0xFF0284C7),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Live Doubt Clarification Thread",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                        Text(
                            "${doubtThreads.size} messages",
                            fontSize = 11.sp,
                            color = subTextColor
                        )
                    }

                    doubtThreads.forEachIndexed { index, (userQ, aiAns) ->
                        // Student Doubt Bubble (Telegram Sent Message - Right Aligned)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 4.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp
                                ),
                                color = if (isDark) Color(0xFF2B5278) else Color(0xFFE3EEF8),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0x3364B5F6) else Color(0x330284C7)
                                ),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.88f)
                                    .padding(start = 24.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "You",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF90CAF9) else Color(0xFF0284C7)
                                        )
                                        Text(
                                            "✓✓",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isDark) Color(0xFF90CAF9) else Color(0xFF0284C7)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        userQ,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = textColor
                                    )
                                }
                            }
                        }

                        // AI Clarification Bubble (Telegram Received Message - Left Aligned)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF2AABEE), Color(0xFF6366F1))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp
                                ),
                                color = if (isDark) Color(0xFF1E2C3A) else Color.White,
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) Color(0x332AABEE) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = 3.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "AI Master Tutor",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF64B5F6) else Color(0xFF0284C7)
                                        )
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("AI Reply", aiAns)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Copied AI reply! 📋", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = subTextColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    MathJaxView(text = aiAns, isDark = isDark, fontSize = 12.5.sp)
                                }
                            }
                        }
                    }

                    // Active Typing indicator if sending
                    if (isSendingDoubt) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2AABEE).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color(0xFF2AABEE),
                                    strokeWidth = 2.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF1E2C3A) else Color(0xFFF1F5F9),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    "AI Tutor is thinking & deriving solution...",
                                    fontSize = 11.5.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = if (isDark) Color(0xFF90CAF9) else Color(0xFF0284C7),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
