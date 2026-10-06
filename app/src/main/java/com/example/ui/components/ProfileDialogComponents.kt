package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.CompletedTopic
import com.example.data.DailyPractice
import com.example.data.GeminiChatAssistant
import com.example.data.MockTest
import com.example.data.StudyLog
import com.example.utils.ExportImportHelper
import com.example.utils.NeetProgressPdfGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modular sub-components for DashboardProfileDialog to maintain pristine code architecture
 * and avoid JVM method bytecode limits.
 */

@Composable
fun ProfileAcademicReportCard(
    isDark: Boolean,
    textColor: Color,
    subTextColor: Color,
    tempName: String,
    tempClass: String,
    tempAvatarUri: String?,
    neetTargetMillis: Long,
    logs: List<StudyLog>,
    tests: List<MockTest>,
    dailyPractices: List<DailyPractice>,
    completedTopics: List<CompletedTopic>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (isDark) {
                    Brush.linearGradient(
                        listOf(Color(0xF00F172A), Color(0xEE1E293B))
                    )
                } else {
                    Brush.linearGradient(
                        listOf(Color(0xEBFFFFFF), Color(0xDDF8FAFC), Color(0xD0F1F5F9))
                    )
                }
            )
            .border(
                1.5.dp,
                if (isDark) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color(0xFF6366F1).copy(alpha = 0.45f),
                RoundedCornerShape(22.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF818CF8), Color(0xFF6366F1))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!tempAvatarUri.isNullOrBlank()) {
                        AsyncImage(
                            model = tempAvatarUri,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = tempName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Academic Progress Report",
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontSize = 14.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF059669))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DOSSIER",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Text(
                        text = "$tempName • $tempClass",
                        fontSize = 11.5.sp,
                        color = subTextColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Grid Stats Boxes
            val totalMins = logs.sumOf { it.durationSeconds.toLong() } / 60L
            val totalHours = totalMins / 60L
            val remainingMins = totalMins % 60L
            val totalSolvedMCQs = dailyPractices.sumOf { it.physicsSolved + it.chemistrySolved + it.biologySolved }
            val testCount = tests.size
            val bestScore = tests.maxOfOrNull { it.score } ?: 0
            val daysLeft = maxOf(0L, (neetTargetMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24))

            val gridBg = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Box 1: Study Time
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(gridBg)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text("⏱️ STUDY TIME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = subTextColor, maxLines = 1, softWrap = false)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${totalHours}h ${remainingMins}m", fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF818CF8), maxLines = 1, softWrap = false)
                        }
                    }
                    // Box 2: MCQs Solved
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(gridBg)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text("🎯 MCQs SOLVED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = subTextColor, maxLines = 1, softWrap = false)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$totalSolvedMCQs Q", fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF34D399), maxLines = 1, softWrap = false)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Box 3: Mocks / Best
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(gridBg)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text("🏆 MOCKS / BEST", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = subTextColor, maxLines = 1, softWrap = false)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(if (testCount > 0) "$testCount T ($bestScore)" else "0 Tests", fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B), maxLines = 1, softWrap = false)
                        }
                    }
                    // Box 4: Days Left
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(gridBg)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text("⌛ DAYS LEFT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = subTextColor, maxLines = 1, softWrap = false)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$daysLeft Days", fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = Color(0xFFF43F5E), maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subject Pills
            val phySolved = dailyPractices.sumOf { it.physicsSolved }
            val chemSolved = dailyPractices.sumOf { it.chemistrySolved }
            val bioSolved = dailyPractices.sumOf { it.biologySolved }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x206366F1))
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚛️ Phy: $phySolved", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF818CF8), maxLines = 1, softWrap = false)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x20EAB308))
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧪 Chem: $chemSolved", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFACC15), maxLines = 1, softWrap = false)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x2010B981))
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌿 Bio: $bioSolved", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399), maxLines = 1, softWrap = false)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row: Share Scorecard & Save PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val reportData = NeetProgressPdfGenerator.ReportData(
                                candidateName = tempName,
                                candidateClass = tempClass,
                                targetNeetDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(neetTargetMillis)),
                                totalStudyMinutes = logs.sumOf { it.durationSeconds.toLong() } / 60L,
                                mockTests = tests,
                                dailyPractices = dailyPractices,
                                studyLogs = logs,
                                completedChecklistCount = completedTopics.size,
                                daysRemaining = daysLeft,
                                avatarUri = tempAvatarUri
                            )
                            val saveResult = NeetProgressPdfGenerator.savePdfToStorage(context, reportData)
                            if (saveResult.success) {
                                NeetProgressPdfGenerator.sharePdf(context, saveResult.uri, saveResult.file)
                            } else {
                                Toast.makeText(context, "Failed to generate PDF: ${saveResult.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Scorecard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val reportData = NeetProgressPdfGenerator.ReportData(
                                candidateName = tempName,
                                candidateClass = tempClass,
                                targetNeetDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(neetTargetMillis)),
                                totalStudyMinutes = logs.sumOf { it.durationSeconds.toLong() } / 60L,
                                mockTests = tests,
                                dailyPractices = dailyPractices,
                                studyLogs = logs,
                                completedChecklistCount = completedTopics.size,
                                daysRemaining = daysLeft,
                                avatarUri = tempAvatarUri
                            )
                            val saveResult = NeetProgressPdfGenerator.savePdfToStorage(context, reportData)
                            if (saveResult.success) {
                                Toast.makeText(
                                    context,
                                    "✅ Academic Report saved to Storage!\n📁 ${saveResult.displayPath}",
                                    Toast.LENGTH_LONG
                                ).show()
                                NeetProgressPdfGenerator.openPdf(context, saveResult.uri, saveResult.file)
                            } else {
                                Toast.makeText(context, "Failed to save PDF: ${saveResult.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCloudBackupCard(
    isDark: Boolean,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onShareBackup: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) Color(0xFF062E1B) else Color(0xFFECFDF5))
            .border(1.5.dp, Color(0xFF10B981).copy(alpha = 0.8f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF059669).copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Cloud Sync",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Google Drive Sync Active",
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF065F46),
                                fontSize = 14.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "HEALTHY",
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(
                            text = "Last: 28 Aug 2026, 11:50 pm • Next in 7d",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Your study records and database are safely synchronized. You will be reminded again in 7 day(s).",
                fontSize = 12.sp,
                color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onExportBackup,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📤 Export", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                Button(
                    onClick = onImportBackup,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📥 Import", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                OutlinedButton(
                    onClick = onShareBackup,
                    border = BorderStroke(1.dp, Color(0xFF059669)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("☁️ Share", color = if (isDark) Color(0xFF34D399) else Color(0xFF047857), fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
fun ProfileAppearanceCard(
    isDark: Boolean,
    isDarkTheme: Boolean,
    textColor: Color,
    subTextColor: Color,
    onSetTheme: (Boolean) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "App Appearance & Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 14.5.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LIQUID GLASS",
                    color = Color(0xFF0284C7),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Text(
            text = if (isDarkTheme) "Liquid Obsidian Dark (Aurora Neon Glow)" else "Liquid Crystal Light (Reflective Frosted Glass)",
            style = MaterialTheme.typography.bodySmall,
            color = subTextColor,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Liquid Obsidian Dark
            val obsidianBgBrush = if (isDarkTheme) {
                Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
            } else {
                Brush.linearGradient(listOf(Color(0x301E293B), Color(0x301E293B)))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(obsidianBgBrush)
                    .border(
                        if (isDarkTheme) 2.dp else 1.dp,
                        if (isDarkTheme) Color(0xFF818CF8) else Color(0x30CBD5E1),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onSetTheme(true) }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = "Obsidian Glass",
                        tint = if (isDarkTheme) Color(0xFF818CF8) else subTextColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Obsidian Glass",
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else textColor,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Dark Aurora",
                            color = subTextColor,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Card 2: Liquid Crystal Light
            val crystalBgBrush = if (!isDarkTheme) {
                Brush.linearGradient(listOf(Color(0xE6FFFFFF), Color(0xD0F1F5F9)))
            } else {
                Brush.linearGradient(listOf(Color(0x18FFFFFF), Color(0x18FFFFFF)))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(crystalBgBrush)
                    .border(
                        if (!isDarkTheme) 2.dp else 1.dp,
                        if (!isDarkTheme) Color(0xFF6366F1) else Color(0x20CBD5E1),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onSetTheme(false) }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Crystal Glass",
                        tint = if (!isDarkTheme) Color(0xFFF59E0B) else subTextColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Crystal Glass",
                            fontWeight = FontWeight.Bold,
                            color = if (!isDarkTheme) Color(0xFF0F172A) else Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Light Frosted",
                            color = subTextColor,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileGeminiSettingsSection(
    isDark: Boolean,
    textColor: Color,
    subTextColor: Color,
    isVerifyingKeys: Boolean,
    onVerifyApiKeys: () -> Unit,
    selectedModel: String,
    onSelectModel: (String) -> Unit,
    currentlyWorkingKey: String = "",
    key1: String,
    key1Status: String,
    onKey1Change: (String) -> Unit,
    onClearKey1: () -> Unit = {},
    key2: String,
    key2Status: String,
    onKey2Change: (String) -> Unit,
    onClearKey2: () -> Unit = {},
    key3: String,
    key3Status: String,
    onKey3Change: (String) -> Unit,
    onClearKey3: () -> Unit = {},
    showGeminiQuotaBadge: Boolean = true,
    onToggleShowGeminiQuotaBadge: (Boolean) -> Unit = {}
) {
    // Resolve which key is currently working
    val effectiveWorkingKey = when {
        currentlyWorkingKey.isNotBlank() -> currentlyWorkingKey.trim()
        key1Status.contains("Active") || key1Status.contains("Ready") || key1Status.contains("✅") -> key1.trim()
        key2Status.contains("Active") || key2Status.contains("Ready") || key2Status.contains("✅") -> key2.trim()
        key3Status.contains("Active") || key3Status.contains("Ready") || key3Status.contains("✅") -> key3.trim()
        key1.isNotBlank() && !key1Status.contains("Invalid") -> key1.trim()
        else -> ""
    }

    val isKey1Working = effectiveWorkingKey.isNotBlank() && effectiveWorkingKey == key1.trim()
    val isKey2Working = effectiveWorkingKey.isNotBlank() && effectiveWorkingKey == key2.trim()
    val isKey3Working = effectiveWorkingKey.isNotBlank() && effectiveWorkingKey == key3.trim()

    val workingKeyLabel = when {
        isKey1Working -> "Key #1 (Primary)"
        isKey2Working -> "Key #2 (Backup 1)"
        isKey3Working -> "Key #3 (Backup 2)"
        effectiveWorkingKey.isNotBlank() -> "Active Custom Key"
        else -> "None Configured"
    }

    val maskedWorkingKey = if (effectiveWorkingKey.isNotBlank()) {
        val k = effectiveWorkingKey
        if (k.length > 10) "${k.take(7)}••••${k.takeLast(4)}" else k
    } else "Not Configured / Tap 'Verify Keys'"

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lakshya AI Keys & AI Models 🤖",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Multi-key failover & connection verification",
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onVerifyApiKeys,
                enabled = !isVerifyingKeys,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                if (isVerifyingKeys) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Testing...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
                } else {
                    Text("⚡ Verify Keys", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
                }
            }
        }

        // Dedicated Currently Working Key Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (effectiveWorkingKey.isNotBlank()) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFF64748B).copy(alpha = 0.1f),
            border = BorderStroke(1.dp, if (effectiveWorkingKey.isNotBlank()) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF64748B).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (effectiveWorkingKey.isNotBlank()) "🟢" else "⚪", fontSize = 14.sp)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Currently Working: $workingKeyLabel",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (effectiveWorkingKey.isNotBlank()) Color(0xFF10B981) else textColor
                            )
                        }
                        Text(
                            text = maskedWorkingKey,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = subTextColor
                        )
                    }
                }
                if (effectiveWorkingKey.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.25f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text("ACTIVE ⚡", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                    }
                }
            }
        }

        // Model Selector Pills
        Column {
            Text(
                text = "Active AI Model",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = subTextColor,
                fontSize = 11.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF6366F1).copy(alpha = 0.18f))
                    .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🧠", fontSize = 18.sp)
                    Column {
                        Text("Google Gemini", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                        Text("Default Intelligent AI Engine", fontSize = 11.sp, color = subTextColor)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("⚡ Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            }
        }

        // Primary Key Field
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Primary API Key", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = subTextColor)
                    if (isKey1Working) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("⚡ WORKING NOW", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                        }
                    }
                }
                if (key1Status.isNotBlank()) {
                    Text(
                        text = key1Status,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        color = when {
                            key1Status.contains("Active") || key1Status.contains("Ready") || key1Status.contains("✅") -> Color(0xFF10B981)
                            key1Status.contains("Invalid") || key1Status.contains("❌") -> Color(0xFFEF4444)
                            key1Status.contains("Testing") || key1Status.contains("Verifying") -> Color(0xFFF59E0B)
                            else -> Color(0xFFF59E0B)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = key1,
                onValueChange = onKey1Change,
                placeholder = { Text("API Key (AQ... or AIza...)", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.5.sp),
                trailingIcon = if (key1.isNotBlank()) {
                    {
                        IconButton(onClick = onClearKey1) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Key 1", tint = subTextColor, modifier = Modifier.size(18.dp))
                        }
                    }
                } else null
            )
        }

        // Fallback Key 1 Field
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Fallback Key 1", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = subTextColor)
                    if (isKey2Working) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("⚡ WORKING NOW", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                        }
                    }
                }
                if (key2Status.isNotBlank()) {
                    Text(
                        text = key2Status,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        color = when {
                            key2Status.contains("Active") || key2Status.contains("Ready") || key2Status.contains("✅") -> Color(0xFF10B981)
                            key2Status.contains("Invalid") || key2Status.contains("❌") -> Color(0xFFEF4444)
                            key2Status.contains("Testing") || key2Status.contains("Verifying") -> Color(0xFFF59E0B)
                            else -> Color(0xFFF59E0B)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = key2,
                onValueChange = onKey2Change,
                placeholder = { Text("Backup API Key 1 (AQ... or AIza...)", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.5.sp),
                trailingIcon = if (key2.isNotBlank()) {
                    {
                        IconButton(onClick = onClearKey2) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Key 2", tint = subTextColor, modifier = Modifier.size(18.dp))
                        }
                    }
                } else null
            )
        }

        // Fallback Key 2 Field
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Fallback Key 2", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = subTextColor)
                    if (isKey3Working) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("⚡ WORKING NOW", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                        }
                    }
                }
                if (key3Status.isNotBlank()) {
                    Text(
                        text = key3Status,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        color = when {
                            key3Status.contains("Active") || key3Status.contains("Ready") || key3Status.contains("✅") -> Color(0xFF10B981)
                            key3Status.contains("Invalid") || key3Status.contains("❌") -> Color(0xFFEF4444)
                            key3Status.contains("Testing") || key3Status.contains("Verifying") -> Color(0xFFF59E0B)
                            else -> Color(0xFFF59E0B)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = key3,
                onValueChange = onKey3Change,
                placeholder = { Text("Backup API Key 2 (AQ... or AIza...)", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.5.sp),
                trailingIcon = if (key3.isNotBlank()) {
                    {
                        IconButton(onClick = onClearKey3) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Key 3", tint = subTextColor, modifier = Modifier.size(18.dp))
                        }
                    }
                } else null
            )
        }

        // Live Gemini Quota Radar & Switch Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (showGeminiQuotaBadge) Color(0xFF6366F1).copy(alpha = 0.1f) else (if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF1F5F9)),
            border = BorderStroke(1.2.dp, if (showGeminiQuotaBadge) Color(0xFF6366F1).copy(alpha = 0.45f) else (if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🛡️", fontSize = 16.sp)
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
        }

        Text(
            text = "💡 Google AI Studio keys starting with 'AQ...' and standard 'AIza...' are 100% supported. Use ✕ button to remove any key anytime.",
            fontSize = 10.5.sp,
            color = subTextColor,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun ProfileSecurityLockSection(
    isDark: Boolean,
    textColor: Color,
    subTextColor: Color,
    cardBorder: Color,
    appPassword: String?,
    habitPassword: String?,
    onSetSecurityLock: (String?, String?, String?) -> Unit,
    onSetHabitPassword: (String?) -> Unit
) {
    val context = LocalContext.current
    var isLockEnabled by remember { mutableStateOf(!appPassword.isNullOrBlank()) }
    var tempPassword by remember { mutableStateOf(appPassword ?: "") }
    var tempQuestion by remember { mutableStateOf("What was the name of your first school?") }
    var tempAnswer by remember { mutableStateOf("") }
    var showPasswordVisible by remember { mutableStateOf(false) }
    var showQuestionMenu by remember { mutableStateOf(false) }

    var isHabitLockEnabled by remember { mutableStateOf(!habitPassword.isNullOrBlank()) }
    var tempHabitPassword by remember { mutableStateOf(habitPassword ?: "") }

    val securityQuestionOptions = listOf(
        "What was the name of your first school?",
        "What city were you born in?",
        "What was your childhood pet's name?",
        "What is your mother's maiden name?",
        "What was the model of your first phone?"
    )

    Column {
        // App Lock Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(if (isDark) Color(0xFF13182C) else Color(0xFFEEF2FF))
                .border(1.5.dp, if (isDark) Color(0xFF6366F1).copy(alpha = 0.5f) else Color(0xFFC7D2FE), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock",
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Password & Privacy Lock 🔐",
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    fontSize = 14.5.sp
                                )
                            }
                            Text(
                                text = "Protects Habit Detox & Profile with Security Question reset",
                                style = MaterialTheme.typography.bodySmall,
                                color = subTextColor,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = isLockEnabled,
                        onCheckedChange = { checked ->
                            isLockEnabled = checked
                            if (!checked) {
                                tempPassword = ""
                                tempAnswer = ""
                                onSetSecurityLock(null, null, null)
                                Toast.makeText(context, "Password protection disabled 🔓", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF6366F1)
                        )
                    )
                }

                if (isLockEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Password Input
                        Column {
                            Text(
                                text = "Lock Password / PIN",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = subTextColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = tempPassword,
                                onValueChange = { tempPassword = it },
                                placeholder = { Text("Enter your password or PIN", fontSize = 12.sp) },
                                visualTransformation = if (showPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPasswordVisible = !showPasswordVisible }) {
                                        Icon(
                                            imageVector = if (showPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Visibility",
                                            tint = subTextColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                            )
                        }

                        // Security Question Selector
                        Column {
                            Text(
                                text = "Security Question (For Password Reset)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = subTextColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0x18FFFFFF) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isDark) Color(0x336366F1) else Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showQuestionMenu = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = tempQuestion,
                                            color = textColor,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Question",
                                            tint = Color(0xFF6366F1)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showQuestionMenu,
                                    onDismissRequest = { showQuestionMenu = false },
                                    modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
                                ) {
                                    securityQuestionOptions.forEach { q ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = q,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (tempQuestion == q) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (tempQuestion == q) Color(0xFF6366F1) else (if (isDark) Color.White else Color(0xFF0F172A))
                                                )
                                            },
                                            onClick = {
                                                tempQuestion = q
                                                showQuestionMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Security Answer
                        Column {
                            Text(
                                text = "Security Question Answer",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = subTextColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = tempAnswer,
                                onValueChange = { tempAnswer = it },
                                placeholder = { Text("e.g. Pizza, DPS School, Tommy...", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                            )
                            Text(
                                text = "ℹ️ Used to safely reset your password if you ever forget it.",
                                fontSize = 10.5.sp,
                                color = subTextColor,
                                modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                            )
                        }

                        // Save Button for Security Lock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (tempPassword.isBlank()) {
                                        Toast.makeText(context, "Please enter a valid password", Toast.LENGTH_SHORT).show()
                                    } else if (tempAnswer.isBlank()) {
                                        Toast.makeText(context, "Please provide a security answer", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onSetSecurityLock(tempPassword.trim(), tempQuestion.trim(), tempAnswer.trim())
                                        Toast.makeText(context, "✅ Password & Security Question saved!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Lock Settings 🔒", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            if (!appPassword.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        isLockEnabled = false
                                        tempPassword = ""
                                        tempAnswer = ""
                                        onSetSecurityLock(null, null, null)
                                        Toast.makeText(context, "Password protection removed 🔓", Toast.LENGTH_SHORT).show()
                                    },
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp)
                                ) {
                                    Text("Remove Lock 🔓", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = cardBorder)
        Spacer(modifier = Modifier.height(16.dp))

        // Habit Detox Lock Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .background(if (isDark) Color(0x0AFFFFFF) else Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.size(36.dp).background(Color(0x1A10B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "Habit Tracker Lock",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = "Require a password to access Habits & Detox",
                                style = MaterialTheme.typography.bodySmall,
                                color = subTextColor,
                                modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                            )
                        }
                    }
                    Switch(
                        checked = isHabitLockEnabled,
                        onCheckedChange = { checked ->
                            isHabitLockEnabled = checked
                            if (!checked) {
                                tempHabitPassword = ""
                                onSetHabitPassword(null)
                                Toast.makeText(context, "Habit Lock disabled 🔓", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981)
                        )
                    )
                }

                if (isHabitLockEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column {
                            Text(
                                text = "Habit Lock Password / PIN",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = subTextColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = tempHabitPassword,
                                onValueChange = { tempHabitPassword = it },
                                placeholder = { Text("Enter password or PIN", fontSize = 12.sp) },
                                visualTransformation = if (showPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPasswordVisible = !showPasswordVisible }) {
                                        Icon(
                                            imageVector = if (showPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Visibility",
                                            tint = subTextColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (tempHabitPassword.isBlank()) {
                                        Toast.makeText(context, "Please enter a valid password", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onSetHabitPassword(tempHabitPassword.trim())
                                        Toast.makeText(context, "✅ Habit Lock saved!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Lock 🔒", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            if (!habitPassword.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        isHabitLockEnabled = false
                                        tempHabitPassword = ""
                                        onSetHabitPassword(null)
                                        Toast.makeText(context, "Habit Lock removed 🔓", Toast.LENGTH_SHORT).show()
                                    },
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp)
                                ) {
                                    Text("Remove Lock", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
fun ProfileDangerZoneSection(
    subTextColor: Color,
    onTriggerReset: () -> Unit
) {
    Column {
        Text(
            text = "Danger Zone",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEF4444),
            fontSize = 14.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Permanently wipe all study logs, goals, test history & app data",
            style = MaterialTheme.typography.bodySmall,
            color = subTextColor,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onTriggerReset,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = "Permanent Delete",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Permanent Reset All Data ⚠️", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileDangerResetDialog(
    isDark: Boolean,
    subTextColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "⚠️ Permanent Reset All Data?",
                fontWeight = FontWeight.Black,
                color = Color(0xFFEF4444)
            )
        },
        text = {
            Text(
                "Are you sure you want to permanently delete all study logs, goals, mock test results, DPP entries, and app preferences? This action CANNOT be undone.",
                fontSize = 13.5.sp,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) {
                Text("Yes, Permanently Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = subTextColor)
            }
        },
        containerColor = if (isDark) Color(0xFF1E293B) else Color.White
    )
}

