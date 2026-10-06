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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CloudflareModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudflareModelPickerDialog(
    isDark: Boolean,
    currentSelectedModelId: String,
    models: List<CloudflareModel>,
    isLoading: Boolean,
    onSelectModel: (String) -> Unit,
    onRefreshLiveModels: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: All, 1: Reasoning, 2: Flagship 70B, 3: Vision/OCR, 4: Fast
    var customModelInput by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val filterTabs = listOf("🌟 All Models", "🧠 Deep Reasoning (R1)", "⚡ Flagship (70B+)", "🖼️ Vision & OCR", "🚀 Fast (8B/9B)")

    val filteredModels = remember(models, searchQuery, selectedFilterTab) {
        models.filter { model ->
            val idL = model.id.lowercase()
            val descL = model.description.lowercase()

            val matchesTab = when (selectedFilterTab) {
                1 -> idL.contains("r1") || idL.contains("reason") || descL.contains("reasoning")
                2 -> idL.contains("70b") || idL.contains("72b") || idL.contains("instruct") && !idL.contains("8b")
                3 -> model.isVision || idL.contains("vision") || idL.contains("image")
                4 -> idL.contains("8b") || idL.contains("9b") || idL.contains("7b")
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                idL.contains(q) || model.name.lowercase().contains(q) || descL.contains(q)
            }

            matchesTab && matchesSearch
        }
    }

    val dialogBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val cfOrange = Color(0xFFF6821F)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, cfOrange.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            color = dialogBg,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(cfOrange, Color(0xFFFA8C16)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("☁️", fontSize = 20.sp)
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Cloudflare Workers AI",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(cfOrange.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("10k Daily Free", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = cfOrange)
                                }
                            }
                            Text(
                                "Live Edge Models Sync • DeepSeek R1 • Llama 3.3 • Qwen",
                                fontSize = 11.sp,
                                color = subTextColor
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefreshLiveModels,
                            enabled = !isLoading,
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = cfOrange)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Live Sync", tint = cfOrange, modifier = Modifier.size(20.dp))
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = subTextColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search models by name or id (e.g. DeepSeek, Llama, Qwen)...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = subTextColor, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = subTextColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cfOrange,
                        unfocusedBorderColor = borderColor,
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterTabs.indices.toList()) { index ->
                        val isSelected = selectedFilterTab == index
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilterTab = index },
                            label = {
                                Text(
                                    filterTabs[index],
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cfOrange,
                                selectedLabelColor = Color.White,
                                containerColor = cardBg,
                                labelColor = textColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) cfOrange else borderColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Model Counts & Custom Model Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${filteredModels.size} models available",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = subTextColor
                    )

                    TextButton(
                        onClick = { showCustomInput = !showCustomInput },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            if (showCustomInput) Icons.Default.ExpandLess else Icons.Default.Add,
                            contentDescription = null,
                            tint = cfOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (showCustomInput) "Hide Custom" else "Enter Custom Model ID",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cfOrange
                        )
                    }
                }

                // Custom Model Entry Accordion
                AnimatedVisibility(visible = showCustomInput) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .border(1.dp, cfOrange.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            "Enter any exact Cloudflare Model Name (e.g. @cf/meta/llama-3.3-70b-instruct-fp8-fast):",
                            fontSize = 10.5.sp,
                            color = subTextColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customModelInput,
                                onValueChange = { customModelInput = it },
                                placeholder = { Text("@cf/...", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
                            )
                            Button(
                                onClick = {
                                    val trimmed = customModelInput.trim()
                                    if (trimmed.isNotBlank()) {
                                        onSelectModel(trimmed)
                                        onDismiss()
                                    }
                                },
                                enabled = customModelInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = cfOrange),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Apply", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Models List
                if (filteredModels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🔍", fontSize = 32.sp)
                            Text("No models found matching '$searchQuery'", fontSize = 13.sp, color = subTextColor)
                            Button(
                                onClick = onRefreshLiveModels,
                                colors = ButtonDefaults.buttonColors(containerColor = cfOrange),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Live from Cloudflare", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(filteredModels, key = { it.id }) { model ->
                            val isSelected = model.id.equals(currentSelectedModelId, ignoreCase = true)
                            CloudflareModelCard(
                                model = model,
                                isSelected = isSelected,
                                isDark = isDark,
                                cardBg = cardBg,
                                textColor = textColor,
                                subTextColor = subTextColor,
                                borderColor = borderColor,
                                cfOrange = cfOrange,
                                onSelect = {
                                    onSelectModel(model.id)
                                    onDismiss()
                                },
                                onCopyId = {
                                    clipboardManager.setText(AnnotatedString(model.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudflareModelCard(
    model: CloudflareModel,
    isSelected: Boolean,
    isDark: Boolean,
    cardBg: Color,
    textColor: Color,
    subTextColor: Color,
    borderColor: Color,
    cfOrange: Color,
    onSelect: () -> Unit,
    onCopyId: () -> Unit
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) cfOrange.copy(alpha = if (isDark) 0.18f else 0.1f) else cardBg,
        border = BorderStroke(
            if (isSelected) 1.8.dp else 1.dp,
            if (isSelected) cfOrange else borderColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Model Name & Active Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = model.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) cfOrange else textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (model.isBeta) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.18f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("BETA", fontSize = 7.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B5CF6))
                        }
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(cfOrange)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text("ACTIVE ✦", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Model ID Row with Copy button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = model.id,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = subTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onCopyId,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy ID",
                        tint = subTextColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = model.description,
                fontSize = 11.sp,
                color = subTextColor,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Badges: Task, Vision / Reasoning Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Task Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            model.task,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subTextColor
                        )
                    }

                    // Vision Badge if applicable
                    if (model.isVision) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0D9488).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "🖼️ Vision OCR",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D9488)
                            )
                        }
                    }
                }

                // Recommended For tag
                Text(
                    text = model.recommendedFor,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = cfOrange
                )
            }
        }
    }
}
