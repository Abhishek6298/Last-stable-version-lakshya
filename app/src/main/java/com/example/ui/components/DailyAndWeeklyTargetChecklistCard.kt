package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Goal
import com.example.data.GoalStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyAndWeeklyTargetChecklistCard(
    goals: List<Goal>,
    isDark: Boolean,
    onToggleGoalStatus: (goalId: Int, newStatus: String) -> Unit,
    onDeleteGoal: (goalId: Int) -> Unit,
    onAddGoal: (subject: String, text: String, targetType: String, date: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    
    // Active Tab: "today" (Daily) or "weekly" (Weekly)
    var selectedTargetTab by remember { mutableStateOf("today") }
    
    // Filter: "All", "Pending", "Completed"
    var selectedFilter by remember { mutableStateOf("All") }
    
    // Quick Add state
    var showQuickAdd by remember { mutableStateOf(false) }
    var newGoalText by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Physics") }
    val focusManager = LocalFocusManager.current

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0x0AFFFFFF) else Color.White
    val cardBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)

    val todayGoals = remember(goals, todayDate) {
        goals.filter { (it.targetType == "today" || it.targetType.isBlank()) && (it.date.isBlank() || it.date == todayDate) }
    }
    val weeklyGoals = remember(goals) {
        goals.filter { it.targetType == "weekly" }
    }

    val currentList = if (selectedTargetTab == "today") todayGoals else weeklyGoals
    val pendingCount = currentList.count { it.status != GoalStatus.COMPLETED.value }
    val completedCount = currentList.count { it.status == GoalStatus.COMPLETED.value }
    val totalCount = currentList.size
    val progressPct = if (totalCount > 0) ((completedCount.toFloat() / totalCount.toFloat()) * 100).toInt() else 0
    val isAllCleared = totalCount > 0 && completedCount == totalCount

    val filteredList = remember(currentList, selectedFilter) {
        when (selectedFilter) {
            "Pending" -> currentList.filter { it.status != GoalStatus.COMPLETED.value }
            "Completed" -> currentList.filter { it.status == GoalStatus.COMPLETED.value }
            else -> currentList
        }
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    if (selectedTargetTab == "today") {
                        listOf(Color(0xFF6366F1), Color(0xFF3B82F6), Color(0xFF06B6D4))
                    } else {
                        listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFF43F5E))
                    }
                ),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedTargetTab == "today") Color(0x336366F1) else Color(0x338B5CF6)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTargetTab == "today") "🎯" else "📅",
                            fontSize = 22.sp
                        )
                    }
                    Column {
                        Text(
                            text = "Targets Checklist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                        Text(
                            text = if (selectedTargetTab == "today") "Daily Focus & Question Goals" else "Weekly Milestone Checklist",
                            style = MaterialTheme.typography.labelSmall,
                            color = subTextColor
                        )
                    }
                }

                // Quick Add Toggle Button
                Surface(
                    onClick = { showQuickAdd = !showQuickAdd },
                    shape = RoundedCornerShape(12.dp),
                    color = if (showQuickAdd) Color(0xFF6366F1) else if (isDark) Color(0x226366F1) else Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (showQuickAdd) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (showQuickAdd) "Cancel" else "Add Target",
                            tint = if (showQuickAdd) Color.White else Color(0xFF6366F1),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (showQuickAdd) "Close" else "Add Target",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showQuickAdd) Color.White else Color(0xFF6366F1)
                        )
                    }
                }
            }

            // Dual Tab Switcher: "🎯 Daily Targets" vs "📅 Weekly Targets"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0x15FFFFFF) else Color(0xFFF1F5F9))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Daily Tab
                val isTodayTab = selectedTargetTab == "today"
                val todayDone = todayGoals.count { it.status == GoalStatus.COMPLETED.value }
                val todayTotal = todayGoals.size
                Surface(
                    onClick = { selectedTargetTab = "today" },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isTodayTab) Color(0xFF6366F1) else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 Daily Targets",
                            fontSize = 13.sp,
                            fontWeight = if (isTodayTab) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTodayTab) Color.White else subTextColor
                        )
                        if (todayTotal > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isTodayTab) Color.White.copy(alpha = 0.25f) else Color(0x226366F1))
                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "$todayDone/$todayTotal",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTodayTab) Color.White else Color(0xFF6366F1)
                                )
                            }
                        }
                    }
                }

                // Weekly Tab
                val isWeeklyTab = selectedTargetTab == "weekly"
                val weeklyDone = weeklyGoals.count { it.status == GoalStatus.COMPLETED.value }
                val weeklyTotal = weeklyGoals.size
                Surface(
                    onClick = { selectedTargetTab = "weekly" },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isWeeklyTab) Color(0xFF8B5CF6) else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 Weekly Targets",
                            fontSize = 13.sp,
                            fontWeight = if (isWeeklyTab) FontWeight.Bold else FontWeight.Medium,
                            color = if (isWeeklyTab) Color.White else subTextColor
                        )
                        if (weeklyTotal > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isWeeklyTab) Color.White.copy(alpha = 0.25f) else Color(0x228B5CF6))
                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "$weeklyDone/$weeklyTotal",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWeeklyTab) Color.White else Color(0xFF8B5CF6)
                                )
                            }
                        }
                    }
                }
            }

            // Target Progress & Streak Summary Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0x12FFFFFF) else Color(0xFFF8FAFC))
                    .border(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isAllCleared) "🎉 Outstanding! All Done!" else "⚡ $completedCount of $totalCount Completed",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAllCleared) Color(0xFF10B981) else textColor
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isAllCleared) Color(0x3310B981) else Color(0x226366F1))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$progressPct%",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isAllCleared) Color(0xFF10B981) else Color(0xFF6366F1)
                            )
                        }
                    }

                    // Progress Bar
                    val animatedProgress by animateFloatAsState(
                        targetValue = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f,
                        animationSpec = tween(600, easing = FastOutSlowInEasing),
                        label = "targetProgressAnim"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        if (isAllCleared) {
                                            listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF00F5A0))
                                        } else if (selectedTargetTab == "today") {
                                            listOf(Color(0xFF6366F1), Color(0xFF3B82F6), Color(0xFF06B6D4))
                                        } else {
                                            listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFF43F5E))
                                        }
                                    )
                                )
                        )
                    }

                    if (totalCount > 0 && !isAllCleared) {
                        Text(
                            text = "🔥 $pendingCount task${if (pendingCount == 1) "" else "s"} remaining. Check off items as you complete your revision!",
                            fontSize = 11.sp,
                            color = subTextColor
                        )
                    }
                }
            }

            // Quick Add Input Box (Expandable)
            AnimatedVisibility(
                visible = showQuickAdd,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0x1A6366F1) else Color(0xFFEEF2FF))
                        .border(1.2.dp, Color(0xFF6366F1).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "✨ Add New ${if (selectedTargetTab == "today") "Daily" else "Weekly"} Target",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        // Subject Selector Chips
                        val subjects = listOf("Physics" to "⚡", "Chemistry" to "🧪", "Biology" to "🧬", "General" to "🎯")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            subjects.forEach { (subj, emoji) ->
                                val isSubjSelected = selectedSubject == subj
                                Surface(
                                    onClick = { selectedSubject = subj },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSubjSelected) Color(0xFF6366F1) else if (isDark) Color(0x18FFFFFF) else Color.White,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSubjSelected) Color(0xFF6366F1) else cardBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("$emoji $subj", fontSize = 11.sp, fontWeight = if (isSubjSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSubjSelected) Color.White else textColor)
                                    }
                                }
                            }
                        }

                        // Text Field
                        OutlinedTextField(
                            value = newGoalText,
                            onValueChange = { newGoalText = it },
                            placeholder = {
                                Text(
                                    "e.g., Solve 30 MCQs + Read NCERT Ch-4",
                                    fontSize = 12.5.sp,
                                    color = subTextColor
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = cardBorder,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    newGoalText = ""
                                    showQuickAdd = false
                                }
                            ) {
                                Text("Cancel", color = subTextColor, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newGoalText.isNotBlank()) {
                                        onAddGoal(
                                            selectedSubject,
                                            newGoalText.trim(),
                                            selectedTargetTab,
                                            if (selectedTargetTab == "today") todayDate else ""
                                        )
                                        newGoalText = ""
                                        showQuickAdd = false
                                        focusManager.clearFocus()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                enabled = newGoalText.isNotBlank()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Target", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Filter Chips: All | Pending | Completed
            if (totalCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filters = listOf("All" to totalCount, "Pending" to pendingCount, "Completed" to completedCount)
                    filters.forEach { (filterName, count) ->
                        val isFilterSelected = selectedFilter == filterName
                        Surface(
                            onClick = { selectedFilter = filterName },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isFilterSelected) {
                                if (filterName == "Completed") Color(0xFF10B981) else Color(0xFF6366F1)
                            } else if (isDark) Color(0x12FFFFFF) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isFilterSelected) Color.Transparent else cardBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = filterName,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isFilterSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isFilterSelected) Color.White else subTextColor
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isFilterSelected) Color.White.copy(alpha = 0.25f) else Color(0x22000000))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFilterSelected) Color.White else subTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Checklist Items
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0x0AFFFFFF) else Color(0xFFF8FAFC))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (totalCount == 0) "📋" else if (selectedFilter == "Completed") "⏳" else "✨",
                            fontSize = 32.sp
                        )
                        Text(
                            text = if (totalCount == 0) {
                                "No ${if (selectedTargetTab == "today") "daily" else "weekly"} targets set yet"
                            } else if (selectedFilter == "Completed") {
                                "No completed targets in this list yet"
                            } else {
                                "All ${if (selectedTargetTab == "today") "daily" else "weekly"} targets completed! Great job!"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = if (totalCount == 0) {
                                "Tap '+ Add Target' above to set your study goals for ${if (selectedTargetTab == "today") "today" else "this week"}."
                            } else {
                                "Keep your momentum going! Add new revision milestones anytime."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor,
                            fontSize = 11.5.sp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredList.forEach { goal ->
                        val isDone = goal.status == GoalStatus.COMPLETED.value
                        
                        val subjectBadgeColor = when (goal.subject.lowercase()) {
                            "physics" -> Color(0xFF3B82F6)
                            "chemistry" -> Color(0xFFF59E0B)
                            "biology" -> Color(0xFF10B981)
                            else -> Color(0xFF8B5CF6)
                        }
                        
                        val subjectEmoji = when (goal.subject.lowercase()) {
                            "physics" -> "⚡"
                            "chemistry" -> "🧪"
                            "biology" -> "🧬"
                            else -> "🎯"
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDone) {
                                if (isDark) Color(0x0C10B981) else Color(0xFFF0FDF4)
                            } else {
                                if (isDark) Color(0x12FFFFFF) else Color.White
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isDone) Color(0xFF10B981).copy(alpha = 0.5f)
                                else if (isDark) Color(0x1EFFFFFF) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onToggleGoalStatus(
                                        goal.id,
                                        if (isDone) GoalStatus.TODO.value else GoalStatus.COMPLETED.value
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Large Custom Touch Target Checkbox
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDone) Color(0xFF10B981)
                                            else if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
                                        )
                                        .clickable {
                                            onToggleGoalStatus(
                                                goal.id,
                                                if (isDone) GoalStatus.TODO.value else GoalStatus.COMPLETED.value
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Completed",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, subTextColor, CircleShape)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Content Column
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Subject Pill
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(subjectBadgeColor.copy(alpha = 0.18f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "$subjectEmoji ${goal.subject}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = subjectBadgeColor
                                            )
                                        }

                                        // Target Type / Date Pill
                                        if (goal.targetType == "weekly") {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "📅 Weekly",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF8B5CF6)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = goal.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isDone) FontWeight.Normal else FontWeight.SemiBold,
                                        color = if (isDone) subTextColor else textColor,
                                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }

                                // Delete Target Icon Button
                                IconButton(
                                    onClick = { onDeleteGoal(goal.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Goal",
                                        tint = subTextColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(17.dp)
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
