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
import com.example.data.GeminiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiModelPickerDialog(
    isDark: Boolean,
    currentSelectedModelId: String,
    models: List<GeminiModel>,
    isLoading: Boolean,
    onSelectModel: (String) -> Unit,
    onRefreshLiveModels: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf(0) } // 0: All, 1: Lite / Fast, 2: Flagship, 3: Reasoning
    var customModelInput by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val filterTabs = listOf("🌟 All", "⚡ Lite / High Speed", "🚀 Flagship (3.8/3.5)", "🧠 Reasoning & Pro")

    val filteredModels = remember(models, searchQuery, selectedFilterTab) {
        models.filter { model ->
            val idL = model.id.lowercase()
            val descL = model.description.lowercase()

            val matchesTab = when (selectedFilterTab) {
                1 -> model.category.equals("Lite", ignoreCase = true) || idL.contains("lite") || idL.contains("8b") || idL.contains("3.1-flash-lite")
                2 -> model.category.equals("Flagship", ignoreCase = true) || idL.contains("3.8") || idL.contains("3.5") || idL.contains("3.7") || idL.contains("flash")
                3 -> model.category.equals("Reasoning", ignoreCase = true) || idL.contains("pro") || descL.contains("reasoning")
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
    val geminiBrandColor = Color(0xFF38BDF8)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, geminiBrandColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✦", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Column {
                            Text(
                                text = "Google Gemini Model Hub",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor
                            )
                            Text(
                                text = "${models.size} Official Gemini Models • Full Multimodal",
                                fontSize = 11.sp,
                                color = subTextColor
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onRefreshLiveModels,
                            enabled = !isLoading,
                            modifier = Modifier.size(34.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = geminiBrandColor
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Models",
                                    tint = geminiBrandColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = subTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search 3.8 Flash, 3.5 Flash, 3.1 Pro, Flash Lite...",
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = geminiBrandColor,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = subTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedBorderColor = geminiBrandColor,
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterTabs.size) { index ->
                        val isSelected = selectedFilterTab == index
                        val tabBg = if (isSelected) geminiBrandColor else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                        val tabText = if (isSelected) Color.White else subTextColor

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = tabBg,
                            modifier = Modifier
                                .clickable { selectedFilterTab = index }
                        ) {
                            Text(
                                text = filterTabs[index],
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = tabText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Model List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (filteredModels.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("✦", fontSize = 32.sp, color = geminiBrandColor)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "No Gemini models match '$searchQuery'",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = subTextColor
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredModels, key = { it.id }) { model ->
                            val isCurrentSelected = model.id.equals(currentSelectedModelId, ignoreCase = true)
                            val itemBorder = if (isCurrentSelected) {
                                BorderStroke(1.5.dp, geminiBrandColor)
                            } else {
                                BorderStroke(1.dp, borderColor)
                            }
                            val itemBg = if (isCurrentSelected) {
                                geminiBrandColor.copy(alpha = if (isDark) 0.12f else 0.08f)
                            } else {
                                cardBg
                            }

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = itemBg),
                                border = itemBorder,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectModel(model.id) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
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
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )

                                            if (isCurrentSelected) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = geminiBrandColor,
                                                    modifier = Modifier.padding(start = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "ACTIVE ✦",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(model.id))
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Model ID",
                                                tint = subTextColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Text(
                                        text = model.id,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = geminiBrandColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (model.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = model.description,
                                            fontSize = 11.sp,
                                            color = subTextColor,
                                            lineHeight = 15.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Tags Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                            ) {
                                                Text(
                                                    text = "${model.contextLength / 1024}k context",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = subTextColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            if (model.recommendedFor.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = geminiBrandColor.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = model.recommendedFor,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = geminiBrandColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = if (isCurrentSelected) "Selected" else "Tap to Select ✦",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentSelected) geminiBrandColor else subTextColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Model ID Collapsible Section
                AnimatedVisibility(visible = showCustomInput) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        OutlinedTextField(
                            value = customModelInput,
                            onValueChange = { customModelInput = it },
                            placeholder = { Text("e.g. gemini-3.8-flash, gemini-3.5-flash, gemini-3.1-pro-preview...", fontSize = 12.sp) },
                            singleLine = true,
                            trailingIcon = {
                                Button(
                                    onClick = {
                                        if (customModelInput.isNotBlank()) {
                                            onSelectModel(customModelInput.trim())
                                        }
                                    },
                                    enabled = customModelInput.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = geminiBrandColor),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Bottom Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showCustomInput = !showCustomInput },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (showCustomInput) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = "Custom Model",
                            tint = geminiBrandColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showCustomInput) "Hide Custom ID" else "Enter Custom Model ID",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = geminiBrandColor
                        )
                    }

                    Text(
                        text = "${filteredModels.size} of ${models.size} models",
                        fontSize = 10.sp,
                        color = subTextColor
                    )
                }
            }
        }
    }
}
