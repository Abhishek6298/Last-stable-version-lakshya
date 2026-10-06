package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.ui.components.AppSegmentedControl
import com.example.ui.components.FireworksProgressBar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BookProgression
import com.example.data.EdunitiTarget
import com.example.data.ExamSyllabusDatabase
import com.example.ui.AppViewModel
import com.example.ui.components.GlassCard

import com.example.data.DynamicChecklistTopic
import org.json.JSONArray
import kotlinx.coroutines.launch
import kotlin.math.ceil

val DEFAULT_PHYSICS_CHAPTERS = listOf(
    "Units and Measurements",
    "Motion in a Straight Line",
    "Motion in a Plane",
    "Laws of Motion",
    "Work, Energy and Power",
    "System of Particles & Rotational Motion",
    "Gravitation",
    "Mechanical Properties of Solids",
    "Mechanical Properties of Fluids",
    "Thermal Properties of Matter",
    "Thermodynamics",
    "Kinetic Theory of Gases",
    "Oscillations",
    "Waves",
    "Electric Charges and Fields",
    "Electrostatic Potential & Capacitance",
    "Current Electricity",
    "Moving Charges and Magnetism",
    "Magnetism and Matter",
    "Electromagnetic Induction",
    "Alternating Current",
    "Electromagnetic Waves",
    "Ray Optics and Optical Instruments",
    "Wave Optics",
    "Dual Nature of Radiation and Matter",
    "Atoms",
    "Nuclei",
    "Semiconductor Electronics"
)

val DEFAULT_CHEMISTRY_CHAPTERS = listOf(
    "Some Basic Concepts of Chemistry",
    "Structure of Atom",
    "Classification of Elements & Periodicity",
    "Chemical Bonding & Molecular Structure",
    "Chemical Thermodynamics",
    "Equilibrium (Chemical & Ionic)",
    "Redox Reactions",
    "Solutions",
    "Electrochemistry",
    "Chemical Kinetics",
    "p-Block Elements",
    "d- and f-Block Elements",
    "Coordination Compounds",
    "Organic Chemistry: Principles & Techniques (GOC)",
    "Hydrocarbons",
    "Haloalkanes and Haloarenes",
    "Alcohols, Phenols and Ethers",
    "Aldehydes, Ketones and Carboxylic Acids",
    "Organic Compounds Containing Nitrogen (Amines)",
    "Biomolecules"
)

val DEFAULT_BIOLOGY_CHAPTERS = listOf(
    "The Living World",
    "Biological Classification",
    "Plant Kingdom",
    "Animal Kingdom",
    "Morphology of Flowering Plants",
    "Anatomy of Flowering Plants",
    "Structural Organisation in Animals",
    "Cell: The Unit of Life",
    "Biomolecules",
    "Cell Cycle and Cell Division",
    "Photosynthesis in Higher Plants",
    "Respiration in Plants",
    "Plant Growth and Development",
    "Breathing and Exchange of Gases",
    "Body Fluids and Circulation",
    "Excretory Products and their Elimination",
    "Locomotion and Movement",
    "Neural Control and Coordination",
    "Chemical Coordination and Integration",
    "Sexual Reproduction in Flowering Plants",
    "Human Reproduction",
    "Reproductive Health",
    "Principles of Inheritance and Variation",
    "Molecular Basis of Inheritance",
    "Evolution",
    "Human Health and Disease",
    "Microbes in Human Welfare",
    "Biotechnology: Principles and Processes",
    "Biotechnology and its Applications",
    "Organisms and Populations",
    "Ecosystem",
    "Biodiversity and Conservation"
)

val DEFAULT_SUBTOPICS = listOf(
    "Theory & NCERT Reading",
    "Concept & Solved Examples",
    "Exercise-1 (Topic MCQs)",
    "Exercise-2 (Advanced Practice)",
    "PYQs (Past 10 Years Questions)"
)

data class SubtopicStatusOption(
    val label: String,
    val displayText: String,
    val activeColor: Color,
    val activeBg: Color
)

fun parseSubtopicsProgress(progressStr: String?): Map<String, String> {
    if (progressStr.isNullOrBlank()) return emptyMap()
    return progressStr.split("|").mapNotNull {
        val parts = it.split("=")
        if (parts.size == 2) parts[0] to parts[1] else null
    }.toMap()
}

fun formatSubtopicsProgress(map: Map<String, String>): String {
    return map.entries.joinToString("|") { "${it.key}=${it.value}" }
}

fun getFileName(context: Context, uri: Uri): String {
    var name = "uploaded_document.pdf"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1) {
                name = it.getString(index)
            }
        }
    }
    return name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onNavigate: ((String) -> Unit)? = null,
    onNavigateBack: () -> Unit = {}
) {
    val activeExamGoal by viewModel.activeExamGoal.collectAsStateWithLifecycle()
    val progressions by viewModel.bookProgressions.collectAsStateWithLifecycle()
    val edunitiTargets by viewModel.edunitiTargets.collectAsStateWithLifecycle()
    val checklistState by viewModel.edunitiChecklistState.collectAsStateWithLifecycle()
    val allChecklistTopics by viewModel.dynamicChecklistTopics.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isDark = isDarkMode

    val availableSubjects = remember(activeExamGoal) {
        ExamSyllabusDatabase.getSubjectsFor(activeExamGoal)
    }

    var activeTab by remember { mutableStateOf("Chapters") } // "Chapters", "NotesVault", "Eduniti", "EdunitiChecklist"
    var selectedSubject by remember { mutableStateOf(availableSubjects.firstOrNull() ?: "Physics") }
    var checklistSelectedSubject by remember { mutableStateOf("Physics") }
    var searchQuery by remember { mutableStateOf("") }
    var expandedChapters by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(activeExamGoal) {
        if (selectedSubject !in availableSubjects) {
            selectedSubject = availableSubjects.firstOrNull() ?: "Physics"
        }
    }

    var isChecklistUploading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val checklistPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isChecklistUploading = true
            Toast.makeText(context, "Scanning 100% of PDF pages with Lakshya AI...", Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                val res = com.example.data.GeminiChatAssistant.generateChecklistFromPDF(context, it.toString(), checklistSelectedSubject)
                res.onSuccess { rawResponse ->
                    try {
                        var cleanJson = rawResponse.trim()
                        if (cleanJson.contains("```json")) {
                            cleanJson = cleanJson.substringAfter("```json").substringBefore("```")
                        } else if (cleanJson.contains("```")) {
                            cleanJson = cleanJson.substringAfter("```").substringBefore("```")
                        }
                        val firstBracket = cleanJson.indexOf('[')
                        val lastBracket = cleanJson.lastIndexOf(']')
                        if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
                            val jsonArrayStr = cleanJson.substring(firstBracket, lastBracket + 1)
                            val arr = JSONArray(jsonArrayStr)
                            var count = 0
                            val phases = mutableSetOf<String>()
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val rawTopic = obj.optString("topicName", "Topic ${i + 1}").trim().replace("\n", " ")
                                val rawPhase = obj.optString("phaseTitle", "$checklistSelectedSubject Checklist").trim().replace("\n", " ")
                                val rawCols = obj.optString("columns", "2020,2021,2022,2023,2024,2025,2026")
                                    .split(",")
                                    .map { it.trim().replace("\n", " ") }
                                    .filter { it.isNotEmpty() && !it.equals("DPPS", ignoreCase = true) && !it.equals("DPP", ignoreCase = true) && !it.equals("DPPS SHEETS", ignoreCase = true) && !it.equals("DPP SHEETS", ignoreCase = true) }
                                    .joinToString(",")

                                val t = DynamicChecklistTopic(
                                    subject = obj.optString("subject", checklistSelectedSubject).trim().ifBlank { checklistSelectedSubject },
                                    phaseTitle = rawPhase.ifBlank { "$checklistSelectedSubject Checklist" },
                                    topicName = rawTopic.ifBlank { "Topic ${i + 1}" },
                                    days = obj.optInt("days", 4).coerceAtLeast(1),
                                    columns = if (rawCols.isNotBlank()) rawCols else "2020,2021,2022,2023,2024,2025,2026"
                                )
                                viewModel.saveDynamicChecklistTopic(t)
                                phases.add(t.phaseTitle)
                                count++
                            }
                            Toast.makeText(context, "✅ Extracted $count topics across ${phases.size} phases from PDF!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Could not extract structured checklist from PDF. Please check PDF content.", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Parsing error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { 
                    Toast.makeText(context, "Lakshya AI Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
                isChecklistUploading = false
            }
        }
    }

    // Dialog state for editing a chapter's book name
    var editingChapterName by remember { mutableStateOf<String?>(null) }
    var editingBookType by remember { mutableStateOf("NEET") }
    var editingBookNameText by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf(0L) }
    var editingProgress by remember { mutableStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }

    // EDUNITI Target Dialog states
    var showAddEdunitiTargetDialog by remember { mutableStateOf(false) }
    var edunitiTitle by remember { mutableStateOf("") }
    var edunitiTotalQuestionsStr by remember { mutableStateOf("") }
    var edunitiTotalDaysStr by remember { mutableStateOf("") }
    var uploadedPdfName by remember { mutableStateOf<String?>(null) }
    var uploadedPdfUri by remember { mutableStateOf<String?>(null) }

    // Intercept back presses when dialogs or secondary tabs are active, or navigate back
    BackHandler(enabled = true) {
        when {
            showEditDialog -> showEditDialog = false
            showAddEdunitiTargetDialog -> showAddEdunitiTargetDialog = false
            activeTab != "Chapters" -> activeTab = "Chapters"
            else -> onNavigateBack()
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            uploadedPdfUri = uri.toString()
            uploadedPdfName = getFileName(context, uri)
        }
    }

    val currentChapters = remember(activeExamGoal, selectedSubject) {
        val list = ExamSyllabusDatabase.getChaptersFor(activeExamGoal, selectedSubject)
        if (list.isNotEmpty()) {
            list.map { it.name }
        } else {
            when (selectedSubject) {
                "Physics" -> DEFAULT_PHYSICS_CHAPTERS
                "Chemistry" -> DEFAULT_CHEMISTRY_CHAPTERS
                else -> DEFAULT_BIOLOGY_CHAPTERS
            }
        }
    }

    val filteredChapters = remember(currentChapters, searchQuery) {
        if (searchQuery.isBlank()) {
            currentChapters
        } else {
            currentChapters.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val cardBorder = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
    val dialogBg = if (isDark) Color(0xFF0F172A) else Color.White

    // Calculate Stats for Selected Subject (aligned across chapter list)
    val stats = remember(progressions, selectedSubject, currentChapters) {
        val subjectProgressions = progressions.filter { it.subject == selectedSubject }
        val totalChapters = currentChapters.size
        var done = 0
        var inProg = 0
        var sumProgress = 0

        currentChapters.forEach { chName ->
            val prog = subjectProgressions.find { it.chapter == chName }
            if (prog != null) {
                val map = parseSubtopicsProgress(prog.subtopicsProgress)
                val pct = if (map.isNotEmpty()) {
                    val earnedPoints = DEFAULT_SUBTOPICS.sumOf { sub ->
                        when (map[sub]) {
                            "Completed" -> 2
                            "Ongoing" -> 1
                            else -> 0
                        }
                    }
                    (earnedPoints * 100) / (DEFAULT_SUBTOPICS.size * 2)
                } else {
                    prog.progressPercent
                }
                sumProgress += pct
                if (pct >= 100) done++ else if (pct > 0) inProg++
            }
        }
        val avgProgress = if (totalChapters > 0) sumProgress / totalChapters else 0
        Triple(done, inProg, avgProgress)
    }
    val (completedCount, inProgressCount, avgProgress) = stats

    val progressionMap = remember(progressions, selectedSubject) {
        progressions.filter { it.subject == selectedSubject }.associateBy { it.chapter }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 16.dp),
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
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = when (activeTab) {
                                    "Chapters" -> "Syllabus Tracker"
                                    "Eduniti" -> "EDUNITI Targets"
                                    else -> "Velocity Checklist"
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6366F1))
                            )
                        }
                        Text(
                            text = when (activeTab) {
                                "Chapters" -> "Complete NCERT syllabus tracker with subtopics"
                                "Eduniti" -> "Custom booklet targets with daily recommended questions"
                                else -> "Structured phase-by-phase velocity checklist"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor
                        )
                    }
                }
            }

            // Modern Tab Selector Bar
            item {
                AppSegmentedControl(
                    items = listOf("Chapters", "Eduniti", "EdunitiChecklist"),
                    selectedItem = activeTab,
                    onItemSelected = { activeTab = it },
                    itemLabel = {
                        when (it) {
                            "Chapters" -> "NCERT Tracker"
                            "Eduniti" -> "Targets"
                            else -> "Checklist"
                        }
                    },
                    itemEmoji = {
                        when (it) {
                            "Chapters" -> "📖"
                            "Eduniti" -> "⚡"
                            else -> "📋"
                        }
                    },
                    selectedColor = Color(0xFF6D28D9),
                    selectedGradient = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
                    isDark = isDark,
                    fontSize = 12.sp
                )
            }

            // ========================= TAB 1: NCERT CHAPTERS =========================
            if (activeTab == "Chapters") {
                // Subject Filter Bar
                item {
                    if (availableSubjects.size <= 4) {
                        AppSegmentedControl(
                            items = availableSubjects,
                            selectedItem = selectedSubject,
                            onItemSelected = { selectedSubject = it },
                            itemLabel = { it },
                            itemEmoji = {
                                when {
                                    it.contains("Physics", ignoreCase = true) -> "⚛️"
                                    it.contains("Chemistry", ignoreCase = true) -> "🧪"
                                    it.contains("Biology", ignoreCase = true) -> "🧬"
                                    it.contains("Math", ignoreCase = true) -> "📐"
                                    else -> "📚"
                                }
                            },
                            selectedColor = when {
                                selectedSubject.contains("Physics", ignoreCase = true) -> Color(0xFF4F46E5)
                                selectedSubject.contains("Chemistry", ignoreCase = true) -> Color(0xFF059669)
                                selectedSubject.contains("Math", ignoreCase = true) -> Color(0xFF2563EB)
                                else -> Color(0xFFDB2777)
                            },
                            selectedGradient = when {
                                selectedSubject.contains("Physics", ignoreCase = true) -> listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                                selectedSubject.contains("Chemistry", ignoreCase = true) -> listOf(Color(0xFF10B981), Color(0xFF059669))
                                selectedSubject.contains("Math", ignoreCase = true) -> listOf(Color(0xFF38BDF8), Color(0xFF2563EB))
                                else -> listOf(Color(0xFFEC4899), Color(0xFFDB2777))
                            },
                            isDark = isDark
                        )
                    } else {
                        // Scrollable subject chips
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(availableSubjects) { subj ->
                                val isSel = selectedSubject == subj
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
                                    else -> "📖"
                                }
                                Surface(
                                    color = if (isSel) Color(0xFF0284C7) else if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF0284C7) else if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable { selectedSubject = subj }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(emoji, fontSize = 12.sp)
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
                    }
                }

                // Glowing Subject Overview Card
                item {
                    val accentColor = when (selectedSubject) {
                        "Physics" -> Color(0xFF6366F1)
                        "Chemistry" -> Color(0xFF10B981)
                        else -> Color(0xFFEC4899)
                    }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.5.dp,
                                Brush.linearGradient(
                                    listOf(accentColor.copy(alpha = 0.7f), Color(0xFF38BDF8).copy(alpha = 0.4f), accentColor.copy(alpha = 0.2f))
                                ),
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "$selectedSubject Mastery",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = textColor
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = accentColor.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "$avgProgress%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = accentColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$completedCount Done • $inProgressCount In Progress • ${currentChapters.size - completedCount - inProgressCount} Left",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = subTextColor
                                    )
                                }

                                // Glowing Circular Progress Ring
                                Box(
                                    modifier = Modifier.size(54.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val strokeW = 6.dp.toPx()
                                        drawCircle(
                                            color = accentColor.copy(alpha = 0.15f),
                                            style = Stroke(width = strokeW)
                                        )
                                        val sweep = (avgProgress / 100f) * 360f
                                        drawArc(
                                            brush = Brush.sweepGradient(
                                                listOf(accentColor, Color(0xFF38BDF8), accentColor)
                                            ),
                                            startAngle = -90f,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                        )
                                    }
                                    Text(
                                        text = "$avgProgress%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = accentColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Glowing linear bar
                            val masteryProg = (avgProgress / 100f).coerceIn(0f, 1f)
                            FireworksProgressBar(
                                progress = masteryProg,
                                height = 8.dp,
                                gradientColors = listOf(accentColor, Color(0xFF38BDF8), accentColor),
                                sparkColor = Color(0xFF38BDF8),
                                isDark = isDark
                            )
                        }
                    }
                }

                // Glowing Search Box
                item {
                    val accentColor = when (selectedSubject) {
                        "Physics" -> Color(0xFF6366F1)
                        "Chemistry" -> Color(0xFF10B981)
                        else -> Color(0xFFEC4899)
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search ${selectedSubject.lowercase()} chapters...", color = subTextColor, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentColor) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = subTextColor)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = cardBorder,
                            focusedContainerColor = if (isDark) Color(0x0DFFFFFF) else Color(0x08000000),
                            unfocusedContainerColor = if (isDark) Color(0x0DFFFFFF) else Color(0x08000000)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                if (filteredChapters.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No chapters found matching '$searchQuery' 🔍", color = subTextColor, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Chapter Progress Cards
                items(filteredChapters, key = { it }) { chapterName ->
                    val progression = progressionMap[chapterName]
                    val bookType = progression?.bookType ?: "NEET"
                    val bookName = progression?.bookName ?: "NCERT"
                    val rawProgress = progression?.progressPercent ?: 0
                    val progId = progression?.id ?: 0L
                    val subtopicsProgressStr = progression?.subtopicsProgress ?: ""

                    val isExpanded = expandedChapters.contains(chapterName)

                    val accentColor = when (selectedSubject) {
                        "Physics" -> Color(0xFF6366F1)
                        "Chemistry" -> Color(0xFF10B981)
                        else -> Color(0xFFEC4899)
                    }

                    val subtopicStatuses = remember(subtopicsProgressStr) {
                        parseSubtopicsProgress(subtopicsProgressStr)
                    }

                    val progress = if (subtopicStatuses.isNotEmpty()) {
                        val earnedPoints = DEFAULT_SUBTOPICS.sumOf { sub ->
                            when (subtopicStatuses[sub]) {
                                "Completed" -> 2
                                "Ongoing" -> 1
                                else -> 0
                            }
                        }
                        (earnedPoints * 100) / (DEFAULT_SUBTOPICS.size * 2)
                    } else rawProgress

                    val cardBorderBrush = remember(progress, accentColor, cardBorder) {
                        Brush.linearGradient(
                            if (progress == 100) {
                                listOf(Color(0xFF10B981), Color(0xFF34D399))
                            } else if (progress > 0) {
                                listOf(accentColor.copy(alpha = 0.7f), Color(0xFF38BDF8).copy(alpha = 0.4f))
                            } else {
                                listOf(cardBorder, cardBorder)
                            }
                        )
                    }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.2.dp, cardBorderBrush, RoundedCornerShape(20.dp))
                            .clickable {
                                expandedChapters = if (isExpanded) {
                                    expandedChapters - chapterName
                                } else {
                                    expandedChapters + chapterName
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Row: Chapter Title & Progress Ring
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                    Text(
                                        text = chapterName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Book Name Badge
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDark) Color(0x15FFFFFF) else Color(0xFFF1F5F9),
                                            border = BorderStroke(0.8.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)),
                                            modifier = Modifier.clickable {
                                                editingChapterName = chapterName
                                                editingBookType = bookType
                                                editingBookNameText = bookName
                                                editingId = progId
                                                editingProgress = progress
                                                showEditDialog = true
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Book,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = bookName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor
                                                )
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit Book",
                                                    tint = subTextColor,
                                                    modifier = Modifier.size(9.dp)
                                                )
                                            }
                                        }

                                        // Subject Target Switch
                                        if (selectedSubject == "Biology") {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0x15EC4899),
                                                border = BorderStroke(0.8.dp, Color(0xFFEC4899).copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    text = "NEET Core",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFEC4899),
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9))
                                                    .padding(2.dp)
                                            ) {
                                                listOf("NEET", "JEE").forEach { type ->
                                                    val isSelected = bookType == type
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (isSelected) accentColor else Color.Transparent)
                                                            .clickable {
                                                                viewModel.saveBookProgression(
                                                                    BookProgression(
                                                                        id = progId,
                                                                        subject = selectedSubject,
                                                                        chapter = chapterName,
                                                                        bookType = type,
                                                                        bookName = bookName,
                                                                        progressPercent = progress,
                                                                        status = if (progress == 100) "Completed" else if (progress > 0) "In Progress" else "Not Started",
                                                                        subtopicsProgress = subtopicsProgressStr
                                                                    )
                                                                )
                                                            }
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = type,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) Color.White else subTextColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Glowing Progress Capsule & Expand Icon
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (progress == 100) Color(0xFF10B981).copy(alpha = 0.15f) else accentColor.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, if (progress == 100) Color(0xFF10B981).copy(alpha = 0.4f) else accentColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "$progress%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (progress == 100) Color(0xFF10B981) else accentColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Toggle subtopics",
                                        tint = subTextColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Row: Notes & Quick Revision Button + Progress Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val chapterProg = (progress / 100f).coerceIn(0f, 1f)
                                FireworksProgressBar(
                                    progress = chapterProg,
                                    height = 6.dp,
                                    gradientColors = if (progress == 100) listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF00F5A0))
                                                     else listOf(accentColor, Color(0xFF38BDF8)),
                                    sparkColor = if (progress == 100) Color(0xFF10B981) else Color(0xFF38BDF8),
                                    isDark = isDark,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                )
                            }

                            // Subtopics Section (Visible only when Expanded)
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 1.dp)
                                        .background(cardBorder.copy(alpha = 0.5f))
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Subtopics & Stages Tracker",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = subTextColor
                                    )
                                    Text(
                                        text = "Auto-calculates %",
                                        fontSize = 9.sp,
                                        color = accentColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DEFAULT_SUBTOPICS.forEach { subtopic ->
                                        val currentSubtopicStatus = subtopicStatuses[subtopic] ?: "Not Started"

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = subtopic,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textColor
                                            )

                                            // 3-State Glowing Status Selector
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                listOf(
                                                    SubtopicStatusOption("Not Started", "⚪ NS", Color(0xFF64748B), Color(0x1064748B)),
                                                    SubtopicStatusOption("Ongoing", "🟡 In Progress", Color(0xFFF59E0B), Color(0x18F59E0B)),
                                                    SubtopicStatusOption("Completed", "🟢 Done", Color(0xFF10B981), Color(0x1810B981))
                                                ).forEach { option ->
                                                    val isCurrent = currentSubtopicStatus == option.label
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isCurrent) option.activeBg else (if (isDark) Color(0x0AFFFFFF) else Color(0xFFF8FAFC)))
                                                            .border(
                                                                1.dp,
                                                                if (isCurrent) option.activeColor else Color.Transparent,
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable {
                                                                val updatedMap = subtopicStatuses.toMutableMap()
                                                                updatedMap[subtopic] = option.label
                                                                val newProgressStr = formatSubtopicsProgress(updatedMap)

                                                                val earnedPoints = DEFAULT_SUBTOPICS.sumOf { sub ->
                                                                    when (updatedMap[sub]) {
                                                                        "Completed" -> 2
                                                                        "Ongoing" -> 1
                                                                        else -> 0
                                                                    }
                                                                }
                                                                val newPercent = (earnedPoints * 100) / (DEFAULT_SUBTOPICS.size * 2)
                                                                val allSubtopicsDone = DEFAULT_SUBTOPICS.all { sub -> updatedMap[sub] == "Completed" }
                                                                val newStatus = if (allSubtopicsDone) "Completed" else if (newPercent > 0) "In Progress" else "Not Started"

                                                                viewModel.saveBookProgression(
                                                                    BookProgression(
                                                                        id = progId,
                                                                        subject = selectedSubject,
                                                                        chapter = chapterName,
                                                                        bookType = if (selectedSubject == "Biology") "NEET" else bookType,
                                                                        bookName = bookName,
                                                                        progressPercent = newPercent,
                                                                        status = newStatus,
                                                                        subtopicsProgress = newProgressStr
                                                                    )
                                                                )
                                                            }
                                                            .padding(vertical = 6.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = option.displayText,
                                                            fontSize = 10.sp,
                                                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                                            color = if (isCurrent) option.activeColor else subTextColor
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

            // ========================= TAB 2: EDUNITI TARGETS =========================
            else if (activeTab == "Eduniti") {
                item {
                    Button(
                        onClick = { 
                            edunitiTitle = ""
                            edunitiTotalQuestionsStr = ""
                            edunitiTotalDaysStr = ""
                            uploadedPdfName = null
                            uploadedPdfUri = null
                            showAddEdunitiTargetDialog = true 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Booklet Target", fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }

                // Stats Banner
                item {
                    val activeCount = edunitiTargets.count { it.questionsSolved < it.totalQuestions }
                    val completedCountT = edunitiTargets.count { it.questionsSolved >= it.totalQuestions && it.totalQuestions > 0 }
                    val totalSolvedQuestions = edunitiTargets.sumOf { it.questionsSolved }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.2.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Eduniti Target Analytics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Active Targets", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                                    Text("$activeCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = textColor)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Completed", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                                    Text("$completedCountT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Qs Solved", style = MaterialTheme.typography.labelSmall, color = subTextColor)
                                    Text("$totalSolvedQuestions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6366F1))
                                }
                            }
                        }
                    }
                }

                if (edunitiTargets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🎯 No Booklet Targets Added Yet", style = MaterialTheme.typography.titleMedium, color = textColor, fontWeight = FontWeight.Bold)
                                Text("Upload your PDF question booklet and set daily goals.", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                            }
                        }
                    }
                }

                // Targets List
                items(edunitiTargets, key = { it.id }) { target ->
                    val perDaySuggestion = remember(target.totalQuestions, target.totalDays) {
                        if (target.totalDays > 0) {
                            ceil(target.totalQuestions.toDouble() / target.totalDays.toDouble()).toInt()
                        } else 0
                    }

                    val progressPercent = remember(target.questionsSolved, target.totalQuestions) {
                        if (target.totalQuestions > 0) {
                            (target.questionsSolved * 100) / target.totalQuestions
                        } else 0
                    }

                    val isTargetDone = target.questionsSolved >= target.totalQuestions

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isTargetDone) Color(0xFF10B981).copy(alpha = 0.6f) else cardBorder,
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = target.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )

                                    if (!target.pdfName.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isDark) Color(0x15FFFFFF) else Color(0xFFF1F5F9),
                                            border = BorderStroke(0.8.dp, cardBorder),
                                            modifier = Modifier.clickable {
                                                if (!target.pdfUri.isNullOrBlank()) {
                                                    try {
                                                        val uri = Uri.parse(target.pdfUri)
                                                        viewModel.openPdfInReader(uri, target.pdfName)
                                                        if (onNavigate != null) {
                                                            onNavigate("pdf_viewer")
                                                        } else {
                                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                                setDataAndType(uri, "application/pdf")
                                                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                            }
                                                            context.startActivity(Intent.createChooser(intent, "Open Booklet PDF"))
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Cannot open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudUpload,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = target.pdfName,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = subTextColor
                                                )
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteEdunitiTarget(target.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete target",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Suggestion Pill
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF042F1A) else Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💡", fontSize = 14.sp)
                                    Text(
                                        text = "Target: Solve $perDaySuggestion questions/day (${target.totalDays} days)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFF34D399) else Color(0xFF065F46)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Counter controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${target.questionsSolved} / ${target.totalQuestions} Solved ($progressPercent%)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTargetDone) Color(0xFF10B981) else textColor
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilledTonalIconButton(
                                        onClick = {
                                            if (target.questionsSolved > 0) {
                                                val delta = 5.coerceAtMost(target.questionsSolved)
                                                viewModel.saveEdunitiTarget(target.copy(questionsSolved = target.questionsSolved - delta))
                                                val subj = when {
                                                    target.title.contains("chem", ignoreCase = true) -> "Chemistry"
                                                    target.title.contains("bio", ignoreCase = true) || target.title.contains("botan", ignoreCase = true) || target.title.contains("zool", ignoreCase = true) -> "Biology"
                                                    else -> "Physics"
                                                }
                                                viewModel.recordSolvedQuestions(subj, -delta)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("-5", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    FilledTonalIconButton(
                                        onClick = {
                                            viewModel.saveEdunitiTarget(target.copy(questionsSolved = (target.questionsSolved + 5).coerceAtMost(target.totalQuestions)))
                                            val subj = when {
                                                target.title.contains("chem", ignoreCase = true) -> "Chemistry"
                                                target.title.contains("bio", ignoreCase = true) || target.title.contains("botan", ignoreCase = true) || target.title.contains("zool", ignoreCase = true) -> "Biology"
                                                else -> "Physics"
                                            }
                                            viewModel.recordSolvedQuestions(subj, 5)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("+5", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Fireworks progress bar
                            val progressFrac = (progressPercent / 100f).coerceIn(0f, 1f)
                            FireworksProgressBar(
                                progress = progressFrac,
                                height = 6.dp,
                                gradientColors = if (isTargetDone) listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF00F5A0))
                                                 else listOf(Color(0xFF6366F1), Color(0xFF818CF8), Color(0xFFA78BFA)),
                                sparkColor = if (isTargetDone) Color(0xFF10B981) else Color(0xFF818CF8),
                                isDark = isDark
                            )
                        }
                    }
                }
            }

            // ========================= TAB 4: VELOCITY CHECKLIST =========================
            else if (activeTab == "EdunitiChecklist") {
                edunitiChecklistSection(
                    allTopics = allChecklistTopics,
                    checklistState = checklistState,
                    selectedSubject = checklistSelectedSubject,
                    onSubjectSelected = { checklistSelectedSubject = it },
                    viewModel = viewModel,
                    isDark = isDark,
                    onNavigate = onNavigate
                )
            }
            item {
                com.example.ui.components.AppBrandingFooter(isDark = isDark)
            }
        }

        // ========================= MODAL: EDIT BOOK PRESET DIALOG =========================
        if (showEditDialog && editingChapterName != null) {
            val accentColor = when (selectedSubject) {
                "Physics" -> Color(0xFF6366F1)
                "Chemistry" -> Color(0xFF10B981)
                else -> Color(0xFFEC4899)
            }
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = {
                    Text(
                        text = "Edit Reference Book",
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = editingChapterName!!,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = subTextColor
                        )

                        OutlinedTextField(
                            value = editingBookNameText,
                            onValueChange = { editingBookNameText = it },
                            label = { Text("Book Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = cardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Or choose a standard book preset:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor
                        )

                        val presets = when (selectedSubject) {
                            "Physics" -> listOf("NCERT", "HC Verma", "DC Pandey", "SL Arora", "Errorless")
                            "Chemistry" -> listOf("NCERT", "OP Tandon", "MS Chouhan", "N Avasthi", "JD Lee")
                            else -> listOf("NCERT", "MTG Fingertips", "Errorless", "Dr. Ali", "Trueman")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.take(3).forEach { preset ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9),
                                    border = BorderStroke(0.8.dp, cardBorder),
                                    modifier = Modifier.clickable { editingBookNameText = preset }
                                ) {
                                    Text(
                                        text = preset,
                                        fontSize = 11.sp,
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        onClick = {
                            viewModel.saveBookProgression(
                                BookProgression(
                                    id = editingId,
                                    subject = selectedSubject,
                                    chapter = editingChapterName!!,
                                    bookType = editingBookType,
                                    bookName = editingBookNameText.ifBlank { "NCERT" },
                                    progressPercent = editingProgress,
                                    status = if (editingProgress == 100) "Completed" else if (editingProgress > 0) "In Progress" else "Not Started"
                                )
                            )
                            showEditDialog = false
                        }
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel", color = subTextColor)
                    }
                },
                containerColor = dialogBg
            )
        }

        // ========================= MODAL: ADD EDUNITI TARGET DIALOG =========================
        if (showAddEdunitiTargetDialog) {
            AlertDialog(
                onDismissRequest = { showAddEdunitiTargetDialog = false },
                title = {
                    Text(
                        text = "Add Booklet Target",
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Set your target booklet & time frame to calculate your daily question velocity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor
                        )

                        OutlinedTextField(
                            value = edunitiTitle,
                            onValueChange = { edunitiTitle = it },
                            label = { Text("Target Title / Topic") },
                            placeholder = { Text("e.g., Optics 200 Questions Booklet") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = cardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = edunitiTotalQuestionsStr,
                            onValueChange = { edunitiTotalQuestionsStr = it },
                            label = { Text("Total Questions") },
                            placeholder = { Text("e.g., 150") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = cardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = edunitiTotalDaysStr,
                            onValueChange = { edunitiTotalDaysStr = it },
                            label = { Text("Total Days") },
                            placeholder = { Text("e.g., 5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = cardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Upload booklet trigger
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0x0AFFFFFF) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pdfPickerLauncher.launch("application/pdf") }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Upload icon",
                                    tint = if (uploadedPdfName != null) Color(0xFF10B981) else Color(0xFF6366F1)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (uploadedPdfName != null) "PDF Attached" else "Attach Booklet PDF (Optional)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = textColor
                                    )
                                    Text(
                                        text = uploadedPdfName ?: "Tap to choose file",
                                        fontSize = 10.sp,
                                        color = subTextColor
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        onClick = {
                            viewModel.saveEdunitiTarget(
                                EdunitiTarget(
                                    title = edunitiTitle.ifBlank { "Untitled Booklet Target" },
                                    totalQuestions = edunitiTotalQuestionsStr.toIntOrNull() ?: 0,
                                    totalDays = edunitiTotalDaysStr.toIntOrNull() ?: 1,
                                    questionsSolved = 0,
                                    pdfName = uploadedPdfName,
                                    pdfUri = uploadedPdfUri
                                )
                            )
                            edunitiTitle = ""
                            edunitiTotalQuestionsStr = ""
                            edunitiTotalDaysStr = ""
                            uploadedPdfName = null
                            uploadedPdfUri = null
                            showAddEdunitiTargetDialog = false
                        }
                    ) {
                        Text("Add Target", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            edunitiTitle = ""
                            edunitiTotalQuestionsStr = ""
                            edunitiTotalDaysStr = ""
                            uploadedPdfName = null
                            uploadedPdfUri = null
                            showAddEdunitiTargetDialog = false
                        }
                    ) {
                        Text("Cancel", color = subTextColor)
                    }
                },
                containerColor = dialogBg
            )
        }

        if (activeTab == "EdunitiChecklist") {
            FloatingActionButton(
                onClick = { checklistPdfLauncher.launch("application/pdf") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp),
                containerColor = Color(0xFF8B5CF6)
            ) {
                if (isChecklistUploading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Upload PDF", tint = Color.White)
                }
            }
        }
    }
}

