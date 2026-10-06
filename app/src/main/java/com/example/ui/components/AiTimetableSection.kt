package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.data.GeminiChatAssistant
import com.example.data.GeneratedTimetablePlan
import com.example.data.TimetableSlot
import com.example.ui.AppViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTimetableSection(
    viewModel: AppViewModel,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }

    // State for Timetable Plan
    var timetablePlan by remember {
        mutableStateOf(loadSavedTimetable(context))
    }

    var isGenerating by remember { mutableStateOf(false) }
    var showAiConfigDialog by remember { mutableStateOf(false) }
    var showAddSlotDialog by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<TimetableSlot?>(null) }
    var filterSubject by remember { mutableStateOf("All") }

    // Generation parameters
    var targetHours by remember { mutableIntStateOf(12) }
    var wakeTime by remember { mutableStateOf("06:00 AM") }
    var sleepTime by remember { mutableStateOf("11:30 PM") }
    var focusSubject by remember { mutableStateOf("Balanced NEET (PCB)") }
    var studentType by remember { mutableStateOf("Dropper 24x7 Self-Study") }
    var customRequirements by remember { mutableStateOf("") }

    // Auto-save whenever timetable changes
    fun saveCurrentPlan(newPlan: GeneratedTimetablePlan) {
        timetablePlan = newPlan
        saveTimetableToPrefs(context, newPlan)
    }

    // Calculate slot completion stats
    val totalSlots = timetablePlan.slots.size
    val completedSlots = timetablePlan.slots.count { it.isCompleted }
    val studySlots = timetablePlan.slots.filter { it.subject != "Break" }
    val completedStudySlots = studySlots.count { it.isCompleted }
    val progressPct = if (totalSlots > 0) (completedSlots.toFloat() / totalSlots.toFloat()) else 0f

    val totalStudyMinutes = studySlots.sumOf { it.durationMinutes }
    val completedStudyMinutes = studySlots.filter { it.isCompleted }.sumOf { it.durationMinutes }

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val cardBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =====================================================================
        // 🔮 SLEEK & THIN LAKSHYA AI TIMETABLE HEADER CARD
        // =====================================================================
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF0F1524) else Color(0xFFF8FAFC),
            border = BorderStroke(
                0.9.dp,
                Brush.horizontalGradient(
                    listOf(Color(0xFF6366F1).copy(alpha = 0.7f), Color(0xFFA855F7).copy(alpha = 0.5f), Color(0xFF10B981).copy(alpha = 0.6f))
                )
            ),
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().testTag("ai_timetable_hero_card")
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(Color(0xFF1E1B4B).copy(alpha = 0.35f), Color(0xFF0F172A).copy(alpha = 0.4f))
                            } else {
                                listOf(Color(0xFFEEF2FF).copy(alpha = 0.6f), Color.White)
                            }
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.2f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "LAKSHYA AI",
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(
                                    text = "LAKSHYA AI",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = Color(0xFF818CF8)
                                )
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = Color(0x2910B981)
                                ) {
                                    Text(
                                        text = "NTA 2026",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                            Text(
                                text = timetablePlan.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Slim AI Create Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF6366F1),
                        modifier = Modifier
                            .clickable { showAiConfigDialog = true }
                            .testTag("open_ai_generator_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Text("AI Create", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // AI Strategy Note (Slim Pill)
                if (timetablePlan.strategyNote.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0x1F6366F1) else Color(0xFFEEF2FF),
                        border = BorderStroke(0.6.dp, Color(0xFF818CF8).copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("💡", fontSize = 12.sp)
                            Text(
                                text = timetablePlan.strategyNote,
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA),
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2
                            )
                        }
                    }
                }

                // Slim Progress Bar & Quick Stats
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Schedule Progress",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subTextColor
                        )
                        Text(
                            text = "$completedSlots/$totalSlots (${(progressPct * 100).toInt()}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }

                    FireworksProgressBar(
                        progress = progressPct,
                        height = 6.dp,
                        gradientColors = listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF00F5A0)),
                        sparkColor = Color(0xFF34D399),
                        isDark = isDark
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📚 Target: ${(totalStudyMinutes / 60f).let { String.format(java.util.Locale.US, "%.1f", it) }}h",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = subTextColor
                        )
                        Text(
                            text = "⏱️ Done: ${(completedStudyMinutes / 60f).let { String.format(java.util.Locale.US, "%.1f", it) }}h",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF818CF8)
                        )
                    }
                }
            }
        }

        // =====================================================================
        // ⚡ QUICK 1-TAP PRESET CHIPS & ACTION ROW
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daily Time Blocks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Add Custom Slot Button
                OutlinedButton(
                    onClick = { showAddSlotDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_custom_slot_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Slot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Reset Completion Button
                IconButton(
                    onClick = {
                        val resetSlots = timetablePlan.slots.map { it.copy(isCompleted = false) }
                        saveCurrentPlan(timetablePlan.copy(slots = resetSlots))
                        Toast.makeText(context, "All slots reset for new day! 🌅", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = subTextColor, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Subject Filter Tabs
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val filters = listOf("All", "Physics", "Chemistry", "Biology", "Revision", "Break")
            items(filters, key = { it }) { item ->
                val isSelected = filterSubject == item
                val itemColor = when (item) {
                    "Physics" -> Color(0xFF3B82F6)
                    "Chemistry" -> Color(0xFFF59E0B)
                    "Biology" -> Color(0xFF10B981)
                    "Revision" -> Color(0xFFA855F7)
                    "Break" -> Color(0xFF64748B)
                    else -> Color(0xFF6366F1)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) itemColor else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                    border = BorderStroke(1.dp, if (isSelected) itemColor else Color.Transparent),
                    modifier = Modifier.clickable { filterSubject = item }
                ) {
                    Text(
                        text = item,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else subTextColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // =====================================================================
        // 📋 TIME SLOTS LIST (INTERACTIVE CARDS WITH REAL-TIME ACTIVE INDICATOR)
        // =====================================================================
        val currentMinutesOfDay = remember {
            val cal = Calendar.getInstance()
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        }

        val filteredSlots = remember(timetablePlan.slots, filterSubject) {
            if (filterSubject == "All") timetablePlan.slots else timetablePlan.slots.filter { it.subject == filterSubject }
        }

        if (filteredSlots.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📅", fontSize = 32.sp)
                    Text("No slots found for $filterSubject", fontWeight = FontWeight.Bold, color = textColor)
                    Text("Use LAKSHYA AI or tap 'Add Slot' to create your routine.", fontSize = 12.sp, color = subTextColor, textAlign = TextAlign.Center)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredSlots.forEachIndexed { index, slot ->
                    key(slot.id) {
                        val isActiveNow = isSlotActive(slot.startTime, slot.endTime, currentMinutesOfDay)
                        TimetableSlotCard(
                            slot = slot,
                            isActive = isActiveNow,
                            isDark = isDark,
                            onToggleDone = { checked ->
                                val updated = timetablePlan.slots.map {
                                    if (it.id == slot.id) it.copy(isCompleted = checked) else it
                                }
                                saveCurrentPlan(timetablePlan.copy(slots = updated))
                            },
                            onEdit = { editingSlot = slot },
                            onDelete = {
                                val updated = timetablePlan.slots.filter { it.id != slot.id }
                                saveCurrentPlan(timetablePlan.copy(slots = updated))
                                Toast.makeText(context, "Slot removed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // 🔮 LAKSHYA AI TIMETABLE GENERATOR DIALOG
    // =========================================================================
    if (showAiConfigDialog) {
        AlertDialog(
            onDismissRequest = { if (!isGenerating) showAiConfigDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF6366F1))
                    Text("Generate with LAKSHYA AI", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Customize your ideal NEET study routine and let Lakshya AI craft a synchronized daily plan:",
                        fontSize = 12.sp,
                        color = subTextColor
                    )

                    // Target Study Hours Picker
                    Text("Daily Study Target: $targetHours Hours", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(8, 10, 12, 14, 16).forEach { hours ->
                            val isSel = targetHours == hours
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF6366F1) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                                modifier = Modifier.clickable { targetHours = hours }
                            ) {
                                Text(
                                    text = "${hours}h",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else textColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Wake & Sleep Times
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = wakeTime,
                            onValueChange = { wakeTime = it },
                            label = { Text("Wake-up Time", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sleepTime,
                            onValueChange = { sleepTime = it },
                            label = { Text("Bedtime", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Primary Focus Subject
                    OutlinedTextField(
                        value = focusSubject,
                        onValueChange = { focusSubject = it },
                        label = { Text("Focus / Weak Area", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Physics Numericals + Organic Chem") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Aspirant Profile Type
                    OutlinedTextField(
                        value = studentType,
                        onValueChange = { studentType = it },
                        label = { Text("Student Type", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Dropper Full Day / School + Coaching") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Custom Instructions
                    OutlinedTextField(
                        value = customRequirements,
                        onValueChange = { customRequirements = it },
                        label = { Text("Custom Requirements (Optional)", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Add 2 hours for Eduniti checklist & 100 MCQs daily") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    if (isGenerating) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF6366F1))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Lakshya AI is architecting your schedule...", fontSize = 12.sp, color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isGenerating = true
                        coroutineScope.launch {
                            val result = GeminiChatAssistant.generateAiTimetable(
                                context = context,
                                targetHours = targetHours,
                                wakeTime = wakeTime,
                                sleepTime = sleepTime,
                                focusSubject = focusSubject,
                                studentType = studentType,
                                customRequirements = customRequirements
                            )
                            isGenerating = false
                            if (result.isSuccess) {
                                val generated = result.getOrNull() ?: GeminiChatAssistant.getDefaultTimetable(targetHours)
                                saveCurrentPlan(generated)
                                showAiConfigDialog = false
                                Toast.makeText(context, "New LAKSHYA AI Timetable Activated! 🚀", Toast.LENGTH_SHORT).show()
                            } else {
                                // Fallback to instant top ranker schedule
                                val fallback = GeminiChatAssistant.getDefaultTimetable(targetHours)
                                saveCurrentPlan(fallback)
                                showAiConfigDialog = false
                                Toast.makeText(context, "Activated AI Preset Schedule (${targetHours}h)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Generate Plan 🔮", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAiConfigDialog = false },
                    enabled = !isGenerating
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // =========================================================================
    // ➕ ADD / EDIT CUSTOM SLOT DIALOG
    // =========================================================================
    if (showAddSlotDialog || editingSlot != null) {
        val isEditing = editingSlot != null
        var sStartTime by remember { mutableStateOf(editingSlot?.startTime ?: "09:00 AM") }
        var sEndTime by remember { mutableStateOf(editingSlot?.endTime ?: "11:00 AM") }
        var sSubject by remember { mutableStateOf(editingSlot?.subject ?: "Physics") }
        var sTitle by remember { mutableStateOf(editingSlot?.title ?: "") }
        var sDesc by remember { mutableStateOf(editingSlot?.description ?: "") }
        var sTag by remember { mutableStateOf(editingSlot?.tag ?: "Core Study") }

        AlertDialog(
            onDismissRequest = {
                showAddSlotDialog = false
                editingSlot = null
            },
            title = {
                Text(if (isEditing) "Edit Time Block" else "Add New Time Block", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sStartTime,
                            onValueChange = { sStartTime = it },
                            label = { Text("Start Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sEndTime,
                            onValueChange = { sEndTime = it },
                            label = { Text("End Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Subject Selector
                    Text("Subject / Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subTextColor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Physics", "Chemistry", "Biology", "Revision", "Break").forEach { subj ->
                            val isSel = sSubject == subj
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF6366F1) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                                modifier = Modifier.clickable { sSubject = subj }
                            ) {
                                Text(
                                    text = subj,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else textColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = sTitle,
                        onValueChange = { sTitle = it },
                        label = { Text("Activity Title") },
                        placeholder = { Text("e.g. Thermodynamics Numerical Practice") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = sDesc,
                        onValueChange = { sDesc = it },
                        label = { Text("Target / Description (Optional)") },
                        placeholder = { Text("e.g. 50 MCQs + Error Analysis") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sTitle.isBlank()) sTitle = "$sSubject Session"
                        if (isEditing) {
                            val updatedSlots = timetablePlan.slots.map {
                                if (it.id == editingSlot?.id) {
                                    it.copy(
                                        startTime = sStartTime,
                                        endTime = sEndTime,
                                        subject = sSubject,
                                        title = sTitle,
                                        description = sDesc,
                                        tag = sTag
                                    )
                                } else it
                            }
                            saveCurrentPlan(timetablePlan.copy(slots = updatedSlots))
                        } else {
                            val newSlot = TimetableSlot(
                                startTime = sStartTime,
                                endTime = sEndTime,
                                subject = sSubject,
                                title = sTitle,
                                description = sDesc,
                                tag = sTag,
                                isCompleted = false
                            )
                            saveCurrentPlan(timetablePlan.copy(slots = timetablePlan.slots + newSlot))
                        }
                        showAddSlotDialog = false
                        editingSlot = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text(if (isEditing) "Save Changes" else "Add Block", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddSlotDialog = false
                    editingSlot = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Single Slot Card with Realtime Active Glow & Completion Cross-off
 */
@Composable
fun TimetableSlotCard(
    slot: TimetableSlot,
    isActive: Boolean,
    isDark: Boolean,
    onToggleDone: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val subjectColor = when (slot.subject) {
        "Physics" -> Color(0xFF3B82F6)
        "Chemistry" -> Color(0xFFF59E0B)
        "Biology" -> Color(0xFF10B981)
        "Revision" -> Color(0xFFA855F7)
        "Mock Test" -> Color(0xFFEC4899)
        else -> Color(0xFF64748B)
    }

    val cardBg = if (slot.isCompleted) {
        if (isDark) Color(0x1210B981) else Color(0xFFF0FDF4)
    } else if (isActive) {
        if (isDark) Color(0xFF181C2E) else Color(0xFFEEF2FF)
    } else {
        if (isDark) Color(0xFF0F1524) else Color.White
    }

    val cardBorder = if (slot.isCompleted) {
        Color(0xFF10B981).copy(alpha = 0.4f)
    } else if (isActive) {
        Color(0xFF818CF8)
    } else {
        if (isDark) Color(0x1F818CF8) else Color(0xFFE2E8F0)
    }

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(if (isActive) 1.2.dp else 0.8.dp, cardBorder),
        shadowElevation = if (isActive) 3.dp else 0.5.dp,
        modifier = Modifier.fillMaxWidth().testTag("timetable_slot_${slot.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = slot.isCompleted,
                onCheckedChange = { onToggleDone(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF10B981),
                    uncheckedColor = subTextColor.copy(alpha = 0.6f),
                    checkmarkColor = Color.White
                ),
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Main Content Area
            Column(modifier = Modifier.weight(1f)) {
                // Adaptive Top Meta Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Subject Tag
                    Surface(
                        color = subjectColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = slot.subject,
                            color = subjectColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }

                    // Time Range Tag
                    Text(
                        text = "⏰ ${slot.startTime} – ${slot.endTime}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) Color(0xFF818CF8) else subTextColor,
                        maxLines = 1
                    )

                    // Active Now Badge
                    if (isActive && !slot.isCompleted) {
                        Surface(
                            color = Color(0xFF6366F1),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                text = "⚡ ACTIVE",
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = slot.title,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = if (slot.isCompleted) FontWeight.Normal else FontWeight.Bold,
                    color = if (slot.isCompleted) subTextColor else textColor,
                    textDecoration = if (slot.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                if (slot.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = slot.description,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = if (slot.isCompleted) subTextColor.copy(alpha = 0.6f) else subTextColor,
                        textDecoration = if (slot.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF43F5E), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/**
 * Checks if current clock time falls inside a slot's start and end times
 */
fun isSlotActive(startStr: String, endStr: String, currentMinutes: Int): Boolean {
    return try {
        val startMin = parseTimeToMinutes(startStr)
        val endMin = parseTimeToMinutes(endStr)
        if (startMin != null && endMin != null) {
            if (endMin >= startMin) {
                currentMinutes in startMin..endMin
            } else {
                // Overnight slot (e.g. 11:00 PM to 06:00 AM)
                currentMinutes >= startMin || currentMinutes <= endMin
            }
        } else false
    } catch (e: Exception) {
        false
    }
}

fun parseTimeToMinutes(timeStr: String): Int? {
    return try {
        val clean = timeStr.trim().uppercase()
        val isPm = clean.contains("PM")
        val isAm = clean.contains("AM")
        val timePart = clean.replace("AM", "").replace("PM", "").trim()
        val parts = timePart.split(":")
        if (parts.size >= 2) {
            var hour = parts[0].trim().toInt()
            val min = parts[1].trim().toInt()
            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0
            return hour * 60 + min
        }
        null
    } catch (e: Exception) {
        null
    }
}

// Storage helpers
fun saveTimetableToPrefs(context: Context, plan: GeneratedTimetablePlan) {
    try {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val root = JSONObject().apply {
            put("title", plan.title)
            put("targetHours", plan.targetHours.toDouble())
            put("strategyNote", plan.strategyNote)
            put("generatedDate", plan.generatedDate)
            val array = JSONArray()
            plan.slots.forEach { slot ->
                array.put(JSONObject().apply {
                    put("id", slot.id)
                    put("startTime", slot.startTime)
                    put("endTime", slot.endTime)
                    put("subject", slot.subject)
                    put("title", slot.title)
                    put("description", slot.description)
                    put("tag", slot.tag)
                    put("isCompleted", slot.isCompleted)
                    put("durationMinutes", slot.durationMinutes)
                })
            }
            put("slots", array)
        }
        prefs.edit().putString("saved_ai_timetable_plan", root.toString()).apply()
        try {
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(context)
        } catch (_: Exception) {}
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadSavedTimetable(context: Context): GeneratedTimetablePlan {
    return try {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("saved_ai_timetable_plan", null)
        if (!jsonStr.isNullOrBlank()) {
            val json = JSONObject(jsonStr)
            val title = json.optString("title", "NEET 12-Hour Mastery Schedule")
            val targetH = json.optDouble("targetHours", 12.0).toFloat()
            val note = json.optString("strategyNote", "")
            val gDate = json.optString("generatedDate", "")
            val slotsArr = json.optJSONArray("slots")
            val slots = mutableListOf<TimetableSlot>()
            if (slotsArr != null) {
                for (i in 0 until slotsArr.length()) {
                    val sObj = slotsArr.optJSONObject(i) ?: continue
                    slots.add(
                        TimetableSlot(
                            id = sObj.optString("id", UUID.randomUUID().toString()),
                            startTime = sObj.optString("startTime", "06:00 AM"),
                            endTime = sObj.optString("endTime", "07:30 AM"),
                            subject = sObj.optString("subject", "Biology"),
                            title = sObj.optString("title", "Core Study Block"),
                            description = sObj.optString("description", ""),
                            tag = sObj.optString("tag", "Core Study"),
                            isCompleted = sObj.optBoolean("isCompleted", false),
                            durationMinutes = sObj.optInt("durationMinutes", 90)
                        )
                    )
                }
            }
            GeneratedTimetablePlan(
                title = title,
                targetHours = targetH,
                strategyNote = note,
                slots = slots,
                generatedDate = gDate
            )
        } else {
            GeminiChatAssistant.getDefaultTimetable(12)
        }
    } catch (e: Exception) {
        GeminiChatAssistant.getDefaultTimetable(12)
    }
}
