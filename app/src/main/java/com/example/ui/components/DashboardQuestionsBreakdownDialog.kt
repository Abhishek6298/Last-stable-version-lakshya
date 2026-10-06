package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun DashboardQuestionsBreakdownDialog(
    isDark: Boolean,
    totalAllTimeQuestions: Int,
    totalPhysicsAllTime: Int,
    totalChemistryAllTime: Int,
    totalBiologyAllTime: Int,
    todaySolvedQuestions: Int,
    todayTargetQuestions: Int,
    pSolvedToday: Int,
    cSolvedToday: Int,
    bSolvedToday: Int,
    pTargetToday: Int,
    cTargetToday: Int,
    bTargetToday: Int,
    onNavigateToAiTest: () -> Unit,
    onNavigateToMistakes: () -> Unit,
    onNavigateToTargets: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(28.dp))
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF10B981).copy(alpha = 0.5f),
                            Color(0xFF38BDF8).copy(alpha = 0.3f)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF10B981), Color(0xFF059669))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                "Total Questions Solved",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary
                            )
                            Text(
                                "Cumulative Subject Progress",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Big Grand Total Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5),
                                        if (isDark) Color(0xFF042F2E) else Color(0xFFF0FDF4)
                                    )
                                )
                            )
                            .border(
                                1.5.dp,
                                Color(0xFF10B981).copy(alpha = 0.4f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "ALL-TIME SOLVED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669),
                                    letterSpacing = 1.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        "+$todaySolvedQuestions today",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "$totalAllTimeQuestions",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                )
                                Text(
                                    " Questions Total",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp, start = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Proportional Multi-Segment Bar
                            val safeTotal = totalAllTimeQuestions.coerceAtLeast(1).toFloat()
                            val pWeight = (totalPhysicsAllTime.toFloat() / safeTotal).coerceAtLeast(0.001f)
                            val cWeight = (totalChemistryAllTime.toFloat() / safeTotal).coerceAtLeast(0.001f)
                            val bWeight = (totalBiologyAllTime.toFloat() / safeTotal).coerceAtLeast(0.001f)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (isDark) Color(0x44334155) else Color(0xFFCBD5E1))
                            ) {
                                if (totalAllTimeQuestions > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(pWeight)
                                            .fillMaxHeight()
                                            .background(Color(0xFF38BDF8))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(cWeight)
                                            .fillMaxHeight()
                                            .background(Color(0xFFF59E0B))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(bWeight)
                                            .fillMaxHeight()
                                            .background(Color(0xFF10B981))
                                    )
                                }
                            }
                        }
                    }

                    // 3 Subject Detailed Cards
                    Text(
                        "Subject Breakdown",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        letterSpacing = 0.5.sp
                    )

                    // Physics Card
                    SubjectQuestionProgressCard(
                        subjectName = "Physics",
                        icon = "⚛️",
                        accentColor = Color(0xFF38BDF8),
                        allTimeCount = totalPhysicsAllTime,
                        todaySolved = pSolvedToday,
                        todayTarget = pTargetToday,
                        totalAllTime = totalAllTimeQuestions,
                        isDark = isDark
                    )

                    // Chemistry Card
                    SubjectQuestionProgressCard(
                        subjectName = "Chemistry",
                        icon = "🧪",
                        accentColor = Color(0xFFF59E0B),
                        allTimeCount = totalChemistryAllTime,
                        todaySolved = cSolvedToday,
                        todayTarget = cTargetToday,
                        totalAllTime = totalAllTimeQuestions,
                        isDark = isDark
                    )

                    // Biology Card
                    SubjectQuestionProgressCard(
                        subjectName = "Biology",
                        icon = "🧬",
                        accentColor = Color(0xFF10B981),
                        allTimeCount = totalBiologyAllTime,
                        todaySolved = bSolvedToday,
                        todayTarget = bTargetToday,
                        totalAllTime = totalAllTimeQuestions,
                        isDark = isDark
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Auto-Add Sources Info Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(cardBg)
                            .border(
                                1.dp,
                                if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                RoundedCornerShape(18.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "HOW QUESTIONS GET ADDED AUTOMATICALLY",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669),
                                    letterSpacing = 0.5.sp
                                )
                            }

                            AutoSourceItem(
                                title = "AI CBT Test Series",
                                description = "Whenever you submit full tests, mini mocks, or chapter tests, all attempted questions are counted here.",
                                iconColor = Color(0xFF38BDF8),
                                isDark = isDark
                            )

                            AutoSourceItem(
                                title = "Abhi Magic 🪄 Paper & Question Scanner",
                                description = "Questions extracted from photos, PDFs, or test series via Abhi Magic 🪄 OCR are added directly to your count.",
                                iconColor = Color(0xFFF59E0B),
                                isDark = isDark
                            )

                            AutoSourceItem(
                                title = "Practice Questions & Targets",
                                description = "Daily MCQs, Eduniti checklist solved questions, and NCERT practice drills are continuously added.",
                                iconColor = Color(0xFF10B981),
                                isDark = isDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onNavigateToAiTest()
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Text(
                            "AI CBT",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onNavigateToMistakes()
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Text(
                            "OCR Scan",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                    }

                    Button(
                        onClick = {
                            onNavigateToTargets()
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        )
                    ) {
                        Text(
                            "Practice",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectQuestionProgressCard(
    subjectName: String,
    icon: String,
    accentColor: Color,
    allTimeCount: Int,
    todaySolved: Int,
    todayTarget: Int,
    totalAllTime: Int,
    isDark: Boolean
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val pctOfTotal = if (totalAllTime > 0) {
        ((allTimeCount.toFloat() / totalAllTime.toFloat()) * 100).toInt()
    } else 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(
                1.dp,
                accentColor.copy(alpha = if (isDark) 0.35f else 0.25f),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(icon, fontSize = 16.sp)
                    Text(
                        subjectName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "$pctOfTotal% of total",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "All-Time Solved",
                        fontSize = 10.5.sp,
                        color = textSecondary
                    )
                    Text(
                        "$allTimeCount Qs",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Today's Progress",
                        fontSize = 10.5.sp,
                        color = textSecondary
                    )
                    Text(
                        "$todaySolved / $todayTarget Qs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoSourceItem(
    title: String,
    description: String,
    iconColor: Color,
    isDark: Boolean
) {
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(iconColor)
        )
        Column {
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                description,
                fontSize = 11.sp,
                color = textSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
