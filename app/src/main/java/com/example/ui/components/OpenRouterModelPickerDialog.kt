package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.OpenRouterModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenRouterModelPickerDialog(
    isDark: Boolean,
    currentSelectedModelId: String,
    models: List<OpenRouterModel>,
    isLoading: Boolean,
    onSelectModel: (String) -> Unit,
    onRefreshLiveModels: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf(0) } // 0: All, 1: Free Only, 2: Vision/OCR, 3: Reasoning
    var customModelInput by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    val filterTabs = listOf("🌟 All", "✨ Free (100%)", "📷 Vision & OCR", "🧠 Reasoning")

    val filteredModels = remember(models, searchQuery, selectedFilterTab) {
        models.filter { model ->
            // Tab filter
            val matchesTab = when (selectedFilterTab) {
                1 -> model.isFree
                2 -> model.isVision
                3 -> {
                    val idL = model.id.lowercase()
                    val descL = model.description.lowercase()
                    idL.contains("r1") || idL.contains("reason") || idL.contains("o1") || idL.contains("o3") || idL.contains("thinking") || descL.contains("reasoning")
                }
                else -> true
            }

            // Search filter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                model.id.lowercase().contains(q) ||
                        model.name.lowercase().contains(q) ||
                        model.topProvider.lowercase().contains(q) ||
                        model.description.lowercase().contains(q)
            }

            matchesTab && matchesSearch
        }
    }

    val dialogBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, if (isDark) Color(0xFF6366F1).copy(alpha = 0.5f) else Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            color = dialogBg,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🌐", fontSize = 22.sp)
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "OpenRouter Models",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${models.size} LIVE",
                                        color = Color(0xFF10B981),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = "Select latest 2025/2026 model for CBT Tests & OCR",
                                fontSize = 11.5.sp,
                                color = subTextColor
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onRefreshLiveModels,
                            enabled = !isLoading,
                            modifier = Modifier.size(38.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color(0xFF6366F1),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Models",
                                    tint = Color(0xFF6366F1)
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = subTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by name (e.g. gemini, deepseek, qwen, claude, free)...", fontSize = 12.5.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = subTextColor) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = subTextColor)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = borderColor,
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Tabs (All, Free Only, Vision/OCR, Reasoning)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterTabs.indices.toList()) { index ->
                        val isSelected = selectedFilterTab == index
                        val bg = if (isSelected) Color(0xFF6366F1) else cardBg
                        val textC = if (isSelected) Color.White else textColor
                        val borderC = if (isSelected) Color(0xFF6366F1) else borderColor

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .border(1.dp, borderC, RoundedCornerShape(10.dp))
                                .clickable { selectedFilterTab = index }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filterTabs[index],
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textC
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Model Input Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing ${filteredModels.size} models",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = subTextColor
                    )
                    TextButton(
                        onClick = { showCustomInput = !showCustomInput },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (showCustomInput) "Hide Custom ID" else "+ Enter Custom Model ID",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                    }
                }

                AnimatedVisibility(visible = showCustomInput) {
                    Column(modifier = Modifier.padding(bottom = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customModelInput,
                                onValueChange = { customModelInput = it },
                                placeholder = { Text("e.g. meta-llama/llama-3.3-70b-instruct", fontSize = 11.5.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = borderColor,
                                    focusedContainerColor = cardBg,
                                    unfocusedContainerColor = cardBg
                                )
                            )
                            Button(
                                onClick = {
                                    val trimmed = customModelInput.trim()
                                    if (trimmed.isNotBlank()) {
                                        val normalized = when {
                                            trimmed.contains("/") -> trimmed
                                            trimmed.contains("pro", ignoreCase = true) -> "google/gemini-2.5-pro"
                                            trimmed.contains("flash", ignoreCase = true) || trimmed.contains("gemini", ignoreCase = true) -> "google/gemini-2.5-flash"
                                            else -> trimmed
                                        }
                                        onSelectModel(normalized)
                                        onDismiss()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("Use", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Models List
                if (filteredModels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No models match your query.",
                                fontSize = 13.sp,
                                color = subTextColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedFilterTab = 0
                                    onRefreshLiveModels()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reset Filters / Refresh Live")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredModels, key = { it.id }) { model ->
                            val isSelected = model.id == currentSelectedModelId

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.15f) else cardBg,
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) Color(0xFF6366F1) else borderColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectModel(model.id)
                                        onDismiss()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = model.name,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFF6366F1))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("SELECTED", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                                                    }
                                                }
                                            }

                                            Text(
                                                text = model.id,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = subTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Badges
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (model.isFree) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = "FREE",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF10B981)
                                                    )
                                                }
                                            }

                                            if (model.isVision) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = "📷 OCR",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0284C7)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (model.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = model.description,
                                            fontSize = 11.sp,
                                            color = subTextColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Context length & provider meta
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val ctxStr = if (model.contextLength >= 1000000) {
                                                "${model.contextLength / 1000000}M Context"
                                            } else {
                                                "${model.contextLength / 1024}K Context"
                                            }
                                            Text(
                                                text = "⚡ $ctxStr",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = subTextColor
                                            )
                                            Text("•", fontSize = 10.sp, color = subTextColor)
                                            Text(
                                                text = "By ${model.topProvider}",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = subTextColor
                                            )
                                        }

                                        Text(
                                            text = if (model.isFree) "100% Free" else "$${model.promptPricing}/M tokens",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (model.isFree) Color(0xFF10B981) else subTextColor
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
