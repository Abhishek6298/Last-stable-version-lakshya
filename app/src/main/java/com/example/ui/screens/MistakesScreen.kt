package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import coil.compose.AsyncImage
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.horizontalScroll
import coil.request.ImageRequest
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ExtractedIncorrectQuestion
import com.example.data.GeminiChatAssistant
import com.example.data.MistakeChatMessage
import com.example.data.MistakeLog
import com.example.data.MistakePhoto
import com.example.data.MistakePhotoCategory
import com.example.data.photosList
import com.example.data.imageUriList
import com.example.data.isDueForReview
import com.example.data.stageLabel
import com.example.data.stageEmoji
import com.example.data.effectiveStage
import com.example.data.TestbookMistakeExtractionResult
import com.example.data.AiTestQuestion
import com.example.ui.AppViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.AppSegmentedControl
import com.example.ui.components.MathJaxView
import com.example.ui.components.AiQuestionSolutionDeepExplainerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakesScreen(
    viewModel: AppViewModel,
    onNavigate: ((String) -> Unit)? = null,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeSubTab by remember { mutableStateOf("Mistakes") } // "Mistakes" or "Notes & Chat"
    val mistakes by viewModel.allMistakeLogs.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedSubject by remember { mutableStateOf("All") }
    var selectedTypeFilter by remember { mutableStateOf("All") }
    var selectedSpacedFilter by remember { mutableStateOf("ALL") } // "ALL", "DUE", "DAY_1", "DAY_3", "DAY_7", "MASTERED"
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var initialMistakeImageUri by remember { mutableStateOf<String?>(null) }
    var mistakeToDelete by remember { mutableStateOf<MistakeLog?>(null) }
    var mistakeToEdit by remember { mutableStateOf<MistakeLog?>(null) }

    LaunchedEffect(Unit) {
        viewModel.pendingMistakeImageUri?.let { uri ->
            initialMistakeImageUri = uri
            showAddDialog = true
            viewModel.pendingMistakeImageUri = null
        }
    }

    // Full-Screen Image Gallery state
    var previewImagesList by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewPhotosList by remember { mutableStateOf<List<MistakePhoto>>(emptyList()) }
    var previewImageIndex by remember { mutableIntStateOf(0) }

    // Batch Gemini Vision OCR States
    var isBatchScanning by remember { mutableStateOf(false) }
    var batchScanStatus by remember { mutableStateOf("") }
    var batchScanUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var extractedMistakeResult by remember { mutableStateOf<TestbookMistakeExtractionResult?>(null) }
    var showBatchImportDialog by remember { mutableStateOf(false) }

    // Intercept back presses when modals or subtabs are open, or navigate back
    BackHandler(enabled = true) {
        when {
            previewImagesList.isNotEmpty() -> {
                previewImagesList = emptyList()
                previewPhotosList = emptyList()
            }
            showBatchImportDialog -> showBatchImportDialog = false
            showAddDialog -> showAddDialog = false
            mistakeToEdit != null -> mistakeToEdit = null
            mistakeToDelete != null -> mistakeToDelete = null
            activeSubTab != "Mistakes" -> activeSubTab = "Mistakes"
            else -> onNavigateBack()
        }
    }

    // Helper to process selected screenshot URIs
    fun processScreenshots(uris: List<Uri>) {
        if (uris.isEmpty()) return
        batchScanUris = uris
        isBatchScanning = true
        batchScanStatus = "Reading ${uris.size} screenshot(s) & preparing Abhi Magic 🪄 OCR..."

        coroutineScope.launch {
            try {
                val bytesList = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        try {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                if (bytesList.isEmpty()) {
                    isBatchScanning = false
                    Toast.makeText(context, "Could not load selected image(s)", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                batchScanStatus = "Analyzing ${bytesList.size} screenshot(s) with Abhi Magic 🪄 AI..."
                val result = GeminiChatAssistant.extractMistakesFromTestbookScreenshots(
                    context = context,
                    imagesBytesList = bytesList
                )

                isBatchScanning = false
                if (result.isSuccess) {
                    val data = result.getOrNull()
                    if (data != null && data.questions.isNotEmpty()) {
                        extractedMistakeResult = data
                        showBatchImportDialog = true
                    } else {
                        Toast.makeText(context, "No questions could be extracted from image", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "OCR Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                isBatchScanning = false
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launchers for Single & Batch Screenshots
    val batchImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            processScreenshots(uris)
        }
    }

    val singleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            processScreenshots(listOf(uri))
        }
    }

    val isDark = MaterialTheme.colorScheme.background.run { (red + green + blue) < 1.0f }
    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF090D16),
                Color(0xFF0F172A),
                Color(0xFF0B0F19)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFF8FAFC),
                Color(0xFFEEF2F6),
                Color(0xFFF1F5F9)
            )
        )
    }

    val primaryTextColor = if (isDark) Color.White else Color(0xFF0F172A)
    val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val searchBarBg = if (isDark) Color(0x1AFFFFFF) else Color.White
    val searchBarBorder = if (isDark) Color(0x26FFFFFF) else Color(0xFFE2E8F0)

    val availableSubjects = listOf("Physics", "Chemistry", "Biology")
    val subjects = listOf("All", "Physics", "Chemistry", "Biology")

    val filteredMistakes = remember(mistakes, selectedSubject, selectedTypeFilter, searchQuery, selectedSpacedFilter) {
        mistakes.filter { log ->
            val matchSubject = selectedSubject == "All" || log.subject.equals(selectedSubject, ignoreCase = true)
            val matchType = selectedTypeFilter == "All" || log.mistakeType.equals(selectedTypeFilter, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() || 
                log.question.contains(searchQuery, ignoreCase = true) || 
                log.mistakeType.contains(searchQuery, ignoreCase = true) ||
                log.subject.contains(searchQuery, ignoreCase = true) ||
                log.chapter.contains(searchQuery, ignoreCase = true)
            val matchSpaced = when (selectedSpacedFilter) {
                "DUE" -> log.isDueForReview
                "DAY_1" -> !log.isMastered && (log.reviewStage == 0 || log.reviewCount == 0)
                "DAY_3" -> !log.isMastered && log.reviewStage == 1
                "DAY_7" -> !log.isMastered && (log.reviewStage == 2 || log.reviewStage == 3)
                "MASTERED" -> log.isMastered || log.reviewStage >= 4
                else -> true
            }
            matchSubject && matchType && matchSearch && matchSpaced
        }.sortedWith(
            compareByDescending<MistakeLog> { it.isDueForReview }
                .thenByDescending { it.timestamp }
        )
    }

    // Stats calculations
    val totalCount = mistakes.size
    val physicsCount = mistakes.count { it.subject.equals("Physics", ignoreCase = true) }
    val chemistryCount = mistakes.count { it.subject.equals("Chemistry", ignoreCase = true) }
    val biologyCount = mistakes.count { it.subject.equals("Biology", ignoreCase = true) }
    val now = remember { System.currentTimeMillis() }
    val day1DueCount = remember(mistakes, now) {
        mistakes.count { !it.isMastered && (it.reviewStage == 0 || it.reviewCount == 0) }
    }
    val day3DueCount = remember(mistakes, now) {
        mistakes.count { !it.isMastered && it.reviewStage == 1 }
    }
    val day7DueCount = remember(mistakes, now) {
        mistakes.count { !it.isMastered && (it.reviewStage == 2 || it.reviewStage == 3) }
    }
    val masteredCount = remember(mistakes) {
        mistakes.count { it.isMastered || it.reviewStage >= 4 }
    }
    val totalDueCount = remember(mistakes, now) {
        mistakes.count { it.isDueForReview }
    }
    val masteredPercent = remember(mistakes, masteredCount, totalCount) {
        if (totalCount == 0) 0 else ((masteredCount.toFloat() / totalCount) * 100).toInt()
    }
    val subjectCounts = remember(mistakes) {
        mistakes.groupBy { it.subject }.mapValues { it.value.size }
    }

    val topMistakeType = remember(mistakes) {
        if (mistakes.isEmpty()) "None"
        else mistakes.groupBy { it.mistakeType }.maxByOrNull { it.value.size }?.key ?: "None"
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF6366F1),
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                icon = { Icon(Icons.Filled.Add, "Add Mistake") },
                text = { Text("Log Mistake", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                modifier = Modifier.padding(bottom = 60.dp) // Offset for floating dock
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                        // Header Bar
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = onNavigateBack,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000))
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = primaryTextColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "Mistake Notebook",
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = (-0.5).sp
                                                ),
                                                color = primaryTextColor
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFF43F5E).copy(alpha = 0.15f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    "$totalCount",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                                    color = Color(0xFFF43F5E),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                        Text(
                                            "Analyze errors, fix concepts & avoid repeats",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = secondaryTextColor,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                // Overview Analytics Banner
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.AutoGraph,
                                            contentDescription = null,
                                            tint = Color(0xFF6366F1),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "Error Analysis Overview",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = primaryTextColor
                                    )
                                }

                                if (mistakes.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFE11D48).copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.25f))
                                    ) {
                                        Text(
                                            "Frequent: $topMistakeType",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            color = Color(0xFFFB7185),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Subject Stat Boxes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MiniStatBox(
                                    modifier = Modifier.weight(1f),
                                    title = "Physics",
                                    count = physicsCount,
                                    accentColor = Color(0xFF38BDF8),
                                    isDark = isDark
                                )
                                MiniStatBox(
                                    modifier = Modifier.weight(1f),
                                    title = "Chemistry",
                                    count = chemistryCount,
                                    accentColor = Color(0xFF34D399),
                                    isDark = isDark
                                )
                                MiniStatBox(
                                    modifier = Modifier.weight(1f),
                                    title = "Biology",
                                    count = biologyCount,
                                    accentColor = Color(0xFFF43F5E),
                                    isDark = isDark
                                )
                            }
                        }
                    }
                }

                // Spaced Repetition Active Recall Banner (Day 1, Day 3, Day 7 Reviews)
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.5.dp,
                                Brush.linearGradient(
                                    colors = if (isDark) {
                                        listOf(Color(0xFF8B5CF6), Color(0xFF6366F1), Color(0xFF38BDF8))
                                    } else {
                                        listOf(Color(0xFFC4B5FD), Color(0xFFA5B4FC), Color(0xFFBAE6FD))
                                    }
                                ),
                                RoundedCornerShape(24.dp)
                            ),
                        cornerRadius = 24.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🧠", fontSize = 18.sp)
                                    }
                                    Column {
                                        Text(
                                            "Spaced Repetition Recall",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = primaryTextColor
                                        )
                                        Text(
                                            "Ebbinghaus Curve: Day 1 • 3 • 7 Reviews",
                                            fontSize = 11.sp,
                                            color = secondaryTextColor
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = if (totalDueCount > 0) Color(0xFF6366F1).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (totalDueCount > 0) Color(0xFF6366F1).copy(alpha = 0.3f) else Color(0xFF10B981).copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = if (totalDueCount > 0) "$totalDueCount Due Today" else "✓ Up-to-date",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        color = if (totalDueCount > 0) Color(0xFF818CF8) else Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 4 Interactive Intervals Pills (Clickable to Filter!)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Day 1
                                val isDay1Sel = selectedSpacedFilter == "DAY_1"
                                Surface(
                                    onClick = {
                                        selectedSpacedFilter = if (isDay1Sel) "ALL" else "DAY_1"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDay1Sel) Color(0xFF38BDF8).copy(alpha = 0.2f) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                                    border = if (isDay1Sel) BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("DAY 1 (24H)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isDay1Sel) Color(0xFF38BDF8) else secondaryTextColor, maxLines = 1, softWrap = false)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("$day1DueCount", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF38BDF8))
                                    }
                                }
                                // Day 3
                                val isDay3Sel = selectedSpacedFilter == "DAY_3"
                                Surface(
                                    onClick = {
                                        selectedSpacedFilter = if (isDay3Sel) "ALL" else "DAY_3"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDay3Sel) Color(0xFFF59E0B).copy(alpha = 0.2f) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                                    border = if (isDay3Sel) BorderStroke(1.5.dp, Color(0xFFF59E0B)) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("DAY 3 (72H)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isDay3Sel) Color(0xFFF59E0B) else secondaryTextColor, maxLines = 1, softWrap = false)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("$day3DueCount", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                                    }
                                }
                                // Day 7
                                val isDay7Sel = selectedSpacedFilter == "DAY_7"
                                Surface(
                                    onClick = {
                                        selectedSpacedFilter = if (isDay7Sel) "ALL" else "DAY_7"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDay7Sel) Color(0xFFA855F7).copy(alpha = 0.2f) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                                    border = if (isDay7Sel) BorderStroke(1.5.dp, Color(0xFFA855F7)) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("DAY 7+ (WK)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isDay7Sel) Color(0xFFA855F7) else secondaryTextColor, maxLines = 1, softWrap = false)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("$day7DueCount", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFA855F7))
                                    }
                                }
                                // Mastered
                                val isMasteredSel = selectedSpacedFilter == "MASTERED"
                                Surface(
                                    onClick = {
                                        selectedSpacedFilter = if (isMasteredSel) "ALL" else "MASTERED"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isMasteredSel) Color(0xFF10B981).copy(alpha = 0.2f) else if (isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9),
                                    border = if (isMasteredSel) BorderStroke(1.5.dp, Color(0xFF10B981)) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("MASTERED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isMasteredSel) Color(0xFF10B981) else secondaryTextColor, maxLines = 1, softWrap = false)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("$masteredCount", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action buttons: Direct Due & Filter Toggles in Notebook
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Due Today Quick Filter Button
                                val isDueSelected = selectedSpacedFilter == "DUE"
                                Button(
                                    onClick = {
                                        selectedSpacedFilter = if (isDueSelected) "ALL" else "DUE"
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .heightIn(min = 42.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDueSelected) Color(0xFF10B981) else Color(0xFF6366F1)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        if (isDueSelected) Icons.Filled.CheckCircle else Icons.Filled.FilterList,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isDueSelected) "Showing Due ($totalDueCount)" else "⚡ Show Due Today ($totalDueCount)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                // Reset / Show All Filter Button
                                if (selectedSpacedFilter != "ALL") {
                                    OutlinedButton(
                                        onClick = {
                                            selectedSpacedFilter = "ALL"
                                        },
                                        modifier = Modifier
                                            .weight(0.8f)
                                            .heightIn(min = 42.dp),
                                        border = BorderStroke(1.5.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Refresh,
                                            contentDescription = null,
                                            tint = primaryTextColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Show All",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryTextColor,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            // Mastery Progress Bar
                            if (totalCount > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Long-Term Mastery Retention",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = secondaryTextColor
                                        )
                                        Text(
                                            "$masteredPercent% ($masteredCount/$totalCount Mastered)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    LinearProgressIndicator(
                                        progress = { (masteredPercent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = Color(0xFF10B981),
                                        trackColor = if (isDark) Color(0x1FFFFFFF) else Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Vision Scanner Hero Banner (Batch & Single OCR)
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        if (isDark) listOf(Color(0xFF1E1B4B).copy(alpha = 0.5f), Color(0xFF311042).copy(alpha = 0.5f))
                                        else listOf(Color(0xFFEEF2FF), Color(0xFFFAF5FF))
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🪄", fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            "Abhi Magic 🪄 Question Scanner",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.5.sp,
                                            color = primaryTextColor,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    "AI OCR",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF10B981),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                            Text(
                                                "Auto-detect wrong test questions & categorize",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = secondaryTextColor,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Batch Scan Button
                                Button(
                                    onClick = { batchImagePicker.launch("image/*") },
                                    modifier = Modifier.weight(1.2f).heightIn(min = 44.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6366F1)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Batch Screenshots",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }

                                // Single Image Button
                                OutlinedButton(
                                    onClick = { singleImagePicker.launch("image/*") },
                                    modifier = Modifier.weight(0.9f).heightIn(min = 44.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isDark) Color(0x26FFFFFF) else Color(0xFFCBD5E1)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        "Single Image",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Search Bar
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = searchBarBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, searchBarBorder),
                        shadowElevation = if (isDark) 0.dp else 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = secondaryTextColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = primaryTextColor,
                                    fontSize = 14.sp
                                ),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search questions, concepts, types...",
                                            color = secondaryTextColor.copy(alpha = 0.7f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Clear",
                                        tint = secondaryTextColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Subject Filter Chips
                item {
                    if (subjects.size <= 4) {
                        AppSegmentedControl(
                            items = subjects,
                            selectedItem = selectedSubject,
                            onItemSelected = { selectedSubject = it },
                            itemLabel = { it },
                            itemEmoji = {
                                when (it) {
                                    "Physics" -> "⚡"
                                    "Chemistry" -> "🧪"
                                    "Biology" -> "🧬"
                                    else -> "📚"
                                }
                            },
                            itemBadge = {
                                val count = when (it) {
                                    "Physics" -> physicsCount
                                    "Chemistry" -> chemistryCount
                                    "Biology" -> biologyCount
                                    else -> totalCount
                                }
                                "$count"
                            },
                            selectedColor = when (selectedSubject) {
                                "Physics" -> Color(0xFF2563EB)
                                "Chemistry" -> Color(0xFFD97706)
                                "Biology" -> Color(0xFFE11D48)
                                else -> Color(0xFF6D28D9)
                            },
                            selectedGradient = when (selectedSubject) {
                                "Physics" -> listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                                "Chemistry" -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                "Biology" -> listOf(Color(0xFFF43F5E), Color(0xFFE11D48))
                                else -> listOf(Color(0xFF7C3AED), Color(0xFF6D28D9))
                            },
                            isDark = isDark,
                            fontSize = 11.5.sp,
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 2.dp),
                            cornerRadius = 14.dp
                        )
                    } else {
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(subjects.size) { idx ->
                                val subj = subjects[idx]
                                val isSelected = selectedSubject == subj
                                val count = if (subj == "All") totalCount else (subjectCounts[subj] ?: 0)
                                Surface(
                                    onClick = { selectedSubject = subj },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF6366F1) else (if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF818CF8) else (if (isDark) Color(0x1FFFFFFF) else Color(0xFFE2E8F0))
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            subj,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else primaryTextColor
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else (if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0))
                                        ) {
                                            Text(
                                                "$count",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else secondaryTextColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Mistakes List Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (filteredMistakes.isEmpty()) "Recorded Mistakes" else "Recorded Mistakes (${filteredMistakes.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = primaryTextColor
                        )

                        if (selectedSubject != "All" || searchQuery.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    selectedSubject = "All"
                                    searchQuery = ""
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Reset Filters", fontSize = 12.sp, color = Color(0xFF6366F1))
                            }
                        }
                    }
                }

                // Empty State
                if (filteredMistakes.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            cornerRadius = 24.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF6366F1).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (searchQuery.isNotEmpty()) Icons.Outlined.SearchOff else Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    if (searchQuery.isNotEmpty()) "No matching mistakes found" else "No Mistakes Logged Yet!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = primaryTextColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    if (searchQuery.isNotEmpty()) "Try searching for a different keyword or concept." else "Log your errors during mock tests or question practice to master tricky concepts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondaryTextColor,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add First Mistake")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredMistakes, key = { it.id }) { mistake ->
                        ModernMistakeItem(
                            mistake = mistake,
                            isDark = isDark,
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            onEdit = { mistakeToEdit = mistake },
                            onDelete = { mistakeToDelete = mistake },
                            onAdvanceReview = {
                                viewModel.advanceMistakeReview(mistake.id)
                                Toast.makeText(context, "🎉 Advanced review stage!", Toast.LENGTH_SHORT).show()
                            },
                            onResetReview = {
                                viewModel.resetMistakeReview(mistake.id)
                                Toast.makeText(context, "🔄 Reset to Day 1 review queue", Toast.LENGTH_SHORT).show()
                            },
                            onMarkMastered = { mastered ->
                                viewModel.markMistakeMastered(mistake.id, mastered)
                                Toast.makeText(context, if (mastered) "🏆 Marked as Mastered!" else "Returned to review queue", Toast.LENGTH_SHORT).show()
                            },
                            onScheduleReview = { nextTime ->
                                viewModel.setMistakeCustomReviewTime(mistake.id, nextTime)
                                Toast.makeText(context, "📅 Review scheduled!", Toast.LENGTH_SHORT).show()
                            },
                            onOpenStudyTubeVideo = { url, tsSec ->
                                viewModel.openInStudyTube(
                                    urlOrIdOrQuery = url,
                                    title = mistake.pdfName?.replace("StudyTube: ", "") ?: mistake.chapter,
                                    subject = mistake.subject,
                                    isPlaylist = false,
                                    timestampSeconds = tsSec
                                )
                                onNavigate?.invoke("studytube")
                            },
                            onOpenImages = { images, idx ->
                                previewImagesList = images
                                previewPhotosList = mistake.photosList
                                previewImageIndex = idx
                            },
                            onOpenPdf = { uriStr, name ->
                                try {
                                    val uri = Uri.parse(uriStr)
                                    viewModel.openPdfInReader(uri, name)
                                    if (onNavigate != null) {
                                        onNavigate("pdf_viewer")
                                    } else {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Open PDF"))
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                item {
                    com.example.ui.components.AppBrandingFooter(isDark = isDark)
                }
            }
        }
    }

    // Full-Screen Image Gallery Viewer Dialog (Supports Multiple Images with Next/Prev & Thumbnails)
    if (previewImagesList.isNotEmpty()) {
        val currentImg = previewImagesList.getOrElse(previewImageIndex) { previewImagesList.first() }
        val currentPhotoCat = if (previewPhotosList.isNotEmpty() && previewImageIndex < previewPhotosList.size) {
            previewPhotosList[previewImageIndex].category
        } else {
            null
        }
        val categoryTitle = when (currentPhotoCat) {
            MistakePhotoCategory.QUESTION -> "❓ Question Photo"
            MistakePhotoCategory.ANSWER -> "💡 Answer / Solution"
            MistakePhotoCategory.BOTH -> "📑 Q&A (Same Photo)"
            null -> "Mistake Photo / Solution"
        }
        Dialog(
            onDismissRequest = {
                previewImagesList = emptyList()
                previewPhotosList = emptyList()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable {
                        previewImagesList = emptyList()
                        previewPhotosList = emptyList()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar with Close button & Counter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    categoryTitle,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (previewImagesList.size > 1) {
                                    Text(
                                        "Photo ${previewImageIndex + 1} of ${previewImagesList.size}",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = {
                                previewImagesList = emptyList()
                                previewPhotosList = emptyList()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close Preview",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Centered Full Size Image with Next / Previous Arrows
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = currentImg,
                            contentDescription = "Full Mistake Image Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )

                        // Previous Image Button
                        if (previewImagesList.size > 1 && previewImageIndex > 0) {
                            IconButton(
                                onClick = { previewImageIndex-- },
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 8.dp)
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Next Image Button
                        if (previewImagesList.size > 1 && previewImageIndex < previewImagesList.size - 1) {
                            IconButton(
                                onClick = { previewImageIndex++ },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 8.dp)
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Bottom Thumbnail Strip for Multi-Image Switch
                    if (previewImagesList.size > 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            items(previewImagesList.size) { tIdx ->
                                val isSelected = tIdx == previewImageIndex
                                Surface(
                                    onClick = { previewImageIndex = tIdx },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        2.dp,
                                        if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.size(width = 54.dp, height = 54.dp)
                                ) {
                                    AsyncImage(
                                        model = previewImagesList[tIdx],
                                        contentDescription = "Thumbnail ${tIdx + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (previewImagesList.size > 1) "Tap thumbnail or arrows to switch photos, tap X to return" else "Tap outside or X button to return",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (mistakeToDelete != null) {
        val toDelete = mistakeToDelete!!
        AlertDialog(
            onDismissRequest = { mistakeToDelete = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
            title = {
                Text(
                    "Delete Mistake Note?",
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
            },
            text = {
                Text(
                    "Are you sure you want to remove this mistake log for ${toDelete.subject}?",
                    color = secondaryTextColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMistakeLog(toDelete.id)
                        mistakeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mistakeToDelete = null }
                ) {
                    Text("Cancel", color = secondaryTextColor)
                }
            }
        )
    }

    // Modern Add/Edit Mistake Modal Dialog
    if (showAddDialog || mistakeToEdit != null) {
        val currentEdit = mistakeToEdit
        ModernAddMistakeDialog(
            isDark = isDark,
            editingMistake = currentEdit,
            initialImageUri = initialMistakeImageUri,
            availableSubjects = availableSubjects,
            onDismiss = {
                showAddDialog = false
                mistakeToEdit = null
                initialMistakeImageUri = null
            },
            onOpenPdf = { uriStr, name ->
                try {
                    val uri = Uri.parse(uriStr)
                    viewModel.openPdfInReader(uri, name)
                    if (onNavigate != null) {
                        onNavigate("pdf_viewer")
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(Intent.createChooser(intent, "Open PDF"))
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Cannot open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onSave = { subject, question, type, chapter, pdfUri, pdfName, imageUri ->
                if (currentEdit != null) {
                    viewModel.updateMistakeLog(
                        currentEdit.copy(
                            subject = subject,
                            question = question,
                            mistakeType = type,
                            chapter = chapter,
                            pdfUri = pdfUri,
                            pdfName = pdfName,
                            imageUri = imageUri
                        )
                    )
                } else {
                    viewModel.addMistakeLog(
                        subject = subject,
                        question = question,
                        mistakeType = type,
                        chapter = chapter,
                        pdfUri = pdfUri,
                        pdfName = pdfName,
                        imageUri = imageUri
                    )
                }
                showAddDialog = false
                mistakeToEdit = null
            }
        )
    }

    // Batch Mistake Review & Import Dialog
    if (showBatchImportDialog && extractedMistakeResult != null) {
        val resultData = extractedMistakeResult!!
        BatchMistakesImportDialog(
            result = resultData,
            isDark = isDark,
            onDismiss = { 
                showBatchImportDialog = false
                extractedMistakeResult = null
            },
            onImportConfirmed = { selectedQuestions ->
                val mistakeImgDir = File(context.filesDir, "mistake_images").apply { if (!exists()) mkdirs() }
                val persistentPhotoUris = batchScanUris.mapNotNull { uri ->
                    try {
                        val fileName = getMistakeImageFileName(context, uri)
                        val cleanName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                        val localImg = File(mistakeImgDir, "${System.currentTimeMillis()}_${(100..999).random()}_$cleanName")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            localImg.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (localImg.exists() && localImg.length() > 0) {
                            Uri.fromFile(localImg).toString()
                        } else {
                            uri.toString()
                        }
                    } catch (e: Exception) {
                        uri.toString()
                    }
                }
                val combinedPhotoUriStr = if (persistentPhotoUris.isNotEmpty()) {
                    persistentPhotoUris.joinToString("||") { "${MistakePhotoCategory.BOTH.name}::$it" }
                } else null

                val mistakeLogs = selectedQuestions.map { q ->
                    val content = if (q.formattedNotebookEntry.isNotBlank()) {
                        q.formattedNotebookEntry
                    } else {
                        buildString {
                            if (q.chapter.isNotBlank() && q.chapter != "General") {
                                append("**[${q.chapter}]**\n\n")
                            }
                            append("**Question:**\n${q.questionText}\n")
                            if (q.options.isNotEmpty()) {
                                append("\n**Options:**\n")
                                q.options.forEach { opt -> append("- $opt\n") }
                            }
                            if (q.userWrongOption.isNotBlank() || q.correctOption.isNotBlank()) {
                                append("\n❌ **My Answer:** ${q.userWrongOption}\n✅ **Correct Answer:** ${q.correctOption}\n")
                            }
                            if (q.mistakeType.isNotBlank()) {
                                append("⚠️ **Mistake Reason:** ${q.mistakeType}\n")
                            }
                            if (q.keyConceptMissed.isNotBlank()) {
                                append("💡 **Key Concept:** ${q.keyConceptMissed}\n")
                            }
                            if (q.stepByStepSolution.isNotBlank()) {
                                append("\n📝 **Master Solution:**\n${q.stepByStepSolution}")
                            }
                        }
                    }
                    MistakeLog(
                        subject = q.subject,
                        question = content,
                        mistakeType = q.mistakeType,
                        chapter = q.chapter,
                        imageUri = combinedPhotoUriStr
                    )
                }

                viewModel.addBatchMistakeLogs(mistakeLogs)
                showBatchImportDialog = false
                extractedMistakeResult = null
                batchScanUris = emptyList()
                Toast.makeText(
                    context,
                    "🎉 Imported ${mistakeLogs.size} mistake(s) with Question & Answer into Notebook!",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // Fullscreen Scanning AI Animation Overlay
    AnimatedVisibility(
        visible = isBatchScanning,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF818CF8),
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "🪄 ABHI MAGIC OCR",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    batchScanStatus,
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchMistakesImportDialog(
    result: TestbookMistakeExtractionResult,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onImportConfirmed: (List<ExtractedIncorrectQuestion>) -> Unit
) {
    val questionsState = remember {
        mutableStateListOf<ExtractedIncorrectQuestion>().apply {
            addAll(result.questions)
        }
    }

    val dialogBg = if (isDark) Color(0xFF0F172A) else Color.White
    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val primaryText = if (isDark) Color.White else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBorder = if (isDark) Color(0x1FFFFFFF) else Color(0xFFE2E8F0)

    val selectedCount = questionsState.count { it.isSelectedForImport }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Extracted Mistakes",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = primaryText
                            )
                            Text(
                                "Found ${questionsState.size} question(s) from screenshots",
                                fontSize = 12.sp,
                                color = secondaryText
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = secondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Select All / Deselect All Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$selectedCount of ${questionsState.size} selected",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF818CF8)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                for (i in questionsState.indices) {
                                    questionsState[i] = questionsState[i].copy(isSelectedForImport = true)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Select All", fontSize = 11.5.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = {
                                for (i in questionsState.indices) {
                                    questionsState[i] = questionsState[i].copy(isSelectedForImport = false)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Deselect", fontSize = 11.5.sp, color = secondaryText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Question List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(questionsState.size, key = { index -> questionsState[index].id }) { index ->
                        val item = questionsState[index]
                        val subjectColor = when (item.subject.lowercase()) {
                            "physics" -> Color(0xFF38BDF8)
                            "chemistry" -> Color(0xFF34D399)
                            "biology" -> Color(0xFFF43F5E)
                            else -> Color(0xFFA855F7)
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            border = androidx.compose.foundation.BorderStroke(
                                if (item.isSelectedForImport) 1.5.dp else 1.dp,
                                if (item.isSelectedForImport) Color(0xFF6366F1) else cardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Subject tag
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = subjectColor.copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, subjectColor.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                item.subject,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = subjectColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Mistake Type tag
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                item.mistakeType,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFF59E0B),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Checkbox
                                    Checkbox(
                                        checked = item.isSelectedForImport,
                                        onCheckedChange = { checked ->
                                            questionsState[index] = item.copy(isSelectedForImport = checked)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF6366F1)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Question text
                                MathJaxView(
                                    text = item.questionText,
                                    isDark = isDark,
                                    fontSize = 13.sp
                                )

                                if (item.userWrongOption.isNotBlank() || item.correctOption.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDark) Color(0x0AFFFFFF) else Color(0xFFEEF2F6))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (item.userWrongOption.isNotBlank()) {
                                            Text(
                                                "❌ Marked: ${item.userWrongOption}",
                                                fontSize = 11.sp,
                                                color = Color(0xFFEF4444),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (item.correctOption.isNotBlank()) {
                                            Text(
                                                "✅ Correct: ${item.correctOption}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF10B981),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (item.keyConceptMissed.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "💡 Concept: ${item.keyConceptMissed}",
                                        fontSize = 11.5.sp,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                    )
                                }

                                if (item.stepByStepSolution.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "📝 Solution: ${item.stepByStepSolution}",
                                        fontSize = 11.sp,
                                        color = secondaryText,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Subject Switcher for fine-tuning
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Subject:", fontSize = 10.5.sp, color = secondaryText)
                                    listOf("Physics", "Chemistry", "Biology").forEach { subj ->
                                        val isSubjSelected = item.subject.equals(subj, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSubjSelected) Color(0xFF6366F1) else Color.Transparent)
                                                .border(1.dp, if (isSubjSelected) Color(0xFF6366F1) else cardBorder, RoundedCornerShape(6.dp))
                                                .clickable {
                                                    questionsState[index] = item.copy(subject = subj)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                subj,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSubjSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSubjSelected) Color.White else secondaryText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                    ) {
                        Text("Cancel", color = secondaryText, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val selected = questionsState.filter { it.isSelectedForImport }
                            if (selected.isNotEmpty()) {
                                onImportConfirmed(selected)
                            }
                        },
                        enabled = selectedCount > 0,
                        modifier = Modifier.weight(1.8f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1),
                            disabledContainerColor = Color(0xFF6366F1).copy(alpha = 0.4f)
                        )
                    ) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Import ($selectedCount) to Notebook",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MiniStatBox(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    accentColor: Color,
    isDark: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0x0AFFFFFF) else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    title,
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "$count",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
        }
    }
}

@Composable
fun ModernMistakeItem(
    mistake: MistakeLog,
    isDark: Boolean,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdvanceReview: () -> Unit = {},
    onResetReview: () -> Unit = {},
    onMarkMastered: (Boolean) -> Unit = {},
    onScheduleReview: (Long) -> Unit = {},
    onOpenStudyTubeVideo: ((url: String, timestampSec: Int) -> Unit)? = null,
    onOpenImages: (images: List<String>, initialIndex: Int) -> Unit = { _, _ -> },
    onOpenPdf: (uriString: String, fileName: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val subjectColor = when (mistake.subject.lowercase()) {
        "physics" -> Color(0xFF38BDF8)
        "chemistry" -> Color(0xFF34D399)
        "biology" -> Color(0xFFF43F5E)
        else -> Color(0xFFA855F7)
    }

    val typeColor = when (mistake.mistakeType.lowercase()) {
        "silly mistake", "silly" -> Color(0xFFF59E0B)
        "conceptual error", "conceptual" -> Color(0xFFEF4444)
        "formula error", "formula" -> Color(0xFF8B5CF6)
        "calculation slip", "calculation" -> Color(0xFF06B6D4)
        "time rush", "time pressure" -> Color(0xFFEC4899)
        else -> Color(0xFF64748B)
    }

    val dateFormatted = remember(mistake.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(mistake.timestamp))
    }

    var isExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { isExpanded = !isExpanded },
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Header: Subject & Chapter Badges + Edit/Delete Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Subject Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = subjectColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, subjectColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            mistake.subject,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            color = subjectColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Chapter Badge
                    if (mistake.chapter.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f))
                        ) {
                            Text(
                                "📖 ${mistake.chapter}",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                color = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Edit Button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit Mistake",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = secondaryTextColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Mistake Type / Reason Badge placed below Chapter Name
            if (mistake.mistakeType.isNotBlank()) {
                Spacer(modifier = Modifier.height(7.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = typeColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, typeColor.copy(alpha = 0.3f)),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            when (mistake.mistakeType.lowercase()) {
                                "silly mistake", "silly" -> "⚡"
                                "conceptual error", "conceptual" -> "💡"
                                "formula error", "formula" -> "📐"
                                "calculation slip", "calculation" -> "🔢"
                                "time rush", "time pressure" -> "⏱️"
                                else -> "🏷️"
                            },
                            fontSize = 10.sp
                        )
                        Text(
                            mistake.mistakeType,
                            color = typeColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Question / Concept missed text
            MathJaxView(
                text = mistake.question,
                isDark = isDark,
                fontSize = 14.sp
            )

            // Attached Photos / Images (Supports 3 Options: Question, Answer, Q&A Same Photo)
            val attachedPhotos = mistake.photosList
            val attachedImages = mistake.imageUriList
            if (attachedPhotos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                val qCount = attachedPhotos.count { it.category == MistakePhotoCategory.QUESTION }
                val aCount = attachedPhotos.count { it.category == MistakePhotoCategory.ANSWER }
                val bothCount = attachedPhotos.count { it.category == MistakePhotoCategory.BOTH }

                if (attachedPhotos.size == 1) {
                    val singlePhoto = attachedPhotos[0]
                    val catAccent = when (singlePhoto.category) {
                        MistakePhotoCategory.QUESTION -> Color(0xFF0284C7)
                        MistakePhotoCategory.ANSWER -> Color(0xFFD97706)
                        MistakePhotoCategory.BOTH -> Color(0xFF059669)
                    }
                    val catLabel = when (singlePhoto.category) {
                        MistakePhotoCategory.QUESTION -> "Question Photo"
                        MistakePhotoCategory.ANSWER -> "Answer Photo"
                        MistakePhotoCategory.BOTH -> "Q&A (Same Photo)"
                    }
                    // Single Image Preview Card
                    Surface(
                        onClick = {
                            onOpenImages(attachedImages, 0)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = catAccent.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, catAccent.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 220.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0x1A000000) else Color(0x0A000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = singlePhoto.uri,
                                    contentDescription = "Attached Mistake Photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 220.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                // Category Badge (Top-Start) strictly horizontal
                                Surface(
                                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                                    color = catAccent.copy(alpha = 0.92f),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(singlePhoto.category.emoji, fontSize = 10.5.sp, maxLines = 1, softWrap = false)
                                        Text(
                                            catLabel,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.ZoomIn,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            "Tap to view full image",
                                            color = Color.White,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Multiple Images Gallery Carousel
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF6366F1).copy(alpha = 0.08f))
                            .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    Icons.Filled.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "${attachedPhotos.size} Photos",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (qCount > 0) {
                                    Surface(shape = CircleShape, color = Color(0xFF0284C7).copy(alpha = 0.18f)) {
                                        Text(
                                            "❓ $qCount Q",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = Color(0xFF0284C7),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                if (aCount > 0) {
                                    Surface(shape = CircleShape, color = Color(0xFFD97706).copy(alpha = 0.18f)) {
                                        Text(
                                            "💡 $aCount Ans",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = Color(0xFFD97706),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                if (bothCount > 0) {
                                    Surface(shape = CircleShape, color = Color(0xFF059669).copy(alpha = 0.18f)) {
                                        Text(
                                            "📑 $bothCount Q&A",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = Color(0xFF059669),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                            Text(
                                "View gallery",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF6366F1),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(attachedPhotos.size) { imgIdx ->
                                val photo = attachedPhotos[imgIdx]
                                val itemAccent = when (photo.category) {
                                    MistakePhotoCategory.QUESTION -> Color(0xFF0284C7)
                                    MistakePhotoCategory.ANSWER -> Color(0xFFD97706)
                                    MistakePhotoCategory.BOTH -> Color(0xFF059669)
                                }
                                val itemBadge = when (photo.category) {
                                    MistakePhotoCategory.QUESTION -> "Question"
                                    MistakePhotoCategory.ANSWER -> "Answer"
                                    MistakePhotoCategory.BOTH -> "Q&A"
                                }
                                Surface(
                                    onClick = { onOpenImages(attachedImages, imgIdx) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) Color(0x1A000000) else Color(0x0A000000),
                                    border = BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.size(width = 135.dp, height = 100.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = photo.uri,
                                            contentDescription = "Photo ${imgIdx + 1}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(bottomEnd = 8.dp),
                                            color = itemAccent.copy(alpha = 0.90f),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(photo.category.emoji, fontSize = 9.sp, maxLines = 1, softWrap = false)
                                                Text(
                                                    itemBadge,
                                                    color = Color.White,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
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

            // Attached Source: StudyTube Lecture Video OR Attached PDF Document
            if (!mistake.pdfUri.isNullOrBlank()) {
                val rawUri = mistake.pdfUri
                val isVideoSource = rawUri.contains("youtube.com", ignoreCase = true) ||
                        rawUri.contains("youtu.be", ignoreCase = true) ||
                        rawUri.startsWith("studytube", ignoreCase = true) ||
                        mistake.pdfName?.contains("StudyTube", ignoreCase = true) == true ||
                        mistake.pdfName?.contains("Lecture", ignoreCase = true) == true

                Spacer(modifier = Modifier.height(10.dp))
                if (isVideoSource) {
                    val timeSec = Regex("[?&]t=(\\d+)").find(rawUri)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val formattedTs = if (timeSec > 0) {
                        val h = timeSec / 3600
                        val m = (timeSec % 3600) / 60
                        val s = timeSec % 60
                        if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
                    } else "00:00"

                    Surface(
                        onClick = {
                            if (onOpenStudyTubeVideo != null) {
                                onOpenStudyTubeVideo(rawUri, timeSec)
                            } else {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUri))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open video", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE11D48).copy(alpha = 0.14f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDA4AF).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFE11D48), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "Play Lecture",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    mistake.pdfName?.replace("StudyTube: ", "")?.ifBlank { "StudyTube Lecture Video" } ?: "StudyTube Lecture Video",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF881337),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Tap to watch on StudyTube at exact timestamp",
                                    fontSize = 10.5.sp,
                                    color = if (isDark) Color(0xFFFDA4AF) else Color(0xFF9F1239)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE11D48)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.AccessTime,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        "Play @ $formattedTs",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        onClick = {
                            onOpenPdf(mistake.pdfUri, mistake.pdfName ?: "Mistake Note PDF")
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                mistake.pdfName?.ifBlank { "Attached PDF Document" } ?: "Attached PDF Document",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        "Open PDF",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 🧠 Spaced Repetition Review & Recall Control Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    1.dp,
                    if (mistake.isDueForReview) Color(0xFF8B5CF6).copy(alpha = 0.5f)
                    else if (isDark) Color(0x20FFFFFF) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                var showSrMenu by remember { mutableStateOf(false) }

                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stage Badge + Due indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (mistake.isMastered) Color(0xFF10B981).copy(alpha = 0.18f)
                                        else when (mistake.reviewStage) {
                                            0 -> Color(0xFF38BDF8).copy(alpha = 0.18f)
                                            1 -> Color(0xFFF59E0B).copy(alpha = 0.18f)
                                            2 -> Color(0xFFA855F7).copy(alpha = 0.18f)
                                            else -> Color(0xFF6366F1).copy(alpha = 0.18f)
                                        },
                                border = BorderStroke(
                                    1.dp,
                                    if (mistake.isMastered) Color(0xFF10B981).copy(alpha = 0.4f)
                                    else when (mistake.reviewStage) {
                                        0 -> Color(0xFF38BDF8).copy(alpha = 0.4f)
                                        1 -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                                        2 -> Color(0xFFA855F7).copy(alpha = 0.4f)
                                        else -> Color(0xFF6366F1).copy(alpha = 0.4f)
                                    }
                                )
                            ) {
                                Text(
                                    text = "${mistake.stageEmoji} ${mistake.stageLabel}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (mistake.isMastered) Color(0xFF10B981)
                                            else when (mistake.reviewStage) {
                                                0 -> Color(0xFF0284C7)
                                                1 -> Color(0xFFD97706)
                                                2 -> Color(0xFF9333EA)
                                                else -> Color(0xFF4F46E5)
                                            },
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }

                            if (mistake.isDueForReview) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        "⚡ Due Today",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (mistake.isMastered) {
                                Text(
                                    "✓ Retained",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF10B981)
                                )
                            } else if (mistake.nextReviewTime > 0) {
                                val remainingDays = ((mistake.nextReviewTime - System.currentTimeMillis()) / 86400_000L).coerceAtLeast(1)
                                Text(
                                    "⏳ In ${remainingDays}d",
                                    fontSize = 11.sp,
                                    color = secondaryTextColor
                                )
                            }
                        }

                        // SR Action Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Quick Solved (+1 Stage) Button
                            if (!mistake.isMastered) {
                                FilledTonalButton(
                                    onClick = onAdvanceReview,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                                        contentColor = Color(0xFF10B981)
                                    ),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Solved (+1)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Spaced Repetition Options Dropdown (Reschedule / Master / Reset)
                            Box {
                                IconButton(
                                    onClick = { showSrMenu = true },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.MoreVert,
                                        contentDescription = "Spaced Repetition Options",
                                        tint = secondaryTextColor,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSrMenu,
                                    onDismissRequest = { showSrMenu = false },
                                    modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                                ) {
                                    if (!mistake.isMastered) {
                                        DropdownMenuItem(
                                            text = { Text("✓ Solved (Advance +1 Stage)", fontSize = 13.sp) },
                                            leadingIcon = { Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp)) },
                                            onClick = {
                                                showSrMenu = false
                                                onAdvanceReview()
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(if (mistake.isMastered) "↩ Move back to Active Queue" else "🏆 Mark as Mastered", fontSize = 13.sp) },
                                        leadingIcon = { Icon(Icons.Filled.EmojiEvents, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showSrMenu = false
                                            onMarkMastered(!mistake.isMastered)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🔄 Reset to Day 1 (24h)", fontSize = 13.sp) },
                                        leadingIcon = { Icon(Icons.Filled.Refresh, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showSrMenu = false
                                            onResetReview()
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("📅 Review in 1 Day (Tomorrow)", fontSize = 12.sp) },
                                        onClick = {
                                            showSrMenu = false
                                            onScheduleReview(System.currentTimeMillis() + 1L * 86400_000L)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("📅 Review in 3 Days", fontSize = 12.sp) },
                                        onClick = {
                                            showSrMenu = false
                                            onScheduleReview(System.currentTimeMillis() + 3L * 86400_000L)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("📅 Review in 7 Days (1 Week)", fontSize = 12.sp) },
                                        onClick = {
                                            showSrMenu = false
                                            onScheduleReview(System.currentTimeMillis() + 7L * 86400_000L)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row: Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = secondaryTextColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        dateFormatted,
                        fontSize = 11.sp,
                        color = secondaryTextColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAddMistakeDialog(
    isDark: Boolean,
    editingMistake: MistakeLog? = null,
    initialImageUri: String? = null,
    initialSubject: String? = null,
    initialChapter: String? = null,
    initialQuestion: String? = null,
    initialPdfUri: String? = null,
    initialPdfName: String? = null,
    availableSubjects: List<String> = listOf("Physics", "Chemistry", "Biology"),
    onDismiss: () -> Unit,
    onOpenPdf: (uriString: String, fileName: String) -> Unit = { _, _ -> },
    onSave: (subject: String, question: String, type: String, chapter: String, pdfUri: String?, pdfName: String?, imageUri: String?) -> Unit
) {
    val context = LocalContext.current

    var subject by remember(availableSubjects, editingMistake, initialSubject) {
        mutableStateOf(editingMistake?.subject ?: initialSubject ?: (availableSubjects.firstOrNull() ?: "Physics"))
    }
    var chapter by remember(editingMistake, initialChapter) {
        mutableStateOf(editingMistake?.chapter ?: initialChapter ?: "")
    }
    var mistakeType by remember(editingMistake) {
        mutableStateOf(editingMistake?.mistakeType ?: "Silly Mistake")
    }
    var customType by remember { mutableStateOf("") }
    var question by remember(editingMistake, initialQuestion) {
        mutableStateOf(editingMistake?.question ?: initialQuestion ?: "")
    }
    var isCustomTypeSelected by remember { mutableStateOf(false) }
    var isChapterPickerOpen by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isExtractingAi by remember { mutableStateOf(false) }
    var extractStatus by remember { mutableStateOf("") }

    // 3 Photo Categories Attachment state (Questions, Answer, Q&A Same Photo)
    var attachedPhotos by remember(editingMistake, initialImageUri) {
        val list = mutableListOf<MistakePhoto>()
        if (initialImageUri != null) {
            val parts = initialImageUri.split("||").map { it.trim() }.filter { it.isNotBlank() }
            list.addAll(parts.map { MistakePhoto.fromSerialized(it) })
        }
        if (editingMistake != null) {
            list.addAll(editingMistake.photosList)
        }
        mutableStateOf(list.toList())
    }

    var selectedPhotoForOptionsIdx by remember { mutableStateOf<Int?>(null) }
    var previewingPhotoUri by remember { mutableStateOf<String?>(null) }

    fun extractFromAttachedPhotos() {
        if (attachedPhotos.isEmpty()) {
            Toast.makeText(context, "Please attach a photo first", Toast.LENGTH_SHORT).show()
            return
        }
        isExtractingAi = true
        extractStatus = "Reading image data..."
        coroutineScope.launch {
            try {
                val bytesList = withContext(Dispatchers.IO) {
                    attachedPhotos.mapNotNull { photo ->
                        try {
                            val parsed = Uri.parse(photo.uri)
                            if (parsed.scheme == "file") {
                                val f = File(parsed.path ?: "")
                                if (f.exists()) f.readBytes() else null
                            } else {
                                context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                if (bytesList.isEmpty()) {
                    isExtractingAi = false
                    Toast.makeText(context, "Could not read photo data", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                extractStatus = "Extracting Question & Answer with Abhi Magic 🪄..."
                val result = GeminiChatAssistant.extractMistakesFromTestbookScreenshots(
                    context = context,
                    imagesBytesList = bytesList
                )

                isExtractingAi = false
                if (result.isSuccess) {
                    val data = result.getOrNull()
                    val firstQ = data?.questions?.firstOrNull()
                    if (firstQ != null) {
                        if (firstQ.subject.isNotBlank()) {
                            subject = firstQ.subject
                        }
                        if (firstQ.chapter.isNotBlank() && firstQ.chapter != "General") {
                            chapter = firstQ.chapter
                        }
                        if (firstQ.mistakeType.isNotBlank()) {
                            mistakeType = firstQ.mistakeType
                            isCustomTypeSelected = false
                        }
                        val formattedText = if (firstQ.formattedNotebookEntry.isNotBlank()) {
                            firstQ.formattedNotebookEntry
                        } else {
                            buildString {
                                if (firstQ.chapter.isNotBlank() && firstQ.chapter != "General") {
                                append("**[${firstQ.chapter}]**\n\n")
                            }
                            append("**Question:**\n${firstQ.questionText}\n")
                            if (firstQ.options.isNotEmpty()) {
                                append("\n**Options:**\n")
                                firstQ.options.forEach { opt -> append("- $opt\n") }
                            }
                            if (firstQ.userWrongOption.isNotBlank() || firstQ.correctOption.isNotBlank()) {
                                append("\n❌ **My Answer:** ${firstQ.userWrongOption}\n✅ **Correct Answer:** ${firstQ.correctOption}\n")
                            }
                            if (firstQ.keyConceptMissed.isNotBlank()) {
                                append("\n💡 **Key Concept:** ${firstQ.keyConceptMissed}\n")
                            }
                            if (firstQ.stepByStepSolution.isNotBlank()) {
                                append("\n📝 **Master Solution:**\n${firstQ.stepByStepSolution}")
                            }
                        }
                    }
                    question = formattedText
                    Toast.makeText(context, "🎉 Question & Answer extracted with AI!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No text recognized in photo", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "AI OCR: ${result.exceptionOrNull()?.message ?: "Extraction failed"}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            isExtractingAi = false
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

    var selectedPhotoCategory by remember { mutableStateOf(MistakePhotoCategory.BOTH) }

    // Multi-Image Picker launcher supporting category tagging
    val multiImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newPhotos = mutableListOf<MistakePhoto>()
            val mistakeImgDir = File(context.filesDir, "mistake_images").apply { if (!exists()) mkdirs() }
            for (uri in uris) {
                try {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        // ignore
                    }
                    val fileName = getMistakeImageFileName(context, uri)
                    var persistentUriStr = uri.toString()
                    try {
                        val cleanName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                        val localImg = File(mistakeImgDir, "${System.currentTimeMillis()}_${(100..999).random()}_$cleanName")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            localImg.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (localImg.exists() && localImg.length() > 0) {
                            persistentUriStr = Uri.fromFile(localImg).toString()
                        }
                    } catch (saveEx: Exception) {
                        saveEx.printStackTrace()
                    }
                    newPhotos.add(MistakePhoto(uri = persistentUriStr, category = selectedPhotoCategory))
                } catch (e: Exception) {
                    // ignore
                }
            }
            if (newPhotos.isNotEmpty()) {
                attachedPhotos = attachedPhotos + newPhotos
                val catLabel = when (selectedPhotoCategory) {
                    MistakePhotoCategory.QUESTION -> "Question"
                    MistakePhotoCategory.ANSWER -> "Answer"
                    MistakePhotoCategory.BOTH -> "Q&A (Same Photo)"
                }
                Toast.makeText(context, "${newPhotos.size} $catLabel photo(s) attached!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Single Image Picker fallback launcher
    val singleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val mistakeImgDir = File(context.filesDir, "mistake_images").apply { if (!exists()) mkdirs() }
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // ignore
                }
                val fileName = getMistakeImageFileName(context, uri)
                var persistentUriStr = uri.toString()
                try {
                    val cleanName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                    val localImg = File(mistakeImgDir, "${System.currentTimeMillis()}_${(100..999).random()}_$cleanName")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        localImg.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (localImg.exists() && localImg.length() > 0) {
                        persistentUriStr = Uri.fromFile(localImg).toString()
                    }
                } catch (saveEx: Exception) {
                    saveEx.printStackTrace()
                }
                val newPhoto = MistakePhoto(uri = persistentUriStr, category = selectedPhotoCategory)
                attachedPhotos = attachedPhotos + newPhoto
                val catLabel = when (selectedPhotoCategory) {
                    MistakePhotoCategory.QUESTION -> "Question"
                    MistakePhotoCategory.ANSWER -> "Answer"
                    MistakePhotoCategory.BOTH -> "Q&A (Same Photo)"
                }
                Toast.makeText(context, "📸 1 $catLabel photo attached!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun launchPickerFor(cat: MistakePhotoCategory) {
        selectedPhotoCategory = cat
        try {
            multiImagePicker.launch("image/*")
        } catch (e: Exception) {
            try {
                singleImagePicker.launch("image/*")
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not open gallery: ${ex.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Simple PDF / Video attachment state (No AI processing)
    var attachedPdfUri by remember(editingMistake, initialPdfUri) {
        mutableStateOf<String?>(editingMistake?.pdfUri ?: initialPdfUri)
    }
    var attachedPdfName by remember(editingMistake, initialPdfName) {
        mutableStateOf<String?>(editingMistake?.pdfName ?: initialPdfName)
    }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Non-persistable URI is fine
                }
                val fileName = getMistakeDocFileName(context, uri)
                var persistentUriStr = uri.toString()
                try {
                    val mistakePdfDir = File(context.filesDir, "mistake_pdfs").apply { if (!exists()) mkdirs() }
                    val cleanName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                    val localPdf = File(mistakePdfDir, "${System.currentTimeMillis()}_$cleanName")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        localPdf.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (localPdf.exists() && localPdf.length() > 0) {
                        persistentUriStr = Uri.fromFile(localPdf).toString()
                    }
                } catch (saveEx: Exception) {
                    saveEx.printStackTrace()
                }

                attachedPdfUri = persistentUriStr
                attachedPdfName = fileName
                Toast.makeText(context, "Attached: $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Could not attach file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val presetTypes = listOf(
        "Silly Mistake",
        "Conceptual Gap",
        "Formula Error",
        "Calculation Slip",
        "Time Rush",
        "Question Misread"
    )

    val subjects = remember(availableSubjects) {
        if (availableSubjects.isNotEmpty()) availableSubjects else listOf("Physics", "Chemistry", "Biology")
    }

    val subjectChapters = remember(subject) {
        when {
            subject.contains("Physics", ignoreCase = true) -> com.example.data.ExamSyllabusDatabase.neetPhysicsChapters.map { it.name }
            subject.contains("Chemistry", ignoreCase = true) -> com.example.data.ExamSyllabusDatabase.neetChemistryChapters.map { it.name }
            subject.contains("Biology", ignoreCase = true) -> com.example.data.ExamSyllabusDatabase.neetBiologyChapters.map { it.name }
            else -> com.example.data.ExamSyllabusDatabase.getChaptersFor(com.example.data.ExamCategory.NEET, subject).map { it.name }
        }
    }

    val dialogBg = if (isDark) Color(0xFF131C31) else Color.White
    val primaryText = if (isDark) Color.White else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = dialogBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0xFFE2E8F0)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📓", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                if (editingMistake != null) "Edit Mistake Note" else "Log a Mistake",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = primaryText,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                if (editingMistake != null) "Update subject, category, or problem details" else "Learn & prevent future negative marks",
                                fontSize = 12.sp,
                                color = secondaryText
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = secondaryText, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subject Selector
                Text(
                    "Subject",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryText
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (subjects.size <= 3) {
                    AppSegmentedControl(
                        items = subjects,
                        selectedItem = subject,
                        onItemSelected = { 
                            subject = it
                            // Clear chapter if it doesn't belong to new subject
                            chapter = ""
                        },
                        itemLabel = { it },
                        itemEmoji = {
                            when (it) {
                                "Physics" -> "⚡"
                                "Chemistry" -> "🧪"
                                else -> "🧬"
                            }
                        },
                        selectedColor = when (subject) {
                            "Physics" -> Color(0xFF2563EB)
                            "Chemistry" -> Color(0xFFD97706)
                            else -> Color(0xFFE11D48)
                        },
                        selectedGradient = when (subject) {
                            "Physics" -> listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                            "Chemistry" -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                            else -> listOf(Color(0xFFF43F5E), Color(0xFFE11D48))
                        },
                        isDark = isDark,
                        fontSize = 12.sp,
                        cornerRadius = 14.dp
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(subjects.size) { idx ->
                            val s = subjects[idx]
                            val isSelected = subject == s
                            Surface(
                                onClick = { 
                                    subject = s 
                                    chapter = ""
                                },
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
                                    color = if (isSelected) Color.White else primaryText,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Chapter Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Chapter Name",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryText
                    )
                    if (chapter.isNotBlank()) {
                        Text(
                            "Clear",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { chapter = "" }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Chapter Selection Button / Box
                Surface(
                    onClick = { isChapterPickerOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0x0DFFFFFF) else Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (chapter.isNotBlank()) Color(0xFF6366F1) else (if (isDark) Color(0x26FFFFFF) else Color(0xFFE2E8F0))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Book,
                                contentDescription = null,
                                tint = if (chapter.isNotBlank()) Color(0xFF6366F1) else secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                if (chapter.isNotBlank()) chapter else "Select $subject Chapter (Optional)...",
                                fontSize = 13.sp,
                                fontWeight = if (chapter.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (chapter.isNotBlank()) primaryText else secondaryText.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = "Select Chapter",
                            tint = secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Quick Chapter Chips for Fast Selection
                if (subjectChapters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(subjectChapters.take(6)) { ch ->
                            val isSelected = chapter.equals(ch, ignoreCase = true)
                            Surface(
                                onClick = { chapter = ch },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.2f) else (if (isDark) Color(0x0AFFFFFF) else Color(0xFFF1F5F9)),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF6366F1) else (if (isDark) Color(0x14FFFFFF) else Color(0xFFE2E8F0))
                                )
                            ) {
                                Text(
                                    ch,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) (if (isDark) Color.White else Color(0xFF4F46E5)) else secondaryText,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mistake Type Preset Selector
                Text(
                    "Mistake Reason / Category",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryText
                )
                Spacer(modifier = Modifier.height(6.dp))

                // 2-row chip cloud
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetTypes.take(3).forEach { type ->
                            MistakeTypeChip(
                                modifier = Modifier.weight(1f),
                                label = type,
                                isSelected = !isCustomTypeSelected && mistakeType == type,
                                isDark = isDark,
                                onSelect = {
                                    isCustomTypeSelected = false
                                    mistakeType = type
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetTypes.drop(3).forEach { type ->
                            MistakeTypeChip(
                                modifier = Modifier.weight(1f),
                                label = type,
                                isSelected = !isCustomTypeSelected && mistakeType == type,
                                isDark = isDark,
                                onSelect = {
                                    isCustomTypeSelected = false
                                    mistakeType = type
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Question & Concept Field
                Text(
                    "Question Details & Concept Missed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryText
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = {
                        Text(
                            "e.g. Missed negative sign in Lens formula, or forgot catalyst in Reimer-Tiemann reaction...",
                            fontSize = 13.sp,
                            color = secondaryText.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 115.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = if (isDark) Color(0x26FFFFFF) else Color(0xFFE2E8F0),
                        focusedContainerColor = if (isDark) Color(0x0DFFFFFF) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0x0DFFFFFF) else Color(0xFFF8FAFC),
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Multiple Photos / Images Attachment Section - 3 Options: Question, Answer, Q&A in same photo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Photo Attachment Options",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            "Choose: 1. Question, 2. Answer, or 3. Both in same photo",
                            fontSize = 11.sp,
                            color = secondaryText,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (attachedPhotos.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f))
                        ) {
                            Text(
                                "${attachedPhotos.size} Added",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6366F1),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3-Option Segmented Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val categories = listOf(
                        MistakePhotoCategory.QUESTION,
                        MistakePhotoCategory.ANSWER,
                        MistakePhotoCategory.BOTH
                    )
                    categories.forEach { cat ->
                        val isSelected = selectedPhotoCategory == cat
                        val catCount = attachedPhotos.count { it.category == cat }
                        val accentColor = when (cat) {
                            MistakePhotoCategory.QUESTION -> Color(0xFF0284C7)
                            MistakePhotoCategory.ANSWER -> Color(0xFFD97706)
                            MistakePhotoCategory.BOTH -> Color(0xFF059669)
                        }
                        val label = when (cat) {
                            MistakePhotoCategory.QUESTION -> "Question"
                            MistakePhotoCategory.ANSWER -> "Answer"
                            MistakePhotoCategory.BOTH -> "Q&A (1 Photo)"
                        }

                        Surface(
                            onClick = { selectedPhotoCategory = cat },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) accentColor.copy(alpha = if (isDark) 0.22f else 0.12f) else (if (isDark) Color(0x0DFFFFFF) else Color(0xFFF8FAFC)),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) accentColor else (if (isDark) Color(0x1FFFFFFF) else Color(0xFFE2E8F0))
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    cat.emoji,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) (if (isDark) Color.White else accentColor) else secondaryText,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (catCount > 0) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(accentColor)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            "$catCount",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Active Category Action Box
                val activeAccent = when (selectedPhotoCategory) {
                    MistakePhotoCategory.QUESTION -> Color(0xFF0284C7)
                    MistakePhotoCategory.ANSWER -> Color(0xFFD97706)
                    MistakePhotoCategory.BOTH -> Color(0xFF059669)
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = activeAccent.copy(alpha = if (isDark) 0.12f else 0.07f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, activeAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(selectedPhotoCategory.emoji, fontSize = 12.sp, maxLines = 1, softWrap = false)
                                Text(
                                    when (selectedPhotoCategory) {
                                        MistakePhotoCategory.QUESTION -> "Option 1: Question Photo"
                                        MistakePhotoCategory.ANSWER -> "Option 2: Answer / Solution Photo"
                                        MistakePhotoCategory.BOTH -> "Option 3: Question & Answer (In Same Photo)"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                when (selectedPhotoCategory) {
                                    MistakePhotoCategory.QUESTION -> "Crop / screenshot of problem or diagram"
                                    MistakePhotoCategory.ANSWER -> "Step-by-step solution or formula breakdown"
                                    MistakePhotoCategory.BOTH -> "Single combined photo having both Q & A"
                                },
                                fontSize = 10.5.sp,
                                color = secondaryText,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { launchPickerFor(selectedPhotoCategory) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = activeAccent),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Attach Photo",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Display attached photos gallery with Category Badges and tap to select options
                if (attachedPhotos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF6366F1).copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            val qCount = attachedPhotos.count { it.category == MistakePhotoCategory.QUESTION }
                            val aCount = attachedPhotos.count { it.category == MistakePhotoCategory.ANSWER }
                            val bothCount = attachedPhotos.count { it.category == MistakePhotoCategory.BOTH }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    if (qCount > 0) {
                                        Surface(shape = CircleShape, color = Color(0xFF0284C7).copy(alpha = 0.18f)) {
                                            Text(
                                                "❓ $qCount Q",
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                                color = Color(0xFF0284C7),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    if (aCount > 0) {
                                        Surface(shape = CircleShape, color = Color(0xFFD97706).copy(alpha = 0.18f)) {
                                            Text(
                                                "💡 $aCount Ans",
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                                color = Color(0xFFD97706),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    if (bothCount > 0) {
                                        Surface(shape = CircleShape, color = Color(0xFF059669).copy(alpha = 0.18f)) {
                                            Text(
                                                "📑 $bothCount Q&A",
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                                color = Color(0xFF059669),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                TextButton(
                                    onClick = { attachedPhotos = emptyList() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Clear", tint = Color(0xFFEF4444), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Clear All", fontSize = 10.5.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                                }
                            }

                            Text(
                                "💡 Tap on any photo to choose its option (Question / Answer / Q&A Both)",
                                fontSize = 10.sp,
                                color = secondaryText,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Horizontal thumbnail list with Category Badge & tap-to-select options
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(attachedPhotos.size) { imgIdx ->
                                    val photo = attachedPhotos[imgIdx]
                                    val itemAccent = when (photo.category) {
                                        MistakePhotoCategory.QUESTION -> Color(0xFF0284C7)
                                        MistakePhotoCategory.ANSWER -> Color(0xFFD97706)
                                        MistakePhotoCategory.BOTH -> Color(0xFF059669)
                                    }
                                    val itemLabel = when (photo.category) {
                                        MistakePhotoCategory.QUESTION -> "Question"
                                        MistakePhotoCategory.ANSWER -> "Answer"
                                        MistakePhotoCategory.BOTH -> "Q&A Both"
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(width = 125.dp, height = 105.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isDark) Color(0x1A000000) else Color(0x0A000000))
                                            .border(1.5.dp, itemAccent.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedPhotoForOptionsIdx = imgIdx
                                            }
                                    ) {
                                        AsyncImage(
                                            model = photo.uri,
                                            contentDescription = "Attached Photo #${imgIdx + 1}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )

                                        // Category Badge at Top-Start
                                        Surface(
                                            onClick = { selectedPhotoForOptionsIdx = imgIdx },
                                            shape = RoundedCornerShape(bottomEnd = 7.dp),
                                            color = itemAccent.copy(alpha = 0.95f),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(photo.category.emoji, fontSize = 9.sp, maxLines = 1, softWrap = false)
                                                Text(
                                                    itemLabel,
                                                    color = Color.White,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // Delete individual photo button at Top-End
                                        Surface(
                                            onClick = {
                                                attachedPhotos = attachedPhotos.toMutableList().apply { removeAt(imgIdx) }
                                            },
                                            shape = CircleShape,
                                            color = Color(0xFFEF4444),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(3.dp)
                                                .size(20.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Filled.Close,
                                                    contentDescription = "Delete Photo",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }

                                        // Bottom Action Indicator "Choose Option ⚙️"
                                        Surface(
                                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                                            color = Color.Black.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    "Select Option",
                                                    color = Color.White,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // ⚡ Auto-Extract Question & Answer from Photo with Gemini Vision AI
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { extractFromAttachedPhotos() },
                                enabled = !isExtractingAi,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1),
                                    disabledContainerColor = Color(0xFF6366F1).copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 42.dp)
                            ) {
                                if (isExtractingAi) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        extractStatus,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "🪄 Auto-Extract with Abhi Magic 🪄",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Simple & Clean PDF Upload / Attachment Section
                Text(
                    "PDF Document (Optional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryText
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (attachedPdfUri != null) {
                    val rawPdfUri = attachedPdfUri ?: ""
                    val isVideoAttachment = rawPdfUri.contains("youtube.com", ignoreCase = true) ||
                            rawPdfUri.contains("youtu.be", ignoreCase = true) ||
                            rawPdfUri.startsWith("studytube", ignoreCase = true) ||
                            attachedPdfName?.contains("StudyTube", ignoreCase = true) == true ||
                            attachedPdfName?.contains("Lecture", ignoreCase = true) == true

                    val cardColor = if (isVideoAttachment) Color(0xFFE11D48) else Color(0xFFEF4444)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = cardColor.copy(alpha = 0.10f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardColor.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cardColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isVideoAttachment) Icons.Filled.PlayArrow else Icons.Filled.PictureAsPdf,
                                        contentDescription = null,
                                        tint = cardColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        attachedPdfName ?: if (isVideoAttachment) "StudyTube Lecture Timestamp" else "Attached Document.pdf",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (isVideoAttachment) "📺 Lecture timestamp link attached" else "📄 PDF attached to this note",
                                        fontSize = 10.5.sp,
                                        color = if (isVideoAttachment) Color(0xFFE11D48) else Color(0xFF10B981),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        if (attachedPdfUri != null) {
                                            onOpenPdf(attachedPdfUri!!, attachedPdfName ?: (if (isVideoAttachment) "Lecture Video" else "Attached Document.pdf"))
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(if (isVideoAttachment) "Play" else "View", fontSize = 11.5.sp, color = if (isVideoAttachment) Color(0xFFE11D48) else Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = { pdfPicker.launch("application/pdf") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Change", fontSize = 11.5.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = {
                                        attachedPdfUri = null
                                        attachedPdfName = null
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove Attachment",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        onClick = { pdfPicker.launch("application/pdf") },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0x0DFFFFFF) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0x26FFFFFF) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.PictureAsPdf,
                                    contentDescription = "Upload PDF",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Upload / Attach PDF File",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText
                                )
                                Text(
                                    "Attach question paper, DPP sheet, or solution PDF",
                                    fontSize = 11.sp,
                                    color = secondaryText
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "Upload",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0xFFCBD5E1))
                    ) {
                        Text("Cancel", color = secondaryText, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val finalType = if (isCustomTypeSelected && customType.isNotBlank()) customType.trim() else mistakeType
                            val combinedImageUri = if (attachedPhotos.isEmpty()) null else attachedPhotos.joinToString("||") { it.toSerialized() }
                            val finalQuestion = if (question.isNotBlank()) {
                                question.trim()
                            } else if (attachedPdfName != null) {
                                "Attached PDF: $attachedPdfName"
                            } else if (attachedPhotos.isNotEmpty()) {
                                val chapterLabel = if (chapter.isNotBlank()) chapter else subject
                                buildString {
                                    append("**[$chapterLabel Problem & Solution Note]**\n\n")
                                    val qCount = attachedPhotos.count { it.category == MistakePhotoCategory.QUESTION }
                                    val aCount = attachedPhotos.count { it.category == MistakePhotoCategory.ANSWER }
                                    val bothCount = attachedPhotos.count { it.category == MistakePhotoCategory.BOTH }
                                    val parts = mutableListOf<String>()
                                    if (qCount > 0) parts.add("$qCount Question photo(s)")
                                    if (aCount > 0) parts.add("$aCount Answer photo(s)")
                                    if (bothCount > 0) parts.add("$bothCount Q&A photo(s)")
                                    append("*(Attached ${parts.joinToString(", ")} - tap image above to view full photo & solution)*")
                                }
                            } else {
                                val chapterLabel = if (chapter.isNotBlank()) chapter else subject
                                "**[$chapterLabel $finalType Note]**"
                            }
                            onSave(subject, finalQuestion, finalType, chapter.trim(), attachedPdfUri, attachedPdfName, combinedImageUri)
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        )
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (editingMistake != null) "Update Note" else "Save Note", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Searchable Chapter Selection Dialog
    if (isChapterPickerOpen) {
        ChapterSelectionDialog(
            subject = subject,
            chapters = subjectChapters,
            currentSelection = chapter,
            isDark = isDark,
            onDismiss = { isChapterPickerOpen = false },
            onChapterSelected = { selected ->
                chapter = selected
                isChapterPickerOpen = false
            }
        )
    }

    // Photo Category Option Selector Dialog (Shown when user taps any attached photo)
    selectedPhotoForOptionsIdx?.let { idx ->
        if (idx in attachedPhotos.indices) {
            val targetPhoto = attachedPhotos[idx]
            MistakePhotoOptionsDialog(
                photo = targetPhoto,
                index = idx,
                total = attachedPhotos.size,
                isDark = isDark,
                onDismiss = { selectedPhotoForOptionsIdx = null },
                onSelectCategory = { newCat ->
                    attachedPhotos = attachedPhotos.toMutableList().apply {
                        set(idx, targetPhoto.copy(category = newCat))
                    }
                    selectedPhotoForOptionsIdx = null
                },
                onViewFullscreen = {
                    previewingPhotoUri = targetPhoto.uri
                },
                onDelete = {
                    attachedPhotos = attachedPhotos.toMutableList().apply {
                        removeAt(idx)
                    }
                    selectedPhotoForOptionsIdx = null
                }
            )
        } else {
            selectedPhotoForOptionsIdx = null
        }
    }

    // Fullscreen Photo Viewer Dialog
    previewingPhotoUri?.let { uriStr ->
        MistakePhotoFullscreenViewer(
            imageUri = uriStr,
            onDismiss = { previewingPhotoUri = null }
        )
    }
}

@Composable
fun MistakePhotoOptionsDialog(
    photo: MistakePhoto,
    index: Int,
    total: Int,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSelectCategory: (MistakePhotoCategory) -> Unit,
    onViewFullscreen: () -> Unit,
    onDelete: () -> Unit
) {
    val dialogBg = if (isDark) Color(0xFF1E293B) else Color.White
    val primaryText = if (isDark) Color.White else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
            shadowElevation = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📸", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Photo #${index + 1} Options",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = primaryText
                            )
                            Text(
                                "Choose what this photo represents",
                                fontSize = 11.5.sp,
                                color = secondaryText
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = secondaryText, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Thumbnail Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0x26000000) else Color(0x0D000000))
                        .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .clickable { onViewFullscreen() }
                ) {
                    AsyncImage(
                        model = photo.uri,
                        contentDescription = "Photo Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.ZoomIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Zoom 🔍", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "SELECT OPTION / TYPE:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = secondaryText,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3 Options to choose from:
                val options = listOf(
                    Triple(MistakePhotoCategory.QUESTION, "❓ Option 1: Question Photo", "Question text, diagram, or problem statement"),
                    Triple(MistakePhotoCategory.ANSWER, "💡 Option 2: Answer / Solution Photo", "Step-by-step solution, calculation, or formula"),
                    Triple(MistakePhotoCategory.BOTH, "📑 Option 3: Q&A (Both in Same Photo)", "Single screenshot containing both Question and Answer")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    options.forEach { (cat, title, desc) ->
                        val isSelected = photo.category == cat
                        val accent = when (cat) {
                            MistakePhotoCategory.QUESTION -> Color(0xFF0284C7)
                            MistakePhotoCategory.ANSWER -> Color(0xFFD97706)
                            MistakePhotoCategory.BOTH -> Color(0xFF059669)
                        }
                        Surface(
                            onClick = { onSelectCategory(cat) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) accent.copy(alpha = if (isDark) 0.25f else 0.12f) else (if (isDark) Color(0x0DFFFFFF) else Color(0xFFF8FAFC)),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) accent else (if (isDark) Color(0x1FFFFFFF) else Color(0xFFE2E8F0))
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        title,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) accent else primaryText
                                    )
                                    Text(
                                        desc,
                                        fontSize = 10.5.sp,
                                        color = secondaryText
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Actions: Fullscreen & Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewFullscreen,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Full", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

@Composable
fun MistakePhotoFullscreenViewer(
    imageUri: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Full Mistake Photo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )

            // Close button at top right
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun ChapterSelectionDialog(
    subject: String,
    chapters: List<String>,
    currentSelection: String,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onChapterSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredChapters = remember(searchQuery, chapters) {
        if (searchQuery.isBlank()) chapters
        else chapters.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    val dialogBg = if (isDark) Color(0xFF131C31) else Color.White
    val primaryText = if (isDark) Color.White else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0xFFE2E8F0)),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(horizontal = 4.dp)
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
                            "Select $subject Chapter",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = primaryText
                        )
                        Text(
                            "${chapters.size} Syllabus Chapters Available",
                            fontSize = 11.5.sp,
                            color = secondaryText
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x26FFFFFF) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = primaryText,
                                fontSize = 13.sp
                            ),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search chapter name...",
                                        color = secondaryText.copy(alpha = 0.7f),
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    tint = secondaryText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Chapter option if search query has no exact match
                if (searchQuery.isNotBlank() && !chapters.any { it.equals(searchQuery.trim(), ignoreCase = true) }) {
                    Surface(
                        onClick = { onChapterSelected(searchQuery.trim()) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF6366F1).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Use custom: \"${searchQuery.trim()}\"",
                                color = Color(0xFF6366F1),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }

                // Chapter List
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredChapters, key = { it }) { ch ->
                        val isSelected = currentSelection.equals(ch, ignoreCase = true)
                        Surface(
                            onClick = { onChapterSelected(ch) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.15f) else (if (isDark) Color(0x0AFFFFFF) else Color(0xFFF8FAFC)),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF6366F1) else (if (isDark) Color(0x14FFFFFF) else Color(0xFFE2E8F0))
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("📖", fontSize = 14.sp)
                                    Text(
                                        ch,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) (if (isDark) Color.White else Color(0xFF4F46E5)) else primaryText
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(18.dp)
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
fun MistakeTypeChip(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    isDark: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.2f) else if (isDark) Color(0x0AFFFFFF) else Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF6366F1) else if (isDark) Color(0x14FFFFFF) else Color(0xFFE2E8F0)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onSelect() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) (if (isDark) Color.White else Color(0xFF4F46E5)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            )
        }
    }
}

/**
 * Robust parser converting MistakeLog text into a structured AiTestQuestion
 * with question text, options A-D, user selection, answer key, and master solution.
 */
fun parseMistakeToAiQuestion(mistake: MistakeLog): AiTestQuestion {
    val rawText = mistake.question
    var qText = rawText
    var optA = ""
    var optB = ""
    var optC = ""
    var optD = ""
    var correctOpt = "Key"
    var selectedOpt: String? = null
    var explanation = ""

    // 1. Extract [Your Answer: Option X | Correct: Option Y]
    val ansMatch = Regex("""\[Your Answer:\s*(?:Option\s*)?([A-Za-z0-9]+)\s*\|\s*Correct:\s*(?:Option\s*)?([A-Za-z0-9]+)\]""").find(rawText)
    if (ansMatch != null) {
        val uAns = ansMatch.groupValues[1].trim()
        if (!uAns.equals("Unattempted", ignoreCase = true)) {
            selectedOpt = uAns
        }
        correctOpt = ansMatch.groupValues[2].trim()
    } else {
        val myAnsMatch = Regex("""(?:My Answer|Your Answer):\s*\(?([A-D])\)?""", RegexOption.IGNORE_CASE).find(rawText)
        if (myAnsMatch != null) selectedOpt = myAnsMatch.groupValues[1]

        val correctAnsMatch = Regex("""(?:Correct Answer|Key):\s*\(?([A-D])\)?""", RegexOption.IGNORE_CASE).find(rawText)
        if (correctAnsMatch != null) correctOpt = correctAnsMatch.groupValues[1]
    }

    // 2. Extract Solution
    val solMatch = Regex("""(?:💡\s*Solution:|Master Solution:|Explanation:)\s*([\s\S]+)$""").find(rawText)
    if (solMatch != null) {
        explanation = solMatch.groupValues[1].trim()
    }

    // 3. Extract Options (A), (B), (C), (D)
    val optAMatch = Regex("""(?m)^\s*[\(\[]?A[\)\]\.\:]\s*(.*?)$""").find(rawText)
    val optBMatch = Regex("""(?m)^\s*[\(\[]?B[\)\]\.\:]\s*(.*?)$""").find(rawText)
    val optCMatch = Regex("""(?m)^\s*[\(\[]?C[\)\]\.\:]\s*(.*?)$""").find(rawText)
    val optDMatch = Regex("""(?m)^\s*[\(\[]?D[\)\]\.\:]\s*(.*?)$""").find(rawText)

    if (optAMatch != null) optA = optAMatch.groupValues[1].trim()
    if (optBMatch != null) optB = optBMatch.groupValues[1].trim()
    if (optCMatch != null) optC = optCMatch.groupValues[1].trim()
    if (optDMatch != null) optD = optDMatch.groupValues[1].trim()

    // 4. Clean Question Text if items were extracted
    if (ansMatch != null || solMatch != null || optAMatch != null) {
        var cleaned = rawText
        if (ansMatch != null) cleaned = cleaned.replace(ansMatch.value, "")
        if (solMatch != null) cleaned = cleaned.replace(solMatch.value, "")
        if (optAMatch != null) cleaned = cleaned.replace(optAMatch.value, "")
        if (optBMatch != null) cleaned = cleaned.replace(optBMatch.value, "")
        if (optCMatch != null) cleaned = cleaned.replace(optCMatch.value, "")
        if (optDMatch != null) cleaned = cleaned.replace(optDMatch.value, "")
        cleaned = cleaned.trim()
        if (cleaned.isNotBlank()) {
            qText = cleaned
        }
    }

    return AiTestQuestion(
        id = (mistake.id ?: 1L).toInt(),
        subject = mistake.subject,
        chapter = mistake.mistakeType,
        questionText = qText,
        optionA = optA,
        optionB = optB,
        optionC = optC,
        optionD = optD,
        correctOption = correctOpt,
        selectedOption = selectedOpt,
        explanation = explanation
    )
}

private fun getMistakeDocFileName(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "document.pdf"
}

private fun getMistakeImageFileName(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "mistake_photo.jpg"
}
