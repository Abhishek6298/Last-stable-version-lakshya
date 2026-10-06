package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Habit
import com.example.ui.AppViewModel
import com.example.ui.components.FireworksProgressBar
import com.example.ui.components.GlassCard
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.NativeMarkdownText

import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.saveable.rememberSaveable
import android.widget.Toast
import com.example.ui.components.PasswordPromptDialog

@Composable
fun HabitTrackerScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    androidx.activity.compose.BackHandler(enabled = true) {
        onNavigateBack()
    }
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val habitPassword by viewModel.habitPassword.collectAsStateWithLifecycle()
    val securityQuestion by viewModel.securityQuestion.collectAsStateWithLifecycle()
    val securityAnswer by viewModel.securityAnswer.collectAsStateWithLifecycle()
    
    var isUnlocked by rememberSaveable { mutableStateOf(false) }

    val isProtected = !habitPassword.isNullOrBlank()

    var showAddDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("Habits") }
    var showNoteDialog by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<com.example.data.Note?>(null) }
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    var selectedHabit by remember { mutableStateOf<Habit?>(null) }

    // Intercept back presses when dialogs, modals or notes tab are active
    BackHandler(
        enabled = showAddDialog || showNoteDialog ||
                  selectedNote != null || selectedHabit != null || activeTab != "Habits"
    ) {
        when {
            showAddDialog -> showAddDialog = false
            showNoteDialog -> showNoteDialog = false
            selectedNote != null -> selectedNote = null
            selectedHabit != null -> selectedHabit = null
            activeTab != "Habits" -> activeTab = "Habits"
        }
    }

    // Seed default habits if empty
    LaunchedEffect(habits.isEmpty()) {
        if (habits.isEmpty()) {
            viewModel.addHabit("No Porn / Fap (Detox)", "\uD83D\uDEAB", false)
            viewModel.addHabit("Wake up at 6 AM", "\uD83C\uDF05", true)
            viewModel.addHabit("Drink 3L Water", "\uD83D\uDCA7", true)
            viewModel.addHabit("Exercise / Yoga", "\uD83E\uDDD8", true)
            viewModel.addHabit("No Social Media Scrolling", "\uD83D\uDCF4", false)
            viewModel.addHabit("Meditation", "\uD83E\uDDE8", true)
        }
    }

    if (isProtected && !isUnlocked) {
        PrivacyLockScreen(
            modifier = modifier,
            isDark = isDark,
            habitPassword = habitPassword,
            securityQuestion = securityQuestion,
            securityAnswer = securityAnswer,
            onUnlocked = { isUnlocked = true }
        )
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (activeTab == "Habits") showAddDialog = true 
                    else { selectedNote = null; showNoteDialog = true } 
                },
                containerColor = Color(0xFF6366F1),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(if (activeTab == "Habits") Icons.Default.Add else Icons.Default.Edit, contentDescription = "Add")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
                            tint = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Habits & Detox",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            "Track your daily routine and break bad habits",
                            fontSize = 13.sp,
                            color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
                        )
                    }
                }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Custom Adaptive Tab Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isDark) Color(0x221E293B) else Color(0xFFF1F5F9),
                                RoundedCornerShape(16.dp)
                            )
                            .border(
                                1.dp,
                                if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            "Habits" to Pair("Daily Habits", Icons.Default.TaskAlt),
                            "Notes" to Pair("Thoughts & Notes", Icons.AutoMirrored.Filled.EventNote)
                        )
                        tabs.forEach { (tabKey, tabInfo) ->
                            val isSelected = (activeTab == tabKey) || (activeTab == "Thoughts & Notes" && tabKey == "Notes")
                            val (tabTitle, tabIcon) = tabInfo
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color(0xFF6366F1) else Color.Transparent
                                    )
                                    .clickable { activeTab = tabKey }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        tabIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) Color.White else if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tabTitle,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            
            if (activeTab == "Habits") {
                items(habits, key = { it.id }) { habit ->
                    HabitCard(habit, viewModel, isDark) {
                        selectedHabit = habit
                    }
                }
            } else {
                if (notes.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, tint = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No thoughts captured yet.", color = if (isDark) Color(0x80FFFFFF) else Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Text("Tap the + button to add a note.", color = if (isDark) Color(0x66FFFFFF) else Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                } else {
                    item {
                        NotesGrid(notes, isDark) { note ->
                            selectedNote = note
                            showNoteDialog = true
                        }
                    }
                }
            }
        }

    if (showAddDialog) {
        AddHabitDialog(
            isDark = isDark,
            onDismiss = { showAddDialog = false },
            onAdd = { name, emoji, isGood ->
                viewModel.addHabit(name, emoji, isGood)
                showAddDialog = false
            }
        )
    }

    if (showNoteDialog) {
        NoteEditDialog(
            note = selectedNote,
            isDark = isDark,
            onDismiss = { showNoteDialog = false },
            onSave = { title, content, color ->
                if (selectedNote == null) {
                    viewModel.addNote(title, content, color)
                } else {
                    viewModel.updateNote(selectedNote!!.copy(title = title, content = content, color = color))
                }
                showNoteDialog = false
            },
            onDelete = if (selectedNote != null) { id ->
                viewModel.deleteNote(id)
                showNoteDialog = false
            } else null
        )
    }

    selectedHabit?.let { habit ->
        HabitDetailsDialog(
            habit = habit,
            viewModel = viewModel,
            isDark = isDark,
            onDismiss = { selectedHabit = null }
        )
    }
}
}

@Composable
fun HabitCard(habit: Habit, viewModel: AppViewModel, isDark: Boolean, onClick: () -> Unit) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val todayStr = remember(sdf) { sdf.format(Calendar.getInstance().time) }
    
    // Get last 7 days (memoized)
    val last7Days = remember(sdf, dayFormat) {
        (6 downTo 0).map { i ->
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            Pair(sdf.format(c.time), dayFormat.format(c.time).take(1))
        }
    }

    val completedDates = remember(habit.completedDates) { habit.completedDates.split(",").filter { it.isNotBlank() }.toSet() }
    val currentStreak = remember(completedDates, sdf) { calculateStreak(completedDates, sdf) }

    val primaryColor = if (habit.isGoodHabit) Color(0xFF10B981) else Color(0xFFF43F5E)
    val bgColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(habit.iconEmoji, fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            habit.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            if (habit.isGoodHabit) "Build this habit" else "Break this bad habit",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("\uD83D\uDD25", fontSize = 14.sp)
                    Text("$currentStreak", fontWeight = FontWeight.Black, fontSize = 16.sp, color = if (isDark) Color.White else Color(0xFF0F172A))
                    IconButton(onClick = { viewModel.deleteHabit(habit.id) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = if (isDark) Color(0x66FFFFFF) else Color(0x66000000), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                last7Days.forEach { (dateStr, dayLabel) ->
                    val isDone = completedDates.contains(dateStr)
                    val isToday = dateStr == todayStr
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            dayLabel,
                            fontSize = 12.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) primaryColor else if (isDark) Color(0x66FFFFFF) else Color(0x66000000)
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDone) primaryColor else if (isDark) Color(0x1AFFFFFF) else Color(0x0D000000))
                                .clickable { viewModel.toggleHabitDay(habit, dateStr) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Text(if (habit.isGoodHabit) "✔️" else "\uD83D\uDEE1\uFE0F", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            val doneDaysCount = last7Days.count { completedDates.contains(it.first) }
            val weekProg = (doneDaysCount / 7f).coerceIn(0f, 1f)

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("7-Day Consistency", fontSize = 10.5.sp, color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B))
                Text("$doneDaysCount / 7 Days (${(weekProg * 100).toInt()}%)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            }
            Spacer(modifier = Modifier.height(4.dp))
            FireworksProgressBar(
                progress = weekProg,
                height = 6.dp,
                gradientColors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f), Color(0xFFFFD600)),
                sparkColor = primaryColor,
                isDark = isDark
            )
        }
    }
}

fun calculateStreak(completedDates: Collection<String>, sdf: SimpleDateFormat): Int {
    if (completedDates.isEmpty()) return 0
    
    val cal = Calendar.getInstance()
    var streak = 0
    
    // Check if today is completed
    val today = sdf.format(cal.time)
    if (completedDates.contains(today)) {
        streak++
    }
    
    // Move to yesterday
    cal.add(Calendar.DAY_OF_YEAR, -1)
    
    while (true) {
        val dateStr = sdf.format(cal.time)
        if (completedDates.contains(dateStr)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
    }
    
    return streak
}

@Composable
fun AddHabitDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("\uD83D\uDCAA") }
    var isGood by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "New Habit",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color.Black,
                        unfocusedTextColor = if (isDark) Color.White else Color.Black
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { if (it.length <= 2) emoji = it },
                        label = { Text("Emoji") },
                        modifier = Modifier.width(90.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                        )
                    )
                    Text(
                        text = if (isGood) "Positive Habit" else "Detox / Break",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isGood) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(checked = isGood, onCheckedChange = { isGood = it })
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (isGood) "Build Positive Routine" else "Break Bad Habit (Detox)",
                        color = if (isDark) Color.White else Color.Black,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) onAdd(name, emoji, isGood)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun HabitDetailsDialog(
    habit: Habit,
    viewModel: AppViewModel,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val motivationLoading by viewModel.detoxMotivationLoading.collectAsStateWithLifecycle()
    val motivationResult by viewModel.detoxMotivationResult.collectAsStateWithLifecycle()

    val completedDates = remember(habit.completedDates) { habit.completedDates.split(",").filter { it.isNotBlank() }.toSet() }
    val currentStreak = remember(completedDates) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        calculateStreak(completedDates, sdf)
    }
    
    val primaryColor = if (habit.isGoodHabit) Color(0xFF10B981) else Color(0xFFF43F5E)

    // Generate last 30 days for heatmap
    val last30Days = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        (29 downTo 0).map { i ->
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            sdf.format(c.time)
        }
    }
    
    var userFeeling by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = {
            viewModel.clearDetoxMotivation()
            onDismiss()
        },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 32.dp, bottomEnd = 32.dp),
            color = if (isDark) Color(0xFF0B1120) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 24.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(habit.iconEmoji, fontSize = 24.sp)
                            }
                            Column {
                                Text(
                                    habit.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color.Black
                                )
                                Text(
                                    if (habit.isGoodHabit) "Good Habit Tracker" else "Detox Tracker",
                                    fontSize = 12.sp,
                                    color = primaryColor
                                )
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("\uD83D\uDD25", fontSize = 16.sp)
                            Text("$currentStreak Days", fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (isDark) Color.White else Color.Black)
                        }
                    }
                }

                // 30 Day Heatmap Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("30-Day History", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                        
                        // We use a simple grid logic, 7 items per row
                        val chunked = last30Days.chunked(7)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (row in chunked) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    for (dateStr in row) {
                                        val isDone = completedDates.contains(dateStr)
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isDone) primaryColor else if (isDark) Color(0x1AFFFFFF) else Color(0x0D000000))
                                                .clickable { viewModel.toggleHabitDay(habit, dateStr) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isDone) {
                                                Text(if (habit.isGoodHabit) "✔️" else "\uD83D\uDEE1\uFE0F", fontSize = 14.sp)
                                            }
                                        }
                                    }
                                    // Fill empty slots to keep alignment
                                    repeat(7 - row.size) {
                                        Box(modifier = Modifier.size(38.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Gemini AI Detox Coach (especially for Bad Habits)
                item {
                    if (!habit.isGoodHabit) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0x1AF43F5E) else Color(0x0DF43F5E),
                            border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("\uD83E\uDD16", fontSize = 20.sp)
                                    Text("AI Study Coach", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                                }
                                
                                if (motivationResult != null) {
                                    NativeMarkdownText(
                                        text = motivationResult!!,
                                        isDark = isDark,
                                        fontSize = 14.sp
                                    )
                                    
                                    Button(
                                        onClick = { viewModel.clearDetoxMotivation() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33F43F5E), contentColor = Color(0xFFF43F5E))
                                    ) {
                                        Text("Dismiss")
                                    }
                                } else if (motivationLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.CenterHorizontally),
                                        color = Color(0xFFF43F5E)
                                    )
                                } else {
                                    Text(
                                        "Feeling the urge? Don't break your streak. Tell the AI what you're feeling for a personalized reality check.",
                                        fontSize = 13.sp,
                                        color = if (isDark) Color(0xB3FFFFFF) else Color(0xFF64748B)
                                    )
                                    OutlinedTextField(
                                        value = userFeeling,
                                        onValueChange = { userFeeling = it },
                                        placeholder = { Text("What are you feeling right now?", fontSize = 13.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = if (isDark) Color.White else Color.Black,
                                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                                        )
                                    )
                                    Button(
                                        onClick = { viewModel.getDetoxMotivation(context, habit.name, userFeeling) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
                                    ) {
                                        Text("Stop Me! (Get AI Motivation)")
                                    }
                                }
                            }
                        }
                    } else {
                        // AI Coach for Good habits too? (Optional, but user asked "sabhi me add karo")
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0x1A10B981) else Color(0x0D10B981),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("\uD83E\uDD16", fontSize = 20.sp)
                                    Text("AI Motivation Coach", fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                                }
                                
                                if (motivationResult != null) {
                                    NativeMarkdownText(
                                        text = motivationResult!!,
                                        isDark = isDark,
                                        fontSize = 14.sp
                                    )
                                    
                                    Button(
                                        onClick = { viewModel.clearDetoxMotivation() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3310B981), contentColor = Color(0xFF10B981))
                                    ) {
                                        Text("Dismiss")
                                    }
                                } else if (motivationLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.CenterHorizontally),
                                        color = Color(0xFF10B981)
                                    )
                                } else {
                                    Text(
                                        "Lacking motivation to do this? Tell the AI what's stopping you for a personalized reality check.",
                                        fontSize = 13.sp,
                                        color = if (isDark) Color(0xB3FFFFFF) else Color(0xFF64748B)
                                    )
                                    OutlinedTextField(
                                        value = userFeeling,
                                        onValueChange = { userFeeling = it },
                                        placeholder = { Text("What's stopping you right now?", fontSize = 13.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = if (isDark) Color.White else Color.Black,
                                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                                        )
                                    )
                                    Button(
                                        onClick = { viewModel.getDetoxMotivation(context, habit.name, userFeeling) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("Motivate Me!")
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    TextButton(
                        onClick = {
                            viewModel.clearDetoxMotivation()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", color = if (isDark) Color.White else Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun NotesGrid(
    notes: List<com.example.data.Note>,
    isDark: Boolean,
    onNoteClick: (com.example.data.Note) -> Unit
) {
    val notePairs = remember(notes) { notes.chunked(2) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        notePairs.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { note ->
                    val df = remember { java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault()) }
                    val dateStr = remember(note.timestamp) { df.format(java.util.Date(note.timestamp)) }
                    val noteColor = Color(note.color)
                    
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onNoteClick(note) },
                        shape = RoundedCornerShape(18.dp),
                        color = noteColor,
                        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)),
                        shadowElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            if (note.title.isNotBlank()) {
                                Text(
                                    text = note.title,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    fontSize = 14.5.sp,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Text(
                                text = parseMarkdown(note.content),
                                color = Color(0xFF1E293B),
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp,
                                maxLines = 6,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.08f)
                                ) {
                                    Text(
                                        text = dateStr,
                                        color = Color(0xFF334155),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color(0xFF334155).copy(alpha = 0.6f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun NoteEditDialog(
    note: com.example.data.Note?,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, color: Long) -> Unit,
    onDelete: ((Int) -> Unit)? = null
) {
    var noteTitle by remember { mutableStateOf(note?.title ?: "") }
    var noteContentValue by remember { 
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(note?.content ?: "")) 
    }
    
    val colors = listOf<Long>(
        0xFFFFD54FL, // Warm Amber / Yellow
        0xFF81C784L, // Mint Green
        0xFF64B5F6L, // Soft Sky Blue
        0xFFE57373L, // Coral Rose
        0xFFBA68C8L, // Lavender Purple
        0xFFFFB74DL, // Sunset Peach
        0xFFF06292L, // Candy Pink
        0xFF4DD0E1L  // Aqua Teal
    )
    var selectedColor by remember { mutableStateOf(note?.color ?: colors[0]) }

    fun applyFormat(prefix: String, suffix: String = "") {
        val text = noteContentValue.text
        val selection = noteContentValue.selection
        val selectedText = if (selection.start != selection.end) {
            text.substring(selection.start.coerceAtLeast(0), selection.end.coerceAtMost(text.length))
        } else ""
        
        val insertText = if (selectedText.isNotEmpty()) selectedText else "text"
        val newText = text.substring(0, selection.start.coerceAtLeast(0)) +
                prefix + insertText + suffix +
                text.substring(selection.end.coerceAtMost(text.length))
                
        val newStart = selection.start.coerceAtLeast(0) + prefix.length
        val newEnd = newStart + insertText.length
        
        noteContentValue = androidx.compose.ui.text.input.TextFieldValue(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newStart, newEnd)
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF6366F1).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (note == null) Icons.Default.Add else Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (note == null) "New Note" else "Edit Note",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0x80FFFFFF) else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    placeholder = { 
                        Text(
                            "Title (e.g. Physics Formula, Quick Concept)",
                            fontSize = 13.5.sp,
                            color = if (isDark) Color(0x66FFFFFF) else Color(0xFF94A3B8)
                        ) 
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = if (isDark) Color(0x26FFFFFF) else Color(0xFFE2E8F0),
                        focusedContainerColor = if (isDark) Color(0x1A0F172A) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0x1A0F172A) else Color(0xFFF8FAFC)
                    )
                )

                // Rich Text Toolbar
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0x26FFFFFF) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bold
                        IconButton(
                            onClick = { applyFormat("**", "**") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                "B",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else Color(0xFF1E293B)
                            )
                        }
                        // Italic
                        IconButton(
                            onClick = { applyFormat("*", "*") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                "I",
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else Color(0xFF1E293B)
                            )
                        }
                        // Bullet List
                        IconButton(
                            onClick = { applyFormat("- ") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Bullet List",
                                tint = if (isDark) Color.White else Color(0xFF1E293B),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        // Numbered List
                        IconButton(
                            onClick = { applyFormat("1. ") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                "1.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (isDark) Color.White else Color(0xFF1E293B)
                            )
                        }
                        // Quote
                        IconButton(
                            onClick = { applyFormat("> ") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                "❝",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else Color(0xFF1E293B)
                            )
                        }
                        // Code / Formula
                        IconButton(
                            onClick = { applyFormat("`", "`") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                "< >",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isDark) Color.White else Color(0xFF1E293B)
                            )
                        }
                    }
                }

                // Note Content Input
                OutlinedTextField(
                    value = noteContentValue,
                    onValueChange = { noteContentValue = it },
                    placeholder = { 
                        Text(
                            "Write your thoughts or study notes here... (Supports Markdown)",
                            fontSize = 13.5.sp,
                            color = if (isDark) Color(0x66FFFFFF) else Color(0xFF94A3B8)
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 130.dp, max = 200.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = if (isDark) Color(0x26FFFFFF) else Color(0xFFE2E8F0),
                        focusedContainerColor = if (isDark) Color(0x1A0F172A) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0x1A0F172A) else Color(0xFFF8FAFC)
                    )
                )

                // Color Selection Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "COLOR THEME",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(colors) { colorValue ->
                            val isColorSelected = selectedColor == colorValue
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorValue))
                                    .border(
                                        width = if (isColorSelected) 3.dp else 1.dp,
                                        color = if (isColorSelected) Color(0xFF0F172A) else Color.Black.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorValue },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isColorSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note != null && onDelete != null) {
                        IconButton(
                            onClick = { onDelete(note.id) },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Note",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = if (isDark) Color(0xCCFFFFFF) else Color(0xFF475569))
                    }

                    Button(
                        onClick = {
                            if (noteContentValue.text.isNotBlank() || noteTitle.isNotBlank()) {
                                onSave(noteTitle, noteContentValue.text, selectedColor)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (note == null) "Create Note" else "Save Note",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}


fun parseMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    val formattedText = com.example.util.MathFormatter.formatAll(text)
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    val lines = formattedText.lines()
    
    lines.forEachIndexed { index, line ->
        var currentLine = line
        var isQuote = false
        
        if (currentLine.startsWith("- ") || currentLine.startsWith("• ")) {
            currentLine = "• " + currentLine.substring(2)
        } else if (currentLine.startsWith("> ")) {
            isQuote = true
            currentLine = "❝ " + currentLine.substring(2)
        }

        val pattern = java.util.regex.Pattern.compile("\\*\\*(.*?)\\*\\*|\\*(.*?)\\*|`(.*?)`")
        val matcher = pattern.matcher(currentLine)
        var lastIdx = 0

        while (matcher.find()) {
            if (matcher.start() > lastIdx) {
                val sub = currentLine.substring(lastIdx, matcher.start())
                if (isQuote) {
                    builder.withStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color(0xFF475569))) {
                        append(sub)
                    }
                } else {
                    builder.append(sub)
                }
            }
            if (matcher.group(1) != null) {
                builder.withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))) {
                    append(matcher.group(1)!!)
                }
            } else if (matcher.group(2) != null) {
                builder.withStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                    append(matcher.group(2)!!)
                }
            } else if (matcher.group(3) != null) {
                builder.withStyle(androidx.compose.ui.text.SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, background = Color.Black.copy(alpha = 0.06f))) {
                    append(" " + matcher.group(3)!! + " ")
                }
            }
            lastIdx = matcher.end()
        }
        if (lastIdx < currentLine.length) {
            val rem = currentLine.substring(lastIdx)
            if (isQuote) {
                builder.withStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color(0xFF475569))) {
                    append(rem)
                }
            } else {
                builder.append(rem)
            }
        }
        if (index < lines.size - 1) {
            builder.append("\n")
        }
    }
    return builder.toAnnotatedString()
}
@Composable
fun PrivacyLockScreen(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    habitPassword: String?,
    securityQuestion: String,
    securityAnswer: String?,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotAnswerInput by remember { mutableStateOf("") }
    var forgotAnswerError by remember { mutableStateOf(false) }

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    // Intercept back presses when forgot dialog is active
    BackHandler(enabled = showForgotDialog) {
        showForgotDialog = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.85f) else Color(0xFFFFFFFF),
            border = BorderStroke(1.5.dp, if (isDark) Color(0xFF6366F1).copy(alpha = 0.5f) else Color(0xFFC7D2FE)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Habits & Detox Locked 🔒",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter your privacy password to view habits and notes",
                        fontSize = 12.5.sp,
                        color = subTextColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        passwordError = false
                    },
                    label = { Text("Enter Password / PIN", color = subTextColor, fontSize = 12.sp) },
                    placeholder = { Text("Password", fontSize = 12.sp) },
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
                    isError = passwordError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )

                if (passwordError) {
                    Text(
                        "Incorrect password. Please try again.",
                        color = Color(0xFFEF4444),
                        fontSize = 11.5.sp
                    )
                }

                Button(
                    onClick = {
                        if (passwordInput.trim() == habitPassword?.trim()) {
                            onUnlocked()
                        } else {
                            passwordError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock Habits 🔓", fontWeight = FontWeight.Bold, color = Color.White)
                }

                TextButton(
                    onClick = {
                        forgotAnswerInput = ""
                        forgotAnswerError = false
                        showForgotDialog = true
                    }
                ) {
                    Text(
                        "Forgot Password? (Security Question) 🔑",
                        color = Color(0xFF6366F1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showForgotDialog) {
            AlertDialog(
                onDismissRequest = { showForgotDialog = false },
                title = {
                    Text("🔑 Password Recovery", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Answer your security question to unlock Habits & Detox.",
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
                                true
                            } else {
                                forgotAnswerInput.trim().equals(expectedAnswer, ignoreCase = true)
                            }

                            if (isCorrect) {
                                Toast.makeText(context, "✅ Security Question verified! Unlocked.", Toast.LENGTH_SHORT).show()
                                showForgotDialog = false
                                onUnlocked()
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
                containerColor = if (isDark) Color(0xFF1E293B) else Color.White
            )
        }
    }
}
