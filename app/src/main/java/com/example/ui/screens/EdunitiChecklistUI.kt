package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Circle
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppViewModel
import kotlinx.coroutines.launch
import com.example.data.DynamicChecklistTopic
import org.json.JSONArray

import com.example.data.NeetTenYearTrendDatabase

val NEET_CHECKLIST_YEARS: List<String>
    get() {
        val maxYear = maxOf(2026, NeetTenYearTrendDatabase.currentExamYear)
        return (2020..maxYear).map { it.toString() }
    }

fun filterYearOnlyColumns(rawColumns: String): List<String> {
    return NEET_CHECKLIST_YEARS
}

fun createTopic(subject: String, phaseTitle: String, topicName: String, days: Int, columns: String): DynamicChecklistTopic {
    return DynamicChecklistTopic(
        subject = subject,
        phaseTitle = phaseTitle,
        topicName = topicName,
        days = days,
        columns = columns
    )
}

fun getFullNeetChecklist(): List<DynamicChecklistTopic> {
    val pyqCols = "2020,2021,2022,2023,2024,2025,2026"
    return listOf(
        // ==================== PHYSICS ====================
        createTopic("Physics", "Units & Kinematics", "Units & Measurements", 2, pyqCols),
        createTopic("Physics", "Units & Kinematics", "Motion in a Straight Line", 3, pyqCols),
        createTopic("Physics", "Units & Kinematics", "Motion in a Plane & Vectors", 3, pyqCols),

        createTopic("Physics", "Mechanics & Gravitation", "Laws of Motion & Friction", 4, pyqCols),
        createTopic("Physics", "Mechanics & Gravitation", "Work, Power & Energy", 4, pyqCols),
        createTopic("Physics", "Mechanics & Gravitation", "System of Particles & Rotational Motion", 5, pyqCols),
        createTopic("Physics", "Mechanics & Gravitation", "Gravitation", 3, pyqCols),

        createTopic("Physics", "Properties of Matter & Thermodynamics", "Mechanical Properties of Solids", 2, pyqCols),
        createTopic("Physics", "Properties of Matter & Thermodynamics", "Mechanical Properties of Fluids", 3, pyqCols),
        createTopic("Physics", "Properties of Matter & Thermodynamics", "Thermal Properties of Matter & Calorimetry", 2, pyqCols),
        createTopic("Physics", "Properties of Matter & Thermodynamics", "Thermodynamics", 4, pyqCols),
        createTopic("Physics", "Properties of Matter & Thermodynamics", "Kinetic Theory of Gases (KTG)", 2, pyqCols),

        createTopic("Physics", "Oscillations & Waves", "Oscillations & Simple Harmonic Motion", 3, pyqCols),
        createTopic("Physics", "Oscillations & Waves", "Waves & Sound", 3, pyqCols),

        createTopic("Physics", "Electrodynamics", "Electric Charges, Fields & Potential", 5, pyqCols),
        createTopic("Physics", "Electrodynamics", "Capacitance & Dielectrics", 3, pyqCols),
        createTopic("Physics", "Electrodynamics", "Current Electricity & Circuits", 5, pyqCols),
        createTopic("Physics", "Electrodynamics", "Moving Charges & Magnetism", 4, pyqCols),
        createTopic("Physics", "Electrodynamics", "Magnetism & Matter", 2, pyqCols),
        createTopic("Physics", "Electrodynamics", "Electromagnetic Induction (EMI)", 3, pyqCols),
        createTopic("Physics", "Electrodynamics", "Alternating Current (AC)", 3, pyqCols),
        createTopic("Physics", "Electrodynamics", "Electromagnetic Waves (EM Waves)", 2, pyqCols),

        createTopic("Physics", "Optics", "Ray Optics & Optical Instruments", 5, pyqCols),
        createTopic("Physics", "Optics", "Wave Optics", 3, pyqCols),

        createTopic("Physics", "Modern Physics & Devices", "Dual Nature of Radiation & Matter", 3, pyqCols),
        createTopic("Physics", "Modern Physics & Devices", "Atoms & Hydrogen Spectrum", 2, pyqCols),
        createTopic("Physics", "Modern Physics & Devices", "Nuclei & Radioactivity", 2, pyqCols),
        createTopic("Physics", "Modern Physics & Devices", "Semiconductor Electronics & Logic Gates", 4, pyqCols),
        createTopic("Physics", "Modern Physics & Devices", "Experimental Physics & Instruments", 2, pyqCols),

        // ==================== CHEMISTRY ====================
        createTopic("Chemistry", "Physical Chemistry", "Some Basic Concepts of Chemistry (Mole Concept)", 3, pyqCols),
        createTopic("Chemistry", "Physical Chemistry", "Structure of Atom", 3, pyqCols),
        createTopic("Chemistry", "Physical Chemistry", "Chemical Thermodynamics & Energetics", 4, pyqCols),
        createTopic("Chemistry", "Physical Chemistry", "Chemical & Ionic Equilibrium", 5, pyqCols),
        createTopic("Chemistry", "Physical Chemistry", "Redox Reactions", 2, pyqCols),
        createTopic("Chemistry", "Physical Chemistry", "Solutions & Colligative Properties", 4, pyqCols),
        createTopic("Chemistry", "Physical Chemistry", "Electrochemistry", 4, pyqCols),
        createTopic("Chemistry", "Physical Chemistry", "Chemical Kinetics", 3, pyqCols),

        createTopic("Chemistry", "Inorganic Chemistry", "Classification of Elements & Periodicity", 2, pyqCols),
        createTopic("Chemistry", "Inorganic Chemistry", "Chemical Bonding & Molecular Structure", 5, pyqCols),
        createTopic("Chemistry", "Inorganic Chemistry", "p-Block Elements (Group 13 to 18)", 4, pyqCols),
        createTopic("Chemistry", "Inorganic Chemistry", "d and f Block Elements", 3, pyqCols),
        createTopic("Chemistry", "Inorganic Chemistry", "Coordination Compounds", 4, pyqCols),

        createTopic("Chemistry", "Organic Chemistry", "General Organic Chemistry (GOC) & Isomerism", 5, pyqCols),
        createTopic("Chemistry", "Organic Chemistry", "Hydrocarbons", 4, pyqCols),
        createTopic("Chemistry", "Organic Chemistry", "Haloalkanes & Haloarenes", 3, pyqCols),
        createTopic("Chemistry", "Organic Chemistry", "Alcohols, Phenols & Ethers", 4, pyqCols),
        createTopic("Chemistry", "Organic Chemistry", "Aldehydes, Ketones & Carboxylic Acids", 5, pyqCols),
        createTopic("Chemistry", "Organic Chemistry", "Organic Compounds Containing Nitrogen (Amines)", 3, pyqCols),
        createTopic("Chemistry", "Organic Chemistry", "Biomolecules", 3, pyqCols),
        createTopic("Chemistry", "Organic Chemistry", "Principles Related to Practical Chemistry", 2, pyqCols),

        // ==================== BIOLOGY ====================
        createTopic("Biology", "Diversity & Structural Org.", "The Living World", 1, pyqCols),
        createTopic("Biology", "Diversity & Structural Org.", "Biological Classification", 2, pyqCols),
        createTopic("Biology", "Diversity & Structural Org.", "Plant Kingdom", 3, pyqCols),
        createTopic("Biology", "Diversity & Structural Org.", "Animal Kingdom", 4, pyqCols),
        createTopic("Biology", "Diversity & Structural Org.", "Morphology of Flowering Plants", 3, pyqCols),
        createTopic("Biology", "Diversity & Structural Org.", "Anatomy of Flowering Plants", 3, pyqCols),
        createTopic("Biology", "Diversity & Structural Org.", "Structural Organisation in Animals", 2, pyqCols),

        createTopic("Biology", "Cell Biology & Biomolecules", "Cell: The Unit of Life", 3, pyqCols),
        createTopic("Biology", "Cell Biology & Biomolecules", "Biomolecules", 3, pyqCols),
        createTopic("Biology", "Cell Biology & Biomolecules", "Cell Cycle & Cell Division", 3, pyqCols),

        createTopic("Biology", "Plant Physiology", "Photosynthesis in Higher Plants", 3, pyqCols),
        createTopic("Biology", "Plant Physiology", "Respiration in Plants", 3, pyqCols),
        createTopic("Biology", "Plant Physiology", "Plant Growth & Development", 2, pyqCols),

        createTopic("Biology", "Human Physiology", "Breathing & Exchange of Gases", 2, pyqCols),
        createTopic("Biology", "Human Physiology", "Body Fluids & Circulation", 3, pyqCols),
        createTopic("Biology", "Human Physiology", "Excretory Products & Their Elimination", 2, pyqCols),
        createTopic("Biology", "Human Physiology", "Locomotion & Movement", 2, pyqCols),
        createTopic("Biology", "Human Physiology", "Neural Control & Coordination", 3, pyqCols),
        createTopic("Biology", "Human Physiology", "Chemical Coordination & Integration", 3, pyqCols),

        createTopic("Biology", "Reproduction & Development", "Sexual Reproduction in Flowering Plants", 3, pyqCols),
        createTopic("Biology", "Reproduction & Development", "Human Reproduction", 3, pyqCols),
        createTopic("Biology", "Reproduction & Development", "Reproductive Health", 2, pyqCols),

        createTopic("Biology", "Genetics & Evolution", "Principles of Inheritance & Variation", 5, pyqCols),
        createTopic("Biology", "Genetics & Evolution", "Molecular Basis of Inheritance", 5, pyqCols),
        createTopic("Biology", "Genetics & Evolution", "Evolution", 3, pyqCols),

        createTopic("Biology", "Biology in Human Welfare", "Human Health & Disease", 3, pyqCols),
        createTopic("Biology", "Biology in Human Welfare", "Microbes in Human Welfare", 2, pyqCols),
        createTopic("Biology", "Biology in Human Welfare", "Biotechnology: Principles & Processes", 4, pyqCols),
        createTopic("Biology", "Biology in Human Welfare", "Biotechnology & Its Applications", 3, pyqCols),

        createTopic("Biology", "Ecology & Environment", "Organisms & Populations", 3, pyqCols),
        createTopic("Biology", "Ecology & Environment", "Ecosystem", 3, pyqCols),
        createTopic("Biology", "Ecology & Environment", "Biodiversity & Conservation", 2, pyqCols)
    )
}

fun LazyListScope.edunitiChecklistSection(
    allTopics: List<DynamicChecklistTopic>,
    checklistState: Map<String, Boolean>,
    selectedSubject: String,
    onSubjectSelected: (String) -> Unit,
    viewModel: AppViewModel,
    isDark: Boolean,
    onNavigate: ((String) -> Unit)? = null
) {
    val activeExamGoal = viewModel.activeExamGoal.value
    val availableSubjects = com.example.data.ExamSyllabusDatabase.getSubjectsFor(activeExamGoal).ifEmpty {
        listOf("Physics", "Chemistry", "Biology")
    }

    item(key = "chk_subj_selector") {
        if (availableSubjects.size <= 3) {
            AppSegmentedControl(
                items = availableSubjects,
                selectedItem = selectedSubject,
                onItemSelected = onSubjectSelected,
                itemLabel = { it },
                itemEmoji = {
                    when (it) {
                        "Physics" -> "⚛️"
                        "Chemistry" -> "🧪"
                        else -> "🧬"
                    }
                },
                selectedColor = when (selectedSubject) {
                    "Physics" -> Color(0xFF4F46E5)
                    "Chemistry" -> Color(0xFF059669)
                    else -> Color(0xFFDB2777)
                },
                selectedGradient = when (selectedSubject) {
                    "Physics" -> listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                    "Chemistry" -> listOf(Color(0xFF10B981), Color(0xFF059669))
                    else -> listOf(Color(0xFFEC4899), Color(0xFFDB2777))
                },
                isDark = isDark
            )
        } else {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableSubjects.size) { idx ->
                    val subj = availableSubjects[idx]
                    val isSelected = selectedSubject == subj
                    Surface(
                        onClick = { onSubjectSelected(subj) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF6366F1) else (if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF818CF8) else (if (isDark) Color(0x1FFFFFFF) else Color(0xFFE2E8F0))
                        )
                    ) {
                        Text(
                            subj,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else (if (isDark) Color.White else Color(0xFF0F172A))
                        )
                    }
                }
            }
        }
    }

    item(key = "chk_master_actions_$selectedSubject") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.importAllChecklistForExam(activeExamGoal, clearExisting = false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedSubject) {
                        "Physics" -> Color(0xFF4F46E5)
                        "Chemistry" -> Color(0xFF059669)
                        "Biology" -> Color(0xFFDB2777)
                        else -> Color(0xFF6366F1)
                    }
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "📥 Load Master Chapters",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            OutlinedButton(
                onClick = { viewModel.importAllChecklistForExam(activeExamGoal, clearExisting = true) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "🔄 Reset All",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    val subjectTopics = allTopics.filter { it.subject == selectedSubject }

    var totalCheckboxes = 0
    var completedCheckboxes = 0
    subjectTopics.forEach { topic ->
        val cols = filterYearOnlyColumns(topic.columns)
        cols.forEach { col ->
            totalCheckboxes++
            val key = "dyn_${topic.id}_$col"
            if (checklistState[key] == true) {
                completedCheckboxes++
            }
        }
    }
    val progressPercentage = if (totalCheckboxes > 0) (completedCheckboxes * 100) / totalCheckboxes else 0

    item(key = "chk_progress_$selectedSubject") {
        GlowingSquareProgressCard(
            subject = selectedSubject,
            progressPercentage = progressPercentage,
            completedTasks = completedCheckboxes,
            totalTasks = totalCheckboxes,
            isDark = isDark
        )
    }

    if (subjectTopics.isEmpty()) {
        item(key = "chk_empty_$selectedSubject") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF15121B) else Color(0xFFF8FAFC)
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "📋 No $selectedSubject Chapters in Checklist",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Load the complete official NEET chapter-wise checklist with PYQ Year tracking (2020-2026), or upload your custom coaching planner PDF.",
                        fontSize = 13.sp,
                        color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.importAllNeetChecklist(clearExisting = false) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (selectedSubject) {
                                "Physics" -> Color(0xFF4F46E5)
                                "Chemistry" -> Color(0xFF059669)
                                else -> Color(0xFFDB2777)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("📥 Load Complete $selectedSubject Chapters", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        val grouped = subjectTopics.groupBy { it.phaseTitle }
        grouped.forEach { (phase, topics) ->
            item(key = "phase_${selectedSubject}_$phase") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = phase.uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = "(${topics.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isDark) Color(0x88FFFFFF) else Color(0xFF64748B)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.deleteDynamicChecklistPhase(phase, selectedSubject) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Phase",
                            tint = Color.Red.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            items(topics, key = { "topic_${it.id}" }) { topic ->
                DynamicTopicRow(topic, checklistState, viewModel, isDark, onNavigate)
            }
        }
    }
}

@Composable
fun GlowingSquareProgressCard(
    subject: String,
    progressPercentage: Int,
    completedTasks: Int,
    totalTasks: Int,
    isDark: Boolean
) {
    val neonColor = when (subject) {
        "Physics" -> Color(0xFF8B5CF6)   // Neon Violet
        "Chemistry" -> Color(0xFF06B6D4) // Electric Cyan
        else -> Color(0xFF10B981)        // Emerald Green
    }
    val cardBg = if (isDark) Color(0xFF15121B) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        neonColor.copy(alpha = 0.5f),
                        Color(0xFFEC4899).copy(alpha = 0.4f),
                        neonColor.copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(neonColor)
                    )
                    Text(
                        text = "$subject Velocity Checklist",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$completedTasks / $totalTasks Sub-tasks Completed",
                    fontSize = 11.5.sp,
                    color = subTextColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Fireworks Progress Bar
                val progressFraction = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks.toFloat()).coerceIn(0f, 1f) else 0f
                FireworksProgressBar(
                    progress = progressFraction,
                    height = 7.dp,
                    gradientColors = listOf(neonColor, Color(0xFF06B6D4), Color(0xFFFFD600)),
                    sparkColor = neonColor,
                    isDark = isDark
                )
            }

            // Compact Progress Ring
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(neonColor.copy(alpha = 0.08f))
                    .border(1.dp, neonColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(46.dp)) {
                    val strokeWidth = 5.dp.toPx()
                    drawCircle(
                        color = neonColor.copy(alpha = 0.15f),
                        style = Stroke(width = strokeWidth)
                    )
                    val sweepAngle = (progressPercentage / 100f) * 360f
                    drawArc(
                        brush = Brush.sweepGradient(listOf(neonColor, Color(0xFF06B6D4), neonColor)),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$progressPercentage%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = neonColor
                    )
                    Text(
                        text = "DONE",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = subTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicTopicRow(
    topic: DynamicChecklistTopic,
    checklistState: Map<String, Boolean>,
    viewModel: AppViewModel,
    isDark: Boolean,
    onNavigate: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val cardBg = if (isDark) Color(0xFF15121B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    val columns = remember(topic.columns) {
        filterYearOnlyColumns(topic.columns)
    }

    val completedCount = remember(columns, checklistState, topic.id) {
        columns.count { checklistState["dyn_${topic.id}_$it"] == true }
    }
    val isAllDone = columns.isNotEmpty() && completedCount == columns.size

    val rowBorder = remember(isDark, isAllDone) {
        if (isAllDone) Color(0xFF10B981).copy(alpha = 0.6f)
        else if (isDark) Color(0x22FFFFFF)
        else Color(0xFFE2E8F0)
    }

    var showLinkDialog by remember { mutableStateOf(false) }
    var showAiAssistantDialog by remember { mutableStateOf(false) }
    var inputUrlText by remember { mutableStateOf(topic.customPlaylistUrl ?: "") }

    val playlistUri = remember(topic.subject, topic.topicName, topic.customPlaylistUrl) {
        val customUrl = topic.customPlaylistUrl
        if (!customUrl.isNullOrBlank()) {
            if (customUrl.startsWith("http://") || customUrl.startsWith("https://")) {
                customUrl
            } else {
                "https://www.youtube.com/results?search_query=" + Uri.encode(customUrl)
            }
        } else {
            val defaultQuery = when (topic.subject) {
                "Physics" -> "Eduniti Physics ${topic.topicName} revision playlist"
                "Chemistry" -> "ChemSiksha Chemistry ${topic.topicName} playlist"
                "Biology" -> "NEET Biology ${topic.topicName} playlist"
                else -> "${topic.subject} ${topic.topicName} playlist"
            }
            "https://www.youtube.com/results?search_query=" + Uri.encode(defaultQuery)
        }
    }

    val youtubeIntent = remember(playlistUri) {
        Intent(Intent.ACTION_VIEW, Uri.parse(playlistUri))
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Custom Playlist / Link", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Paste YouTube URL or search channel for '${topic.topicName}':",
                        fontSize = 12.sp,
                        color = if (isDark) Color.LightGray else Color.DarkGray
                    )
                    OutlinedTextField(
                        value = inputUrlText,
                        onValueChange = { inputUrlText = it },
                        placeholder = { Text("e.g. https://youtube.com/playlist?list=...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val savedUrl = inputUrlText.trim().ifBlank { null }
                        viewModel.saveDynamicChecklistTopic(topic.copy(customPlaylistUrl = savedUrl))
                        showLinkDialog = false
                        Toast.makeText(context, "Playlist link saved!", Toast.LENGTH_SHORT).show()
                        if (!savedUrl.isNullOrBlank()) {
                            viewModel.openInStudyTube(
                                urlOrIdOrQuery = savedUrl,
                                title = "${topic.subject}: ${topic.topicName}",
                                subject = topic.subject,
                                isPlaylist = true
                            )
                            onNavigate?.invoke("studytube")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text(if (inputUrlText.isNotBlank()) "Save & Open in StudyTube" else "Save Link", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    inputUrlText = ""
                    viewModel.saveDynamicChecklistTopic(topic.copy(customPlaylistUrl = null))
                    showLinkDialog = false
                    Toast.makeText(context, "Reset to default playlist", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Reset Default", color = Color.Red)
                }
            }
        )
    }

    if (showAiAssistantDialog) {
        EdunitiGeminiAssistantDialog(
            contextType = topic.subject,
            onDismiss = { showAiAssistantDialog = false },
            isDark = isDark,
            initialTopicName = topic.topicName
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg)
            .border(1.dp, rowBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Top compact header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isAllDone) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = topic.topicName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = if (isAllDone) Color(0xFF10B981) else textColor,
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // YouTube Playlist Chip -> Directly opens in StudyTube
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFF0000).copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, Color(0xFFFF0000).copy(alpha = 0.3f)),
                    modifier = Modifier.clickable {
                        val targetUrlOrQuery = if (!topic.customPlaylistUrl.isNullOrBlank()) {
                            topic.customPlaylistUrl!!
                        } else {
                            when (topic.subject) {
                                "Physics" -> "Eduniti Physics ${topic.topicName} revision playlist"
                                "Chemistry" -> "ChemSiksha Chemistry ${topic.topicName} playlist"
                                "Biology" -> "NEET Biology ${topic.topicName} playlist"
                                else -> "${topic.subject} ${topic.topicName} playlist"
                            }
                        }

                        // Open directly in StudyTube
                        viewModel.openInStudyTube(
                            urlOrIdOrQuery = targetUrlOrQuery,
                            title = "${topic.subject}: ${topic.topicName}",
                            subject = topic.subject,
                            isPlaylist = true
                        )
                        if (onNavigate != null) {
                            onNavigate("studytube")
                        } else {
                            try {
                                context.startActivity(youtubeIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Opening in StudyTube...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "StudyTube Playlist",
                            tint = Color(0xFFFF0000),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (!topic.customPlaylistUrl.isNullOrBlank()) "Link" else "Playlist",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF0000)
                        )
                    }
                }

                // Link Edit
                IconButton(
                    onClick = {
                        inputUrlText = topic.customPlaylistUrl ?: ""
                        showLinkDialog = true
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Edit Link",
                        tint = if (isDark) Color.LightGray else Color.DarkGray,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Days chip
                Text(
                    text = "${topic.days}d",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                )

                // AI Guide Chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF6366F1).copy(alpha = 0.15f),
                    border = BorderStroke(0.8.dp, Color(0xFF6366F1).copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { showAiAssistantDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Guide",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "AI Guide",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF818CF8)
                        )
                    }
                }

                // Delete topic
                IconButton(
                    onClick = { viewModel.deleteDynamicChecklistTopic(topic.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.6f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Compact milestone chip row
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 1.dp)
        ) {
            items(columns.size) { idx ->
                val col = columns[idx]
                val key = "dyn_${topic.id}_$col"
                val isChecked = checklistState[key] == true
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isChecked) Color(0xFF10B981).copy(alpha = 0.15f) else (if (isDark) Color(0x0DFFFFFF) else Color(0xFFF1F5F9)),
                    border = BorderStroke(
                        0.8.dp,
                        if (isChecked) Color(0xFF10B981).copy(alpha = 0.6f) else (if (isDark) Color(0x18FFFFFF) else Color(0xFFCBD5E1))
                    ),
                    modifier = Modifier.clickable {
                        viewModel.toggleEdunitiChecklistItem(
                            key = key,
                            value = !isChecked,
                            chapterName = topic.topicName,
                            milestone = col
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = null,
                            tint = if (isChecked) Color(0xFF10B981) else (if (isDark) Color(0x66FFFFFF) else Color(0xFF94A3B8)),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = col,
                            fontSize = 9.5.sp,
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                            color = if (isChecked) Color(0xFF10B981) else (if (isDark) Color(0xCCFFFFFF) else Color(0xFF475569)),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
