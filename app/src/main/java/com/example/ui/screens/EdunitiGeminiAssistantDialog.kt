@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GeminiChatAssistant
import com.example.ui.components.MathJaxView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdunitiGeminiAssistantDialog(
    contextType: String,
    onDismiss: () -> Unit,
    isDark: Boolean,
    initialTopicName: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf(initialTopicName?.let { "Explain high yield focus for: $it" } ?: "") }
    var response by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf(if (contextType.contains("Bio", ignoreCase = true)) "Biology" else if (contextType.contains("Chem", ignoreCase = true)) "Chemistry" else "Physics") }

    val dialogBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B)

    val quickActionPrompts = listOf(
        "🎯 10-Day Revision Plan" to "Create a rigorous, day-by-day 10-day micro revision roadmap for $selectedSubject focusing on high-weightage NEET topics, question practice targets, and revision cycles.",
        "⚡ Formulas & Traps" to "Provide the most important high-yield formulas, unit conversions, and conceptual exception traps in $selectedSubject for NEET UG. Include MathJax LaTeX syntax for all equations.",
        "📊 5-Year PYQ Trends" to "Analyze the 5-Year NEET PYQ patterns (2021-2025) for $selectedSubject. Which subtopics were asked every single year, and what are the projected 2026 focal areas?",
        "⚠️ Common Mistake Traps" to "List the Top 7 silliest traps and conceptual blunders students make in $selectedSubject in NEET exams, with exact explanations on how to avoid them.",
        "📝 5 Tricky MCQs" to "Generate 5 tricky, assertion-reason and multi-statement NEET level MCQs for $selectedSubject with complete step-by-step solutions and NCERT references.",
        "🧠 Active Recall Mnemonics" to "Give me memorable active-recall mnemonics, flowchart memory hacks, and fast visual triggers for difficult concepts in $selectedSubject."
    )

    fun sendGeminiQuery(queryText: String) {
        if (queryText.isBlank() || isLoading) return
        isLoading = true
        response = null
        val fullPrompt = """
            You are the elite Lakshya NEET AI Academic Master & Revision Coach.
            Subject: $selectedSubject
            Section Context: Lakshya $contextType Checklist & Performance Targets
            
            Student Prompt:
            "$queryText"
            
            Instructions:
            1. Deliver actionable, authoritative, and concise NEET UG guidance based on top ranker strategies.
            2. For all mathematical formulas, fractions, physics laws, chemical reactions, and numbers, strictly format using LaTeX MathJax syntax enclosed in single dollar signs for inline or double dollar signs for block equations.
            3. Highlight high-yield points with emojis, bullet points, and clean bold headings.
        """.trimIndent()

        coroutineScope.launch {
            val res = GeminiChatAssistant.executeTestingPrompt(context, fullPrompt)
            res.onSuccess { text ->
                response = text
                isLoading = false
            }.onFailure { err ->
                response = "⚠️ Error fetching AI response: ${err.localizedMessage ?: "Unknown error"}\n\nPlease verify your Lakshya AI Key in Settings or try again."
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp)
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.95f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(dialogBg)
                    .border(
                        BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)))),
                        RoundedCornerShape(26.dp)
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1), Color(0xFF3B82F6))))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column {
                            Text(
                                text = "Lakshya AI Super Assistant",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Powered by Lakshya AI Multi-Model Intelligence",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                // Subject Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF131B2E) else Color(0xFFEEF2FF))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subject Context:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = subTextColor
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Physics", "Chemistry", "Biology").forEach { subj ->
                            val isSelected = selectedSubject == subj
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF6366F1) else Color.Transparent,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF4F46E5) else Color(0x336366F1)),
                                modifier = Modifier.clickable { selectedSubject = subj }
                            ) {
                                Text(
                                    text = subj,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else textColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Quick Prompt Chips
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickActionPrompts.forEach { (label, query) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0x338B5CF6) else Color(0x1F8B5CF6),
                            border = BorderStroke(1.dp, Color(0x558B5CF6)),
                            modifier = Modifier.clickable(enabled = !isLoading) {
                                prompt = query
                                sendGeminiQuery(query)
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0))

                // Chat / Response Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (response == null && !isLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🧠", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Lakshya Checklist AI Mentor",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap any quick prompt above or type your specific question below to get instant micro-revision plans, 5-year PYQ insights, and NCERT exception analysis.",
                                fontSize = 12.5.sp,
                                color = subTextColor,
                                modifier = Modifier.padding(horizontal = 24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF8B5CF6),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Lakshya AI is analyzing NEET patterns & formulating advice...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = subTextColor
                            )
                        }
                    } else if (response != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💡 AI Academic Guidance:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Lakshya AI", response)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Copied response to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy", fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(cardBg)
                                    .border(1.dp, if (isDark) Color(0x336366F1) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                MathJaxView(
                                    text = response ?: "",
                                    isDark = isDark,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Input Area
                Surface(
                    color = if (isDark) Color(0xFF131B2E) else Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask doubt, revision plan, or formula...", fontSize = 13.sp, color = subTextColor) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = cardBg,
                                unfocusedContainerColor = cardBg,
                                focusedBorderColor = Color(0xFF8B5CF6),
                                unfocusedBorderColor = if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 3
                        )

                        IconButton(
                            onClick = { sendGeminiQuery(prompt) },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)))),
                            enabled = !isLoading && prompt.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
}

