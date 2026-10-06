package com.example.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.launch
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import android.Manifest
import java.io.File
import com.example.ui.components.ImageCropperDialog
import coil.compose.AsyncImage
import com.example.data.AiProvider
import com.example.data.ChatMessageEntity
import com.example.data.ExamCategory
import com.example.data.ExamSyllabusDatabase
import com.example.ui.AppViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.MathJaxView
import com.example.ui.components.ChatMessageRenderer
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.QuickAiEngineSwitchDialog
import com.example.ui.components.OpenRouterModelPickerDialog
import com.example.ui.components.GroqModelPickerDialog
import com.example.ui.components.GeminiModelPickerDialog
import com.example.ui.components.CloudflareModelPickerDialog
import com.example.util.MathFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val activeExamGoal by viewModel.activeExamGoal.collectAsStateWithLifecycle()
    val currentAiProvider by viewModel.aiProvider.collectAsStateWithLifecycle()
    val openRouterSelectedModel by viewModel.openRouterSelectedModel.collectAsStateWithLifecycle()
    val openRouterModels by viewModel.openRouterModels.collectAsStateWithLifecycle()
    val isOpenRouterLoadingModels by viewModel.isOpenRouterLoadingModels.collectAsStateWithLifecycle()
    val groqSelectedModel by viewModel.groqSelectedModel.collectAsStateWithLifecycle()
    val groqModels by viewModel.groqModels.collectAsStateWithLifecycle()
    val isGroqLoadingModels by viewModel.isGroqLoadingModels.collectAsStateWithLifecycle()
    val cloudflareSelectedModel by viewModel.cloudflareSelectedModel.collectAsStateWithLifecycle()
    val cloudflareModels by viewModel.cloudflareModels.collectAsStateWithLifecycle()
    val isCloudflareLoadingModels by viewModel.isCloudflareLoadingModels.collectAsStateWithLifecycle()
    var showQuickAiEngineDialog by remember { mutableStateOf(false) }
    var showOpenRouterModelPickerDialog by remember { mutableStateOf(false) }
    var showGroqModelPickerDialog by remember { mutableStateOf(false) }
    var showCloudflareModelPickerDialog by remember { mutableStateOf(false) }
    val availableSubjects = remember(activeExamGoal) {
        val list = ExamSyllabusDatabase.getSubjectsFor(activeExamGoal)
        if (list.isNotEmpty()) list else listOf("Physics", "Chemistry", "Biology")
    }

    var errorSubject by remember { mutableStateOf(availableSubjects.firstOrNull() ?: "Physics") }
    var chatType by remember { mutableStateOf("doubt") } // "error" or "doubt"
    var chatInput by remember { mutableStateOf("") }

    LaunchedEffect(activeExamGoal) {
        if (errorSubject !in availableSubjects) {
            errorSubject = availableSubjects.firstOrNull() ?: "Physics"
        }
    }

    // Selected attachment state
    var attachedUri by remember { mutableStateOf<String?>(null) }
    var attachedType by remember { mutableStateOf<String?>(null) } // "image", "pdf", "table"
    var attachedName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.pendingAiBotImageUri?.let { uri ->
            attachedUri = uri
            attachedType = "image"
            attachedName = "Smart Select Screenshot"
            viewModel.pendingAiBotImageUri = null
        }
        viewModel.pendingAiBotSubject?.let { subject ->
            val matched = availableSubjects.firstOrNull { it.equals(subject, ignoreCase = true) }
                ?: availableSubjects.firstOrNull { subject.contains(it, ignoreCase = true) || it.contains(subject, ignoreCase = true) }
            if (matched != null) {
                errorSubject = matched
            }
            viewModel.pendingAiBotSubject = null
        }
    }

    // Dialog & Popup States
    var selectedMsgForMenu by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var showLongPressMenu by remember { mutableStateOf(false) }

    var editingMsg by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editTextValue by remember { mutableStateOf("") }

    var showTableCreatorDialog by remember { mutableStateOf(false) }
    var fullImagePreviewUri by remember { mutableStateOf<String?>(null) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var imageUriToCrop by remember { mutableStateOf<Uri?>(null) }
    var showClearChatDialog by remember { mutableStateOf(false) }

    // Intercept back presses when preview or dialog is open, or navigate back
    BackHandler(enabled = true) {
        when {
            showClearChatDialog -> showClearChatDialog = false
            imageUriToCrop != null -> imageUriToCrop = null
            fullImagePreviewUri != null -> fullImagePreviewUri = null
            showEditDialog -> showEditDialog = false
            showTableCreatorDialog -> showTableCreatorDialog = false
            showLongPressMenu -> showLongPressMenu = false
            else -> onNavigateBack()
        }
    }

    // Camera Capture Launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraPhotoUri != null) {
            imageUriToCrop = cameraPhotoUri
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val tempFile = File(context.cacheDir, "camera_doubt_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                cameraPhotoUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to click question photos", Toast.LENGTH_SHORT).show()
        }
    }

    val launchCamera: () -> Unit = {
        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        )
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                val tempFile = File(context.cacheDir, "camera_doubt_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                cameraPhotoUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Activity launchers for Attachments
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUriToCrop = it
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            attachedUri = it.toString()
            attachedType = "pdf"
            attachedName = it.lastPathSegment ?: "DoubtDoc.pdf"
        }
    }

    // Voice / Speech Recognition Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                chatInput = if (chatInput.isBlank()) spokenText else "$chatInput $spokenText"
            }
        }
    }

    fun startVoiceInput() {
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak your doubt or question...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice recognition not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    // Text To Speech Setup for AI Study Bot Solution Audio Readout
    var ttsInstance by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    var speakingMsgId by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(context) {
        var tts: android.speech.tts.TextToSpeech? = null
        try {
            tts = android.speech.tts.TextToSpeech(context) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    try {
                        tts?.language = Locale("en", "IN")
                    } catch (_: Exception) {}
                }
            }
            ttsInstance = tts
        } catch (_: Exception) {}

        onDispose {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (_: Exception) {}
        }
    }

    val onToggleSpeak: (ChatMessageEntity) -> Unit = { msg ->
        if (speakingMsgId == msg.id) {
            try { ttsInstance?.stop() } catch (_: Exception) {}
            speakingMsgId = null
        } else {
            try {
                ttsInstance?.stop()
                speakingMsgId = msg.id
                val cleanText = msg.text
                    .replace(Regex("""[*_`#$>]"""), "")
                    .replace(Regex("""\[!.*?\]"""), "")
                    .take(1200)
                ttsInstance?.speak(cleanText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "MSG_${msg.id}")
            } catch (e: Exception) {
                Toast.makeText(context, "Could not start audio readout", Toast.LENGTH_SHORT).show()
                speakingMsgId = null
            }
        }
    }

    // Collect messages from Room DB via ViewModel
    val messagesFlow = remember(errorSubject, chatType) {
        viewModel.getChatMessages(errorSubject, chatType)
    }
    val messages by messagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isAiGenerating by viewModel.isAiGenerating.collectAsStateWithLifecycle(initialValue = false)
    val selectedGeminiModel by viewModel.selectedGeminiModel.collectAsStateWithLifecycle()
    val geminiQuotaUsage by viewModel.geminiQuotaUsage.collectAsStateWithLifecycle()
    val showGeminiQuotaBadge by viewModel.showGeminiQuotaBadge.collectAsStateWithLifecycle()
    var showGeminiQuotaDialog by remember { mutableStateOf(false) }
    val geminiModels by viewModel.geminiModels.collectAsStateWithLifecycle()
    val isGeminiLoadingModels by viewModel.isGeminiLoadingModels.collectAsStateWithLifecycle()
    var showGeminiModelPickerDialog by remember { mutableStateOf(false) }
    var showModelDropdown by remember { mutableStateOf(false) }

    val activeModelLabel = when (currentAiProvider) {
        AiProvider.OPENROUTER -> "🌐 " + openRouterSelectedModel.substringAfterLast("/")
        AiProvider.GROQ -> "⚡ " + groqSelectedModel.substringAfterLast("/")
        AiProvider.CLOUDFLARE -> "☁️ " + cloudflareSelectedModel.substringAfterLast("/")
        AiProvider.NATIVE_GEMINI -> "✦ " + selectedGeminiModel.substringAfterLast("/")
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isScrolledUp by remember {
        derivedStateOf {
            listState.canScrollForward
        }
    }

    // Auto-scroll on new messages to the exact last item
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val total = listState.layoutInfo.totalItemsCount
            val targetIdx = (total - 1).coerceAtLeast(0)
            try {
                listState.animateScrollToItem(targetIdx)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                try { listState.scrollToItem(targetIdx) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) {}
            }
        }
    }

    val isDark = isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val cardBorder = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
    val subjectAccentColor = when (errorSubject) {
        "Physics" -> Color(0xFF6366F1)
        "Chemistry" -> Color(0xFF10B981)
        "Botany" -> Color(0xFF14B8A6)
        else -> Color(0xFFEC4899)
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing)
                .padding(
                    start = if (isLandscape) 10.dp else 4.dp,
                    end = if (isLandscape) 10.dp else 4.dp,
                    top = if (isLandscape) 2.dp else 4.dp,
                    bottom = if (isLandscape) 4.dp else 6.dp
                )
        ) {
            if (isLandscape) {
                // ==================== 1. LANDSCAPE CONSOLIDATED TOP BAR ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000))
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "AI Study Bot",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Dynamic AI Engine & Model Selector Badge (Gemini vs OpenRouter vs Groq vs Cloudflare)
                    val engineBadgeColor = when (currentAiProvider) {
                        AiProvider.OPENROUTER -> Color(0xFF10B981)
                        AiProvider.GROQ -> Color(0xFFF97316)
                        AiProvider.CLOUDFLARE -> Color(0xFFF6821F)
                        AiProvider.NATIVE_GEMINI -> Color(0xFF6366F1)
                    }
                    val engineBadgeText = when (currentAiProvider) {
                        AiProvider.OPENROUTER -> "🌐 " + openRouterSelectedModel.substringAfterLast("/").take(16)
                        AiProvider.GROQ -> "⚡ " + groqSelectedModel.substringAfterLast("/").take(16)
                        AiProvider.CLOUDFLARE -> "☁️ " + cloudflareSelectedModel.substringAfterLast("/").take(16)
                        AiProvider.NATIVE_GEMINI -> "✦ " + selectedGeminiModel.substringAfterLast("/").take(16)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = engineBadgeColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, engineBadgeColor.copy(alpha = 0.75f)),
                        modifier = Modifier.clickable { showQuickAiEngineDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(engineBadgeColor)
                            )
                            Text(
                                text = engineBadgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else textColor
                            )
                            Text(
                                text = "▾",
                                fontSize = 8.5.sp,
                                color = engineBadgeColor
                            )
                        }
                    }

                    if (currentAiProvider == AiProvider.NATIVE_GEMINI && showGeminiQuotaBadge) {
                        Spacer(modifier = Modifier.width(4.dp))
                        com.example.ui.components.GeminiQuotaRadarPill(
                            quotaUsage = geminiQuotaUsage,
                            onClick = { showGeminiQuotaDialog = true }
                        )
                    }

                    if (isAiGenerating) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(8.dp),
                                    strokeWidth = 1.2.dp,
                                    color = Color(0xFFA5B4FC)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "Reasoning...",
                                    fontSize = 9.sp,
                                    color = Color(0xFFA5B4FC),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Compact Mode Selector in Landscape
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (chatType == "doubt") Color(0xFF6366F1).copy(alpha = 0.25f) else (if (isDark) Color(0x0EFFFFFF) else Color(0xFFF1F5F9)),
                            border = BorderStroke(1.dp, if (chatType == "doubt") Color(0xFF818CF8) else cardBorder),
                            modifier = Modifier.clickable { chatType = "doubt" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (chatType == "doubt") Color(0xFFA5B4FC) else subTextColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Doubt Solver",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (chatType == "doubt") textColor else subTextColor
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (chatType == "error") Color(0xFFF43F5E).copy(alpha = 0.25f) else (if (isDark) Color(0x0EFFFFFF) else Color(0xFFF1F5F9)),
                            border = BorderStroke(1.dp, if (chatType == "error") Color(0xFFFB7185) else cardBorder),
                            modifier = Modifier.clickable { chatType = "error" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (chatType == "error") Color(0xFFFB7185) else subTextColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Mistake Diagnosis",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (chatType == "error") textColor else subTextColor
                                )
                            }
                        }

                        // Clear All Chat Icon Button (Landscape)
                        IconButton(
                            onClick = { showClearChatDialog = true },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear All Chat",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                // ==================== 1. PORTRAIT FUTURISTIC HEADER ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000))
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "AI Doubt & Error Solver",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF6366F1).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.75f)),
                                    modifier = Modifier.clickable { showModelDropdown = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF38BDF8))
                                        )
                                        Text(
                                            text = activeModelLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF38BDF8)
                                        )
                                        Text(
                                            text = "▾",
                                            fontSize = 9.sp,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showModelDropdown,
                                    onDismissRequest = { showModelDropdown = false },
                                    modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                                ) {
                                    // Option 1: Native Gemini
                                    val isGemini = currentAiProvider == AiProvider.NATIVE_GEMINI
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        text = "✦ Gemini: ${selectedGeminiModel.substringAfterLast("/")}",
                                                        fontWeight = if (isGemini) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isGemini) Color(0xFF38BDF8) else textColor,
                                                        fontSize = 12.sp
                                                    )
                                                    if (isGemini) {
                                                        Text("(ACTIVE)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF38BDF8))
                                                    }
                                                }
                                                Text(
                                                    text = "Tap to pick Gemini 3.8 Flash, 3.5, Pro & Lite models",
                                                    fontSize = 10.sp,
                                                    color = subTextColor
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setAiProvider(AiProvider.NATIVE_GEMINI)
                                            showModelDropdown = false
                                            showGeminiModelPickerDialog = true
                                        }
                                    )

                                    // Option 2: OpenRouter AI Hub
                                    val isOpenRouter = currentAiProvider == AiProvider.OPENROUTER
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        text = "🌐 OpenRouter: ${openRouterSelectedModel.substringAfterLast("/")}",
                                                        fontWeight = if (isOpenRouter) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isOpenRouter) Color(0xFF10B981) else textColor,
                                                        fontSize = 12.sp
                                                    )
                                                    if (isOpenRouter) {
                                                        Text("(ACTIVE)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                                                    }
                                                }
                                                Text(
                                                    text = "300+ Live Models • Tap to pick model",
                                                    fontSize = 10.sp,
                                                    color = subTextColor
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setAiProvider(AiProvider.OPENROUTER)
                                            showModelDropdown = false
                                            showOpenRouterModelPickerDialog = true
                                        }
                                    )

                                    // Option 3: Groq LPU Engine
                                    val isGroq = currentAiProvider == AiProvider.GROQ
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        text = "⚡ Groq LPU: ${groqSelectedModel.substringAfterLast("/")}",
                                                        fontWeight = if (isGroq) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isGroq) Color(0xFFF97316) else textColor,
                                                        fontSize = 12.sp
                                                    )
                                                    if (isGroq) {
                                                        Text("(ACTIVE)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFF97316))
                                                    }
                                                }
                                                Text(
                                                    text = "500-800 tps • Ultra-fast LPU & Vision OCR",
                                                    fontSize = 10.sp,
                                                    color = subTextColor
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setAiProvider(AiProvider.GROQ)
                                            showModelDropdown = false
                                            showGroqModelPickerDialog = true
                                        }
                                    )

                                    // Option 4: Cloudflare Workers AI
                                    val isCloudflare = currentAiProvider == AiProvider.CLOUDFLARE
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        text = "☁️ Cloudflare: ${cloudflareSelectedModel.substringAfterLast("/")}",
                                                        fontWeight = if (isCloudflare) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isCloudflare) Color(0xFFF6821F) else textColor,
                                                        fontSize = 12.sp
                                                    )
                                                    if (isCloudflare) {
                                                        Text("(ACTIVE)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFF6821F))
                                                    }
                                                }
                                                Text(
                                                    text = "10k Free Daily Neurons • Serverless Edge GPUs",
                                                    fontSize = 10.sp,
                                                    color = subTextColor
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setAiProvider(AiProvider.CLOUDFLARE)
                                            showModelDropdown = false
                                            showCloudflareModelPickerDialog = true
                                        }
                                    )
                                }
                            }

                            if (isAiGenerating) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF6366F1).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(10.dp),
                                            strokeWidth = 1.5.dp,
                                            color = Color(0xFFA5B4FC)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Reasoning...",
                                            fontSize = 10.sp,
                                            color = Color(0xFFA5B4FC),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Instant 24/7 step-by-step master breakdown & trap alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Clear All Chat Icon Button (Portrait Top Bar)
                    IconButton(
                        onClick = { showClearChatDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear All Chat",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            // ==================== 2. HORIZONTALLY SCROLLABLE SUBJECT ROW ====================
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isLandscape) 8.dp else 16.dp, vertical = if (isLandscape) 2.dp else 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableSubjects) { subj ->
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
                        subj.contains("Physics", ignoreCase = true) -> "⚛️"
                        subj.contains("Chemistry", ignoreCase = true) -> "🧪"
                        subj.contains("Botany", ignoreCase = true) -> "🌿"
                        subj.contains("Zoology", ignoreCase = true) || subj.contains("Biology", ignoreCase = true) -> "🧬"
                        else -> "📖"
                    }
                    val color = when {
                        subj.contains("Physics", ignoreCase = true) || subj.contains("Fluid", ignoreCase = true) -> Color(0xFF6366F1)
                        subj.contains("Chemistry", ignoreCase = true) || subj.contains("Tech", ignoreCase = true) -> Color(0xFF10B981)
                        subj.contains("Heat", ignoreCase = true) || subj.contains("Thermo", ignoreCase = true) -> Color(0xFFF97316)
                        subj.contains("Mass", ignoreCase = true) || subj.contains("CRE", ignoreCase = true) -> Color(0xFF8B5CF6)
                        subj.contains("Control", ignoreCase = true) || subj.contains("Economics", ignoreCase = true) -> Color(0xFF0284C7)
                        subj.contains("Math", ignoreCase = true) || subj.contains("Aptitude", ignoreCase = true) -> Color(0xFFEC4899)
                        subj.contains("Botany", ignoreCase = true) -> Color(0xFF14B8A6)
                        else -> Color(0xFFEC4899)
                    }

                    val isSelected = errorSubject.equals(subj, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) color.copy(alpha = 0.2f) else (if (isDark) Color(0x0EFFFFFF) else Color(0xFFF1F5F9)),
                        border = BorderStroke(1.2.dp, if (isSelected) color else cardBorder),
                        modifier = Modifier.clickable { errorSubject = subj }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (isLandscape) 5.dp else 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(emoji, fontSize = if (isLandscape) 12.sp else 13.sp)
                            Text(
                                text = subj,
                                fontSize = if (isLandscape) 11.5.sp else 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) textColor else subTextColor
                            )
                        }
                    }
                }
            }

            // ==================== 3. PORTRAIT DUAL-MODE SELECTOR ====================
            if (!isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Doubt Solver Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (chatType == "doubt") Color(0xFF6366F1).copy(alpha = 0.2f) else (if (isDark) Color(0x0EFFFFFF) else Color(0xFFF1F5F9)),
                        border = BorderStroke(1.2.dp, if (chatType == "doubt") Color(0xFF818CF8) else cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { chatType = "doubt" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (chatType == "doubt") Color(0xFFA5B4FC) else subTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "⚡ Instant Doubt Solver",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (chatType == "doubt") textColor else subTextColor
                            )
                        }
                    }

                    // Error Diagnosis Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (chatType == "error") Color(0xFFF43F5E).copy(alpha = 0.2f) else (if (isDark) Color(0x0EFFFFFF) else Color(0xFFF1F5F9)),
                        border = BorderStroke(1.2.dp, if (chatType == "error") Color(0xFFFB7185) else cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { chatType = "error" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (chatType == "error") Color(0xFFFB7185) else subTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "⚠️ Mistake Diagnosis",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (chatType == "error") textColor else subTextColor
                            )
                        }
                    }
                }
            }

            // ==================== 4. MESSAGES CHAT CONTAINER ====================
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .weight(1f)
                    .border(
                        1.dp,
                        if (chatType == "error") Color(0xFFF43F5E).copy(alpha = 0.25f) else Color(0xFF6366F1).copy(alpha = 0.25f),
                        RoundedCornerShape(22.dp)
                    ),
                cornerRadius = 22.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Empty State or Message List
                    if (messages.isEmpty()) {
                        val emptyScrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (isLandscape) 8.dp else 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.verticalScroll(emptyScrollState)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = subjectAccentColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.5.dp, subjectAccentColor.copy(alpha = 0.4f)),
                                    modifier = Modifier.size(if (isLandscape) 40.dp else 64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (chatType == "error") "⚠️" else "⚡",
                                            fontSize = if (isLandscape) 18.sp else 28.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(if (isLandscape) 6.dp else 14.dp))

                                Text(
                                    text = if (chatType == "error") "Diagnose $errorSubject Mistakes" else "Ask Anything in $errorSubject",
                                    style = if (isLandscape) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textColor
                                )

                                Spacer(modifier = Modifier.height(if (isLandscape) 2.dp else 4.dp))

                                Text(
                                    text = if (chatType == "error")
                                        "Paste a wrong question or upload a photo to spot your conceptual gap, silly trap, or calculation slip."
                                    else
                                        "Ask doubts, formula derivations, elimination tricks, or upload photo/PDF question.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subTextColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = if (isLandscape) 8.dp else 16.dp)
                                )

                                Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 18.dp))

                                // Quick Starter Prompt Pills
                                Text(
                                    text = "💡 Tap a quick starter:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subjectAccentColor
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                val starters = if (chatType == "error") {
                                    listOf(
                                        "Why is this step wrong in thermodynamics?",
                                        "Common traps in optics sign convention?",
                                        "How to avoid calculation mistakes in kinematics?"
                                    )
                                } else {
                                    listOf(
                                        "Shortcut for finding hybridization in seconds",
                                        "Explain Lenz's law & induced current direction",
                                        "Key differences between Mitosis & Meiosis"
                                    )
                                }

                                starters.forEach { starter ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                                        border = BorderStroke(0.8.dp, cardBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable {
                                                chatInput = starter
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("✨", fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = starter,
                                                fontSize = 12.sp,
                                                color = textColor,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Render Chat List with Smooth Scrolling & Floating Jump-To-Bottom (High-Performance Zero-Allocation Grouping)
                        val groupedMessages = remember(messages) {
                            val nowCal = java.util.Calendar.getInstance()
                            val nowYear = nowCal.get(java.util.Calendar.YEAR)
                            val nowDay = nowCal.get(java.util.Calendar.DAY_OF_YEAR)
                            val msgCal = java.util.Calendar.getInstance()
                            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                            messages.groupBy { msg ->
                                msgCal.timeInMillis = msg.timestamp
                                val msgYear = msgCal.get(java.util.Calendar.YEAR)
                                val msgDay = msgCal.get(java.util.Calendar.DAY_OF_YEAR)
                                when {
                                    nowYear == msgYear && nowDay == msgDay -> "Today"
                                    nowYear == msgYear && (nowDay - msgDay) == 1 -> "Yesterday"
                                    else -> try {
                                        dateFormat.format(Date(msg.timestamp))
                                    } catch (_: Exception) {
                                        "Earlier"
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)
                            ) {
                                groupedMessages.forEach { (dateHeader, dateMsgs) ->
                                    item(key = "header_$dateHeader") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                color = if (isDark) Color(0x33000000) else Color(0xFFE2E8F0),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = dateHeader,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = subTextColor,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    items(dateMsgs, key = { it.id }) { msg ->
                                        val isUser = msg.sender == "user"
                                        val isStreaming = isAiGenerating && !isUser && msg.id == messages.lastOrNull()?.id

                                        ChatMessageBubble(
                                            msg = msg,
                                            isDark = isDark,
                                            isStreaming = isStreaming,
                                            chatType = chatType,
                                            activeModelLabel = activeModelLabel,
                                            isSpeaking = speakingMsgId == msg.id,
                                            onToggleSpeak = { onToggleSpeak(msg) },
                                            onCopy = {
                                                clipboardManager.setText(AnnotatedString(msg.text))
                                                Toast.makeText(context, "Copied solution to clipboard! 📋", Toast.LENGTH_SHORT).show()
                                            },
                                            onAddToMistakes = {
                                                val qText = msg.text.lines().firstOrNull() ?: "Question from $errorSubject"
                                                viewModel.addMistakeLog(
                                                    subject = errorSubject,
                                                    question = qText.take(150),
                                                    mistakeType = if (chatType == "error") "Conceptual Trap" else "Theory Recall"
                                                )
                                                Toast.makeText(context, "Logged to Mistake Notebook! 📓", Toast.LENGTH_SHORT).show()
                                            },
                                            onSimplify = {
                                                if (!isAiGenerating) {
                                                    viewModel.explainConceptWithGemini(
                                                        context = context,
                                                        subject = errorSubject,
                                                        chatType = chatType,
                                                        conceptText = "Please explain this concept simply with intuition and a real-life analogy: ${msg.text.take(300)}"
                                                    )
                                                }
                                            },
                                            onPracticeSimilar = {
                                                if (!isAiGenerating) {
                                                    viewModel.explainConceptWithGemini(
                                                        context = context,
                                                        subject = errorSubject,
                                                        chatType = chatType,
                                                        conceptText = "Generate 2 high-yield JEE/NEET practice questions testing this exact concept with step-by-step hints: ${msg.text.take(300)}"
                                                    )
                                                }
                                            },
                                            onStepBreakdown = {
                                                if (!isAiGenerating) {
                                                    viewModel.explainConceptWithGemini(
                                                        context = context,
                                                        subject = errorSubject,
                                                        chatType = chatType,
                                                        conceptText = "Break down the solution step-by-step with clear numbered steps, formulas, and reasoning: ${msg.text.take(300)}"
                                                    )
                                                }
                                            },
                                            onImageClick = { uri -> fullImagePreviewUri = uri },
                                            onLongClick = {
                                                selectedMsgForMenu = msg
                                                showLongPressMenu = true
                                            },
                                            renderTable = { tableText ->
                                                val tableData = remember(tableText) { parseMarkdownTable(tableText) }
                                                if (tableData != null) {
                                                    TelegramTableComponent(
                                                        title = tableData.first,
                                                        headers = tableData.second,
                                                        rows = tableData.third,
                                                        isDark = isDark
                                                    )
                                                } else {
                                                    ChatMessageRenderer(
                                                        text = tableText,
                                                        isDark = isDark,
                                                        textColor = if (isDark) Color.White else Color(0xFF0F172A),
                                                        isStreaming = isStreaming
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Smooth Scroll-to-Bottom Floating Action Pill
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isScrolledUp,
                                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 12.dp, end = 12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.94f),
                                    shadowElevation = 8.dp,
                                    border = BorderStroke(1.2.dp, if (isDark) Color(0x44818CF8) else Color(0xFFC7D2FE)),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clickable {
                                            coroutineScope.launch {
                                                val total = listState.layoutInfo.totalItemsCount
                                                if (total > 0) {
                                                    listState.animateScrollToItem(total - 1)
                                                }
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Scroll to bottom",
                                            tint = if (isDark) Color(0xFFA5B4FC) else Color(0xFF6366F1),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== 5. BOTTOM ATTACHMENT & INPUT BAR ====================
            Spacer(modifier = Modifier.height(8.dp))

            // Attachment preview chip if present
            if (attachedUri != null || attachedName != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                    border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (attachedType == "image") Icons.Default.Image else Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = if (attachedType == "image") Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = attachedName ?: "Attached File",
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                attachedUri = null
                                attachedType = null
                                attachedName = null
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = subTextColor, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Floating Input Dock
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = if (isDark) Color(0xFF131B2E) else Color(0xFFF8FAFC),
                border = BorderStroke(1.2.dp, Brush.linearGradient(listOf(Color(0xFF6366F1).copy(alpha = 0.5f), Color(0xFF38BDF8).copy(alpha = 0.5f)))),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showAttachMenu by remember { mutableStateOf(false) }

                    // Camera Button 📷 (One-tap Click Question Photo)
                    IconButton(
                        onClick = launchCamera,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Click Question Photo",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Attachment Menu Button 📎
                    Box {
                        IconButton(
                            onClick = { showAttachMenu = !showAttachMenu },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach Question",
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showAttachMenu,
                            onDismissRequest = { showAttachMenu = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF0F172A) else Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("📷 Click Question Photo", color = textColor, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF38BDF8)) },
                                onClick = {
                                    showAttachMenu = false
                                    launchCamera()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🖼️ Question Screenshot / Gallery", color = textColor, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF10B981)) },
                                onClick = {
                                    showAttachMenu = false
                                    photoPickerLauncher.launch("image/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📄 Doubt PDF / Test Paper", color = textColor, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444)) },
                                onClick = {
                                    showAttachMenu = false
                                    pdfPickerLauncher.launch("application/pdf")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📊 Custom Formula Table", color = textColor, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                onClick = {
                                    showAttachMenu = false
                                    showTableCreatorDialog = true
                                }
                            )
                        }
                    }

                    // Text Input
                    TextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = {
                            Text(
                                text = if (chatType == "error") "Paste wrong question or error..." else "Ask any doubt, concept, or trick...",
                                color = subTextColor,
                                fontSize = 13.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = Color(0xFF6366F1)
                        ),
                        maxLines = 4,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    val isInputEmpty = chatInput.isBlank() && attachedUri == null

                    // Send or Mic Action
                    IconButton(
                        onClick = {
                            if (isInputEmpty) {
                                startVoiceInput()
                            } else if (!isAiGenerating) {
                                viewModel.sendChatMessage(
                                    context = context,
                                    subject = errorSubject,
                                    chatType = chatType,
                                    text = chatInput,
                                    mediaUri = attachedUri,
                                    mediaType = attachedType,
                                    mediaName = attachedName
                                )
                                chatInput = ""
                                attachedUri = null
                                attachedType = null
                                attachedName = null
                            }
                        },
                        enabled = !isAiGenerating,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isInputEmpty) Color(0xFF6366F1) else Color(0xFF38BDF8),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isInputEmpty) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "Voice Dictation",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp).padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==================== 6. LONG PRESS MESSAGE OPTIONS DIALOG ====================
        if (showLongPressMenu && selectedMsgForMenu != null) {
            val msg = selectedMsgForMenu!!
            val isGeminiMsg = msg.sender != "user"
            AlertDialog(
                onDismissRequest = { showLongPressMenu = false },
                title = {
                    Text(
                        text = if (isGeminiMsg) "🤖 Lakshya AI Solution Options" else "💬 Question Actions",
                        color = textColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 1. Analyze Error
                        Surface(
                            color = Color(0xFFF43F5E).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isAiGenerating) {
                                        Toast.makeText(context, "Lakshya AI is busy generating...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.analyzeErrorWithGemini(context, errorSubject, chatType, msg.text, msg.mediaUri, msg.mediaType)
                                        Toast.makeText(context, "Analyzing mistake breakdown...", Toast.LENGTH_SHORT).show()
                                    }
                                    showLongPressMenu = false
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("⚠️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Diagnose Root Mistake with Lakshya AI", color = Color(0xFFFB7185), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // 2. Solve Doubt
                        Surface(
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isAiGenerating) {
                                        Toast.makeText(context, "Lakshya AI is busy generating...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.solveDoubtWithGemini(context, errorSubject, chatType, msg.text, msg.mediaUri, msg.mediaType)
                                        Toast.makeText(context, "Solving step-by-step...", Toast.LENGTH_SHORT).show()
                                    }
                                    showLongPressMenu = false
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("⚡", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Solve Step-by-Step with Lakshya AI", color = Color(0xFFA5B4FC), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // 3. Explain Concept
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isAiGenerating) {
                                        Toast.makeText(context, "Lakshya AI is busy generating...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.explainConceptWithGemini(context, errorSubject, chatType, msg.text)
                                        Toast.makeText(context, "Explaining NCERT core concept...", Toast.LENGTH_SHORT).show()
                                    }
                                    showLongPressMenu = false
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("💡", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Explain NCERT Concept with Lakshya AI", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // 4. Log to Mistake Notebook
                        Surface(
                            color = if (isDark) Color(0x18FFFFFF) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addMistakeLog(
                                        subject = errorSubject,
                                        question = msg.text.take(150),
                                        mistakeType = "Conceptual Trap"
                                    )
                                    Toast.makeText(context, "Saved to Mistake Notebook! 📓", Toast.LENGTH_SHORT).show()
                                    showLongPressMenu = false
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("📓", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Log to Mistake Notebook", color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        // 5. Copy Text
                        Surface(
                            color = if (isDark) Color(0x18FFFFFF) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(msg.text))
                                    Toast.makeText(context, "Copied text! 📋", Toast.LENGTH_SHORT).show()
                                    showLongPressMenu = false
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Copy Text", color = textColor, fontSize = 13.sp)
                            }
                        }

                        // 6. Delete
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.deleteChatMessage(msg.id)
                                    Toast.makeText(context, "Deleted message", Toast.LENGTH_SHORT).show()
                                    showLongPressMenu = false
                                }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Delete Message", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLongPressMenu = false }) {
                        Text("Cancel", color = subTextColor)
                    }
                },
                containerColor = if (isDark) Color(0xFF0F172A) else Color.White
            )
        }

        // ==================== 7. TABLE CREATOR DIALOG ====================
        if (showTableCreatorDialog) {
            TableCreatorDialog(
                onDismiss = { showTableCreatorDialog = false },
                onTableCreated = { tableTitle, headers, rows ->
                    val tableText = buildString {
                        appendLine("[TABLE:$tableTitle]")
                        appendLine("| " + headers.joinToString(" | ") + " |")
                        appendLine("| " + headers.map { "---" }.joinToString(" | ") + " |")
                        rows.forEach { row ->
                            appendLine("| " + row.joinToString(" | ") + " |")
                        }
                    }
                    viewModel.sendChatMessage(
                        context = context,
                        subject = errorSubject,
                        chatType = chatType,
                        text = tableText,
                        mediaType = "table",
                        mediaName = tableTitle
                    )
                    showTableCreatorDialog = false
                },
                isDark = isDark
            )
        }

        // ==================== 8. FULL IMAGE PREVIEW ====================
        if (fullImagePreviewUri != null) {
            Dialog(onDismissRequest = { fullImagePreviewUri = null }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF0F172A) else Color.White,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Question Image Preview", color = textColor, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { fullImagePreviewUri = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        AsyncImage(
                            model = fullImagePreviewUri,
                            contentDescription = "Full Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                }
            }
        }

        // Quick AI Engine Switcher Dialog
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
                    showGeminiModelPickerDialog = true
                },
                selectedOpenRouterModel = openRouterSelectedModel,
                onOpenModelPicker = {
                    showQuickAiEngineDialog = false
                    showOpenRouterModelPickerDialog = true
                },
                selectedGroqModel = groqSelectedModel,
                onOpenGroqModelPicker = {
                    showQuickAiEngineDialog = false
                    showGroqModelPickerDialog = true
                },
                selectedCloudflareModel = cloudflareSelectedModel,
                onOpenCloudflareModelPicker = {
                    showQuickAiEngineDialog = false
                    showCloudflareModelPickerDialog = true
                },
                onDismiss = { showQuickAiEngineDialog = false }
            )
        }

        // Live Gemini Model Browser & Selector
        if (showGeminiModelPickerDialog) {
            GeminiModelPickerDialog(
                isDark = isDark,
                currentSelectedModelId = selectedGeminiModel,
                models = geminiModels,
                isLoading = isGeminiLoadingModels,
                onSelectModel = { modelId ->
                    viewModel.setSelectedGeminiModel(modelId)
                    showGeminiModelPickerDialog = false
                    Toast.makeText(context, "Model switched to: ${modelId.substringAfterLast("/")} (Gemini Active ✦)", Toast.LENGTH_SHORT).show()
                },
                onRefreshLiveModels = {
                    viewModel.fetchGeminiLiveModels { _, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showGeminiModelPickerDialog = false }
            )
        }

        if (showGeminiQuotaDialog) {
            com.example.ui.components.GeminiQuotaShieldDialog(
                quotaUsage = geminiQuotaUsage,
                onDismissRequest = { showGeminiQuotaDialog = false },
                onRefresh = { viewModel.refreshGeminiQuota() }
            )
        }

        // Live OpenRouter Model Browser & Selector
        if (showOpenRouterModelPickerDialog) {
            OpenRouterModelPickerDialog(
                isDark = isDark,
                currentSelectedModelId = openRouterSelectedModel,
                models = openRouterModels,
                isLoading = isOpenRouterLoadingModels,
                onSelectModel = { modelId ->
                    viewModel.setOpenRouterSelectedModel(modelId)
                    showOpenRouterModelPickerDialog = false
                    Toast.makeText(context, "Model switched to: ${modelId.substringAfterLast("/")} (OpenRouter Active)", Toast.LENGTH_SHORT).show()
                },
                onRefreshLiveModels = {
                    viewModel.fetchOpenRouterLiveModels()
                },
                onDismiss = { showOpenRouterModelPickerDialog = false }
            )
        }

        // Live Groq Model Browser & Selector
        if (showGroqModelPickerDialog) {
            GroqModelPickerDialog(
                isDark = isDark,
                currentSelectedModelId = groqSelectedModel,
                models = groqModels,
                isLoading = isGroqLoadingModels,
                onSelectModel = { modelId ->
                    viewModel.setGroqSelectedModel(modelId)
                    showGroqModelPickerDialog = false
                    Toast.makeText(context, "Model switched to: ${modelId.substringAfterLast("/")} (Groq LPU Active ⚡)", Toast.LENGTH_SHORT).show()
                },
                onRefreshLiveModels = {
                    viewModel.fetchGroqLiveModels { _, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showGroqModelPickerDialog = false }
            )
        }

        // Live Cloudflare Model Browser & Selector
        if (showCloudflareModelPickerDialog) {
            CloudflareModelPickerDialog(
                isDark = isDark,
                currentSelectedModelId = cloudflareSelectedModel,
                models = cloudflareModels,
                isLoading = isCloudflareLoadingModels,
                onSelectModel = { modelId ->
                    viewModel.setCloudflareSelectedModel(modelId)
                    showCloudflareModelPickerDialog = false
                    Toast.makeText(context, "Model switched to: ${modelId.substringAfterLast("/")} (Cloudflare Active ☁️)", Toast.LENGTH_SHORT).show()
                },
                onRefreshLiveModels = {
                    viewModel.fetchCloudflareLiveModels { _, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showCloudflareModelPickerDialog = false }
            )
        }

        // Lag-free Smooth Square Image Cropper & Rotator
        if (imageUriToCrop != null) {
            ImageCropperDialog(
                imageUri = imageUriToCrop!!,
                onCropSuccess = { croppedUri ->
                    attachedUri = croppedUri.toString()
                    attachedType = "image"
                    attachedName = "Cropped Question"
                    imageUriToCrop = null
                },
                onDismiss = {
                    imageUriToCrop = null
                }
            )
        }

        // Clear All Chat Confirmation Dialog
        if (showClearChatDialog) {
            AlertDialog(
                onDismissRequest = { showClearChatDialog = false },
                title = {
                    Text(
                        text = "Clear All Chat? 🗑️",
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete all chat history? This will permanently remove all messages from app storage and the device.",
                        color = subTextColor,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllChatMessages()
                            Toast.makeText(context, "All chat history deleted from storage! 🗑️", Toast.LENGTH_SHORT).show()
                            showClearChatDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete All", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearChatDialog = false }) {
                        Text("Cancel", color = subTextColor)
                    }
                },
                containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

// ==================== TELEGRAM STYLE TABLE COMPONENT ====================
@Composable
fun TelegramTableComponent(
    title: String,
    headers: List<String>,
    rows: List<List<String>>,
    isDark: Boolean = true
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0x22000000) else Color(0xFFF8FAFC))
            .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        if (title.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("📊", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFFF59E0B))
            }
        }

        val colCount = remember(headers, rows) {
            maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 1)
        }
        val colWidths = remember(headers, rows, colCount) {
            (0 until colCount).map { colIndex ->
                val headerLen = headers.getOrNull(colIndex)?.length ?: 0
                val maxCellLen = rows.maxOfOrNull { it.getOrNull(colIndex)?.length ?: 0 } ?: 0
                val maxLen = maxOf(headerLen, maxCellLen)
                when {
                    maxLen <= 6 -> 90.dp
                    maxLen <= 12 -> 120.dp
                    maxLen <= 22 -> 160.dp
                    maxLen <= 35 -> 210.dp
                    else -> 260.dp
                }
            }
        }

        Box(modifier = Modifier.horizontalScroll(scrollState)) {
            Column {
                if (headers.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x446366F1))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (c in 0 until colCount) {
                            val h = headers.getOrNull(c) ?: ""
                            val width = colWidths[c]
                            Box(
                                modifier = Modifier
                                    .width(width)
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    com.example.util.MathFormatter.formatScienceAndMathInText(h),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                rows.forEachIndexed { idx, row ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (idx % 2 == 0) (if (isDark) Color(0x11FFFFFF) else Color(0xFFF1F5F9)) else (if (isDark) Color(0x05FFFFFF) else Color.White))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (c in 0 until colCount) {
                            val cell = row.getOrNull(c) ?: ""
                            val width = colWidths[c]
                            Box(
                                modifier = Modifier
                                    .width(width)
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    com.example.util.MathFormatter.formatScienceAndMathInText(cell),
                                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A),
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

// Helper to parse markdown table syntax
fun parseMarkdownTable(text: String): Triple<String, List<String>, List<List<String>>>? {
    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
    var title = ""
    val tableLines = mutableListOf<String>()

    val isSep = { line: String ->
        val clean = line.replace("|", "").replace(" ", "").replace("\t", "")
        clean.isNotEmpty() && clean.all { it == '-' || it == ':' }
    }

    for (line in lines) {
        if (line.startsWith("[TABLE:") && line.endsWith("]")) {
            title = line.removePrefix("[TABLE:").removeSuffix("]")
        } else if (line.contains("|") && !isSep(line)) {
            tableLines.add(line)
        }
    }

    if (tableLines.isEmpty()) return null

    val parseRow = { rowStr: String ->
        var raw = rowStr.trim()
        if (raw.startsWith("|")) raw = raw.substring(1)
        if (raw.endsWith("|")) raw = raw.substring(0, raw.length - 1)
        raw.split("|").map { it.trim() }
    }

    val headers = parseRow(tableLines.first())
    val rows = tableLines.drop(1).map { parseRow(it) }

    return Triple(title, headers, rows)
}

// Table Creator Dialog
@Composable
fun TableCreatorDialog(
    onDismiss: () -> Unit,
    onTableCreated: (title: String, headers: List<String>, rows: List<List<String>>) -> Unit,
    isDark: Boolean = true
) {
    var title by remember { mutableStateOf("Formula Comparison") }
    var col1Name by remember { mutableStateOf("Concept / Topic") }
    var col2Name by remember { mutableStateOf("Formula / Value") }
    var col3Name by remember { mutableStateOf("Mistake Note") }

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val unfocusedBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFCBD5E1)

    val rowsList = remember {
        mutableStateListOf(
            mutableStateListOf("Electrostatics", "F = k*q1*q2 / r^2", "Forgot direction vector"),
            mutableStateListOf("Optics Lens", "1/f = 1/v - 1/u", "Sign convention error")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📊 Build Custom Table", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Table Title / Caption", color = subTextColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF59E0B), unfocusedBorderColor = unfocusedBorder, focusedTextColor = textColor, unfocusedTextColor = textColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Table Columns:", fontSize = 12.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = col1Name,
                        onValueChange = { col1Name = it },
                        label = { Text("Col 1", color = subTextColor) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF818CF8), unfocusedBorderColor = unfocusedBorder, focusedTextColor = textColor, unfocusedTextColor = textColor)
                    )
                    OutlinedTextField(
                        value = col2Name,
                        onValueChange = { col2Name = it },
                        label = { Text("Col 2", color = subTextColor) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF818CF8), unfocusedBorderColor = unfocusedBorder, focusedTextColor = textColor, unfocusedTextColor = textColor)
                    )
                    OutlinedTextField(
                        value = col3Name,
                        onValueChange = { col3Name = it },
                        label = { Text("Col 3", color = subTextColor) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF818CF8), unfocusedBorderColor = unfocusedBorder, focusedTextColor = textColor, unfocusedTextColor = textColor)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Table Rows (${rowsList.size}):", fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { rowsList.add(mutableStateListOf("", "", "")) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x336366F1)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ Add Row", fontSize = 11.sp, color = Color(0xFF818CF8))
                    }
                }

                Column(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowsList.forEachIndexed { rIdx, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = row.getOrElse(0) { "" },
                                onValueChange = { v -> if (row.size > 0) row[0] = v },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF34D399), unfocusedBorderColor = unfocusedBorder, focusedTextColor = textColor, unfocusedTextColor = textColor)
                            )
                            OutlinedTextField(
                                value = row.getOrElse(1) { "" },
                                onValueChange = { v -> if (row.size > 1) row[1] = v },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF34D399), unfocusedBorderColor = unfocusedBorder, focusedTextColor = textColor, unfocusedTextColor = textColor)
                            )
                            OutlinedTextField(
                                value = row.getOrElse(2) { "" },
                                onValueChange = { v -> if (row.size > 2) row[2] = v },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF34D399), unfocusedBorderColor = unfocusedBorder, focusedTextColor = textColor, unfocusedTextColor = textColor)
                            )
                            if (rowsList.size > 1) {
                                IconButton(
                                    onClick = { rowsList.removeAt(rIdx) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Row", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val headers = listOf(col1Name, col2Name, col3Name).filter { it.isNotBlank() }
                val rows = rowsList.map { it.toList() }
                onTableCreated(title, headers, rows)
            }) {
                Text("Insert Table", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = subTextColor)
            }
        },
        containerColor = if (isDark) Color(0xFF0F172A) else Color.White
    )
}
