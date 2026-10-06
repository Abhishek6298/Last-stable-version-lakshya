package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiProvider
import com.example.data.OpenRouterModel

@Composable
fun AiEngineSettingsSection(
    isDark: Boolean,
    textColor: Color,
    subTextColor: Color,
    // Dual Provider
    activeProvider: AiProvider,
    onProviderChange: (AiProvider) -> Unit,
    // Gemini Settings
    isVerifyingGeminiKeys: Boolean,
    onVerifyGeminiKeys: () -> Unit,
    currentlyWorkingGeminiKey: String = "",
    selectedGeminiModelId: String = "gemini-3.8-flash",
    onSelectGeminiModel: (String) -> Unit = {},
    geminiModels: List<com.example.data.GeminiModel> = emptyList(),
    isGeminiLoadingModels: Boolean = false,
    onRefreshGeminiModels: () -> Unit = {},
    onOpenGeminiModelPicker: () -> Unit = {},
    showGeminiQuotaBadge: Boolean = true,
    onToggleShowGeminiQuotaBadge: (Boolean) -> Unit = {},
    geminiKey1: String,
    geminiKey1Status: String,
    onGeminiKey1Change: (String) -> Unit,
    onClearGeminiKey1: () -> Unit,
    geminiKey2: String = "",
    geminiKey2Status: String = "",
    onGeminiKey2Change: (String) -> Unit = {},
    onClearGeminiKey2: () -> Unit = {},
    geminiKey3: String = "",
    geminiKey3Status: String = "",
    onGeminiKey3Change: (String) -> Unit = {},
    onClearGeminiKey3: () -> Unit = {},
    // OpenRouter Settings
    openRouterKey: String,
    openRouterKeyStatus: String,
    isVerifyingOpenRouterKey: Boolean,
    onOpenRouterKeyChange: (String) -> Unit,
    onClearOpenRouterKey: () -> Unit,
    onVerifyOpenRouterKey: () -> Unit,
    selectedOpenRouterModelId: String,
    onSelectOpenRouterModel: (String) -> Unit,
    openRouterModels: List<OpenRouterModel>,
    isOpenRouterLoadingModels: Boolean,
    onRefreshOpenRouterModels: () -> Unit,
    onOpenModelPicker: () -> Unit,
    openRouterMaxTokens: Int = 8192,
    onOpenRouterMaxTokensChange: (Int) -> Unit = {},
    // Groq Settings
    groqKey: String = "",
    groqKeyStatus: String = "",
    isVerifyingGroqKey: Boolean = false,
    onGroqKeyChange: (String) -> Unit = {},
    onClearGroqKey: () -> Unit = {},
    onVerifyGroqKey: () -> Unit = {},
    selectedGroqModelId: String = com.example.data.GroqManager.DEFAULT_TEXT_MODEL,
    onSelectGroqModel: (String) -> Unit = {},
    groqModels: List<com.example.data.GroqModel> = emptyList(),
    isGroqLoadingModels: Boolean = false,
    onRefreshGroqModels: () -> Unit = {},
    onOpenGroqModelPicker: () -> Unit = {},
    // Cloudflare Settings
    cloudflareAccountId: String = "",
    onCloudflareAccountIdChange: (String) -> Unit = {},
    cloudflareApiToken: String = "",
    cloudflareStatus: String = "",
    isVerifyingCloudflare: Boolean = false,
    onCloudflareApiTokenChange: (String) -> Unit = {},
    onClearCloudflareToken: () -> Unit = {},
    onVerifyCloudflare: () -> Unit = {},
    selectedCloudflareModelId: String = com.example.data.CloudflareManager.DEFAULT_TEXT_MODEL,
    onSelectCloudflareModel: (String) -> Unit = {},
    cloudflareModels: List<com.example.data.CloudflareModel> = emptyList(),
    isCloudflareLoadingModels: Boolean = false,
    onRefreshCloudflareModels: () -> Unit = {},
    onOpenCloudflareModelPicker: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isKeyVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Section Title & Dual Engine Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Engines & OCR Models 🤖",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Switch between Gemini, OpenRouter, Groq & Cloudflare",
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Quad-Engine Segmented Selector
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Tab 1: Native Gemini
                val isGemini = activeProvider == AiProvider.NATIVE_GEMINI
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isGemini) {
                                Modifier.background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))))
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable { onProviderChange(AiProvider.NATIVE_GEMINI) }
                        .padding(vertical = 10.dp, horizontal = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("✦ Gemini", fontSize = 10.sp, fontWeight = if (isGemini) FontWeight.Bold else FontWeight.Medium, color = if (isGemini) Color.White else textColor)
                        if (isGemini) {
                            Text("ACTIVE", fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }

                // Tab 2: OpenRouter Hub
                val isOpenRouter = activeProvider == AiProvider.OPENROUTER
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isOpenRouter) {
                                Modifier.background(Brush.linearGradient(listOf(Color(0xFF0D9488), Color(0xFF10B981))))
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable { onProviderChange(AiProvider.OPENROUTER) }
                        .padding(vertical = 10.dp, horizontal = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("🌐 Router", fontSize = 10.sp, fontWeight = if (isOpenRouter) FontWeight.Bold else FontWeight.Medium, color = if (isOpenRouter) Color.White else textColor)
                        if (isOpenRouter) {
                            Text("ACTIVE", fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }

                // Tab 3: Groq LPU
                val isGroq = activeProvider == AiProvider.GROQ
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isGroq) {
                                Modifier.background(Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C))))
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable { onProviderChange(AiProvider.GROQ) }
                        .padding(vertical = 10.dp, horizontal = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("⚡ Groq", fontSize = 10.sp, fontWeight = if (isGroq) FontWeight.Bold else FontWeight.Medium, color = if (isGroq) Color.White else textColor)
                        if (isGroq) {
                            Text("ACTIVE", fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }

                // Tab 4: Cloudflare Workers AI
                val isCloudflare = activeProvider == AiProvider.CLOUDFLARE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isCloudflare) {
                                Modifier.background(Brush.linearGradient(listOf(Color(0xFFF6821F), Color(0xFFEA580C))))
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable { onProviderChange(AiProvider.CLOUDFLARE) }
                        .padding(vertical = 10.dp, horizontal = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("☁️ Cloudflare", fontSize = 9.5.sp, fontWeight = if (isCloudflare) FontWeight.Bold else FontWeight.Medium, color = if (isCloudflare) Color.White else textColor)
                        if (isCloudflare) {
                            Text("ACTIVE", fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }

        // PROVIDER 1: NATIVE GEMINI SECTION
        AnimatedVisibility(
            visible = activeProvider == AiProvider.NATIVE_GEMINI,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header with Test Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Google Gemini",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF6366F1).copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("Gemini", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6366F1))
                        }
                    }

                    TextButton(
                        onClick = onVerifyGeminiKeys,
                        enabled = !isVerifyingGeminiKeys && geminiKey1.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (isVerifyingGeminiKeys) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color(0xFF6366F1), strokeWidth = 1.5.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Testing...", fontSize = 11.sp, color = Color(0xFF6366F1))
                        } else {
                            Text("⚡ Test Key", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                        }
                    }
                }

                // Active Gemini Model Card
                val currentGeminiModel = remember(geminiModels, selectedGeminiModelId) {
                    geminiModels.find { it.id.equals(selectedGeminiModelId, ignoreCase = true) }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF0F172A) else Color(0xFFF0F9FF),
                    border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ACTIVE GOOGLE GEMINI MODEL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0284C7)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentGeminiModel?.name ?: selectedGeminiModelId,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = selectedGeminiModelId,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0284C7),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = onOpenGeminiModelPicker,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Browse Models",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Browse Models ✦",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Select Chips for Top Gemini Models
                        val quickGeminiModels = listOf(
                            "gemini-3.8-flash" to "3.8 Flash 🌟",
                            "gemini-3.5-flash" to "3.5 Flash 🚀",
                            "gemini-3.1-pro-preview" to "3.1 Pro 🧠",
                            "gemini-3.1-flash-lite-preview" to "3.1 Lite ⚡",
                            "gemini-flash-latest" to "Flash Latest 🔄",
                            "gemini-3.7-flash" to "3.7 Flash 🌟"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickGeminiModels) { (mId, mLabel) ->
                                val isSelected = selectedGeminiModelId.equals(mId, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF38BDF8) else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                    modifier = Modifier.clickable { onSelectGeminiModel(mId) }
                                ) {
                                    Text(
                                        text = mLabel,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else subTextColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Gemini API Key Input Card (Single Key, No Fallback)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Google Gemini API Key (AIzaSy... / AQ...)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = geminiKey1,
                            onValueChange = onGeminiKey1Change,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("AIzaSy... or AQ...", fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                        Icon(
                                            imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Visibility",
                                            tint = subTextColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (geminiKey1.isBlank()) {
                                        IconButton(
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text
                                                if (!clip.isNullOrBlank()) {
                                                    onGeminiKey1Change(clip.trim())
                                                    Toast.makeText(context, "Pasted from clipboard! 📋", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp))
                                        }
                                    } else {
                                        IconButton(onClick = onClearGeminiKey1) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = subTextColor, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            )
                        )

                        if (geminiKey1Status.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = geminiKey1Status,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    geminiKey1Status.contains("Active") || geminiKey1Status.contains("Ready") || geminiKey1Status.contains("✅") -> Color(0xFF10B981)
                                    geminiKey1Status.contains("Invalid") || geminiKey1Status.contains("Error") || geminiKey1Status.contains("❌") -> Color(0xFFEF4444)
                                    else -> Color(0xFFF59E0B)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Hint
                        Text(
                            text = "💡 Direct single key without fallback. Get free key at aistudio.google.com.",
                            fontSize = 10.5.sp,
                            color = subTextColor,
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Quota Radar & Shield Card with On/Off Toggle
                        val usage = remember(selectedGeminiModelId) {
                            com.example.data.GeminiRateLimiter.getUsage(context, selectedGeminiModelId)
                        }
                        var showQuotaShieldDetailDialog by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (showGeminiQuotaBadge) Color(0xFF6366F1).copy(alpha = 0.1f) else (if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF1F5F9)),
                            border = BorderStroke(1.2.dp, if (showGeminiQuotaBadge) Color(0xFF6366F1).copy(alpha = 0.45f) else (if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("🛡️", fontSize = 15.sp)
                                        Column {
                                            Text(
                                                text = "Gemini Quota & Rate Limit Badge",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                            Text(
                                                text = if (showGeminiQuotaBadge) "Visible in AI Chat & Screens (ON)" else "Hidden in AI Chat & Screens (OFF)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (showGeminiQuotaBadge) Color(0xFF10B981) else subTextColor
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = showGeminiQuotaBadge,
                                        onCheckedChange = { onToggleShowGeminiQuotaBadge(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF6366F1),
                                            uncheckedThumbColor = Color(0xFF94A3B8),
                                            uncheckedTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡ Limit: ${usage.maxRpm} RPM • ${usage.remainingToday}/${usage.maxRpd} left",
                                        fontSize = 10.5.sp,
                                        color = if (usage.isNearLimit) Color(0xFFF59E0B) else (if (isDark) Color(0xFF93C5FD) else Color(0xFF4338CA)),
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    TextButton(
                                        onClick = { showQuotaShieldDetailDialog = true },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Inspect Radar 📊", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                                    }
                                }
                            }
                        }

                        if (showQuotaShieldDetailDialog) {
                            com.example.ui.components.GeminiQuotaShieldDialog(
                                quotaUsage = usage,
                                onDismissRequest = { showQuotaShieldDetailDialog = false },
                                onRefresh = {
                                    com.example.data.GeminiRateLimiter.refreshUsageState(context, selectedGeminiModelId)
                                }
                            )
                        }
                    }
                }
            }
        }

        // PROVIDER 2: OPENROUTER SECTION
        AnimatedVisibility(
            visible = activeProvider == AiProvider.OPENROUTER,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Active OpenRouter Model Card
                val currentModel = remember(openRouterModels, selectedOpenRouterModelId) {
                    openRouterModels.find { it.id == selectedOpenRouterModelId }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF0F2B26) else Color(0xFFECFDF5),
                    border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SELECTED OPENROUTER MODEL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF059669)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentModel?.name ?: selectedOpenRouterModelId,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = selectedOpenRouterModelId,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = subTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (currentModel?.isFree == true || selectedOpenRouterModelId.contains(":free")) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF10B981))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("100% FREE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                                if (currentModel?.isVision == true || selectedOpenRouterModelId.contains("vision") || selectedOpenRouterModelId.contains("flash")) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF0284C7))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("📷 OCR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Browse Models Button
                        Button(
                            onClick = onOpenModelPicker,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            contentPadding = PaddingValues(vertical = 9.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Browse & Change Model (${openRouterModels.size} Live Models)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Quick Model Selection Chips (Latest 2025/2026 Models)
                Column {
                    Text(
                        text = "⚡ Quick Pick Latest Free / Flagship Models:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = subTextColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val quickPicks = listOf(
                            Triple("Gemini 2.5 Flash", "google/gemini-2.5-flash", "⚡ High Speed & Vision"),
                            Triple("DeepSeek R1 (Free)", "deepseek/deepseek-r1:free", "🧠 Reasoning"),
                            Triple("Qwen 2.5 VL (Free)", "qwen/qwen-2.5-vl-72b-instruct:free", "📷 Diagrams"),
                            Triple("Claude 3.7 Sonnet", "anthropic/claude-3.7-sonnet", "🏆 SOTA"),
                            Triple("DeepSeek V3", "deepseek/deepseek-chat", "⚡ 671B MoE"),
                            Triple("Pixtral Large", "mistralai/pixtral-large-2411", "📷 128k Vision")
                        )

                        items(quickPicks) { (label, modelId, badge) ->
                            val isCurrent = selectedOpenRouterModelId == modelId
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) Color(0xFF10B981) else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                border = BorderStroke(1.dp, if (isCurrent) Color(0xFF10B981) else Color(0x3064748B)),
                                modifier = Modifier.clickable { onSelectOpenRouterModel(modelId) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) Color.White else textColor
                                    )
                                    Text(
                                        text = badge,
                                        fontSize = 9.sp,
                                        color = if (isCurrent) Color.White.copy(alpha = 0.85f) else subTextColor
                                    )
                                }
                            }
                        }
                    }
                }

                // OpenRouter API Key Input
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OpenRouter API Key (sk-or-v1-...)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor
                        )
                        Button(
                            onClick = onVerifyOpenRouterKey,
                            enabled = !isVerifyingOpenRouterKey && openRouterKey.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            if (isVerifyingOpenRouterKey) {
                                CircularProgressIndicator(modifier = Modifier.size(10.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Testing...", fontSize = 10.5.sp, color = Color.White)
                            } else {
                                Text("⚡ Verify Key", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = openRouterKey,
                        onValueChange = onOpenRouterKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("sk-or-v1-...", fontSize = 12.sp) },
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility",
                                        tint = subTextColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (openRouterKey.isBlank()) {
                                    IconButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                onOpenRouterKeyChange(clip.trim())
                                                Toast.makeText(context, "Pasted from clipboard! 📋", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    IconButton(onClick = onClearOpenRouterKey) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = subTextColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                        )
                    )

                    // OpenRouter Key Status Message
                    if (openRouterKeyStatus.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = openRouterKeyStatus,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                openRouterKeyStatus.contains("Connected") || openRouterKeyStatus.contains("✅") -> Color(0xFF10B981)
                                openRouterKeyStatus.contains("❌") || openRouterKeyStatus.contains("Invalid") -> Color(0xFFEF4444)
                                else -> Color(0xFFF59E0B)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // OpenRouter Key Info & Refresh Live Models Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Get free key from openrouter.ai/keys",
                            fontSize = 10.5.sp,
                            color = Color(0xFF0D9488),
                            fontWeight = FontWeight.Medium
                        )

                        TextButton(
                            onClick = onRefreshOpenRouterModels,
                            enabled = !isOpenRouterLoadingModels,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0D9488))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOpenRouterLoadingModels) "Fetching..." else "Sync Live Models",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D9488)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(8.dp))

                    // OpenRouter Token Limit Controller
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🎯", fontSize = 13.sp)
                                Text(
                                    "OpenRouter Token Limit",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0D9488).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$openRouterMaxTokens Tokens",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0D9488),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            "Controls maximum output length per response for OpenRouter models.",
                            fontSize = 10.sp,
                            color = subTextColor
                        )

                        // Preset Token Buttons
                        val tokenPresets = listOf(
                            2048 to "2K (Fast)",
                            4096 to "4K",
                            8192 to "8K (Std)",
                            16384 to "16K (Long)",
                            32768 to "32K (Max)"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tokenPresets.forEach { (tokens, label) ->
                                val isSelected = openRouterMaxTokens == tokens
                                Surface(
                                    onClick = { onOpenRouterMaxTokensChange(tokens) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF0D9488) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF0D9488) else if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else textColor,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Custom Token Input Option (Allows user to type ANY number of tokens)
                        var isCustomTokenMode by remember { mutableStateOf(openRouterMaxTokens !in listOf(2048, 4096, 8192, 16384, 32768)) }
                        var customTokenText by remember(openRouterMaxTokens) { mutableStateOf(openRouterMaxTokens.toString()) }

                        if (isCustomTokenMode) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customTokenText,
                                    onValueChange = { input ->
                                        customTokenText = input.filter { it.isDigit() }
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    label = { Text("Custom Tokens (1024 - 65536)", fontSize = 10.5.sp) },
                                    placeholder = { Text("e.g. 12000", fontSize = 10.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0D9488),
                                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Button(
                                    onClick = {
                                        val parsed = customTokenText.toIntOrNull()?.coerceIn(1024, 65536) ?: 8192
                                        customTokenText = parsed.toString()
                                        onOpenRouterMaxTokensChange(parsed)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { isCustomTokenMode = true },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "✏️ Set Custom Token Number",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0D9488)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // PROVIDER 3: GROQ LPU ENGINE SECTION
        AnimatedVisibility(
            visible = activeProvider == AiProvider.GROQ,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header with Test Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Groq LPU Engine (Ultra-Fast ⚡)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF97316)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF97316).copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("500-800 tps", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF97316))
                        }
                    }

                    TextButton(
                        onClick = onVerifyGroqKey,
                        enabled = !isVerifyingGroqKey && groqKey.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (isVerifyingGroqKey) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = Color(0xFFF97316))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Testing...", fontSize = 11.sp, color = Color(0xFFF97316))
                        } else {
                            Text("⚡ Test Key", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                        }
                    }
                }

                // Groq API Key Input Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Groq API Key (starts with 'gsk_')",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = groqKey,
                            onValueChange = onGroqKeyChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("gsk_...", fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                        Icon(
                                            imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Visibility",
                                            tint = subTextColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (groqKey.isBlank()) {
                                        IconButton(
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text
                                                if (!clip.isNullOrBlank()) {
                                                    onGroqKeyChange(clip.trim())
                                                    Toast.makeText(context, "Pasted from clipboard! 📋", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color(0xFFF97316), modifier = Modifier.size(18.dp))
                                        }
                                    } else {
                                        IconButton(onClick = onClearGroqKey) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = subTextColor, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF97316),
                                unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            )
                        )

                        if (groqKeyStatus.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = groqKeyStatus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    groqKeyStatus.contains("Active") || groqKeyStatus.contains("Ready") || groqKeyStatus.contains("⚡") -> Color(0xFF10B981)
                                    groqKeyStatus.contains("Invalid") || groqKeyStatus.contains("Error") -> Color(0xFFEF4444)
                                    else -> Color(0xFFF59E0B)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Get Free Key hint
                        Text(
                            text = "💡 Get your free Groq API key at console.groq.com/keys with 14,400 daily requests.",
                            fontSize = 10.5.sp,
                            color = subTextColor,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Active Groq Model Card (Compact, fixed height design)
                val effectiveGroqModels = if (groqModels.isNotEmpty()) groqModels else com.example.data.GroqManager.popularGroqModels
                val currentGroqModel = remember(effectiveGroqModels, selectedGroqModelId) {
                    effectiveGroqModels.find { it.id == selectedGroqModelId }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF2A170A) else Color(0xFFFFF7ED),
                    border = BorderStroke(1.5.dp, Color(0xFFF97316).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SELECTED GROQ LPU MODEL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFEA580C)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentGroqModel?.name ?: selectedGroqModelId,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = selectedGroqModelId,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = subTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF97316))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("500-800 TPS", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                                if (currentGroqModel?.isVision == true || selectedGroqModelId.contains("vision") || selectedGroqModelId.contains("qwen")) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF0284C7))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("🖼️ VISION", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Browse Models Button
                        Button(
                            onClick = onOpenGroqModelPicker,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                            contentPadding = PaddingValues(vertical = 9.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Browse & Change Model (${effectiveGroqModels.size} Live Models)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Model Chips
                        Text(
                            text = "⚡ Quick Switch Popular Models:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subTextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val quickModels = listOf(
                            "qwen/qwen3.8-27b" to "Qwen 3.8 (Vision)",
                            "llama-3.3-70b-versatile" to "Llama 3.3 70B",
                            "llama-3.2-11b-vision-preview" to "Llama 3.2 Vision",
                            "deepseek-r1-distill-llama-70b" to "DeepSeek R1"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickModels) { (id, label) ->
                                val isSelected = selectedGroqModelId == id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFF97316).copy(alpha = 0.2f) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) Color(0xFFF97316) else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.clickable { onSelectGroqModel(id) }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFFF97316) else textColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // PROVIDER 4: CLOUDFLARE WORKERS AI SECTION
        AnimatedVisibility(
            visible = activeProvider == AiProvider.CLOUDFLARE,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header with Test Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Cloudflare Workers AI (10,000 Free Daily ☁️)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF6821F)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF6821F).copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("10k Neurons Free", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF6821F))
                        }
                    }

                    TextButton(
                        onClick = onVerifyCloudflare,
                        enabled = !isVerifyingCloudflare && cloudflareAccountId.isNotBlank() && cloudflareApiToken.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (isVerifyingCloudflare) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = Color(0xFFF6821F))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Testing...", fontSize = 11.sp, color = Color(0xFFF6821F))
                        } else {
                            Text("⚡ Test Connection", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF6821F))
                        }
                    }
                }

                // Cloudflare Account ID & API Token Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Cloudflare Account ID (from dashboard):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = cloudflareAccountId,
                            onValueChange = onCloudflareAccountIdChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. c0e812f8...", fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            trailingIcon = {
                                if (cloudflareAccountId.isBlank()) {
                                    IconButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                onCloudflareAccountIdChange(clip.trim())
                                                Toast.makeText(context, "Pasted Account ID! 📋", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color(0xFFF6821F), modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    IconButton(onClick = { onCloudflareAccountIdChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = subTextColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF6821F),
                                unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Cloudflare API Token (Workers AI permission):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = cloudflareApiToken,
                            onValueChange = onCloudflareApiTokenChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("API Token...", fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            visualTransformation = if (isKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                        Icon(
                                            imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Visibility",
                                            tint = subTextColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (cloudflareApiToken.isBlank()) {
                                        IconButton(
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text
                                                if (!clip.isNullOrBlank()) {
                                                    onCloudflareApiTokenChange(clip.trim())
                                                    Toast.makeText(context, "Pasted API Token! 📋", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color(0xFFF6821F), modifier = Modifier.size(18.dp))
                                        }
                                    } else {
                                        IconButton(onClick = onClearCloudflareToken) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = subTextColor, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF6821F),
                                unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            )
                        )

                        if (cloudflareStatus.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = cloudflareStatus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    cloudflareStatus.contains("Active") || cloudflareStatus.contains("Ready") || cloudflareStatus.contains("☁️") -> Color(0xFF10B981)
                                    cloudflareStatus.contains("Error") || cloudflareStatus.contains("Invalid") || cloudflareStatus.contains("Required") -> Color(0xFFEF4444)
                                    else -> Color(0xFFF59E0B)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Get your free Account ID & Token at dash.cloudflare.com. Cloudflare provides 10,000 Neurons free every day!",
                            fontSize = 10.5.sp,
                            color = subTextColor,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Active Cloudflare Model Card
                val effectiveCfModels = if (cloudflareModels.isNotEmpty()) cloudflareModels else com.example.data.CloudflareManager.popularCloudflareModels
                val currentCfModel = remember(effectiveCfModels, selectedCloudflareModelId) {
                    effectiveCfModels.find { it.id == selectedCloudflareModelId }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF2A170A) else Color(0xFFFFF7ED),
                    border = BorderStroke(1.5.dp, Color(0xFFF6821F).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SELECTED CLOUDFLARE MODEL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFEA580C)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentCfModel?.name ?: selectedCloudflareModelId.substringAfterLast("/"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = selectedCloudflareModelId,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = subTextColor
                                )
                            }

                            Button(
                                onClick = onOpenCloudflareModelPicker,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF6821F)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Live Models", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Quick Model Switcher Chips
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "QUICK CLOUDFLARE MODELS:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subTextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val quickCfModels = listOf(
                            "@cf/deepseek-ai/deepseek-r1-distill-qwen-32b" to "DeepSeek R1 🧠",
                            "@cf/meta/llama-3.3-70b-instruct-fp8-fast" to "Llama 3.3 70B ⚡",
                            "@cf/meta/llama-3.2-11b-vision-instruct" to "Llama 3.2 Vision 🖼️",
                            "@cf/qwen/qwen2.5-72b-instruct" to "Qwen 2.5 72B 📐",
                            "@cf/meta/llama-3.1-8b-instruct" to "Llama 3.1 8B 🚀"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickCfModels) { (id, label) ->
                                val isSelected = selectedCloudflareModelId == id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFF6821F).copy(alpha = 0.2f) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) Color(0xFFF6821F) else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.clickable { onSelectCloudflareModel(id) }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFFF6821F) else textColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

@Composable
fun QuickAiEngineSwitchDialog(
    isDark: Boolean,
    activeProvider: AiProvider,
    onProviderChange: (AiProvider) -> Unit,
    selectedGeminiModel: String = "gemini-3.8-flash",
    onOpenGeminiModelPicker: () -> Unit = {},
    selectedOpenRouterModel: String,
    onOpenModelPicker: () -> Unit,
    selectedGroqModel: String = "",
    onOpenGroqModelPicker: () -> Unit = {},
    selectedCloudflareModel: String = "",
    onOpenCloudflareModelPicker: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val dialogBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderCol = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = dialogBg,
            border = BorderStroke(1.5.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🤖", fontSize = 20.sp)
                        Column {
                            Text("Active AI Engine", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Text("Choose provider for CBT, Doubt & OCR", fontSize = 11.sp, color = subTextColor)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = subTextColor)
                    }
                }

                // Option 1: Native Gemini
                val isGemini = activeProvider == AiProvider.NATIVE_GEMINI
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isGemini) Color(0xFF38BDF8).copy(alpha = 0.15f) else cardBg,
                    border = BorderStroke(if (isGemini) 2.dp else 1.dp, if (isGemini) Color(0xFF38BDF8) else borderCol),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onProviderChange(AiProvider.NATIVE_GEMINI)
                            onDismiss()
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("✦", fontSize = 22.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                Column {
                                    Text("Google Gemini Engine", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                                    Text("Official Google multimodal AI models", fontSize = 11.sp, color = subTextColor)
                                }
                            }
                            if (isGemini) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF38BDF8))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = "✦ $selectedGeminiModel",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0284C7),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            TextButton(
                                onClick = onOpenGeminiModelPicker,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Switch Model ✦", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            }
                        }
                    }
                }

                // Option 2: OpenRouter Hub
                val isOpenRouter = activeProvider == AiProvider.OPENROUTER
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isOpenRouter) Color(0xFF10B981).copy(alpha = 0.15f) else cardBg,
                    border = BorderStroke(if (isOpenRouter) 2.dp else 1.dp, if (isOpenRouter) Color(0xFF10B981) else borderCol),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onProviderChange(AiProvider.OPENROUTER)
                            onDismiss()
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("🌐", fontSize = 22.sp)
                                Column {
                                    Text("OpenRouter AI Hub", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                                    Text("300+ Live Models (DeepSeek, Claude, Qwen)", fontSize = 11.sp, color = subTextColor)
                                }
                            }
                            if (isOpenRouter) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF10B981))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }

                        if (isOpenRouter) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Model: ${selectedOpenRouterModel.substringAfterLast("/")}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    TextButton(onClick = onOpenModelPicker, contentPadding = PaddingValues(0.dp)) {
                                        Text("Change Model 🔍", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                                    }
                                }
                            }
                        }
                    }
                }

                // Option 3: Groq LPU Engine
                val isGroq = activeProvider == AiProvider.GROQ
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isGroq) Color(0xFFF97316).copy(alpha = 0.15f) else cardBg,
                    border = BorderStroke(if (isGroq) 2.dp else 1.dp, if (isGroq) Color(0xFFF97316) else borderCol),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onProviderChange(AiProvider.GROQ)
                            onDismiss()
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("⚡", fontSize = 22.sp)
                                Column {
                                    Text("Groq LPU Engine", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                                    Text("500-800 tps • Ultra-fast LPU & Vision OCR", fontSize = 11.sp, color = subTextColor)
                                }
                            }
                            if (isGroq) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF97316))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }

                        if (isGroq && selectedGroqModel.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF0F172A) else Color(0xFFFFF7ED),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Model: ${selectedGroqModel.substringAfterLast("/")}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    TextButton(onClick = onOpenGroqModelPicker, contentPadding = PaddingValues(0.dp)) {
                                        Text("Change Model 🔍", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                                    }
                                }
                            }
                        }
                    }
                }

                // Option 4: Cloudflare Workers AI
                val isCloudflare = activeProvider == AiProvider.CLOUDFLARE
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCloudflare) Color(0xFFF6821F).copy(alpha = 0.15f) else cardBg,
                    border = BorderStroke(if (isCloudflare) 2.dp else 1.dp, if (isCloudflare) Color(0xFFF6821F) else borderCol),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onProviderChange(AiProvider.CLOUDFLARE)
                            onDismiss()
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("☁️", fontSize = 22.sp)
                                Column {
                                    Text("Cloudflare Workers AI", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                                    Text("10,000 Free Daily Neurons • DeepSeek R1 • Llama 3.3", fontSize = 11.sp, color = subTextColor)
                                }
                            }
                            if (isCloudflare) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF6821F))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }

                        if (isCloudflare && selectedCloudflareModel.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF0F172A) else Color(0xFFFFF7ED),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Model: ${selectedCloudflareModel.substringAfterLast("/")}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    TextButton(onClick = onOpenCloudflareModelPicker, contentPadding = PaddingValues(0.dp)) {
                                        Text("Change Model 🔍", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF6821F))
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
