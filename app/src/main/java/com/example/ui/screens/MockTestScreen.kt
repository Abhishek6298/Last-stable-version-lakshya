@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens
import androidx.compose.material.icons.automirrored.filled.*

import android.content.Intent
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveAtmosphericCapsuleChart
import com.example.ui.components.CapsuleChartDataPoint
import com.example.ui.components.FireworksProgressBar
import kotlin.math.roundToInt
import com.example.ui.components.AppSegmentedControl
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MockTest
import com.example.data.ScheduledMockTest
import com.example.data.TestSubjectCategory
import com.example.data.getSubjectCategory
import com.example.data.getSubjectScore
import com.example.data.isSingleSubjectTest
import com.example.data.getMaxScore
import com.example.ui.AppViewModel
import com.example.ui.components.MathJaxView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

data class MockMonthWeekPartition(
    val header: String,
    val fullLabel: String,
    val startDay: Int,
    val endDay: Int,
    val startMs: Long,
    val endMs: Long
)

@Composable
fun MockTestScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onNavigate: ((String) -> Unit)? = null,
    onNavigateBack: () -> Unit = {}
) {
    val tests by viewModel.mockTests.collectAsStateWithLifecycle()
    val scheduledTests by viewModel.scheduledMockTests.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val selectedTab by viewModel.mockTestSelectedTab.collectAsStateWithLifecycle()

    val isDark = isDarkMode
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val subTextMuted = if (isDark) Color(0x80FFFFFF) else Color(0xFF94A3B8)
    val cardBg = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
    val dialogBg = if (isDark) Color(0xFF000000) else Color.White
    
    var analyticsSubjectFilter by remember { mutableStateOf(TestSubjectCategory.FULL_MOCK) }
    var dialogTestType by remember { mutableStateOf(TestSubjectCategory.FULL_MOCK) }

    var showDialog by remember { mutableStateOf(false) }
    var editingTestId by remember { mutableStateOf<Int?>(null) }
    var editingTimestamp by remember { mutableStateOf<Long?>(null) }
    var testName by remember { mutableStateOf("") }
    var physics by remember { mutableStateOf("") }
    var chemistry by remember { mutableStateOf("") }
    var biology by remember { mutableStateOf("") }
    var customMaxMarks by remember { mutableStateOf("") }
    var negative by remember { mutableStateOf("") }
    var phyIncorrect by remember { mutableStateOf("") }
    var chemIncorrect by remember { mutableStateOf("") }
    var bioIncorrect by remember { mutableStateOf("") }
    var editingPdfUri by remember { mutableStateOf<String?>(null) }
    var editingPdfFileName by remember { mutableStateOf<String?>(null) }
    var editingGeminiAnalysis by remember { mutableStateOf<String?>(null) }
    var selectedTestForPdfUpload by remember { mutableStateOf<MockTest?>(null) }
    var showTestbookFullscreen by remember { mutableStateOf(false) }

    // State for scheduling upcoming mock test
    var showScheduleDialog by remember { mutableStateOf(false) }
    var editingSchedId by remember { mutableStateOf<Int?>(null) }
    var schedTitle by remember { mutableStateOf("") }
    var schedDate by remember { mutableStateOf("") }
    var schedPhy by remember { mutableStateOf("") }
    var schedChem by remember { mutableStateOf("") }
    var schedBio by remember { mutableStateOf("") }
    var schedPinned by remember { mutableStateOf(false) }

    // Intercept back presses when fullscreen or dialogs are open, or navigate back
    BackHandler(enabled = true) {
        when {
            showTestbookFullscreen -> showTestbookFullscreen = false
            showScheduleDialog -> showScheduleDialog = false
            showDialog -> showDialog = false
            selectedTab != 0 -> viewModel.updateMockTestSelectedTab(0)
            else -> onNavigateBack()
        }
    }
    
    var timeFilter by remember { mutableStateOf("Week") }
    var historySubjectFilter by remember { mutableStateOf<TestSubjectCategory?>(null) }

    val currentCal = remember { Calendar.getInstance() }
    val currentMockYear = remember { currentCal.get(Calendar.YEAR) }
    val currentMockMonth = remember { currentCal.get(Calendar.MONTH) }
    var selectedMockYear by remember { mutableStateOf(currentMockYear) }
    var selectedMockMonth by remember { mutableStateOf(currentMockMonth) }

    val (mockMonthStartMs, mockMonthEndMs, mockMonthLabel, mockMonthPartitions) = remember(selectedMockYear, selectedMockMonth) {
        val sCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedMockYear)
            set(Calendar.MONTH, selectedMockMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val maxD = sCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val eCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedMockYear)
            set(Calendar.MONTH, selectedMockMonth)
            set(Calendar.DAY_OF_MONTH, maxD)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(sCal.time)
        val mName = SimpleDateFormat("MMM", Locale.getDefault()).format(sCal.time)

        fun makePartition(header: String, partLabel: String, sDay: Int, eDay: Int): MockMonthWeekPartition {
            val partStart = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedMockYear)
                set(Calendar.MONTH, selectedMockMonth)
                set(Calendar.DAY_OF_MONTH, sDay)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val partEnd = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedMockYear)
                set(Calendar.MONTH, selectedMockMonth)
                set(Calendar.DAY_OF_MONTH, eDay)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            return MockMonthWeekPartition(header, partLabel, sDay, eDay, partStart, partEnd)
        }

        val parts = listOf(
            makePartition("W1", "1-7 $mName", 1, 7),
            makePartition("W2", "8-14 $mName", 8, 14),
            makePartition("W3", "15-21 $mName", 15, 21),
            makePartition("W4", "22-$maxD $mName", 22, maxD)
        )

        Quadruple(sCal.timeInMillis, eCal.timeInMillis, label, parts)
    }

    val isPastMockMonth = (selectedMockYear < currentMockYear || (selectedMockYear == currentMockYear && selectedMockMonth < currentMockMonth))
    val canGoNextMockMonth = !(selectedMockYear == currentMockYear && selectedMockMonth == currentMockMonth)

    val onPrevMockMonth = {
        if (selectedMockMonth == 0) {
            selectedMockYear -= 1
            selectedMockMonth = 11
        } else {
            selectedMockMonth -= 1
        }
    }
    val onNextMockMonth = {
        if (canGoNextMockMonth) {
            if (selectedMockMonth == 11) {
                selectedMockYear += 1
                selectedMockMonth = 0
            } else {
                selectedMockMonth += 1
            }
        }
    }
    val onResetMockMonth = {
        selectedMockYear = currentMockYear
        selectedMockMonth = currentMockMonth
    }

    val filteredTests = remember(tests, timeFilter, selectedMockYear, selectedMockMonth, mockMonthStartMs, mockMonthEndMs) {
        val cal = Calendar.getInstance()
        val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        if (timeFilter in monthNames) {
            val targetMonthIndex = monthNames.indexOf(timeFilter)
            tests.filter {
                val testCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                testCal.get(Calendar.MONTH) == targetMonthIndex && testCal.get(Calendar.YEAR) == selectedMockYear
            }.sortedBy { it.timestamp }
        } else if (timeFilter == "Month" || timeFilter == "Monthly") {
            tests.filter { it.timestamp in mockMonthStartMs..mockMonthEndMs }.sortedBy { it.timestamp }
        } else {
            val now = System.currentTimeMillis()
            val cutoff = when (timeFilter) {
                "Day" -> now - (24L * 60 * 60 * 1000)
                "Week", "Weekly" -> now - (7L * 24 * 60 * 60 * 1000)
                else -> 0L
            }
            if (cutoff > 0L) {
                tests.filter { it.timestamp >= cutoff }.sortedBy { it.timestamp }
            } else {
                tests.sortedBy { it.timestamp }
            }
        }
    }

    val historyDisplayedTests = remember(filteredTests, historySubjectFilter) {
        if (historySubjectFilter == null) filteredTests
        else when (historySubjectFilter) {
            TestSubjectCategory.FULL_MOCK -> filteredTests.filter { !it.isSingleSubjectTest() }
            TestSubjectCategory.PHYSICS -> filteredTests.filter { it.getSubjectCategory() == TestSubjectCategory.PHYSICS || it.physics > 0 }
            TestSubjectCategory.CHEMISTRY -> filteredTests.filter { it.getSubjectCategory() == TestSubjectCategory.CHEMISTRY || it.chemistry > 0 }
            TestSubjectCategory.BIOLOGY -> filteredTests.filter { it.getSubjectCategory() == TestSubjectCategory.BIOLOGY || it.biology > 0 }
            null -> filteredTests
        }
    }

    val syllabusAnalysisLoading by viewModel.syllabusAnalysisLoading.collectAsStateWithLifecycle()
    val syllabusAnalysisResult by viewModel.syllabusAnalysisResult.collectAsStateWithLifecycle()
    val syllabusText by viewModel.syllabusAnalyzerText.collectAsStateWithLifecycle()
    val syllabusFileUri by viewModel.syllabusAnalyzerUri.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        viewModel.updateSyllabusAnalyzerUri(uri)
    }

    val mockTestPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val targetTest = selectedTestForPdfUpload
        if (uri != null && targetTest != null) {
            val fileName = getFileNameFromUri(context, uri) ?: "Mock_Test_Paper.pdf"
            viewModel.analyzeMockTestPdf(context, targetTest, uri, fileName)
        }
        selectedTestForPdfUpload = null
    }

    val dialogPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            editingPdfUri = uri.toString()
            editingPdfFileName = getFileNameFromUri(context, uri) ?: "Mock_Test_Paper.pdf"
        }
    }

    if (selectedTab == 1) {
        AiTestScreen(
            viewModel = viewModel,
            onNavigateBack = { viewModel.updateMockTestSelectedTab(0) },
            modifier = modifier
        )
    } else {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                        Text("Mock Tests", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = textColor)
                        Text("Track past scores & analyze syllabus for upcoming tests.", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                AppSegmentedControl(
                    items = listOf("My Scores", "AI Test Engine ✨", "Syllabus AI"),
                    selectedItem = when (selectedTab) {
                        2 -> "Syllabus AI"
                        else -> "My Scores"
                    },
                    onItemSelected = {
                        viewModel.updateMockTestSelectedTab(
                            when (it) {
                                "AI Test Engine ✨" -> 1
                                "Syllabus AI" -> 2
                                else -> 0
                            }
                        )
                    },
                    itemLabel = { it },
                    itemIcon = {
                        when (it) {
                            "My Scores" -> Icons.Default.Analytics
                            "AI Test Engine ✨" -> Icons.Default.Psychology
                            else -> Icons.Default.AutoAwesome
                        }
                    },
                    selectedColor = Color(0xFF6D28D9),
                    selectedGradient = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
                    isDark = isDark,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (selectedTab == 2) {
                // SYLLABUS ANALYZER SECTION
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Mock Test Syllabus AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                            Text("Paste your syllabus topics or upload a PDF to get important topics, most asked questions, and PYQ trends.", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                                ) {
                                    Text("🤖 Lakshya AI Analysis", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = subTextColor, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), softWrap = false)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                                ) {
                                    Text("📈 PYQ Trends", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = subTextColor, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), softWrap = false)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = syllabusText,
                                onValueChange = { viewModel.updateSyllabusAnalyzerText(it) },
                                label = { Text("Type topics, description, or custom instructions here...") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = cardBorder,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { pdfLauncher.launch("application/pdf") },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0), contentColor = textColor),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upload PDF")
                                }
                                
                                if (syllabusFileUri != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = Color(0x3310B981),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("PDF Selected", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF10B981), modifier = Modifier.size(14.dp).clickable { viewModel.updateSyllabusAnalyzerUri(null) })
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { 
                                    viewModel.analyzeSyllabus(context, syllabusText, syllabusFileUri?.toString(), "pdf") 
                                },
                                enabled = (syllabusText.isNotBlank() || syllabusFileUri != null) && !syllabusAnalysisLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (syllabusAnalysisLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyze Syllabus with Lakshya AI", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                if (syllabusAnalysisResult.isNotBlank()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Analysis Result", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                                Spacer(modifier = Modifier.height(12.dp))
                                MathJaxView(
                                    text = syllabusAnalysisResult,
                                    isDark = isDark,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            } else {

            // 🧠 LAKSHYA AI CBT TEST ENGINE LAUNCHER HERO BANNER (NEET / JEE 39Y PYQ & PDF SCANNER)
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF38BDF8))
                            ),
                            RoundedCornerShape(22.dp)
                        )
                        .clickable { viewModel.updateMockTestSelectedTab(1) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🤖", fontSize = 22.sp)
                                }
                                Column {
                                    Text(
                                        "AI CBT TEST ENGINE",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = textColor,
                                        maxLines = 1,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.3.sp
                                    )
                                    Text(
                                        "Full Mock, Chapter CBT & PDF Extractor",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = subTextColor,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                                        ) {
                                            Text("⚡ CBT Ready", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = subTextColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), softWrap = false)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                                        ) {
                                            Text("📄 PDF Extractor", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = subTextColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), softWrap = false)
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF8B5CF6).copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f))
                            ) {
                                Text(
                                    "39Y PYQ ⚡",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                            ) {
                                Text("⏱️ Real Timer", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = subTextColor, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), softWrap = false)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                            ) {
                                Text("📄 PDF Scanner", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = subTextColor, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), softWrap = false)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                            ) {
                                Text("🔄 Auto Sync", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = subTextColor, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), softWrap = false)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { viewModel.updateMockTestSelectedTab(1) },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                    Text("📖 Chapter Drill", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFC084FC) else Color(0xFF6D28D9))
                                }
                            }
                            Surface(
                                onClick = { viewModel.updateMockTestSelectedTab(1) },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                    Text("🎯 Full Mock", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA))
                                }
                            }
                            Surface(
                                onClick = { viewModel.updateMockTestSelectedTab(1) },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                    Text("📄 PDF Scan", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.updateMockTestSelectedTab(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Take AI CBT Test", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // TESTBOOK AI LIVE ARENA LAUNCHER BANNER
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFFEC4899))
                            ),
                            RoundedCornerShape(22.dp)
                        )
                        .clickable { showTestbookFullscreen = true }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1)))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🌐", fontSize = 22.sp)
                                }
                                Column {
                                    Text(
                                        "TESTBOOK AI ARENA",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = textColor,
                                        maxLines = 1,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.3.sp
                                    )
                                    Text(
                                        "Full-Screen Web Test & Scorecard OCR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = subTextColor,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f))
                            ) {
                                Text(
                                    "LIVE ARENA ⚡",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                            ) {
                                Text("🖥️ Full Screen Arena", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = subTextColor, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), softWrap = false)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                            ) {
                                Text("📸 Lakshya Score OCR", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = subTextColor, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), softWrap = false)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showTestbookFullscreen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Open Testbook Arena", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // UPCOMING SCHEDULED MOCK TESTS CARD
            item {
                val activeScheduled = remember(scheduledTests) {
                    scheduledTests
                        .filter { !it.isCompleted }
                        .sortedWith(compareByDescending<ScheduledMockTest> { it.isPinned }.thenBy { it.scheduledDate })
                }
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            1.2.dp,
                            Brush.horizontalGradient(
                                listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899))
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
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    "📅 Scheduled Mock Tests",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textColor,
                                    fontSize = 17.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (activeScheduled.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF6366F1).copy(alpha = 0.18f),
                                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                "${activeScheduled.size} Upcoming",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                softWrap = false
                                            )
                                        }
                                    }
                                    Text(
                                        "Target exam dates & subject syllabus plans",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = subTextColor,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                                    ) {
                                        Text("🗓️ Smart Scheduler", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = subTextColor, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), softWrap = false)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                                    ) {
                                        Text("🔔 Auto Reminders", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = subTextColor, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), softWrap = false)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    editingSchedId = null
                                    schedTitle = ""
                                    schedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() + 7 * 86400000L))
                                    schedPhy = ""
                                    schedChem = ""
                                    schedBio = ""
                                    schedPinned = false
                                    showScheduleDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Schedule",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Schedule",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (activeScheduled.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) Color(0xFF131B2E) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🗓️", fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No upcoming mock tests scheduled!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Plan your next test date & syllabus targets to stay ahead.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = subTextColor,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                activeScheduled.forEach { st ->
                                    val daysDiff = remember(st.scheduledDate) {
                                        try {
                                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            val parsed = sdf.parse(st.scheduledDate)
                                            if (parsed != null) {
                                                val todayCal = Calendar.getInstance().apply {
                                                    set(Calendar.HOUR_OF_DAY, 0)
                                                    set(Calendar.MINUTE, 0)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                val testCal = Calendar.getInstance().apply {
                                                    time = parsed
                                                    set(Calendar.HOUR_OF_DAY, 0)
                                                    set(Calendar.MINUTE, 0)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                val diffMillis = testCal.timeInMillis - todayCal.timeInMillis
                                                (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                                            } else null
                                        } catch (e: Exception) { null }
                                    }

                                    val formattedTargetDate = remember(st.scheduledDate) {
                                        try {
                                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            val parsed = sdf.parse(st.scheduledDate)
                                            if (parsed != null) {
                                                SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(parsed)
                                            } else st.scheduledDate
                                        } catch (e: Exception) { st.scheduledDate }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (isDark) Color(0xFF131B2E) else Color(0xFFFFFFFF),
                                        border = if (st.isPinned) {
                                            BorderStroke(
                                                1.5.dp,
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFFF59E0B), Color(0xFFEAB308), Color(0xFFFBBF24))
                                                )
                                            )
                                        } else {
                                            BorderStroke(
                                                1.dp,
                                                if (isDark) Color(0xFF2A3756) else Color(0xFFE2E8F0)
                                            )
                                        },
                                        shadowElevation = if (st.isPinned) 4.dp else 2.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            // Top Title & Countdown Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = st.title,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = textColor,
                                                            fontSize = 15.sp
                                                        )
                                                        if (st.isPinned) {
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                                                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                                                            ) {
                                                                Text(
                                                                    "📌 PINNED",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Black,
                                                                    color = Color(0xFFF59E0B),
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                                    softWrap = false
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            "🗓️ $formattedTargetDate",
                                                            fontSize = 11.sp,
                                                            color = subTextColor,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        if (daysDiff != null) {
                                                            val (badgeText, badgeBg, badgeTextColor) = when {
                                                                daysDiff == 0 -> Triple("🔥 Today!", Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFEF4444))
                                                                daysDiff == 1 -> Triple("⚡ Tomorrow", Color(0xFF8B5CF6).copy(alpha = 0.15f), Color(0xFF8B5CF6))
                                                                daysDiff > 1 -> Triple("⏳ In $daysDiff days", Color(0xFF3B82F6).copy(alpha = 0.15f), Color(0xFF3B82F6))
                                                                else -> Triple("⚠️ Past Due (${-daysDiff}d)", Color(0xFFDC2626).copy(alpha = 0.15f), Color(0xFFDC2626))
                                                            }
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = badgeBg
                                                            ) {
                                                                Text(
                                                                    badgeText,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = badgeTextColor,
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                                    softWrap = false
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    // Pin / Unpin Button
                                                    IconButton(
                                                        onClick = { viewModel.togglePinScheduledMockTest(st) },
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (st.isPinned) Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                                else if (isDark) Color(0x226366F1)
                                                                else Color(0xFFEEF2FF)
                                                            )
                                                    ) {
                                                        Icon(
                                                            imageVector = if (st.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                                            contentDescription = if (st.isPinned) "Unpin Test" else "Pin Test to Top",
                                                            tint = if (st.isPinned) Color(0xFFF59E0B) else (if (isDark) Color(0xFFA5B4FC) else Color(0xFF6366F1)),
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            editingSchedId = st.id
                                                            schedTitle = st.title
                                                            schedDate = st.scheduledDate
                                                            schedPhy = st.physicsSyllabus
                                                            schedChem = st.chemistrySyllabus
                                                            schedBio = st.biologySyllabus
                                                            schedPinned = st.isPinned
                                                            showScheduleDialog = true
                                                        },
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isDark) Color(0x226366F1) else Color(0xFFEEF2FF))
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF6366F1), modifier = Modifier.size(15.dp))
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.deleteScheduledMockTest(st.id) },
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isDark) Color(0x22EF4444) else Color(0xFFFEF2F2))
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            
                                            // Subject-wise syllabus display cards
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                if (st.physicsSyllabus.isNotBlank()) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isDark) Color(0x183B82F6) else Color(0xFFEFF6FF))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("⚡ Physics: ", fontSize = 11.sp, color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                                                        Text(st.physicsSyllabus, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Normal, modifier = Modifier.weight(1f))
                                                    }
                                                }
                                                if (st.biologySyllabus.isNotBlank()) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isDark) Color(0x1810B981) else Color(0xFFECFDF5))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("🧬 Biology: ", fontSize = 11.sp, color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857), fontWeight = FontWeight.Bold)
                                                        Text(st.biologySyllabus, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Normal, modifier = Modifier.weight(1f))
                                                    }
                                                }
                                                if (st.chemistrySyllabus.isNotBlank()) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isDark) Color(0x18F59E0B) else Color(0xFFFFFBEB))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("🧪 Chemistry: ", fontSize = 11.sp, color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309), fontWeight = FontWeight.Bold)
                                                        Text(st.chemistrySyllabus, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Normal, modifier = Modifier.weight(1f))
                                                    }
                                                }
                                                if (st.physicsSyllabus.isBlank() && st.chemistrySyllabus.isBlank() && st.biologySyllabus.isBlank() && st.syllabusNotes.isNotBlank()) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isDark) Color(0x18FFFFFF) else Color(0xFFF8FAFC))
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("📖 Syllabus: ", fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Bold)
                                                        Text(st.syllabusNotes, fontSize = 11.sp, color = subTextColor, fontWeight = FontWeight.Normal, modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Quick Launch in CBT Engine
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF6366F1).copy(alpha = 0.1f),
                                                border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.updateMockTestSelectedTab(1) }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text("🚀", fontSize = 12.sp)
                                                        Text(
                                                            "Take Mock in AI CBT Engine",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF6366F1)
                                                        )
                                                    }
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = "Take Test",
                                                        tint = Color(0xFF6366F1),
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
            }

            // Score Progression Interactive Capsule Chart with Subject Selector
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Text(
                        text = "ANALYTICS SUBJECT FILTER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = subTextColor,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filterOptions = listOf(
                            Triple(TestSubjectCategory.FULL_MOCK, "🏆 Full Mock (720M)", Color(0xFF6366F1)),
                            Triple(TestSubjectCategory.PHYSICS, "⚡ Physics (180M)", Color(0xFF3B82F6)),
                            Triple(TestSubjectCategory.CHEMISTRY, "🧪 Chemistry (180M)", Color(0xFFF59E0B)),
                            Triple(TestSubjectCategory.BIOLOGY, "🧬 Biology (360M)", Color(0xFF10B981))
                        )
                        filterOptions.forEach { (cat, label, themeColor) ->
                            val isSelected = analyticsSubjectFilter == cat
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) themeColor else (if (isDark) Color(0x1EFFFFFF) else Color(0xFFF1F5F9)),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) themeColor else (if (isDark) Color(0x28FFFFFF) else Color(0xFFE2E8F0))
                                ),
                                modifier = Modifier.clickable { analyticsSubjectFilter = cat }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                val isJeeExam = remember(filteredTests) {
                    filteredTests.any { it.testName.contains("JEE", true) || it.testName.contains("IIT", true) || (it.geminiAnalysis ?: "").contains("JEE", true) }
                }

                // Strictly filter tests based on selected subject category:
                // Full mock: strictly EXCLUDES single subject tests!
                // Subject filter: includes tests with scores for that subject
                val subjectFilteredTests = remember(filteredTests, analyticsSubjectFilter) {
                    when (analyticsSubjectFilter) {
                        TestSubjectCategory.FULL_MOCK -> filteredTests.filter { !it.isSingleSubjectTest() }
                        TestSubjectCategory.PHYSICS -> filteredTests.filter { it.getSubjectCategory() == TestSubjectCategory.PHYSICS || it.physics > 0 }
                        TestSubjectCategory.CHEMISTRY -> filteredTests.filter { it.getSubjectCategory() == TestSubjectCategory.CHEMISTRY || it.chemistry > 0 }
                        TestSubjectCategory.BIOLOGY -> filteredTests.filter { it.getSubjectCategory() == TestSubjectCategory.BIOLOGY || it.biology > 0 }
                    }
                }

                val chartData = remember(subjectFilteredTests, timeFilter, selectedMockYear, selectedMockMonth, mockMonthStartMs, mockMonthEndMs) {
                    val nowMs = System.currentTimeMillis()
                    when (timeFilter) {
                        "Day" -> subjectFilteredTests.sortedBy { it.timestamp }.takeLast(7)
                        "Week" -> {
                            val inWeek = subjectFilteredTests.filter { (nowMs - it.timestamp) <= 7L * 24 * 3600 * 1000L }
                            if (inWeek.isNotEmpty()) inWeek else subjectFilteredTests.sortedBy { it.timestamp }.takeLast(7)
                        }
                        else -> { // Month
                            subjectFilteredTests.filter { it.timestamp in mockMonthStartMs..mockMonthEndMs }.sortedBy { it.timestamp }
                        }
                    }
                }

                val targetMaxScore = when (analyticsSubjectFilter) {
                    TestSubjectCategory.FULL_MOCK -> if (isJeeExam) 300f else 720f
                    TestSubjectCategory.PHYSICS -> if (isJeeExam) 100f else 180f
                    TestSubjectCategory.CHEMISTRY -> if (isJeeExam) 100f else 180f
                    TestSubjectCategory.BIOLOGY -> if (isJeeExam) 100f else 360f
                }

                val mockChartPoints = remember(subjectFilteredTests, timeFilter, analyticsSubjectFilter, targetMaxScore, isJeeExam, selectedMockYear, selectedMockMonth, mockMonthPartitions, mockMonthStartMs, mockMonthEndMs) {
                    val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val sdfDay = SimpleDateFormat("EEE", Locale.getDefault())
                    val sdfDate = SimpleDateFormat("dd MMM", Locale.getDefault())
                    
                    val maxSub = if (isJeeExam) 100f else 180f
                    val maxBio = if (isJeeExam) 100f else 360f

                    when (timeFilter) {
                        "Day" -> {
                            val recent = subjectFilteredTests.sortedBy { it.timestamp }.takeLast(7)
                            if (recent.isEmpty()) {
                                listOf("T1", "T2", "T3", "T4", "T5").mapIndexed { idx, name ->
                                    CapsuleChartDataPoint(
                                        id = "mock_day_empty_$idx",
                                        dayLetter = name,
                                        fullDate = "${analyticsSubjectFilter.displayName} Test ${idx + 1}",
                                        value = 0f,
                                        displayValue = "No Test",
                                        isAboveAverage = false,
                                        detailNotes = "No ${analyticsSubjectFilter.displayName} test logged yet"
                                    )
                                }
                            } else {
                                val avg = recent.map { it.getSubjectScore(analyticsSubjectFilter) }.average().toFloat().coerceAtLeast(1f)
                                recent.mapIndexed { idx, t ->
                                    val sc = t.getSubjectScore(analyticsSubjectFilter)
                                    val isSingle = t.isSingleSubjectTest()
                                    val testMax = if (analyticsSubjectFilter == TestSubjectCategory.FULL_MOCK) t.getMaxScore(isJeeExam) else targetMaxScore.toInt()
                                    CapsuleChartDataPoint(
                                        id = "mock_t_${t.id}_$idx",
                                        dayLetter = "T${idx + 1}",
                                        fullDate = "${t.testName} (${sdfDate.format(Date(t.timestamp))})",
                                        value = sc.toFloat(),
                                        displayValue = "$sc/$testMax",
                                        isAboveAverage = sc >= avg,
                                        detailNotes = if (isSingle) {
                                            "${analyticsSubjectFilter.displayName} Only • Score: $sc/$testMax"
                                        } else if (analyticsSubjectFilter == TestSubjectCategory.FULL_MOCK) {
                                            "Phy: ${t.physics}, Chem: ${t.chemistry}, ${if (isJeeExam) "Math" else "Bio"}: ${t.biology}${if (t.negative > 0) " • -${t.negative} Neg" else ""}"
                                        } else {
                                            "${analyticsSubjectFilter.displayName} Score: $sc/$testMax (Total: ${t.score})"
                                        },
                                        subMetric1 = if (t.physics > 0) (t.physics.toFloat() / maxSub * 100f).coerceIn(0f, 100f) else 0f,
                                        subMetric2 = if (t.chemistry > 0) (t.chemistry.toFloat() / maxSub * 100f).coerceIn(0f, 100f) else 0f,
                                        subMetric3 = if (t.biology > 0) (t.biology.toFloat() / maxBio * 100f).coerceIn(0f, 100f) else 0f
                                    )
                                }
                            }
                        }
                        "Week" -> {
                            val points = mutableListOf<CapsuleChartDataPoint>()
                            val nowMs = System.currentTimeMillis()
                            val weekTests = subjectFilteredTests.filter { (nowMs - it.timestamp) <= 7L * 24 * 3600 * 1000L }
                            val avg = if (weekTests.isNotEmpty()) weekTests.map { it.getSubjectScore(analyticsSubjectFilter) }.average().toFloat() else (targetMaxScore * 0.65f)
                            for (i in 6 downTo 0) {
                                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                                val dayKey = sdfKey.format(c.time)
                                val dayLabel = sdfDay.format(c.time).take(3)
                                val dateLabel = sdfDate.format(c.time)
                                val dayTest = subjectFilteredTests.filter { sdfKey.format(Date(it.timestamp)) == dayKey }.maxByOrNull { it.getSubjectScore(analyticsSubjectFilter) }
                                val sc = dayTest?.getSubjectScore(analyticsSubjectFilter) ?: 0
                                val isSingle = dayTest?.isSingleSubjectTest() == true
                                val testMax = if (dayTest != null) {
                                    if (analyticsSubjectFilter == TestSubjectCategory.FULL_MOCK) dayTest.getMaxScore(isJeeExam) else targetMaxScore.toInt()
                                } else targetMaxScore.toInt()
                                points.add(
                                    CapsuleChartDataPoint(
                                        id = "mock_week_$dayKey",
                                        dayLetter = dayLabel,
                                        fullDate = if (dayTest != null) "${dayTest.testName} • ${sdfDate.format(Date(dayTest.timestamp))}" else "$dayLabel, $dateLabel • No Test",
                                        value = sc.toFloat(),
                                        displayValue = if (dayTest != null) "$sc/$testMax" else "0/$testMax",
                                        isAboveAverage = sc >= avg && sc > 0,
                                        detailNotes = if (dayTest != null) {
                                            if (isSingle) "${analyticsSubjectFilter.displayName} Only • $sc/$testMax"
                                            else if (analyticsSubjectFilter == TestSubjectCategory.FULL_MOCK) "Phy: ${dayTest.physics}, Chem: ${dayTest.chemistry}, ${if (isJeeExam) "Math" else "Bio"}: ${dayTest.biology}"
                                            else "${analyticsSubjectFilter.displayName} Score: $sc/$testMax (Total: ${dayTest.score})"
                                        } else "No ${analyticsSubjectFilter.displayName} test on $dateLabel",
                                        subMetric1 = if (dayTest != null && dayTest.physics > 0) (dayTest.physics.toFloat() / maxSub * 100f).coerceIn(0f, 100f) else 0f,
                                        subMetric2 = if (dayTest != null && dayTest.chemistry > 0) (dayTest.chemistry.toFloat() / maxSub * 100f).coerceIn(0f, 100f) else 0f,
                                        subMetric3 = if (dayTest != null && dayTest.biology > 0) (dayTest.biology.toFloat() / maxBio * 100f).coerceIn(0f, 100f) else 0f
                                    )
                                )
                            }
                            points
                        }
                        else -> { // "Month"
                            val testsInMonth = subjectFilteredTests.filter { it.timestamp in mockMonthStartMs..mockMonthEndMs }
                            val avg = if (testsInMonth.isNotEmpty()) testsInMonth.map { it.getSubjectScore(analyticsSubjectFilter) }.average().toFloat() else (targetMaxScore * 0.65f)
                            mockMonthPartitions.mapIndexed { idx, part ->
                                val inWeek = testsInMonth.filter { it.timestamp in part.startMs..part.endMs }
                                val sc = if (inWeek.isNotEmpty()) inWeek.map { it.getSubjectScore(analyticsSubjectFilter) }.average().roundToInt() else 0
                                val avgPhy = if (inWeek.isNotEmpty()) (inWeek.map { it.physics }.average() / maxSub * 100).toFloat() else 0f
                                val avgChem = if (inWeek.isNotEmpty()) (inWeek.map { it.chemistry }.average() / maxSub * 100).toFloat() else 0f
                                val avgBio = if (inWeek.isNotEmpty()) (inWeek.map { it.biology }.average() / maxBio * 100).toFloat() else 0f
                                CapsuleChartDataPoint(
                                    id = "mock_month_${selectedMockYear}_${selectedMockMonth}_$idx",
                                    dayLetter = part.header,
                                    fullDate = "${part.fullLabel} • ${if (inWeek.isNotEmpty()) "${inWeek.size} Tests" else "No Tests"}",
                                    value = sc.toFloat(),
                                    displayValue = if (sc > 0) "$sc/${targetMaxScore.toInt()}" else "0/${targetMaxScore.toInt()}",
                                    isAboveAverage = sc >= avg && sc > 0,
                                    detailNotes = if (inWeek.isNotEmpty()) "${inWeek.size} tests • Avg: $sc/${targetMaxScore.toInt()}" else "No tests recorded in this week",
                                    subMetric1 = avgPhy,
                                    subMetric2 = avgChem,
                                    subMetric3 = avgBio
                                )
                            }
                        }
                    }
                }

                val currentScores = remember(chartData, analyticsSubjectFilter) {
                    chartData.map { it.getSubjectScore(analyticsSubjectFilter) }
                }

                val avgScore = remember(currentScores, targetMaxScore) {
                    if (currentScores.isNotEmpty()) "${currentScores.average().roundToInt()} / ${targetMaxScore.toInt()}" else "0 / ${targetMaxScore.toInt()}"
                }
                val avgP = remember(chartData, isJeeExam) {
                    if (chartData.isNotEmpty()) {
                        chartData.map { t ->
                            (t.physics.toDouble() / (if (isJeeExam) 100.0 else 180.0) * 100.0)
                        }.average().toInt().coerceIn(0, 100)
                    } else 0
                }
                val avgC = remember(chartData, isJeeExam) {
                    if (chartData.isNotEmpty()) {
                        chartData.map { t ->
                            (t.chemistry.toDouble() / (if (isJeeExam) 100.0 else 180.0) * 100.0)
                        }.average().toInt().coerceIn(0, 100)
                    } else 0
                }
                val avgB = remember(chartData, isJeeExam) {
                    if (chartData.isNotEmpty()) {
                        chartData.map { t ->
                            (t.biology.toDouble() / (if (isJeeExam) 100.0 else 360.0) * 100.0)
                        }.average().toInt().coerceIn(0, 100)
                    } else 0
                }

                val qualityRating = remember(currentScores, targetMaxScore) {
                    if (currentScores.isEmpty()) "Awaiting Tests 🎯"
                    else {
                        val pct = (currentScores.average() / targetMaxScore) * 100
                        when {
                            pct >= 80 -> "Target Achieved 🏆"
                            pct >= 65 -> "High Accuracy 🎯"
                            pct >= 50 -> "Good Progress 📈"
                            else -> "Foundation Building ⚡"
                        }
                    }
                }

                InteractiveAtmosphericCapsuleChart(
                    title = when (analyticsSubjectFilter) {
                        TestSubjectCategory.FULL_MOCK -> if (timeFilter == "Month") "Score Progression (${targetMaxScore.toInt()}M) • $mockMonthLabel" else "Score Progression (${targetMaxScore.toInt()}M)"
                        else -> if (timeFilter == "Month") "${analyticsSubjectFilter.displayName} Progression (${targetMaxScore.toInt()}M) • $mockMonthLabel" else "${analyticsSubjectFilter.displayName} Progression (${targetMaxScore.toInt()}M)"
                    },
                    subtitle = when (analyticsSubjectFilter) {
                        TestSubjectCategory.FULL_MOCK -> "Full-length NEET mock tests (Excludes single-subject tests) • Touch to inspect"
                        else -> "${analyticsSubjectFilter.displayName} single subject & sectional tests • Touch to inspect"
                    },
                    selectedPeriod = if (timeFilter in listOf("Day", "Week", "Month")) timeFilter else "Week",
                    onPeriodSelected = { timeFilter = it },
                    dataPoints = mockChartPoints,
                    averageDisplay = avgScore,
                    qualityScore = if (currentScores.isNotEmpty()) "${((currentScores.average() / targetMaxScore) * 100).roundToInt()}%" else "0%",
                    qualityRating = qualityRating,
                    gauge1Label = if (analyticsSubjectFilter == TestSubjectCategory.FULL_MOCK) "Physics" else "Phy Avg",
                    gauge1Percent = avgP,
                    gauge2Label = if (analyticsSubjectFilter == TestSubjectCategory.FULL_MOCK) "Chemistry" else "Chem Avg",
                    gauge2Percent = avgC,
                    gauge3Label = if (isJeeExam) "Maths" else (if (analyticsSubjectFilter == TestSubjectCategory.FULL_MOCK) "Biology" else "Bio Avg"),
                    gauge3Percent = avgB,
                    maxValue = targetMaxScore,
                    topGuideLabel = "${targetMaxScore.toInt()} M",
                    bottomGuideLabel = "0 M",
                    isDark = isDark,
                    monthLabel = mockMonthLabel,
                    isPastMonth = isPastMockMonth,
                    canGoNextMonth = canGoNextMockMonth,
                    canGoPrevMonth = true,
                    onPrevMonth = onPrevMockMonth,
                    onNextMonth = onNextMockMonth,
                    onResetToCurrentMonth = onResetMockMonth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (tests.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Test History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = textColor)
                            Text("Past mock tests & sectional records (${historyDisplayedTests.size})", style = MaterialTheme.typography.bodyMedium, color = subTextColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Subject History Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val historyFilters = listOf(
                            null to "All Tests (${filteredTests.size})",
                            TestSubjectCategory.FULL_MOCK to "🏆 Full Mock",
                            TestSubjectCategory.PHYSICS to "⚡ Physics",
                            TestSubjectCategory.CHEMISTRY to "🧪 Chemistry",
                            TestSubjectCategory.BIOLOGY to "🧬 Biology"
                        )
                        historyFilters.forEach { (cat, label) ->
                            val isSelected = historySubjectFilter == cat
                            val activeColor = when (cat) {
                                TestSubjectCategory.PHYSICS -> Color(0xFF3B82F6)
                                TestSubjectCategory.CHEMISTRY -> Color(0xFFF59E0B)
                                TestSubjectCategory.BIOLOGY -> Color(0xFF10B981)
                                else -> Color(0xFF6366F1)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) activeColor else (if (isDark) Color(0x1EFFFFFF) else Color(0xFFF1F5F9)),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) activeColor else (if (isDark) Color(0x28FFFFFF) else Color(0xFFCBD5E1))
                                ),
                                modifier = Modifier.clickable { historySubjectFilter = cat }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                
                items(historyDisplayedTests.reversed(), key = { it.id }) { test ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        val detectedMax = remember(test.testName, test.geminiAnalysis, test.physics, test.chemistry, test.biology) {
                            test.getMaxScore()
                        }

                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(test.testName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                                        Text("(${sdf.format(Date(test.timestamp))})", style = MaterialTheme.typography.labelSmall, color = subTextMuted)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val subCat = test.getSubjectCategory()
                                        val badgeColor = when (subCat) {
                                            TestSubjectCategory.PHYSICS -> Color(0xFF3B82F6)
                                            TestSubjectCategory.CHEMISTRY -> Color(0xFFF59E0B)
                                            TestSubjectCategory.BIOLOGY -> Color(0xFF10B981)
                                            else -> Color(0xFF6366F1)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = badgeColor.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                if (subCat == TestSubjectCategory.FULL_MOCK) "🏆 Full Mock (${detectedMax}M)" else "${subCat.displayName} (${detectedMax}M)",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                softWrap = false
                                            )
                                        }

                                        if (test.geminiAnalysis != null && test.geminiAnalysis.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                                            ) {
                                                Text("📸 AI OCR Used", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = subTextColor, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), softWrap = false)
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                                        ) {
                                            Text("📊 Detailed Breakdown", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = subTextColor, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), softWrap = false)
                                        }
                                    }
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF6366F1), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("${test.score} / $detectedMax", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    
                                    // Edit Button
                                    IconButton(
                                        onClick = {
                                            editingTestId = test.id
                                            editingTimestamp = test.timestamp
                                            testName = test.testName
                                            physics = test.physics.toString()
                                            chemistry = test.chemistry.toString()
                                            biology = test.biology.toString()
                                            negative = test.negative.toString()
                                            dialogTestType = test.getSubjectCategory()

                                            val negMatch = Regex("""Negative Breakdown:\s*Phy:\s*-?(\d+),\s*Chem:\s*-?(\d+),\s*(?:Bio|Maths?):\s*-?(\d+)""").find(test.geminiAnalysis ?: "")
                                            if (negMatch != null) {
                                                phyIncorrect = if (negMatch.groupValues[1] != "0") negMatch.groupValues[1] else ""
                                                chemIncorrect = if (negMatch.groupValues[2] != "0") negMatch.groupValues[2] else ""
                                                bioIncorrect = if (negMatch.groupValues[3] != "0") negMatch.groupValues[3] else ""
                                            } else {
                                                phyIncorrect = ""
                                                chemIncorrect = ""
                                                bioIncorrect = ""
                                            }

                                            editingPdfUri = test.pdfUri
                                            editingPdfFileName = test.pdfFileName
                                            editingGeminiAnalysis = test.geminiAnalysis
                                            customMaxMarks = test.getMaxScore().toString()
                                            showDialog = true
                                        },
                                        modifier = Modifier.size(36.dp).padding(start = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit test", tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp))
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = { viewModel.deleteMockTest(test.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete test", tint = Color(0xFFF43F5E), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            val scoreFrac = if (detectedMax > 0) (test.score.toFloat() / detectedMax.toFloat()).coerceIn(0f, 1f) else 0f
                            FireworksProgressBar(
                                progress = scoreFrac,
                                height = 7.dp,
                                gradientColors = if (scoreFrac >= 0.75f) listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFFFFD600))
                                                 else listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFFFD600)),
                                sparkColor = if (scoreFrac >= 0.75f) Color(0xFF10B981) else Color(0xFF6366F1),
                                isDark = isDark,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            HorizontalDivider(color = cardBorder, modifier = Modifier.padding(bottom = 12.dp))
                            
                            val isJee = remember(test.testName, test.geminiAnalysis) {
                                test.testName.contains("JEE", ignoreCase = true) || 
                                test.testName.contains("IIT", ignoreCase = true) || 
                                (test.geminiAnalysis ?: "").contains("JEE", ignoreCase = true) || 
                                (test.geminiAnalysis ?: "").contains("Math", ignoreCase = true)
                            }

                            val (phyMax, chemMax, bioMax) = remember(test.geminiAnalysis, isJee, test.testName) {
                                val match = Regex("""Sectional Breakdown:\s*Phy:\s*\d+/(\d+),\s*Chem:\s*\d+/(\d+),\s*(?:Bio|Maths?):\s*\d+/(\d+)""").find(test.geminiAnalysis ?: "")
                                if (match != null) {
                                    Triple(
                                        match.groupValues[1].toIntOrNull() ?: if (isJee) 100 else 180,
                                        match.groupValues[2].toIntOrNull() ?: if (isJee) 100 else 180,
                                        match.groupValues[3].toIntOrNull() ?: if (isJee) 100 else 360
                                    )
                                } else {
                                    if (isJee) {
                                        if (test.testName.contains("30Q", ignoreCase = true)) Triple(40, 40, 40)
                                        else Triple(100, 100, 100)
                                    } else {
                                        Triple(180, 180, 360)
                                    }
                                }
                            }

                            val (phyNeg, chemNeg, bioNeg) = remember(test.geminiAnalysis) {
                                val match = Regex("""Negative Breakdown:\s*Phy:\s*-?(\d+),\s*Chem:\s*-?(\d+),\s*(?:Bio|Maths?):\s*-?(\d+)""").find(test.geminiAnalysis ?: "")
                                if (match != null) {
                                    Triple(
                                        match.groupValues[1].toIntOrNull() ?: 0,
                                        match.groupValues[2].toIntOrNull() ?: 0,
                                        match.groupValues[3].toIntOrNull() ?: 0
                                    )
                                } else {
                                    Triple(0, 0, 0)
                                }
                            }

                            val cardSubCat = test.getSubjectCategory()

                            // Conditional subject score rows: ONLY display the subjects relevant to this mock test
                            if (cardSubCat == TestSubjectCategory.FULL_MOCK || cardSubCat == TestSubjectCategory.PHYSICS) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("⚡ Physics", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB))
                                    Text("${test.physics} / $phyMax", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = textColor)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            if (cardSubCat == TestSubjectCategory.FULL_MOCK || cardSubCat == TestSubjectCategory.CHEMISTRY) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("🧪 Chemistry", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706))
                                    Text("${test.chemistry} / $chemMax", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = textColor)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            if (cardSubCat == TestSubjectCategory.FULL_MOCK || cardSubCat == TestSubjectCategory.BIOLOGY) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isJee) "📐 Mathematics" else "🧬 Biology", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF34D399) else Color(0xFF059669))
                                    Text("${test.biology} / $bioMax", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = textColor)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // Dedicated Negative Penalty Banner in History Card
                            if (test.negative > 0 || (phyNeg > 0 || chemNeg > 0 || bioNeg > 0)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.1f),
                                    border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("❌ Negative Penalty:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                            if (cardSubCat == TestSubjectCategory.FULL_MOCK) {
                                                if (phyNeg > 0 || chemNeg > 0 || bioNeg > 0) {
                                                    Text(
                                                        if (isJee) "(P:-$phyNeg, C:-$chemNeg, M:-$bioNeg)" else "(P:-$phyNeg, C:-$chemNeg, B:-$bioNeg)",
                                                        fontSize = 11.sp,
                                                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else {
                                                val subLabel = when (cardSubCat) {
                                                    TestSubjectCategory.PHYSICS -> "Physics"
                                                    TestSubjectCategory.CHEMISTRY -> "Chemistry"
                                                    TestSubjectCategory.BIOLOGY -> if (isJee) "Maths" else "Biology"
                                                    else -> ""
                                                }
                                                if (subLabel.isNotEmpty()) {
                                                    Text("($subLabel)", fontSize = 11.sp, color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        val displayNeg = if (test.negative > 0) test.negative else (phyNeg + chemNeg + bioNeg)
                                        Text(
                                            "-$displayNeg Marks",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }

                            // Attached PDF Chip
                            if (!test.pdfUri.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF6366F1).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = test.pdfFileName ?: "Test_Paper.pdf",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textColor,
                                                maxLines = 1
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                try {
                                                    val uri = Uri.parse(test.pdfUri)
                                                    viewModel.openPdfInReader(uri, test.pdfFileName ?: test.testName)
                                                    if (onNavigate != null) {
                                                        onNavigate("pdf_viewer")
                                                    } else {
                                                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                                            setDataAndType(uri, "application/pdf")
                                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        }
                                                        context.startActivity(Intent.createChooser(intent, "Open Mock Test PDF"))
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "No app found to open PDF", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("View PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                                        }
                                    }
                                }
                            }

                            // Gemini Exam Level Analysis Section
                            if (!test.geminiAnalysis.isNullOrBlank()) {
                                var isExpanded by remember { mutableStateOf(false) }
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0xFF1E1B2E) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = "Lakshya AI", tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(if (isJee) "Lakshya JEE Level Analysis" else "Lakshya NEET Level Analysis", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9))
                                            }
                                            Text(
                                                if (isExpanded) "Hide ▲" else "View Analysis ▼",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF8B5CF6)
                                            )
                                        }

                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            MathJaxView(
                                                text = test.geminiAnalysis,
                                                isDark = isDark,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            // Action button: Upload PDF / Analyze
                            val analyzingId by viewModel.analyzingMockTestId.collectAsStateWithLifecycle()
                            val isAnalyzingThis = analyzingId == test.id

                            if (isAnalyzingThis) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF8B5CF6))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Lakshya analyzing paper level vs ${if (isJee) "JEE" else "NEET"}...", fontSize = 12.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        if (test.pdfUri != null) {
                                            viewModel.analyzeMockTestPdf(context, test, Uri.parse(test.pdfUri), test.pdfFileName ?: "Mock_Paper.pdf")
                                        } else {
                                            selectedTestForPdfUpload = test
                                            mockTestPdfLauncher.launch("application/pdf")
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            imageVector = if (test.pdfUri == null) Icons.Default.PictureAsPdf else Icons.Default.AutoAwesome,
                                            contentDescription = "Action",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (test.pdfUri == null) {
                                                if (isJee) "📄 Upload PDF & Compare Level with JEE" else "📄 Upload PDF & Compare Level with NEET"
                                            } else if (test.geminiAnalysis == null) {
                                                if (isJee) "✨ Compare Level with JEE Paper" else "✨ Compare Level with NEET Paper"
                                            } else {
                                                if (isJee) "🔄 Re-Analyze JEE Level" else "🔄 Re-Analyze NEET Level"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            } // close if selectedTab == 0

            item {
                com.example.ui.components.AppBrandingFooter(isDark = isDark)
            }
        }
        
        if (selectedTab == 0) {
            FloatingActionButton(
                onClick = { 
                    editingTestId = null
                    editingTimestamp = null
                    testName = ""
                    physics = ""
                    chemistry = ""
                    biology = ""
                    phyIncorrect = ""
                    chemIncorrect = ""
                    bioIncorrect = ""
                    negative = ""
                    editingPdfUri = null
                    editingPdfFileName = null
                    editingGeminiAnalysis = null
                    customMaxMarks = ""
                    showDialog = true 
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp),
                shape = RoundedCornerShape(18.dp),
                containerColor = Color(0xFF6366F1),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Mock Test", modifier = Modifier.size(22.dp))
                    Text("Add Test", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        
        if (showDialog) {
            val livePhy = physics.toIntOrNull() ?: 0
            val liveChem = chemistry.toIntOrNull() ?: 0
            val liveBio = biology.toIntOrNull() ?: 0
            val livePhyInc = phyIncorrect.toIntOrNull() ?: 0
            val liveChemInc = chemIncorrect.toIntOrNull() ?: 0
            val liveBioInc = bioIncorrect.toIntOrNull() ?: 0
            val liveTotalInc = when (dialogTestType) {
                TestSubjectCategory.PHYSICS -> livePhyInc
                TestSubjectCategory.CHEMISTRY -> liveChemInc
                TestSubjectCategory.BIOLOGY -> liveBioInc
                else -> livePhyInc + liveChemInc + liveBioInc
            }
            val liveNeg = if (liveTotalInc > 0) liveTotalInc else (negative.toIntOrNull() ?: 0)
            val liveTotal = when (dialogTestType) {
                TestSubjectCategory.PHYSICS -> (livePhy - livePhyInc).coerceAtLeast(0)
                TestSubjectCategory.CHEMISTRY -> (liveChem - liveChemInc).coerceAtLeast(0)
                TestSubjectCategory.BIOLOGY -> (liveBio - liveBioInc).coerceAtLeast(0)
                else -> (livePhy + liveChem + liveBio - liveNeg).coerceAtLeast(0)
            }

            val isDialogJee = remember(testName) { testName.contains("JEE", true) || testName.contains("IIT", true) }
            val defaultCategoryMax = when (dialogTestType) {
                TestSubjectCategory.FULL_MOCK -> if (isDialogJee) 300 else 720
                TestSubjectCategory.PHYSICS -> if (isDialogJee) 100 else 180
                TestSubjectCategory.CHEMISTRY -> if (isDialogJee) 100 else 180
                TestSubjectCategory.BIOLOGY -> if (isDialogJee) 100 else 360
            }
            val dialogMaxScore = customMaxMarks.toIntOrNull()?.takeIf { it > 0 } ?: defaultCategoryMax

            AlertDialog(
                onDismissRequest = { 
                    showDialog = false 
                    dialogTestType = TestSubjectCategory.FULL_MOCK
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                shape = RoundedCornerShape(24.dp),
                containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                tonalElevation = 8.dp,
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFF6366F1).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (editingTestId != null) Icons.Default.Edit else Icons.Default.Score,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (editingTestId != null) "Edit Mock Test" else "Log Mock Test Score",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = textColor
                                )
                                Text(
                                    text = if (dialogTestType == TestSubjectCategory.FULL_MOCK) "Enter subject marks & track accuracy" else "Logging ${dialogTestType.displayName} specific test",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subTextColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Test Type Mode Selector (Single Subject vs Full Mock)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "TEST CATEGORY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = subTextColor,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val typeOptions = listOf(
                                    Triple(TestSubjectCategory.FULL_MOCK, "🏆 Full (720M)", Color(0xFF6366F1)),
                                    Triple(TestSubjectCategory.PHYSICS, "⚡ Phy Only (180M)", Color(0xFF3B82F6)),
                                    Triple(TestSubjectCategory.CHEMISTRY, "🧪 Chem Only (180M)", Color(0xFFF59E0B)),
                                    Triple(TestSubjectCategory.BIOLOGY, "🧬 Bio Only (360M)", Color(0xFF10B981))
                                )
                                typeOptions.forEach { (type, label, color) ->
                                    val isSelected = dialogTestType == type
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) color else (if (isDark) Color(0x1EFFFFFF) else Color(0xFFF1F5F9)),
                                        border = BorderStroke(1.dp, if (isSelected) color else (if (isDark) Color(0x28FFFFFF) else Color(0xFFE2E8F0))),
                                        modifier = Modifier.clickable { dialogTestType = type }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Live Calculated Total Score Banner
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF),
                            border = BorderStroke(1.2.dp, Color(0xFF818CF8).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        if (dialogTestType == TestSubjectCategory.FULL_MOCK) "LIVE TOTAL SCORE" else "LIVE ${dialogTestType.displayName.uppercase()} SCORE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        "$liveTotal / $dialogMaxScore",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDark) Color.White else Color(0xFF1E1B4B)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (dialogTestType == TestSubjectCategory.FULL_MOCK || dialogTestType == TestSubjectCategory.PHYSICS) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF3B82F6).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "P:$livePhy",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF3B82F6),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    if (dialogTestType == TestSubjectCategory.FULL_MOCK || dialogTestType == TestSubjectCategory.CHEMISTRY) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "C:$liveChem",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD97706),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    if (dialogTestType == TestSubjectCategory.FULL_MOCK || dialogTestType == TestSubjectCategory.BIOLOGY) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "B:$liveBio",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    if (liveNeg > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "-$liveNeg",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEF4444),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Maximum Marks (Total Marks of Test)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "MAXIMUM / TOTAL MARKS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = subTextColor,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    "Max: $dialogMaxScore M",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Quick Marks Presets
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val presets = listOf(720, 360, 300, 180, 100, 75, 50, 40, 20)
                                presets.forEach { pVal ->
                                    val isPSelected = dialogMaxScore == pVal
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isPSelected) Color(0xFF6366F1) else (if (isDark) Color(0x1EFFFFFF) else Color(0xFFF1F5F9)),
                                        border = BorderStroke(1.dp, if (isPSelected) Color(0xFF6366F1) else (if (isDark) Color(0x28FFFFFF) else Color(0xFFCBD5E1))),
                                        modifier = Modifier.clickable { customMaxMarks = pVal.toString() }
                                    ) {
                                        Text(
                                            text = "${pVal}M",
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isPSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (isPSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customMaxMarks,
                                onValueChange = { customMaxMarks = it },
                                label = { Text("Custom Max Marks (Any score, e.g. 40, 100, 180, 720)") },
                                placeholder = { Text("Default: $defaultCategoryMax Marks") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        // Test Name Input
                        OutlinedTextField(
                            value = testName,
                            onValueChange = { testName = it },
                            label = { Text("Test Name / Series") },
                            placeholder = { 
                                Text(
                                    when (dialogTestType) {
                                        TestSubjectCategory.PHYSICS -> "e.g. Physics Chapter Test #1, Allen Phy Minor"
                                        TestSubjectCategory.CHEMISTRY -> "e.g. Chemistry Test #1, Organic Chem Sectional"
                                        TestSubjectCategory.BIOLOGY -> "e.g. Biology Full NCERT Test, Genetics Sectional"
                                        else -> "e.g. Allen Full Mock #1, Testbook FT-2"
                                    }
                                ) 
                            },
                            leadingIcon = {
                                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Subject Marks & Incorrect Input Section
                        Text(
                            if (dialogTestType == TestSubjectCategory.FULL_MOCK) "Subject Marks & Incorrect Questions:" else "${dialogTestType.displayName} Score & Incorrect Questions:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor
                        )

                        // Physics Row: Score & Incorrect
                        if (dialogTestType == TestSubjectCategory.FULL_MOCK || dialogTestType == TestSubjectCategory.PHYSICS) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = physics,
                                    onValueChange = { physics = it },
                                    label = { Text("⚡ Phy Score") },
                                    placeholder = { Text(if (isDialogJee) "/100" else "/180") },
                                    modifier = Modifier.weight(1.15f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF3B82F6),
                                        unfocusedBorderColor = if (isDark) Color(0xFF1E3A8A) else Color(0xFFBFDBFE),
                                        focusedLabelColor = Color(0xFF3B82F6),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                OutlinedTextField(
                                    value = phyIncorrect,
                                    onValueChange = { phyIncorrect = it },
                                    label = { Text("❌ Phy Wrong") },
                                    placeholder = { Text("-Qs") },
                                    modifier = Modifier.weight(0.85f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFEF4444),
                                        unfocusedBorderColor = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFECACA),
                                        focusedLabelColor = Color(0xFFDC2626),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }

                        // Chemistry Row: Score & Incorrect
                        if (dialogTestType == TestSubjectCategory.FULL_MOCK || dialogTestType == TestSubjectCategory.CHEMISTRY) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = chemistry,
                                    onValueChange = { chemistry = it },
                                    label = { Text("🧪 Chem Score") },
                                    placeholder = { Text(if (isDialogJee) "/100" else "/180") },
                                    modifier = Modifier.weight(1.15f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFF59E0B),
                                        unfocusedBorderColor = if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A),
                                        focusedLabelColor = Color(0xFFD97706),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                OutlinedTextField(
                                    value = chemIncorrect,
                                    onValueChange = { chemIncorrect = it },
                                    label = { Text("❌ Chem Wrong") },
                                    placeholder = { Text("-Qs") },
                                    modifier = Modifier.weight(0.85f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFEF4444),
                                        unfocusedBorderColor = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFECACA),
                                        focusedLabelColor = Color(0xFFDC2626),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }

                        // Biology / Math Row: Score & Incorrect
                        if (dialogTestType == TestSubjectCategory.FULL_MOCK || dialogTestType == TestSubjectCategory.BIOLOGY) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = biology,
                                    onValueChange = { biology = it },
                                    label = { Text(if (isDialogJee) "🧮 Math Score" else "🧬 Bio Score") },
                                    placeholder = { Text(if (isDialogJee) "/100" else "/360") },
                                    modifier = Modifier.weight(1.15f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF10B981),
                                        unfocusedBorderColor = if (isDark) Color(0xFF064E3B) else Color(0xFFA7F3D0),
                                        focusedLabelColor = Color(0xFF059669),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                OutlinedTextField(
                                    value = bioIncorrect,
                                    onValueChange = { bioIncorrect = it },
                                    label = { Text(if (isDialogJee) "❌ Math Wrong" else "❌ Bio Wrong") },
                                    placeholder = { Text("-Qs") },
                                    modifier = Modifier.weight(0.85f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFEF4444),
                                        unfocusedBorderColor = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFECACA),
                                        focusedLabelColor = Color(0xFFDC2626),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }

                        // Auto-Calculated Negative Penalty Summary Box
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("❌ Auto Negative:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                    Text(
                                        "-$liveNeg Marks",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                                Text(
                                    when (dialogTestType) {
                                        TestSubjectCategory.PHYSICS -> "Phy: -$livePhyInc"
                                        TestSubjectCategory.CHEMISTRY -> "Chem: -$liveChemInc"
                                        TestSubjectCategory.BIOLOGY -> "${if (isDialogJee) "Math" else "Bio"}: -$liveBioInc"
                                        else -> "Phy: -$livePhyInc | Chem: -$liveChemInc | ${if (isDialogJee) "Math" else "Bio"}: -$liveBioInc"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                                )
                            }
                        }

                        // PDF Attachment Section Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (editingPdfUri != null) Color(0xFF8B5CF6) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = "PDF",
                                            tint = if (editingPdfUri != null) Color(0xFF8B5CF6) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (editingPdfUri != null) (editingPdfFileName ?: "Attached Paper.pdf") else "Question Paper PDF (Optional)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (editingPdfUri != null) Color(0xFF8B5CF6) else textColor,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = if (editingPdfUri != null) "✨ Auto ${if (isDialogJee) "JEE" else "NEET"}-level analysis enabled" else "Attach to get AI comparison with ${if (isDialogJee) "JEE" else "NEET"} difficulty",
                                                fontSize = 10.sp,
                                                color = subTextColor
                                            )
                                        }
                                    }

                                    if (editingPdfUri != null) {
                                        IconButton(
                                            onClick = {
                                                editingPdfUri = null
                                                editingPdfFileName = null
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove PDF", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { dialogPdfLauncher.launch("application/pdf") },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B5CF6)),
                                            modifier = Modifier.heightIn(min = 30.dp)
                                        ) {
                                            Text("Browse", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val isSinglePhy = dialogTestType == TestSubjectCategory.PHYSICS
                            val isSingleChem = dialogTestType == TestSubjectCategory.CHEMISTRY
                            val isSingleBio = dialogTestType == TestSubjectCategory.BIOLOGY

                            val p = if (isSingleChem || isSingleBio) 0 else (physics.toIntOrNull() ?: 0)
                            val c = if (isSinglePhy || isSingleBio) 0 else (chemistry.toIntOrNull() ?: 0)
                            val b = if (isSinglePhy || isSingleChem) 0 else (biology.toIntOrNull() ?: 0)
                            val pInc = if (isSingleChem || isSingleBio) 0 else (phyIncorrect.toIntOrNull() ?: 0)
                            val cInc = if (isSinglePhy || isSingleBio) 0 else (chemIncorrect.toIntOrNull() ?: 0)
                            val bInc = if (isSinglePhy || isSingleChem) 0 else (bioIncorrect.toIntOrNull() ?: 0)
                            val n = if ((pInc + cInc + bInc) > 0) (pInc + cInc + bInc) else (negative.toIntOrNull() ?: 0)
                            val s = when (dialogTestType) {
                                TestSubjectCategory.PHYSICS -> p - pInc
                                TestSubjectCategory.CHEMISTRY -> c - cInc
                                TestSubjectCategory.BIOLOGY -> b - bInc
                                else -> p + c + b - n
                            }

                            val defaultTitle = when (dialogTestType) {
                                TestSubjectCategory.PHYSICS -> "Physics Test (${dialogMaxScore}M)"
                                TestSubjectCategory.CHEMISTRY -> "Chemistry Test (${dialogMaxScore}M)"
                                TestSubjectCategory.BIOLOGY -> "Biology Test (${dialogMaxScore}M)"
                                else -> if (dialogMaxScore == 720) "Full Mock Test (720M)" else "Mock Test (${dialogMaxScore}M)"
                            }
                            val name = testName.ifEmpty { defaultTitle }
                            val id = editingTestId
                            val pdfUri = editingPdfUri
                            val pdfName = editingPdfFileName
                            val isJeeSave = name.contains("JEE", true) || name.contains("IIT", true)

                            val singleSubjectPrefix = when (dialogTestType) {
                                TestSubjectCategory.PHYSICS -> "Single Subject (Physics)\n"
                                TestSubjectCategory.CHEMISTRY -> "Single Subject (Chemistry)\n"
                                TestSubjectCategory.BIOLOGY -> if (isJeeSave) "Single Subject (Maths)\n" else "Single Subject (Biology)\n"
                                else -> ""
                            }
                            val negBreakdownLine = "Negative Breakdown: Phy: -$pInc, Chem: -$cInc, ${if (isJeeSave) "Math" else "Bio"}: -$bInc"
                            val analysis = buildString {
                                append("Max Marks: $dialogMaxScore\n")
                                if (singleSubjectPrefix.isNotEmpty()) append(singleSubjectPrefix)
                                if (!editingGeminiAnalysis.isNullOrBlank()) {
                                    val cleaned = editingGeminiAnalysis!!.lines().filter { !it.startsWith("Negative Breakdown:") && !it.startsWith("Single Subject") && !it.startsWith("Max Marks:") }.joinToString("\n").trim()
                                    if (cleaned.isNotBlank()) append(cleaned).append("\n")
                                }
                                append(negBreakdownLine)
                            }.trim()

                            if (id != null) {
                                val ts = editingTimestamp ?: System.currentTimeMillis()
                                viewModel.updateMockTest(id, name, s, p, c, b, n, ts, pdfUri, pdfName, analysis)
                            } else {
                                viewModel.addMockTest(name, s, p, c, b, n, pdfUri, pdfName, analysis)
                            }

                            // Auto-trigger Gemini analysis if PDF was selected and not yet analyzed
                            if (pdfUri != null && (editingGeminiAnalysis.isNullOrBlank() || editingGeminiAnalysis!!.startsWith("Negative Breakdown:"))) {
                                val newTest = MockTest(id = id ?: 0, testName = name, score = s, physics = p, chemistry = c, biology = b, negative = n)
                                viewModel.analyzeMockTestPdf(context, newTest, Uri.parse(pdfUri), pdfName ?: "Test_Paper.pdf")
                            }

                            showDialog = false
                            dialogTestType = TestSubjectCategory.FULL_MOCK
                            editingTestId = null
                            editingTimestamp = null
                            testName = ""
                            physics = ""
                            chemistry = ""
                            biology = ""
                            customMaxMarks = ""
                            phyIncorrect = ""
                            chemIncorrect = ""
                            bioIncorrect = ""
                            negative = ""
                            editingPdfUri = null
                            editingPdfFileName = null
                            editingGeminiAnalysis = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(if (editingTestId != null) "Update Test" else "Save Score", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { 
                            showDialog = false
                            dialogTestType = TestSubjectCategory.FULL_MOCK
                            editingTestId = null
                            editingTimestamp = null
                            phyIncorrect = ""
                            chemIncorrect = ""
                            bioIncorrect = ""
                            editingPdfUri = null
                            editingPdfFileName = null
                            editingGeminiAnalysis = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                    ) {
                        Text("Cancel", color = subTextColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        if (showScheduleDialog) {
            AlertDialog(
                onDismissRequest = { showScheduleDialog = false },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                shape = RoundedCornerShape(24.dp),
                containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                tonalElevation = 8.dp,
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7))),
                                        RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.EventNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (editingSchedId != null) "Edit Scheduled Test" else "Schedule Mock Test",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = textColor
                                )
                                Text(
                                    text = "Set target date & syllabus chapters",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subTextColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = schedTitle,
                            onValueChange = { schedTitle = it },
                            label = { Text("Test Title / Series") },
                            placeholder = { Text("e.g. Major Test 1 (Full Syllabus)") },
                            leadingIcon = {
                                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )

                        OutlinedTextField(
                            value = schedDate,
                            onValueChange = { schedDate = it },
                            label = { Text("Target Date (YYYY-MM-DD)") },
                            placeholder = { Text("YYYY-MM-DD") },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )

                        // Quick Date Suggestion Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            listOf(
                                "+3 Days" to 3,
                                "+1 Week" to 7,
                                "+2 Weeks" to 14,
                                "+1 Month" to 30
                            ).forEach { (label, days) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF),
                                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            schedDate = sdf.format(Date(System.currentTimeMillis() + days * 86400000L))
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            "📚 Target Syllabus Topics:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor
                        )

                        // Physics Syllabus
                        OutlinedTextField(
                            value = schedPhy,
                            onValueChange = { schedPhy = it },
                            label = { Text("⚡ Physics Topics") },
                            placeholder = { Text("e.g. Kinematics, Optics, Modern Physics...") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = if (isDark) Color(0xFF1E3A8A) else Color(0xFFBFDBFE),
                                focusedLabelColor = Color(0xFF3B82F6),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )

                        // Biology Syllabus
                        OutlinedTextField(
                            value = schedBio,
                            onValueChange = { schedBio = it },
                            label = { Text("🧬 Biology Topics") },
                            placeholder = { Text("e.g. Genetics, Human Physiology, Ecology, Cell Biology...") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = if (isDark) Color(0xFF064E3B) else Color(0xFFA7F3D0),
                                focusedLabelColor = Color(0xFF059669),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )

                        // Chemistry Syllabus
                        OutlinedTextField(
                            value = schedChem,
                            onValueChange = { schedChem = it },
                            label = { Text("🧪 Chemistry Topics") },
                            placeholder = { Text("e.g. Thermodynamics, Organic Reactions...") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A),
                                focusedLabelColor = Color(0xFFD97706),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )

                        // Pin to top toggle card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (schedPinned) Color(0xFFF59E0B).copy(alpha = 0.15f) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (schedPinned) Color(0xFFF59E0B) else (if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { schedPinned = !schedPinned }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (schedPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = null,
                                        tint = if (schedPinned) Color(0xFFF59E0B) else subTextColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            "Pin Test to Top",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = textColor
                                        )
                                        Text(
                                            "Keeps this test on top of upcoming targets",
                                            fontSize = 11.sp,
                                            color = subTextColor
                                        )
                                    }
                                }
                                Switch(
                                    checked = schedPinned,
                                    onCheckedChange = { schedPinned = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFF59E0B),
                                        checkedTrackColor = Color(0xFFF59E0B).copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val title = schedTitle.ifEmpty { "Upcoming Test" }
                            val date = schedDate.ifEmpty { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                            val id = editingSchedId
                            if (id != null) {
                                viewModel.updateScheduledMockTest(id, title, date, schedPhy, schedChem, schedBio, "", false, schedPinned)
                            } else {
                                viewModel.addScheduledMockTest(title, date, schedPhy, schedChem, schedBio, "", schedPinned)
                            }
                            showScheduleDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(if (editingSchedId != null) "Update Test" else "Schedule Test", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showScheduleDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                    ) {
                        Text("Cancel", color = subTextColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        if (showTestbookFullscreen) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showTestbookFullscreen = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true
                )
            ) {
                TestbookWebScreen(
                    viewModel = viewModel,
                    onBackToApp = { showTestbookFullscreen = false }
                )
            }
        }
    }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, androidx.compose.foundation.shape.CircleShape))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


