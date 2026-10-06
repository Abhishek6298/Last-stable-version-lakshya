package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LakshyaNotebookScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    var uploadedUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedFileName by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var notebookReady by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadedUri = uri
            uploadedFileName = "Document.pdf" // Visual default
            isProcessing = true
            coroutineScope.launch {
                delay(2500) // Simulate AI parsing
                isProcessing = false
                notebookReady = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.clip(CircleShape).background(if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lakshya Notebook", fontSize = 20.sp, fontWeight = FontWeight.Black, color = textColor, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    Text("Powered by Abhi Magic 🪄", fontSize = 12.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }

            if (!notebookReady && !isProcessing) {
                // Upload State
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(100.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6)))).clickable { filePicker.launch("*/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Upload Source Document", fontSize = 22.sp, fontWeight = FontWeight.Black, color = textColor, maxLines = 1, softWrap = false)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Upload a PDF, notes, or image. Lakshya Notebook will instantly generate Mind Maps, Slide Decks, and a Podcast overview.", fontSize = 14.sp, color = subTextColor, textAlign = TextAlign.Center)
                }
            } else if (isProcessing) {
                // Processing State
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF8B5CF6), modifier = Modifier.size(64.dp), strokeWidth = 4.dp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Abhi Magic 🪄 is reading your document...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, softWrap = false)
                    Text("Generating Mind Maps & Slide Decks...", fontSize = 13.sp, color = subTextColor, maxLines = 1, softWrap = false)
                }
            } else {
                // Notebook Ready State
                NotebookDashboard(
                    isDark = isDark,
                    fileName = uploadedFileName ?: "Source Document",
                    onClear = {
                        notebookReady = false
                        uploadedUri = null
                    }
                )
            }
        }
    }
}

@Composable
fun NotebookDashboard(isDark: Boolean, fileName: String, onClear: () -> Unit) {
    var selectedTab by remember { mutableStateOf("Chat") }
    val tabs = listOf("Chat", "Study Guide", "Mind Map", "Flashcards", "Slides", "Podcast")

    Column(modifier = Modifier.fillMaxSize()) {
        // Source Chip
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0x3310B981) else Color(0xFFD1FAE5),
                modifier = Modifier.weight(1f)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Source Active: $fileName", color = if (isDark) Color(0xFF34D399) else Color(0xFF065F46), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false, modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onClear, modifier = Modifier.clip(CircleShape).background(if (isDark) Color(0x33EF4444) else Color(0xFFFFE4E6))) {
                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFFEF4444))
            }
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(selectedTab),
            containerColor = Color.Transparent,
            contentColor = Color(0xFF8B5CF6),
            edgePadding = 16.dp,
            divider = {}
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    text = { Text(tab, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium, fontSize = 14.sp, color = if (isSelected) Color(0xFF8B5CF6) else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B), maxLines = 1, softWrap = false) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
            Crossfade(targetState = selectedTab, label = "tab_crossfade") { tab ->
                when (tab) {
                    "Chat" -> NotebookChatTab(isDark = isDark)
                    "Study Guide" -> NotebookStudyGuideTab(isDark = isDark)
                    "Mind Map" -> NotebookMindMapTab(isDark = isDark)
                    "Flashcards" -> NotebookFlashcardsTab(isDark = isDark)
                    "Slides" -> NotebookSlidesTab(isDark = isDark)
                    "Podcast" -> NotebookPodcastTab(isDark = isDark)
                }
            }
        }
    }
}

@Composable
fun NotebookChatTab(isDark: Boolean) {
    val bgColor = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    var text by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf("Hello! I've read your document. Ask me anything about it, or try a suggested question.")) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Suggested Chips
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Summarize the core concept", "What are the key formulas?", "Create 5 MCQs from this").forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9),
                    modifier = Modifier.clickable { messages = messages + suggestion + "Here is a simulated answer based on your document using Abhi Magic 🪄." }
                ) {
                    Text(suggestion, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569), maxLines = 1, softWrap = false)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chat History
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(messages.size) { index ->
                val isUser = index % 2 != 0
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isUser) Color(0xFF8B5CF6) else if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = messages[index],
                            color = if (isUser) Color.White else textColor,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Input
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Ask about this document...", maxLines = 1, softWrap = false) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = bgColor,
                unfocusedContainerColor = bgColor,
                focusedBorderColor = Color(0xFF8B5CF6),
                unfocusedBorderColor = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
            ),
            trailingIcon = {
                IconButton(onClick = { 
                    if (text.isNotBlank()) {
                        messages = messages + text + "This is a simulated AI response extracted directly from your document using Abhi Magic 🪄."
                        text = ""
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFF8B5CF6))
                }
            }
        )
    }
}

@Composable
fun NotebookStudyGuideTab(isDark: Boolean) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text("Auto-Generated Study Guide", fontSize = 18.sp, fontWeight = FontWeight.Black, color = textColor, maxLines = 1, softWrap = false)
        }
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = if (isDark) Color(0xFF1E293B) else Color.White, shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Brief Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6), maxLines = 1, softWrap = false)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("The document outlines fundamental principles of physics, focusing on mechanics and conservation laws. It details step-by-step derivations for kinetic energy and momentum.", fontSize = 14.sp, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = if (isDark) Color(0xFF1E293B) else Color.White, shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Key Terminology (Glossary)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), maxLines = 1, softWrap = false)
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf("Inertia" to "Resistance to change in motion.", "Momentum" to "Product of mass and velocity.", "Kinematics" to "Study of motion without forces.").forEach { (term, def) ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("$term: ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                            Text(def, fontSize = 14.sp, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotebookMindMapTab(isDark: Boolean) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("AI Concept Mind Map", fontSize = 18.sp, fontWeight = FontWeight.Black, color = textColor, maxLines = 1, softWrap = false)
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (isDark) Color(0xFF1E293B) else Color.White),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                val topNode = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 4)
                val leftNode = androidx.compose.ui.geometry.Offset(size.width / 4, size.height * 0.75f)
                val rightNode = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.75f)

                drawLine(color = Color(0xFF8B5CF6), start = center, end = topNode, strokeWidth = 4f)
                drawLine(color = Color(0xFF8B5CF6), start = center, end = leftNode, strokeWidth = 4f)
                drawLine(color = Color(0xFF8B5CF6), start = center, end = rightNode, strokeWidth = 4f)

                drawCircle(color = Color(0xFF8B5CF6), radius = 100f, center = center)
                drawCircle(color = Color(0xFF3B82F6), radius = 70f, center = topNode)
                drawCircle(color = Color(0xFF10B981), radius = 70f, center = leftNode)
                drawCircle(color = Color(0xFFF59E0B), radius = 70f, center = rightNode)
            }
            Text("Core\nConcept", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun NotebookFlashcardsTab(isDark: Boolean) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    var isFlipped by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Interactive Flashcards", fontSize = 18.sp, fontWeight = FontWeight.Black, color = textColor, maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 16.dp))
        
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isFlipped) Color(0xFF8B5CF6) else if (isDark) Color(0xFF1E293B) else Color.White,
            modifier = Modifier.weight(1f).fillMaxWidth().clickable { isFlipped = !isFlipped },
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
                if (!isFlipped) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Question", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, softWrap = false)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("What is the primary function of the core concept discussed?", fontSize = 20.sp, fontWeight = FontWeight.Black, color = textColor, textAlign = TextAlign.Center)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Answer", color = Color(0xFFD8B4FE), fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, softWrap = false)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("It serves as the fundamental basis for all derived calculations in the document.", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { isFlipped = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))) { Text("Prev") }
            Text("Tap card to flip", color = textColor, modifier = Modifier.align(Alignment.CenterVertically))
            Button(onClick = { isFlipped = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))) { Text("Next") }
        }
    }
}

@Composable
fun NotebookSlidesTab(isDark: Boolean) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    Column(modifier = Modifier.fillMaxSize()) {
        Text("AI Slide Deck", fontSize = 18.sp, fontWeight = FontWeight.Black, color = textColor, maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 16.dp))
        
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text("1. Introduction to the Topic", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B5CF6), maxLines = 2)
                Spacer(modifier = Modifier.height(24.dp))
                listOf("• First major takeaway derived from the text.", "• Important formula or definition highlighted.", "• Abhi Magic 🪄 ensures accurate citations.").forEach {
                    Text(it, fontSize = 16.sp, color = textColor, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))) { Text("Prev") }
            Text("Slide 1 of 5", color = textColor, modifier = Modifier.align(Alignment.CenterVertically))
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))) { Text("Next") }
        }
    }
}

@Composable
fun NotebookPodcastTab(isDark: Boolean) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    var isPlaying by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(200.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6)))), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(100.dp))
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Audio Overview", fontSize = 24.sp, fontWeight = FontWeight.Black, color = textColor, maxLines = 1, softWrap = false)
        Text("2 AI hosts discussing your document.", fontSize = 14.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B), maxLines = 1, softWrap = false)
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {}) { Icon(Icons.Default.Replay10, contentDescription = "Rewind", tint = textColor, modifier = Modifier.size(36.dp)) }
            FloatingActionButton(onClick = { isPlaying = !isPlaying }, containerColor = Color(0xFF8B5CF6), modifier = Modifier.size(72.dp), shape = CircleShape) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = {}) { Icon(Icons.Default.Forward10, contentDescription = "Forward", tint = textColor, modifier = Modifier.size(36.dp)) }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        if (isPlaying) {
            Text("Host 1: \"So, looking at this document, the main idea is...\"", color = Color(0xFF10B981), fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}
